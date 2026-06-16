# ⭐ Hapanels

<div class="hero">
  <p class="hero-lead">Natywny panel Home Assistant dla Shelly Wall Display i tabletów z Androidem.</p>
  <div class="badge-row">
    <span class="badge">Home Assistant</span>
    <span class="badge">Android</span>
    <span class="badge">Kotlin</span>
    <span class="badge">Status: active</span>
  </div>
</div>

## Dostępna aplikacja

<div class="app-cards">
  <a class="app-card" href="https://github.com/tw-zs/Hapanels/releases">
    <div class="icon"><img src="assets/hapanels_icon_no_text.svg" alt="Hapanels"></div>
    <h3>Hapanels</h3>
    <p>Natywny dashboard panelowy: Compose UI, Shelly relay/button/proximity, auto-jasność, AOD i MQTT discovery.</p>
  </a>
  <a class="app-card" href="development/">
    <div class="icon">📷</div>
    <h3>Kamery i dashboardy</h3>
    <p>Następny kierunek: natywny viewer kamer Home Assistant i dalsza synchronizacja dashboardów.</p>
  </a>
</div>

## Szybka instalacja

Pobierz najnowszy APK z GitHub Releases i zainstaluj go na panelu albo tablecie.

[Pobierz APK](https://github.com/tw-zs/Hapanels/releases){ .md-button .md-button--primary }
[Repozytorium](https://github.com/tw-zs/Hapanels){ .md-button }

1. Pobierz APK z sekcji Releases.
2. Zainstaluj aplikację na Shelly Wall Display albo tablecie Android.
3. Połącz z Home Assistant.
4. Włącz MQTT, jeśli chcesz discovery panelu w HA.

!!! warning "Zasada sprzętowa"
    Hapanels pokazuje tylko realne, zweryfikowane funkcje urządzenia. Nie wystawiamy fikcyjnych sensorów temperatury, wilgotności ani odległości.

## O projekcie

Hapanels nie udaje Lovelace w WebView. Home Assistant dostarcza encje, konfigurację i usługi, a aplikacja renderuje panel natywnie na Androidzie.

OAuth redirect dla Home Assistant: `r1ha://auth-callback`.
