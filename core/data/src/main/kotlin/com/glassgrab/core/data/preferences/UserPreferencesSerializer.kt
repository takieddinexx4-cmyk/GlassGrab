package com.glassgrab.core.data.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.glassgrab.core.domain.model.UserPreferences
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences = try {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) defaultValue else ProtoBuf.decodeFromByteArray(bytes)
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read UserPreferences proto.", e)
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(ProtoBuf.encodeToByteArray(t))
    }
}
