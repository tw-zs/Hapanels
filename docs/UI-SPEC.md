---
status: canonical
created: 2026-07-20
updated: 2026-07-20
scope: panel-refinement
platforms: [android-compose, home-assistant-studio]
---

# Hapanels — kanoniczny kontrakt UI

> Kontrakt wyglądu, informacji i interakcji dla Panel Grid, Cards, AOD oraz Hapanels Studio. Dokument opisuje stan docelowy. Gdy kod zachowuje się inaczej, wpis oznaczony **Locked target** jest wymaganiem do wdrożenia, nie opisem funkcji już gotowej.

## 1. Zakres i statusy

Dokument obejmuje dopracowanie wyglądu panelu, typy kafli, prezentację encji, foldery i popupy, akcje, układy responsywne, Cards, Panel Grid, AOD oraz podgląd i edytor Studio.

Stosowane etykiety:

- **Current** — zachowanie istniejące w obecnych źródłach.
- **Locked target** — obowiązujący rezultat implementacji.
- **Deferred** — świadomie poza tym kontraktem; nie implementować przy okazji.

## 2. Zasady produktu

1. **Renderer natywny.** Panel Grid, Cards, popupy, foldery i AOD renderuje Jetpack Compose. Lovelace/WebView nie zastępuje głównego UI.
2. **Informacja przed dekoracją.** Nazwa, stan i skutek dotknięcia muszą być zrozumiałe bez odgadywania koloru lub ikony.
3. **Jedno dotknięcie, jeden przewidywalny skutek.** Akcja nie zmienia znaczenia zależnie od miejsca pustego wewnątrz kafla.
4. **Bezpieczne sterowanie.** Otwieranie zamka, bramy i inne ryzykowne operacje nie mogą być przypadkowym toggle.
5. **Panel działa z daleka i z bliska.** Najważniejszy stan jest czytelny z 2–3 m; szczegóły i kontrolki pozostają dotykalne przy panelu.
6. **Stan HA jest prawdą runtime.** Optymistyczna reakcja może pokazać wysłanie polecenia, ale nie może udawać potwierdzonego stanu.
7. **Studio edytuje ten sam model.** Nie istnieją pola „tylko w JS”, które wyglądają na zapisane, lecz Android je ignoruje.
8. **Boring first.** Używać obecnego Compose, kotlinx.serialization, MDI, Nunito i natywnego custom elementu HA. Bez nowego frameworka lub biblioteki UI.

## 3. Hierarchia source of truth

Przy konflikcie obowiązuje kolejność:

1. Ten dokument — kontrakt produktu i interakcji.
2. Wersjonowany, współdzielony schemat JSON dashboardu — nazwy pól, typy, walidacja, migracja.
3. Natywny renderer Compose — referencja geometrii i zachowania runtime.
4. Hapanels Studio — autor i przybliżony podgląd tego samego schematu.
5. `preview.html` — wyłącznie fixture deweloperski, nigdy źródło semantyki.
6. Cards i `EntityOverride` — źródło istniejących wzorców domenowych; nie zmienia automatycznie konfiguracji Panel Grid.

**Current:** Kotlin używa `ignoreUnknownKeys = true`; Studio zapisuje między innymi `layout_editor` i `showIcon/showTitle/showSubtitle`, których bieżący model Androida nie deklaruje.

**Locked target:** zapis Studio przechodzi walidację względem wersji schematu obsługiwanej przez tablet. Nieobsługiwane pole blokuje zapis z komunikatem wskazującym pole; nie jest cicho pomijane.

## 4. System wizualny

### 4.1 Narzędzia i zasoby

| Właściwość | Kontrakt |
|---|---|
| Renderer panelu | Jetpack Compose, istniejący stack |
| Renderer Studio | istniejący Web Component w panelu HA |
| Font panelu | Nunito z lokalnych zasobów; fallback sans-serif tylko przy błędzie zasobu |
| Font Studio | font HA, następnie Nunito/system-ui |
| Ikony | MDI; brak ikony → `mdi:cog` i błąd walidacji w Studio |
| Design system | własne tokeny Hapanels; shadcn/registry nie dotyczy |

### 4.2 Viewport i siatka referencyjna

**Locked target:** główny viewport urządzenia to `1280 × 800 px`. Górne 48 px jest strefą systemową/chrome. Powierzchnia treści Panel Grid ma `1280 × 752 px`.

- Margines treści: 16 px z każdej strony.
- Siatka wide: 12 kolumn × 9 wierszy.
- Gap: 8 px.
- Pozycje `col`/`row` są 1-based; spany co najmniej 1.
- Renderer zaokrągla piksele deterministycznie; ostatnia kolumna/wiersz przejmuje resztę, bez szczeliny na brzegu.
- Kafel nie może wychodzić poza siatkę ani nakładać się na inny. Studio blokuje zapis takiego draftu.
- Root nie ma stałego tytułu zajmującego siatkę. Subpanel ma pasek nawigacji wewnątrz strefy treści o wysokości 48 px, po czym osobną siatkę.

### 4.3 Breakpointy i reflow

Breakpoint liczony z szerokości dostępnej treści w dp:

| Klasa | Zakres | Panel Grid |
|---|---:|---|
| Compact | `< 600 dp` | 2 kolumny semantyczne; pojedynczy scroll pionowy; kolejność `order`; kafle large zajmują 2 kolumny, small/action 1 |
| Standard | `600–819 dp` | 4 kolumny semantyczne; scroll pionowy; large 2 kolumny, small/action 1 |
| Wide | `>= 820 dp` | stała siatka 12 × 9 bez scrolla przy referencyjnym 1280 × 752 |

Zasady:

- Układ wide jest referencją Studio.
- Na compact/standard `col` i `row` nie są skalowane do mikroskopijnych komórek. Kafle są pakowane wierszami według `order`, z zachowaniem względnej wielkości.
- `short_label` zastępuje `label` tylko wtedy, gdy `label` nie mieści się w dwóch liniach. Studio pokazuje oba warianty.
- Orientacja zmienia układ, nie semantykę ani kolejność. Powrót do landscape odtwarza wide grid.
- Popup: wide 70% × 70% powierzchni treści; standard 84% × 78%; compact pełna szerokość i maks. 90% wysokości.
- Studio: przy `<= 900 px` edytor przechodzi z 3 kolumn do 1; przy `<= 620 px` formularze mają 1 kolumnę. Wszystkie akcje pozostają dostępne bez poziomego scrolla strony.

### 4.4 Spacing

| Token | Wartość | Użycie |
|---|---:|---|
| `xs` | 4 px | drobny odstęp ikona–status |
| `sm` | 8 px | gap siatki, kontrolki inline |
| `md` | 16 px | padding kafla i strony |
| `lg` | 24 px | sekcje, nagłówki Studio |
| `xl` | 32 px | duże rozdzielenie sekcji |
| `2xl` | 48 px | pasek nawigacji, minimalny target |
| `3xl` | 64 px | odstęp pustego stanu |

Wyjątki: padding zwartego kafla 12 px; target ikony nadal minimum 48 × 48 px. Nie dodawać wartości 6, 10, 14, 18 jako nowych tokenów układu.

### 4.5 Typografia

Do panelu i Studio stosować cztery role i dwa ciężary:

| Rola | Rozmiar | Waga | Line-height | Użycie |
|---|---:|---:|---:|---|
| Metadata | 12 sp/px | 400 | 1.33 | jednostka, drugorzędny stan, walidacja pomocnicza |
| Body/Label | 16 sp/px | 400 lub 700 | 1.5 | nazwa kafla, przycisk, treść |
| Heading/Value | 24 sp/px | 700 | 1.2 | nagłówek popupu, główna wartość małego kafla |
| Display | 80 sp/px | 700 | 1.0 | zegar i pojedyncza wartość hero |

- Maks. 2 linie nazwy kafla; potem ellipsis.
- Status i jednostka: maks. 1 linia.
- Nie używać ALL CAPS dla zwykłych nazw. ALL CAPS do krótkiego statusu technicznego do 12 znaków.
- Zegar skaluje rolę Display w kontenerze, nie wprowadza nowego tokenu. Minimum wizualne na compact odpowiada 48 sp.
- Tekst musi respektować systemowe skalowanie do 1.3× bez obcięcia głównej nazwy i akcji.

### 4.6 Kolor i kontrast

Każdy preset mapuje te same role z `HapanelsThemeColors`. Domyślny dark:

| Rola | Wartość | Udział i użycie |
|---|---|---|
| Dominant | `panelBg #090D10` | 60%: tło panelu i overlay |
| Secondary | `surface #23242D`, `surfaceVariant #2E303A` | 30%: kafle, popup, nawigacja |
| Accent | `accent #E99900` | 10%: aktywny stan, wybrany element, progress, focus, jedna główna CTA |
| Danger | `#FF5338` | wyłącznie błąd, niedostępność krytyczna, akcja destrukcyjna |
| Success | `#58C56A` | potwierdzony stan aktywny/sukces; nie dekoracja |
| Primary text | `#FFFFFF` | nazwy i wartości |
| Muted text | `#888C96` | metadane; tylko na odpowiednio ciemnym tle |

Akcent jest zarezerwowany dla: aktywnej ikony/indicatora, zaznaczonego presetu, focus ring, progressu i głównej CTA „Zapisz układ”. Nie barwi wszystkich przycisków jednocześnie.

**Locked target — semantyka `HapanelsTileAccent`:** obecny enum pozostaje minimalny i nie dostaje nowych dekoracyjnych kolorów:

| Wartość config | Rola docelowa | Dozwolone użycie |
|---|---|---|
| `orange` | `accent` motywu | domyślny akcent kafla i zaznaczonej funkcji |
| `white` | neutral | ikona/element bez semantyki aktywności lub zagrożenia; w light mode używa kontrastowego `textPrimary`, nie literalnej bieli |
| `red` | danger | wyłącznie jawnie destrukcyjna lub niebezpieczna akcja, np. „Usuń”, „Rozbrój alarm”; wymaga właściwego confirmation |

- Studio odrzuca zapis `accent: red` użyty dekoracyjnie, na read-only value albo zwykłym toggle.
- `tile.accent` opisuje stałą rolę kafla/akcji. Nie opisuje bieżącego stanu encji.
- Kolory stanu encji są obliczane osobno z live state: active/success, inactive/muted, unavailable/danger. Nie zapisuje się ich w `tile.accent` i nie są nadpisywane przez statyczne `red`/`orange`/`white`.
- Kafel encji może mieć neutralną ikonę i równocześnie zielony tekst stanu „Włączone”; te role nie konkurują.

- Tekst zwykły: kontrast minimum 4.5:1; tekst duży i ikona informacyjna 3:1.
- Stan nie może być zakodowany tylko kolorem: wymagane słowo, ikona lub kształt.
- Preset niespełniający kontrastu dla par `textPrimary/surface`, `textMuted/surface`, `accent/panelBg` nie może zostać opublikowany.

### 4.7 Kształt, border, elevation

- Kafel: radius 18 px, border 1 px `border`, brak stałego cienia na panelu.
- Mała kontrolka/pill: radius 12 px; badge statusu może być pełną kapsułą.
- Popup: radius 28 px, border 1 px; overlay 96% tła, bez blur wymaganego do działania.
- Studio card: radius 18–22 px, border 1 px, cień najwyżej `0 12px 32px rgba(0,0,0,.18)`.
- Focus ring: 2 px accent + 2 px odstępu; nie zmienia wymiaru elementu.
- Nie używać glassmorphism jako wymaganej semantyki. Popup musi pozostać czytelny bez `backdrop-filter`.

**Locked target — tryb motywu:** Studio oferuje `light`, `dark` i `system`. Wartość `system` jest zapisywana w dashboard config i na tablecie podąża za trybem Androida. Podgląd Studio rozwiązuje `system` według aktualnego motywu HA/przeglądarki i jawnie pokazuje wynik jako „System · jasny” albo „System · ciemny”; nie zmienia zapisanej wartości na `light` lub `dark`.

### 4.8 Motion

- Press: 100 ms do skali 0.97 i alpha 0.82; powrót 120 ms.
- Popup enter: 180 ms fade + scale 0.965 → 1; exit 140 ms.
- Nawigacja folderu: 180 ms poziomy slide/fade.
- Aktualizacja stanu: 160 ms crossfade koloru/tekstu; żadnego pulsowania ciągłego.
- Roleta: animacja do potwierdzonej pozycji 300–520 ms; podczas drag podąża 1:1.
- `prefers-reduced-motion` w Studio i systemowe ograniczenie animacji w Androidzie wyłącza przesunięcia/skale; zachowuje natychmiastową zmianę stanu.
- AOD nie ma dekoracyjnych animacji. Fade wake jest zgodny z `wake_fade_ms`, zakres 0–2000 ms.

### 4.9 Stany komponentów

| Stan | Kontrakt |
|---|---|
| Default | pełny kontrast, border 1 px |
| Focus | widoczny ring 2 px; kolejność focus logiczna |
| Pressed | scale/alpha; semantyka bez zmiany |
| Disabled | alpha 0.45, brak press/haptic, przyczyna dostępna w tekście/tooltipie |
| Loading config | centralny progress + „Ładowanie panelu…”; po 8 s przycisk „Spróbuj ponownie” |
| Pending command | mały progress przy stanie; dotychczasowy stan pozostaje widoczny |
| Unavailable | alpha 0.55, tekst „Niedostępne”, danger indicator, brak akcji sterującej |
| Unknown/missing | tekst „Brak danych” lub „Nie znaleziono encji”; neutralny kolor, brak toggle |
| Error | krótki opis + droga naprawy; zachować ostatni potwierdzony stan |
| Empty | instrukcja, gdzie dodać element; bez atrap „Brak kafla” w runtime |

Touch target minimum 48 × 48 dp. Odstęp między niezależnymi targetami minimum 8 dp. Hold uruchamia się po 500 ms, daje jeden haptic i nie wykonuje potem tap.

## 5. Architektura informacji i nawigacja

### 5.1 Root, panel i popup

- Root zawiera kafle bez przypisanego rodzica oraz kafle wejściowe `folder`/`popup`.
- Folder otwiera panel wskazany przez stabilne `panel_id`.
- Panel ma własny tytuł, layout i listę kafli; nie jest wywnioskowany wyłącznie przez filtrowanie płaskiej listy.
- Popup pokazuje panel wskazany przez `panel_id` jako modal nad bieżącym kontekstem.
- Popup ma zawsze widoczny przycisk zamknięcia w prawym górnym rogu powierzchni modala: target 48 × 48 px, ikona `mdi:close`, accessible label „Zamknij popup”. Przycisk nie nachodzi na tytuł; nagłówek rezerwuje dla niego 48 px szerokości.
- Dotknięcie backdropu zamyka popup. Dotknięcie wnętrza nie propaguje do backdropu. Android Back oraz Escape w Studio zamykają popup i zwracają focus na kafel, który go otworzył. Żadna z tych dróg nie wykonuje akcji kafla pod modalem.
- Maksymalna głębokość folderów: 3 poziomy poniżej root. Popup nie zwiększa głębokości, ale popup w popupie jest niedozwolony.
- Cykl `panel_id` jest błędem walidacji. Usunięcie panelu używanego przez folder/popup jest blokowane do czasu usunięcia referencji lub jawnego potwierdzenia usunięcia obu.

**Current:** Compose obsługuje jeden `currentPanelId`, powrót bez pełnego stosu oraz płaskie powiązanie dzieci przez wspólny `panel_id`.

**Locked target:** nawigacja używa stosu paneli. Pasek subpanelu ma stałe 64 dp i znajduje się poza obszarem siatki, więc przycisk oraz tytuł nigdy nie nakładają się na kafle. Pokazuje target Wstecz 48 × 48, tytuł i breadcrumbs. Na wide breadcrumbs: `Dom / Parter / Salon`; na compact tylko `Wstecz` + bieżący tytuł. Root Panel Grid nigdy nie pokazuje tego paska ani tytułu i wykorzystuje pełną wysokość siatki. Android Back zamyka najpierw popup, potem cofa folder, potem opuszcza Panel Grid.

### 5.2 Pusty i uszkodzony kontekst

- Pusty root: „Brak kafli” / „Dodaj pierwszy kafel w Hapanels Studio.”
- Pusty panel: „Ten panel jest pusty” / „Dodaj kafle w Hapanels Studio.”
- Pusta przeglądarka/sekcja kamer: tytuł „Brak kamer”, treść „Dodaj encję camera w Hapanels Studio.” Opcjonalna CTA „Dodaj kamerę” jest widoczna wyłącznie wtedy, gdy bieżąca powierzchnia ma działającą nawigację bezpośrednio do edytora kamer w Studio; bez tej trasy nie pokazywać martwej CTA.
- Brak docelowego `panel_id`: nie nawigować do pustego ekranu; pokazać toast/banner „Nie znaleziono panelu. Sprawdź konfigurację w Studio.”
- Brak encji: kafel pozostaje na miejscu jako błąd konfiguracji; nie przesuwa reszty siatki.
- Encja chwilowo niedostępna: stan unavailable, nie missing.

## 6. Taksonomia kafli

### 6.1 Presety rozmiarów

Na wide rozmiar semantyczny daje domyślne minimum; jawne `colSpan/rowSpan` może powiększyć kafel, ale nie zmniejszyć poniżej minimum rodzaju.

| Size | Domyślny span | Minimalna zawartość |
|---|---:|---|
| `action` | 2 × 2 | ikona + czasownik |
| `small` | 2 × 2 | ikona + nazwa + jeden stan |
| `large` | 4 × 3 | ikona/wizualizacja + nazwa + stan + opcjonalna kontrolka |

**Locked target:** `text` i `spacer` mogą używać `small`, `action` lub `large`; jawne spany nadal nie mogą być mniejsze niż 1 × 1. `spacer` nie ma minimalnej zawartości ani targetu dotykowego.

### 6.2 Wspólna anatomia i hierarchia wizualna

**Locked target:** zwykły kafel Panel Grid ma hierarchię: (1) live value/state jako element primary, największy i o najwyższym kontraście; (2) `label` jako secondary; (3) ikona jako tertiary. Ikona wspiera skanowanie, ale nie konkuruje rozmiarem z wartością. Opcjonalny secondary status/jednostka pozostaje metadata.

Wyjątki:

- `folder`: label jest primary, ikona i chevron tertiary, liczba dzieci metadata;
- `action`: czasownik + obiekt jest primary, ikona secondary, wynik ostatniej akcji metadata;
- `clock`: czas jest primary Display, data secondary, ikona nie jest renderowana;
- `camera`: snapshot jest primary, label secondary, status odświeżenia metadata;
- `cover`: wizualizacja i pozycja są wspólnym primary, label secondary.

Nazwa pochodzi z `label`, a `short_label` jest kontrolowanym fallbackiem. `entity_id` nie jest pokazywane na panelu; Studio pokazuje je jako metadane.

Każdy interaktywny kafel ma dostępnościową nazwę: „{label}, {stan}, {akcja po dotknięciu}”. Read-only: „{label}, {wartość}”.

**Current:** Studio zapisuje niewersjonowane pola `showIcon`, `showTitle` i `showSubtitle`, które Android ignoruje.

**Locked target — prezentacja kafla:** każdy typ kafla korzysta z oficjalnego obiektu `presentation`:

```json
{
  "presentation": {
    "show_icon": true,
    "show_label": true,
    "show_value": true,
    "show_secondary": true,
    "background": "surface",
    "border": "default",
    "content_alignment": "center"
  }
}
```

| Pole | Wartości | Default |
|---|---|---|
| `show_icon` | boolean | `true` |
| `show_label` | boolean | `true` |
| `show_value` | boolean | `true` dla encji, `false` dla action/folder/popup |
| `show_secondary` | boolean | `true` |
| `background` | `surface`, `transparent` | `surface` |
| `border` | `default`, `none` | `default` |
| `content_alignment` | `start`, `center`, `end` | `center` |

Znaczenie kanałów i defaulty per kind (`I/L/V/S` = icon/label/value/secondary):

| Kind | Icon | Label | Value | Secondary | Default I/L/V/S | Gwarantowany kanał |
|---|---|---|---|---|---|---|
| `clock` | ignorowany | opcjonalny opis zegara | czas | data | `false/false/true/true` | value |
| `entity` | ikona domeny | label kafla | sformatowany stan lub fallback | jednostka/metadata | `true/true/true/true` | icon, label lub value |
| `category` | ikona kategorii | nazwa | opcjonalne summary | liczba dzieci/metadata | `true/true/true/true` | icon lub label; summary nie jest gwarantowane |
| `action` | ikona akcji | czasownik + obiekt | nieużywane | wynik ostatniej akcji | `true/true/false/false` | icon lub label |
| `cover` | ikona domeny | label kafla | wizualizacja pozycji albo tekst stanu/fallback | procent/kierunek | `true/true/true/true` | icon, label lub value |
| `camera` | fallback ikony przy braku obrazu | label kafla | snapshot albo placeholder | status odświeżenia | `true/true/true/true` | value; snapshot zawsze ma placeholder |
| `folder` | ikona folderu | nazwa folderu | nieużywane | liczba dzieci | `true/true/false/true` | icon lub label |
| `popup` | ikona popupu | nazwa popupu | nieużywane | opcjonalny opis | `true/true/false/false` | icon lub label |
| `text` | opcjonalna ikona | opcjonalny label | `content` | opcjonalna statyczna metadata | `false/false/true/false` | value; content jest wymagany |
| `spacer` | nie dotyczy | nie dotyczy | nie dotyczy | nie dotyczy | brak `presentation` | nieinteraktywny |

- Flagi sterują wyłącznie warstwą wizualną; Compose semantics nadal zawiera nazwę, stan i akcję.
- Dla `text` pole `show_value` steruje widocznością `content`; nie istnieje osobna, domyślna interpretacja Markdown poza tym kanałem.
- Ikona-only, tekst-only, value-only oraz transparentny kafel bez ramki są poprawnymi wariantami.
- Interaktywny kafel musi mieć co najmniej jeden gwarantowany, wypełniony kanał wizualny: niepusty label przy `show_label`, poprawną ikonę przy `show_icon`, stan/fallback przy `show_value` albo statycznie skonfigurowaną treść secondary przy `show_secondary`. Opcjonalna metadata runtime nie liczy się jako gwarantowany kanał. Studio blokuje secondary-only bez statycznej treści oraz każdą konfigurację, która po utracie opcjonalnych danych tworzyłaby niewidzialny target, i proponuje `spacer`.
- `folder`, `popup`, `category` i destrukcyjny `action` muszą zachować co najmniej ikonę albo label. Sama wartość bez kontekstu nie wystarcza.
- `spacer` nie przyjmuje `presentation`. Renderer zawsze rozwiązuje go do wszystkich kanałów ukrytych, `background: transparent`, `border: none`; Studio i Android odrzucają jawny, niezgodny obiekt presentation dla spacer.

### 6.3 Kontrakty typów

| Typ | Dozwolone size | Anatomia i stan | Tap | Hold | Fallback |
|---|---|---|---|---|---|
| `clock` | large | czas Display, data Body; style `classic`, `compact`, `date_top` | brak lub jawna nawigacja do zegara | brak | zły styl → `classic`; brak czasu → `--:--` |
| `entity` | small, large | ikona domeny, label, sformatowana wartość/stan, jednostka | domenowy safe default albo jawne `tap_action` | jawne `hold_action`; domyślnie native detail | brak encji → missing; nieznana domena → read-only raw state |
| `category` | action, large | ikona, nazwa kategorii, opcjonalne `summary` typu „3 włączone” | otwiera `panel_id` | opcjonalna jawna akcja zbiorcza z confirmation | bez panelu: legacy może użyć dotychczasowego entity tap; nowy zapis jest błędem |
| `action` | action, small, large | ikona, czasownik + obiekt; opcjonalny status ostatniego wyniku | wykonuje jawne `tap_action` | opcjonalna alternatywa | bez akcji: disabled „Brak akcji” |
| `cover` | small, large; large wymagane dla drag | wizualizacja pozycji, label, procent/stan | large: obszar poza suwakiem otwiera detail; small: jawny toggle/open-close | stop lub detail, jawnie skonfigurowane | brak pozycji → przyciski Otwórz/Stop/Zamknij; brak encji → missing |
| `camera` | large | snapshot 16:9, label, status odświeżania; bez atrap „LIVE” | natywny fullscreen | lista kamer lub jawna akcja | brak obrazu → ikona kamery + „Podgląd niedostępny”; brak kamer → „Brak kamer” / „Dodaj encję camera w Hapanels Studio.” |
| `folder` | action, large | ikona folderu, label, opcjonalna liczba dzieci, chevron | push do `panel_id` | brak | brak panelu → błąd bez nawigacji |
| `popup` | action, large | ikona, label, glyph popup | modal z `panel_id` | brak | brak panelu → błąd bez pustego modala |
| `text` | small, action, large | `content` jako tekst wieloliniowy lub bezpieczny Markdown; `show_value` steruje content, presentation tłem, ramką i alignment | opcjonalna jawna nawigacja lub akcja; domyślnie read-only | opcjonalna jawna akcja | legacy pusty content → „Brak treści” / „Dodaj tekst w Hapanels Studio.” |
| `spacer` | small, action, large | brak treści, tła i ramki | brak | brak | zawsze nieinteraktywny |

Zasady dodatkowe:

- Clock, folder i popup są kaflami panelowymi; nie wymagają `entity_id`.
- Camera nie wykonuje `camera.turn_on/off` przez zwykły tap.
- Cover drag wysyła wartość dopiero po puszczeniu; w trakcie pokazuje preview procentu.
- Action label zaczyna się czasownikiem: „Włącz alarm”, „Zgaś światła”, „Uruchom scenę”. Nie „Alarm settings” ani samo „Akcja”.
- Runtime nie dopełnia layoutu atrapami `Brak kafla`.
- `text` obsługuje bezpieczny podzbiór Markdown: akapity, line breaks, nagłówki, bold, italic i listy. Linki, HTML, obrazy z URL, skrypty, style inline i template wykonujący usługi są niedozwolone. Adres URL pozostaje zwykłym tekstem. Dzięki temu kafel ma najwyżej jeden target tap i jeden target hold; Markdown nie tworzy konkurencyjnych hit regions.
- Studio blokuje zapis `text` z pustym lub blank `content`. Runtime fallback dla legacy config brzmi „Brak treści” — „Dodaj tekst w Hapanels Studio.” i nie wykonuje przypisanej akcji.
- `spacer` istnieje wyłącznie do świadomego komponowania układu. Nie jest ogłaszany przez accessibility i nie może mieć `entity_id`, `tap_action` ani `hold_action`.

## 7. Macierz renderowania domen Home Assistant

Wartość główna nigdy nie łączy surowego stanu i jednostki bez formatowania. Liczby używają ustawionej precyzji, temperatura ustawienia jednostki użytkownika, opcje zachowują pisownię HA.

**Current:** `alarm_control_panel` nie występuje w bieżącym `Domain`; obecny Android nie może traktować go jako obsługiwanej encji Panel Grid.

**Locked target:** dodać jawne wsparcie `alarm_control_panel` zgodne z poniższym wierszem. Do czasu wdrożenia Studio oznacza domenę jako niewspieraną i blokuje zapis takiego kafla; nie mapuje jej do zwykłego switcha.

| Domena | Główna prezentacja | Secondary | Domyślny tap | Hold/detail | Uwagi bezpieczeństwa |
|---|---|---|---|---|---|
| `light` | Wł./Wył.; przy large jasność % | kolor/temperatura tylko gdy wspierane | toggle | native detail | unavailable blokuje toggle |
| `switch`, `input_boolean` | Wł./Wył. | opcjonalny area | toggle | native detail | unsafe switch wymaga confirmation z config |
| `sensor` | raw value + unit | device class/area | brak | native detail | read-only |
| `binary_sensor` | label zależny od device_class: Otwarte/Zamknięte, Ruch/Spokój itd. | last changed | brak | native detail | nie używać ogólnego Wł./Wył., gdy device_class znane |
| `climate` | current temp + target temp | HVAC mode | otwiera native control popup | detail | tap nie wyłącza ogrzewania bez jawnej akcji |
| `cover` | pozycja % lub Otwarta/Zamknięta/Ruch | kierunek/stan | control/detail zależnie od rozmiaru | stop/detail | device_class `door`, `garage`, `gate`: każdy ruch poza Stop domyślnie z confirmation; Studio pozwala jawnie wyłączyć confirmation dla konkretnego kafla |
| `lock` | Zablokowany/Odblokowany | `changed_by`, jeśli dostępne | zablokuj bez confirm; odblokuj z confirm | detail/PIN | nigdy automatyczny toggle bez rozróżnienia kierunku |
| `alarm_control_panel` | Rozbrojony/Uzbrojony dom/Uzbrojony poza domem/Uzbrojony noc/Alarm | pending/triggered jako wyraźny status | otwiera native control popup; brak toggle całego kafla | detail | uzbrojenie wybiera jawny tryb; rozbrojenie zawsze wymaga confirmation, a wymagany kod jest pobierany w kontrolowanym formularzu i nie trafia do config |
| `fan` | Wł./Wył. + speed % | preset/mode, jeśli dostępne | toggle | detail | speed tylko gdy wspierane |
| `media_player` | playing/paused/off + tytuł | artysta, volume | play/pause tylko na jawnej kontrolce; tap kafla otwiera detail | detail | przyciski wg supported_features |
| `select`, `input_select` | bieżąca opcja | label | otwiera picker | detail | brak cyklu opcji jednym przypadkowym tapem |
| `number`, `input_number` | wartość + unit | min–max | otwiera native slider/stepper | detail | snap do `step`; brak update_entity jako widocznej akcji |
| `button`, `input_button` | czasownik z label | opcjonalnie last triggered | `press` | detail | confirmation, gdy config oznacza unsafe |
| `scene` | nazwa sceny | „Scena” | aktywuj | detail | opcjonalne confirmation |
| `script` | nazwa + Uruchomione/Gotowe, gdy wiarygodne | last changed | uruchom | detail | nie udawać toggle; parametrów wymagających input nie uruchamiać bez formularza |
| `camera` | snapshot + dostępność | last refresh | fullscreen stream | lista/detail | kontrola prywatności niezależna od podglądu |
| `person`, `device_tracker` | W domu/Poza domem/Nieznane | strefa lub last seen | native detail | brak | read-only; zdjęcie opcjonalne, inicjały fallback |
| `timer` | pozostały `HH:MM:SS` + active/paused/idle | nazwa | active→pause, paused→start, idle→native timer popup | cancel z confirm | countdown aktualizowany lokalnie między eventami, bez fałszywego HA state |

Jeżeli domena jest obsługiwana przez Cards, formatter stanu i mapa bezpiecznych usług są współdzielone logicznie z Panel Grid. Kompozycja wizualna nie musi być wspólnym composable.

**Locked target — kamera:** widoczny kafel odświeża snapshot co 8 s tylko w foreground. Fullscreen używa ciągłego streamu udostępnionego przez encję `camera` w Home Assistant; Frigate/NVR dostarcza stream przez swoją integrację HA, bez vendor-specific URL w dashboard config. Jeśli encja nie udostępnia streamu albo stream nie wystartuje, fullscreen przechodzi do snapshot fallback co 4 s i pokazuje etykietę „Tryb poklatkowy”. Polling zatrzymuje się natychmiast po ukryciu kafla, zamknięciu fullscreen lub przejściu aplikacji w background.

## 8. Model przycisków i akcji

### 8.1 Semantyka

**Current:** Panel Grid mapuje większość tapów przez `ServiceCall.tapAction(entity_id, isOn)`. Model kafla nie deklaruje pełnego action schema.

**Current:** `confirmation` nie istnieje w `HapanelsDashboardConfig.kt` ani w bieżącym patch schema. Poniższy kształt jest wymaganiem docelowym, nie opisem funkcji już wdrożonej.

**Locked target:** każdy kafel może deklarować:

- `tap_action`
- `hold_action`
- opcjonalne `confirmation`

Wymagane typy action schema:

| Typ | Wymagane dane | Zachowanie |
|---|---|---|
| `none` | brak | brak gestu i press state |
| `more_info` | `entity_id` | natywny popup/detail Hapanels |
| `entity_default` | `entity_id` | bezpieczna akcja domenowa z macierzy |
| `call_service` | `domain`, `service`, jawny `target`, opcjonalne `data` | wywołanie HA; payload walidowany jako JSON |
| `navigate` | `destination` albo `panel_id` | tylko natywne trasy Hapanels/panele |
| `local_panel` | identyfikator istniejącej akcji HAL | lokalna akcja panelu, tylko gdy capability dostępne |

Nie wymagać osobnego schematu MQTT w UI tile; MQTT pozostaje transportem konfiguracji, nie domyślnym targetem przycisku ekranowego.

Docelowy JSON confirmation jest częścią konkretnego `tap_action` albo `hold_action`:

```json
{
  "tap_action": {
    "type": "entity_default",
    "entity_id": "lock.front_door",
    "confirmation": {
      "required": true,
      "kind": "unlock"
    }
  }
}
```

Kształt i defaulty:

| Pole | Typ | Default | Kontrakt |
|---|---|---|---|
| `required` | boolean | `true`, gdy obiekt `confirmation` istnieje | `false` usuwa dialog; dozwolone jako jawny override tylko dla cover safety inferowanego z device_class |
| `kind` | enum | wymagane | `unlock`, `cover_move`, `delete_tile`, `delete_panel`, `clear_tray`, `disarm_alarm`, `custom` |
| `title` | string? | tekst generowany z `kind` i label targetu | opcjonalny override; musi nazywać skutek i target |
| `body` | string? | brak | dodatkowa konsekwencja, nie powtórzenie tytułu |
| `negative_label` | string? | lokalizowany tekst z `kind` | dla `custom` wymagane; musi nazywać zachowywany target/skutek |
| `positive_label` | string? | lokalizowany tekst z `kind` | dla `custom` wymagane; musi zawierać czasownik i target noun |

Brak całego `confirmation` oznacza brak dialogu wyłącznie dla safe action. Validator dodaje błąd, nie cichy default, gdy bezwzględnie unsafe action nie ma `confirmation.required: true` i obsługiwanego `kind`.

### 8.2 Tap, hold i confirmation

- Tap wykonuje dokładnie `tap_action`.
- Hold po 500 ms wykonuje wyłącznie `hold_action`; release nie wykonuje tap.
- Widoczny glyph `⋯` pojawia się, gdy istnieje hold action.
- Confirmation zawiera nazwę skutku i target, np. „Odblokować zamek „Zamek wejściowy”?”. Obie widoczne CTA są kontekstowe i nazywają skutek lub zachowywany target; etykiety ogólne są niedozwolone.
- Kanoniczne pary CTA:
  - odblokowanie: „Nie odblokowuj zamka” / „Odblokuj zamek”;
  - usunięcie kafla: „Zachowaj kafel” / „Usuń kafel”;
  - usunięcie panelu: „Zachowaj panel” / „Usuń panel”;
  - czyszczenie zasobnika: „Zachowaj zawartość” / „Wyczyść zasobnik”;
  - rozbrojenie alarmu: „Nie rozbrajaj alarmu” / „Rozbrój alarm”.
- Escape/Back uruchamia negatywną akcję właściwą dla kontekstu i zamyka dialog; widoczny negatywny przycisk nadal używa pełnej etykiety z powyższej listy.
- Confirmation jest bezwzględnie wymagane dla unlock, alarm disarm, delete/reset i service oznaczonego `unsafe`; Studio nie pozwala go wyłączyć.
- Cover z device_class `door`, `garage` albo `gate` domyślnie wymaga `confirmation.kind: cover_move` dla Open/Close/Set position; Stop działa natychmiast. Copy zależy od kierunku, np. „Nie otwieraj bramy” / „Otwórz bramę” albo „Nie zamykaj bramy” / „Zamknij bramę”.
- Użytkownik może wyłączyć confirmation per kafel wyłącznie dla safety wywnioskowanego z cover device_class. Studio pokazuje trwałe ostrzeżenie „Sterowanie bez potwierdzenia” i zapisuje `{ "required": false, "kind": "cover_move" }`; brak pola nadal oznacza bezpieczny default z dialogiem.
- Precedence jest bezwzględne: jawne `unsafe` na service/action, unlock, disarm, delete i reset zawsze wygrywa nad cover exemption. Validator odrzuca `required: false` dla tych akcji, nawet jeśli target jest także cover.
- Niedostępny target: nie wysyłać usługi; banner „Encja jest niedostępna. Sprawdź Home Assistant.”
- Brak połączenia HA: akcja HA disabled; lokalna akcja HAL może działać i musi być opisana jako lokalna.
- Pending nie blokuje całego panelu; blokuje ponowne wysłanie tej samej akcji przez 800 ms albo do odpowiedzi.

### 8.3 Kafle techniczne panelu

**Locked target:** kafel techniczny nie jest osobnym `kind`. Używa `kind: action` oraz jawnego `tap_action` typu `navigate` albo `local_panel`. Dzięki temu renderer, layout, dostępność i presentation pozostają wspólne ze zwykłymi akcjami.

Pierwszy katalog Studio:

| Etykieta | `tap_action` | Zachowanie |
|---|---|---|
| Otwórz ustawienia | `navigate`, `destination: settings` | root ustawień Hapanels |
| Otwórz wygląd | `navigate`, `destination: settings/appearance` | wygląd panelu |
| Otwórz zachowanie | `navigate`, `destination: settings/behaviour` | zachowanie panelu |
| Otwórz integracje | `navigate`, `destination: settings/integrations` | integracje panelu |
| Otwórz diagnostykę | `navigate`, `destination: panel_diagnostics` | lokalna diagnostyka |
| Włącz AOD | `local_panel`, `action: screen.aod_now` | natychmiast pokazuje skonfigurowany AOD, także gdy automatyczne wejście po bezczynności jest wyłączone |
| Połącz HA ponownie | `local_panel`, `action: connection.reconnect_home_assistant` | zrywa bieżącą próbę/sesję i rozpoczyna nowe połączenie HA |

```json
{
  "kind": "action",
  "label": "Otwórz ustawienia",
  "icon": "mdi:cog",
  "tap_action": {
    "type": "navigate",
    "destination": "settings"
  }
}
```

- Android wykonuje wyłącznie whitelistowane destinations i identyfikatory `local_panel`; nie interpretuje URL, intentu ani dowolnej nazwy metody.
- Nieznany destination/action nie wykonuje fallbacku do HA i nie uruchamia akcji legacy.
- Studio dodaje pozycje jako gotowe presety, ale zapisuje zwykły action schema bez prywatnego pola „technical”.
- Przyszłe akcje zależne od sprzętu, np. relay lub latarka, pojawiają się dopiero po publikacji odpowiedniej capability przez tablet.
- Restart aplikacji, reboot urządzenia, reset i otwarcie Android Settings są poza pierwszym katalogiem; wymagają osobnego modelu confirmation i polityki kiosk/admin.

### 8.4 Copywriting

| Element | Tekst kanoniczny |
|---|---|
| Główna CTA Studio | „Zapisz układ” |
| Dodanie | „Dodaj kafel” / „Dodaj panel” |
| Pusty root | „Brak kafli” — „Dodaj pierwszy kafel w Hapanels Studio.” |
| Missing entity | „Nie znaleziono encji” — „Wybierz inną encję w Hapanels Studio.” |
| Pusty legacy text | „Brak treści” — „Dodaj tekst w Hapanels Studio.” |
| Błąd zapisu | „Nie zapisano zmian. Sprawdź połączenie i spróbuj ponownie.” |
| Konflikt | „Konfiguracja zmieniła się na tablecie. Porównaj zmiany przed zapisem.” |
| Usunięcie kafla | „Usunąć kafel „{label}”?” — „Tej zmiany nie zapiszesz bez ponownego dodania kafla.” |
| Usunięcie panelu | „Usunąć panel „{title}” i jego kafle?” |
| Puste kamery | „Brak kamer” — „Dodaj encję camera w Hapanels Studio.” — opcjonalnie „Dodaj kamerę” tylko z działającą trasą do edytora |
| CTA usunięcia kafla | „Zachowaj kafel” / „Usuń kafel” |
| CTA usunięcia panelu | „Zachowaj panel” / „Usuń panel” |
| CTA czyszczenia zasobnika | „Zachowaj zawartość” / „Wyczyść zasobnik” |

## 9. Cards a Panel Grid

**Current:** Cards to pionowe decki encji z zakładkami, gestami pagera, wheel input, `EntityCard` i per-card `EntityOverride`. Panel Grid ma osobny config i renderer.

**Locked target — wspólne:**

- połączenie i obserwacja encji;
- formatter wartości, jednostek, device_class i unavailable;
- bezpieczne mapowanie domen do usług;
- ikony MDI, semantyczne role kolorów, lokalizacja;
- native detail/picker dla climate, select, number, media i innych złożonych domen.

**Locked target — odrębne:**

- Cards zachowuje pager, wheel, strony ulubionych, duży readout i `EntityOverride`;
- Panel Grid zachowuje przestrzenny dashboard, foldery, popupy, layout JSON i Studio;
- `EntityOverride` nie nadpisuje kafla Panel Grid bez jawnej migracji/pola wspólnego;
- ustawienie startowe nadal wybiera wyłącznie `GRID` albo `CARDS`.

**Deferred:** scalenie Cards i Panel Grid w jeden uniwersalny layout lub zastąpienie obu Lovelace.

## 10. Always On Display

### 10.1 Tryby

| Tryb | Dozwolona zawartość |
|---|---|
| `minimal_clock` | zegar, data, opcjonalnie jeden neutralny status |
| `status_strip` | stały zegar + maks. 4 read-only statusy z `always_on_display.tiles`: person/device_tracker, binary_sensor, sensor lub timer |
| `grid` | maks. 6 kafli: clock, sensor, binary_sensor, person/device_tracker, timer |

- Tło domyślnie `#000000`; jasność 1–10%, wartość z config ograniczona do 1–20% w Studio.
- AOD jest read-only. Pierwszy tap/gest wybudza panel i nie wykonuje akcji kafla.
- Camera, cover control, lock, button, scene, script, media artwork i animacje są niedozwolone.
- Brak danych nie świeci danger stale; pokazuje muted „—”.
- Minimal clock respektuje `clock_style`; style nie zmieniają semantyki daty/czasu.
- Długi tekst i stany przewijane są zabronione. Brak marquee.
- Opcjonalne przesunięcie całej zawartości o 2–4 px co kilka minut jest dozwolone jako ochrona panelu; nie może zmieniać layoutu.
- `status_strip` ma jedno źródło konfiguracji: posortowane `always_on_display.tiles`. Zegar jest stałym elementem layoutu i nie zajmuje slotu statusu. `people` nie jest drugim źródłem, a legacy `entity_ids` jest migrowane do read-only AOD tiles przy pierwszym zapisie.

Migracja legacy `entity_ids`:

- config v1 jest renderowany zgodnościowo przez utworzenie kafli w pamięci; zapis Studio/Android podnosi wersję do 2 i usuwa `entity_ids` dopiero po udanym atomowym zapisie tiles;
- każde `entity_id` tworzy `kind: entity`, `size: small`, ID `aod_` + entity_id z kropkami i niedozwolonymi znakami zastąpionymi `_`; kolizje dostają deterministyczny suffix `_2`, `_3` w kolejności wejściowej;
- label bierze friendly name z HA, a bez niego title-cased object_id; icon bierze ikonę HA, a bez niej domenowy fallback;
- istniejące `tiles` zachowują kolejność i pierwszeństwo. Encja już obecna w tiles nie jest dodawana drugi raz. Nowe kafle dostają order kolejno po najwyższym istniejącym order;
- migrator nie usuwa nadmiarowych elementów. Runtime pokazuje pierwsze 4 statusy dla `status_strip` albo pierwsze 6 kafli dla `grid` i zgłasza ostrzeżenie. Studio umieszcza nadmiar w zasobniku AOD i blokuje kolejny zapis do czasu zejścia do limitu;
- po zapisaniu config v2 renderer ignoruje ewentualne pozostawione extension `entity_ids`; tiles są jedynym źródłem.

**Current:** model obsługuje `minimal_clock`, `status_strip`, `grid`, style zegara, timeout, jasność, fade, tło, entity IDs i tiles.

**Deferred:** zdjęcia, wideo, slideshow, Lovelace overlays i interaktywne AOD.

## 11. Hapanels Studio i parity

### 11.1 Zasada parity

- Studio odczytuje capabilities/supported schema version tabletu przed pokazaniem edytowalnego pola.
- Pole nieobsługiwane przez tablet jest ukryte lub disabled z opisem wersji; nigdy zapisywane „na przyszłość” bez zgody użytkownika.
- Android i Studio używają identycznych enumów, defaultów i zakresów.
- Compose jest wizualną referencją. Preview nie implementuje własnej semantyki tap/hold.

### 11.2 Tolerancja podglądu

Dla fixture 1280 × 800, content 1280 × 752:

- granice kafla i gap: różnica maks. 4 px;
- szerokość/wysokość kafla: maks. 2%;
- kolejność, span, wrapping i wybór `short_label`: identyczne;
- kolory: te same wartości tokenów; browserowe rasteryzowanie fontu może się różnić;
- ikona i stan unavailable/missing: identyczna semantyka;
- popup dimensions: maks. 2% różnicy.

`preview.html` musi używać content 1280 × 752, nie traktować całych 800 px jako powierzchni siatki.

### 11.3 Edytowanie

- Draft jest lokalny do kliknięcia „Zapisz układ”. Zmiana zakładki nie traci draftu; opuszczenie z niezapisanymi zmianami pyta o odrzucenie.
- Drag/resize ma ghost, snap do grid, keyboard alternative i widoczny invalid state.
- Keyboard: strzałki przesuwają zaznaczony kafel; Shift+strzałki zmieniają span; Delete prosi o confirmation.
- Zasobnik nie usuwa kafla z config do zapisu. „Wyczyść zasobnik” jest destrukcyjne i wymaga confirmation.
- Zmniejszenie siatki przenosi wypadające kafle do zasobnika dopiero po zatwierdzeniu.
- `id`, `panel_id`, pozycje i akcje walidowane przed wysłaniem.
- Konflikt rewizji pokazuje listę pól/kafli zmienionych po obu stronach. Użytkownik wybiera per kafel/pole albo całą wersję; „Studio wins” nie zmienia potajemnie base revision.
- Sukces zapisu pokazuje „Zapisano” i nową rewizję. Błąd zachowuje draft.

### 11.4 Walidacja minimalna

Blokować zapis, gdy:

- `version`, `dashboard_id`, `revision`, `layout` są brakujące lub niepoprawne;
- tile `id` nie jest unikalne;
- `panel_id` wskazuje brakujący panel, tworzy cykl albo przekracza głębokość 3;
- kind/size/action enum jest nieznane;
- kafel wychodzi poza grid lub nachodzi na inny;
- `entity_id` nie ma `domain.object_id` albo domena nie jest wspierana;
- action nie ma wymaganych pól, zawiera niepoprawny JSON lub unsafe action bez confirmation;
- `text.content` jest blank, spacer ma presentation/akcję/encję albo interaktywny kafel nie ma gwarantowanego, widocznego kanału treści;
- `accent: red` nie reprezentuje jawnej destrukcyjnej/unsafe action z wymaganym confirmation;
- AOD zawiera niedozwolony typ/interakcję albo więcej niż 6 kafli;
- preset/mode/style nie jest obsługiwany przez tablet;
- payload zawiera pole niewspierane przez Android schema.

Ostrzeżenie, ale nie blokada: encja nie istnieje w aktualnym `hass.states` (może być chwilowo niedostępna).

## 12. Dostępność

- Wszystkie akcje dostępne dotykiem i klawiaturą w Studio.
- Focus nie jest usuwany przez rerender; po zamknięciu dialogu wraca na przycisk otwierający.
- Dialog ma focus trap, poprawny tytuł i opis. Escape/Back wykonuje kontekstową negatywną akcję, np. „Zachowaj kafel”; widoczna etykieta musi nazywać zachowywany target.
- `aria-label`/Compose semantics opisują nazwę, stan i akcję.
- Minimum 48 dp/px dla targetów; ikona dekoracyjna jest ukryta przed czytnikiem.
- Dynamiczny stan ogłaszany jako polite; błędy zapisu jako assertive. Częste update sensoryczne nie spamują czytnika.
- Kolor nie jest jedynym nośnikiem. Focus, selected, danger i unavailable mają także obrys/tekst/ikonę.
- Tekst do 1.3× na panelu i 200% zoom w Studio nie powoduje utraty akcji ani poziomego scrolla strony.
- Reduced motion zgodnie z sekcją Motion.

## 13. Lokalizacja

- Polish jest językiem bazowym produktu; English ma pełne odpowiedniki dla każdej nowej etykiety i błędu.
- Nie tłumaczyć `entity_id`, nazw usług, enumów JSON ani wartości opcji pochodzących z HA.
- Datę i godzinę formatować z locale urządzenia oraz ustawieniem 12/24 h; nie hardcode `Locale("pl", "PL")` w rendererze docelowym.
- Liczby używają separatora locale; payload usług pozostaje locale-independent.
- Teksty nie są podmieniane przez przeszukiwanie DOM. Studio używa kluczy tłumaczeń dla każdej etykiety i atrybutu dostępności.
- Etykiety powinny mieścić się po polsku i angielsku; nie opierać layoutu na stałej szerokości słowa.

## 14. Migracja i kompatybilność

- Istniejący config version 1 musi nadal się załadować; oficjalny schema z `presentation`, `text`, `spacer`, panels i actions ma version 2.
- Nieznane pola zachować przy round-trip tylko wtedy, gdy są formalnie oznaczone jako extension i nie wpływają na UI; Studio nie oferuje ich edycji.
- Legacy flat `panel_id` migrować do jawnej listy paneli deterministycznie: jeden panel na unikalne `panel_id`, tytuł z pierwszego folder/popup label.
- Legacy `category` z `entity_id` i bez `panel_id` zachowuje dotychczasowy tap do czasu edycji; Studio oznacza „Legacy action” i wymaga wyboru docelowego zachowania przy zapisie.
- Legacy tile bez action dostaje domenowy `entity_default` tylko tam, gdzie macierz uznaje go za bezpieczny. Lock, camera, climate, select i number dostają `more_info`.
- Legacy config z `accent: red` nie może zostać automatycznie uznany za akcję niebezpieczną. Migrator klasyfikuje go następująco: jeżeli istniejąca jawna akcja już jest destrukcyjna/unsafe i ma wymagane confirmation, zachowuje `red`; w każdym innym przypadku mapuje wartość do neutralnego `white`, zapisuje wpis w raporcie migracji „Dekoracyjny czerwony akcent zmieniono na neutralny” i Studio pokazuje ten raport przed pierwszym kolejnym zapisem. Nie zmieniać semantyki akcji na podstawie samego koloru.
- Migracja presentation jest deterministyczna: `showIcon` → `show_icon`, `showTitle` → `show_label`, `showSubtitle` → `show_secondary`; brak legacy pola używa defaultu `true`. `show_value` przyjmuje default rodzaju kafla, a background/border/alignment defaulty z sekcji 6.2.
- Legacy `short_label` pozostaje wyłącznie fallbackiem label, nie statycznym subtitle. `showSubtitle: true` nie tworzy sztucznej treści; jeśli kafel był interaktywny i po migracji polegał wyłącznie na pustym secondary, Studio wymaga włączenia label/icon/value przed zapisem.
- Po udanym zapisie version 2 Studio usuwa trzy pola camelCase i zapisuje tylko `presentation`. Ponowny odczyt i zapis version 2 nie zmienia wartości presentation; Android nie ignoruje żadnego z tych pól.
- Unknown kind/enum nie crashuje Androida: config jest odrzucony atomowo, poprzednia poprawna rewizja pozostaje aktywna, Studio dostaje konkretny błąd.
- Patch zachowuje optimistic locking przez `base_revision`.

## 15. Kryteria akceptacji

### Panel Grid

- [ ] Referencyjny ekran renderuje treść 1280 × 752 w siatce 12 × 9, bez overlap i ucięcia.
- [ ] Każdy typ kafla spełnia anatomię, allowed size, tap/hold i fallback z sekcji 6.
- [ ] `presentation` pozwala uzyskać icon-only, text-only, value-only i transparent/no-border bez utraty semantics; niewidoczny interaktywny kafel jest odrzucany.
- [ ] `text` renderuje bezpieczny podzbiór Markdown, a `spacer` pozostaje niewidoczny i nieinteraktywny.
- [ ] Blank text jest blokowany w Studio; legacy blank text pokazuje „Brak treści” i drogę naprawy. Spacer z presentation lub akcją jest odrzucany.
- [ ] Interaktywny kafel pozostaje widoczny po utracie opcjonalnej secondary metadata; secondary-only bez statycznej treści nie przechodzi walidacji.
- [ ] Markdown nie tworzy aktywnych linków ani osobnych hit regions; dotknięcie text tile wykonuje wyłącznie skonfigurowany tile action.
- [ ] Wszystkie domeny z sekcji 7 mają czytelny stan, bezpieczny tap i unavailable treatment.
- [ ] Brak encji, pusta konfiguracja i brak panelu docelowego nie crashują i pokazują właściwy tekst.
- [ ] Pusta sekcja kamer pokazuje „Brak kamer” i „Dodaj encję camera w Hapanels Studio.”; CTA „Dodaj kamerę” istnieje tylko z działającą trasą do edytora.
- [ ] Foldery działają do głębokości 3, breadcrumbs/back zachowują stos, Android Back ma poprawny priorytet.
- [ ] Popup ma widoczny close 48 × 48 px w prawym górnym rogu z etykietą „Zamknij popup”; backdrop, close i Escape/Back nie wykonują akcji kafla pod spodem.
- [ ] Compact/standard reflow zachowuje kolejność i target minimum 48 dp.

### Akcje

- [ ] Tap i hold nie uruchamiają się razem.
- [ ] Unsafe action wymaga confirmation; unavailable/offline nie wysyła usługi.
- [ ] Cover door/garage/gate domyślnie potwierdza Open/Close/Set position, Stop nie pyta, jawny `cover_move required:false` wyłącza dialog, a override jest odrzucany dla explicit unsafe/unlock/disarm/delete/reset.
- [ ] Dialogi unlock/delete tile/delete panel/clear tray używają kanonicznych par CTA z sekcji 8.2; Escape/Back wybiera kontekstową akcję negatywną.
- [ ] `alarm_control_panel` nie jest zwykłym toggle; disarm zawsze wymaga confirmation.
- [ ] Pending i błąd zachowują ostatni potwierdzony stan.
- [ ] Camera tap otwiera native fullscreen, nie toggluje kamerę.
- [ ] Widoczny camera tile używa snapshotu co 8 s; fullscreen używa streamu HA, a brak streamu przechodzi do jawnie opisanego fallbacku 4 s.
- [ ] Presety techniczne Studio zapisują zwykły `kind: action`; whitelistowana nawigacja, AOD teraz i reconnect HA działają bez `entity_id`.
- [ ] Nieznany `navigate.destination` lub `local_panel.action` nie uruchamia żadnego fallbacku.

### AOD

- [ ] Pierwszy tap tylko wybudza.
- [ ] Niedozwolone interaktywne typy są odrzucane przez Studio i Android validator.
- [ ] Minimal/status/grid przestrzegają limitów i brightness.
- [ ] `status_strip` używa wyłącznie AOD tiles, ma stały zegar i maks. 4 statusy; legacy entity_ids migrują bez zmiany kolejności.
- [ ] Migracja AOD generuje stabilne ID/label/icon/order, nie duplikuje istniejącej encji, nie traci nadmiarowych kafli i po zapisie v2 używa wyłącznie tiles.

### Studio

- [ ] Preview spełnia tolerancje sekcji 11.2 dla 1280 × 800.
- [ ] Każde zapisane pole jest konsumowane przez Android lub zapis jest zablokowany.
- [ ] Invalid layout, cykl paneli, zła akcja i konflikt rewizji mają komunikat z drogą naprawy.
- [ ] Draft nie ginie po błędzie lub przypadkowej zmianie zakładki.
- [ ] Edytor działa bez poziomego scrolla przy 900 i 620 px oraz ma keyboard alternative.
- [ ] Studio ładuje i zapisuje `theme.mode: system` bez konwersji do light/dark; preview pokazuje „System · jasny/ciemny”, a Android reaguje na zmianę trybu systemowego bez przepisywania config.

### Dostępność i język

- [ ] Kontrast, focus, 48 dp, semantics i reduced motion spełniają sekcję 12.
- [ ] Wszystkie nowe teksty mają PL i EN; data/czas używają locale urządzenia.

## 16. Non-goals

- Lovelace/WebView jako renderer główny.
- Dowolne custom cards lub wykonywanie JS na tablecie.
- Nowy framework design system, shadcn, React lub biblioteka layoutu.
- Pixel-perfect kopiowanie Cards do Panel Grid.
- Nieskończone zagnieżdżanie folderów.
- Interaktywne sterowanie w AOD.
- Photo/video slideshow, 3D, rozbudowane gradienty i dekoracyjne animacje.
- Pełny kreator automatyzacji HA w Studio.
- Przepisanie transportu MQTT lub HAL w ramach refinements UI.

## 17. Rozstrzygnięte decyzje

| Obszar | Decyzja |
|---|---|
| Widoczność i powierzchnia kafla | Oficjalny `presentation` z flagami icon/label/value/secondary, transparentnym tłem, opcjonalną ramką i alignment. Dodać typy `text` i `spacer`. |
| AOD `status_strip` | Stały zegar + maks. 4 read-only elementy wyłącznie z `always_on_display.tiles`; legacy `entity_ids` migrowane do tiles. |
| Cover safety | Device class `door`, `garage`, `gate` domyślnie wymaga confirmation dla ruchu poza Stop. Studio pozwala wyłączyć je per kafel z trwałym ostrzeżeniem i jawnym zapisem override. |
| Camera runtime | Widoczny tile: snapshot co 8 s. Fullscreen: ciągły stream przez encję camera/HA; fallback snapshot co 4 s z etykietą „Tryb poklatkowy”. |
| Theme `system` | Dostępny w Studio i zapisywany jako `system`; tablet śledzi Android, preview Studio jawnie pokazuje aktualnie rozwiązany wariant. |
| Kafle techniczne | Zwykły `kind: action` z `navigate` lub `local_panel`; pierwszy katalog obejmuje ustawienia, diagnostykę, AOD teraz i reconnect HA. Akcje sprzętowe wymagają opublikowanej capability. |

Sekcja nie zawiera już decyzji blokujących rozpoczęcie etapów. Parametry streamu i koszt dekodowania trzeba zmierzyć na Blake podczas implementacji camera runtime, ale pomiar nie zmienia powyższego kontraktu UX.

## 18. Kolejność wdrożenia

1. **Schema parity gate:** wersja/capabilities, oficjalne pola, walidator, migracja flat panels i action schema.
2. **Wspólna semantyka encji:** formatter domen, unavailable/missing, safe defaults, native detail controls.
3. **Panel shell:** viewport 1280 × 752, tokeny, siatka wide, stany, accessibility.
4. **Taksonomia kafli:** entity/action/category/clock/text/spacer, następnie cover i camera.
5. **Nawigacja:** jawne panele, stos folderów, breadcrumbs, popup focus/back.
6. **Responsive:** standard/compact reflow i testy orientacji.
7. **AOD:** allowed-content validator, trzy layouty, wake-only interaction.
8. **Studio:** edytor oparty o ten sam schema, błędy, draft, konflikty i keyboard path.
9. **Preview parity:** fixture 1280 × 800/752 i screenshot comparison w tolerancjach.
10. **Cards regression:** potwierdzić brak zmian w pagerze, wheel i `EntityOverride`.

Każdy etap kończy się działającym pionowym wycinkiem w obecnym stacku. Nie zaczynać od abstrakcyjnej biblioteki komponentów.

## 19. Deferred

- Rozbudowane źródła AOD: zdjęcia, wideo, slideshow, status widgets spoza zdefiniowanej listy.
- BLE proxy i inne funkcje niezwiązane z UI panelu.
- Pełny per-field merge konfliktów wykraczający poza tiles/panels/actions, jeśli podstawowy merge jest już bezpieczny.
- Dowolne pluginy Studio i zewnętrzne registries komponentów.
- Zmiana package names lub architektury aplikacji.

## Appendix A — mapowanie wymagań na źródła

| Źródło | Użyte fakty |
|---|---|
| `docs/ROADMAP.md` (Milestones 8–10) | natywny Compose, config sync, Studio, foldery/panele, camera, GRID/CARDS, AOD |
| `docs/PRODUCTION_PLAN.md` (Milestones 8–9) | native-first, tablet/wall-panel, MQTT config, camera browser/fullscreen |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/feature/panelgrid/HapanelsDashboardConfig.kt` | current schema, kind/size/accent enums, AOD, patch/revision |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/feature/panelgrid/PanelGridMockupScreen.kt` | renderer geometry, breakpoint 820, popup, cover drag, live labels, current flat panel behavior |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/feature/panelgrid/HapanelsPanelTheme.kt` | token roles, presets, light/dark/system, default palette |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/core/prefs/EntityOverride.kt` | Cards-only customizations, long press, unavailable, text/accent controls |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/ui/components/EntityCard.kt` | current domain rendering, safe gesture gating, unavailable treatment |
| `app/src/main/kotlin/com/github/itskenny0/r1ha/feature/cardstack/CardStackScreen.kt` | Cards pager/tabs/wheel/chrome and shared behavior boundaries |
| `custom_components/hapanels/frontend/hapanels-panel.js` | Studio fields, editor, themes, AOD, preview, conflict and responsive CSS |
| `custom_components/hapanels/frontend/preview.html` | local fixture, current 1280 × 800 assumptions and mock HA state |

## Appendix B — źródła pomocnicze sprawdzone podczas kontraktu

- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/ha/ServiceCall.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/ha/EntityState.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/ha/EntityDomain.kt`
- `app/build.gradle.kts`

## Appendix C — checklist review

- [ ] Copywriting: jednoznaczne CTA, empty, error i destructive copy
- [ ] Visuals: viewport, geometria, anatomy i stany kompletne
- [ ] Color: role 60/30/10, kontrast i semantic colors
- [ ] Typography: 4 role, 2 weights
- [ ] Spacing: skala wielokrotności 4, wyjątki opisane
- [ ] Registry safety: nie dotyczy; brak shadcn/third-party registry
