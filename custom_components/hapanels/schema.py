from __future__ import annotations

from typing import Any


CURRENT_SCHEMA_VERSION = 2
TILE_KINDS = {"clock", "category", "action", "entity", "cover", "camera", "folder", "popup", "text", "spacer"}
ACTION_TYPES = {"none", "entity_default", "navigate", "local_panel"}
PANEL_OPENERS = {"folder", "popup", "category"}
ENTITY_KINDS = {"entity", "cover", "camera"}
AOD_KINDS = {"clock", "entity", "text"}
SAFE_DEFAULT_DOMAINS = {"light", "switch", "input_boolean", "automation", "fan", "scene", "script", "button", "input_button"}
DESTINATIONS = {"settings", "settings/appearance", "settings/behaviour", "settings/integrations", "panel_diagnostics"}
LOCAL_ACTIONS = {"screen.aod_now", "connection.reconnect_home_assistant"}
CONFIRMATIONS = {"unlock", "cover_move", "delete_tile", "delete_panel", "clear_tray", "disarm_alarm", "custom"}

ROOT_FIELDS = {"version", "dashboard_id", "revision", "updated_by", "title", "layout", "theme", "always_on_display", "people", "tiles", "panels", "camera_actions", "extensions", "migration_report"}
LAYOUT_FIELDS = {"type", "columns_landscape", "columns_portrait", "gap", "columns", "rows"}
PANEL_FIELDS = {"id", "title", "layout", "tiles"}
TILE_FIELDS = {"id", "kind", "size", "label", "short_label", "entity_id", "panel_id", "icon", "accent", "order", "col", "row", "colSpan", "rowSpan", "clock_style", "cover_visual", "cover_direction", "tap_action", "hold_action", "content", "summary", "secondary", "presentation", "legacy_action"}
PATCH_TILE_FIELDS = TILE_FIELDS - {"legacy_action"}
ACTION_FIELDS = {"type", "destination", "action", "entity_id", "panel_id", "domain", "service", "target", "data", "confirmation"}
PRESENTATION_FIELDS = {"show_icon", "show_label", "show_value", "show_secondary", "background", "border", "content_alignment"}
CONFIRMATION_FIELDS = {"required", "kind", "title", "body", "negative_label", "positive_label"}
AOD_FIELDS = {"enabled", "layout", "clock_style", "grid_layout", "timeout_sec", "brightness_percent", "wake_fade_ms", "background", "tiles"}
THEME_FIELDS = {"preset", "mode"}
PERSON_FIELDS = {"id", "name", "state", "status"}
PATCH_FIELDS = {"base_revision", "updated_by", "surface", "theme", "aod_clock_style", "tile_updates"}


def validate_dashboard_config(config: Any) -> dict[str, Any]:
    if not isinstance(config, dict):
        raise ValueError("config must be an object")
    version = config.get("version")
    if version not in (1, CURRENT_SCHEMA_VERSION):
        raise ValueError(f"unsupported dashboard schema version: {version}")
    for field in ("dashboard_id", "revision", "updated_by", "layout", "tiles"):
        if field not in config:
            raise ValueError(f"missing config field: {field}")
    if not isinstance(config["dashboard_id"], str) or not config["dashboard_id"].strip():
        raise ValueError("dashboard_id must not be blank")
    if not isinstance(config["revision"], int) or isinstance(config["revision"], bool) or config["revision"] < 0:
        raise ValueError("revision must be a non-negative integer")
    if not isinstance(config["updated_by"], str) or not config["updated_by"].strip():
        raise ValueError("updated_by must not be blank")
    if version == 1:
        if not isinstance(config["layout"], dict) or not isinstance(config["tiles"], list):
            raise ValueError("layout must be an object and tiles must be an array")
        return config

    _reject_unknown(config, ROOT_FIELDS, "config")
    _validate_layout(config["layout"], "layout")
    theme = config.get("theme", {})
    if not isinstance(theme, dict):
        raise ValueError("theme must be an object")
    _reject_unknown(theme, THEME_FIELDS, "theme")
    if theme.get("mode", "dark") not in {"light", "dark", "system"}:
        raise ValueError("theme.mode is unsupported")
    people = config.get("people", [])
    if not isinstance(people, list):
        raise ValueError("people must be an array")
    for index, person in enumerate(people):
        if not isinstance(person, dict):
            raise ValueError(f"people[{index}] must be an object")
        _reject_unknown(person, PERSON_FIELDS, f"people[{index}]")
    panels = config.get("panels", [])
    if not isinstance(config["tiles"], list) or not isinstance(panels, list):
        raise ValueError("tiles and panels must be arrays")
    panel_ids = []
    for index, panel in enumerate(panels):
        path = f"panels[{index}]"
        if not isinstance(panel, dict):
            raise ValueError(f"{path} must be an object")
        _reject_unknown(panel, PANEL_FIELDS, path)
        panel_id = panel.get("id")
        if not isinstance(panel_id, str) or not panel_id:
            raise ValueError(f"{path}.id must not be blank")
        panel_ids.append(panel_id)
        _validate_layout(panel.get("layout"), f"{path}.layout")
        if not isinstance(panel.get("tiles", []), list):
            raise ValueError(f"{path}.tiles must be an array")
    _reject_duplicates(panel_ids, "panels")

    all_ids = []
    _validate_tiles(config["tiles"], config["layout"], "tiles", set(panel_ids), all_ids)
    for index, panel in enumerate(panels):
        _validate_tiles(panel.get("tiles", []), panel["layout"], f"panels[{index}].tiles", set(panel_ids), all_ids)
    aod = config.get("always_on_display", {})
    if not isinstance(aod, dict):
        raise ValueError("always_on_display must be an object")
    _reject_unknown(aod, AOD_FIELDS, "always_on_display")
    if aod.get("layout", "minimal_clock") not in {"minimal_clock", "status_strip", "grid"}:
        raise ValueError("always_on_display.layout is unsupported")
    if "entity_ids" in aod:
        raise ValueError("always_on_display.entity_ids is unsupported in schema v2")
    aod_tiles = aod.get("tiles", [])
    if not isinstance(aod_tiles, list):
        raise ValueError("always_on_display.tiles must be an array")
    _validate_tiles(aod_tiles, aod.get("grid_layout", config["layout"]), "always_on_display.tiles", set(), all_ids, aod=True)
    limit = 4 if aod.get("layout") == "status_strip" else 6
    if len(aod_tiles) > limit:
        raise ValueError(f"always_on_display.tiles supports at most {limit} items")
    if not _int_in_range(aod.get("timeout_sec", 300), 5, 86400):
        raise ValueError("always_on_display.timeout_sec must be 5..86400")
    if not _int_in_range(aod.get("brightness_percent", 3), 1, 100):
        raise ValueError("always_on_display.brightness_percent must be 1..100")
    if not _int_in_range(aod.get("wake_fade_ms", 500), 0, 2000):
        raise ValueError("always_on_display.wake_fade_ms must be 0..2000")
    _reject_duplicates(all_ids, "tiles")
    _validate_panel_graph(config["tiles"], panels, set(panel_ids))
    return config


def validate_dashboard_patch(patch: Any) -> dict[str, Any]:
    if not isinstance(patch, dict):
        raise ValueError("patch must be an object")
    _reject_unknown(patch, PATCH_FIELDS, "patch")
    if not isinstance(patch.get("base_revision"), int) or isinstance(patch.get("base_revision"), bool) or patch["base_revision"] < 0:
        raise ValueError("base_revision must be a non-negative integer")
    if not isinstance(patch.get("updated_by"), str) or not patch["updated_by"].strip():
        raise ValueError("updated_by must not be blank")
    if patch.get("surface", "dashboard") not in {"dashboard", "aod"}:
        raise ValueError("surface must be dashboard or aod")
    updates = patch.get("tile_updates", [])
    if not isinstance(updates, list):
        raise ValueError("tile_updates must be an array")
    for index, update in enumerate(updates):
        if not isinstance(update, dict) or not isinstance(update.get("id"), str) or not update["id"]:
            raise ValueError(f"tile_updates[{index}].id is required")
        _reject_unknown(update, PATCH_TILE_FIELDS, f"tile_updates[{index}]")
    return patch


def _validate_layout(layout: Any, path: str) -> None:
    if not isinstance(layout, dict):
        raise ValueError(f"{path} must be an object")
    _reject_unknown(layout, LAYOUT_FIELDS, path)
    if layout.get("type") != "fixed_grid":
        raise ValueError(f"{path}.type must be fixed_grid")
    if layout.get("gap", "medium") not in {"small", "medium", "large"}:
        raise ValueError(f"{path}.gap is unsupported")
    for field in ("columns_landscape", "columns_portrait"):
        if not _positive_int(layout.get(field)):
            raise ValueError(f"{path}.{field} must be positive")
    for field in ("columns", "rows"):
        if field in layout and not _positive_int(layout[field]):
            raise ValueError(f"{path}.{field} must be positive")


def _validate_tiles(tiles: list[Any], layout: dict[str, Any], path: str, panel_ids: set[str], all_ids: list[str], aod: bool = False) -> None:
    columns = layout.get("columns", 12)
    rows = layout.get("rows", 9)
    placed = []
    for index, tile in enumerate(tiles):
        item_path = f"{path}[{index}]"
        if not isinstance(tile, dict):
            raise ValueError(f"{item_path} must be an object")
        _reject_unknown(tile, TILE_FIELDS, item_path)
        tile_id = tile.get("id")
        kind = tile.get("kind")
        if not isinstance(tile_id, str) or not tile_id:
            raise ValueError(f"{item_path}.id is required")
        all_ids.append(tile_id)
        if kind not in TILE_KINDS or aod and kind not in AOD_KINDS:
            raise ValueError(f"{item_path}.kind is unsupported")
        if tile.get("size") not in {"large", "small", "action"}:
            raise ValueError(f"{item_path}.size is unsupported")
        if kind not in {"text", "spacer"} and (not isinstance(tile.get("label"), str) or not tile["label"]):
            raise ValueError(f"{item_path}.label is required")
        if kind not in {"text", "spacer", "clock"} and (not isinstance(tile.get("icon"), str) or not tile["icon"]):
            raise ValueError(f"{item_path}.icon is required")
        if tile.get("accent", "orange") not in {"orange", "white", "red"}:
            raise ValueError(f"{item_path}.accent is unsupported")
        if not isinstance(tile.get("order"), int) or isinstance(tile.get("order"), bool):
            raise ValueError(f"{item_path}.order must be an integer")
        if kind in ENTITY_KINDS and not _entity_id(tile.get("entity_id")):
            raise ValueError(f"{item_path}.entity_id is required")
        if kind == "text" and not tile.get("content"):
            raise ValueError(f"{item_path}.content is required")
        if kind in {"folder", "popup"} and tile.get("panel_id") not in panel_ids:
            raise ValueError(f"{item_path}.panel_id is invalid")
        if kind == "category" and not tile.get("legacy_action") and tile.get("panel_id") not in panel_ids:
            raise ValueError(f"{item_path}.panel_id is invalid")
        for name in ("tap_action", "hold_action"):
            action = tile.get(name)
            if action is not None:
                _validate_action(action, f"{item_path}.{name}", panel_ids)
        if tile.get("hold_action") is not None:
            raise ValueError(f"{item_path}.hold_action is unsupported")
        if kind == "action" and tile.get("tap_action") is None:
            raise ValueError(f"{item_path}.tap_action is required")
        if kind == "spacer" and any(tile.get(field) is not None for field in ("entity_id", "tap_action", "hold_action", "presentation")):
            raise ValueError(f"{item_path}: spacer cannot define entity, actions, or presentation")
        presentation = tile.get("presentation")
        if presentation is not None:
            if not isinstance(presentation, dict):
                raise ValueError(f"{item_path}.presentation must be an object")
            _reject_unknown(presentation, PRESENTATION_FIELDS, f"{item_path}.presentation")
            if presentation.get("background", "surface") not in {"surface", "transparent"} or presentation.get("border", "default") not in {"default", "none"} or presentation.get("content_alignment", "center") not in {"start", "center", "end"}:
                raise ValueError(f"{item_path}.presentation contains unsupported value")
        if tile.get("accent") == "red" and not _has_unsafe_confirmation(tile):
            raise ValueError(f"{item_path}.accent red requires unsafe confirmation")
        col, row = tile.get("col"), tile.get("row")
        if (col is None) != (row is None):
            raise ValueError(f"{item_path}: col and row must be set together")
        if col is not None:
            col_span, row_span = tile.get("colSpan", 1), tile.get("rowSpan", 1)
            if not all(_positive_int(value) for value in (col, row, col_span, row_span)) or col + col_span - 1 > columns or row + row_span - 1 > rows:
                raise ValueError(f"{item_path} is outside {columns}x{rows} grid")
            for other_id, other_col, other_row, other_col_span, other_row_span in placed:
                if col < other_col + other_col_span and col + col_span > other_col and row < other_row + other_row_span and row + row_span > other_row:
                    raise ValueError(f"{path}: tiles {other_id} and {tile_id} overlap")
            placed.append((tile_id, col, row, col_span, row_span))


def _validate_action(action: Any, path: str, panel_ids: set[str]) -> None:
    if not isinstance(action, dict):
        raise ValueError(f"{path} must be an object")
    _reject_unknown(action, ACTION_FIELDS, path)
    action_type = action.get("type")
    if action_type not in ACTION_TYPES:
        raise ValueError(f"{path}.type is unsupported")
    if action_type == "entity_default" and (not _entity_id(action.get("entity_id")) or action["entity_id"].split(".", 1)[0] not in SAFE_DEFAULT_DOMAINS):
        raise ValueError(f"{path}.entity_id has no safe default action")
    if action_type == "navigate":
        destination, panel_id = action.get("destination"), action.get("panel_id")
        if bool(destination) == bool(panel_id) or destination and destination not in DESTINATIONS or panel_id and panel_id not in panel_ids:
            raise ValueError(f"{path} has invalid navigation target")
    if action_type == "local_panel" and action.get("action") not in LOCAL_ACTIONS:
        raise ValueError(f"{path}.action is unsupported")
    confirmation = action.get("confirmation")
    if confirmation is not None:
        if not isinstance(confirmation, dict):
            raise ValueError(f"{path}.confirmation must be an object")
        _reject_unknown(confirmation, CONFIRMATION_FIELDS, f"{path}.confirmation")
        if confirmation.get("kind") not in CONFIRMATIONS:
            raise ValueError(f"{path}.confirmation.kind is unsupported")


def _validate_panel_graph(root_tiles: list[dict[str, Any]], panels: list[dict[str, Any]], panel_ids: set[str]) -> None:
    targets = lambda tiles: [tile.get("panel_id") for tile in tiles if tile.get("kind") in PANEL_OPENERS and tile.get("panel_id")]
    edges = {panel["id"]: targets(panel.get("tiles", [])) for panel in panels}

    def visit(panel_id: str, path: list[str]) -> None:
        if panel_id in path:
            raise ValueError(f"panels contain cycle: {' -> '.join(path + [panel_id])}")
        if len(path) >= 3:
            raise ValueError(f"panels exceed depth 3 at {panel_id}")
        for target in edges.get(panel_id, []):
            if target not in panel_ids:
                raise ValueError(f"panel_id target does not exist: {target}")
            visit(target, path + [panel_id])

    for target in targets(root_tiles):
        visit(target, [])


def _reject_unknown(value: dict[str, Any], allowed: set[str], path: str) -> None:
    unknown = sorted(set(value) - allowed)
    if unknown:
        raise ValueError(f"{path} contains unsupported field: {unknown[0]}")


def _reject_duplicates(values: list[str], path: str) -> None:
    seen = set()
    for value in values:
        if value in seen:
            raise ValueError(f"{path} contains duplicate id: {value}")
        seen.add(value)


def _positive_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value > 0


def _int_in_range(value: Any, minimum: int, maximum: int) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and minimum <= value <= maximum


def _entity_id(value: Any) -> bool:
    if not isinstance(value, str) or value.count(".") != 1:
        return False
    domain, object_id = value.split(".")
    return bool(domain and object_id and all(char.islower() or char.isdigit() or char == "_" for char in value.replace(".", "")))


def _has_unsafe_confirmation(tile: dict[str, Any]) -> bool:
    return any(
        isinstance(action, dict)
        and isinstance(action.get("confirmation"), dict)
        and action["confirmation"].get("required", True)
        and action["confirmation"].get("kind") in CONFIRMATIONS
        for action in (tile.get("tap_action"), tile.get("hold_action"))
    )
