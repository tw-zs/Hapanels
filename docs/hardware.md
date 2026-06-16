# Sprzęt panelu

Hapanels ma dwa tryby sprzętowe:

- **Shelly Wall Display** dla paneli Shelly.
- **Android tablet** jako bezpieczny fallback.

## Shelly

Aktualnie obsługiwane lub wykrywane funkcje:

- relay 1,
- fizyczne przyciski Shelly,
- jasność ekranu,
- czujnik światła,
- proximity presence jako binary sensor,
- AOD/screensaver i proximity wake.

## MQTT discovery

Panel publikuje encje i diagnostykę do Home Assistant przez MQTT discovery. Przykładowe komendy:

```text
hapanels/<device>/relay/<id>/set
hapanels/<device>/screen/brightness/set
hapanels/<device>/screen/auto_brightness/set
hapanels/<device>/dashboard/config/set
hapanels/<device>/dashboard/config/patch/set
```

## Bez fikcyjnych sensorów

Jeśli hardware nie daje wiarygodnej wartości, Hapanels jej nie wystawia. Proximity na Shelly Blake/XL jest traktowane jako obecność przy panelu, nie jako dokładne centymetry.
