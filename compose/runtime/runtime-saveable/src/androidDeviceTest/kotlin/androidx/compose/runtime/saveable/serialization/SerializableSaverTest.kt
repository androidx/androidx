/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.saveable.serialization

import androidx.compose.runtime.saveable.SaverScope
import androidx.savedstate.read
import androidx.savedstate.serialization.ClassDiscriminatorMode.ALL_OBJECTS
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class SerializableSaverTest {

    @Test
    fun savedStateSaver_reifiedSerializer_defaultConfig_restores() {
        val original = TestData(7)
        val scope = TestSaverScope { true }
        val saver = serializableSaver<TestData>()

        val saved = with(saver) { scope.save(original)!! }
        val restored = saver.restore(saved)
        val hasClassDiscriminator = saved.read { contains("type") }

        assertThat(restored).isEqualTo(original)
        assertThat(hasClassDiscriminator).isFalse() // default config has no class discriminator
    }

    @Test
    fun savedStateSaver_customSerializer_defaultConfig_restores() {
        val original = TestData(7)
        val scope = TestSaverScope { true }
        val saver = serializableSaver(serializer = TestSerializer)

        val saved = with(saver) { scope.save(original)!! }
        val restored = saver.restore(saved)
        val hasClassDiscriminator = saved.read { contains("type") }

        assertThat(restored).isEqualTo(original)
        assertThat(hasClassDiscriminator).isFalse() // default config has no class discriminator
    }

    @Test
    fun savedStateSaver_reifiedSerializer_customConfig_restores() {
        val config = SavedStateConfiguration { classDiscriminatorMode = ALL_OBJECTS }
        val original = TestData(7)
        val scope = TestSaverScope { true }
        val saver = serializableSaver<TestData>(config)

        val saved = with(saver) { scope.save(original)!! }
        val restored = saver.restore(saved)
        val hasClassDiscriminator = saved.read { contains("type") }

        assertThat(restored).isEqualTo(original)
        assertThat(hasClassDiscriminator).isTrue()
    }

    @Test
    fun savedStateSaver_customSerializer_customConfig_restores() {
        val config = SavedStateConfiguration { classDiscriminatorMode = ALL_OBJECTS }
        val original = TestData(7)
        val scope = TestSaverScope { true }
        val saver = serializableSaver<TestData>(TestSerializer, config)

        val saved = with(saver) { scope.save(original)!! }
        val restored = saver.restore(saved)
        val hasClassDiscriminator = saved.read { contains("type") }

        assertThat(restored).isEqualTo(original)
        assertThat(hasClassDiscriminator).isTrue()
    }
}

private class TestSaverScope(val block: (value: Any?) -> Boolean) : SaverScope {
    override fun canBeSaved(value: Any): Boolean = block(value)
}

@Serializable(with = TestSerializer::class) private data class TestData(val value: Int)

private object TestSerializer : KSerializer<TestData> {

    override val descriptor by lazy {
        buildClassSerialDescriptor(TestData::class.qualifiedName!!) {
            element("value", PrimitiveSerialDescriptor("value", PrimitiveKind.INT))
        }
    }

    override fun serialize(encoder: Encoder, value: TestData) {
        encoder.encodeStructure(descriptor) { encodeIntElement(descriptor, 0, value.value) }
    }

    override fun deserialize(decoder: Decoder): TestData {
        return decoder.decodeStructure(descriptor) {
            var value: Int? = null
            while (true) {
                val index = decodeElementIndex(descriptor)
                when (index) {
                    0 -> value = decodeIntElement(descriptor, 0)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }
            TestData(value!!)
        }
    }
}
