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

package androidx.savedstate.serialization.utils

import androidx.savedstate.RobolectricTest
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.savedstate.serialization.platformEncodeDecode
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith

/**
 * Base class for testing [SavedState] serialization and deserialization.
 *
 * This class provides a shared setup for testing the encoding and decoding of objects into
 * [SavedState]. It extends [RobolectricTest] to enable proper functionality of `android.os.Bundle`
 * in unit tests.
 */
internal abstract class SavedStateSerializationBaseTest(
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT
) : RobolectricTest() {

    private val configuration =
        SavedStateConfiguration(configuration) {
            val modules = SerializersModule {
                include(polymorphicTestModule)
                include(contextualTestModule)
            }
            serializersModule = modules.overwriteWith(configuration.serializersModule)
        }

    protected fun doTestNullWithNullableStaticType(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BooleanData?>? = null,
        assertion: SavedStateAssertionScope<BooleanData?>.() -> Unit,
    ) {
        doTest(null, configuration, serializer, assertion)
    }

    protected fun doTestNonNullWithNullableStaticType(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BooleanData?>? = null,
        assertion: SavedStateAssertionScope<BooleanData?>.() -> Unit,
    ) {
        doTest(BooleanData(true), configuration, serializer, assertion)
    }

    protected fun doTestNullData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<NullData>? = null,
        assertion: SavedStateAssertionScope<NullData>.() -> Unit,
    ) {
        doTest(NullData(null), configuration, serializer, assertion)
    }

    protected fun doTestInt(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Int>? = null,
        assertion: SavedStateAssertionScope<Int>.() -> Unit,
    ) {
        doTest(7, configuration, serializer, assertion)
    }

    protected fun doTestIntData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<IntData>? = null,
        assertion: SavedStateAssertionScope<IntData>.() -> Unit,
    ) {
        doTest(IntData(value = 7), configuration, serializer, assertion)
    }

    protected fun doTestLong(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Long>? = null,
        assertion: SavedStateAssertionScope<Long>.() -> Unit,
    ) {
        doTest(7L, configuration, serializer, assertion)
    }

    protected fun doTestLongData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<LongData>? = null,
        assertion: SavedStateAssertionScope<LongData>.() -> Unit,
    ) {
        doTest(LongData(value = 7L), configuration, serializer, assertion)
    }

    protected fun doTestShort(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Short>? = null,
        assertion: SavedStateAssertionScope<Short>.() -> Unit,
    ) {
        doTest(7.toShort(), configuration, serializer, assertion)
    }

    protected fun doTestShortData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ShortData>? = null,
        assertion: SavedStateAssertionScope<ShortData>.() -> Unit,
    ) {
        doTest(ShortData(value = 7.toShort()), configuration, serializer, assertion)
    }

    protected fun doTestByte(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Byte>? = null,
        assertion: SavedStateAssertionScope<Byte>.() -> Unit,
    ) {
        doTest(7.toByte(), configuration, serializer, assertion)
    }

    protected fun doTestByteData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ByteData>? = null,
        assertion: SavedStateAssertionScope<ByteData>.() -> Unit,
    ) {
        doTest(ByteData(value = 7.toByte()), configuration, serializer, assertion)
    }

    protected fun doTestBoolean(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Boolean>? = null,
        assertion: SavedStateAssertionScope<Boolean>.() -> Unit,
    ) {
        doTest(true, configuration, serializer, assertion) // Or false
    }

    protected fun doTestBooleanData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BooleanData>? = null,
        assertion: SavedStateAssertionScope<BooleanData>.() -> Unit,
    ) {
        doTest(BooleanData(value = true), configuration, serializer, assertion) // Or false
    }

    protected fun doTestChar(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Char>? = null,
        assertion: SavedStateAssertionScope<Char>.() -> Unit,
    ) {
        doTest('a', configuration, serializer, assertion) // Or any other char
    }

    protected fun doTestCharData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<CharData>? = null,
        assertion: SavedStateAssertionScope<CharData>.() -> Unit,
    ) {
        doTest(CharData(value = 'a'), configuration, serializer, assertion) // Or any other char
    }

    protected fun doTestFloat(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Float>? = null,
        assertion: SavedStateAssertionScope<Float>.() -> Unit,
    ) {
        doTest(7.0f, configuration, serializer, assertion)
    }

    protected fun doTestFloatData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<FloatData>? = null,
        assertion: SavedStateAssertionScope<FloatData>.() -> Unit,
    ) {
        doTest(FloatData(value = 7.0f), configuration, serializer, assertion)
    }

    protected fun doTestDouble(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Double>? = null,
        assertion: SavedStateAssertionScope<Double>.() -> Unit,
    ) {
        doTest(7.0, configuration, serializer, assertion)
    }

    protected fun doTestDoubleData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<DoubleData>? = null,
        assertion: SavedStateAssertionScope<DoubleData>.() -> Unit,
    ) {
        doTest(DoubleData(value = 7.0), configuration, serializer, assertion)
    }

    protected fun doTestIntList(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<List<Int>>? = null,
        assertion: SavedStateAssertionScope<List<Int>>.() -> Unit,
    ) {
        doTest(listOf(1, 2, 3), configuration, serializer, assertion)
    }

    protected fun doTestListIntData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ListIntData>? = null,
        assertion: SavedStateAssertionScope<ListIntData>.() -> Unit,
    ) {
        doTest(ListIntData(listOf(1, 2, 3)), configuration, serializer, assertion)
    }

    protected fun doTestStringList(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<List<String>>? = null,
        assertion: SavedStateAssertionScope<List<String>>.() -> Unit,
    ) {
        doTest(listOf("a", "b", "c"), configuration, serializer, assertion)
    }

    protected fun doTestListStringData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ListStringData>? = null,
        assertion: SavedStateAssertionScope<ListStringData>.() -> Unit,
    ) {
        doTest(ListStringData(listOf("a", "b", "c")), configuration, serializer, assertion)
    }

    protected fun doTestBooleanArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BooleanArray>? = null,
        assertion: SavedStateAssertionScope<BooleanArray>.() -> Unit,
    ) {
        doTest(booleanArrayOf(true, false, true), configuration, serializer, assertion)
    }

    protected fun doTestBooleanArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BooleanArrayData>? = null,
        assertion: SavedStateAssertionScope<BooleanArrayData>.() -> Unit,
    ) {
        doTest(
            BooleanArrayData(booleanArrayOf(true, false, true)),
            configuration,
            serializer,
            assertion,
        )
    }

    protected fun doTestCharArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<CharArray>? = null,
        assertion: SavedStateAssertionScope<CharArray>.() -> Unit,
    ) {
        doTest(charArrayOf('a', 'b', 'c'), configuration, serializer, assertion)
    }

    protected fun doTestCharArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<CharArrayData>? = null,
        assertion: SavedStateAssertionScope<CharArrayData>.() -> Unit,
    ) {
        doTest(CharArrayData(charArrayOf('a', 'b', 'c')), configuration, serializer, assertion)
    }

    protected fun doTestDoubleArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<DoubleArray>? = null,
        assertion: SavedStateAssertionScope<DoubleArray>.() -> Unit,
    ) {
        doTest(doubleArrayOf(1.0, 2.0, 3.0), configuration, serializer, assertion)
    }

    protected fun doTestDoubleArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<DoubleArrayData>? = null,
        assertion: SavedStateAssertionScope<DoubleArrayData>.() -> Unit,
    ) {
        doTest(DoubleArrayData(doubleArrayOf(1.0, 2.0, 3.0)), configuration, serializer, assertion)
    }

    protected fun doTestFloatArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<FloatArray>? = null,
        assertion: SavedStateAssertionScope<FloatArray>.() -> Unit,
    ) {
        doTest(floatArrayOf(1.0f, 2.0f, 3.0f), configuration, serializer, assertion)
    }

    protected fun doTestFloatArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<FloatArrayData>? = null,
        assertion: SavedStateAssertionScope<FloatArrayData>.() -> Unit,
    ) {
        doTest(FloatArrayData(floatArrayOf(1.0f, 2.0f, 3.0f)), configuration, serializer, assertion)
    }

    protected fun doTestIntArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<IntArray>? = null,
        assertion: SavedStateAssertionScope<IntArray>.() -> Unit,
    ) {
        doTest(intArrayOf(1, 2, 3), configuration, serializer, assertion)
    }

    protected fun doTestIntArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<IntArrayData>? = null,
        assertion: SavedStateAssertionScope<IntArrayData>.() -> Unit,
    ) {
        doTest(IntArrayData(intArrayOf(1, 2, 3)), configuration, serializer, assertion)
    }

    protected fun doTestLongArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<LongArray>? = null,
        assertion: SavedStateAssertionScope<LongArray>.() -> Unit,
    ) {
        doTest(longArrayOf(1L, 2L, 3L), configuration, serializer, assertion)
    }

    protected fun doTestLongArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<LongArrayData>? = null,
        assertion: SavedStateAssertionScope<LongArrayData>.() -> Unit,
    ) {
        doTest(LongArrayData(longArrayOf(1L, 2L, 3L)), configuration, serializer, assertion)
    }

    protected fun doTestStringArray(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Array<String>>? = null,
        assertion: SavedStateAssertionScope<Array<String>>.() -> Unit,
    ) {
        doTest(arrayOf("a", "b", "c"), configuration, serializer, assertion)
    }

    protected fun doTestStringArrayData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<StringArrayData>? = null,
        assertion: SavedStateAssertionScope<StringArrayData>.() -> Unit,
    ) {
        doTest(StringArrayData(arrayOf("a", "b", "c")), configuration, serializer, assertion)
    }

    protected fun doTestBoxData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<BoxData<String>>? = null,
        assertion: SavedStateAssertionScope<BoxData<String>>.() -> Unit,
    ) {
        doTest(BoxData("abc"), configuration, serializer, assertion)
    }

    protected fun doTestObject(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ObjectData>? = null,
        assertion: SavedStateAssertionScope<ObjectData>.() -> Unit,
    ) {
        doTest(ObjectData, configuration, serializer, assertion)
    }

    protected fun doTestClassDiscriminatorConflict(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ClassDiscriminatorConflict>? = null,
    ) {
        doEncodeToSavedState(ClassDiscriminatorConflict(123), serializer, configuration)
    }

    protected fun doTestSerialName(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<SerialNameType>? = null,
        assertion: SavedStateAssertionScope<SerialNameType>.() -> Unit,
    ) {
        doTest(SerialNameType(123), configuration, serializer, assertion)
    }

    protected fun doTestSerialNameData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<SerialNameData>? = null,
        assertion: SavedStateAssertionScope<SerialNameData>.() -> Unit,
    ) {
        doTest(SerialNameData(SerialNameType(456)), configuration, serializer, assertion)
    }

    protected fun doTestSealed(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Sealed>? = null,
        assertion: SavedStateAssertionScope<Sealed>.() -> Unit,
    ) {
        doTest(SealedImpl1(1), configuration, serializer, assertion)
    }

    protected fun doTestSealedData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<SealedData>? = null,
        assertion: SavedStateAssertionScope<SealedData>.() -> Unit,
    ) {
        doTest(SealedData(SealedImpl1(7), SealedImpl2("a")), configuration, serializer, assertion)
    }

    protected fun doTestEnum(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<Enum>? = null,
        assertion: SavedStateAssertionScope<Enum>.() -> Unit,
    ) {
        doTest(Enum.OptionB, configuration, serializer, assertion)
    }

    protected fun doTestEnumData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<EnumData>? = null,
        assertion: SavedStateAssertionScope<EnumData>.() -> Unit,
    ) {
        doTest(EnumData(Enum.OptionA, Enum.OptionB), configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicInterface(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicInterface>? =
            PolymorphicSerializer(PolymorphicInterface::class), // Required in Kotlin/Native.
        assertion: SavedStateAssertionScope<PolymorphicInterface>.() -> Unit,
    ) {
        doTest(PolymorphicInterfaceImpl1(7), configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicInterfaceData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicInterfaceData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicInterfaceData>.() -> Unit,
    ) {
        val data =
            PolymorphicInterfaceData(PolymorphicInterfaceImpl1(7), PolymorphicInterfaceImpl2("a"))
        doTest(data, configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicClass(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicClass>? =
            PolymorphicSerializer(PolymorphicClass::class), // Required in Kotlin/Native.
        assertion: SavedStateAssertionScope<PolymorphicClass>.() -> Unit,
    ) {
        doTest(PolymorphicClassImpl1(7), configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicClassData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicClassData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicClassData>.() -> Unit,
    ) {
        val data = PolymorphicClassData(PolymorphicClassImpl1(7), PolymorphicClassImpl2("a"))
        doTest(data, configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicMixedData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicMixedData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicMixedData>.() -> Unit,
    ) {
        val data =
            PolymorphicMixedData(
                base1 = PolymorphicClassImpl1(2),
                base2 = PolymorphicInterfaceImpl1(3),
            )
        doTest(data, configuration, serializer, assertion)
    }

    protected fun doTestPolymorphicNullMixedData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<PolymorphicNullMixedData>? = null,
        assertion: SavedStateAssertionScope<PolymorphicNullMixedData>.() -> Unit,
    ) {
        val data = PolymorphicNullMixedData(base1 = null, base2 = null)
        doTest(data, configuration, serializer, assertion)
    }

    protected fun doTestContextual(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ContextualType>? = null,
        assertion: SavedStateAssertionScope<ContextualType>.() -> Unit,
    ) {
        doTest(ContextualType("a", "b"), configuration, serializer, assertion)
    }

    protected fun doTestContextualData(
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<ContextualData>? = null,
        assertion: SavedStateAssertionScope<ContextualData>.() -> Unit,
    ) {
        doTest(ContextualData(ContextualType("a", "b")), configuration, serializer, assertion)
    }

    /**
     * Executes the full encode-decode test for an instance of `T`.
     *
     * @param original The original instance to be tested.
     * @param serializer Optional `KSerializer<T>` for custom serialization.
     * @param configuration Optional `SavedStateConfig` for encoding configuration.
     * @param assertion A block to perform additional assertions on the test results.
     */
    protected inline fun <reified T> doTest(
        original: T,
        configuration: SavedStateConfiguration? = this.configuration,
        serializer: KSerializer<T>? = null,
        noinline assertion: SavedStateAssertionScope<T>.() -> Unit,
    ) {
        val platformAgnosticSerialized = doEncodeToSavedState(original, serializer, configuration)
        val platformSpecificSerialized = platformEncodeDecode(platformAgnosticSerialized)
        val deserialized =
            doDecodeFromSavedState<T>(platformSpecificSerialized, serializer, configuration)
        val scope =
            SavedStateAssertionScope(
                original = original,
                deserialized = deserialized,
                serialized = platformSpecificSerialized,
                platformRepresentation = platformAgnosticSerialized.read { contentDeepToString() },
                representation = platformSpecificSerialized.read { contentDeepToString() },
            )
        assertion(scope)
    }

    /**
     * Encodes an instance of `T` into a `SavedState` using optional serialization and
     * configuration.
     */
    protected inline fun <reified T> doEncodeToSavedState(
        original: T,
        serializer: KSerializer<T>? = null,
        configuration: SavedStateConfiguration? = this.configuration,
    ): SavedState {
        return when {
            serializer != null && configuration != null ->
                encodeToSavedState(serializer, original, configuration)
            serializer != null -> encodeToSavedState(serializer, original)
            configuration != null -> encodeToSavedState(original, configuration)
            else -> encodeToSavedState(original)
        }
    }

    /** Decodes a `SavedState` back into an instance of `T`. */
    protected inline fun <reified T> doDecodeFromSavedState(
        serialized: SavedState,
        strategy: DeserializationStrategy<T>? = null,
        configuration: SavedStateConfiguration? = this.configuration,
    ): T {
        return when {
            strategy != null && configuration != null ->
                decodeFromSavedState(strategy, serialized, configuration)
            strategy != null -> decodeFromSavedState(strategy, serialized)
            configuration != null -> decodeFromSavedState(serialized, configuration)
            else -> decodeFromSavedState(serialized)
        }
    }

    /**
     * A helper class that encapsulates the state of an encoding-decoding test for `SavedState`.
     *
     * This class provides a structured way to assert the correctness of the serialization process
     * by storing the original object, its encoded representation, and the final decoded instance.
     * It helps verify that the state remains consistent throughout the encoding-decoding cycle.
     *
     * @param T The type of the object being tested.
     * @param original The original [T] instance before encoding.
     * @param deserialized The [T] instance after decoding, expected to match the original.
     * @param serialized The [SavedState] representation of the object after encoding.
     * @param platformRepresentation A [String] representation tied to platform-specific
     *   serialization behavior (e.g., **after** parceling on Android).
     * @param representation A [String] representation of the encoded state, independent of
     *   platform-specific serialization formats (e.g., **before** parceling on Android).
     */
    protected class SavedStateAssertionScope<T>(
        val original: T,
        val deserialized: T,
        val serialized: SavedState,
        val platformRepresentation: String,
        val representation: String,
    )
}
