# BLibV2

Многомодульная Maven-сборка BLibV2 и его библиотек.

| Каталог | Артефакты |
| --- | --- |
| `BCmd` | `dev.by1337.cmd:BCmd` |
| `BYaml` | `dev.by1337.yaml:common`, `dev.by1337.yaml:byaml-bukkit` |
| `CfgItems` | `dev.by1337.item:ConfigurableItems` |
| `common` | `dev.by1337.core:blib-common` |
| `plugin` | `dev.by1337.core:BLibV2`, готовый JAR плагина |

Движок EventDSL входит в исходники плагина. Общие классы для текста и переводов лежат в `common`. История EventDSL входит в историю BLibV2.

Серверные плагины по-прежнему используют `dev.by1337.core:BLibV2` с Maven scope `provided` и объявляют зависимость от плагина `BLib`.

## Сборка

Запускайте Maven из корня. Сборка использует JDK, на которой запущен Maven. Проверено на JDK 21, 25 и 27. Целевая версия байткода — Java 16. Первая сборка с доступом к сети заполняет Maven-кэш, включая готовые Legacy и NMS JAR для BPatch.

```sh
mvn clean install
mvn -o clean install
```

Путь к готовому плагину — `plugin/target/BLibV2-<версия>.jar`. Сборка из корня берёт изменения во внутренних зависимостях из Maven reactor. Фаза `install` также записывает артефакты в локальный Maven-репозиторий для других проектов.

## Версии

Единая версия задаётся в корневом [`.mvn/maven.config`](.mvn/maven.config). Из неё Maven также заполняет версию плагина в `plugin.yml`.

BYaml использует собственный родительский POM для обоих модулей. При отдельной сборке его версию задаёт [`pom.xml` BYaml](BYaml/pom.xml). Для сборки чистой YAML-библиотеки запустите `mvn -pl common -am clean install` из корня BYaml. Её Maven-координаты — `dev.by1337.yaml:common`.

BCmd и CfgItems также можно собирать командой `mvn clean install` из отдельных checkout. В этом режиме они используют версии из своих POM. Для CfgItems зависимости `byaml-bukkit` и `blib-common` должны быть доступны в Maven-репозитории. Их версию в отдельной сборке задаёт свойство `blib.version` в [`pom.xml` CfgItems](CfgItems/pom.xml).

## Git

`BCmd`, `BYaml` и `CfgItems` подключены как Git submodules. В локальной копии их Git-репозитории находятся в `.git/local-remotes`. URL в `.gitmodules` указывают на эти репозитории. При переносе сборки на сервер эти URL нужно заменить на адреса опубликованных репозиториев с соответствующими коммитами.
