package dev.by1337.core.util.math;

import org.junit.jupiter.api.Test;

import static dev.by1337.core.util.math.FastExpressionParser.Mode.*;
import static org.junit.jupiter.api.Assertions.*;

class FastExpressionParserTest {
    @Test
    void defaultPreservesStringHashesComparisonsAndFunctions() {
        assertEquals("word".hashCode(), FastExpressionParser.parse("word"));
        assertEquals(1, FastExpressionParser.parse("word==word", DEFAULT));
        assertEquals(1, FastExpressionParser.parse("true"));
        assertEquals(5, FastExpressionParser.parse("max(2,5)"));
        assertEquals(FastExpressionParser.parse("word"), FastExpressionParser.parse("word", DEFAULT));
    }

    @Test
    void suffixesWorkInBothModes() {
        String[][] suffixes = { { "k", "K", "к", "К" }, { "m", "M", "м", "М" },
                { "b", "B" }, { "t", "T", "т", "Т" } };
        double factor = 1_000;
        for (String[] group : suffixes) {
            for (String suffix : group) {
                for (var mode : FastExpressionParser.Mode.values()) {
                    assertEquals(2 * factor, FastExpressionParser.parse("2" + suffix, mode), suffix);
                }
            }
            factor *= 1_000;
        }
        assertEquals(2_000_000, FastExpressionParser.parse("2kk", NUMBERS_ONLY));
    }

    @Test
    void numbersOnlySupportsArithmeticAndParentheses() {
        assertEquals(6_000, FastExpressionParser.parse("(1k+2к)*2", NUMBERS_ONLY));
        assertEquals(-3, FastExpressionParser.parse("-(2+4)/2", NUMBERS_ONLY));
        assertEquals(1.5, FastExpressionParser.parse("+.5+1", NUMBERS_ONLY));
        assertEquals(500, FastExpressionParser.parse("2.5k/5", NUMBERS_ONLY));
        assertEquals(1, FastExpressionParser.parse("5%2", NUMBERS_ONLY));
        assertEquals(1_000, FastExpressionParser.parse("1e3", NUMBERS_ONLY));
    }

    @Test
    void numbersOnlyRejectsWordsFunctionsLogicAndInvalidMath() {
        for (String input : new String[]{ "word", "true", "max(2,5)", "rnd()", "1==1", "!1", "1<2",
                "2б", "2Б", "1/0", "0/0", "1e", "2+", "(2+3", ".", "" }) {
            assertThrows(FastExpressionParser.MathFormatException.class,
                    () -> FastExpressionParser.parse(input, NUMBERS_ONLY), input);
        }
    }
}
