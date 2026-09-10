package com.github.itskenny0.r1ha.core.mqtt

import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory

class MqttSession(
    private val host: String,
    private val port: Int,
    private val clientId: String,
    private val username: String? = null,
    private val password: String? = null,
    private val useTls: Boolean = false,
    private val willTopic: String? = null,
    private val willPayload: ByteArray? = null,
    private val willRetain: Boolean = false,
    private val onMessage: suspend (topic: String, payload: ByteArray) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val writeMutex = Mutex()
    private val packetIds = AtomicInteger(1)
    private var connection: MqttConnection? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var readerJob: Job? = null
    private var pingJob: Job? = null
    @Volatile private var connected = false
    @Volatile private var closed = false

    val isConnected: Boolean
        get() = connected && !closed

    suspend fun connect(timeoutMs: Int = 10_000): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val nextConnection = openConnection(host, port, useTls, timeoutMs)
            val nextOutput = DataOutputStream(nextConnection.output)
            val nextInput = DataInputStream(nextConnection.input)
            writeConnect(nextOutput, clientId, username, password, willTopic, willPayload, willRetain)
            nextOutput.flush()
            val ackType = nextInput.readUnsignedByte()
            if (ackType != 0x20) error("expected CONNACK, got 0x${ackType.toString(16)}")
            val ackLen = readRemainingLength(nextInput)
            if (ackLen != 2) error("CONNACK remaining-length must be 2, got $ackLen")
            nextInput.readUnsignedByte()
            val rc = nextInput.readUnsignedByte()
            if (rc != 0) error("MQTT CONNECT refused, return code=$rc")
            nextConnection.setTimeout(0)
            connection = nextConnection
            input = nextInput
            output = nextOutput
            connected = true
            closed = false
            readerJob = scope.launch { readLoop() }
            pingJob = scope.launch { pingLoop() }
            R1Log.i("MqttSession", "connected host=$host port=$port clientId=$clientId")
        }.onFailure { t ->
            closeQuietly()
            R1Log.w("MqttSession", "connect failed host=$host port=$port: ${t.message}")
        }
    }

    suspend fun publish(topic: String, payload: ByteArray, retain: Boolean = false): Result<Unit> = runCatching {
        writeFrame { out -> writePublish(out, topic, payload, retain) }
    }.onFailure { t ->
        R1Log.w("MqttSession", "publish failed topic=$topic: ${t.message}")
    }

    suspend fun subscribe(topicFilter: String): Result<Unit> = runCatching {
        writeFrame { out -> writeSubscribe(out, topicFilter) }
        R1Log.i("MqttSession", "subscribed topic=$topicFilter")
    }.onFailure { t ->
        R1Log.w("MqttSession", "subscribe failed topic=$topicFilter: ${t.message}")
    }

    suspend fun disconnect() {
        closed = true
        runCatching { writeFrame { out -> out.writeByte(0xE0); out.writeByte(0x00) } }
        closeQuietly()
    }

    private suspend fun writeFrame(write: (DataOutputStream) -> Unit) {
        val out = output ?: error("MQTT session is not connected")
        writeMutex.withLock {
            write(out)
            out.flush()
        }
    }

    private suspend fun readLoop() = withContext(Dispatchers.IO) {
        val inp = input ?: return@withContext
        while (!closed && scope.isActive) {
            runCatching {
                val header = inp.readUnsignedByte()
                val remainingLength = readRemainingLength(inp)
                val body = ByteArray(remainingLength)
                inp.readFully(body)
                if (header shr 4 == 3) handlePublish(header, body)
            }.onFailure { t ->
                if (!closed) R1Log.w("MqttSession", "read loop stopped: ${t.message}")
                closeQuietly()
                return@withContext
            }
        }
    }

    private suspend fun pingLoop() {
        while (!closed && scope.isActive) {
            delay(30_000)
            runCatching { writeFrame { out -> out.writeByte(0xC0); out.writeByte(0x00) } }
                .onFailure { t ->
                    if (!closed) R1Log.w("MqttSession", "ping failed: ${t.message}")
                    closeQuietly()
                    return
                }
        }
    }

    private suspend fun handlePublish(header: Int, body: ByteArray) {
        val topicLength = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
        val topic = body.decodeToString(startIndex = 2, endIndex = 2 + topicLength)
        val qos = (header and 0x06) shr 1
        val packetIdOffset = 2 + topicLength
        val payloadStart = packetIdOffset + if (qos > 0) 2 else 0
        if (payloadStart > body.size) return
        val packetId = if (qos > 0) {
            ((body[packetIdOffset].toInt() and 0xFF) shl 8) or (body[packetIdOffset + 1].toInt() and 0xFF)
        } else {
            null
        }
        onMessage(topic, body.copyOfRange(payloadStart, body.size))
        if (qos == 1 && packetId != null) {
            writeFrame { out ->
                out.writeByte(0x40)
                out.writeByte(0x02)
                out.writeShort(packetId)
            }
        }
    }

    private fun writeSubscribe(out: DataOutputStream, topicFilter: String) {
        val body = ByteArrayOutputStream()
        val v = DataOutputStream(body)
        v.writeShort(packetIds.getAndUpdate { if (it == 65_535) 1 else it + 1 })
        writeUtf(v, topicFilter)
        v.writeByte(1) // QoS 1 for important sync commands
        val payload = body.toByteArray()
        out.writeByte(0x82)
        writeRemainingLength(out, payload.size)
        out.write(payload)
    }

    private fun closeQuietly() {
        closed = true
        readerJob?.cancel()
        pingJob?.cancel()
        runCatching { connection?.close() }
        connection = null
        input = null
        output = null
        connected = false
    }
}

private data class MqttConnection(
    val input: InputStream,
    val output: OutputStream,
    val setTimeout: (Int) -> Unit,
    val close: () -> Unit,
)

private fun openConnection(host: String, port: Int, useTls: Boolean, timeoutMs: Int): MqttConnection {
    val uri = runCatching { URI(host) }.getOrNull()
    if (uri?.scheme !in setOf("ws", "wss")) {
        val socket = openSocket(host, port, useTls, timeoutMs)
        return MqttConnection(socket.getInputStream(), socket.getOutputStream(), { socket.soTimeout = it }, socket::close)
    }
    val websocketUri = requireNotNull(uri)
    val websocketHost = websocketUri.host ?: error("WSS URL must include a host")
    val socket = openSocket(websocketHost, if (websocketUri.port == -1) if (websocketUri.scheme == "wss") 443 else 80 else websocketUri.port, websocketUri.scheme == "wss", timeoutMs)
    socket.soTimeout = timeoutMs
    val rawInput = socket.getInputStream()
    val rawOutput = socket.getOutputStream()
    websocketHandshake(rawInput, rawOutput, websocketHost, websocketUri.rawPath.orEmpty().ifBlank { "/" }.let { path -> path + (websocketUri.rawQuery?.let { "?$it" }.orEmpty()) })
    val websocketOutput = WebSocketOutputStream(rawOutput)
    val websocketInput = WebSocketInputStream(rawInput, websocketOutput)
    return MqttConnection(websocketInput, websocketOutput, { socket.soTimeout = it }, socket::close)
}

private fun openSocket(host: String, port: Int, useTls: Boolean, timeoutMs: Int): Socket =
    if (useTls) {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        factory.createSocket().apply { connect(InetSocketAddress(host, port), timeoutMs) }
    } else {
        Socket().apply { connect(InetSocketAddress(host, port), timeoutMs) }
    }

private fun websocketHandshake(input: InputStream, output: OutputStream, host: String, path: String) {
    val nonce = ByteArray(16).also(SecureRandom()::nextBytes)
    val key = Base64.getEncoder().encodeToString(nonce)
    output.write(("GET $path HTTP/1.1\r\nHost: $host\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: $key\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Protocol: mqtt\r\n\r\n").toByteArray())
    output.flush()
    val headers = generateSequence { input.readHttpLine() }.takeWhile { it.isNotEmpty() }.toList()
    check(headers.firstOrNull()?.contains(" 101 ") == true) { "WSS upgrade rejected: ${headers.firstOrNull().orEmpty()}" }
    val accept = headers.firstOrNull { it.startsWith("Sec-WebSocket-Accept:", ignoreCase = true) }?.substringAfter(':')?.trim()
    val expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
    check(accept == expected) { "WSS server returned an invalid handshake" }
}

private fun InputStream.readHttpLine(): String? {
    val bytes = ByteArrayOutputStream()
    while (true) {
        val value = read()
        if (value < 0) return if (bytes.size() == 0) null else bytes.toString(Charsets.UTF_8.name())
        if (value == '\n'.code) return bytes.toString(Charsets.UTF_8.name()).trimEnd('\r')
        bytes.write(value)
        check(bytes.size() <= 8_192) { "WSS response header is too large" }
    }
}

private class WebSocketOutputStream(private val output: OutputStream) : OutputStream() {
    private val pending = ByteArrayOutputStream()

    override fun write(value: Int) = pending.write(value)
    override fun write(bytes: ByteArray, offset: Int, length: Int) = pending.write(bytes, offset, length)

    @Synchronized
    override fun flush() {
        val payload = pending.toByteArray()
        pending.reset()
        if (payload.isNotEmpty()) writeFrame(0x2, payload)
        output.flush()
    }

    @Synchronized
    fun pong(payload: ByteArray) {
        writeFrame(0xA, payload)
        output.flush()
    }

    private fun writeFrame(opcode: Int, payload: ByteArray) {
        output.write(0x80 or opcode)
        when {
            payload.size < 126 -> output.write(0x80 or payload.size)
            payload.size <= 0xFFFF -> {
                output.write(0x80 or 126)
                output.write(payload.size ushr 8)
                output.write(payload.size)
            }
            else -> error("WSS MQTT frame is too large")
        }
        val mask = ByteArray(4).also(SecureRandom()::nextBytes)
        output.write(mask)
        output.write(ByteArray(payload.size) { index -> (payload[index].toInt() xor mask[index % 4].toInt()).toByte() })
    }
}

private class WebSocketInputStream(
    private val input: InputStream,
    private val output: WebSocketOutputStream,
) : InputStream() {
    private var payload = ByteArray(0)
    private var position = 0

    override fun read(): Int {
        if (position == payload.size && !readFrame()) return -1
        return payload[position++].toInt() and 0xFF
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (position == payload.size && !readFrame()) return -1
        val count = minOf(length, payload.size - position)
        payload.copyInto(bytes, offset, position, position + count)
        position += count
        return count
    }

    private fun readFrame(): Boolean {
        while (true) {
            val first = input.read()
            if (first < 0) return false
            val second = input.read()
            check(second >= 0) { "WSS frame ended unexpectedly" }
            val opcode = first and 0x0F
            var length = second and 0x7F
            if (length == 126) length = (input.read() shl 8) or input.read()
            check(length in 0..1_048_576) { "WSS frame is too large" }
            val mask = if ((second and 0x80) != 0) ByteArray(4).also { input.readFully(it) } else null
            val frame = ByteArray(length).also { input.readFully(it) }
            if (mask != null) frame.indices.forEach { frame[it] = (frame[it].toInt() xor mask[it % 4].toInt()).toByte() }
            when (opcode) {
                0x2, 0x0 -> {
                    payload = frame
                    position = 0
                    return true
                }
                0x8 -> return false
                0x9 -> output.pong(frame)
            }
        }
    }
}

private fun InputStream.readFully(bytes: ByteArray) {
    var offset = 0
    while (offset < bytes.size) {
        val count = read(bytes, offset, bytes.size - offset)
        check(count > 0) { "WSS frame ended unexpectedly" }
        offset += count
    }
}

private fun writeConnect(
    out: DataOutputStream,
    clientId: String,
    username: String?,
    password: String?,
    willTopic: String?,
    willPayload: ByteArray?,
    willRetain: Boolean,
) {
    val body = ByteArrayOutputStream()
    val v = DataOutputStream(body)
    v.writeShort(4)
    v.writeBytes("MQTT")
    v.writeByte(0x04)
    var flags = 0x02
    if (willTopic != null && willPayload != null) flags = flags or 0x04 or (if (willRetain) 0x20 else 0x00)
    if (username != null) flags = flags or 0x80
    if (password != null) flags = flags or 0x40
    v.writeByte(flags)
    v.writeShort(60)
    writeUtf(v, clientId)
    if (willTopic != null && willPayload != null) {
        writeUtf(v, willTopic)
        v.writeShort(willPayload.size)
        v.write(willPayload)
    }
    if (username != null) writeUtf(v, username)
    if (password != null) {
        val bytes = password.toByteArray(Charsets.UTF_8)
        v.writeShort(bytes.size)
        v.write(bytes)
    }
    val payload = body.toByteArray()
    out.writeByte(0x10)
    writeRemainingLength(out, payload.size)
    out.write(payload)
}

private fun writePublish(out: DataOutputStream, topic: String, payload: ByteArray, retain: Boolean) {
    val body = ByteArrayOutputStream()
    val v = DataOutputStream(body)
    writeUtf(v, topic)
    v.write(payload)
    val frame = body.toByteArray()
    out.writeByte(0x30 or (if (retain) 0x01 else 0x00))
    writeRemainingLength(out, frame.size)
    out.write(frame)
}

private fun writeUtf(out: DataOutputStream, value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    out.writeShort(bytes.size)
    out.write(bytes)
}

private fun writeRemainingLength(out: DataOutputStream, length: Int) {
    var remaining = length
    do {
        var b = remaining and 0x7F
        remaining = remaining ushr 7
        if (remaining > 0) b = b or 0x80
        out.writeByte(b)
    } while (remaining > 0)
}

private fun readRemainingLength(input: DataInputStream): Int {
    var multiplier = 1
    var value = 0
    repeat(4) {
        val b = input.readUnsignedByte()
        value += (b and 0x7F) * multiplier
        if ((b and 0x80) == 0) return value
        multiplier *= 128
    }
    error("remaining-length overflow")
}
