package com.github.itskenny0.r1ha.core.mqtt

import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

/**
 * Bare-bones MQTT v3.1.1 publish-only client. Implements just enough of the wire
 * protocol to fire a single QoS-0 publish: CONNECT, wait for CONNACK, PUBLISH,
 * DISCONNECT. No subscription, no QoS-1/2 retry, no keep-alive ping (the
 * connection lives for milliseconds; the broker's keep-alive timer never fires).
 *
 * Implemented by hand to avoid pulling in Eclipse Paho (~300 KB lib + its
 * Service-based long-running client) for what is fundamentally a one-shot
 * fire-and-forget message. A future cycle that needs subscriptions or
 * session-resume should swap this for Paho without changing the UI surface.
 *
 * TLS: when [useTls] is true the underlying socket is wrapped in the default
 * SSLSocketFactory; we don't currently support custom trust stores or client
 * certs through this surface (the [com.github.itskenny0.r1ha.core.security.SecurityPolicyStore]
 * config only applies to the HA REST/WS client, not to MQTT brokers).
 */
object MqttPublisher {

    /** Publish [payload] to [topic] on [host]:[port]. Returns true on a clean
     *  CONNACK + PUBLISH dispatch, false on any wire or auth failure. */
    suspend fun publish(
        host: String,
        port: Int,
        topic: String,
        payload: ByteArray,
        clientId: String = "r1ha-${System.currentTimeMillis() and 0xFFFF}",
        username: String? = null,
        password: String? = null,
        useTls: Boolean = false,
        retain: Boolean = false,
        timeoutMs: Int = 10_000,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = openSocket(host, port, useTls, timeoutMs)
            socket.use { s ->
                s.soTimeout = timeoutMs
                val out = DataOutputStream(s.getOutputStream())
                val inp = DataInputStream(s.getInputStream())

                // CONNECT — protocol v3.1.1, clean session, optional username/password.
                writeConnect(out, clientId, username, password)
                out.flush()

                // CONNACK — should be 0x20, remaining-length 2, ack-flags, return-code.
                val ackType = inp.readUnsignedByte()
                if (ackType != 0x20) error("expected CONNACK (0x20), got 0x${ackType.toString(16)}")
                val ackLen = readRemainingLength(inp)
                if (ackLen != 2) error("CONNACK remaining-length must be 2, got $ackLen")
                inp.readUnsignedByte() // session present flag — we always clean-session, so ignored
                val rc = inp.readUnsignedByte()
                if (rc != 0) error("MQTT CONNECT refused, return code=$rc")

                // PUBLISH — QoS 0, no packet identifier.
                writePublish(out, topic, payload, retain)
                out.flush()

                // DISCONNECT — clean shutdown so the broker doesn't log us as a
                // disconnect-without-close.
                out.writeByte(0xE0)
                out.writeByte(0x00)
                out.flush()
            }
            R1Log.i("Mqtt", "published topic=$topic bytes=${payload.size}")
        }.onFailure { t ->
            R1Log.w("Mqtt", "publish '$topic' failed: ${t.message}")
        }
    }

    private fun openSocket(host: String, port: Int, useTls: Boolean, timeoutMs: Int): Socket =
        if (useTls) {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            factory.createSocket().apply {
                connect(java.net.InetSocketAddress(host, port), timeoutMs)
            }
        } else {
            Socket().apply { connect(java.net.InetSocketAddress(host, port), timeoutMs) }
        }

    /** Encode the MQTT v3.1.1 CONNECT packet (variable header + payload),
     *  prefix with the type byte + remaining-length, and write the whole frame. */
    private fun writeConnect(
        out: DataOutputStream,
        clientId: String,
        username: String?,
        password: String?,
    ) {
        val body = java.io.ByteArrayOutputStream()
        val v = DataOutputStream(body)
        // Protocol name "MQTT"
        v.writeShort(4); v.writeBytes("MQTT")
        // Protocol level 4 = v3.1.1
        v.writeByte(0x04)
        // Connect flags
        var flags = 0x02 // clean session
        if (username != null) flags = flags or 0x80
        if (password != null) flags = flags or 0x40
        v.writeByte(flags)
        // Keep alive — 60s. The connection lives milliseconds so the broker's
        // keep-alive timer never fires; still required by the spec.
        v.writeShort(60)
        // Payload — client id (always present), then optional username + password.
        writeUtf(v, clientId)
        if (username != null) writeUtf(v, username)
        if (password != null) {
            // Password field is length-prefixed bytes (binary safe), not utf8.
            val pbytes = password.toByteArray(Charsets.UTF_8)
            v.writeShort(pbytes.size); v.write(pbytes)
        }
        val payload = body.toByteArray()
        out.writeByte(0x10) // CONNECT
        writeRemainingLength(out, payload.size)
        out.write(payload)
    }

    private fun writePublish(
        out: DataOutputStream,
        topic: String,
        payload: ByteArray,
        retain: Boolean,
    ) {
        val body = java.io.ByteArrayOutputStream()
        val v = DataOutputStream(body)
        writeUtf(v, topic)
        v.write(payload)
        val frame = body.toByteArray()
        // PUBLISH header: 0x30 | retain. QoS 0 + DUP 0 = no further bits set.
        val header = 0x30 or (if (retain) 0x01 else 0x00)
        out.writeByte(header)
        writeRemainingLength(out, frame.size)
        out.write(frame)
    }

    private fun writeUtf(v: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        v.writeShort(bytes.size)
        v.write(bytes)
    }

    /** MQTT remaining-length encoding — base-128 varint, max 4 bytes (~268 MB).
     *  Anything larger throws, which would only matter for monster payloads. */
    private fun writeRemainingLength(out: DataOutputStream, length: Int) {
        var remaining = length
        do {
            var b = remaining and 0x7F
            remaining = remaining ushr 7
            if (remaining > 0) b = b or 0x80
            out.writeByte(b)
        } while (remaining > 0)
    }

    private fun readRemainingLength(inp: DataInputStream): Int {
        var multiplier = 1
        var value = 0
        repeat(4) {
            val b = inp.readUnsignedByte()
            value += (b and 0x7F) * multiplier
            if ((b and 0x80) == 0) return value
            multiplier *= 128
        }
        error("remaining-length overflow")
    }
}
