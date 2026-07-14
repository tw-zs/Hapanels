package com.github.itskenny0.r1ha.core.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ShortcutBusTest {
    @Test fun requestBeforeSubscribeIsDelivered() = runTest {
        ShortcutBus.request("search")

        assertThat(ShortcutBus.requests.first()).isEqualTo("search")
    }

    @Test fun requestWithActiveSubscriberIsDelivered() = runTest {
        val route = async(start = CoroutineStart.UNDISPATCHED) { ShortcutBus.requests.first() }

        ShortcutBus.request("assist")

        assertThat(route.await()).isEqualTo("assist")
    }
}
