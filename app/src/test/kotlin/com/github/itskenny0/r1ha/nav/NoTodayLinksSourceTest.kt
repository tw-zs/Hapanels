package com.github.itskenny0.r1ha.nav

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class NoTodayLinksSourceTest {
    @Test fun appHasNoLinksToLegacyTodayRoute() {
        val main = File("src/main")
        val linkedSources = main.walkTopDown()
            .filter { it.extension == "kt" && it.name !in setOf("Routes.kt", "AppNavGraph.kt") }
            .filter { "Routes.DASHBOARD" in it.readText() }
            .map { it.relativeTo(main).path }
            .toList()
        val shortcuts = File(main, "res/xml/shortcuts.xml").readText()

        assertThat(linkedSources).isEmpty()
        assertThat(shortcuts).doesNotContain("android:value=\"today\"")
        assertThat(shortcuts).doesNotContain("android:value=\"dashboard\"")
    }
}
