package com.github.itskenny0.r1ha.core.hardware

import androidx.annotation.Keep
import com.github.itskenny0.r1ha.core.util.R1Log

@Keep
class ShellyInputMonitor(
    private val paths: Array<String> = DEFAULT_INPUT_PATHS,
) {
    @Keep
    interface KeyCallback {
        @Keep
        fun onHardwareKey(keyCode: Int, action: Int, repeatCount: Int)
    }

    private val nativeAvailable: Boolean = runCatching {
        System.loadLibrary("shellyinput")
        true
    }.onFailure { t ->
        R1Log.w("ShellyInputMonitor", "native library unavailable: ${t.message}")
    }.getOrDefault(false)

    fun start(callback: KeyCallback): Boolean {
        if (!nativeAvailable) return false
        return runCatching { nativeStart(callback, paths) }
            .onFailure { t -> R1Log.w("ShellyInputMonitor", "nativeStart failed: ${t.message}", t) }
            .getOrDefault(false)
    }

    fun stop() {
        if (!nativeAvailable) return
        runCatching { nativeStop() }
            .onFailure { t -> R1Log.w("ShellyInputMonitor", "nativeStop failed: ${t.message}", t) }
    }

    @Keep
    private external fun nativeStart(callback: KeyCallback, paths: Array<String>): Boolean

    @Keep
    private external fun nativeStop()

    companion object {
        private val DEFAULT_INPUT_PATHS = Array(8) { index -> "/dev/input/event$index" }
    }
}
