const APP_URL = "https://github.com/tw-zs/Hapanels";
const TILE_ICONS = ["clock", "lightbulb", "lightbulb_off", "shield_lock", "blinds", "home_thermometer", "cctv", "gate", "home_lightning", "motion_sensor", "sprinkler", "cog"];
const TILE_ACCENTS = ["orange", "red", "white"];

class HapanelsStudioPanel extends HTMLElement {
  connectedCallback() {
    this.attachShadow({ mode: "open" });
    this._panels = [];
    this._configs = {};
    this._error = null;
    this._selectedDevice = null;
    this._activeTab = "tiles";
    this._settingsOpen = false;
    this._themeMode = localStorage.getItem("hapanels_studio_theme") || "auto";
    this._hiddenDevices = new Set(JSON.parse(localStorage.getItem("hapanels_hidden_devices") || "[]"));
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
  }

  async _saveTile(device, tileId, prefix, surface = "dashboard") {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    const label = this.shadowRoot.getElementById(`${prefix}-label`)?.value?.trim();
    const shortLabel = this.shadowRoot.getElementById(`${prefix}-short`)?.value?.trim();
    const entityId = this.shadowRoot.getElementById(`${prefix}-entity`)?.value?.trim();
    const icon = this.shadowRoot.getElementById(`${prefix}-icon`)?.value;
    const accent = this.shadowRoot.getElementById(`${prefix}-accent`)?.value;
    const order = Number(this.shadowRoot.getElementById(`${prefix}-order`)?.value);
    if (!config || !Number.isFinite(revision) || !label) return;
    const tile = { id: tileId, label };
    if (shortLabel !== undefined) tile.short_label = shortLabel;
    if (entityId !== undefined) tile.entity_id = entityId;
    if (icon) tile.icon = icon;
    if (accent) tile.accent = accent;
    if (Number.isFinite(order)) tile.order = order;
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

  _visiblePanels() {
    return (this._panels || []).filter((panel) => !this._hiddenDevices.has(panel.device));
  }

  _selectedPanel() {
    return this._visiblePanels().find((panel) => panel.device === this._selectedDevice) || null;
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
        .page { padding: 28px; max-width: 1180px; margin: 0 auto; }
        .hero { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; margin-bottom: 24px; }
        .brand { display: flex; align-items: center; gap: 12px; }
        .logo { width: 42px; height: 42px; border-radius: 14px; background: linear-gradient(135deg, #ff7a1a, #ffb35c); display: grid; place-items: center; color: #160902; font-weight: 950; box-shadow: 0 12px 30px rgba(255,122,26,.25); }
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
        .pill { border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 950; text-transform: uppercase; }
        .synced { background: rgba(68, 196, 119, .18); color: #48c97a; }
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
        .tiles { display: grid; gap: 10px; }
        .tile { display: grid; gap: 10px; padding: 12px; border: 1px solid var(--line); border-radius: 14px; background: var(--surface-2); }
        .tile-head { display: flex; justify-content: space-between; gap: 12px; color: var(--muted); font-size: 12px; font-weight: 850; text-transform: uppercase; }
        .fields { display: grid; grid-template-columns: 1.4fr 1fr 1.5fr .9fr .8fr .6fr auto; gap: 8px; align-items: end; }
        .field { display: grid; gap: 5px; }
        label { color: var(--muted); font-size: 12px; font-weight: 800; }
        input, select { min-width: 0; border: 1px solid var(--line); border-radius: 10px; background: var(--bg); color: var(--text); padding: 10px; font: inherit; }
        .small { padding: 10px 12px; border-radius: 10px; }
        .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
        .settings { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: grid; place-items: start end; padding: 22px; z-index: 10; }
        .settings-panel { width: min(420px, calc(100vw - 44px)); background: var(--surface); border: 1px solid var(--line); border-radius: 24px; padding: 20px; box-shadow: 0 24px 80px rgba(0,0,0,.32); }
        .settings-row { display: grid; gap: 8px; margin-top: 16px; }
        .error { color: #ff725d; margin-top: 12px; }
        a { color: var(--accent); }
      </style>
      <main class="page">
        ${this._header()}
        ${selected ? this._detailView(selected) : this._mainView(panels)}
      </main>
      ${this._settingsOpen ? this._settingsView() : ""}
    `;
    this._bindEvents();
  }

  _header() {
    return `
      <section class="hero">
        <div>
          <div class="brand">
            <div class="logo" aria-hidden="true">H</div>
            <h1>Hapanels Studio</h1>
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
    return `
      <button class="card panel-card" data-select-device="${this._escape(panel.device)}">
        <div class="name">
          <span>${this._escape(panel.device || "panel")}</span>
          <span class="pill ${status}">${status}</span>
        </div>
        <dl>
          <dt>Dashboard</dt><dd>${this._escape(panel.dashboard_id || "-")}</dd>
          <dt>Revision</dt><dd>${this._escape(panel.revision ?? "-")}</dd>
          <dt>Updated by</dt><dd>${this._escape(panel.updated_by || "-")}</dd>
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
            <h2 style="margin-top:16px">${this._escape(panel.device || "panel")}</h2>
            <div class="sub"><span class="pill ${status}">${status}</span> Dashboard ${this._escape(panel.dashboard_id || "-")} · revision ${this._escape(panel.revision ?? "-")}</div>
          </div>
          <div class="actions">
            <button id="refresh-detail" class="secondary">Odśwież</button>
            <button class="danger" data-hide-device="${this._escape(panel.device)}">Usuń tablet</button>
          </div>
        </div>
        <div class="tabs">
          ${this._tab("tiles", "Kafle")}
          ${this._tab("aod", "AOD")}
          ${this._tab("settings", "Ustawienia")}
          ${this._tab("preview", "Podgląd")}
        </div>
        ${this._tabContent(panel.device, config)}
      </section>
    `;
  }

  _tab(id, label) {
    return `<button class="tab ${this._activeTab === id ? "active" : ""}" data-tab="${id}">${label}</button>`;
  }

  _tabContent(device, config) {
    if (!config) return `<div class="empty-box">Brak pobranej konfiguracji dashboardu.</div>`;
    if (this._activeTab === "aod") return this._aodView(device, config);
    if (this._activeTab === "settings") return this._tabletSettingsView(config);
    if (this._activeTab === "preview") return `<div class="empty-box">Podgląd układu będzie następny. Na razie edytuj kafle i AOD.</div>`;
    const tiles = (config.tiles || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0));
    return `<div class="tiles">${tiles.map((tile) => this._tileEditor(device, tile, "dashboard")).join("")}</div>`;
  }

  _aodView(device, config) {
    const aod = config.always_on_display || {};
    const tiles = (aod.tiles || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0));
    return `
      <dl>
        <dt>Jasność min.</dt><dd>${this._escape(aod.brightness_percent ?? "-")}%</dd>
        <dt>Tło</dt><dd>${this._escape(aod.background || "-")}</dd>
        <dt>Kolumny</dt><dd>${this._escape(aod.grid_layout?.columns_landscape ?? "-")}</dd>
      </dl>
      <div class="tiles">${tiles.map((tile) => this._tileEditor(device, tile, "aod")).join("")}</div>
    `;
  }

  _tabletSettingsView(config) {
    return `
      <dl>
        <dt>Dashboard ID</dt><dd>${this._escape(config.dashboard_id || "-")}</dd>
        <dt>Revision</dt><dd>${this._escape(config.revision ?? "-")}</dd>
        <dt>Updated by</dt><dd>${this._escape(config.updated_by || "-")}</dd>
      </dl>
    `;
  }

  _tileEditor(device, tile, surface) {
    const prefix = `tile-${surface}-${device}-${tile.id}`.replace(/[^a-zA-Z0-9_-]/g, "-");
    return `
      <div class="tile">
        <div class="tile-head"><span>${this._escape(tile.id)}</span><span>${this._escape(tile.kind || "tile")} · ${this._escape(tile.size || "-")}</span></div>
        <div class="fields">
          ${this._inputField(`${prefix}-label`, "Label", tile.label || tile.id)}
          ${this._inputField(`${prefix}-short`, "Short", tile.short_label || "")}
          ${this._inputField(`${prefix}-entity`, "Entity", tile.entity_id || "")}
          ${this._selectField(`${prefix}-icon`, "Ikona", TILE_ICONS, tile.icon)}
          ${this._selectField(`${prefix}-accent`, "Accent", TILE_ACCENTS, tile.accent || "orange")}
          ${this._inputField(`${prefix}-order`, "Order", tile.order ?? 0, "number")}
          <button class="small" data-save-tile data-device="${this._escape(device)}" data-surface="${surface}" data-tile="${this._escape(tile.id)}" data-prefix="${this._escape(prefix)}">Zapisz</button>
        </div>
      </div>
    `;
  }

  _inputField(id, label, value, type = "text") {
    return `<div class="field"><label for="${this._escape(id)}">${this._escape(label)}</label><input id="${this._escape(id)}" type="${type}" value="${this._escape(value)}"></div>`;
  }

  _selectField(id, label, options, selected) {
    return `<div class="field"><label for="${this._escape(id)}">${this._escape(label)}</label><select id="${this._escape(id)}">${options.map((option) => `<option value="${this._escape(option)}" ${option === selected ? "selected" : ""}>${this._escape(option)}</option>`).join("")}</select></div>`;
  }

  _settingsView() {
    return `
      <div class="settings" id="settings-backdrop">
        <aside class="settings-panel">
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
        </aside>
      </div>
    `;
  }

  _bindEvents() {
    this.shadowRoot.getElementById("refresh")?.addEventListener("click", () => this._load());
    this.shadowRoot.getElementById("settings")?.addEventListener("click", () => { this._settingsOpen = true; this._render(); });
    this.shadowRoot.getElementById("close-settings")?.addEventListener("click", () => { this._settingsOpen = false; this._render(); });
    this.shadowRoot.getElementById("settings-backdrop")?.addEventListener("click", (event) => {
      if (event.target.id === "settings-backdrop") { this._settingsOpen = false; this._render(); }
    });
    this.shadowRoot.getElementById("theme-mode")?.addEventListener("change", (event) => this._setThemeMode(event.target.value));
    this.shadowRoot.getElementById("add-tablet")?.addEventListener("click", () => window.open(APP_URL, "_blank", "noopener"));
    this.shadowRoot.getElementById("first-tablet")?.addEventListener("click", () => window.open(APP_URL, "_blank", "noopener"));
    this.shadowRoot.getElementById("show-hidden")?.addEventListener("click", () => this._showAllDevices());
    this.shadowRoot.getElementById("back")?.addEventListener("click", () => { this._selectedDevice = null; this._render(); });
    this.shadowRoot.getElementById("refresh-detail")?.addEventListener("click", () => this._load());
    this.shadowRoot.querySelectorAll("[data-select-device]").forEach((button) => {
      button.addEventListener("click", () => { this._selectedDevice = button.dataset.selectDevice; this._activeTab = "tiles"; this._render(); });
    });
    this.shadowRoot.querySelectorAll("[data-hide-device]").forEach((button) => {
      button.addEventListener("click", () => this._hideDevice(button.dataset.hideDevice));
    });
    this.shadowRoot.querySelectorAll("[data-tab]").forEach((button) => {
      button.addEventListener("click", () => { this._activeTab = button.dataset.tab; this._render(); });
    });
    this.shadowRoot.querySelectorAll("[data-save-tile]").forEach((button) => {
      button.addEventListener("click", () => this._saveTile(
        button.dataset.device,
        button.dataset.tile,
        button.dataset.prefix,
        button.dataset.surface,
      ));
    });
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
}

customElements.define("hapanels-studio-panel", HapanelsStudioPanel);
