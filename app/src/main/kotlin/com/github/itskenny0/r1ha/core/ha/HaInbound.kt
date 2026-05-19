package com.github.itskenny0.r1ha.core.ha

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface HaInbound {

    @Serializable @SerialName("auth_required")
    data class AuthRequired(@SerialName("ha_version") val haVersion: String? = null) : HaInbound

    @Serializable @SerialName("auth_ok")
    data class AuthOk(@SerialName("ha_version") val haVersion: String? = null) : HaInbound

    @Serializable @SerialName("auth_invalid")
    data class AuthInvalid(val message: String? = null) : HaInbound

    @Serializable @SerialName("result")
    data class Result(
        val id: Int,
        val success: Boolean,
        val result: JsonElement? = null,
        val error: Error? = null,
    ) : HaInbound {
        // HA's error.code is documented as a string (e.g. "not_found") but in older
        // releases and a few integrations it arrives as an integer or a bare null. Accept
        // any JSON shape and coerce to string on read so the strict deserializer doesn't
        // drop the entire frame and leave pendingCalls hanging until the 15s timeout.
        @Serializable
        data class Error(val code: JsonElement? = null, val message: String? = null) {
            val codeString: String?
                get() = when (val c = code) {
                    null -> null
                    is kotlinx.serialization.json.JsonNull -> null
                    is kotlinx.serialization.json.JsonPrimitive -> c.content
                    else -> c.toString()
                }
        }
    }

    @Serializable @SerialName("event")
    data class Event(val id: Int, val event: EventBody) : HaInbound {
        @Serializable
        data class EventBody(val variables: Variables)
        @Serializable
        data class Variables(val trigger: Trigger)
        @Serializable
        data class Trigger(
            val platform: String,
            @SerialName("entity_id") val entityId: String,
            @SerialName("from_state") val fromState: StateBlock? = null,
            @SerialName("to_state") val toState: StateBlock? = null,
        )
        // HA sometimes emits state-change events where to_state.state is missing (entity
        // disappearing) or non-string. Tolerate both rather than failing the whole frame.
        @Serializable
        data class StateBlock(
            @SerialName("entity_id") val entityId: String? = null,
            val state: String? = null,
            val attributes: JsonObject = JsonObject(emptyMap()),
            @SerialName("last_changed") val lastChanged: String? = null,
        )
    }

    @Serializable @SerialName("pong")
    data class Pong(val id: Int? = null) : HaInbound

    /** Catch-all for frames we don't model — keeps the parser non-fatal. */
    @Serializable @SerialName("__unknown__")
    data object Unknown : HaInbound
}
