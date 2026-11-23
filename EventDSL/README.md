

usages
```java
YamlDecoder<Config> DECODER = RecordYamlDecoder.mapOf(
    Config::new,
    MessageManager.decoder("messages.yml", "lang.yml").fieldOf()
);
Config config = DECODER.decode...;

config.eventCtx.call(key, player);
```

```xml
<root>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <relocations>
                                <relocation>
                                    <pattern>dev.by1337.edsl</pattern>
                                    <!-- package -->
                                    <shadedPattern>dev.by1337.auc.libs.dev.by1337.edsl</shadedPattern>
                                </relocation>
                            </relocations>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>by1337-repo</id>
            <url>https://repo.by1337.space/repository/maven-releases/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>dev.by1337.edsl</groupId>
            <artifactId>EventDSL</artifactId>
            <version>1.5</version>
        </dependency>
    </dependencies>

</root>
```

### Shared entity text (1.8.6)

`LocalizedText.DECODER` accepts an EventDSL string or a nested locale map:

```yaml
hit:
  en_US: '<gold>Hit {0}/{1}'
  ru_RU: '<gold>Бей {0}/{1}'
```

Use `LocalizedText.of("English default")` for decoder defaults. Values remain
`RawTextComponentLike`; English is preferred as the unknown-locale fallback.
Own one `TextRegistry` for the lifetime of the objects carrying its references:

```java
var reference = registry.component(config.hit(), hits, total);
label.setText(reference);
```

The registry uses the shared `LangTranslator`; do not create another translator
or render for the opener. Paper 26.3 resolves the component in each recipient's
packet. The same decoded message keeps one key when arguments change.
Close the registry after removing its entities/items; runtime keys are removed
completely, without accumulating per-opening tombstones. BCrates addons should
use `context.text(message, args...)`, which owns registration and cleanup.
