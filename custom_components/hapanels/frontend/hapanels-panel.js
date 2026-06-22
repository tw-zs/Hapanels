class HapanelsStudioPanel extends HTMLElement {
  connectedCallback() {
    this.attachShadow({ mode: "open" });
    this._panels = [];
    this._configs = {};
    this._error = null;
    this._render();
    this._load();
  }

  set hass(hass) {
    this._hass = hass;
    if (!this._loaded) this._load();
  }

  async _load() {
    if (!this._hass || this._loading) return;
    this._loading = true;
    try {
      const result = await this._hass.callWS({ type: "hapanels/list_panels" });
      this._panels = result.panels || [];
      await Promise.all(this._panels.map((panel) => this._loadConfig(panel.device)));
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

  async _saveTileLabel(device, tileId, inputId) {
    const config = this._configs[device];
    const revision = Number(config?.revision);
    const input = this.shadowRoot.getElementById(inputId);
    const label = input?.value?.trim();
    if (!config || !Number.isFinite(revision) || !label) return;
    await this._hass.callService("hapanels", "patch_dashboard_config", {
      device,
      patch: {
        base_revision: revision,
        updated_by: "homeassistant:hapanels_studio",
        surface: "dashboard",
        tile_updates: [{ id: tileId, label }],
      },
    });
    window.setTimeout(() => this._load(), 800);
  }

  _render() {
    if (!this.shadowRoot) return;
    const panels = this._panels || [];
    this.shadowRoot.innerHTML = `
      <style>
        :host {
          display: block;
          min-height: 100vh;
          background: #080b0f;
          color: #f5f3ee;
          font-family: var(--paper-font-body1_-_font-family, Nunito, system-ui, sans-serif);
        }
        .page { padding: 28px; max-width: 1180px; margin: 0 auto; }
        .hero { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; margin-bottom: 24px; }
        h1 { margin: 0; font-size: 34px; letter-spacing: -0.03em; }
        .sub { color: #a7adb8; margin-top: 8px; }
        button { border: 0; border-radius: 14px; background: #ff7a1a; color: #1a0d03; padding: 11px 16px; font-weight: 800; cursor: pointer; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
        .card { border: 1px solid rgba(255,255,255,.08); border-radius: 22px; background: linear-gradient(145deg, #151a22, #0d1117); padding: 18px; box-shadow: 0 18px 50px rgba(0,0,0,.28); }
        .name { display: flex; justify-content: space-between; gap: 12px; font-size: 19px; font-weight: 800; }
        .pill { border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 900; text-transform: uppercase; }
        .synced { background: rgba(68, 196, 119, .18); color: #61d689; }
        .conflict { background: rgba(255, 83, 56, .18); color: #ff725d; }
        .unknown, .invalid { background: rgba(255,255,255,.1); color: #c5cad3; }
        dl { display: grid; grid-template-columns: 120px 1fr; gap: 8px 12px; margin: 18px 0 0; }
        dt { color: #8d95a3; } dd { margin: 0; overflow-wrap: anywhere; }
        .tiles { margin-top: 18px; display: grid; gap: 10px; }
        .tile { display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: center; padding: 10px; border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(255,255,255,.03); }
        input { min-width: 0; border: 1px solid rgba(255,255,255,.14); border-radius: 10px; background: #090d12; color: #f5f3ee; padding: 10px; font: inherit; }
        .small { padding: 10px 12px; border-radius: 10px; }
        .empty { border: 1px dashed rgba(255,255,255,.18); border-radius: 22px; padding: 28px; color: #a7adb8; }
        .error { color: #ff725d; margin-top: 12px; }
      </style>
      <main class="page">
        <section class="hero">
          <div>
            <h1>Hapanels Studio</h1>
            <div class="sub">Panele wykryte po MQTT i stan synchronizacji dashboardu/AOD.</div>
            ${this._error ? `<div class="error">${this._escape(this._error)}</div>` : ""}
          </div>
          <button id="refresh">Odśwież</button>
        </section>
        ${panels.length ? `<section class="grid">${panels.map((panel) => this._panelCard(panel)).join("")}</section>` : `<section class="empty">Brak paneli. Uruchom tablet z MQTT i poczekaj na topic <code>dashboard/config/sync/state</code>.</section>`}
      </main>
    `;
    this.shadowRoot.getElementById("refresh")?.addEventListener("click", () => this._load());
    this.shadowRoot.querySelectorAll("[data-save-tile]").forEach((button) => {
      button.addEventListener("click", () => this._saveTileLabel(
        button.dataset.device,
        button.dataset.tile,
        button.dataset.input,
      ));
    });
  }

  _panelCard(panel) {
    const status = this._escape(panel.status || "unknown");
    const config = this._configs[panel.device];
    const tiles = (config?.tiles || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0));
    return `
      <article class="card">
        <div class="name">
          <span>${this._escape(panel.device || "panel")}</span>
          <span class="pill ${status}">${status}</span>
        </div>
        <dl>
          <dt>Dashboard</dt><dd>${this._escape(panel.dashboard_id || "-")}</dd>
          <dt>Revision</dt><dd>${this._escape(panel.revision ?? "-")}</dd>
          <dt>Updated by</dt><dd>${this._escape(panel.updated_by || "-")}</dd>
          <dt>Current</dt><dd>${this._escape(panel.current_revision ?? "-")}</dd>
          <dt>Attempted</dt><dd>${this._escape(panel.attempted_base_revision ?? "-")}</dd>
        </dl>
        ${tiles.length ? `<div class="tiles">${tiles.map((tile) => this._tileEditor(panel.device, tile)).join("")}</div>` : `<div class="empty">Brak pobranej konfiguracji dashboardu.</div>`}
      </article>
    `;
  }

  _tileEditor(device, tile) {
    const inputId = `tile-${device}-${tile.id}`.replace(/[^a-zA-Z0-9_-]/g, "-");
    return `
      <div class="tile">
        <input id="${this._escape(inputId)}" value="${this._escape(tile.label || tile.id)}" aria-label="Etykieta kafla ${this._escape(tile.id)}">
        <button class="small" data-save-tile data-device="${this._escape(device)}" data-tile="${this._escape(tile.id)}" data-input="${this._escape(inputId)}">Zapisz</button>
      </div>
    `;
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
