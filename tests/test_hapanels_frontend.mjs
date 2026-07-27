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
const source = readFileSync(new URL("../custom_components/hapanels/frontend/hapanels-panel.js", import.meta.url), "utf8");
vm.runInContext(source, context);
assert.match(source, /tile-live-preview[\s\S]*tile-editor-body/, "production editor must keep accepted live-preview and form hierarchy");
assert.match(source, /@media \(max-width: 1100px\)[\s\S]*layout-side/, "Preview must collapse tray before narrow desktop widths overlap");
assert.match(source, /@media \(max-width: 620px\)[\s\S]*tile-toolbar-filters/, "tile editor must include mobile controls layout");
assert.match(source, /if \(!customElements\.get\("hapanels-studio-panel"\)\)/, "frontend registration must tolerate cache-key reloads");
assert.match(source, /data-layout-edit-tile[\s\S]*dataset\.layoutEditTile/, "Edit in Tiles must read its own data attribute");
assert.match(source, /role="tablist"[\s\S]*Kafle[\s\S]*Układ[\s\S]*Wygląd[\s\S]*Wygaszacz[\s\S]*Informacje/, "tablet navigation must follow the content-to-device workflow");
assert.match(source, /role="tab" aria-selected="\$\{active\}" tabindex="\$\{active \? "0" : "-1"\}"/, "tablet navigation must expose accessible tabs");

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
const liveVisualPanel = new Panel();
liveVisualPanel._hass = { states: { "light.kitchen": { state: "on", attributes: { icon: "mdi:floor-lamp", rgb_color: [10, 20, 30] } } } };
liveVisualPanel._tileDrafts = {};
const masterItem = liveVisualPanel._tileMasterItem("device", { ...normalizedTile, icon_source: "auto", icon_color_source: "entity" });
assert.match(masterItem, /mdi:floor-lamp/, "Tiles list must resolve automatic icons from entity state");
assert.match(masterItem, /rgb\(10,20,30\)/, "Tiles list must resolve entity-sourced icon colour");

const resetTile = JSON.parse(JSON.stringify(resetTileAuthoring({ ...normalizedTile, short_label: "Kitchen", col: 3, row: 2, colSpan: 2, rowSpan: 3, icon_color_source: "custom", icon_color: "#123456" })));
assert.equal(resetTile.label, "Light", "reset must preserve name");
assert.equal(resetTile.entity_id, "light.kitchen", "reset must preserve entity");
assert.deepEqual({ col: resetTile.col, row: resetTile.row, colSpan: resetTile.colSpan, rowSpan: resetTile.rowSpan }, { col: 3, row: 2, colSpan: 2, rowSpan: 3 }, "reset must preserve layout");
assert.equal(resetTile.icon_color_source, "accent", "reset must restore presentation defaults");
const resetTextTile = JSON.parse(JSON.stringify(resetTileAuthoring({ id: "note", kind: "text", size: "small", label: "Note", content: "Keep me", col: 1, row: 1 })));
assert.equal(resetTextTile.content, "Keep me", "reset must preserve required text content");

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
const overviewPanel = new Panel();
const overviewConfig = structuredClone(baseConfig);
overviewConfig.tiles = [{ ...normalizedTile, col: 1, row: 1, colSpan: 3, rowSpan: 2 }];
overviewPanel._panels = [{ device: "tablet-one", panel_name: "Kitchen", status: "synced", dashboard_id: "technical-id", revision: 4, updated_by: "technical-user" }];
overviewPanel._configs = { "tablet-one": overviewConfig };
overviewPanel._hiddenDevices = new Set();
const overview = overviewPanel._mainView(overviewPanel._visiblePanels());
assert.match(overview, /Twoje tablety/, "homepage must introduce the device overview");
assert.match(overview, /data-select-device="tablet-one"/, "homepage must offer direct tile editing");
assert.match(overview, /data-preview-device="tablet-one"/, "homepage must offer direct preview access");
assert.match(overview, /panel-mini-tile/, "homepage must visualize the saved layout");
assert.doesNotMatch(overview, /technical-id|technical-user/, "homepage must hide routine technical metadata");
const resetCases = [
  { id: "reset-entity", kind: "entity", size: "small", label: "Entity", entity_id: "light.test", icon: "mdi:star", accent: "white", order: 0, col: 1, row: 1 },
  { id: "reset-cover", kind: "cover", size: "small", label: "Cover", entity_id: "cover.test", icon: "mdi:star", accent: "white", order: 0, cover_visual: "curtain", cover_direction: "left" },
  { id: "reset-camera", kind: "camera", size: "small", label: "Camera", entity_id: "camera.test", icon: "mdi:star", accent: "white", order: 0 },
  { id: "reset-category", kind: "category", size: "small", label: "Category", panel_id: "room", icon: "mdi:star", accent: "white", order: 0 },
  { id: "reset-action", kind: "action", size: "small", label: "Action", icon: "mdi:star", accent: "white", order: 0, tap_action: { type: "local_panel", action: "screen.aod_now" } },
  { id: "reset-clock", kind: "clock", size: "small", label: "Clock", icon: "mdi:star", accent: "white", order: 0, clock_style: "date_top" },
  { id: "reset-folder", kind: "folder", size: "small", label: "Folder", panel_id: "room", icon: "mdi:star", accent: "white", order: 0 },
  { id: "reset-popup", kind: "popup", size: "small", label: "Popup", panel_id: "room", icon: "mdi:star", accent: "white", order: 0 },
  { id: "reset-text", kind: "text", size: "small", label: "Text", content: "Keep", icon: "mdi:star", accent: "white", order: 0 },
  { id: "reset-spacer", kind: "spacer", size: "small", label: "Spacer", icon: "mdi:star", accent: "white", order: 0 },
];
const resetByKind = Object.fromEntries(resetCases.map((tile) => [tile.kind, JSON.parse(JSON.stringify(resetTileAuthoring(tile)))]));
for (const [kind, reset] of Object.entries(resetByKind)) {
  assert.equal(reset.size, "small", `${kind} reset must preserve tile variant`);
  assert.equal(validateDashboardConfig({ ...structuredClone(baseConfig), tiles: [reset] }).length, 0, `${kind} reset must remain schema-valid`);
}
assert.equal(resetByKind.entity.entity_id, "light.test", "entity reset must preserve entity");
assert.equal(resetByKind.cover.cover_visual, "blind", "cover reset must restore visual default");
assert.equal(resetByKind.cover.cover_direction, "top", "cover reset must restore direction default");
assert.equal(resetByKind.clock.clock_style, "classic", "clock reset must restore style default");
assert.equal(resetByKind.folder.panel_id, "room", "folder reset must preserve panel target");
assert.equal(resetByKind.popup.panel_id, "room", "popup reset must preserve panel target");
assert.equal(resetByKind.text.content, "Keep", "text reset must preserve required content");
assert.equal(resetByKind.action.tap_action.type, "none", "action reset must restore safe no-op");
assert.equal(resetByKind.spacer.presentation, undefined, "spacer reset must remove presentation");
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

const pendingPanel = new Panel();
const pendingConfig = structuredClone(baseConfig);
const trayTile = { ...normalizedTile, id: "tray", label: "Tray", order: 0 };
pendingConfig.extensions.layout_tray = [trayTile];
pendingPanel._configs = { device: pendingConfig };
pendingPanel._selectedDevice = "device";
pendingPanel._layoutContext = "main";
pendingPanel._layoutDrafts = {};
pendingPanel._layoutHistories = {};
pendingPanel._layoutSaveStatus = {};
pendingPanel._tileDrafts = {};
pendingPanel._tileValidation = {};
pendingPanel._tileSaveStatus = {};
pendingPanel._panels = [];
pendingPanel._render = () => {};
const pendingDraft = pendingPanel._layoutDraft("device", pendingConfig);
const pendingTile = { ...normalizedTile, id: "pending", label: "Pending", entity_id: "", col: 1, row: 1, colSpan: 1, rowSpan: 1 };
pendingDraft.tiles.push(pendingTile);
const editableGroups = pendingPanel._editableTileGroups("device", pendingConfig);
assert.ok(editableGroups.find((group) => group.id === "main").tiles.some((item) => item.id === "pending"), "Tiles must list unsaved Preview tiles");
assert.ok(editableGroups.find((group) => group.id === "tray").tiles.some((item) => item.id === "tray"), "Tiles must expose a tray section");
pendingPanel._editTileFromPreview("pending");
assert.equal(pendingPanel._tileDraft("device", "pending").label, "Pending", "Preview tile must open in Tiles before it is saved");
const pendingErrors = pendingPanel._validateTileDraft("device", pendingTile);
assert.ok(pendingErrors.some((error) => error.includes("entity_id")), "pending tile validation must report its missing entity");
assert.ok(!pendingErrors.some((error) => error.includes("nie istnieje")), "pending tile validation must not reject its draft source");
const editedPendingTile = { ...pendingTile, entity_id: "light.pending", tap_action: { type: "entity_default", entity_id: "light.pending" } };
pendingPanel._tileDrafts["device:pending"] = editedPendingTile;
pendingPanel._captureTileDraft = () => editedPendingTile;
let pendingSavedConfig;
pendingPanel._setConfig = async (_device, config) => { pendingSavedConfig = config; return true; };
await pendingPanel._saveTile("device", "pending", "editor");
assert.equal(pendingSavedConfig.tiles.find((item) => item.id === "pending").entity_id, "light.pending", "layout save must persist pending tile authoring");

const addPanel = new Panel();
const addConfig = structuredClone(baseConfig);
addPanel._configs = { device: addConfig };
addPanel._selectedDevice = "device";
addPanel._layoutContext = "main";
addPanel._layoutDrafts = {};
addPanel._layoutHistories = {};
addPanel._tileDrafts = {};
addPanel._tileValidation = {};
addPanel._tileSaveStatus = {};
addPanel._tilePreviewStates = {};
addPanel._panels = [];
addPanel._render = () => {};
let directSaveCalls = 0;
addPanel._setConfig = async () => { directSaveCalls += 1; return false; };
await addPanel._addTile("device", "dashboard", "ha");
const addedTile = addPanel._editableTile("device", addPanel._selectedTileId);
assert.equal(directSaveCalls, 0, "adding a dashboard tile must not save an invalid placeholder");
assert.equal(addPanel._activeTab, "tiles", "new dashboard tile must open in Tiles");
assert.equal(addedTile.entity_id, "", "new dashboard tile must wait for entity selection");
assert.ok(addPanel._tileDrafts[`device:${addedTile.id}`], "new dashboard tile must be marked as an editable draft");
assert.equal(addPanel._hasUnsavedDrafts(), true, "new tile draft must trigger refresh protection");
await addPanel._deleteTile("device", addedTile.id);
assert.equal(directSaveCalls, 0, "deleting an unsaved tile must not write an unchanged config");
assert.equal(addPanel._editableTile("device", addedTile.id), null, "deleting an unsaved tile must remove its layout and tile drafts");
assert.equal(addPanel._hasUnsavedDrafts(), false, "deleting the only draft must clear refresh protection");

const fullGridPanel = new Panel();
const fullGridConfig = structuredClone(baseConfig);
fullGridConfig.layout = { ...fullGridConfig.layout, columns: 1, rows: 1 };
fullGridConfig.tiles = [{ ...normalizedTile, id: "occupied", col: 1, row: 1, colSpan: 1, rowSpan: 1 }];
fullGridPanel._configs = { device: fullGridConfig };
fullGridPanel._layoutDrafts = {};
fullGridPanel._layoutHistories = {};
fullGridPanel._tileDrafts = {};
fullGridPanel._render = () => {};
await fullGridPanel._addTile("device", "dashboard", "ha");
assert.equal(fullGridPanel._layoutTileLocation("device", fullGridPanel._selectedTileId).tray, true, "new tile must enter tray when grid is full");

const childAddPanel = new Panel();
childAddPanel._configs = { device: structuredClone(navigationConfig) };
childAddPanel._layoutDrafts = {};
childAddPanel._layoutHistories = {};
childAddPanel._tileDrafts = {};
childAddPanel._render = () => {};
await childAddPanel._addTile("device", "dashboard", "ha", "clock", "room");
assert.equal(childAddPanel._layoutContext, "folder:room", "child tile add must select its folder context");
assert.ok(childAddPanel._layoutTileLocation("device", childAddPanel._selectedTileId)?.context?.panelId === "room", "child tile must be created in target panel draft");
const pendingChildId = childAddPanel._selectedTileId;
const childSection = childAddPanel._tileChildrenSection("device", navigationConfig.tiles[0]);
assert.ok(childSection.includes(pendingChildId), "folder contents must include pending child drafts");
assert.ok(childSection.includes("2 kafle"), "folder contents must show a grammatical tile count including drafts");
assert.match(childSection, /data-open-panel-layout="room"/, "folder contents must link to its Preview layout");
assert.match(childSection, /data-target="child"[\s\S]*data-panel-id="room"/, "folder contents must support nested panel tiles");
childAddPanel._selectedDevice = "device";
childAddPanel._openPanelLayout("room");
assert.equal(childAddPanel._activeTab, "preview", "folder layout action must open Preview");
assert.equal(childAddPanel._layoutContext, "folder:room", "folder layout action must keep folder context");
childAddPanel._activeTab = "tiles";
childAddPanel._openPanelTilePicker("child", "device", "dashboard", "room");
childAddPanel._choosePanelTile("popup");
assert.ok(childAddPanel._layoutTileLocation("device", childAddPanel._selectedTileId)?.tile?.kind === "popup", "folder contents must allow nested panel tiles");
const popupAddPanel = new Panel();
const popupConfig = structuredClone(navigationConfig);
popupConfig.tiles[0].kind = "popup";
popupAddPanel._configs = { device: popupConfig };
popupAddPanel._layoutDrafts = {};
popupAddPanel._layoutHistories = {};
popupAddPanel._tileDrafts = {};
popupAddPanel._render = () => {};
await popupAddPanel._addTile("device", "dashboard", "ha", "clock", "room");
assert.equal(popupAddPanel._layoutContext, "popup:room", "child tile add must select its popup context");

const openerSavePanel = new Panel();
openerSavePanel._configs = { device: structuredClone(navigationConfig) };
openerSavePanel._layoutDrafts = {};
openerSavePanel._tileDrafts = { "device:room-opener": { ...opener, label: "Renamed room" } };
openerSavePanel._tileValidation = {};
openerSavePanel._tileSaveStatus = {};
openerSavePanel._render = () => {};
openerSavePanel._captureTileDraft = () => openerSavePanel._tileDrafts["device:room-opener"];
let openerSavedConfig;
openerSavePanel._setConfig = async (_device, config) => { openerSavedConfig = config; return true; };
await openerSavePanel._saveTile("device", "room-opener", "editor");
assert.equal(openerSavedConfig.panels.find((panel) => panel.id === "room").title, "Renamed room", "saving folder must synchronize panel title");
openerSavePanel._tileDrafts["device:room-opener"] = { ...opener, label: "New panel", panel_id: "new-panel" };
await openerSavePanel._saveTile("device", "room-opener", "editor");
assert.equal(openerSavedConfig.panels.find((panel) => panel.id === "new-panel").title, "New panel", "saving folder with a new target must create its panel");

const trayDeletePanel = new Panel();
const trayDeleteConfig = structuredClone(baseConfig);
trayDeleteConfig.extensions.layout_tray = [{ ...normalizedTile, id: "saved-tray", label: "Saved tray" }];
trayDeletePanel._configs = { device: trayDeleteConfig };
trayDeletePanel._layoutDrafts = {};
trayDeletePanel._tileDrafts = {};
trayDeletePanel._tileValidation = {};
trayDeletePanel._tileSaveStatus = {};
trayDeletePanel._tilePreviewStates = {};
trayDeletePanel._render = () => {};
trayDeletePanel._editableTileGroups("device", trayDeleteConfig);
let trayDeleteSavedConfig;
trayDeletePanel._setConfig = async (_device, config) => { trayDeleteSavedConfig = config; return true; };
await trayDeletePanel._deleteTile("device", "saved-tray");
assert.equal(trayDeleteSavedConfig.extensions.layout_tray.length, 0, "deleting a saved tray tile must remove it from extensions");

const conditionalPanel = new Panel();
conditionalPanel._configs = { device: structuredClone(baseConfig) };
conditionalPanel._layoutDrafts = {};
conditionalPanel._tileDrafts = {};
conditionalPanel._tileValidation = {};
conditionalPanel._tileSaveStatus = {};
conditionalPanel._tilePreviewStates = {};
conditionalPanel._expandedTiles = new Set();
assert.equal(conditionalPanel._tileCountLabel(1), "1 kafel", "Polish child count must handle singular");
assert.equal(conditionalPanel._tileCountLabel(5), "5 kafli", "Polish child count must handle plural");
conditionalPanel._language = "en";
assert.equal(conditionalPanel._tileCountLabel(2), "2 tiles", "English child count must use English plural");
conditionalPanel._language = "pl";
const spacerEditor = conditionalPanel._tileEditor("device", { id: "space", kind: "spacer", size: "small", label: "Space", accent: "white" }, "dashboard", true);
assert.doesNotMatch(spacerEditor, /id="tile-dashboard-device-space-entity"/, "spacer editor must hide entity source");
assert.doesNotMatch(spacerEditor, /<h3>Prezentacja<\/h3>/, "spacer editor must hide presentation");
assert.doesNotMatch(spacerEditor, /<h3>Zachowanie<\/h3>/, "spacer editor must hide behavior");
const folderEditor = conditionalPanel._tileEditor("device", { id: "folder", kind: "folder", size: "large", label: "Folder", panel_id: "room", icon: "mdi:folder", accent: "orange" }, "dashboard", true);
assert.match(folderEditor, /id="tile-dashboard-device-folder-panel"/, "folder editor must expose panel ID");
assert.doesNotMatch(folderEditor, /id="tile-dashboard-device-folder-entity"/, "folder editor must hide entity source");
assert.doesNotMatch(folderEditor, /<h3>Zachowanie<\/h3>/, "folder editor must hide implicit navigation behavior");
assert.match(folderEditor, /id="tile-dashboard-device-folder-size"[\s\S]*Pełny/, "Tiles editor must expose the visual tile variant");
assert.doesNotMatch(conditionalPanel._layoutSelectedPanel({ id: "folder", label: "Folder", col: 1, row: 1, colSpan: 2, rowSpan: 2 }), /layout-tile-size/, "Preview must not expose tile variant as geometry");
for (const [kind, expected] of [
  ["entity", ["-entity", "Prezentacja", "Zachowanie"]],
  ["cover", ["-entity", "Prezentacja", "Zachowanie", "<h3>Cover</h3>"]],
  ["camera", ["-entity", "Prezentacja", "Zachowanie"]],
  ["action", ["Prezentacja", "Zachowanie"]],
  ["text", ["-content", "Prezentacja", "Zachowanie"]],
  ["clock", ["Prezentacja", "<h3>Zegar</h3>"]],
]) {
  const html = conditionalPanel._tileEditor("device", { id: `kind-${kind}`, kind, size: "small", label: kind, entity_id: ["entity", "cover", "camera"].includes(kind) ? `${kind === "camera" ? "camera" : kind === "cover" ? "cover" : "light"}.test` : undefined, content: kind === "text" ? "Text" : undefined, icon: "mdi:cog", accent: "orange", tap_action: kind === "action" ? { type: "none" } : undefined }, "dashboard", true);
  for (const marker of expected) assert.ok(html.includes(marker), `${kind} editor must include ${marker}`);
}

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
assert.equal(formPanel._tileWithDraft("device", { ...formTile, size: "small", col: 8 }).size, "large", "tile draft must expose its visual variant");

const positionPanel = new Panel();
const savedPositionTile = { ...normalizedTile, col: 3, row: 2, colSpan: 2, rowSpan: 2 };
const movedPositionTile = { ...savedPositionTile, label: "Moved light", col: 8, row: 5 };
positionPanel._configs = { device: { ...structuredClone(baseConfig), tiles: [savedPositionTile] } };
positionPanel._layoutDrafts = { "device:1:main": { context: { id: "main" }, grid: { columns: 12, rows: 9 }, tiles: [movedPositionTile], tray: [], historyKey: "device:1:main" } };
positionPanel._tileDrafts = {};
positionPanel._tileValidation = {};
positionPanel._tileSaveStatus = {};
positionPanel._render = () => {};
positionPanel._captureTileDraft = () => positionPanel._tileDraft("device", "light");
let positionPatch;
positionPanel._hass = { callService: async (_domain, _service, data) => { positionPatch = data.patch; } };
await positionPanel._saveTile("device", "light", "editor");
assert.deepEqual(
  { col: positionPanel._configs.device.tiles[0].col, row: positionPanel._configs.device.tiles[0].row },
  { col: 8, row: 5 },
  "saving tile authoring must preserve current Preview position",
);
for (const field of ["order", "col", "row", "colSpan", "rowSpan"]) assert.equal(positionPatch.tile_updates[0][field], undefined, `Tiles patch must not own ${field}`);
