

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