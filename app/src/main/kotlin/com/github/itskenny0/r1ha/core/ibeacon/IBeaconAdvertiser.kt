package com.github.itskenny0.r1ha.core.ibeacon

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Broadcasts an iBeacon advertisement so HA's iBeacon integration can pick
 * the device up as a [device_tracker] entity. The Apple iBeacon manufacturer-
 * specific data format is:
 *
 *   byte 0:    0x02   (subtype = iBeacon)
 *   byte 1:    0x15   (subtype length = 21 = UUID(16) + major(2) + minor(2) + tx(1))
 *   bytes 2-17: 128-bit UUID, big-endian
 *   bytes 18-19: major (big-endian uint16)
 *   bytes 20-21: minor (big-endian uint16)
 *   byte 22:    measured TX power at 1 m (signed int8, dBm)
 *
 * Apple's Bluetooth SIG company ID 0x004C is prepended by Android via
 * [AdvertiseData.Builder.addManufacturerData], so the payload above starts at
 * "byte 0".
 *
 * Stateless singleton — at most one advertise session is active at a time;
 * [start] is idempotent (replaces the active session). [stop] is safe to call
 * even when nothing is active.
 */
object IBeaconAdvertiser {

    private const val APPLE_COMPANY_ID = 0x004C
    private const val MEASURED_TX_POWER_DBM_1M = -59 // Conservative; ~ Apple default.

    @Volatile private var current: BluetoothLeAdvertiser? = null
    @Volatile private var callback: AdvertiseCallback? = null

    /** Whether we hold the runtime permission needed to advertise on this Android
     *  version. The legacy BLUETOOTH permission is install-time only and auto-
     *  granted; BLUETOOTH_ADVERTISE on API 31+ is runtime and needs prompting. */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Hardware probe — false on phones without a BLE peripheral chip (rare in 2026
     *  but a real consideration for some budget tablets + the R1's own modem). */
    fun supportsAdvertising(context: Context): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return false
        val mgr = context.getSystemService(BluetoothManager::class.java) ?: return false
        val adapter = mgr.adapter ?: return false
        return adapter.bluetoothLeAdvertiser != null
    }

    /**
     * Start advertising the given iBeacon shape. Idempotent: any prior session is
     * torn down first. Logs a warning and bails when the device lacks the
     * permission or hardware so callers don't have to wrap each call in a
     * try/catch.
     */
    fun start(context: Context, uuid: String, major: Int, minor: Int) {
        stop()
        val ctx = context.applicationContext
        if (!hasPermission(ctx)) {
            R1Log.w("IBeacon", "no BLUETOOTH_ADVERTISE permission; skipping start")
            return
        }
        val mgr = ctx.getSystemService(BluetoothManager::class.java) ?: run {
            R1Log.w("IBeacon", "no BluetoothManager service")
            return
        }
        val adapter = mgr.adapter ?: run {
            R1Log.w("IBeacon", "no Bluetooth adapter")
            return
        }
        if (!adapter.isEnabled) {
            R1Log.w("IBeacon", "Bluetooth adapter disabled by user")
            return
        }
        val advertiser = adapter.bluetoothLeAdvertiser ?: run {
            R1Log.w("IBeacon", "device doesn't support BLE peripheral advertising")
            Toaster.error("This device can't advertise BLE")
            return
        }
        val parsedUuid = runCatching { UUID.fromString(uuid) }.getOrNull() ?: run {
            R1Log.w("IBeacon", "invalid UUID: $uuid")
            return
        }
        val payload = buildIBeaconPayload(
            uuid = parsedUuid,
            major = major.coerceIn(0, 65535),
            minor = minor.coerceIn(0, 65535),
        )
        val settings = AdvertiseSettings.Builder()
            // Low-latency keeps the advertisement frequent enough for HA's iBeacon
            // poller to catch it within a few seconds; the power cost on a phone
            // that's plugged in (kiosk R1) is fine.
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(APPLE_COMPANY_ID, payload)
            .build()
        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                R1Log.i("IBeacon", "advertising: uuid=$uuid major=$major minor=$minor")
            }
            override fun onStartFailure(errorCode: Int) {
                R1Log.w("IBeacon", "start failed: code=$errorCode")
            }
        }
        runCatching {
            advertiser.startAdvertising(settings, data, cb)
            current = advertiser
            callback = cb
        }.onFailure { t ->
            R1Log.w("IBeacon", "startAdvertising threw: ${t.message}")
        }
    }

    fun stop() {
        val adv = current
        val cb = callback
        current = null
        callback = null
        if (adv != null && cb != null) {
            runCatching { adv.stopAdvertising(cb) }
                .onFailure { R1Log.d("IBeacon", "stopAdvertising: ${it.message}") }
        }
    }

    /** Assemble the 23-byte iBeacon manufacturer payload. The leading 0x02 0x15
     *  subtype + length bytes are mandatory; the rest is UUID + major + minor +
     *  TX power calibration. */
    private fun buildIBeaconPayload(uuid: UUID, major: Int, minor: Int): ByteArray {
        val buf = ByteBuffer.allocate(23)
        buf.put(0x02.toByte())
        buf.put(0x15.toByte())
        buf.putLong(uuid.mostSignificantBits)
        buf.putLong(uuid.leastSignificantBits)
        buf.putShort(major.toShort())
        buf.putShort(minor.toShort())
        buf.put(MEASURED_TX_POWER_DBM_1M.toByte())
        return buf.array()
    }
}
