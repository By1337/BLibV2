package dev.by1337.edsl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HandlerCodecBuilderTest {
    public static final String YAML = """
            test1: &test1
              - 'message'
              - |
                line 1
                line 2
            test2:
              - if: '100 == {test}'
                do: *test1
            
            
            on_click:
              - 'msg' # можно указывать сразу команду
              - if: '100 == 100' # условие
                do: # если условие верно, то выполнятся эти команды (ранее commands)
                  - '100 == 100'
                else: # если условие ложно, то выполнятся эти команды (ранее deny_commands)
                  - '100 != 100'
              - if: '%vault_eco_balance% > 1000'
                do:
                  console: 'eco take %player_name% 1000'
              - if: '99 == 99'
                do:
                  if: '88 == 88' # можно указывать if внутри других if
                  do: 'Вложенный if'
            on_left_click:
              - oneOf: # oneOf команды, это команды которые останавливаются на первом успешном if
                  - if: '%vault_eco_balance% < 1000'
                    do: 'У Вас не достаточно монет!'
                  - if: '%player_name% == _By1337_'
                    do: 'Вы не _By1337_'
                  - do: # просто do без условия
                      - 'Вы _By1337_ с 1000 монет!'
            on_view: # в on_view тоже можно использовать if
              - if: '%vault_eco_balance% > 1000'
                else: 'Ты слишком бедный, чтобы видеть этот предмет('
            on_right_click:
              - if-all: # можно объединить несколько условий
                  - '%vault_eco_balance% > 1000'
                  - '%player_name% == _By1337%'
                do: 'Все условия верны'
              - if-one:
                  - '%vault_eco_balance% > 1000'
                  - '100 < -1'
                do: 'Как минимум одно условие верно!'""";
    private static final Logger log = LoggerFactory.getLogger(HandlerCodecBuilderTest.class);


    private static AtomicReference<List<String>> response = new AtomicReference<>(new ArrayList<>());
    private static final Command<TestContext> command = new Command<TestContext>("root")
            .sub(new Command<TestContext>("[response]")
                    .argument(new ArgumentStrings<>("value"))
                    .executor((v, args) -> {
                        response.get().add((String) args.get("value"));
                    })
            );
    private static class TestContext extends EventContext<TestContext> {
        public TestContext() {
            super(true);
        }
        @Override
        public Command<TestContext> command() {
            return command;
        }

        @Override
        public Logger getLogger() {
            return log;
        }

        @Override
        public String setPlaceholders(String s) {
            return "";
        }
    }
    
    @Test
    void run(){
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  if: '100 == 100'
                  do: '[response] 1'
                  else: '[response] 2'"""));
        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());
        assertEquals(List.of("1"), response.get());
    }
    @Test
    void run2(){
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - if: '100 == 100'
                    do: '[response] 1'
                  - if: '99 == 99'
                    do: '[response] 2'"""));
        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());
        assertEquals(List.of("1", "2"), response.get());
    }

    @Test
    void ifElse() {
        YamlMap map = YamlMap.load(new StringReader("""
        test:
          - if: '1 == 2'
            do: '[response] ok'
            else: '[response] fail'
    """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("fail"), response.get());
    }

    @Test
    void multipleDoCommands() {
        YamlMap map = YamlMap.load(new StringReader("""
        test:
          - if: '1 == 1'
            do:
              - '[response] a'
              - '[response] b'
    """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("a", "b"), response.get());
    }

    @Test
    void nestedIf() {
        YamlMap map = YamlMap.load(new StringReader("""
        test:
          - if: '1 == 1'
            do:
              if: '2 == 2'
              do: '[response] nested'
    """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("nested"), response.get());
    }

    @Test
    void nestedIfFalse() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - if: '1 == 1'
                    do:
                      if: '2 == 3'
                      do: '[response] nested'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of(), response.get());
    }

    @Test
    void doWithoutIf() {
        YamlMap map = YamlMap.load(new StringReader("""
        test:
          - do: '[response] always'
    """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("always"), response.get());
    }

    @Test
    void oneOfFirstMatch() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - oneOf:
                      - if: '1 == 2'
                        do: '[response] a'
                      - if: '2 == 2'
                        do: '[response] b'
                      - do: '[response] c'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("b"), response.get());
    }

    @Test
    void oneOfFallback() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - oneOf:
                      - if: '1 == 2'
                        do: '[response] a'
                      - if: '2 == 3'
                        do: '[response] b'
                      - do: '[response] fallback'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("fallback"), response.get());
    }

    @Test
    void oneOfStopsAfterSuccess() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - oneOf:
                      - if: '1 == 1'
                        do: '[response] first'
                      - if: '1 == 1'
                        do: '[response] second'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("first"), response.get());
    }

    @Test
    void orderExecution() {
        YamlMap map = YamlMap.load(new StringReader("""
        test:
          - do: '[response] 1'
          - do: '[response] 2'
          - do: '[response] 3'
    """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("1", "2", "3"), response.get());
    }

    @Test
    void ifFalseNoElse() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - if: '1 == 2'
                    do: '[response] fail'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of(), response.get());
    }

    @Test
    void elseList() {
        YamlMap map = YamlMap.load(new StringReader("""
                    test:
                      - if: '1 == 2'
                        do: '[response] ok'
                        else:
                          - '[response] a'
                          - '[response] b'
                """));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handle(new TestContext());

        assertEquals(List.of("a", "b"), response.get());
    }
//      on_place:
//        - nearby-10:
//            - if: '%player_name% != {target:player_name}'
//              do:
//                - console: say give bad effect {target:player_name}
//            - bc: '%player_name% -> {target:player_name}'

    @Test
    void printerTest() {
        YamlMap map = YamlMap.load(new StringReader("""
                test:
                  - weights:
                      10:
                        - test: 123
                      20:
                        - test2: 321 
                  - test:
                      value1: '123'
                      value2: '321'"""));

        EventHandler<TestContext> e = decodeTest(map.get("test"));
        response.get().clear();
        e.handleAndClose(new TestContext());
        e.handleAndClose(new TestContext());

       // assertEquals(List.of("fallback"), response.get());
    }


    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static <T extends EventContext<T>> EventHandler<T> decodeTest(YamlValue v) {
        DataResult<EventHandler<T>> res = EventHandler.<T>codec().decode(v);
        if (res.hasError()) {
            System.out.println(res.error());
        }
        EventHandler<T> t = res.result();
        if (t == null){
            res.getOrThrow();
        }
        var encoded = t.encode();
        YamlMap map = new YamlMap();
        map.set("encoded", encoded);
        System.out.println(map.saveToString());
        System.out.println(gson.toJson(encoded));
        return t;
    }
}