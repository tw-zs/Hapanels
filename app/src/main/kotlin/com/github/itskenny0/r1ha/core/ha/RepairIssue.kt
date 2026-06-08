package com.github.itskenny0.r1ha.core.ha

/**
 * One row from HA's `repairs/list_issues` WS reply.
 *
 * Repairs are HA's mechanism for surfacing config-level problems an integration has
 * detected (deprecated YAML keys, unreachable devices, broken automations) and giving
 * the user a structured "fix flow" rather than a free-form persistent notification.
 *
 * The full payload includes a [data] JsonObject with integration-specific fields the
 * fix flow consumes (which dialog to show next, what config-entry id to operate on,
 * etc.). Hapanels doesn't reimplement the multi-step fix flow — that lives in
 * HA's own frontend — but it surfaces the issue list so a user can see at a glance
 * what's broken and ignore or open-in-browser as needed.
 */
data class RepairIssue(
    /** Integration that raised the issue (e.g. "homeassistant", "config", a custom integration). */
    val domain: String,
    /** Stable id of the issue within [domain]. Combined with [domain] forms the
     *  composite key the `repairs/ignore` and `repairs/show_user_message_flow`
     *  commands use to identify the issue. */
    val issueId: String,
    /**
     * One of {error, warning, critical}. Drives the row's color in the UI: critical
     * shows red, error amber, warning muted. Default to "warning" when HA's payload
     * omits the field rather than dropping the row.
     */
    val severity: String,
    /**
     * When set, points to a translation key in HA's strings.json — the title the
     * user would see in HA's frontend. Bare key shape (e.g. "issue_homeassistant_yaml_deprecated").
     * Hapanels doesn't currently translate these; we render them verbatim
     * as a fallback when no friendlier title is present.
     */
    val translationKey: String?,
    /** Human-readable summary if HA filled it in (some integrations do, most route
     *  through translation keys). Fallback rendering uses [translationKey] when
     *  this is null. */
    val description: String?,
    /** When true the integration recommends the user opens the fix flow rather than
     *  just ignoring it. Surfaced as a colored chip in the UI. */
    val isFixable: Boolean,
    /** Server-side mark: true when the user has previously ignored this issue. The
     *  list endpoint can include ignored issues if the client opts in; we filter
     *  them out by default and show "N IGNORED" badge for the count. */
    val ignored: Boolean,
    /** ISO-8601 timestamp of when the integration first raised this issue. Used to
     *  sort newest-first; null when HA didn't include the field. */
    val createdAt: String?,
)
