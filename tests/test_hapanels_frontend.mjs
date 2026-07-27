import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";

let Panel;
const storage = new Map();
const context = vm.createContext({
  HTMLElement: class {},
  customElements: { get: () => null, define: (_name, element) => { Panel = element; } },
  window: { addEventListener: () => {}, removeEventListener: () => {}, setTimeout: () => 0 },
  localStorage: {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
  },
  structuredClone,
});
vm.runInContext(readFileSync(new URL("../custom_components/hapanels/frontend/hapanels-panel.js", import.meta.url), "utf8"), context);

const panel = new Panel();
const tile = { id: "tile", col: 2, row: 2, colSpan: 2, rowSpan: 2 };
const draft = { grid: { columns: 12, rows: 9 }, tiles: [tile], tray: [] };
const grid = { getBoundingClientRect: () => ({ left: 0, right: 1200, top: 0, bottom: 900, width: 1200, height: 900 }) };
panel._currentLayoutDraft = () => draft;
panel.shadowRoot = { querySelector: (selector) => selector === ".layout-grid" ? grid : null };
panel._blockingTiles = () => [];
panel._isOutsideGrid = () => false;
panel._render = () => {};
panel._layoutDrag = { mode: "resize", source: "grid", edge: "e", tileId: tile.id, startX: 300, startY: 300 };

panel._moveLayoutGhost({ clientX: 349, clientY: 300 });
assert.equal(panel._layoutDrag.ghost.colSpan, 2, "sub-cell movement must not resize");

panel._moveLayoutGhost({ clientX: 351, clientY: 300 });
assert.equal(panel._layoutDrag.ghost.colSpan, 3, "crossing half a cell must resize once");

panel._moveLayoutGhost({ clientX: 1400, clientY: 300 });
assert.deepEqual(
  JSON.parse(JSON.stringify(panel._layoutDrag.ghost)),
  { col: 2, row: 2, colSpan: 11, rowSpan: 2, valid: true },
  "resize must clamp at grid edge even when pointer leaves grid",
);

const historyPanel = new Panel();
const historyDraft = {
  grid: { columns: 12, rows: 9 },
  tiles: [{ id: "history", col: 2, row: 2, colSpan: 1, rowSpan: 1 }],
  tray: [],
  selectedTileId: "history",
  historyKey: "device:1:main",
};
historyPanel._layoutHistories = {};
historyPanel._render = () => {};
historyPanel._currentLayoutDraft = () => historyDraft;
historyPanel._checkpointLayoutHistory(historyDraft);
historyDraft.tiles[0].col = 3;
historyPanel._checkpointLayoutHistory(historyDraft);
historyPanel._undoLayout();
assert.equal(historyDraft.tiles[0].col, 2, "undo must restore the previous draft");
historyPanel._redoLayout();
assert.equal(historyDraft.tiles[0].col, 3, "redo must restore the undone draft");

historyPanel._activeTab = "preview";
let prevented = false;
historyPanel._handleLayoutKeydown({ key: "z", ctrlKey: true, metaKey: false, altKey: false, shiftKey: false, target: null, preventDefault: () => { prevented = true; } });
assert.equal(historyDraft.tiles[0].col, 2, "Ctrl+Z must undo the draft");
assert.equal(prevented, true, "handled shortcut must prevent browser undo");

const revisionPanel = new Panel();
revisionPanel._configs = { device: { version: 2, revision: 2 } };
revisionPanel._rememberPreviousConfig("device", { version: 2, revision: 1 });
let restoredConfig;
revisionPanel._setConfig = async (_device, config) => { restoredConfig = config; };
await revisionPanel._restorePreviousRevision("device");
assert.equal(restoredConfig.revision, 2, "rollback must base the restored config on the current revision");

const discardPanel = new Panel();
const discardDraft = {
  grid: { columns: 12, rows: 9 },
  tiles: [{ id: "discard", col: 4, row: 2, colSpan: 1, rowSpan: 1 }],
  tray: [],
  selectedTileId: "discard",
  historyKey: "device:2:main",
};
const savedDraft = {
  grid: { columns: 12, rows: 9 },
  tiles: [{ id: "discard", col: 2, row: 2, colSpan: 1, rowSpan: 1 }],
  tray: [],
};
discardPanel._configs = { device: { revision: 2 } };
discardPanel._selectedDevice = "device";
discardPanel._layoutHistories = {};
discardPanel._currentLayoutDraft = () => discardDraft;
discardPanel._savedLayoutSnapshot = () => structuredClone(savedDraft);
discardPanel._render = () => {};
discardPanel._checkpointLayoutHistory(discardDraft);
assert.equal(discardPanel._isLayoutDirty("device", {}, discardDraft), true, "changed draft must be dirty");
discardPanel._resetLayoutDraft("device");
assert.equal(discardDraft.tiles[0].col, 2, "discard must restore the saved layout");
discardPanel._undoLayout();
assert.equal(discardDraft.tiles[0].col, 4, "undo must recover discarded changes");

const migrateStudioTile = vm.runInContext("migrateStudioTile", context);
const resetTileAuthoring = vm.runInContext("resetTileAuthoring", context);
const validateDashboardConfig = vm.runInContext("validateDashboardConfig", context);
const normalizedTile = JSON.parse(JSON.stringify(migrateStudioTile({ id: "light", kind: "entity", size: "large", label: "Light", entity_id: "light.kitchen", icon: "mdi:lightbulb", accent: "orange", order: 0 })));
assert.equal(normalizedTile.icon_source, "custom", "normalization must add icon source");
assert.equal(normalizedTile.icon_color_source, "accent", "normalization must add icon color source");
assert.equal(normalizedTile.tap_action.type, "entity_default", "normalization must add safe entity action");

const resetTile = JSON.parse(JSON.stringify(resetTileAuthoring({ ...normalizedTile, short_label: "Kitchen", col: 3, row: 2, colSpan: 2, rowSpan: 3, icon_color_source: "custom", icon_color: "#123456" })));
assert.equal(resetTile.label, "Light", "reset must preserve name");
assert.equal(resetTile.entity_id, "light.kitchen", "reset must preserve entity");
assert.deepEqual({ col: resetTile.col, row: resetTile.row, colSpan: resetTile.colSpan, rowSpan: resetTile.rowSpan }, { col: 3, row: 2, colSpan: 2, rowSpan: 3 }, "reset must preserve layout");
assert.equal(resetTile.icon_color_source, "accent", "reset must restore presentation defaults");

const baseConfig = {
  version: 2,
  dashboard_id: "test",
  revision: 1,
  updated_by: "test",
  layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, columns: 12, rows: 9, gap: "medium" },
  always_on_display: { layout: "minimal_clock", grid_layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, columns: 12, rows: 9, gap: "small" }, tiles: [] },
  panels: [{ id: "room", title: "Room", layout: { type: "fixed_grid", columns_landscape: 3, columns_portrait: 2, columns: 12, rows: 9, gap: "medium" }, tiles: [] }],
  tiles: [],
  extensions: {},
  migration_report: [],
};
const technicalTile = { id: "technical", kind: "action", size: "small", label: "Settings", icon: "mdi:cog", icon_source: "custom", icon_color_source: "accent", accent: "white", order: 0, tap_action: { type: "navigate", destination: "settings", domain: "homeassistant", service: "toggle", target: { entity_id: "light.kitchen" }, data: { transition: 1 } } };
assert.equal(validateDashboardConfig({ ...structuredClone(baseConfig), tiles: [technicalTile] }).length, 0, "action tiles must author full technical fields");
const rejectedTechnical = structuredClone(technicalTile);
rejectedTechnical.kind = "entity";
rejectedTechnical.entity_id = "light.kitchen";
assert.ok(validateDashboardConfig({ ...structuredClone(baseConfig), tiles: [rejectedTechnical] }).some((error) => error.includes("akcja techniczna")), "navigate/local_panel authoring must be limited to action tiles");
const invalidMoreInfo = structuredClone(technicalTile);
invalidMoreInfo.tap_action = { type: "more_info" };
assert.ok(validateDashboardConfig({ ...structuredClone(baseConfig), tiles: [invalidMoreInfo] }).some((error) => error.includes("entity_id")), "more_info must require an entity");

const navigationPanel = new Panel();
const child = { ...normalizedTile, id: "child", label: "Saved", panel_id: undefined, col: 1, row: 1, colSpan: 2, rowSpan: 2 };
const opener = { id: "room-opener", kind: "folder", size: "large", label: "Room", panel_id: "room", icon: "mdi:folder", accent: "orange", order: 0, col: 1, row: 1, colSpan: 2, rowSpan: 2 };
const navigationConfig = structuredClone(baseConfig);
navigationConfig.tiles = [opener];
navigationConfig.panels[0].tiles = [child];
navigationPanel._configs = { device: navigationConfig };
navigationPanel._selectedDevice = "device";
navigationPanel._layoutDrafts = {};
navigationPanel._layoutHistories = {};
navigationPanel._tileDrafts = { "device:child": { ...child, label: "Unsaved" } };
navigationPanel._panels = [];
navigationPanel._render = () => {};
navigationPanel._openTileInPreview("child");
assert.equal(navigationPanel._activeTab, "preview", "Tiles navigation must open Preview");
assert.equal(navigationPanel._layoutContext, "folder:room", "Tiles navigation must select tile context");
assert.equal(navigationPanel._currentLayoutDraft().selectedTileId, "child", "Tiles navigation must select tile");
assert.equal(navigationPanel._tileWithDraft("device", child).label, "Unsaved", "Preview must render unsaved tile draft");
navigationPanel._editTileFromPreview("child");
assert.equal(navigationPanel._activeTab, "tiles", "Preview navigation must return to Tiles");
assert.equal(navigationPanel._selectedTileId, "child", "Preview navigation must select editor tile");
assert.equal(navigationPanel._tileDrafts["device:child"].label, "Unsaved", "cross-navigation must preserve unsaved draft");

const formPanel = new Panel();
const formTile = { ...normalizedTile, size: "large", order: 4, col: 3, row: 2, colSpan: 2, rowSpan: 3, hold_action: { type: "more_info", entity_id: "light.kitchen" } };
formPanel._configs = { device: { ...structuredClone(baseConfig), tiles: [formTile] } };
formPanel._tileDrafts = {};
formPanel._tileValidation = {};
formPanel._tileSaveStatus = {};
formPanel._validateTileDraft = () => [];
const fields = new Map([
  ["editor-label", { value: "Light" }], ["editor-kind", { value: "entity" }], ["editor-accent", { value: "orange" }],
  ["editor-iconSource", { value: "custom" }], ["editor-iconColorSource", { value: "accent" }], ["editor-icon", { value: "mdi:lightbulb" }],
  ["editor-entity", { value: "light.kitchen" }], ["editor-showIcon", { checked: true }], ["editor-showLabel", { checked: true }],
  ["editor-showValue", { checked: true }], ["editor-showSecondary", { checked: true }], ["editor-background", { value: "surface" }],
  ["editor-border", { value: "default" }], ["editor-alignment", { value: "center" }], ["editor-tap-type", { value: "entity_default" }],
  ["editor-tap-entity", { value: "light.kitchen" }], ["editor-hold-type", { value: "unset" }],
]);
formPanel.shadowRoot = { getElementById: (id) => fields.get(id) || null, querySelector: () => null };
const captured = formPanel._captureTileDraft("editor", "device", "light");
assert.deepEqual(
  { size: captured.size, order: captured.order, col: captured.col, row: captured.row, colSpan: captured.colSpan, rowSpan: captured.rowSpan },
  { size: "large", order: 4, col: 3, row: 2, colSpan: 2, rowSpan: 3 },
  "Tiles editor must preserve layout fields that are not present in its form",
);
assert.deepEqual(JSON.parse(JSON.stringify(captured.hold_action)), { type: "none" }, "clearing hold must send an explicit none action");
assert.equal(formPanel._tileWithDraft("device", { ...formTile, size: "small", col: 8 }).size, "small", "tile draft must not override Preview layout fields");
