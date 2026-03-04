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

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
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
import kotlin.jvm.java
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class TinkDataStoreTest {

    @get:Rule val temporaryFolder = TemporaryFolder()

    @BeforeTest
    fun setUp() {
        AeadConfig.register()
    }

    @Test
    fun dataStore_readWrite() = runTest {
        val file = temporaryFolder.newFile("dataStore.pb")
        val dataStore =
            DataStoreFactory.create(
                storage =
                    FileStorage(
                        serializer =
                            AeadSerializer(
                                aead =
                                    KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
                                        .getPrimitive(
                                            RegistryConfiguration.get(),
                                            Aead::class.java,
                                        ),
                                wrappedSerializer = StringSerializer,
                                associatedData = file.absolutePath.encodeToByteArray(),
                            ),
                        produceFile = { file },
                    )
            )

        // Write data.
        dataStore.updateData { "Unencrypted Data" }

        // Ensure file is encrypted
        assertThat(String(file.readBytes())).isNotEqualTo("Unencrypted Data")

        // Read data.
        assertThat(dataStore.data.first()).isEqualTo("Unencrypted Data")
    }

    @Test
    fun dataStore_multipleInstances_sameKeyAndAssociatedData() = runTest {
        val file = temporaryFolder.newFile("dataStore.pb")
        val keySetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

        val scope = CoroutineScope(coroutineContext + Job())
        val dataStore1 =
            DataStoreFactory.create(
                storage =
                    FileStorage(
                        serializer =
                            AeadSerializer(
                                aead =
                                    keySetHandle.getPrimitive(
                                        RegistryConfiguration.get(),
                                        Aead::class.java,
                                    ),
                                wrappedSerializer = StringSerializer,
                                associatedData = file.absolutePath.encodeToByteArray(),
                            ),
                        produceFile = { file },
                    ),
                scope = scope,
            )

        // Write data.
        dataStore1.updateData { "Unencrypted Data" }

        // Cancel scope so the first DataStore instance is no longer active (Mimic new app launch).
        scope.cancel()

        // Ensure file is encrypted
        assertThat(String(file.readBytes())).isNotEqualTo("Unencrypted Data")

        val dataStore2 =
            DataStoreFactory.create(
                FileStorage(
                    serializer =
                        AeadSerializer(
                            aead =
                                keySetHandle.getPrimitive(
                                    RegistryConfiguration.get(),
                                    Aead::class.java,
                                ),
                            wrappedSerializer = StringSerializer,
                            associatedData = file.absolutePath.encodeToByteArray(),
                        ),
                    produceFile = { file },
                )
            )

        // Read data.
        assertThat(dataStore2.data.first()).isEqualTo("Unencrypted Data")
    }

    @Test
    fun swapFiles_sameSerializerAssociatedDataAndKey() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val keySetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val aeadSerializer =
            AeadSerializer(
                aead = keySetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = byteArrayOf(),
            )

        val file1 = temporaryFolder.newFile("dataStore1.pb")
        val file2 = temporaryFolder.newFile("dataStore2.pb")

        // Setup data stores.
        var dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer, produceFile = { file1 }),
                scope = scope,
            )
        var dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer, produceFile = { file2 }),
                scope = scope,
            )

        // Write data.
        dataStore1.updateData { "Unencrypted Data 1" }
        dataStore2.updateData { "Unencrypted Data 2" }

        // Cancel scope so the first DataStore instance is no longer active (Mimic new app launch).
        scope.cancel()

        // Ensure files are encrypted with different values.
        assertThat(String(file1.readBytes())).isNotEqualTo("Unencrypted Data 1")
        assertThat(String(file2.readBytes())).isNotEqualTo("Unencrypted Data 2")
        assertThat(String(file1.readBytes())).isNotEqualTo(file2.readBytes())

        // Setup data stores again, but flip the files.
        dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer, produceFile = { file2 })
            )
        dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer, produceFile = { file1 })
            )

        // Read data.
        // Re-using serializer does not protect against ciphertext swapping/substitution attack.
        assertThat(dataStore1.data.first()).isEqualTo("Unencrypted Data 2")
        assertThat(dataStore2.data.first()).isEqualTo("Unencrypted Data 1")
    }

    @Test
    fun swapFiles_differentSerializer_sameAssociatedDataAndKey() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val keySetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

        val file1 = temporaryFolder.newFile("dataStore1.pb")
        val file2 = temporaryFolder.newFile("dataStore2.pb")

        val aeadSerializer1 =
            AeadSerializer(
                aead = keySetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = byteArrayOf(),
            )
        val aeadSerializer2 =
            AeadSerializer(
                aead = keySetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = byteArrayOf(),
            )

        // Setup data stores.
        var dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file1 }),
                scope = scope,
            )
        var dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file2 }),
                scope = scope,
            )

        // Write data.
        dataStore1.updateData { "Unencrypted Data 1" }
        dataStore2.updateData { "Unencrypted Data 2" }

        // Cancel scope so the first DataStore instance is no longer active (Mimic new app launch).
        scope.cancel()

        // Ensure files are encrypted with different values.
        assertThat(String(file1.readBytes())).isNotEqualTo("Unencrypted Data 1")
        assertThat(String(file2.readBytes())).isNotEqualTo("Unencrypted Data 2")
        assertThat(String(file1.readBytes())).isNotEqualTo(file2.readBytes())

        // Setup data stores again, but flip the files.
        dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file2 })
            )
        dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file1 })
            )

        // Read data.
        // Same associated data and key does not protect against ciphertext swapping/substitution
        // attack.
        assertThat(dataStore1.data.first()).isEqualTo("Unencrypted Data 2")
        assertThat(dataStore2.data.first()).isEqualTo("Unencrypted Data 1")
    }

    @Test
    fun swapFiles_differentSerializerAndAssociatedData_sameKey() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val keySetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

        val file1 = temporaryFolder.newFile("dataStore1.pb")
        val file2 = temporaryFolder.newFile("dataStore2.pb")

        val aeadSerializer1 =
            AeadSerializer(
                aead = keySetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = file1.absolutePath.encodeToByteArray(),
            )
        val aeadSerializer2 =
            AeadSerializer(
                aead = keySetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = file2.absolutePath.encodeToByteArray(),
            )

        // Setup data stores.
        var dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file1 }),
                scope = scope,
            )
        var dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file2 }),
                scope = scope,
            )

        // Write data.
        dataStore1.updateData { "Unencrypted Data 1" }
        dataStore2.updateData { "Unencrypted Data 2" }

        // Cancel scope so the first DataStore instance is no longer active (Mimic new app launch).
        scope.cancel()

        // Ensure files are encrypted with different values.
        assertThat(String(file1.readBytes())).isNotEqualTo("Unencrypted Data 1")
        assertThat(String(file2.readBytes())).isNotEqualTo("Unencrypted Data 2")
        assertThat(String(file1.readBytes())).isNotEqualTo(file2.readBytes())

        // Setup data stores again, but flip the files.
        dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file2 })
            )
        dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file1 })
            )

        // Read data.
        // Different associated data protects against ciphertext swapping/substitution attack even
        // when the same key is used to encrypt both files.
        assertThrows<GeneralSecurityException> { dataStore1.data.first() }
        assertThrows<GeneralSecurityException> { dataStore2.data.first() }
    }

    @Test
    fun swapFiles_differentSerializerAssociatedDataAndKey() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())

        val keySetHandle1 = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val keySetHandle2 = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

        val file1 = temporaryFolder.newFile("dataStore1.pb")
        val file2 = temporaryFolder.newFile("dataStore2.pb")

        val aeadSerializer1 =
            AeadSerializer(
                aead = keySetHandle1.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = file1.absolutePath.encodeToByteArray(),
            )
        val aeadSerializer2 =
            AeadSerializer(
                aead = keySetHandle2.getPrimitive(RegistryConfiguration.get(), Aead::class.java),
                wrappedSerializer = StringSerializer,
                associatedData = file2.absolutePath.encodeToByteArray(),
            )

        // Setup data stores.
        var dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file1 }),
                scope = scope,
            )
        var dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file2 }),
                scope = scope,
            )

        // Write data.
        dataStore1.updateData { "Unencrypted Data 1" }
        dataStore2.updateData { "Unencrypted Data 2" }

        // Cancel scope so the first DataStore instance is no longer active (Mimic new app launch).
        scope.cancel()

        // Ensure files are encrypted with different values.
        assertThat(String(file1.readBytes())).isNotEqualTo("Unencrypted Data 1")
        assertThat(String(file2.readBytes())).isNotEqualTo("Unencrypted Data 2")
        assertThat(String(file1.readBytes())).isNotEqualTo(file2.readBytes())

        // Setup data stores again, but flip the files.
        dataStore1 =
            DataStoreFactory.create(
                storage = FileStorage(serializer = aeadSerializer1, produceFile = { file2 })
            )
        dataStore2 =
            DataStoreFactory.create(
                FileStorage(serializer = aeadSerializer2, produceFile = { file1 })
            )

        // Read data.
        // Different Keys protect against ciphertext swapping/substitution attack.
        assertThrows<GeneralSecurityException> { dataStore1.data.first() }
        assertThrows<GeneralSecurityException> { dataStore2.data.first() }
    }

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
