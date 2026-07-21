# Development

## Wymagania

- JDK 17
- Android SDK z platform/build tools używanymi przez projekt Gradle

## Build APK

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:assembleGithubDebug
```

## Testy AOD/screen managera

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:testGithubDebugUnitTest --tests com.github.itskenny0.r1ha.core.hardware.PanelScreenManagerTest
```

## Dokumentacja

Dokumentacja jest budowana przez MkDocs Material.

```bash
python -m pip install -r requirements-docs.txt
mkdocs serve
mkdocs build --strict
```

GitHub Pages buduje i deployuje wynik `mkdocs build` automatycznie po zmianach w `docs/**`, `mkdocs.yml`, `requirements-docs.txt` albo workflow Pages.
