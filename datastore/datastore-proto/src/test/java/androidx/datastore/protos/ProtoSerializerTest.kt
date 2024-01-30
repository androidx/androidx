/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.protos

import androidx.datastore.core.CorruptionException
import androidx.datastore.testing.TestMessageProto.ExtendableProto
import androidx.datastore.testing.TestMessageProto.ExtensionProto
import androidx.datastore.testing.TestMessageProto.FooProto
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ExtensionRegistryLite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
class ProtoSerializerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun testReadWriteProto() = runTest {
        val file = temporaryFolder.newFile("test_file.pb")

        val fooProtoWithText = FooProto.newBuilder().setText("abc").build()

        val protoSerializer = ProtoSerializer<FooProto>(
            FooProto.getDefaultInstance(),
            ExtensionRegistryLite.getEmptyRegistry()
        )

        file.outputStream().use {
            protoSerializer.writeTo(fooProtoWithText, it)
        }

        val readProto = file.inputStream().use { protoSerializer.readFrom(it) }

        assertThat(readProto).isEqualTo(fooProtoWithText)
    }

    @Test
    fun testReadWriteProtoWithExtension() = runTest {
        val file = temporaryFolder.newFile("test_file.pb")

        val registry = ExtensionRegistryLite.newInstance()
        registry.add(ExtensionProto.extension)

        val protoSerializer = ProtoSerializer<ExtendableProto>(
            ExtendableProto.getDefaultInstance(),
            registry
        )

        val extendedProto = ExtendableProto.newBuilder().setExtension(
            ExtensionProto.extension,
            ExtensionProto.newBuilder().setFoo(FooProto.newBuilder().setText("abc").build()).build()
        ).build()

        file.outputStream().use {
            protoSerializer.writeTo(extendedProto, it)
        }

        val readProto = file.inputStream().use { protoSerializer.readFrom(it) }
        assertThat(readProto).isEqualTo(extendedProto)
    }

    @Test
    fun testThrowsCorruptionException() = runTest {
        val file = temporaryFolder.newFile("test_file.pb")
        file.writeBytes(byteArrayOf(0x00, 0x02)) // Protos cannot start with 0x00.

        val protoSerializer = ProtoSerializer<FooProto>(
            FooProto.getDefaultInstance(),
            ExtensionRegistryLite.getEmptyRegistry()
        )

        assertThrows<CorruptionException> {
            file.inputStream().use { protoSerializer.readFrom(it) }
        }
    }
}
