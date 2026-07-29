# Development

## Requirements

- JDK 17
- Android SDK with platform/build tools used by the Gradle project

## Building the APK

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:assembleGithubDebug
```

## AOD / Screen Manager Tests

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:testGithubDebugUnitTest --tests com.github.itskenny0.r1ha.core.hardware.PanelScreenManagerTest
```

## Documentation

Documentation is built using MkDocs Material.

```bash
python -m pip install -r requirements-docs.txt
mkdocs serve
mkdocs build --strict
```

GitHub Pages builds and deploys the output of `mkdocs build` automatically upon changes to `docs/**`, `mkdocs.yml`, `requirements-docs.txt`, or the Pages workflow.
