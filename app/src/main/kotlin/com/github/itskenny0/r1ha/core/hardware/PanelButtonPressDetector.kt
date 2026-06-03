package com.github.itskenny0.r1ha.core.hardware

class PanelButtonPressDetector(
    private val shortPressMaxMs: Long = 500,
    private val longPressMinMs: Long = 1_000,
    private val multiClickTimeoutMs: Long = 400,
    private val emit: (PanelHardwareEvent.Button) -> Unit,
) {
    private val downAt = mutableMapOf<Int, Long>()
    private val pendingClicks = mutableMapOf<Int, PendingClicks>()

    fun onDown(buttonId: Int, nowMs: Long = System.currentTimeMillis()) {
        downAt[buttonId] = nowMs
        emit(PanelHardwareEvent.Button(buttonId, PanelButtonPressType.DOWN, nowMs))
    }

    fun onUp(
        buttonId: Int,
        nowMs: Long = System.currentTimeMillis(),
        emitShortImmediately: Boolean = false,
    ) {
        val started = downAt.remove(buttonId) ?: nowMs
        emit(PanelHardwareEvent.Button(buttonId, PanelButtonPressType.UP, nowMs))
        val duration = nowMs - started
        if (duration >= longPressMinMs) {
            pendingClicks.remove(buttonId)
            emit(PanelHardwareEvent.Button(buttonId, PanelButtonPressType.LONG, nowMs))
            return
        }
        if (duration > shortPressMaxMs) return
        if (emitShortImmediately) {
            pendingClicks.remove(buttonId)
            emit(PanelHardwareEvent.Button(buttonId, PanelButtonPressType.SHORT, nowMs))
            return
        }
        val pending = pendingClicks[buttonId]
        pendingClicks[buttonId] = if (pending == null || nowMs - pending.lastUpMs > multiClickTimeoutMs) {
            PendingClicks(count = 1, lastUpMs = nowMs)
        } else {
            pending.copy(count = (pending.count + 1).coerceAtMost(3), lastUpMs = nowMs)
        }
    }

    fun flush(nowMs: Long = System.currentTimeMillis()) {
        val ready = pendingClicks.filterValues { nowMs - it.lastUpMs >= multiClickTimeoutMs }
        ready.forEach { (buttonId, pending) ->
            val type = when (pending.count) {
                1 -> PanelButtonPressType.SHORT
                2 -> PanelButtonPressType.DOUBLE
                else -> PanelButtonPressType.TRIPLE
            }
            emit(PanelHardwareEvent.Button(buttonId, type, pending.lastUpMs))
            pendingClicks.remove(buttonId)
        }
    }

    private data class PendingClicks(val count: Int, val lastUpMs: Long)
}
