/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore.tink

import androidx.datastore.core.Serializer
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.Buffer

class AeadSerializerTest {

    @Test
    fun serializationWithNoEncryption() = runTest {
        // Arrange.
        val buffer = Buffer()

        // Serialize.
        StringSerializer.writeTo("Unserialized data", buffer.outputStream())

        // Verify that the data is not encrypted.
        assertThat(buffer.snapshot().utf8()).isEqualTo("Unserialized data")

        // Deserialize.
        val deserializedBytes = StringSerializer.readFrom(buffer.inputStream())
        assertThat(deserializedBytes).isEqualTo("Unserialized data")
    }

    @Test
    fun serializationWithEncryption() = runTest {
        AeadConfig.register()
        val aead =
            KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val aeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = StringSerializer,
                associatedData = "associated_data".encodeToByteArray(),
            )
        val buffer = Buffer()

        // Serialize.
        aeadSerializer.writeTo("Unencrypted data", buffer.outputStream())

        // Verify that the data is encrypted.
        assertThat(buffer.snapshot().utf8()).isNotEqualTo("Unencrypted data")

        // Deserialize.
        val decryptedBytes = aeadSerializer.readFrom(buffer.inputStream())
        assertThat(decryptedBytes).isEqualTo("Unencrypted data")
    }

    @Test
    fun serializationWithEncryptionAndAssociatedData() = runTest {
        AeadConfig.register()
        val aead =
            KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val aeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = StringSerializer,
                associatedData = "associated_data".encodeToByteArray(),
            )
        val buffer = Buffer()

        // Serialize.
        aeadSerializer.writeTo("Unencrypted data", buffer.outputStream())

        // Verify that the data is encrypted.
        assertThat(buffer.snapshot().utf8()).isNotEqualTo("Unencrypted data")

        // Deserialize.
        val decryptedBytes = aeadSerializer.readFrom(buffer.inputStream())
        assertThat(decryptedBytes).isEqualTo("Unencrypted data")
    }

    @Test
    fun serializationWithEncryptionAndIncorrectAssociatedData() = runTest {
        AeadConfig.register()
        val aead =
            KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val aeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = StringSerializer,
                associatedData = "associated_data".encodeToByteArray(),
            )
        val buffer = Buffer()

        // Serialize.
        aeadSerializer.writeTo("Unencrypted data", buffer.outputStream())

        // Deserialize with incorrect associated data should throw.
        val incorrectAeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = StringSerializer,
                associatedData = "incorrect_associated_data".encodeToByteArray(),
            )

        assertThrows<GeneralSecurityException> {
            incorrectAeadSerializer.readFrom(buffer.inputStream())
        }
    }

    @Test
    fun serializationWithEncryptionAndIncorrectKey() = runTest {
        AeadConfig.register()
        val aead =
            KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val associatedData = "associated_data".encodeToByteArray()
        val aeadSerializer = AeadSerializer(aead, StringSerializer, associatedData)
        val buffer = Buffer()

        // Serialize.
        aeadSerializer.writeTo("Unencrypted data", buffer.outputStream())

        // Deserialize with incorrect key should throw.
        val wrongAead =
            KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val incorrectAeadSerializer = AeadSerializer(wrongAead, StringSerializer, associatedData)

        assertThrows<GeneralSecurityException> {
            incorrectAeadSerializer.readFrom(buffer.inputStream())
        }
    }

    companion object {
        private object StringSerializer : Serializer<String> {
            override val defaultValue: String = ""

            override suspend fun readFrom(input: InputStream): String {
                return withContext(Dispatchers.IO) { input.readBytes().decodeToString() }
            }

            override suspend fun writeTo(t: String, output: OutputStream) {
                withContext(Dispatchers.IO) { output.write(t.encodeToByteArray()) }
            }
        }
    }
}
