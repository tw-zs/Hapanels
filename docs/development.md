# Development

Dokumentacja dla developerów chcących przyczynić się do rozwoju Hapanels.

## 🛠️ Wymagania systemowe

Aby skompilować i uruchomić projekt, potrzebujesz:

### Oprogramowanie

| Narzędzie | Wersja | Opis |
|----------|--------|------|
| **JDK** | 17 | Java Development Kit |
| **Android SDK** | Najnowsza | SDK z platform i build tools używanymi przez Gradle |
| **Gradle** | 8.x | System budowania (zainstalowany przez Android Studio) |

### Zalecane środowisko

- **Android Studio** (Flamingo lub nowsze)
- **IntelliJ IDEA** z pluginem Android
- **VS Code** z rozszerzeniami Kotlin i Android

## 📦 Budowanie projektu

### Budowa APK

Aby zbudować wersję debug APK:

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:assembleGithubDebug
```

Plik APK zostanie wygenerowany w:

```
app/build/outputs/apk/github/debug/app-github-debug.apk
```

### Budowa release

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:assembleGithubRelease
```

## 🧪 Testy

### Uruchamianie testów jednostkowych

Testy dla screen managera i AOD:

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:testGithubDebugUnitTest --tests com.github.itskenny0.r1ha.core.hardware.PanelScreenManagerTest
```

### Uruchamianie wszystkich testów

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:testGithubDebugUnitTest
```

## 📚 Dokumentacja

Dokumentacja Hapanels jest budowana przy użyciu **MkDocs Material**.

### Instalacja zależności

```bash
python -m pip install -r requirements-docs.txt
```

### Uruchamianie serwera deweloperskiego

```bash
mkdocs serve
```

Serwer będzie dostępny pod adresem `http://localhost:8000` i będzie automatycznie odświeżał dokumentację przy zmianach.

### Budowanie dokumentacji

```bash
mkdocs build --strict
```

Opcja `--strict` powoduje, że budowanie zakończy się błędem przy nieprawidłowych linkach lub strukturze.

## 🔄 GitHub Pages

Dokumentacja jest automatycznie budowana i deployowana przez **GitHub Pages** po zmianach w:

- `docs/**` – Pliki dokumentacji
- `mkdocs.yml` – Konfiguracja MkDocs
- `requirements-docs.txt` – Zależności dokumentacji
- `.github/workflows/pages.yml` – Workflow GitHub Actions

### Ręczne deployowanie

Jeśli potrzebujesz wymusić rebuild dokumentacji:

1. Zmień dowolny plik w `docs/`
2. Zatwierdź i wypchnij zmiany do `main`
3. GitHub Actions automatycznie zbuduje i opublikuje dokumentację

## 📁 Struktura projektu

```
Hapanels/
├── app/                          # Główna aplikacja Android
│   ├── src/main/kotlin/          # Kod źródłowy Kotlin
│   ├── src/main/res/            # Zasoby (layouty, stringi, itp.)
│   └── build.gradle.kts          # Konfiguracja Gradle
├── docs/                         # Dokumentacja MkDocs
│   ├── index.md                  # Strona główna
│   ├── installation.md           # Instrukcje instalacji
│   └── ...
├── custom_components/            # Integracja Home Assistant
│   └── hapanels/                 # Custom component
├── mkdocs.yml                   # Konfiguracja MkDocs
└── requirements-docs.txt         # Zależności dokumentacji
```

## 🤝 Współpraca

### Fork i Pull Request

1. **Fork** repozytorium na swoim koncie GitHub
2. **Clone** swojego forka
3. Utwórz nowy **branch** dla swojej zmiany
4. Zatwierdź zmiany z czytelnymi commit message
5. Wypchnij branch do swojego forka
6. Utwórz **Pull Request** do głównego repozytorium

### Standardy kodu

- **Kotlin**: Postępuj zgodnie z [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- **Nazewnictwo**: Używaj `camelCase` dla zmiennych i funkcji, `PascalCase` dla klas
- **Dokumentacja**: Dodawaj komentarze dla publicznych API i złożonej logiki
- **Testy**: Dodawaj testy jednostkowe dla nowej funkcjonalności

### Komunikaty commit

Używaj [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: dodaj nową funkcjonalność
fix: popraw błąd w istniejącej funkcji
docs: uaktualnij dokumentację
docs(pl): uaktualnij polską dokumentację
refactor: refaktoryzacja kodu bez zmiany zachowania
chore: zmiany w konfiguracji, zależnościach itp.
```

## 🐛 Debugowanie

### Logowanie

Hapanels używa standardowego logowania Android (`android.util.Log`).

```kotlin
Log.d("TAG", "Debug message")
Log.i("TAG", "Info message")
Log.w("TAG", "Warning message")
Log.e("TAG", "Error message")
```

### Podgląd logów

```bash
# Podgląd logów przez ADB
adb logcat | grep -i hapanels

# Filtrowanie po tagu
adb logcat | grep -i "TAG"
```

### Debugowanie MQTT

Do debugowania połączeń MQTT:

1. Użyj klienta MQTT takiego jak **MQTT Explorer** lub **MQTTX**
2. Połącz się do tego samego brokera co Hapanels
3. Subskrybuj tematy `hapanels/#`

## 📡 API i protokoły

### MQTT

Hapanels używa **MQTT v3.1.1** do komunikacji z Home Assistant.

**Tematy bazowe:**
- `hapanels/<device>/...` – Tematy specyficzne dla urządzenia

**Quality of Service:**
- QOS 1 dla wiadomości, które muszą dotrzeć
- QOS 0 dla wiadomości statusowych

**Retained messages:**
- Wiadomości konfiguracyjne są publikowane jako retained
- Wiadomości statusowe nie są retained

### WebSocket

Komunikacja z Home Assistant odbywa się przez:
- **REST API** – Pobieranie stanów i konfiguracji
- **WebSocket** – Subskrypcja zmian stanów w czasie rzeczywistym

## 🎨 UI i UX

### Jetpack Compose

Hapanels używa **Jetpack Compose** do budowania interfejsu użytkownika.

**Główne komponenty:**
- `PanelGridScreen` – Główny ekran dashboardu
- `HapanelsTile` – Komponent kafla
- `AODScreen` – Ekran Always On Display

### Stylizacja

**Kolory:**
- Podążaj za paletą kolorów zdefiniowaną w `HapanelsTheme`
- Używaj `MaterialTheme.colorScheme` dla dostępu do kolorów

**Typografia:**
- Główna czcionka: **Nunito**
- Rozmiary: `Display`, `Heading`, `Body`, `Metadata`

## 📞 Wsparcie

### Zgłaszanie błędów

Przed zgłoszeniem błędu:

1. Sprawdź, czy problem nie został już zgłoszony
2. Upewnij się, że używasz najnowszej wersji
3. Zbierz logi i informacje o środowisku

**Szablon zgłoszenia:**

```markdown
## Opis problemu

## Kroki do reprodukcji
1. 
2. 
3. 

## Oczekiwane zachowanie

## Aktualne zachowanie

## Informacje o środowisku
- Wersja Hapanels:
- Model urządzenia:
- Wersja Android:
- Wersja Home Assistant:

## Logi
```

### Dyskusja

Do ogólnych pytań i dyskusji użyj:
- [GitHub Discussions](https://github.com/tw-zs/Hapanels/discussions)
- [Forum Home Assistant](https://community.home-assistant.io/)

---

## 📖 Zobacz także

- [Dokumentacja użytkownika](index.md)
- [Integracja Home Assistant](home-assistant-integration.md)
- [Konfiguracja sprzętu](hardware.md)
