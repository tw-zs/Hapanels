import importlib.util
import unittest
from pathlib import Path


SPEC = importlib.util.spec_from_file_location("hapanels_schema", Path(__file__).parents[1] / "custom_components/hapanels/schema.py")
schema = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(schema)


def valid_config():
    return {
        "version": 2,
        "dashboard_id": "home",
        "revision": 1,
        "updated_by": "test",
        "title": "Home",
        "layout": {"type": "fixed_grid", "columns_landscape": 3, "columns_portrait": 2, "columns": 12, "rows": 9},
        "tiles": [{"id": "settings", "kind": "action", "size": "small", "label": "Settings", "icon": "mdi:cog", "order": 0, "tap_action": {"type": "navigate", "destination": "settings"}}],
        "panels": [],
    }


class DashboardSchemaTest(unittest.TestCase):
    def test_accepts_valid_v2(self):
        self.assertEqual(schema.validate_dashboard_config(valid_config())["version"], 2)

    def test_rejects_unknown_field(self):
        config = valid_config()
        config["layout_editor"] = {}
        with self.assertRaisesRegex(ValueError, "unsupported field"):
            schema.validate_dashboard_config(config)

    def test_rejects_overlap(self):
        config = valid_config()
        config["tiles"][0].update({"col": 1, "row": 1, "colSpan": 2, "rowSpan": 2})
        config["tiles"].append({"id": "text", "kind": "text", "size": "small", "label": "", "icon": "", "order": 1, "content": "Hi", "col": 2, "row": 2})
        with self.assertRaisesRegex(ValueError, "overlap"):
            schema.validate_dashboard_config(config)

    def test_rejects_unknown_action(self):
        config = valid_config()
        config["tiles"][0]["tap_action"] = {"type": "anything"}
        with self.assertRaisesRegex(ValueError, "unsupported"):
            schema.validate_dashboard_config(config)

    def test_accepts_auto_icon_entity_color_and_more_info_hold(self):
        config = valid_config()
        tile = config["tiles"][0]
        tile.update({
            "icon_source": "auto",
            "icon_color_source": "entity",
            "hold_action": {"type": "more_info", "entity_id": "light.kitchen"},
        })

        self.assertEqual(schema.validate_dashboard_config(config)["tiles"][0]["icon_source"], "auto")

    def test_accepts_custom_icon_color(self):
        config = valid_config()
        config["tiles"][0].update({"icon_color_source": "custom", "icon_color": "#12AbEf"})

        self.assertEqual(schema.validate_dashboard_config(config)["tiles"][0]["icon_color"], "#12AbEf")

    def test_rejects_invalid_icon_options(self):
        config = valid_config()
        config["tiles"][0]["icon_source"] = "magic"
        with self.assertRaisesRegex(ValueError, "icon_source"):
            schema.validate_dashboard_config(config)

        config = valid_config()
        config["tiles"][0].update({"icon_color_source": "custom", "icon_color": "orange"})
        with self.assertRaisesRegex(ValueError, "icon_color"):
            schema.validate_dashboard_config(config)

    def test_rejects_more_info_without_entity(self):
        config = valid_config()
        config["tiles"][0]["hold_action"] = {"type": "more_info"}

        with self.assertRaisesRegex(ValueError, "entity_id"):
            schema.validate_dashboard_config(config)

    def test_rejects_hold_action_on_aod(self):
        config = valid_config()
        config["always_on_display"] = {
            "layout": "grid",
            "tiles": [{
                "id": "aod",
                "kind": "entity",
                "size": "small",
                "label": "Lamp",
                "entity_id": "light.kitchen",
                "icon": "mdi:lightbulb",
                "order": 0,
                "hold_action": {"type": "more_info", "entity_id": "light.kitchen"},
            }],
        }

        with self.assertRaisesRegex(ValueError, "read-only"):
            schema.validate_dashboard_config(config)

    def test_patch_validates_new_fields_and_actions(self):
        patch = {
            "base_revision": 1,
            "updated_by": "test",
            "tile_updates": [{
                "id": "lamp",
                "icon_source": "auto",
                "icon_color_source": "custom",
                "icon_color": "#abcdef",
                "hold_action": {"type": "more_info", "entity_id": "light.kitchen"},
            }],
        }

        self.assertEqual(schema.validate_dashboard_patch(patch), patch)


if __name__ == "__main__":
    unittest.main()
