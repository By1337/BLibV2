package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventConditionsTest {
    @Test
    void falseConstantActuallyFails() {
        assertFalse(CheckHandler.nopFalse().handle(new EventContext(false)));
    }

    @Test
    void booleanLiteralsSelectTheCorrectBranchAndReturnTheConditionResult() {
        for (String key : List.of("if", "if-all", "if-one")) {
            for (Object value : List.of(true, false, "true", "false", "TRUE", "FALSE")) {
                boolean expected = Boolean.parseBoolean(value.toString());
                assertBranch(key, value, expected);
            }
        }
    }

    @Test
    void allConditionKeysFollowTheirTruthTables() {
        for (String key : List.of("if", "if-all", "if-one")) {
            for (boolean first : List.of(false, true)) {
                for (boolean second : List.of(false, true)) {
                    boolean expected = key.equals("if-one") ? first || second : first && second;
                    assertBranch(key, List.of(first, second), expected);
                }
            }
        }
    }

    @Test
    void comparisonsFollowTheSameTruthTablesAsLiterals() {
        for (String key : List.of("if", "if-all", "if-one")) {
            for (boolean first : List.of(false, true)) {
                for (boolean second : List.of(false, true)) {
                    boolean expected = key.equals("if-one") ? first || second : first && second;
                    assertBranch(key, List.of(first ? "1 == 1" : "1 == 2", second ? "2 == 2" : "2 == 3"), expected);
                }
            }
        }
    }

    @Test
    void emptyAllIsTrueAndEmptyAnyIsFalse() {
        assertBranch("if", List.of(), true);
        assertBranch("if-all", List.of(), true);
        assertBranch("if-one", List.of(), false);
    }

    @Test
    void allStopsAfterFirstFailure() {
        assertShortCircuit("if", List.of("{first} == no", "{second} == yes"), false, List.of("{first}"));
        assertShortCircuit("if-all", List.of("{first} == no", "{second} == yes"), false, List.of("{first}"));
    }

    @Test
    void anyStopsAfterFirstSuccess() {
        assertShortCircuit("if-one", List.of("{first} == yes", "{second} == no"), true, List.of("{first}"));
    }

    @Test
    void anyContinuesAfterFailure() {
        assertShortCircuit("if-one", List.of("{first} == no", "{second} == yes"), true,
                List.of("{first}", "{second}"));
    }

    @Test
    void nestedListsKeepTheParentConditionMode() {
        assertBranch("if-one", List.of(List.of(false, true), false), true);
        assertBranch("if-one", List.of(List.of(false, false), false), false);
        assertBranch("if-all", List.of(List.of(true, false), true), false);
    }

    @Test
    void nestedConditionalBranchesAreDecodedIndependently() {
        List<String> output = new ArrayList<>();
        var handler = decode(Map.of(
                "if-all", List.of(true, true),
                "do", Map.of("if-one", List.of(false, false), "do", "[record] wrong", "else", "[record] nested"),
                "else", "[record] wrong outer"));
        assertTrue(handler.handle(context(output)));
        assertEquals(List.of("nested"), output);
    }

    @Test
    void falseConditionLetsOneOfReachFallback() {
        List<String> output = new ArrayList<>();
        var handler = decode(Map.of("oneOf", List.of(
                Map.of("if-one", List.of(false, false), "do", "[record] wrong"),
                Map.of("if", false, "do", "[record] wrong literal"),
                Map.of("do", "[record] fallback"))));
        assertTrue(handler.handle(context(output)));
        assertEquals(List.of("fallback"), output);
    }

    @Test
    void listContinuesAfterFalseAndKeepsFalseResult() {
        List<String> output = new ArrayList<>();
        var handler = decode(List.of(
                Map.of("if", false, "do", "[record] wrong", "else", "[record] failed"),
                "[record] after"));
        assertFalse(handler.handle(context(output)));
        assertEquals(List.of("failed", "after"), output);
    }

    @Test
    void doWithoutConditionStillRuns() {
        List<String> output = new ArrayList<>();
        assertTrue(decode(Map.of("do", "[record] unconditional")).handle(context(output)));
        assertEquals(List.of("unconditional"), output);
    }

    @Test
    void injectedCheckDecoderIsUsedInsideNestedBranches() {
        dev.by1337.yaml.decoder.YamlDecoder<CheckHandler> checks = (ctx, yaml) -> dev.by1337.yaml.decoder.YamlDecoder.STRING.decode(ctx, yaml)
                .map(value -> event -> value.equals("allow"));
        var decoder = new dev.by1337.edsl.decoder.EventHandlerDecoder(checks);
        var handler = decoder.decode(YamlValue.wrap(Map.of(
                "if", "allow",
                "do", Map.of("if", "deny", "else", "[record] injected")))).getOrThrow();
        List<String> output = new ArrayList<>();
        assertTrue(handler.handle(context(output)));
        assertEquals(List.of("injected"), output);
    }

    private void assertBranch(String key, Object condition, boolean expected) {
        List<String> output = new ArrayList<>();
        var handler = decode(Map.of(key, condition, "do", "[record] success", "else", "[record] failure"));
        String label = key + ": " + condition;
        assertEquals(expected, handler.handle(context(output)), label);
        assertEquals(List.of(expected ? "success" : "failure"), output, label);
    }

    private void assertShortCircuit(String key, List<String> conditions, boolean expected, List<String> expectedEvaluated) {
        List<String> evaluated = new ArrayList<>();
        var context = new EventContext(false) {
            @Override
            public String setPlaceholders(String source) {
                if (source.startsWith("{")) {
                    evaluated.add(source);
                    return "yes";
                }
                return source;
            }
        };
        assertEquals(expected, decode(Map.of(key, conditions)).handle(context));
        assertEquals(expectedEvaluated, evaluated);
    }

    private EventHandler decode(Object yaml) {
        var result = EventHandler.decoder().decode(YamlValue.wrap(yaml));
        assertFalse(result.hasError(), () -> String.valueOf(result.error()));
        return result.getOrThrow();
    }

    private EventContext context(List<String> output) {
        var command = new Command<EventContext>("root")
                .sub(new Command<EventContext>("[record]")
                        .argument(new ArgumentStrings<>("value"))
                        .executor((ctx, args) -> output.add((String) args.get("value"))));
        return new EventContext(false).setCommand(command);
    }
}
