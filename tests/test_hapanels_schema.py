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


if __name__ == "__main__":
    unittest.main()
