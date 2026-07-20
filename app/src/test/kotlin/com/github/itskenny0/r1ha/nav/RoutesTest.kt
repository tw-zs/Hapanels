package com.github.itskenny0.r1ha.nav

import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.Behavior
import com.github.itskenny0.r1ha.core.prefs.OnboardingStage
import com.github.itskenny0.r1ha.core.prefs.ServerConfig
import com.github.itskenny0.r1ha.core.prefs.StartView
import com.github.itskenny0.r1ha.core.prefs.Tokens
import com.github.itskenny0.r1ha.feature.onboarding.resolvedOnboardingStage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutesTest {

    @Test fun missingCredentialsReturnEveryPostAuthStageToConnection() {
        listOf(
            OnboardingStage.PANEL_NAME,
            OnboardingStage.APPEARANCE,
            OnboardingStage.STUDIO,
            OnboardingStage.MQTT,
            OnboardingStage.CHECKLIST,
            OnboardingStage.LAUNCHING,
            OnboardingStage.COMPLETED,
        ).forEach { stage ->
            assertThat(resolvedOnboardingStage(stage, credentialsReady = false))
                .isEqualTo(OnboardingStage.CONNECTION)
        }
    }

    private val server = ServerConfig("https://ha.example.com")
    private val tokens = Tokens("access", "refresh", Long.MAX_VALUE)

    @Test fun startupRequiresServerAndNonBlankAccessToken() {
        assertThat(startupDestination(AppSettings(), tokens)).isEqualTo(Routes.ONBOARDING)
        assertThat(startupDestination(AppSettings(server = server), null)).isEqualTo(Routes.ONBOARDING)
        assertThat(startupDestination(AppSettings(server = server), tokens.copy(accessToken = "  ")))
            .isEqualTo(Routes.ONBOARDING)
        assertThat(startupDestination(AppSettings(server = server), tokens)).isEqualTo(Routes.PANEL_GRID)
    }

    @Test fun readyStartupNeverUsesLegacyDashboard() {
        val destinations = StartView.entries.map { startView ->
            startupDestination(
                AppSettings(server = server, behavior = Behavior(startView = startView)),
                tokens,
            )
        }

        assertThat(destinations).containsExactly(Routes.PANEL_GRID, Routes.CARD_STACK)
        assertThat(destinations).doesNotContain(Routes.DASHBOARD)
    }

    @Test fun incompleteOnboardingResumesEvenWithCredentials() {
        OnboardingStage.entries
            .filterNot { it == OnboardingStage.LEGACY || it == OnboardingStage.COMPLETED }
            .forEach { stage ->
                assertThat(startupDestination(AppSettings(server = server, onboardingStage = stage), tokens))
                    .isEqualTo(Routes.ONBOARDING)
            }
    }

    @Test fun legacyCredentialedInstallDoesNotReenterOnboarding() {
        assertThat(
            startupDestination(
                AppSettings(server = server, onboardingStage = OnboardingStage.LEGACY),
                tokens,
            ),
        ).isEqualTo(Routes.PANEL_GRID)
    }

    @Test fun startViewsMapOnlyToPanelGridAndCards() {
        assertThat(StartView.PANEL_GRID.route()).isEqualTo(Routes.PANEL_GRID)
        assertThat(StartView.CARDS.route()).isEqualTo(Routes.CARD_STACK)
    }

    @Test fun everyLauncherShortcutAvoidsLegacyDashboard() {
        val values = listOf(
            "search", "assist", "panel_grid", "automations", "helpers", "energy",
            "zones", "scenes", "notifications", "cameras", "logbook", "dashboard", "today",
        )

        val destinations = values.map(::shortcutRoute)

        assertThat(destinations).doesNotContain(null)
        assertThat(destinations).doesNotContain(Routes.DASHBOARD)
        assertThat(shortcutRoute("dashboard")).isEqualTo(Routes.PANEL_GRID)
        assertThat(shortcutRoute("today")).isEqualTo(Routes.PANEL_GRID)
        assertThat(shortcutRoute("panel_grid")).isEqualTo(Routes.PANEL_GRID)
    }

    @Test fun launcherShortcutsAreIgnoredDuringOnboarding() {
        assertThat(shortcutRoute("search", Routes.ONBOARDING)).isNull()
        assertThat(shortcutRoute("search", Routes.CARD_STACK)).isEqualTo(Routes.SEARCH)
    }
}
