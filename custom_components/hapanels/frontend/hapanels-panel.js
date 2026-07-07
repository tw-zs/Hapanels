const APP_URL = "https://github.com/tw-zs/Hapanels";
const STUDIO_FRONTEND_VERSION = "20260707-aod-switch";
const TILE_ACCENTS = ["orange", "red", "white"];
const TILE_KINDS = ["entity", "cover", "category", "action", "camera", "clock", "folder", "popup"];
const PANEL_TILE_KINDS = ["clock", "folder", "popup"];
const TILE_SIZES = ["large", "small", "action"];
const CLOCK_STYLES = ["classic", "compact", "date_top"];
const COVER_VISUALS = ["blind", "shade", "curtain", "gate"];
const COVER_DIRECTIONS = ["top", "bottom", "left", "right", "top_left", "top_right", "bottom_left", "bottom_right"];
const AOD_PRESETS = [
  { id: "night", name: "Nocny zegar", desc: "Ciemno, 1% jasności, sam zegar", aod: { enabled: true, layout: "minimal_clock", timeout_sec: 300, brightness_percent: 1, background: "#000000", grid_layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, gap: "small" } } },
  { id: "status", name: "Status dom", desc: "Cichy pasek statusu i osoby", aod: { enabled: true, layout: "status_strip", timeout_sec: 180, brightness_percent: 3, background: "#05070a", grid_layout: { type: "fixed_grid", columns_landscape: 4, columns_portrait: 2, gap: "small" } } },
  { id: "grid", name: "Kafle nocne", desc: "Siatka AOD dla kilku encji", aod: { enabled: true, layout: "grid", timeout_sec: 300, brightness_percent: 4, background: "#080b0f", grid_layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, gap: "medium" } } },
  { id: "off", name: "Wyłącz AOD", desc: "Bez wygaszacza ekranu", aod: { enabled: false, layout: "minimal_clock", timeout_sec: 300, brightness_percent: 3, background: "#000000", grid_layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, gap: "small" } } },
];
const AOD_CLOCK_STYLES = [
  { id: "default", name: "Domyślny", category: "Wygląd domyślny", desc: "Czytelny, spokojny zegar AOD.", treatment: "Lekki font, klasyczny układ godziny i daty." },
  { id: "fullscreen_bold", name: "Slim modern", category: "Wyglądy standardowe", desc: "Pełnoekranowy, smukły zegar z dużym oddechem.", treatment: "Cienkie cyfry i mała data pod spodem." },
  { id: "fullscreen_heavy", name: "Szeroki", category: "Wyglądy standardowe", desc: "Maksymalna godzina widoczna z daleka.", treatment: "Większe, optycznie pogrubione cyfry wypełniają ekran." },
  { id: "warsaw_zaklad", name: "Warszawski Zakład", category: "Polskie inspiracje", desc: "Miejski zegar z charakterem szyldu.", treatment: "Kondensowany font, ramka i techniczny klimat tablicy." },
  { id: "modern", name: "Nowoczesny", category: "Kolorowe abstrakcje", desc: "Czysty, minimalistyczny ekran nocny.", treatment: "Smukłe cyfry, szeroki oddech i miękki kontrast." },
  { id: "popart", name: "Popart", category: "Kolorowe abstrakcje", desc: "Kolorowy, graficzny akcent na AOD.", treatment: "Grube cyfry, mocny cień i plakatowe plamy koloru." },
];
const PANEL_THEME_PRESETS = [
  { id: "default", name: "Domyślny", category: "Wygląd domyślny", description: "Aktualny wygląd panelu Hapanels.", light: { bg: "#f8fafc", surface: "#ffffff", tile: "#e7eaf0", text: "#171a20", muted: "#666a73", accent: "#e99900", border: "#d4d8e0", hover: "#fff3d6", active: "#ffe4a3", selected: "#ffd980" }, dark: { bg: "#090d10", surface: "#23242d", tile: "#2e303a", text: "#ffffff", muted: "#888c96", accent: "#e99900", border: "#3a3d48", hover: "#30313a", active: "#3a3321", selected: "#4b3a16" } },
  { id: "light_breeze", name: "Light Breeze", category: "Wygląd monochromatyczny", description: "Chłodny, jasny błękit z lekkim kontrastem.", light: { bg: "#f4faff", surface: "#ffffff", tile: "#ddecf7", text: "#10202c", muted: "#5d7482", accent: "#3fa7d6", border: "#c2d7e6", hover: "#eaf6fd", active: "#d3edfa", selected: "#b8e1f4" }, dark: { bg: "#07141c", surface: "#132832", tile: "#203946", text: "#eaf8ff", muted: "#9db8c8", accent: "#51b7e8", border: "#315260", hover: "#1b3440", active: "#254c5d", selected: "#2f6177" } },
  { id: "desert_sun", name: "Desert Sun", category: "Wygląd monochromatyczny", description: "Ciepły piasek i bursztyn.", light: { bg: "#fff8ee", surface: "#fffcf7", tile: "#f1e0c8", text: "#2a1b06", muted: "#765f3f", accent: "#d99a21", border: "#d8c29d", hover: "#ffefd2", active: "#ffe1a6", selected: "#ffd27a" }, dark: { bg: "#1b1205", surface: "#2d2110", tile: "#41321a", text: "#fff3df", muted: "#d1b98f", accent: "#eaa12a", border: "#5d4725", hover: "#3a2b16", active: "#5a421d", selected: "#705222" } },
  { id: "forest_leaves", name: "Forest Leaves", category: "Wygląd monochromatyczny", description: "Zielenie lasu, spokojne i kontrastowe.", light: { bg: "#f4fbf2", surface: "#ffffff", tile: "#dcebd7", text: "#11210f", muted: "#5f7358", accent: "#5c9e46", border: "#c3d6bd", hover: "#eaf6e6", active: "#d9efd2", selected: "#c3e6b9" }, dark: { bg: "#081409", surface: "#172619", tile: "#263828", text: "#eff8ec", muted: "#a8bfa0", accent: "#7dbb63", border: "#3a5138", hover: "#203321", active: "#31502f", selected: "#3e6339" } },
  { id: "baltic_dawn", name: "Bałtyk o świcie", category: "Polskie inspiracje", description: "Morski błękit, mgła i różowy świt.", light: { bg: "#f3fafa", surface: "#ffffff", tile: "#d7e9ea", text: "#0f2326", muted: "#60777a", accent: "#e8918e", border: "#bfd6d8", hover: "#e6f4f5", active: "#cdecef", selected: "#ffdad8" }, dark: { bg: "#061416", surface: "#14282b", tile: "#233a3e", text: "#eaf8f9", muted: "#a5bcbf", accent: "#ffb4b0", border: "#365459", hover: "#1d3438", active: "#2b5157", selected: "#613735" } },
  { id: "bieszczady_sunset", name: "Bieszczadzkie zachody", category: "Polskie inspiracje", description: "Ciepły zachód nad połoninami.", light: { bg: "#fff7f0", surface: "#fffcf9", tile: "#f1ded2", text: "#2b170f", muted: "#765f55", accent: "#d06a3a", border: "#d9c1b5", hover: "#ffede3", active: "#ffd9c8", selected: "#ffc4a8" }, dark: { bg: "#1a0e0a", surface: "#2c1d17", tile: "#432e25", text: "#fff1ea", muted: "#d4b5a6", accent: "#ff8d57", border: "#60463a", hover: "#392820", active: "#5b3e31", selected: "#734e3c" } },
  { id: "masurian_nights", name: "Mazurskie noce", category: "Polskie inspiracje", description: "Granat jezior i nocne światło.", light: { bg: "#f5f8ff", surface: "#ffffff", tile: "#dde6f6", text: "#121d2f", muted: "#626f86", accent: "#7f96d6", border: "#c5d2e8", hover: "#ecf3ff", active: "#dcebff", selected: "#c7dbff" }, dark: { bg: "#070d1a", surface: "#171e31", tile: "#252e46", text: "#eff3ff", muted: "#aeb8d0", accent: "#8ea7f2", border: "#3b4663", hover: "#202840", active: "#334061", selected: "#405079" } },
  { id: "aurora_glass", name: "Aurora Glass", category: "Kolorowe abstrakcyjne", description: "Szklany gradient zorzy z chłodnym fioletem i cyjanem.", light: { bg: "#f7f8ff", surface: "#ffffff", tile: "#e7e4ff", text: "#151427", muted: "#686785", accent: "#20bfd6", border: "#d4d0f5", hover: "#f0eeff", active: "#e2ddff", selected: "#d7f6fb" }, dark: { bg: "#080817", surface: "#17162b", tile: "#252344", text: "#f4f1ff", muted: "#b7b1d9", accent: "#69e4f4", border: "#3d3a66", hover: "#201f3a", active: "#332f62", selected: "#214b5a" } },
  { id: "neon_noir", name: "Neon Noir", category: "Kolorowe abstrakcyjne", description: "Ciemny premium look z neonowym magenta i elektrycznym błękitem.", light: { bg: "#fff7fb", surface: "#ffffff", tile: "#ffddec", text: "#26111e", muted: "#795d70", accent: "#0078d4", border: "#e8bed5", hover: "#ffedf6", active: "#ffd9eb", selected: "#d7ecff" }, dark: { bg: "#09040d", surface: "#1b1022", tile: "#31183c", text: "#ffeef8", muted: "#d1a7c4", accent: "#58c7ff", border: "#53305f", hover: "#271430", active: "#421c54", selected: "#193f5c" } },
  { id: "velvet_spectrum", name: "Velvet Spectrum", category: "Kolorowe abstrakcyjne", description: "Miękki, luksusowy miks śliwki, złota i indygo.", light: { bg: "#fff8f4", surface: "#ffffff", tile: "#f0ddea", text: "#251524", muted: "#735c6d", accent: "#c18a2a", border: "#ddc2d5", hover: "#ffeff8", active: "#f2d4e8", selected: "#ffe0a8" }, dark: { bg: "#100913", surface: "#211426", tile: "#35203c", text: "#fff0fa", muted: "#c7a8c4", accent: "#e4b65c", border: "#56355f", hover: "#2d1934", active: "#4b2758", selected: "#60481f" } },
];

class HapanelsStudioPanel extends HTMLElement {
  connectedCallback() {
    this.attachShadow({ mode: "open" });
    this._panels = [];
    this._configs = {};
    this._pendingPatches = {};
    this._error = null;
    this._selectedDevice = null;
    this._activeTab = "tiles";
    this._settingsOpen = false;
    this._tabletPickerOpen = false;
    this._panelTilePicker = null;
    this._confirm = null;
    this._focusedTileId = null;
    this._tabletPickerFilter = "all";
    this._layoutDrafts = {};
    this._layoutContext = "main";
    this._layoutDrag = null;
    this._panelThemeError = null;
    this._aodClockStyleError = null;
    this._layoutGridCollapsed = localStorage.getItem("hapanels_layout_grid_collapsed") === "1";
    this._themeMode = localStorage.getItem("hapanels_studio_theme") || "auto";
    this._hiddenDevices = new Set(JSON.parse(localStorage.getItem("hapanels_hidden_devices") || "[]"));
    this._expandedTiles = new Set(JSON.parse(localStorage.getItem("hapanels_expanded_tiles") || "[]"));
    this._render();
    this._load();
  }

  set hass(hass) {
    const wasDark = this._isDarkTheme();
    this._hass = hass;
    if (!this._loaded) this._load();
    if (this._themeMode === "auto" && wasDark !== this._isDarkTheme()) this._render();
  }

  async _load() {
    if (!this._hass || this._loading) return;
    this._loading = true;
    try {
      const result = await this._hass.callWS({ type: "hapanels/list_panels" });
      this._panels = result.panels || [];
      await Promise.all(this._panels.map((panel) => this._loadConfig(panel.device)));
      if (this._selectedDevice && !this._panels.some((panel) => panel.device === this._selectedDevice)) {
        this._selectedDevice = null;
      }
      this._error = null;
      this._loaded = true;
    } catch (err) {
      this._error = err?.message || String(err);
    } finally {
      this._loading = false;
      this._render();
    }
  }

  async _loadConfig(device) {
    if (!device) return;
    const result = await this._hass.callWS({ type: "hapanels/get_dashboard_config", device });
    this._configs[device] = result.config || null;
    this._pendingPatches[device] = result.pending_patch || null;
  }

  async _saveTile(device, tileId, prefix, surface = "dashboard") {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    const label = this.shadowRoot.getElementById(`${prefix}-label`)?.value?.trim();
    const shortLabel = this.shadowRoot.getElementById(`${prefix}-short`)?.value?.trim();
    const entityId = this.shadowRoot.getElementById(`${prefix}-entity`)?.value?.trim();
    const panelId = this.shadowRoot.getElementById(`${prefix}-panel`)?.value?.trim();
    const kind = this.shadowRoot.getElementById(`${prefix}-kind`)?.value;
    const size = this.shadowRoot.getElementById(`${prefix}-size`)?.value;
    const icon = this.shadowRoot.getElementById(`${prefix}-icon`)?.value;
    const accent = this.shadowRoot.getElementById(`${prefix}-accent`)?.value;
    const order = Number(this.shadowRoot.getElementById(`${prefix}-order`)?.value);
    const col = Number(this.shadowRoot.getElementById(`${prefix}-col`)?.value);
    const row = Number(this.shadowRoot.getElementById(`${prefix}-row`)?.value);
    const colSpan = Number(this.shadowRoot.getElementById(`${prefix}-colSpan`)?.value);
    const rowSpan = Number(this.shadowRoot.getElementById(`${prefix}-rowSpan`)?.value);
    const clockStyle = this.shadowRoot.getElementById(`${prefix}-clockStyle`)?.value;
    const coverVisual = this.shadowRoot.getElementById(`${prefix}-coverVisual`)?.value;
    const coverDirection = this.shadowRoot.getElementById(`${prefix}-coverDirection`)?.value;
    if (!config || !Number.isFinite(revision) || !label) return;
    const tile = { id: tileId, label };
    if (kind) tile.kind = kind;
    if (size) tile.size = size;
    if (shortLabel !== undefined) tile.short_label = shortLabel;
    if (entityId !== undefined) tile.entity_id = entityId;
    if (panelId !== undefined) tile.panel_id = panelId;
    if (icon) tile.icon = icon;
    if (accent) tile.accent = accent;
    if (Number.isFinite(order)) tile.order = order;
    if (Number.isFinite(col)) tile.col = col;
    if (Number.isFinite(row)) tile.row = row;
    if (Number.isFinite(colSpan)) tile.colSpan = colSpan;
    if (Number.isFinite(rowSpan)) tile.rowSpan = rowSpan;
    if (clockStyle) tile.clock_style = clockStyle;
    if (coverVisual) tile.cover_visual = coverVisual;
    if (coverDirection) tile.cover_direction = coverDirection;
    await this._hass.callService("hapanels", "patch_dashboard_config", {
      device,
      patch: {
        base_revision: revision,
        updated_by: "homeassistant:hapanels_studio",
        surface,
        tile_updates: [tile],
      },
    });
    window.setTimeout(() => this._load(), 800);
  }

  async _setConfig(device, config) {
    if (!config) return;
    const next = structuredClone(config);
    next.revision = Number(next.revision || 0) + 1;
    next.updated_by = "homeassistant:hapanels_studio";
    await this._hass.callService("hapanels", "set_dashboard_config", { device, config: next });
    window.setTimeout(() => this._load(), 800);
  }

  async _addTile(device, surface = "dashboard", type = "ha", panelKind = "clock", panelId = "") {
    const config = this._configs[device];
    if (!config) return;
    const next = structuredClone(config);
    const target = surface === "aod" ? (next.always_on_display ||= {}) : next;
    target.tiles ||= [];
    const order = Math.max(-1, ...target.tiles.map((tile) => Number(tile.order) || 0)) + 1;
    const tile = type === "panel" ? this._newLayoutTile("panel", panelKind) : this._newLayoutTile("ha");
    target.tiles.push({ ...tile, panel_id: panelId || tile.panel_id, size: surface === "aod" ? "small" : tile.size, order });
    await this._setConfig(device, next);
  }

  async _saveAodSettings(device) {
    const config = this._configs[device];
    if (!config) return;
    const next = structuredClone(config);
    const aod = next.always_on_display ||= {};
    aod.enabled = !!this.shadowRoot.getElementById("aod-enabled")?.checked;
    aod.layout = this.shadowRoot.getElementById("aod-layout")?.value || aod.layout;
    aod.timeout_sec = Number(this.shadowRoot.getElementById("aod-timeout")?.value) || aod.timeout_sec;
    aod.brightness_percent = Number(this.shadowRoot.getElementById("aod-brightness")?.value) || aod.brightness_percent;
    aod.background = this.shadowRoot.getElementById("aod-background")?.value?.trim() || aod.background;
    aod.entity_ids = (this.shadowRoot.getElementById("aod-entities")?.value || "").split(",").map((value) => value.trim()).filter(Boolean);
    const grid = aod.grid_layout ||= {};
    grid.type = "fixed_grid";
    grid.columns_landscape = Number(this.shadowRoot.getElementById("aod-columns-landscape")?.value) || grid.columns_landscape;
    grid.columns_portrait = Number(this.shadowRoot.getElementById("aod-columns-portrait")?.value) || grid.columns_portrait;
    grid.gap = this.shadowRoot.getElementById("aod-gap")?.value || grid.gap;
    await this._setConfig(device, next);
  }

  async _applyAodPreset(device, presetId) {
    const config = this._configs[device];
    const preset = AOD_PRESETS.find((item) => item.id === presetId);
    if (!config || !preset) return;
    const next = structuredClone(config);
    next.always_on_display = { ...(next.always_on_display || {}), ...structuredClone(preset.aod) };
    await this._setConfig(device, next);
  }

  async _deleteTile(device, tileId, surface = "dashboard") {
    const config = this._configs[device];
    if (!config) return;
    const next = structuredClone(config);
    const target = surface === "aod" ? (next.always_on_display ||= {}) : next;
    target.tiles = (target.tiles || []).filter((tile) => tile.id !== tileId);
    await this._setConfig(device, next);
  }

  _askConfirm(title, message, action) {
    this._confirm = { title, message, action };
    this._render();
  }

  _closeConfirm() {
    this._confirm = null;
    this._render();
  }

  _confirmAction() {
    const action = this._confirm?.action;
    this._confirm = null;
    this._render();
    action?.();
  }

  _visiblePanels() {
    return (this._panels || []).filter((panel) => !this._hiddenDevices.has(panel.device));
  }

  _selectedPanel() {
    return this._visiblePanels().find((panel) => panel.device === this._selectedDevice) || null;
  }

  _panelLabel(panel) {
    return panel?.panel_name || this._configs?.[panel?.device]?.title || panel?.device || "panel";
  }

  _pickerPanels() {
    const all = this._panels || [];
    if (this._tabletPickerFilter === "hidden") return all.filter((panel) => this._hiddenDevices.has(panel.device));
    if (this._tabletPickerFilter === "active") return all.filter((panel) => !this._hiddenDevices.has(panel.device));
    return all;
  }

  _setThemeMode(mode) {
    this._themeMode = mode;
    localStorage.setItem("hapanels_studio_theme", mode);
    this._settingsOpen = false;
    this._render();
  }

  _hideDevice(device) {
    if (!device) return;
    this._hiddenDevices.add(device);
    localStorage.setItem("hapanels_hidden_devices", JSON.stringify([...this._hiddenDevices]));
    if (this._selectedDevice === device) this._selectedDevice = null;
    this._render();
  }

  _showAllDevices() {
    this._hiddenDevices.clear();
    localStorage.setItem("hapanels_hidden_devices", "[]");
    this._render();
  }

  _isDarkTheme() {
    if (this._themeMode === "dark") return true;
    if (this._themeMode === "light") return false;
    return this._hass?.themes?.darkMode ?? true;
  }

  _render() {
    if (!this.shadowRoot) return;
    const panels = this._visiblePanels();
    const selected = this._selectedPanel();
    const dark = this._isDarkTheme();
    this.shadowRoot.innerHTML = `
      <style>
        :host {
          --bg: ${dark ? "#080b0f" : "#f5f6f8"};
          --surface: ${dark ? "#111821" : "#ffffff"};
          --surface-2: ${dark ? "#151d28" : "#eef1f5"};
          --text: ${dark ? "#f5f3ee" : "#101419"};
          --muted: ${dark ? "#a7adb8" : "#596472"};
          --line: ${dark ? "rgba(255,255,255,.09)" : "rgba(0,0,0,.10)"};
          --accent: #ff7a1a;
          display: block;
          min-height: 100vh;
          background: var(--bg);
          color: var(--text);
          font-family: var(--paper-font-body1_-_font-family, Nunito, system-ui, sans-serif);
        }
        * { box-sizing: border-box; }
        .page { padding: 28px; max-width: 1180px; margin: 0 auto; }
        .hero { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; margin-bottom: 24px; }
        .brand { display: flex; align-items: center; gap: 12px; }
        .logo { width: 42px; height: 42px; border-radius: 14px; background: linear-gradient(135deg, #ff7a1a, #ffb35c); display: grid; place-items: center; color: #160902; box-shadow: 0 12px 30px rgba(255,122,26,.25); }
        .logo svg { width: 36px; height: 36px; }
        h1 { margin: 0; font-size: 34px; letter-spacing: -0.03em; }
        h2 { margin: 0; font-size: 24px; }
        .sub { color: var(--muted); margin-top: 8px; }
        .actions { display: flex; gap: 10px; align-items: center; }
        button { border: 0; border-radius: 14px; background: var(--accent); color: #1a0d03; padding: 11px 16px; font-weight: 850; cursor: pointer; }
        button.secondary { background: var(--surface-2); color: var(--text); border: 1px solid var(--line); }
        button.danger { background: rgba(255,83,56,.14); color: #ff725d; border: 1px solid rgba(255,83,56,.30); }
        .iconbtn { width: 44px; height: 44px; padding: 0; display: grid; place-items: center; font-size: 20px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
        .card { border: 1px solid var(--line); border-radius: 22px; background: var(--surface); padding: 18px; box-shadow: 0 18px 50px rgba(0,0,0,.16); }
        .panel-card { text-align: left; color: inherit; width: 100%; }
        .name { display: flex; justify-content: space-between; gap: 12px; font-size: 19px; font-weight: 850; }
        .panel-title { display: grid; gap: 3px; min-width: 0; }
        .panel-title small { color: var(--muted); font-size: 12px; font-weight: 800; overflow-wrap: anywhere; }
        .panel-pills { display: flex; gap: 6px; align-items: flex-start; flex-wrap: wrap; justify-content: flex-end; }
        .pill { border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 950; text-transform: uppercase; }
        .synced { background: rgba(68, 196, 119, .18); color: #48c97a; }
        .visible { background: rgba(79,156,255,.16); color: #7cb4ff; }
        .hidden { background: rgba(127,127,127,.12); color: var(--muted); }
        .conflict { background: rgba(255, 83, 56, .18); color: #ff725d; }
        .unknown, .invalid { background: rgba(127,127,127,.12); color: var(--muted); }
        dl { display: grid; grid-template-columns: 120px 1fr; gap: 8px 12px; margin: 18px 0 0; }
        dt { color: var(--muted); } dd { margin: 0; overflow-wrap: anywhere; }
        .empty { min-height: 52vh; display: grid; place-items: center; text-align: center; }
        .empty-box { max-width: 540px; border: 1px dashed var(--line); border-radius: 28px; padding: 42px; background: var(--surface); }
        .empty-box p { color: var(--muted); line-height: 1.55; }
        .tabs { display: flex; gap: 8px; margin: 20px 0 16px; flex-wrap: wrap; }
        .tab { background: var(--surface-2); color: var(--text); border: 1px solid var(--line); }
        .tab.active { background: var(--accent); color: #1a0d03; border-color: transparent; }
        .tiles { display: grid; gap: 14px; }
        .tile { overflow: hidden; border: 1px solid var(--line); border-radius: 18px; background: linear-gradient(145deg, var(--surface), var(--surface-2)); box-shadow: 0 18px 50px rgba(0,0,0,.13); }
        .tile.focused { border-color: var(--accent); box-shadow: 0 0 0 2px color-mix(in srgb, var(--accent) 55%, transparent), 0 18px 50px rgba(0,0,0,.20); }
        .tile-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 16px 18px; border-bottom: 1px solid var(--line); color: var(--muted); font-size: 12px; font-weight: 850; text-transform: uppercase; cursor: pointer; user-select: none; }
        .tile.collapsed .tile-head { border-bottom: 0; }
        .tile-expandable { overflow: hidden; max-height: 2400px; opacity: 1; transform: translateY(0); transition: max-height .26s cubic-bezier(.2,.8,.2,1), opacity .16s ease, transform .16s ease; will-change: max-height, opacity, transform; }
        .tile-expandable-inner { overflow: hidden; }
        .tile.collapsed .tile-expandable { max-height: 0; opacity: 0; transform: translateY(-8px); pointer-events: none; transition-duration: 300ms, 300ms, 300ms; }
        .tile-title { display: flex; align-items: center; gap: 10px; color: var(--text); }
        .tile-title button { padding: 0; width: 28px; height: 28px; display: grid; place-items: center; background: transparent; color: var(--muted); }
        .tile-toggle { padding: 0; width: 30px; height: 30px; display: grid; place-items: center; background: transparent; color: var(--muted); }
        .tile-toggle span { transition: transform .15s ease; }
        .tile.collapsed .tile-toggle span { transform: rotate(180deg); }
        .tile-summary { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
        .tile-body { display: grid; grid-template-columns: 360px minmax(0, 1fr); }
        .tile-preview-pane { padding: 18px; border-right: 1px solid var(--line); background: rgba(255,255,255,.02); }
        .tile-preview-box { min-height: 210px; margin: 18px auto; border: 1px solid var(--line); border-radius: 16px; display: grid; place-items: center; text-align: center; max-width: 250px; background: var(--bg); }
        .tile-preview-target { border-radius: 10px; cursor: default; transition: background .12s ease, box-shadow .12s ease, color .12s ease; }
        .tile-preview-target:hover { background: color-mix(in srgb, var(--tile-accent, currentColor) 12%, transparent); }
        .field, .icon-field { transition: background 800ms ease, box-shadow 800ms ease, transform 800ms ease; }
        .field label, .icon-field label { transition: color 300ms ease; }
        .field.tile-highlight, .icon-field.tile-highlight { border-radius: 12px; outline: 0; background: color-mix(in srgb, var(--tile-accent, currentColor) 18%, var(--surface)); box-shadow: 0 0 36px color-mix(in srgb, var(--tile-accent, currentColor) 34%, transparent); transform: translateY(-1px); transition-duration: 300ms, 300ms, 300ms; }
        .field.tile-highlight label, .icon-field.tile-highlight label { color: color-mix(in srgb, var(--tile-accent, currentColor) 78%, var(--muted)); }
        .field.tile-highlight input, .field.tile-highlight select, .icon-field.tile-highlight input { border-color: transparent; box-shadow: none; background: color-mix(in srgb, var(--tile-accent, currentColor) 8%, var(--bg)); }
        .field:has(.icon-field.tile-highlight) { background: transparent; box-shadow: none; transform: none; }
        .icon-field.tile-highlight .icon-preview { background: color-mix(in srgb, var(--tile-accent, currentColor) 18%, var(--bg)); box-shadow: none; border-color: transparent; }
        .tile-preview-box ha-icon { --mdc-icon-size: 64px; color: currentColor; }
        .tile-preview-box strong { display: block; margin-top: 14px; font-size: 24px; color: var(--text); text-transform: none; }
        .tile-preview-box span { display: block; margin-top: 6px; color: var(--muted); font-size: 16px; text-transform: none; }
        .tile-preview-meta { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
        .tile-preview-meta div { display: grid; grid-template-columns: 24px 1fr; gap: 8px; align-items: center; color: var(--muted); font-size: 12px; text-transform: none; }
        .tile-preview-meta strong { display: block; color: var(--text); font-size: 13px; font-weight: 700; text-transform: none; }
        .tile-note { margin-top: 18px; padding: 12px; border: 1px solid rgba(79,156,255,.22); border-radius: 12px; color: #7cb4ff; background: rgba(79,156,255,.10); text-transform: none; }
        .tile-form { display: grid; }
        .tile-section { padding: 18px; border-bottom: 1px solid var(--line); }
        .tile-section:last-child { border-bottom: 0; }
        .tile-section h3 { margin: 0 0 14px; color: var(--muted); font-size: 13px; text-transform: uppercase; }
        .tile-footer { display: flex; justify-content: space-between; gap: 12px; padding: 14px 18px; border-top: 1px solid var(--line); background: rgba(0,0,0,.08); }
        .fields { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 12px; align-items: end; }
        .field { display: grid; gap: 5px; }
        .span-2 { grid-column: span 2; }
        .span-3 { grid-column: span 3; }
        .span-4 { grid-column: span 4; }
        .span-6 { grid-column: span 6; }
        .span-12 { grid-column: 1 / -1; }
        .tile-actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
        .child-tiles { display: grid; gap: 8px; margin-top: 10px; }
        .child-tile { display: flex; justify-content: space-between; gap: 10px; align-items: center; padding: 10px; border: 1px solid var(--line); border-radius: 12px; background: var(--surface-2); }
        .child-tile small { display: block; color: var(--muted); margin-top: 2px; }
        label { color: var(--muted); font-size: 12px; font-weight: 800; }
        .version { display: inline-flex; align-items: center; margin-left: 10px; padding: 4px 8px; border: 1px solid var(--line); border-radius: 999px; color: var(--muted); font-size: 12px; font-weight: 800; vertical-align: middle; }
        input, select, ha-entity-picker { min-width: 0; width: 100%; font: inherit; }
        input, select { border: 1px solid var(--line); border-radius: 10px; background: var(--bg); color: var(--text); padding: 10px; }
        ha-entity-picker { --mdc-theme-surface: var(--surface); --mdc-theme-on-surface: var(--text); --mdc-theme-primary: var(--accent); display: block; }
        .entity-fallback { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
        .icon-field { display: grid; grid-template-columns: 36px 1fr; gap: 10px; align-items: center; padding: 4px; border-radius: 12px; background: color-mix(in srgb, var(--bg) 92%, transparent); }
        .icon-field input { min-height: 36px; }
        ha-icon-picker { max-width: 100%; overflow: hidden; }
        .icon-preview { width: 36px; height: 36px; display: grid; place-items: center; border: 1px solid var(--line); border-radius: 10px; color: #e99900; background: var(--bg); }
        .accent-orange { color: #e99900; }
        .accent-red { color: #ff5338; }
        .accent-white { color: #f1f1f1; }
        ha-icon-picker { --mdc-theme-surface: var(--surface); --mdc-theme-on-surface: var(--text); color: inherit; }
        .small { padding: 10px 12px; border-radius: 10px; }
        .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
        dialog.studio-dialog { width: min(520px, calc(100vw - 44px)); max-height: calc(100vh - 44px); overflow: auto; background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: 24px; padding: 20px; box-shadow: 0 24px 80px rgba(0,0,0,.32); opacity: 0; transform: translateY(10px) scale(.98); transition: opacity .16s ease, transform .16s ease; }
        dialog.studio-dialog[open] { opacity: 1; transform: none; }
        dialog.studio-dialog.narrow { width: min(420px, calc(100vw - 44px)); }
        dialog.studio-dialog::backdrop { background: rgba(0,0,0,.42); backdrop-filter: blur(8px); }
        .panel-tile-options { display: grid; gap: 12px; margin-top: 18px; }
        .panel-tile-option { display: grid; grid-template-columns: 126px minmax(0, 1fr); gap: 14px; align-items: center; text-align: left; background: var(--surface-2); color: var(--text); border: 1px solid var(--line); }
        .panel-tile-option small { display: block; color: var(--muted); margin-top: 4px; line-height: 1.35; }
        .panel-tile-preview { aspect-ratio: 1.35; border-radius: 16px; background: #25262f; color: #f5f3ee; display: grid; place-items: center; gap: 4px; padding: 12px; text-align: center; }
        .panel-tile-preview ha-icon { --mdc-icon-size: 36px; color: #e99900; }
        .panel-tile-preview.accent-white ha-icon { color: #f1f1f1; }
        .panel-tile-preview.accent-red ha-icon { color: #ff5338; }
        .panel-tile-preview strong, .panel-tile-preview small { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
         .settings-panel { display: grid; gap: 16px; }
          .appearance-panel {
            --appearance-bg: var(--bg);
            --appearance-surface: var(--surface);
            --appearance-surface-variant: var(--surface-2);
            --appearance-text: var(--text);
            --appearance-text-muted: var(--muted);
            --appearance-accent: var(--accent);
            --appearance-border: var(--line);
            --appearance-hover: color-mix(in srgb, var(--appearance-accent) 10%, var(--appearance-surface-variant));
            --appearance-active: color-mix(in srgb, var(--appearance-accent) 18%, var(--appearance-surface));
            --appearance-selected: color-mix(in srgb, var(--appearance-accent) 40%, transparent);
            display: grid;
            gap: 18px;
            color: var(--appearance-text);
          }
          .appearance-toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: end; flex-wrap: wrap; }
          .appearance-mode { display: flex; gap: 8px; flex-wrap: wrap; }
          .appearance-mode button { background: var(--appearance-surface-variant); color: var(--appearance-text); border: 1px solid var(--appearance-border); }
          .appearance-mode button.active { background: var(--appearance-accent); color: #160902; border-color: transparent; }
          .appearance-category { display: grid; gap: 10px; }
          .appearance-category h3 { margin: 0; color: var(--appearance-text-muted); font-size: 13px; text-transform: uppercase; letter-spacing: .04em; }
          .appearance-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(238px, 1fr)); gap: 12px; }
          .appearance-card { text-align: left; border: 1px solid var(--appearance-border); border-radius: 20px; background: var(--appearance-surface-variant); color: var(--appearance-text); padding: 14px; display: grid; gap: 10px; }
          .appearance-card:hover { background: var(--appearance-hover); }
          .appearance-card.active { border-color: var(--appearance-accent); box-shadow: 0 0 0 2px var(--appearance-selected); background: var(--appearance-active); }
          .appearance-card strong { font-size: 18px; }
          .appearance-card small { color: var(--appearance-text-muted); line-height: 1.4; }
          .appearance-card-head { display: flex; justify-content: space-between; gap: 10px; align-items: start; }
          .appearance-preview { height: 96px; border: 1px solid var(--appearance-border); border-radius: 16px; padding: 10px; background: var(--preset-bg); display: grid; grid-template-columns: 1.4fr 1fr; gap: 8px; }
          .appearance-preview-main, .appearance-preview-side span { border-radius: 12px; background: var(--preset-tile); }
          .appearance-preview-main { display: grid; place-items: center; color: var(--preset-accent); }
          .appearance-preview-main::before { content: ""; width: 28px; height: 28px; border-radius: 50%; background: currentColor; box-shadow: 0 0 22px color-mix(in srgb, currentColor 55%, transparent); }
          .appearance-preview-side { display: grid; gap: 8px; }
          .appearance-swatches { display: flex; gap: 6px; flex-wrap: wrap; }
          .appearance-swatch { width: 22px; height: 22px; border-radius: 999px; border: 1px solid var(--appearance-border); background: var(--swatch); }
          .appearance-support { display: flex; gap: 6px; flex-wrap: wrap; }
          .appearance-error { padding: 10px 12px; border-radius: 12px; border: 1px solid rgba(255,83,56,.30); background: rgba(255,83,56,.10); color: #ff725d; }
          .appearance-actions { display: flex; gap: 10px; flex-wrap: wrap; }
          .aod-style-panel { display: grid; gap: 18px; margin-top: 18px; }
          .aod-enable-card { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 18px; }
          .aod-enable-copy { display: grid; gap: 4px; }
          .aod-enable-copy strong { font-size: 18px; }
          .aod-switch { position: relative; display: inline-flex; align-items: center; gap: 10px; cursor: pointer; color: var(--muted); font-weight: 900; }
          .aod-switch input { position: absolute; opacity: 0; pointer-events: none; }
          .aod-switch-track { width: 58px; height: 32px; border-radius: 999px; border: 1px solid var(--line); background: var(--surface-2); position: relative; transition: background .16s ease, border-color .16s ease; }
          .aod-switch-track::after { content: ""; position: absolute; width: 24px; height: 24px; left: 3px; top: 3px; border-radius: 999px; background: var(--muted); transition: transform .16s ease, background .16s ease; }
          .aod-switch input:checked + .aod-switch-track { background: color-mix(in srgb, var(--accent) 36%, var(--surface-2)); border-color: var(--accent); }
          .aod-switch input:checked + .aod-switch-track::after { transform: translateX(26px); background: var(--accent); }
          .aod-style-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(238px, 1fr)); gap: 12px; }
          .aod-style-card { text-align: left; border: 1px solid var(--line); border-radius: 18px; background: var(--surface-2); color: var(--text); padding: 12px; display: grid; gap: 10px; }
          .aod-style-card.active { border-color: var(--accent); box-shadow: 0 0 0 2px color-mix(in srgb, var(--accent) 35%, transparent); background: color-mix(in srgb, var(--accent) 12%, var(--surface-2)); }
          .aod-style-card strong { font-size: 17px; }
          .aod-style-card small { color: var(--muted); line-height: 1.35; }
          .aod-style-head { display: flex; justify-content: space-between; gap: 8px; align-items: start; }
          .aod-clock-preview { height: 84px; border: 1px solid rgba(255,255,255,.10); border-radius: 16px; background: #020304; color: #f7f3e8; display: grid; place-items: center; text-align: center; overflow: hidden; }
          .aod-clock-preview span { display: block; }
          .aod-clock-preview .time { font-size: 30px; line-height: 1; letter-spacing: .02em; }
          .aod-clock-preview .date { margin-top: 5px; color: #9ca3af; font-size: 10px; }
          .aod-clock-preview.modern .time { font-weight: 200; letter-spacing: .08em; }
          .aod-clock-preview.warsaw_zaklad { border-color: #c6a15b; color: #f3d28a; font-family: Georgia, serif; text-transform: uppercase; }
          .aod-clock-preview.warsaw_zaklad .time { border: 1px solid #c6a15b; padding: 6px 10px; font-weight: 800; }
          .aod-clock-preview.popart { background: radial-gradient(circle at 80% 20%, #ffcf33 0 18%, transparent 19%), radial-gradient(circle at 12% 85%, #31d1ff 0 18%, transparent 19%), #120713; color: #ff5aa5; }
          .aod-clock-preview.popart .time { font-weight: 950; text-shadow: 3px 3px 0 #111; }
          .aod-clock-preview.fullscreen_bold .time { font-size: 44px; font-weight: 950; letter-spacing: -.08em; }
          .aod-clock-preview.fullscreen_heavy .time { font-size: 52px; font-weight: 950; letter-spacing: -.12em; text-shadow: 1px 0 0 #fff, -1px 0 0 #fff, 0 1px 0 #fff, 0 -1px 0 #fff; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .spinner { width: 22px; height: 22px; border: 3px solid var(--line); border-top-color: var(--accent); border-radius: 50%; animation: spin .8s linear infinite; }
        .picker-filters { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
        .picker-filters button.active { background: var(--accent); color: #1a0d03; border-color: transparent; }
        .tablet-list { display: grid; gap: 10px; margin-top: 16px; }
        .preset-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin-bottom: 16px; }
        .preset-card { text-align: left; background: var(--surface-2); color: var(--text); border: 1px solid var(--line); }
        .preset-card strong { display: block; margin-bottom: 4px; }
        .preset-card span { color: var(--muted); font-size: 12px; }
        .conflict-box { margin: 16px 0; border: 1px solid rgba(255,83,56,.30); background: rgba(255,83,56,.10); border-radius: 18px; padding: 16px; }
        .conflict-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
        .settings-row { display: grid; gap: 8px; margin-top: 16px; }
        .toolbar { display: flex; justify-content: flex-end; gap: 8px; margin-bottom: 12px; }
        .add-button { display: inline-flex; align-items: center; gap: 8px; border-radius: 999px; padding: 10px 14px; transition: background .18s ease, box-shadow .18s ease, transform .18s ease; }
        .add-button::after { content: "+"; display: inline-grid; place-items: center; width: 18px; height: 18px; border-radius: 999px; background: rgba(0,0,0,.16); font-size: 13px; line-height: 1; }
        .add-button.secondary::after { background: color-mix(in srgb, var(--accent) 18%, transparent); color: var(--accent); }
        .add-button.secondary { background: color-mix(in srgb, var(--surface-2) 78%, transparent); color: var(--text); border: 1px solid color-mix(in srgb, var(--accent) 28%, var(--line)); }
        .add-button:hover { transform: translateY(-1px); box-shadow: 0 10px 26px rgba(255,122,26,.18); }
        .add-button.secondary:hover { background: color-mix(in srgb, var(--accent) 10%, var(--surface-2)); box-shadow: 0 10px 26px color-mix(in srgb, var(--accent) 12%, transparent); }
        .tablet-preview { background: #080c0f; border: 1px solid var(--line); border-radius: 18px; padding: 14px; color: #f5f3ee; max-width: 980px; margin: 0 auto; }
        .tablet-top { display: grid; grid-template-columns: 1.25fr 1.2fr 1.6fr; gap: 10px; margin-bottom: 10px; }
        .tablet-clock { min-height: 130px; display: grid; place-items: center; color: #eee; font-size: 64px; font-weight: 300; letter-spacing: .03em; }
        .tablet-people { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
        .tablet-person { border-radius: 16px; background: #25262f; padding: 12px; display: flex; gap: 10px; align-items: center; }
        .tablet-dot { width: 34px; height: 34px; border-radius: 50%; background: #333541; display: grid; place-items: center; color: #ff5338; }
        .tablet-body { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
        .tablet-main-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; grid-column: span 2; }
        .tablet-side-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; align-content: start; }
        .tablet-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; grid-auto-flow: dense; }
        .tablet-top .tablet-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
        .preview-tile { position: relative; min-height: 110px; border-radius: 18px; background: #25262f; padding: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: #c8c8d0; }
        .preview-large { min-height: 164px; }
        .preview-small { min-height: 112px; }
        .preview-action { min-height: 82px; }
        .preview-camera { min-height: 140px; }
        .preview-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; width: 100%; margin-top: 10px; }
        .preview-action-pill { border-radius: 16px; background: #25262f; border: 0; color: #e4e1dc; padding: 14px; font-weight: 800; }
        .preview-tile ha-icon { --mdc-icon-size: 58px; color: #e99900; }
        .preview-tile.accent-red ha-icon { color: #ff5338; }
        .preview-tile.accent-white ha-icon { color: #f1f1f1; }
        .preview-size { position: absolute; top: 10px; right: 10px; width: 92px; padding: 6px; font-size: 12px; opacity: .82; }
        .preview-meta { color: var(--muted); font-size: 12px; overflow-wrap: anywhere; }
        .layout-editor { display: grid; grid-template-columns: 220px minmax(0, 1fr) 180px; gap: 16px; align-items: stretch; }
        .layout-left-panel { display: grid; align-content: start; gap: 12px; }
        .layout-toolbar { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
        .layout-frame { position: relative; width: 100%; align-self: start; border: 1px solid #334155; border-radius: 10px; background: #0f172a; overflow: hidden; }
        .layout-frame.popup-context { border-color: rgba(255,255,255,.16); background: radial-gradient(circle at 20% 20%, rgba(255,255,255,.10), transparent 28%), #05070a; }
        .layout-frame.popup-context::before { content:""; position:absolute; inset:0; background: rgba(0,0,0,.38); backdrop-filter: blur(6px); }
        .layout-frame.will-drop { border: 3px dashed #ef4444; overflow: visible; }
        .layout-grid { position: absolute; inset: 10px; display: grid; gap: 6px; overflow: visible; min-height: 0; grid-auto-columns: calc((100% - ((var(--cols) - 1) * 6px)) / var(--cols)); grid-auto-rows: calc((100% - ((var(--rows) - 1) * 6px)) / var(--rows)); }
        .layout-frame.popup-context .layout-grid { inset: 15%; border: 1px solid rgba(255,255,255,.18); border-radius: 24px; padding: 10px; background: linear-gradient(135deg, rgba(255,255,255,.18), rgba(255,255,255,.06)); box-shadow: 0 24px 80px rgba(0,0,0,.38); backdrop-filter: blur(18px) saturate(1.35); }
        .layout-frame.popup-context .layout-grid::before { content:""; pointer-events:none; position:absolute; inset:0; border-radius:24px; opacity:.32; background-image: radial-gradient(circle, rgba(255,255,255,.55) 0 1px, transparent 1px); background-size: 5px 5px; mix-blend-mode: overlay; }
        .layout-frame.popup-context .layout-cell-tile { background: rgba(31,41,55,.72); backdrop-filter: blur(8px); }
        .layout-cell-tile { position: relative; min-width: 0; min-height: 0; box-sizing: border-box; overflow: hidden; border: 1px solid #334155; border-radius: 6px; background: #1f2937; color: #cbd5e1; padding: 0; display: grid; place-items: center; text-align: center; cursor: pointer; user-select: none; font-size: clamp(.55rem, 1.4vw, .8rem); }
        .layout-tile-content { min-width: 0; min-height: 0; display: grid; gap: 3px; place-items: center; text-align: center; padding: 4px; pointer-events: none; }
        .layout-tile-icon { line-height: 1; }
        .layout-tile-title { font-weight: 700; }
        .layout-tile-subtitle { color: #94a3b8; font-size: .78em; }
        .layout-cell-tile.selected { border-color: #facc15; background: #3f3515; color: #fef3c7; }
        .layout-cell-tile.will-remove { border-color: #ef4444; background: #3b1f24; box-shadow: inset 0 0 0 2px rgba(239,68,68,.45); }
        .layout-cell-tile.dragging { opacity: .75; z-index: 3; transition: none; pointer-events: none; }
        .layout-cell-tile.outside { opacity: .45; outline: 2px dashed #ff725d; }
        .layout-cell-tile ha-icon { --mdc-icon-size: 32px; color: #e99900; }
        .layout-cell-tile strong, .layout-cell-tile .preview-meta { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .layout-cell-tile.accent-red ha-icon { color: #ff5338; }
        .layout-cell-tile.accent-white ha-icon { color: #f1f1f1; }
        .layout-resize-handle { position: absolute; background: #facc15; opacity: 0; touch-action: none; }
        .layout-cell-tile:hover .layout-resize-handle, .layout-cell-tile.selected .layout-resize-handle { opacity: 1; }
        .layout-resize-handle.right { top: 8px; right: -4px; width: 8px; bottom: 8px; cursor: ew-resize; }
        .layout-resize-handle.left { top: 8px; left: -4px; width: 8px; bottom: 8px; cursor: ew-resize; }
        .layout-resize-handle.bottom { left: 8px; right: 8px; bottom: -4px; height: 8px; cursor: ns-resize; }
        .layout-resize-handle.top { left: 8px; right: 8px; top: -4px; height: 8px; cursor: ns-resize; }
        .layout-resize-handle.corner { right: -5px; bottom: -5px; width: 12px; height: 12px; border-radius: 50%; cursor: nwse-resize; }
        .layout-resize-handle.corner.top-left { left: -5px; top: -5px; right: auto; bottom: auto; cursor: nwse-resize; }
        .layout-resize-handle.corner.top-right { top: -5px; right: -5px; bottom: auto; cursor: nesw-resize; }
        .layout-resize-handle.corner.bottom-left { left: -5px; bottom: -5px; right: auto; cursor: nesw-resize; }
        .layout-ghost { pointer-events: none; z-index: 4; border: 3px solid #facc15; border-radius: 6px; background: repeating-linear-gradient(45deg, rgba(250,204,21,.28) 0 8px, rgba(250,204,21,.12) 8px 16px), rgba(17,24,39,.72); box-shadow: 0 0 0 2px rgba(17,24,39,.95), 0 0 24px rgba(250,204,21,.55); }
        .layout-ghost.invalid { border-color: #ef4444; background: rgba(239, 68, 68, .16); }
        .layout-side { display: grid; gap: 12px; min-height: 0; }
        .layout-panel { border: 1px solid #334155; border-radius: 8px; padding: 12px; background: #0f172a; display: grid; gap: 10px; }
        .layout-panel[data-layout-tray-zone] { height: 100%; min-height: 0; grid-template-rows: auto minmax(0, 1fr) auto; }
        .layout-panel.tray-active { border-color: #facc15; box-shadow: 0 0 0 3px rgba(250,204,21,.18); }
        .layout-panel.tray-flash { border-color: #ef4444; box-shadow: 0 0 0 3px rgba(239,68,68,.25); }
        .layout-buttons { display: flex; gap: 8px; flex-wrap: wrap; }
        .layout-context-select { width: 100%; }
        .tray-list { min-height: 0; overflow-y: auto; overscroll-behavior: contain; display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; align-content: start; align-items: start; margin-bottom: 12px; }
        .tray-item { aspect-ratio: var(--tray-aspect, 1); display: grid; place-items: center; border: 0; border-radius: 6px; background: #374151; color: #cbd5e1; padding: 0; cursor: grab; user-select: none; }
        .tray-item.dragging { opacity: .6; }
        .tray-item.returning { transition: transform 180ms ease; }
        .tray-preview-tile { min-height: 0; border-radius: 0; background: transparent; color: #f5f3ee; display: grid; place-items: center; gap: 4px; padding: 6px; min-width: 0; text-align: center; }
        .tray-preview-tile ha-icon { --mdc-icon-size: 28px; color: #e99900; }
        .tray-preview-tile.accent-red ha-icon { color: #ff5338; }
        .tray-preview-tile.accent-white ha-icon { color: #f1f1f1; }
        .tray-preview-tile strong { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: .75rem; }
        .tray-info { display: none; }
        .layout-panel details summary { cursor: pointer; font-weight: 800; }
        .layout-panel .fields { grid-template-columns: repeat(auto-fit, minmax(min(100%, 86px), 1fr)); gap: 10px; }
        .layout-panel .field, .layout-panel .layout-checks, .layout-panel .sub, .layout-panel .warning { grid-column: auto / span 1; min-width: 0; }
        .layout-panel button[data-layout-apply] { justify-self: start; max-width: 100%; }
        .layout-checks { display: flex; flex-wrap: wrap; gap: 10px; }
        .layout-checks label { display: flex; align-items: center; gap: 6px; color: #cbd5e1; font-size: .8rem; }
        .layout-checks input { width: auto; }
        .clock-style-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
        .clock-style-card { border: 1px solid var(--line); border-radius: 12px; background: var(--surface-2); padding: 8px; color: var(--text); text-align: center; }
        .clock-style-card.active { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
        .clock-style-card strong { display: block; font-size: 24px; line-height: 1; }
        .clock-style-card small { color: var(--muted); font-size: 10px; }
        .cover-preview { min-height: 120px; display: grid; place-items: center; gap: 4px; border-radius: 16px; background: color-mix(in srgb, var(--tile-accent, currentColor) 8%, var(--bg)); color: var(--text); overflow: hidden; }
        .cover-preview strong, .cover-preview small { position: relative; z-index: 1; }
        .cover-preview small { color: var(--muted); }
        .cover-scene { position: relative; width: min(180px, 70%); height: 54px; border-radius: 14px; background: rgba(255,255,255,.06); overflow: hidden; box-shadow: inset 0 0 0 1px var(--line); }
        .cover-scene span { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(255,255,255,.20), rgba(255,255,255,.06)); animation: coverPreview 2.4s ease-in-out infinite alternate; }
        .cover-curtain .cover-scene span { width: 50%; background: repeating-linear-gradient(90deg, rgba(255,255,255,.26) 0 8px, rgba(255,255,255,.10) 8px 16px); }
        .cover-curtain .cover-scene span + span { left: auto; right: 0; animation-direction: alternate-reverse; }
        .cover-shade .cover-scene span { background: repeating-linear-gradient(0deg, rgba(255,255,255,.24) 0 4px, rgba(255,255,255,.08) 4px 9px); }
        .cover-gate .cover-scene span { background: repeating-linear-gradient(90deg, rgba(255,255,255,.22) 0 10px, rgba(255,255,255,.08) 10px 18px); }
        .cover-top .cover-scene span { transform-origin: top; }
        .cover-bottom .cover-scene span { transform-origin: bottom; }
        .cover-left .cover-scene span { transform-origin: left; }
        .cover-right .cover-scene span { transform-origin: right; }
        @keyframes coverPreview { from { transform: scale(1); opacity: .95; } to { transform: scale(.42); opacity: .72; } }
        .error { color: #ff725d; margin-top: 12px; }
        a { color: var(--accent); }
        @media (max-width: 900px) {
          .layout-editor { grid-template-columns: 1fr; }
          .tile-body { grid-template-columns: 1fr; }
          .tile-preview-pane { border-right: 0; border-bottom: 1px solid var(--line); }
          .fields { grid-template-columns: repeat(6, minmax(0, 1fr)); }
          .span-2, .span-3, .span-4 { grid-column: span 3; }
          .span-6 { grid-column: 1 / -1; }
        }
        @media (max-width: 620px) {
          .fields { grid-template-columns: 1fr; }
          .span-2, .span-3, .span-4, .span-6 { grid-column: 1 / -1; }
        }
      </style>
      <main class="page">
        ${this._header()}
        ${selected ? this._detailView(selected) : this._mainView(panels)}
      </main>
      ${this._settingsOpen ? this._settingsView() : ""}
      ${this._tabletPickerOpen ? this._tabletPickerView() : ""}
        ${this._confirm ? this._confirmDialog() : ""}
        ${this._panelTilePicker ? this._panelTilePickerView() : ""}
    `;
    this._bindEvents();
  }

  _header() {
    return `
      <section class="hero">
        <div>
          <div class="brand">
            <div class="logo" aria-hidden="true">${this._logoMark()}</div>
            <h1>Hapanels Studio <span class="version">${STUDIO_FRONTEND_VERSION}</span></h1>
          </div>
          <div class="sub">Panele wykryte po MQTT i stan synchronizacji dashboardu/AOD.</div>
          ${this._error ? `<div class="error">${this._escape(this._error)}</div>` : ""}
        </div>
        <div class="actions">
          <button id="refresh">Odśwież</button>
          <button id="settings" class="secondary iconbtn" title="Ustawienia">⚙</button>
        </div>
      </section>
    `;
  }

  _logoMark() {
    return `<svg viewBox="330 235 540 545" role="img" aria-label="Hapanels"><path d="M365 717V452c0-19 9-36 24-47l168-135c25-20 61-20 86 0l168 135c15 11 24 28 24 47v265" fill="none" stroke="currentColor" stroke-width="54" stroke-linecap="round" stroke-linejoin="round"/><path d="M445 476c0-14 12-26 26-26h103c14 0 26 12 26 26v248c0 14-12 26-26 26H471c-14 0-26-12-26-26zM630 476c0-14 12-26 26-26h103c14 0 26 12 26 26v78c0 14-12 26-26 26H656c-14 0-26-12-26-26zM630 641c0-14 12-26 26-26h103c14 0 26 12 26 26v83c0 14-12 26-26 26H656c-14 0-26-12-26-26z" fill="#1e90ff"/></svg>`;
  }

  _mainView(panels) {
    if (!panels.length) {
      return `
        <section class="empty">
          <div class="empty-box">
            <div class="logo" style="margin:0 auto 18px">H</div>
            <h2>Witaj w Hapanels Studio</h2>
            <p>Hapanels łączy natywną aplikację na tablet z integracją Home Assistant. Tablet renderuje szybki panel Compose, a Studio zarządza konfiguracją przez MQTT.</p>
            <p><a href="${APP_URL}" target="_blank" rel="noreferrer">Pobierz aplikację Hapanels</a> i uruchom MQTT, żeby tablet pojawił się tutaj automatycznie.</p>
            <button id="first-tablet">Dodaj swój pierwszy tablet +</button>
          </div>
        </section>
      `;
    }
    return `<section class="grid">${panels.map((panel) => this._panelCard(panel)).join("")}</section>`;
  }

  _panelCard(panel) {
    const status = this._escape(panel.status || "unknown");
    const hidden = this._hiddenDevices.has(panel.device);
    return `
      <button class="card panel-card" data-select-device="${this._escape(panel.device)}">
        <div class="name">
          <span class="panel-title"><span>${this._escape(this._panelLabel(panel))}</span><small>${this._escape(panel.device || "-")}</small></span>
          <span class="panel-pills"><span class="pill ${hidden ? "hidden" : "visible"}">${hidden ? "NIEWIDOCZNY" : "WIDOCZNY"}</span><span class="pill ${status}">${this._statusLabel(panel.status)}</span></span>
        </div>
        <dl>
          <dt>Dashboard</dt><dd>${this._escape(panel.dashboard_id || "-")}</dd>
          <dt>Rewizja</dt><dd>${this._escape(panel.revision ?? "-")}</dd>
          <dt>Zmienił</dt><dd>${this._escape(panel.updated_by || "-")}</dd>
        </dl>
      </button>
    `;
  }

  _detailView(panel) {
    const status = this._escape(panel.status || "unknown");
    const config = this._configs[panel.device];
    return `
      <section class="card">
        <div class="detail-head">
          <div>
            <button id="back" class="secondary small">← Tablety</button>
            <h2 style="margin-top:16px">${this._escape(this._panelLabel(panel))}</h2>
            <div class="sub"><span class="pill ${status}">${this._statusLabel(panel.status)}</span> Dashboard ${this._escape(panel.dashboard_id || "-")} · rewizja ${this._escape(panel.revision ?? "-")}</div>
          </div>
          <div class="actions">
            <button id="refresh-detail" class="secondary">Odśwież</button>
            <button class="danger" data-hide-device="${this._escape(panel.device)}">Usuń tablet</button>
          </div>
        </div>
        ${panel.status === "conflict" ? this._conflictView(panel) : ""}
        <div class="tabs">
          ${this._tab("tiles", "Kafle")}
          ${this._tab("aod", "Always On Display")}
          ${this._tab("settings", "Informacje")}
          ${this._tab("preview", "Podgląd")}
          ${this._tab("appearance", "Wygląd")}
        </div>
        ${this._tabContent(panel.device, config, panel)}
      </section>
    `;
  }

  _tab(id, label) {
    return `<button class="tab ${this._activeTab === id ? "active" : ""}" data-tab="${id}">${label}</button>`;
  }

  _conflictView(panel) {
    return `
      <div class="conflict-box">
        <strong>Konflikt synchronizacji</strong>
        <div class="sub">Studio wysłało zmianę dla rewizji ${this._escape(panel.attempted_base_revision ?? "-")}, ale tablet ma rewizję ${this._escape(panel.current_revision ?? panel.revision ?? "-")}.</div>
        ${this._pendingPatches[panel.device]?.tile_updates?.length ? `<div class="sub">Oczekujące zmiany kafli: ${this._escape(this._pendingPatches[panel.device].tile_updates.length)}</div>` : ""}
        <div class="conflict-actions">
          <button data-resolve-conflict="studio" data-device="${this._escape(panel.device)}">Użyj wersji Studio</button>
          <button class="secondary" data-resolve-conflict="tablet" data-device="${this._escape(panel.device)}">Użyj wersji tabletu</button>
        </div>
      </div>
    `;
  }

  async _resolveConflict(device, mode) {
    if (mode === "studio") {
      const patch = structuredClone(this._pendingPatches[device]);
      if (patch) {
        const panel = this._panels.find((item) => item.device === device);
        patch.base_revision = Number(panel?.current_revision ?? panel?.revision ?? this._configs[device]?.revision ?? 0);
        await this._hass.callService("hapanels", "patch_dashboard_config", { device, patch });
      }
    } else if (mode === "tablet") {
      await this._hass.callService("hapanels", "accept_tablet_config", { device });
    }
    await this._load();
  }

  _tabContent(device, config, panel = null) {
    if (!config) return `<div class="empty-box">Brak pobranej konfiguracji dashboardu.</div>`;
    if (this._activeTab === "aod") return this._aodView(device, config);
    if (this._activeTab === "settings") return this._tabletInfoView(config, panel);
    if (this._activeTab === "preview") return this._previewView(device, config);
    if (this._activeTab === "appearance") return this._appearanceView(device, config, panel);
    const tiles = (config.tiles || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0));
    return `
      <div class="toolbar">
        <button class="add-button" data-add-ha-tile data-device="${this._escape(device)}" data-surface="dashboard">Kafel HA</button>
        <button class="add-button secondary" data-pick-panel-tile data-target="tiles" data-device="${this._escape(device)}" data-surface="dashboard">Kafel panelu</button>
      </div>
      <div class="tiles">${tiles.map((tile) => this._tileEditor(device, tile, "dashboard")).join("")}</div>
    `;
  }

  _aodView(device, config) {
    const aod = config.always_on_display || {};
    const tiles = (aod.tiles || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0));
    const showClockStyles = !tiles.length && (aod.layout || "minimal_clock") !== "grid";
    return `
      <div class="card aod-enable-card">
        <span class="aod-enable-copy"><strong>Always On Display</strong><span class="sub">Wygaszacz ekranu dla tabletu.</span></span>
        <label class="aod-switch"><input id="aod-enabled" type="checkbox" ${aod.enabled ? "checked" : ""}><span class="aod-switch-track"></span><span>${aod.enabled ? "Włączony" : "Wyłączony"}</span></label>
      </div>
      ${showClockStyles ? this._aodClockStylePicker(device, config) : `<div class="sub" style="margin:12px 0 18px">Style zegara AOD działają tylko dla zegara bez kafli, nie dla AOD Grid.</div>`}
      <div class="toolbar"><button data-add-tile data-device="${this._escape(device)}" data-surface="aod">Dodaj kafel AOD +</button></div>
      <div class="tiles">${tiles.map((tile) => this._tileEditor(device, tile, "aod")).join("")}</div>
      <div class="tiles" style="margin-top:18px">
        <div class="tile">
          <div class="tile-head"><span>Ustawienia AOD</span><span>${this._escape(aod.layout || "minimal_clock")}</span></div>
          <div class="preset-grid" style="padding:18px 18px 0">
            ${AOD_PRESETS.map((preset) => `<button class="preset-card" data-aod-preset="${this._escape(preset.id)}" data-device="${this._escape(device)}"><strong>${this._escape(preset.name)}</strong><span>${this._escape(preset.desc)}</span></button>`).join("")}
          </div>
          <div class="fields" style="padding:18px">
            ${this._selectField("aod-layout", "Układ", ["minimal_clock", "status_strip", "grid"], aod.layout || "minimal_clock")}
            ${this._inputField("aod-timeout", "Timeout s", aod.timeout_sec ?? 300, "number")}
            ${this._inputField("aod-brightness", "Jasność %", aod.brightness_percent ?? 3, "number")}
            ${this._inputField("aod-background", "Tło", aod.background || "#000000")}
            ${this._inputField("aod-entities", "Entity IDs", (aod.entity_ids || []).join(", "))}
            ${this._inputField("aod-columns-landscape", "Kolumny L", aod.grid_layout?.columns_landscape ?? 3, "number")}
            ${this._inputField("aod-columns-portrait", "Kolumny P", aod.grid_layout?.columns_portrait ?? 2, "number")}
            ${this._selectField("aod-gap", "Gap", ["small", "medium", "large"], aod.grid_layout?.gap || "small")}
            <button class="small" data-save-aod data-device="${this._escape(device)}">Zapisz AOD</button>
          </div>
        </div>
      </div>
    `;
  }

  _aodClockStylePicker(device, config) {
    const selected = this._aodClockStyle(device, config);
    const groups = AOD_CLOCK_STYLES.reduce((items, style) => {
      (items[style.category] ||= []).push(style);
      return items;
    }, {});
    return `
      <div class="aod-style-panel">
        <div>
          <strong>Styl zegara AOD</strong>
          <div class="sub">Tylko ekran AOD bez kafli. Nie zmienia motywu panelu.</div>
        </div>
        ${this._aodClockStyleError ? `<div class="appearance-error">${this._escape(this._aodClockStyleError)}</div>` : ""}
        ${Object.entries(groups).map(([category, styles]) => `
          <section class="appearance-category">
            <h3>${this._escape(category)}</h3>
            <div class="aod-style-grid">
              ${styles.map((style) => this._aodClockStyleCard(device, style, style.id === selected)).join("")}
            </div>
          </section>
        `).join("")}
      </div>
    `;
  }

  _aodClockStyleCard(device, style, active) {
    return `
      <button class="aod-style-card ${active ? "active" : ""}" data-aod-clock-style="${this._escape(style.id)}" data-device="${this._escape(device)}">
        <span class="aod-style-head"><strong>${this._escape(style.name)}</strong>${active ? `<span class="pill synced">Aktywny</span>` : ""}</span>
        <span class="aod-clock-preview ${this._escape(style.id)}" aria-hidden="true"><span><span class="time">12:34</span><span class="date">środa, 01 lipca</span></span></span>
        <small>${this._escape(style.desc)}</small>
        <small>${this._escape(style.treatment)}</small>
      </button>
    `;
  }

  _aodClockStyleStorageKey(device) {
    return `hapanels_aod_clock_style_${device || "default"}`;
  }

  _aodClockStyle(device, config) {
    const value = localStorage.getItem(this._aodClockStyleStorageKey(device)) || config?.always_on_display?.clock_style;
    return AOD_CLOCK_STYLES.some((style) => style.id === value) ? value : AOD_CLOCK_STYLES[0].id;
  }

  async _applyAodClockStyle(device, styleId) {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    if (!config || !Number.isFinite(revision) || !AOD_CLOCK_STYLES.some((style) => style.id === styleId)) return;
    const aod = { ...(config.always_on_display || {}), clock_style: styleId };
    this._configs[device] = { ...config, always_on_display: aod };
    localStorage.setItem(this._aodClockStyleStorageKey(device), styleId);
    this._aodClockStyleError = null;
    this._render();
    try {
      await this._hass.callService("hapanels", "patch_dashboard_config", {
        device,
        patch: {
          base_revision: revision,
          updated_by: "homeassistant:hapanels_studio",
          surface: "aod",
          aod_clock_style: styleId,
        },
      });
      window.setTimeout(() => this._load(), 800);
    } catch (err) {
      this._aodClockStyleError = `Styl zapisany lokalnie. Nie udało się wysłać do panelu: ${err?.message || err}`;
      this._render();
    }
  }

  _previewView(device, config) {
    const contexts = this._layoutContextOptions(config);
    if (!contexts.some((item) => item.id === this._layoutContext)) this._layoutContext = "main";
    const context = contexts.find((item) => item.id === this._layoutContext) || contexts[0];
    const draft = this._layoutDraft(device, config, context);
    const grid = draft.grid;
    const selected = draft.tiles.find((tile) => tile.id === draft.selectedTileId) || draft.tiles[0];
    const outside = draft.tiles.filter((tile) => this._isOutsideGrid(tile, grid));
    const willRemove = draft.tiles.filter((tile) => tile.col > grid.columns || tile.row > grid.rows);
    const resolution = this._tabletResolution(device);
    return `
      <div class="layout-editor">
        <div class="layout-left-panel">
          <div class="layout-panel">
            <label class="field layout-context-select">
              <span>Kontekst</span>
              <select id="layout-context">
                ${contexts.map((item) => `<option value="${this._escape(item.id)}" ${item.id === context.id ? "selected" : ""}>${this._escape(item.label)}</option>`).join("")}
              </select>
            </label>
            <div class="layout-buttons">
              <button class="small add-button" data-layout-add-ha>Kafel HA</button>
              <button class="small add-button secondary" data-pick-panel-tile data-target="layout" ${context.id === "main" ? "" : "disabled"}>Kafel panelu</button>
              <button class="small secondary" data-layout-tray>Usuń kafelek</button>
            </div>
            <span class="sub">${this._escape(draft.tiles.length)}/${this._escape(grid.columns * grid.rows)}</span>
          </div>
          <div class="layout-panel">
            <details data-layout-grid-panel ${this._layoutGridCollapsed ? "" : "open"}>
              <summary>Ustawienia grida</summary>
              <div class="fields" style="margin-top:10px">
                ${this._inputField("layout-columns", "Kafle poziomo", grid.columns, "number", "span-6")}
                ${this._inputField("layout-rows", "Kafle pionowo", grid.rows, "number", "span-6")}
                ${this._inputField("layout-aspect-width", "Szerokość", grid.aspectWidth, "number", "span-6")}
                ${this._inputField("layout-aspect-height", "Wysokość", grid.aspectHeight, "number", "span-6")}
                ${resolution ? `<span class="sub span-12">Rozdzielczość ustawiana automatycznie z wykrytego urządzenia.</span>` : ""}
                <span class="sub span-6">Proporcje: ${this._escape(this._aspectLabel(grid.aspectWidth, grid.aspectHeight))}</span>
                ${willRemove.length ? `<span class="warning span-6">Uwaga: po kliknięciu Zastosuj ${this._escape(willRemove.length)} kaf. trafi do zasobnika.</span>` : ""}
                <button class="small" data-layout-apply data-device="${this._escape(device)}">Zastosuj</button>
              </div>
            </details>
          </div>
          ${selected ? this._layoutSelectedPanel(selected) : `<div class="layout-panel"><strong>Ustawienia kafla</strong><span class="sub">Brak kafli.</span></div>`}
          <div class="layout-panel">
            <div class="layout-buttons">
              <button class="small secondary" data-layout-reset data-device="${this._escape(device)}">Reset draftu</button>
              <button class="small secondary" data-layout-panel-preset data-device="${this._escape(device)}">Układ panelu</button>
              <button class="small" data-layout-save data-device="${this._escape(device)}">Zapisz układ</button>
            </div>
          </div>
        </div>
        <div class="layout-frame ${context.type === "popup" ? "popup-context" : ""} ${willRemove.length ? "will-drop" : ""}" style="aspect-ratio:${this._escape(grid.aspectWidth)} / ${this._escape(grid.aspectHeight)}">
          <div class="layout-grid" style="--cols:${this._escape(grid.columns)};--rows:${this._escape(grid.rows)};grid-template-columns:repeat(${this._escape(grid.columns)}, minmax(0, 1fr));grid-template-rows:repeat(${this._escape(grid.rows)}, minmax(0, 1fr));">
            ${draft.tiles.map((tile) => this._layoutTile(tile, grid, tile.id === draft.selectedTileId)).join("")}
            ${this._layoutGhost(draft)}
          </div>
        </div>
        <div class="layout-side">
          <div class="layout-panel ${this._layoutDrag?.toTray ? "tray-active" : ""} ${this._layoutTrayFlash ? "tray-flash" : ""}" data-layout-tray-zone>
            <strong>Zasobnik</strong>
            ${draft.tray.length ? `<div class="tray-list">${draft.tray.map((tile) => this._layoutTrayTile(device, tile)).join("")}</div>` : `<span class="sub">Pusty.</span>`}
            <button type="button" class="small secondary" data-layout-clear-tray ${draft.tray.length ? "" : "disabled"}>Wyczyść zasobnik</button>
          </div>
        </div>
      </div>
    `;
  }

  _appearanceView(device, config, panel = null) {
    const presetId = this._panelThemePreset(device, config);
    const mode = this._panelThemeMode(device, config);
    const groups = PANEL_THEME_PRESETS.reduce((items, preset) => {
      (items[preset.category] ||= []).push(preset);
      return items;
    }, {});
    return `
      <div class="appearance-panel">
        <div class="appearance-toolbar">
          <div>
            <strong>Motyw panelu</strong>
            <div class="sub">${this._escape(this._panelLabel(panel))} · dashboard ${this._escape(config.dashboard_id || "-")} · rewizja ${this._escape(config.revision ?? "-")}</div>
          </div>
          <div class="appearance-mode" aria-label="Tryb jasny lub ciemny panelu">
            <button class="small ${mode === "light" ? "active" : "secondary"}" data-panel-theme-mode="light" data-device="${this._escape(device)}">Jasny</button>
            <button class="small ${mode === "dark" ? "active" : "secondary"}" data-panel-theme-mode="dark" data-device="${this._escape(device)}">Ciemny</button>
          </div>
        </div>
        ${this._panelThemeError ? `<div class="appearance-error">${this._escape(this._panelThemeError)}</div>` : ""}
        ${Object.entries(groups).map(([category, presets]) => `
          <section class="appearance-category">
            <h3>${this._escape(category)}</h3>
            <div class="appearance-grid">
              ${presets.map((preset) => this._appearancePresetCard(device, preset, preset.id === presetId, mode)).join("")}
            </div>
          </section>
        `).join("")}
      </div>
    `;
  }

  _appearancePresetCard(device, preset, active, mode) {
    const colors = preset[mode] || preset.dark;
    const swatches = [colors.bg, colors.surface, colors.tile, colors.text, colors.muted, colors.accent];
    return `
      <button class="appearance-card ${active ? "active" : ""}" data-panel-theme-preset="${this._escape(preset.id)}" data-device="${this._escape(device)}" style="--preset-bg:${this._escape(colors.bg)};--preset-tile:${this._escape(colors.tile)};--preset-accent:${this._escape(colors.accent)}">
        <span class="appearance-card-head">
          <span><strong>${this._escape(preset.name)}</strong></span>
          ${active ? `<span class="pill synced">Aktywny</span>` : ""}
        </span>
        <span class="appearance-preview" aria-hidden="true"><span class="appearance-preview-main"></span><span class="appearance-preview-side"><span></span><span></span></span></span>
        <small>${this._escape(preset.description)}</small>
        <span class="appearance-swatches">${swatches.map((color) => `<span class="appearance-swatch" style="--swatch:${this._escape(color)}" title="${this._escape(color)}"></span>`).join("")}</span>
        <span class="appearance-support"><span class="pill unknown">Light</span><span class="pill unknown">Dark</span></span>
      </button>
    `;
  }

  _panelThemeStorageKey(device, key) {
    return `hapanels_panel_theme_${key}_${device || "default"}`;
  }

  _panelThemePreset(device, config) {
    const value = localStorage.getItem(this._panelThemeStorageKey(device, "preset")) || config?.theme?.preset;
    return PANEL_THEME_PRESETS.some((preset) => preset.id === value) ? value : PANEL_THEME_PRESETS[0].id;
  }

  _panelThemeMode(device, config) {
    const value = localStorage.getItem(this._panelThemeStorageKey(device, "mode")) || config?.theme?.mode;
    return value === "light" || value === "dark" ? value : "dark";
  }

  async _applyPanelTheme(device, nextTheme) {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    if (!config || !Number.isFinite(revision)) return;
    const theme = { preset: this._panelThemePreset(device, config), mode: this._panelThemeMode(device, config), ...nextTheme };
    this._configs[device] = { ...config, theme: { ...(config.theme || {}), ...theme } };
    localStorage.setItem(this._panelThemeStorageKey(device, "preset"), theme.preset);
    localStorage.setItem(this._panelThemeStorageKey(device, "mode"), theme.mode);
    this._panelThemeError = null;
    this._render();
    try {
      await this._hass.callService("hapanels", "patch_dashboard_config", {
        device,
        patch: {
          base_revision: revision,
          updated_by: "homeassistant:hapanels_studio",
          theme,
        },
      });
      window.setTimeout(() => this._load(), 800);
    } catch (err) {
      this._panelThemeError = `Motyw zapisany lokalnie. Nie udało się wysłać do panelu: ${err?.message || err}`;
      this._render();
    }
  }

  _layoutContextOptions(config) {
    const items = [{ id: "main", type: "main", panelId: "", label: "Panel główny" }];
    (config.tiles || []).filter((tile) => ["folder", "popup"].includes(tile.kind) && tile.panel_id).forEach((tile) => {
      items.push({ id: `${tile.kind}:${tile.panel_id}`, type: tile.kind, panelId: tile.panel_id, label: `${tile.kind === "popup" ? "Popup" : "Folder"}: ${tile.label || tile.panel_id}` });
    });
    return items;
  }

  _layoutContextTiles(config, context) {
    const tiles = config.tiles || [];
    if (!context || context.id === "main") return tiles.filter((tile) => !tile.panel_id || ["folder", "popup"].includes(tile.kind));
    return tiles.filter((tile) => tile.panel_id === context.panelId && !["folder", "popup"].includes(tile.kind));
  }

  _layoutContextEditor(config, context) {
    if (!context || context.id === "main") return config.layout_editor || {};
    return config.layout_editor?.contexts?.[context.panelId] || {};
  }

  _layoutDraft(device, config, context = null) {
    const activeContext = context || this._layoutContextOptions(config).find((item) => item.id === this._layoutContext) || { id: "main" };
    const key = `${device}:${config.revision ?? "new"}:${activeContext.id}`;
    if (this._layoutDrafts[key]) return this._layoutDrafts[key];
    const resolution = this._tabletResolution(device);
    const editor = this._layoutContextEditor(config, activeContext);
    const grid = {
      columns: Number(editor.grid?.columns) || 12,
      rows: Number(editor.grid?.rows) || 9,
      aspectWidth: resolution?.width || Number(editor.grid?.aspectWidth) || 16,
      aspectHeight: resolution?.height || Number(editor.grid?.aspectHeight) || 9,
    };
    const tiles = this._layoutTiles(this._layoutContextTiles(config, activeContext).slice().sort((a, b) => (a.order || 0) - (b.order || 0)), grid);
    const draft = {
      context: activeContext,
      grid,
      tiles,
      tray: this._layoutSharedTrayDraft(device, config, grid),
      selectedTileId: tiles[0]?.id || null,
    };
    this._layoutDrafts[key] = draft;
    return draft;
  }

  _layoutSharedTrayDraft(device, config, grid) {
    const key = `${device}:${config.revision ?? "new"}:tray`;
    if (!this._layoutDrafts[key]) {
      this._layoutDrafts[key] = (config.layout_editor?.tray || []).map((tile, index) => this._layoutTileModel(tile, index, grid));
    }
    return this._layoutDrafts[key];
  }

  _layoutTiles(tiles, grid) {
    const occupied = new Set();
    const mark = (tile) => {
      for (let row = tile.row; row < tile.row + tile.rowSpan; row += 1) {
        for (let col = tile.col; col < tile.col + tile.colSpan; col += 1) occupied.add(`${col}:${row}`);
      }
    };
    return tiles.map((tile, index) => {
      const model = this._layoutTileModel(tile, index, grid);
      if (tile.col && tile.row) {
        mark(model);
        return model;
      }
      for (let row = 1; row <= grid.rows; row += 1) {
        for (let col = 1; col <= grid.columns; col += 1) {
          model.col = col;
          model.row = row;
          if (!this._isOutsideGrid(model, grid) && !this._isOccupied(model, occupied)) {
            mark(model);
            return model;
          }
        }
      }
      model.col = 1;
      model.row = grid.rows + 1;
      return model;
    });
  }

  _isOccupied(tile, occupied) {
    for (let row = tile.row; row < tile.row + tile.rowSpan; row += 1) {
      for (let col = tile.col; col < tile.col + tile.colSpan; col += 1) {
        if (occupied.has(`${col}:${row}`)) return true;
      }
    }
    return false;
  }

  _overlaps(a, b) {
    return a.col < b.col + b.colSpan && a.col + a.colSpan > b.col && a.row < b.row + b.rowSpan && a.row + a.rowSpan > b.row;
  }

  _blockingTiles(draft, area, excludeId = null) {
    return draft.tiles.filter((tile) => tile.id !== excludeId && this._overlaps(area, tile));
  }

  _inLayoutGrid(tile, grid) {
    return tile.col >= 1 && tile.row >= 1 && tile.col + tile.colSpan <= grid.columns + 1 && tile.row + tile.rowSpan <= grid.rows + 1;
  }

  _moveLayoutGroup(draft, blockers, ignoredIds, deltaCol, deltaRow, reserved = []) {
    const ignored = new Set([...ignoredIds, ...blockers.map((tile) => tile.id)]);
    const next = blockers.map((tile) => ({ tile, placement: { ...tile, col: tile.col + deltaCol, row: tile.row + deltaRow } }));
    const valid = next.every(({ placement }) => (
      this._inLayoutGrid(placement, draft.grid)
      && !reserved.some((area) => this._overlaps(placement, area))
      && !draft.tiles.some((other) => !ignored.has(other.id) && this._overlaps(placement, other))
    ));
    if (!valid) return false;
    next.forEach(({ tile, placement }) => Object.assign(tile, placement));
    return true;
  }

  _findFreeLayoutSlot(draft, tile, ignoreIds = new Set(), reserved = []) {
    for (let row = 1; row <= draft.grid.rows + 1 - tile.rowSpan; row += 1) {
      for (let col = 1; col <= draft.grid.columns + 1 - tile.colSpan; col += 1) {
        const candidate = { ...tile, col, row };
        if (reserved.some((area) => this._overlaps(candidate, area))) continue;
        if (!draft.tiles.some((other) => !ignoreIds.has(other.id) && this._overlaps(candidate, other))) return candidate;
      }
    }
    return null;
  }

  _moveBlockersToFree(draft, blockers, reserved = []) {
    const originals = blockers.map((tile) => ({ tile, col: tile.col, row: tile.row, colSpan: tile.colSpan, rowSpan: tile.rowSpan }));
    const ignoreIds = new Set(blockers.map((tile) => tile.id));
    for (const tile of blockers) {
      const next = this._findFreeLayoutSlot(draft, tile, ignoreIds, reserved);
      if (!next) {
        originals.forEach((item) => Object.assign(item.tile, { col: item.col, row: item.row, colSpan: item.colSpan, rowSpan: item.rowSpan }));
        return false;
      }
      tile.col = next.col;
      tile.row = next.row;
      ignoreIds.delete(tile.id);
    }
    return true;
  }

  _layoutOccupied(draft, excludeId) {
    const occupied = new Set();
    for (const tile of draft.tiles) {
      if (tile.id === excludeId) continue;
      for (let row = tile.row; row < tile.row + tile.rowSpan; row += 1) {
        for (let col = tile.col; col < tile.col + tile.colSpan; col += 1) occupied.add(`${col}:${row}`);
      }
    }
    return occupied;
  }

  _layoutTileModel(tile, index, grid) {
    const preset = this._layoutPreset(tile);
    const size = tile.size || "large";
    const fallbackSpan = size === "large" ? { colSpan: 2, rowSpan: 2 } : { colSpan: 1, rowSpan: 1 };
    return {
      ...structuredClone(tile),
      col: Number(tile.col) || preset?.col || (index % grid.columns) + 1,
      row: Number(tile.row) || preset?.row || Math.floor(index / grid.columns) + 1,
      colSpan: Math.min(Number(tile.colSpan) || preset?.colSpan || fallbackSpan.colSpan, grid.columns),
      rowSpan: Math.min(Number(tile.rowSpan) || preset?.rowSpan || fallbackSpan.rowSpan, grid.rows),
    };
  }

  _layoutPreset(tile) {
    const text = `${tile.id || ""} ${tile.label || ""} ${tile.short_label || ""} ${tile.kind || ""}`.toLowerCase();
    if (text.includes("alarm")) return { col: 9, row: 1, colSpan: 2, rowSpan: 2 };
    if (text.includes("klimat")) return { col: 11, row: 1, colSpan: 2, rowSpan: 2 };
    if (text.includes("lampa")) return { col: 1, row: 3, colSpan: 4, rowSpan: 3 };
    if (text.includes("oświet") || text.includes("oswiet")) return { col: 5, row: 3, colSpan: 4, rowSpan: 3 };
    if (text.includes("rolet")) return { col: 9, row: 3, colSpan: 4, rowSpan: 3 };
    if (text.includes("hrj") || text.includes("kamera")) return { col: 1, row: 6, colSpan: 4, rowSpan: 3 };
    if (text.includes("brama")) return { col: 5, row: 6, colSpan: 4, rowSpan: 4 };
    if (text.includes("energia")) return { col: 9, row: 6, colSpan: 2, rowSpan: 2 };
    if (text.includes("config")) return { col: 11, row: 6, colSpan: 2, rowSpan: 2 };
    return null;
  }

  _aspectLabel(width, height) {
    const gcd = (a, b) => (b ? gcd(b, a % b) : a);
    const w = Math.max(1, Number(width) || 16);
    const h = Math.max(1, Number(height) || 9);
    const divisor = gcd(w, h);
    return `${w / divisor}:${h / divisor}`;
  }

  _layoutTile(tile, grid, selected) {
    const dragging = this._layoutDrag?.tileId === tile.id;
    const willRemove = tile.col > grid.columns || tile.row > grid.rows;
    const dragStyle = dragging && this._layoutDrag?.source === "grid" ? `transform:translate(${this._escape(this._layoutDrag.offsetX || 0)}px, ${this._escape(this._layoutDrag.offsetY || 0)}px);` : "";
    return `
      <button class="layout-cell-tile ${selected ? "selected" : ""} ${willRemove ? "will-remove" : ""} ${dragging ? "dragging" : ""} ${this._isOutsideGrid(tile, grid) ? "outside" : ""} accent-${this._escape(tile.accent || "orange")}" data-layout-select data-layout-drag data-tile="${this._escape(tile.id)}" style="grid-column:${this._escape(tile.col)} / span ${this._escape(tile.colSpan)};grid-row:${this._escape(tile.row)} / span ${this._escape(tile.rowSpan)};${dragStyle}">
        <span class="layout-tile-content">
          <ha-icon class="layout-tile-icon" icon="${this._escape(this._mdiIcon(tile.icon))}" ${tile.showIcon === false ? "hidden" : ""}></ha-icon>
          <strong class="layout-tile-title" ${tile.showTitle === false ? "hidden" : ""}>${this._escape(tile.label || tile.id)}</strong>
          <span class="layout-tile-subtitle" ${tile.showSubtitle === false ? "hidden" : ""}>${this._escape(tile.short_label || "")}</span>
        </span>
        ${[
          ["left", "w"],
          ["right", "e"],
          ["top", "n"],
          ["bottom", "s"],
          ["corner top-left", "nw"],
          ["corner top-right", "ne"],
          ["corner bottom-left", "sw"],
          ["corner bottom-right", "se"],
        ].map(([className, edge]) => `<span class="layout-resize-handle ${className}" data-layout-resize-drag data-edge="${edge}" data-tile="${this._escape(tile.id)}"></span>`).join("")}
      </button>
    `;
  }

  _layoutTrayTile(device, tile) {
    return `
      <div class="tray-item ${this._layoutDrag?.tileId === tile.id ? "dragging" : ""}" data-layout-restore-drag data-tile="${this._escape(tile.id)}" style="--tray-aspect:${this._escape(tile.colSpan || 1)} / ${this._escape(tile.rowSpan || 1)};grid-column:span ${this._escape(Math.min(3, tile.colSpan || 1))}">
        <div class="tray-preview-tile accent-${this._escape(tile.accent || "orange")}">
          <ha-icon icon="${this._escape(this._mdiIcon(tile.icon))}"></ha-icon>
          <strong>${this._escape(tile.label || tile.id)}</strong>
        </div>
      </div>
    `;
  }

  _layoutGhost(draft) {
    const drag = this._layoutDrag;
    const tile = drag ? (draft.tiles.find((item) => item.id === drag.tileId) || draft.tray.find((item) => item.id === drag.tileId)) : null;
    if (!tile || !drag.ghost || drag.toTray) return "";
    const ghost = { ...tile, ...drag.ghost };
    return `<div class="layout-cell-tile layout-ghost ${drag.ghost.valid ? "" : "invalid"}" style="grid-column:${this._escape(ghost.col)} / span ${this._escape(ghost.colSpan)};grid-row:${this._escape(ghost.row)} / span ${this._escape(ghost.rowSpan)}"><strong>${this._escape(tile.short_label || tile.label || tile.id)}</strong><span class="preview-meta">${drag.copy ? "Kopia · " : ""}${this._escape(ghost.colSpan)}×${this._escape(ghost.rowSpan)}</span></div>`;
  }

  _layoutSelectedPanel(tile) {
    const clockStyle = tile.clock_style || "classic";
    return `
      <div class="layout-panel tile-settings">
        <strong>Ustawienia kafla</strong>
        <div class="fields">
          ${this._inputField("layout-tile-title", "Nazwa", tile.label || tile.id, "text", "span-6")}
          ${this._inputField("layout-tile-subtitle", "Podpis", tile.short_label || "", "text", "span-6")}
          ${this._inputField("layout-tile-icon", "Ikona", this._mdiIcon(tile.icon), "text", "span-6")}
          <div class="layout-checks span-6" aria-label="Widoczne informacje kafla">
            <label><input id="layout-show-icon" type="checkbox" ${tile.showIcon === false ? "" : "checked"}> Ikona</label>
            <label><input id="layout-show-title" type="checkbox" ${tile.showTitle === false ? "" : "checked"}> Nazwa</label>
            <label><input id="layout-show-subtitle" type="checkbox" ${tile.showSubtitle === false ? "" : "checked"}> Podpis</label>
          </div>
          ${tile.kind === "clock" ? `
            ${this._selectField("layout-clock-style", "Styl zegara", CLOCK_STYLES, clockStyle, "span-6")}
            <div class="clock-style-options span-12">${this._clockStyleCards(clockStyle, true)}</div>
          ` : ""}
        </div>
        ${["folder", "popup"].includes(tile.kind) ? this._tileChildrenSection(this._selectedDevice, tile) : ""}
      </div>
    `;
  }

  _isOutsideGrid(tile, grid) {
    return tile.col < 1 || tile.row < 1 || tile.col + tile.colSpan - 1 > grid.columns || tile.row + tile.rowSpan - 1 > grid.rows;
  }

  _currentLayoutDraft() {
    const device = this._selectedDevice;
    const config = this._configs[device];
    return device && config ? this._layoutDraft(device, config) : null;
  }

  _updateLayoutGrid() {
    const draft = this._currentLayoutDraft();
    if (!draft) return;
    draft.grid.columns = Math.max(1, Number(this.shadowRoot.getElementById("layout-columns")?.value) || draft.grid.columns);
    draft.grid.rows = Math.max(1, Number(this.shadowRoot.getElementById("layout-rows")?.value) || draft.grid.rows);
    draft.grid.aspectWidth = Math.max(1, Number(this.shadowRoot.getElementById("layout-aspect-width")?.value) || draft.grid.aspectWidth);
    draft.grid.aspectHeight = Math.max(1, Number(this.shadowRoot.getElementById("layout-aspect-height")?.value) || draft.grid.aspectHeight);
    this._render();
  }

  _selectedLayoutTile() {
    const draft = this._currentLayoutDraft();
    return draft?.tiles.find((tile) => tile.id === draft.selectedTileId) || null;
  }

  _moveLayoutTile(dx, dy) {
    const draft = this._currentLayoutDraft();
    const tile = this._selectedLayoutTile();
    if (!tile) return;
    const next = { ...tile, col: Math.max(1, tile.col + dx), row: Math.max(1, tile.row + dy) };
    if (!draft || this._isOutsideGrid(next, draft.grid) || this._isOccupied(next, this._layoutOccupied(draft, tile.id))) return;
    tile.col = next.col;
    tile.row = next.row;
    this._render();
  }

  _resizeLayoutTile(dw, dh) {
    const draft = this._currentLayoutDraft();
    const tile = this._selectedLayoutTile();
    if (!tile) return;
    const next = { ...tile, colSpan: Math.max(1, tile.colSpan + dw), rowSpan: Math.max(1, tile.rowSpan + dh) };
    if (!draft || this._isOutsideGrid(next, draft.grid) || this._isOccupied(next, this._layoutOccupied(draft, tile.id))) return;
    tile.colSpan = next.colSpan;
    tile.rowSpan = next.rowSpan;
    this._render();
  }

  _layoutTileToTray() {
    const draft = this._currentLayoutDraft();
    const tile = this._selectedLayoutTile();
    if (!draft || !tile) return;
    draft.tiles = draft.tiles.filter((item) => item.id !== tile.id);
    draft.tray.push(tile);
    draft.selectedTileId = draft.tiles[0]?.id || null;
    this._render();
  }

  _restoreLayoutTile(tileId) {
    const draft = this._currentLayoutDraft();
    if (!draft) return;
    const tile = draft.tray.find((item) => item.id === tileId);
    if (!tile) return;
    draft.tray = draft.tray.filter((item) => item.id !== tileId);
    tile.col = 1;
    tile.row = 1;
    draft.tiles.push(tile);
    draft.selectedTileId = tile.id;
    this._render();
  }

  _addLayoutDraftTile(type = "ha", panelKind = "clock") {
    const draft = this._currentLayoutDraft();
    if (!draft) return;
    const tile = this._newLayoutTile(type, panelKind);
    if (draft.context?.panelId) tile.panel_id = draft.context.panelId;
    Object.assign(tile, this._findFreeLayoutSlot(draft, tile, new Set(), []) || { col: 1, row: draft.grid.rows + 1 });
    draft.tiles.push(tile);
    draft.selectedTileId = tile.id;
    this._render();
  }

  _newLayoutTile(type, panelKind) {
    const stamp = Date.now();
    if (type !== "panel") {
      return { id: `ha_${stamp}`, kind: "entity", size: "small", label: "Kafel Home Assistant", short_label: "", entity_id: "", icon: "mdi:cog", accent: "orange", colSpan: 1, rowSpan: 1 };
    }
    const kind = PANEL_TILE_KINDS.includes(panelKind) ? panelKind : "clock";
    const templates = {
      clock: { id: `clock_${stamp}`, kind: "clock", size: "large", label: "Zegar", icon: "mdi:clock-outline", accent: "white", colSpan: 3, rowSpan: 2, clock_style: "classic" },
      folder: { id: `folder_${stamp}`, kind: "folder", size: "large", label: "Folder", short_label: "Folder", panel_id: `panel_${stamp}`, icon: "mdi:folder", accent: "orange", colSpan: 2, rowSpan: 2 },
      popup: { id: `popup_${stamp}`, kind: "popup", size: "large", label: "Popup", short_label: "Popup", panel_id: `popup_${stamp}`, icon: "mdi:view-grid-plus", accent: "orange", colSpan: 2, rowSpan: 2 },
    };
    return templates[kind];
  }

  _uniqueTileId(base, draft) {
    const slug = String(base || "tile").replace(/[^a-zA-Z0-9_-]/g, "_").replace(/^_+|_+$/g, "") || "tile";
    const used = new Set([...draft.tiles, ...draft.tray].flatMap((tile) => [tile.id, tile.panel_id].filter(Boolean)));
    for (let i = 1; i < 1000; i += 1) {
      const id = `${slug}_copy${i === 1 ? "" : i}`;
      if (!used.has(id)) return id;
    }
    return `${slug}_copy_${Date.now()}`;
  }

  _panelTileKindLabel(kind) {
    return ({ clock: "Zegar", folder: "Folder / następny panel", popup: "Popup" }[kind] || kind);
  }

  _panelTileKindDescription(kind) {
    return ({
      clock: "Duży kafel zegara na panelu.",
      folder: "Otwiera następny panel po panel_id.",
      popup: "Otwiera popup z kaflami o tym samym panel_id.",
    }[kind] || "Kafel panelu");
  }

  _openPanelTilePicker(target, device = null, surface = "dashboard") {
    this._panelTilePicker = { target, device, surface };
    this._render();
  }

  _closePanelTilePicker() {
    this._panelTilePicker = null;
    this._render();
  }

  _choosePanelTile(kind) {
    const picker = this._panelTilePicker;
    this._panelTilePicker = null;
    if (!picker) return;
    if (picker.target === "layout") this._addLayoutDraftTile("panel", kind);
    else this._addTile(picker.device, picker.surface, "panel", kind);
  }

  _panelTilePickerView() {
    return `
      <dialog class="studio-dialog panel-tile-picker" id="panel-tile-picker-dialog">
        <button class="iconbtn secondary" id="close-panel-tile-picker" style="float:right">×</button>
        <h2>Dodaj kafel panelu</h2>
        <div class="sub">Wybierz typ kafla. Podgląd pokazuje domyślny wygląd i zachowanie.</div>
        <div class="panel-tile-options">
          ${PANEL_TILE_KINDS.map((kind) => this._panelTileOption(kind)).join("")}
        </div>
      </dialog>
    `;
  }

  _panelTileOption(kind) {
    const tile = this._newLayoutTile("panel", kind);
    return `
      <button class="panel-tile-option" data-choose-panel-tile="${this._escape(kind)}">
        <span class="panel-tile-preview accent-${this._escape(tile.accent || "orange")}">
          <ha-icon icon="${this._escape(this._mdiIcon(tile.icon))}"></ha-icon>
          <strong>${this._escape(tile.label)}</strong>
          <small>${this._escape(tile.panel_id || kind)}</small>
        </span>
        <span><strong>${this._escape(this._panelTileKindLabel(kind))}</strong><small>${this._escape(this._panelTileKindDescription(kind))}</small></span>
      </button>
    `;
  }

  _clockStyleLabel(style) {
    return ({ classic: "Klasyczny", compact: "Kompakt", date_top: "Data u góry" }[style] || style);
  }

  _clockStyleCards(selected, live = false) {
    return CLOCK_STYLES.map((style) => `
      <button type="button" class="clock-style-card ${style === selected ? "active" : ""}" ${live ? `data-clock-style="${this._escape(style)}"` : ""}>
        ${style === "date_top" ? `<small>środa, 01 lipca</small><strong>12:34</strong>` : `<strong>${style === "compact" ? "12:34" : "12:34"}</strong><small>środa, 01 lipca</small>`}
        <small>${this._escape(this._clockStyleLabel(style))}</small>
      </button>
    `).join("");
  }

  _syncSelectedLayoutTile() {
    const tile = this._selectedLayoutTile();
    if (!tile) return;
    tile.label = this.shadowRoot.getElementById("layout-tile-title")?.value || tile.label;
    tile.short_label = this.shadowRoot.getElementById("layout-tile-subtitle")?.value || "";
    tile.icon = this.shadowRoot.getElementById("layout-tile-icon")?.value || "mdi:cog";
    tile.showIcon = !!this.shadowRoot.getElementById("layout-show-icon")?.checked;
    tile.showTitle = !!this.shadowRoot.getElementById("layout-show-title")?.checked;
    tile.showSubtitle = !!this.shadowRoot.getElementById("layout-show-subtitle")?.checked;
    const clockStyle = this.shadowRoot.getElementById("layout-clock-style")?.value;
    if (clockStyle) tile.clock_style = clockStyle;
    const element = this.shadowRoot.querySelector(`[data-layout-select][data-tile="${CSS.escape(tile.id)}"]`);
    element?.querySelector(".layout-tile-icon")?.setAttribute("icon", this._mdiIcon(tile.icon));
    const title = element?.querySelector(".layout-tile-title");
    const subtitle = element?.querySelector(".layout-tile-subtitle");
    if (title) {
      title.textContent = tile.label || tile.id;
      title.hidden = tile.showTitle === false;
    }
    const icon = element?.querySelector(".layout-tile-icon");
    if (icon) icon.hidden = tile.showIcon === false;
    if (subtitle) {
      subtitle.textContent = tile.short_label || "";
      subtitle.hidden = tile.showSubtitle === false;
    }
  }

  _syncLayoutTrayHeight() {
    const frame = this.shadowRoot.querySelector(".layout-frame");
    const tray = this.shadowRoot.querySelector("[data-layout-tray-zone]");
    if (!frame || !tray) return;
    tray.style.height = `${frame.getBoundingClientRect().height}px`;
  }

  _clearLayoutTray() {
    const draft = this._currentLayoutDraft();
    if (!draft || !draft.tray.length || !window.confirm("Na pewno wyczyścić zasobnik?")) return;
    draft.tray.splice(0, draft.tray.length);
    this._render();
  }

  _flashLayoutTray() {
    this._layoutTrayFlash = true;
    window.setTimeout(() => {
      this._layoutTrayFlash = false;
      this._render();
    }, 260);
  }

  _applyLayoutGrid() {
    const draft = this._currentLayoutDraft();
    if (!draft) return;
    const inside = [];
    for (const tile of draft.tiles) {
      if (tile.col > draft.grid.columns || tile.row > draft.grid.rows) {
        draft.tray.push(tile);
        continue;
      }
      tile.colSpan = Math.min(tile.colSpan, draft.grid.columns - tile.col + 1);
      tile.rowSpan = Math.min(tile.rowSpan, draft.grid.rows - tile.row + 1);
      inside.push(tile);
    }
    draft.tiles = inside;
    draft.selectedTileId = draft.tiles[0]?.id || null;
    this._render();
  }

  _applyPanelPreset() {
    const draft = this._currentLayoutDraft();
    if (!draft) return;
    const resolution = this._tabletResolution(this._selectedDevice);
    draft.grid = { columns: 12, rows: 9, aspectWidth: resolution?.width || 16, aspectHeight: resolution?.height || 9 };
    const used = new Set();
    draft.tiles.forEach((tile) => {
      const preset = this._layoutPreset(tile) || { col: 9, row: 8, colSpan: 2, rowSpan: 2 };
      Object.assign(tile, preset);
      if (this._isOutsideGrid(tile, draft.grid) || this._isOccupied(tile, used)) Object.assign(tile, this._firstFreeSlot(tile, draft.grid, used));
      for (let row = tile.row; row < tile.row + tile.rowSpan; row += 1) {
        for (let col = tile.col; col < tile.col + tile.colSpan; col += 1) used.add(`${col}:${row}`);
      }
    });
    draft.selectedTileId = draft.tiles[0]?.id || null;
    this._render();
  }

  _firstFreeSlot(tile, grid, occupied) {
    const candidate = { ...tile, colSpan: Math.min(tile.colSpan, grid.columns), rowSpan: Math.min(tile.rowSpan, grid.rows) };
    for (let row = 1; row <= grid.rows; row += 1) {
      for (let col = 1; col <= grid.columns; col += 1) {
        candidate.col = col;
        candidate.row = row;
        if (!this._isOutsideGrid(candidate, grid) && !this._isOccupied(candidate, occupied)) return candidate;
      }
    }
    return { ...candidate, col: 1, row: grid.rows + 1 };
  }

  _startLayoutDrag(event, tileId) {
    if (event.button !== 0) return;
    const draft = this._currentLayoutDraft();
    const tile = draft?.tiles.find((item) => item.id === tileId);
    if (!draft || !tile) return;
    event.preventDefault();
    draft.selectedTileId = tileId;
    const rect = event.currentTarget.getBoundingClientRect();
    const gridRect = this.shadowRoot.querySelector(".layout-grid")?.getBoundingClientRect();
    const grabCol = gridRect ? Math.min(tile.colSpan - 1, Math.max(0, Math.floor((event.clientX - rect.left) / (gridRect.width / draft.grid.columns)))) : 0;
    const grabRow = gridRect ? Math.min(tile.rowSpan - 1, Math.max(0, Math.floor((event.clientY - rect.top) / (gridRect.height / draft.grid.rows)))) : 0;
    this._layoutDrag = { mode: "move", source: "grid", tileId, startX: event.clientX, startY: event.clientY, offsetX: 0, offsetY: 0, grabCol, grabRow, copy: event.altKey, ghost: { col: tile.col, row: tile.row, colSpan: tile.colSpan, rowSpan: tile.rowSpan, valid: true }, toTray: false };
    const move = (moveEvent) => this._moveLayoutGhost(moveEvent);
    const up = () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
      window.removeEventListener("pointercancel", up);
      this._finishLayoutDrag();
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
    window.addEventListener("pointercancel", up);
    this._render();
  }

  _startLayoutTrayDrag(event, tileId) {
    if (event.button !== 0 || event.target.closest("button")) return;
    const draft = this._currentLayoutDraft();
    const tile = draft?.tray.find((item) => item.id === tileId);
    if (!draft || !tile) return;
    event.preventDefault();
    this._layoutDrag = { mode: "move", source: "tray", tileId, grabCol: 0, grabRow: 0, ghost: { col: tile.col || 1, row: tile.row || 1, colSpan: tile.colSpan || 1, rowSpan: tile.rowSpan || 1, valid: false }, toTray: false };
    const move = (moveEvent) => this._moveLayoutGhost(moveEvent);
    const up = () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
      window.removeEventListener("pointercancel", up);
      this._finishLayoutDrag();
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
    window.addEventListener("pointercancel", up);
    this._render();
  }

  _startLayoutResize(event, tileId, edge = "se") {
    if (event.button !== 0) return;
    const draft = this._currentLayoutDraft();
    const tile = draft?.tiles.find((item) => item.id === tileId);
    if (!draft || !tile) return;
    event.preventDefault();
    event.stopPropagation();
    draft.selectedTileId = tileId;
    this._layoutDrag = { mode: "resize", source: "grid", edge, tileId, ghost: { col: tile.col, row: tile.row, colSpan: tile.colSpan, rowSpan: tile.rowSpan, valid: true }, toTray: false };
    const move = (moveEvent) => this._moveLayoutGhost(moveEvent);
    const up = () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
      window.removeEventListener("pointercancel", up);
      this._finishLayoutDrag();
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
    window.addEventListener("pointercancel", up);
    this._render();
  }

  _moveLayoutGhost(event) {
    const draft = this._currentLayoutDraft();
    const drag = this._layoutDrag;
    const tile = drag ? (draft?.tiles.find((item) => item.id === drag.tileId) || draft?.tray.find((item) => item.id === drag.tileId)) : null;
    const gridEl = this.shadowRoot.querySelector(".layout-grid");
    if (!draft || !drag || !tile || !gridEl) return;
    const rect = gridEl.getBoundingClientRect();
    const trayRect = this.shadowRoot.querySelector("[data-layout-tray-zone]")?.getBoundingClientRect();
    drag.copy = drag.mode === "move" && drag.source === "grid" && event.altKey;
    drag.toTray = drag.source === "grid" && trayRect && event.clientX >= trayRect.left && event.clientX <= trayRect.right && event.clientY >= trayRect.top && event.clientY <= trayRect.bottom;
    if (drag.mode === "move" && drag.source === "grid") {
      drag.offsetX = event.clientX - drag.startX;
      drag.offsetY = event.clientY - drag.startY;
    }
    const inGrid = event.clientX >= rect.left && event.clientX <= rect.right && event.clientY >= rect.top && event.clientY <= rect.bottom;
    const rawCol = Math.floor(((event.clientX - rect.left) / rect.width) * draft.grid.columns) + 1;
    const rawRow = Math.floor(((event.clientY - rect.top) / rect.height) * draft.grid.rows) + 1;
    const col = Math.min(draft.grid.columns + 1 - tile.colSpan, Math.max(1, rawCol - (drag.grabCol || 0)));
    const row = Math.min(draft.grid.rows + 1 - tile.rowSpan, Math.max(1, rawRow - (drag.grabRow || 0)));
    const ghost = drag.mode === "resize" ? this._resizeGhost(tile, col, row, drag.edge || "se") : { ...tile, col, row };
    const copyHasRoom = !drag.copy || !this._blockingTiles(draft, ghost).length;
    drag.ghost = { col: ghost.col, row: ghost.row, colSpan: ghost.colSpan, rowSpan: ghost.rowSpan, valid: inGrid && !drag.toTray && !this._isOutsideGrid(ghost, draft.grid) && copyHasRoom };
    this._render();
  }

  _resizeGhost(tile, col, row, edge) {
    const right = tile.col + tile.colSpan - 1;
    const bottom = tile.row + tile.rowSpan - 1;
    const ghost = { ...tile };
    if (edge.includes("w")) {
      ghost.col = Math.min(col, right);
      ghost.colSpan = right - ghost.col + 1;
    }
    if (edge.includes("e")) ghost.colSpan = Math.max(1, col - tile.col + 1);
    if (edge.includes("n")) {
      ghost.row = Math.min(row, bottom);
      ghost.rowSpan = bottom - ghost.row + 1;
    }
    if (edge.includes("s")) ghost.rowSpan = Math.max(1, row - tile.row + 1);
    return ghost;
  }

  _finishLayoutDrag() {
    const draft = this._currentLayoutDraft();
    const drag = this._layoutDrag;
    const tile = drag ? (draft?.tiles.find((item) => item.id === drag.tileId) || draft?.tray.find((item) => item.id === drag.tileId)) : null;
    if (draft && drag?.toTray && !drag.copy && drag.source === "grid" && tile) {
      draft.tiles = draft.tiles.filter((item) => item.id !== tile.id);
      draft.tray.push(tile);
      draft.selectedTileId = draft.tiles[0]?.id || null;
    } else if (draft && drag?.copy && drag.mode === "move" && drag.source === "grid" && drag.ghost?.valid && tile) {
      const copy = structuredClone(tile);
      copy.id = this._uniqueTileId(tile.id, draft);
      if (copy.label) copy.label = `${copy.label} kopia`;
      if (["folder", "popup"].includes(copy.kind)) copy.panel_id = this._uniqueTileId(copy.panel_id || copy.id, draft);
      copy.col = drag.ghost.col;
      copy.row = drag.ghost.row;
      copy.colSpan = drag.ghost.colSpan;
      copy.rowSpan = drag.ghost.rowSpan;
      delete copy.order;
      draft.tiles.push(copy);
      draft.selectedTileId = copy.id;
    } else if (draft && drag?.ghost?.valid && tile) {
      const target = { ...tile, ...drag.ghost };
      const blockers = this._blockingTiles(draft, target, tile.id);
      const shifted = drag.mode === "move" && blockers.length && this._moveLayoutGroup(draft, blockers, new Set([tile.id]), tile.col - target.col, tile.row - target.row, [target]);
      if (drag.mode === "move" && blockers.length && !shifted && !this._moveBlockersToFree(draft, blockers, [target])) {
        if (drag.source === "grid") {
          draft.tiles = draft.tiles.filter((item) => !blockers.includes(item));
          draft.tray.push(...blockers);
        } else {
          this._flashLayoutTray();
          this._layoutDrag = null;
          this._render();
          return;
        }
      }
      tile.col = drag.ghost.col;
      tile.row = drag.ghost.row;
      tile.colSpan = drag.ghost.colSpan;
      tile.rowSpan = drag.ghost.rowSpan;
      if (drag.mode === "resize") {
        const covered = this._blockingTiles(draft, tile, tile.id);
        draft.tiles = draft.tiles.filter((item) => !covered.includes(item));
        draft.tray.push(...covered);
      }
      if (drag.source === "tray") {
        draft.tray.splice(draft.tray.findIndex((item) => item.id === tile.id), 1);
        tile.panel_id = draft.context?.id === "main" ? (["folder", "popup"].includes(tile.kind) ? tile.panel_id : "") : draft.context.panelId;
        draft.tiles.push(tile);
      }
      draft.selectedTileId = tile.id;
    }
    this._layoutDrag = null;
    this._render();
  }

  _resetLayoutDraft(device) {
    const config = this._configs[device];
    if (!config) return;
    Object.keys(this._layoutDrafts).filter((key) => key.startsWith(`${device}:${config.revision ?? "new"}:`)).forEach((key) => delete this._layoutDrafts[key]);
    this._render();
  }

  async _saveLayout(device) {
    const config = this._configs[device];
    const draft = this._layoutDraft(device, config);
    if (!config || !draft) return;
    const next = structuredClone(config);
    next.layout_editor = {
      ...(next.layout_editor || {}),
    };
    const tray = this._layoutSharedTrayDraft(device, config, draft.grid);
    if (draft.context?.id === "main") {
      next.layout_editor.grid = structuredClone(draft.grid);
    } else {
      next.layout_editor.contexts = { ...(next.layout_editor.contexts || {}) };
      next.layout_editor.contexts[draft.context.panelId] = {
        ...(next.layout_editor.contexts[draft.context.panelId] || {}),
        grid: structuredClone(draft.grid),
      };
    }
    next.layout_editor.tray = tray.map((tile, index) => ({ ...structuredClone(tile), order: index }));
    const savedIds = new Set(draft.tiles.map((tile) => tile.id));
    const kept = (next.tiles || []).filter((tile) => !savedIds.has(tile.id) && !tray.some((trayTile) => trayTile.id === tile.id));
    next.tiles = [...kept, ...draft.tiles.map((tile, index) => ({ ...structuredClone(tile), order: index }))];
    await this._setConfig(device, next);
  }

  _previewTile(device, tile) {
    return `
      <div class="preview-tile preview-${this._escape(tile.size || "small")} ${tile.kind === "camera" ? "preview-camera" : ""} accent-${this._escape(tile.accent || "orange")}">
        <select class="preview-size" data-preview-size data-device="${this._escape(device)}" data-tile="${this._escape(tile.id)}">
          ${TILE_SIZES.map((size) => `<option value="${this._escape(size)}" ${size === (tile.size || "large") ? "selected" : ""}>${this._escape(size)}</option>`).join("")}
        </select>
        <ha-icon icon="${this._escape(this._mdiIcon(tile.icon))}"></ha-icon>
        <strong>${this._escape(tile.short_label || tile.label || tile.id)}</strong>
        <span class="preview-meta">${this._escape(tile.entity_id || tile.kind || "-")}</span>
        ${tile.kind === "camera" ? `<div class="preview-actions"><button class="preview-action-pill">Wyłącz kamery</button><button class="preview-action-pill">Włącz kamery</button></div>` : ""}
      </div>
    `;
  }

  async _setTileSize(device, tileId, size) {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    if (!config || !Number.isFinite(revision)) return;
    await this._hass.callService("hapanels", "patch_dashboard_config", {
      device,
      patch: {
        base_revision: revision,
        updated_by: "homeassistant:hapanels_studio",
        surface: "dashboard",
        tile_updates: [{ id: tileId, size }],
      },
    });
    window.setTimeout(() => this._load(), 800);
  }

  _tabletInfoView(config, panel = null) {
    const grid = config.layout_editor?.grid || {};
    const fallbackResolution = this._fallbackScreenResolution();
    const physicalWidth = Number(panel?.screen_width_px ?? fallbackResolution?.width);
    const physicalHeight = Number(panel?.screen_height_px ?? fallbackResolution?.height);
    const width = Number.isFinite(physicalWidth) ? physicalWidth : Number(grid.aspectWidth) || 16;
    const height = Number.isFinite(physicalHeight) ? physicalHeight : Number(grid.aspectHeight) || 9;
    const resolutionSource = Number.isFinite(physicalWidth) && Number.isFinite(physicalHeight) ? "tablet" : "projekt layoutu";
    return `
      <dl>
        <dt>Dashboard ID</dt><dd>${this._escape(config.dashboard_id || "-")}</dd>
        <dt>Rewizja</dt><dd>${this._escape(config.revision ?? "-")}</dd>
        <dt>Ostatnia zmiana</dt><dd>${this._escape(config.updated_by || "-")}</dd>
        <dt>Rozdzielczość</dt><dd>${this._escape(width)}×${this._escape(height)} <span class="sub">(${this._escape(resolutionSource)})</span></dd>
        <dt>Proporcje</dt><dd>${this._escape(this._aspectLabel(width, height))}</dd>
        <dt>Grid</dt><dd>${this._escape(grid.columns || 12)}×${this._escape(grid.rows || 9)}</dd>
      </dl>
    `;
  }

  _fallbackScreenResolution() {
    const state = Object.entries(this._hass?.states || {}).find(([entityId, item]) =>
      entityId.startsWith("sensor.hapanels_") && entityId.endsWith("_screen_resolution") && /^\d+x\d+$/i.test(item?.state || ""),
    )?.[1]?.state;
    if (!state) return null;
    const [width, height] = state.toLowerCase().split("x").map(Number);
    return Number.isFinite(width) && Number.isFinite(height) ? { width, height } : null;
  }

  _tabletResolution(device) {
    const panel = (this._panels || []).find((item) => item.device === device);
    const width = Number(panel?.screen_width_px);
    const height = Number(panel?.screen_height_px);
    if (Number.isFinite(width) && Number.isFinite(height)) return { width, height };
    return this._fallbackScreenResolution();
  }

  _tileEditor(device, tile, surface) {
    const prefix = `tile-${surface}-${device}-${tile.id}`.replace(/[^a-zA-Z0-9_-]/g, "-");
    const entityLabel = tile.entity_id ? this._entityLabel(tile.entity_id) : (tile.kind || "-");
    const tileLabel = tile.short_label || tile.label || tile.id;
    const accent = tile.accent || "orange";
    const collapsedKey = `${surface}:${device}:${tile.id}`;
    const focused = this._focusedTileId === tile.id;
    const collapsed = !focused && !this._expandedTiles.has(collapsedKey);
    const childSection = surface === "dashboard" && ["folder", "popup"].includes(tile.kind) ? this._tileChildrenSection(device, tile) : "";
    const clockSection = tile.kind === "clock" ? `
      <section class="tile-section">
        <h3>Zegar</h3>
        <div class="fields">
          ${this._selectField(`${prefix}-clockStyle`, "Styl", CLOCK_STYLES, tile.clock_style || "classic", "span-6", "clock")}
          <div class="clock-style-options span-12">${this._clockStyleCards(tile.clock_style || "classic")}</div>
        </div>
      </section>
    ` : "";
    const coverSection = tile.kind === "cover" ? `
      <section class="tile-section">
        <h3>Cover</h3>
        <div class="fields">
          ${this._selectField(`${prefix}-coverVisual`, "Wygląd", COVER_VISUALS, tile.cover_visual || "blind", "span-6", "cover")}
          ${this._selectField(`${prefix}-coverDirection`, "Otwieranie", COVER_DIRECTIONS, tile.cover_direction || "top", "span-6", "cover")}
          <div class="cover-preview span-12 cover-${this._escape(tile.cover_visual || "blind")} cover-${this._escape(tile.cover_direction || "top")}">
            <div class="cover-scene"><span></span><span></span></div>
            <strong>${this._escape(this._coverVisualLabel(tile.cover_visual || "blind"))}</strong>
            <small>${this._escape(this._coverDirectionLabel(tile.cover_direction || "top"))}</small>
          </div>
        </div>
      </section>
    ` : "";
    return `
      <div class="tile ${collapsed ? "collapsed" : ""} ${focused ? "focused" : ""}" data-tile-editor data-prefix="${this._escape(prefix)}" data-tile-id="${this._escape(tile.id)}" data-collapsed-key="${this._escape(collapsedKey)}" style="--tile-accent:${this._escape(this._accentColor(accent))}">
        <div class="tile-head">
          <div class="tile-title">
            <span>${this._escape(tileLabel)}</span>
            <small>${this._escape(tile.id)}</small>
            <button class="secondary" title="Edytuj nazwę">✎</button>
          </div>
          <div class="tile-summary">
            <span>${this._escape(tile.kind || "tile")}</span><span>·</span>
            <span>${this._escape(tile.size || "-")}</span><span>·</span>
            <span>order ${this._escape(tile.order ?? 0)}</span><span>·</span>
            <span>${this._escape(entityLabel)}</span>
            <button class="tile-toggle secondary" data-toggle-tile title="Zwiń/rozwiń"><span>⌃</span></button>
          </div>
        </div>
        <div class="tile-expandable">
          <div class="tile-expandable-inner">
            <div class="tile-body">
              <aside class="tile-preview-pane">
                <h3>Podgląd kafla</h3>
                <div class="tile-preview-box accent-${this._escape(accent)}">
                  <div>
                    <ha-icon class="tile-live-icon tile-preview-target" data-highlight-target="icon" icon="${this._escape(this._mdiIcon(tile.icon))}"></ha-icon>
                    <strong class="tile-live-label tile-preview-target" data-highlight-target="label">${this._escape(tile.short_label || tile.label || tile.id)}</strong>
                    <span class="tile-live-entity tile-preview-target" data-highlight-target="entity">${this._escape(entityLabel)}</span>
                  </div>
                </div>
                <div class="tile-preview-meta">
                  <div class="tile-preview-target" data-highlight-target="kind"><span>○</span><span>Typ<strong class="tile-live-kind">${this._escape(tile.kind || "-")}</strong></span></div>
                  <div class="tile-preview-target" data-highlight-target="size"><span>▣</span><span>Rozmiar<strong class="tile-live-size">${this._escape(tile.size || "-")}</strong></span></div>
                  <div class="tile-preview-target" data-highlight-target="accent"><span>●</span><span>Accent<strong class="tile-live-accent">${this._escape(accent)}</strong></span></div>
                </div>
              </aside>
              <div class="tile-form">
                <section class="tile-section">
                  <h3>Podstawowe</h3>
                  <div class="fields">
                    ${this._inputField(`${prefix}-label`, "Label", tile.label || tile.id, "text", "span-6", "label")}
                    ${this._entityField(`${prefix}-entity`, "Źródło", tile.entity_id || "", "entity", "span-6", tile.kind === "cover" ? ["cover"] : null)}
                    ${this._inputField(`${prefix}-short`, "Short (opcjonalnie)", tile.short_label || "", "text", "span-6", "label")}
                  </div>
                </section>
                <section class="tile-section">
                  <h3>Wygląd</h3>
                  <div class="fields">
                    ${this._selectField(`${prefix}-kind`, "Typ", TILE_KINDS, tile.kind || "entity", "span-3", "kind")}
                    ${this._selectField(`${prefix}-size`, "Rozmiar", TILE_SIZES, tile.size || "large", "span-3", "size")}
                    ${this._iconField(`${prefix}-icon`, "Ikona (MDI)", this._mdiIcon(tile.icon), accent, "icon", "span-6")}
                    ${this._selectField(`${prefix}-accent`, "Accent (kolor)", TILE_ACCENTS, accent, "span-4", "accent")}
                  </div>
                </section>
                <section class="tile-section">
                  <h3>Zaawansowane</h3>
                  <div class="fields">
                    ${this._inputField(`${prefix}-panel`, "Panel (opcjonalnie)", tile.panel_id || "", "text", "span-6", "panel")}
                    ${this._inputField(`${prefix}-order`, "Order", tile.order ?? 0, "number", "span-6", "order")}
                    ${this._inputField(`${prefix}-col`, "Kolumna", tile.col ?? "", "number", "span-3", "layout")}
                    ${this._inputField(`${prefix}-row`, "Wiersz", tile.row ?? "", "number", "span-3", "layout")}
                    ${this._inputField(`${prefix}-colSpan`, "Szerokość", tile.colSpan ?? "", "number", "span-3", "layout")}
                    ${this._inputField(`${prefix}-rowSpan`, "Wysokość", tile.rowSpan ?? "", "number", "span-3", "layout")}
                  </div>
                </section>
                ${clockSection}
                ${coverSection}
                ${childSection}
              </div>
            </div>
            <div class="tile-footer">
              <button class="small danger" data-delete-tile data-device="${this._escape(device)}" data-surface="${surface}" data-tile="${this._escape(tile.id)}">Usuń</button>
              <div class="tile-actions">
                <button class="small" data-save-tile data-device="${this._escape(device)}" data-surface="${surface}" data-tile="${this._escape(tile.id)}" data-prefix="${this._escape(prefix)}">Zapisz</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _tileChildrenSection(device, tile) {
    const panelId = tile.panel_id || "";
    const children = (this._configs[device]?.tiles || []).filter((item) => item.id !== tile.id && item.panel_id === panelId && !["folder", "popup"].includes(item.kind));
    return `
      <section class="tile-section">
        <h3>Zawartość ${this._escape(tile.kind === "popup" ? "popupu" : "folderu")}</h3>
        <div class="sub">Kafle z Panel ID: ${this._escape(panelId || "ustaw panel_id i zapisz")}</div>
        <div class="child-tiles">
          ${children.length ? children.map((child) => `
            <div class="child-tile">
              <span><strong>${this._escape(child.label || child.id)}</strong><small>${this._escape(child.kind || "entity")} · ${this._escape(child.entity_id || child.id)}</small></span>
              <button class="small secondary" data-focus-child-tile="${this._escape(child.id)}">Edytuj</button>
            </div>
          `).join("") : `<span class="sub">Brak kafli. Dodaj pierwszy kafel Home Assistant.</span>`}
        </div>
        <div class="tile-actions" style="margin-top:12px">
          <button class="small" data-add-child-ha data-device="${this._escape(device)}" data-panel-id="${this._escape(panelId)}" ${panelId ? "" : "disabled"}>Dodaj kafel HA do zawartości +</button>
        </div>
      </section>
    `;
  }

  _inputField(id, label, value, type = "text", span = "span-3", fieldKey = "") {
    return `<div class="field ${span}" ${fieldKey ? `data-field-key="${this._escape(fieldKey)}"` : ""}><label for="${this._escape(id)}">${this._escape(label)}</label><input id="${this._escape(id)}" type="${type}" value="${this._escape(value)}"></div>`;
  }

  _entityField(id, label, selected, fieldKey = "", span = "span-4", domains = null) {
    const domainAttr = Array.isArray(domains) && domains.length ? ` data-domains="${this._escape(domains.join(","))}"` : "";
    if (customElements.get("ha-entity-picker")) {
      return `<div class="field ${this._escape(span)}" ${fieldKey ? `data-field-key="${this._escape(fieldKey)}"` : ""}><label for="${this._escape(id)}">${this._escape(label)}</label><ha-entity-picker id="${this._escape(id)}" value="${this._escape(selected)}" allow-custom-entity${domainAttr}></ha-entity-picker></div>`;
    }
    return `<div class="field ${this._escape(span)}" ${fieldKey ? `data-field-key="${this._escape(fieldKey)}"` : ""}><label for="${this._escape(id)}">${this._escape(label)}</label><input id="${this._escape(id)}" value="${this._escape(selected)}" placeholder="${domains?.[0] || "light"}.kitchen"></div>`;
  }

  _selectField(id, label, options, selected, span = "span-3", fieldKey = "") {
    return `<div class="field ${span}" ${fieldKey ? `data-field-key="${this._escape(fieldKey)}"` : ""}><label for="${this._escape(id)}">${this._escape(label)}</label><select id="${this._escape(id)}">${options.map((option) => `<option value="${this._escape(option)}" ${option === selected ? "selected" : ""}>${this._escape(option)}</option>`).join("")}</select></div>`;
  }

  _iconField(id, label, selected, accent = "orange", fieldKey = "", span = "span-4") {
    const accentClass = `accent-${this._escape(accent)}`;
    return `<div class="field ${this._escape(span)}" ${fieldKey ? `data-field-key="${this._escape(fieldKey)}"` : ""}><label for="${this._escape(id)}">${this._escape(label)}</label><div class="icon-field ${accentClass}"><div class="icon-preview ${accentClass}"><ha-icon icon="${this._escape(selected)}"></ha-icon></div>${customElements.get("ha-icon-picker") ? `<ha-icon-picker id="${this._escape(id)}" value="${this._escape(selected)}"></ha-icon-picker>` : `<input id="${this._escape(id)}" value="${this._escape(selected)}" placeholder="mdi:cog">`}</div></div>`;
  }

  _mdiIcon(icon) {
    if (!icon) return "mdi:cog";
    return icon.startsWith("mdi:") ? icon : `mdi:${icon.replaceAll("_", "-")}`;
  }

  _coverVisualLabel(value) {
    return ({ blind: "Roleta", shade: "Żaluzja", curtain: "Zasłony", gate: "Brama" }[value] || value);
  }

  _coverDirectionLabel(value) {
    return ({ top: "Od góry", bottom: "Od dołu", left: "Od lewej", right: "Od prawej", top_left: "Z góry/lewej", top_right: "Z góry/prawej", bottom_left: "Z dołu/lewej", bottom_right: "Z dołu/prawej" }[value] || value);
  }

  _statusLabel(status) {
    return ({ synced: "ZSYNCHRONIZOWANE", conflict: "KONFLIKT", invalid: "BŁĄD", unknown: "NIEZNANY" }[status] || this._escape(status || "unknown"));
  }

  _accentColor(accent) {
    return ({ orange: "#e99900", red: "#ff5338", white: "#f1f1f1" }[accent] || "#e99900");
  }

  _settingsView() {
    return `
      <dialog class="studio-dialog narrow" id="settings-dialog">
        <section class="settings-panel">
          <div class="name"><span>Ustawienia Studio</span><button id="close-settings" class="secondary small">Zamknij</button></div>
          <div class="settings-row">
            <label>Motyw</label>
            <select id="theme-mode">
              <option value="auto" ${this._themeMode === "auto" ? "selected" : ""}>Auto z HA</option>
              <option value="light" ${this._themeMode === "light" ? "selected" : ""}>Jasny</option>
              <option value="dark" ${this._themeMode === "dark" ? "selected" : ""}>Ciemny</option>
            </select>
          </div>
          <div class="settings-row">
            <button id="add-tablet">Dodaj tablet</button>
            <button id="show-hidden" class="secondary">Pokaż ukryte tablety</button>
          </div>
          <p class="sub">Tablety dodają się automatycznie po uruchomieniu aplikacji Hapanels z MQTT. Usunięcie w Studio ukrywa tablet lokalnie, nie kasuje urządzenia w HA.</p>
        </section>
      </dialog>
    `;
  }

  _tabletPickerView() {
    const panels = this._pickerPanels();
    return `
      <dialog class="studio-dialog" id="tablet-picker-dialog">
        <section class="settings-panel">
          <div class="name"><span>Wybierz wykryty tablet</span><button id="close-tablet-picker" class="secondary small">Zamknij</button></div>
          <div class="picker-filters">
            <button class="secondary small ${this._tabletPickerFilter === "active" ? "active" : ""}" data-picker-filter="active">Aktywne</button>
            <button class="secondary small ${this._tabletPickerFilter === "hidden" ? "active" : ""}" data-picker-filter="hidden">Ukryte</button>
            <button class="secondary small ${this._tabletPickerFilter === "all" ? "active" : ""}" data-picker-filter="all">Wszystkie</button>
          </div>
          ${panels.length ? `<div class="tablet-list">${panels.map((panel) => this._panelCard(panel)).join("")}</div>` : `<p class="sub">Brak wykrytych tabletów. Uruchom aplikację Hapanels z MQTT.</p>`}
        </section>
      </dialog>
    `;
  }

  _confirmDialog() {
    return `
      <dialog class="studio-dialog narrow" id="confirm-dialog">
        <section class="settings-panel">
          <div class="name"><span>${this._escape(this._confirm.title)}</span><button id="cancel-confirm-x" class="secondary small">Zamknij</button></div>
          <p class="sub">${this._escape(this._confirm.message)}</p>
          <div class="actions">
            <button id="confirm-action" class="danger">Tak</button>
            <button id="cancel-confirm" class="secondary">Anuluj</button>
          </div>
        </section>
      </dialog>
    `;
  }

  _showTabletPicker() {
    this._settingsOpen = false;
    this._tabletPickerOpen = true;
    this._tabletPickerFilter = "all";
    this._render();
  }

  _entityLabel(id) {
    return this._hass?.states?.[id]?.attributes?.friendly_name || id;
  }

  _bindEvents() {
    this._bindDialog("settings-dialog", () => { this._settingsOpen = false; this._render(); });
    this._bindDialog("tablet-picker-dialog", () => { this._tabletPickerOpen = false; this._render(); });
    this._bindDialog("confirm-dialog", () => this._closeConfirm());
    this._bindDialog("panel-tile-picker-dialog", () => this._closePanelTilePicker());
    this.shadowRoot.getElementById("refresh")?.addEventListener("click", () => this._load());
    this.shadowRoot.getElementById("settings")?.addEventListener("click", () => { this._settingsOpen = true; this._render(); });
    this.shadowRoot.getElementById("close-settings")?.addEventListener("click", () => { this._settingsOpen = false; this._render(); });
    this.shadowRoot.getElementById("theme-mode")?.addEventListener("change", (event) => this._setThemeMode(event.target.value));
    this.shadowRoot.getElementById("add-tablet")?.addEventListener("click", () => this._showTabletPicker());
    this.shadowRoot.getElementById("first-tablet")?.addEventListener("click", () => this._showTabletPicker());
    this.shadowRoot.getElementById("close-tablet-picker")?.addEventListener("click", () => { this._tabletPickerOpen = false; this._render(); });
    this.shadowRoot.getElementById("show-hidden")?.addEventListener("click", () => this._showAllDevices());
    this.shadowRoot.querySelectorAll("[data-picker-filter]").forEach((button) => {
      button.addEventListener("click", () => { this._tabletPickerFilter = button.dataset.pickerFilter; this._render(); });
    });
    this.shadowRoot.getElementById("confirm-action")?.addEventListener("click", () => this._confirmAction());
    this.shadowRoot.getElementById("cancel-confirm")?.addEventListener("click", () => this._closeConfirm());
    this.shadowRoot.getElementById("cancel-confirm-x")?.addEventListener("click", () => this._closeConfirm());
    this.shadowRoot.getElementById("close-panel-tile-picker")?.addEventListener("click", () => this._closePanelTilePicker());
    this.shadowRoot.querySelectorAll("[data-choose-panel-tile]").forEach((button) => {
      button.addEventListener("click", () => this._choosePanelTile(button.dataset.choosePanelTile));
    });
    this.shadowRoot.getElementById("back")?.addEventListener("click", () => { this._selectedDevice = null; this._render(); });
    this.shadowRoot.getElementById("refresh-detail")?.addEventListener("click", () => this._load());
    this.shadowRoot.querySelectorAll("[data-select-device]").forEach((button) => {
      button.addEventListener("click", () => { this._hiddenDevices.delete(button.dataset.selectDevice); localStorage.setItem("hapanels_hidden_devices", JSON.stringify([...this._hiddenDevices])); this._tabletPickerOpen = false; this._selectedDevice = button.dataset.selectDevice; this._activeTab = "tiles"; this._render(); });
    });
    this.shadowRoot.querySelectorAll("[data-hide-device]").forEach((button) => {
      button.addEventListener("click", () => this._askConfirm("Usunąć tablet?", "Czy na pewno chcesz usunąć tablet?", () => this._hideDevice(button.dataset.hideDevice)));
    });
    this.shadowRoot.querySelectorAll("[data-tab]").forEach((button) => {
      button.addEventListener("click", () => { this._activeTab = button.dataset.tab; this._render(); });
    });
    this.shadowRoot.querySelectorAll("[data-theme-mode]").forEach((select) => {
      select.addEventListener("change", () => this._setThemeMode(select.value));
    });
    this.shadowRoot.querySelectorAll("[data-panel-theme-mode]").forEach((button) => {
      button.addEventListener("click", () => this._applyPanelTheme(button.dataset.device, { mode: button.dataset.panelThemeMode }));
    });
    this.shadowRoot.querySelectorAll("[data-panel-theme-preset]").forEach((button) => {
      button.addEventListener("click", () => this._applyPanelTheme(button.dataset.device, { preset: button.dataset.panelThemePreset }));
    });
    this.shadowRoot.querySelectorAll("[data-show-hidden]").forEach((button) => {
      button.addEventListener("click", () => this._showAllDevices());
    });
    this.shadowRoot.querySelectorAll("ha-entity-picker[id$='-entity']").forEach((picker) => {
      picker.hass = this._hass;
      picker.value = picker.getAttribute("value") || "";
      const domains = picker.dataset.domains?.split(",").filter(Boolean);
      if (domains?.length) picker.includeDomains = domains;
      picker.addEventListener("value-changed", (event) => {
        picker.value = event.detail?.value || "";
        this._fillEntityDefaults(picker);
        this._syncTilePreview(picker);
      });
    });
    this.shadowRoot.querySelectorAll("input[id$='-entity']").forEach((input) => {
      input.addEventListener("change", () => { this._fillEntityDefaults(input); this._syncTilePreview(input); });
    });
    this.shadowRoot.querySelectorAll("ha-icon-picker").forEach((picker) => {
      picker.hass = this._hass;
      picker.value = picker.getAttribute("value") || "mdi:cog";
      picker.addEventListener("value-changed", (event) => { picker.value = event.detail?.value || "mdi:cog"; this._syncTilePreview(picker); });
    });
    this.shadowRoot.querySelectorAll("[data-resolve-conflict]").forEach((button) => {
      button.addEventListener("click", () => this._resolveConflict(button.dataset.device, button.dataset.resolveConflict));
    });
    this.shadowRoot.querySelectorAll("[data-save-tile]").forEach((button) => {
      button.addEventListener("click", () => this._saveTile(
        button.dataset.device,
        button.dataset.tile,
        button.dataset.prefix,
        button.dataset.surface,
      ));
    });
    this.shadowRoot.querySelectorAll("[data-add-tile]").forEach((button) => {
      button.addEventListener("click", () => this._addTile(button.dataset.device, button.dataset.surface));
    });
    this.shadowRoot.querySelectorAll("[data-add-ha-tile]").forEach((button) => {
      button.addEventListener("click", () => this._addTile(button.dataset.device, button.dataset.surface, "ha"));
    });
    this.shadowRoot.querySelectorAll("[data-add-child-ha]").forEach((button) => {
      button.addEventListener("click", () => this._addTile(button.dataset.device, "dashboard", "ha", "clock", button.dataset.panelId));
    });
    this.shadowRoot.querySelectorAll("[data-focus-child-tile]").forEach((button) => {
      button.addEventListener("click", () => { this._focusedTileId = button.dataset.focusChildTile; this._collapsedTiles.delete(`dashboard:${this._selectedDevice}:${button.dataset.focusChildTile}`); this._render(); });
    });
    this.shadowRoot.querySelectorAll("[data-pick-panel-tile]").forEach((button) => {
      button.addEventListener("click", () => this._openPanelTilePicker(button.dataset.target, button.dataset.device, button.dataset.surface));
    });
    this.shadowRoot.querySelectorAll("[data-save-aod]").forEach((button) => {
      button.addEventListener("click", () => this._saveAodSettings(button.dataset.device));
    });
    this.shadowRoot.querySelectorAll("[data-aod-preset]").forEach((button) => {
      button.addEventListener("click", () => this._applyAodPreset(button.dataset.device, button.dataset.aodPreset));
    });
    this.shadowRoot.querySelectorAll("[data-aod-clock-style]").forEach((button) => {
      button.addEventListener("click", () => this._applyAodClockStyle(button.dataset.device, button.dataset.aodClockStyle));
    });
    this.shadowRoot.querySelectorAll("[data-preview-size]").forEach((select) => {
      select.addEventListener("change", () => this._setTileSize(select.dataset.device, select.dataset.tile, select.value));
    });
    this.shadowRoot.querySelectorAll("#layout-columns, #layout-rows, #layout-aspect-width, #layout-aspect-height").forEach((input) => {
      input.addEventListener("input", () => this._updateLayoutGrid());
      input.addEventListener("change", () => this._updateLayoutGrid());
    });
    this.shadowRoot.querySelector("[data-layout-grid-panel]")?.addEventListener("toggle", (event) => {
      this._layoutGridCollapsed = !event.target.open;
      localStorage.setItem("hapanels_layout_grid_collapsed", this._layoutGridCollapsed ? "1" : "0");
    });
    this.shadowRoot.querySelectorAll("[data-layout-select]").forEach((button) => {
      button.addEventListener("click", () => { const draft = this._currentLayoutDraft(); if (draft) { draft.selectedTileId = button.dataset.tile; this._render(); } });
      button.addEventListener("dblclick", () => this._editTileFromPreview(button.dataset.tile));
    });
    this.shadowRoot.querySelectorAll("[data-layout-drag]").forEach((button) => {
      button.addEventListener("pointerdown", (event) => this._startLayoutDrag(event, button.dataset.tile));
    });
    this.shadowRoot.querySelectorAll("[data-layout-resize-drag]").forEach((handle) => {
      handle.addEventListener("pointerdown", (event) => this._startLayoutResize(event, handle.dataset.tile, handle.dataset.edge));
    });
    this.shadowRoot.querySelectorAll("[data-layout-restore-drag]").forEach((item) => {
      item.addEventListener("pointerdown", (event) => this._startLayoutTrayDrag(event, item.dataset.tile));
    });
    this.shadowRoot.querySelectorAll("[data-layout-move]").forEach((button) => {
      button.addEventListener("click", () => { const [dx, dy] = button.dataset.layoutMove.split(",").map(Number); this._moveLayoutTile(dx, dy); });
    });
    this.shadowRoot.querySelectorAll("[data-layout-resize]").forEach((button) => {
      button.addEventListener("click", () => { const [dw, dh] = button.dataset.layoutResize.split(",").map(Number); this._resizeLayoutTile(dw, dh); });
    });
    this.shadowRoot.querySelector("[data-layout-add-ha]")?.addEventListener("click", () => this._addLayoutDraftTile("ha"));
    this.shadowRoot.getElementById("layout-context")?.addEventListener("change", (event) => {
      this._layoutContext = event.target.value || "main";
      this._layoutDrag = null;
      this._render();
    });
    this.shadowRoot.querySelectorAll("#layout-tile-title, #layout-tile-subtitle, #layout-tile-icon, #layout-show-icon, #layout-show-title, #layout-show-subtitle, #layout-clock-style").forEach((input) => {
      input.addEventListener("input", () => this._syncSelectedLayoutTile());
      input.addEventListener("change", () => this._syncSelectedLayoutTile());
    });
    this.shadowRoot.querySelectorAll("[data-clock-style]").forEach((button) => {
      button.addEventListener("click", () => {
        const select = this.shadowRoot.getElementById("layout-clock-style");
        if (select) select.value = button.dataset.clockStyle;
        this._syncSelectedLayoutTile();
        this._render();
      });
    });
    this.shadowRoot.querySelector("[data-layout-tray]")?.addEventListener("click", () => this._layoutTileToTray());
    this.shadowRoot.querySelectorAll("[data-layout-restore]").forEach((button) => {
      button.addEventListener("click", () => this._restoreLayoutTile(button.dataset.tile));
    });
    this.shadowRoot.querySelector("[data-layout-clear-tray]")?.addEventListener("click", () => this._clearLayoutTray());
    if (this._activeTab === "preview") {
      requestAnimationFrame(() => this._syncLayoutTrayHeight());
      window.addEventListener("resize", () => this._syncLayoutTrayHeight(), { once: true });
    }
    this.shadowRoot.querySelectorAll("[data-layout-apply]").forEach((button) => {
      button.addEventListener("click", () => this._applyLayoutGrid(button.dataset.device));
    });
    this.shadowRoot.querySelectorAll("[data-layout-panel-preset]").forEach((button) => {
      button.addEventListener("click", () => this._applyPanelPreset(button.dataset.device));
    });
    this.shadowRoot.querySelectorAll("[data-layout-reset]").forEach((button) => {
      button.addEventListener("click", () => this._resetLayoutDraft(button.dataset.device));
    });
    this.shadowRoot.querySelectorAll("[data-layout-save]").forEach((button) => {
      button.addEventListener("click", () => this._saveLayout(button.dataset.device));
    });
    this.shadowRoot.querySelectorAll("[data-layout-edit-tile]").forEach((button) => {
      button.addEventListener("click", () => this._editTileFromPreview(button.dataset.tile));
    });
    this.shadowRoot.querySelectorAll("[data-delete-tile]").forEach((button) => {
      button.addEventListener("click", () => this._askConfirm("Usunąć kafel?", "Czy na pewno chcesz usunąć kafel?", () => this._deleteTile(button.dataset.device, button.dataset.tile, button.dataset.surface)));
    });
    this.shadowRoot.querySelectorAll("select[id$='-accent']").forEach((select) => {
      select.addEventListener("change", () => { this._syncIconAccent(select); this._syncTilePreview(select); });
    });
    this.shadowRoot.querySelectorAll("[data-tile-editor] input, [data-tile-editor] select").forEach((input) => {
      input.addEventListener("input", () => this._syncTilePreview(input));
      input.addEventListener("change", () => this._syncTilePreview(input));
    });
    this.shadowRoot.querySelectorAll("[data-toggle-tile]").forEach((button) => {
      button.addEventListener("click", (event) => {
        event.stopPropagation();
        this._toggleTileCollapsed(button.closest("[data-tile-editor]"));
      });
    });
    this.shadowRoot.querySelectorAll(".tile-head").forEach((head) => {
      head.addEventListener("click", (event) => {
        if (event.target.closest("button")) return;
        this._toggleTileCollapsed(head.closest("[data-tile-editor]"));
      });
    });
    this.shadowRoot.querySelectorAll(".tile-head").forEach((head) => {
      head.addEventListener("dblclick", () => this._toggleTileCollapsed(head.closest("[data-tile-editor]")));
    });
    this.shadowRoot.querySelectorAll("[data-highlight-target]").forEach((element) => {
      element.addEventListener("mouseenter", () => this._setTileHighlight(element, true));
      element.addEventListener("mouseleave", () => this._setTileHighlight(element, false));
    });
  }

  _bindDialog(id, onClose) {
    const dialog = this.shadowRoot.getElementById(id);
    if (!dialog) return;
    if (!dialog.open) dialog.showModal();
    dialog.addEventListener("click", (event) => { if (event.target === dialog) dialog.close(); });
    dialog.addEventListener("close", onClose, { once: true });
  }

  _syncIconAccent(select) {
    const prefix = select.id.replace(/-accent$/, "");
    const field = this.shadowRoot.getElementById(`${prefix}-icon`)?.closest(".icon-field");
    const preview = field?.querySelector(".icon-preview");
    [field, preview].forEach((element) => {
      if (!element) return;
      element.classList.remove("accent-orange", "accent-red", "accent-white");
      element.classList.add(`accent-${select.value}`);
    });
  }

  _editTileFromPreview(tileId) {
    if (!tileId) return;
    this._focusedTileId = tileId;
    this._activeTab = "tiles";
    this._render();
    window.setTimeout(() => {
      const tile = this.shadowRoot.querySelector(`[data-tile-id="${CSS.escape(tileId)}"]`);
      tile?.scrollIntoView({ block: "center", behavior: "smooth" });
    }, 0);
  }

  _toggleTileCollapsed(tile) {
    const key = tile?.dataset.collapsedKey;
    if (!key) return;
    if (this._expandedTiles.has(key)) this._expandedTiles.delete(key);
    else this._expandedTiles.add(key);
    localStorage.setItem("hapanels_expanded_tiles", JSON.stringify([...this._expandedTiles]));
    tile.classList.toggle("collapsed", !this._expandedTiles.has(key));
  }

  _setTileHighlight(element, active) {
    const tile = element.closest("[data-tile-editor]");
    const key = element.dataset.highlightTarget;
    const target = tile?.querySelector(`[data-field-key="${CSS.escape(key)}"]`);
    if (!target) return;
    const iconField = target.querySelector(".icon-field");
    if (iconField) iconField.classList.toggle("tile-highlight", active);
    else target.classList.toggle("tile-highlight", active);
  }

  _syncTilePreview(input) {
    const tile = input.closest("[data-tile-editor]");
    const prefix = tile?.dataset.prefix;
    if (!tile || !prefix) return;
    const value = (suffix) => this.shadowRoot.getElementById(`${prefix}-${suffix}`)?.value?.trim() || "";
    const accent = value("accent") || "orange";
    const icon = this._mdiIcon(value("icon"));
    const label = value("short") || value("label") || tile.querySelector(".tile-title span")?.textContent || "Kafel";
    const entity = value("entity");
    const kind = value("kind") || "entity";
    const entityPicker = this.shadowRoot.getElementById(`${prefix}-entity`);
    if (entityPicker?.tagName === "HA-ENTITY-PICKER") entityPicker.includeDomains = kind === "cover" ? ["cover"] : undefined;
    tile.querySelector(".tile-live-icon")?.setAttribute("icon", icon);
    const previewBox = tile.querySelector(".tile-preview-box");
    if (previewBox) {
      previewBox.classList.remove("accent-orange", "accent-red", "accent-white");
      previewBox.classList.add(`accent-${accent}`);
    }
    tile.style.setProperty("--tile-accent", this._accentColor(accent));
    const setText = (selector, text) => { const el = tile.querySelector(selector); if (el) el.textContent = text; };
    setText(".tile-live-label", label);
    setText(".tile-live-entity", entity ? this._entityLabel(entity) : kind);
    setText(".tile-live-kind", kind);
    setText(".tile-live-size", value("size") || "-");
    setText(".tile-live-accent", accent);
    const coverPreview = tile.querySelector(".cover-preview");
    if (coverPreview) {
      coverPreview.className = `cover-preview span-12 cover-${value("coverVisual") || "blind"} cover-${value("coverDirection") || "top"}`;
      setText(".cover-preview strong", this._coverVisualLabel(value("coverVisual") || "blind"));
      setText(".cover-preview small", this._coverDirectionLabel(value("coverDirection") || "top"));
    }
  }

  _escape(value) {
    return String(value).replace(/[&<>'"]/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      "'": "&#39;",
      '"': "&quot;",
    }[char]));
  }

  _fillEntityDefaults(input) {
    const prefix = input.id.replace(/-entity$/, "");
    const state = this._hass?.states?.[input.value];
    if (!state) return;
    const label = this.shadowRoot.getElementById(`${prefix}-label`);
    const icon = this.shadowRoot.getElementById(`${prefix}-icon`);
    if (label && (!label.value || label.value === "Nowy kafel")) label.value = state.attributes?.friendly_name || input.value;
    if (icon && !icon.value) icon.value = state.attributes?.icon || this._domainIcon(input.value);
  }

  _domainIcon(entityId) {
    const domain = entityId.split(".")[0];
    return ({ light: "mdi:lightbulb", switch: "mdi:toggle-switch", cover: "mdi:blinds", climate: "mdi:home-thermometer", camera: "mdi:cctv", alarm_control_panel: "mdi:shield-lock", person: "mdi:account" }[domain] || "mdi:cog");
  }
}

customElements.define("hapanels-studio-panel", HapanelsStudioPanel);
