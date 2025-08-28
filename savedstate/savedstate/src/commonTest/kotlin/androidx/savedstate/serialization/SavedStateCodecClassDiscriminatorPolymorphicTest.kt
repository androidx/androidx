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

package androidx.savedstate.serialization

import androidx.kruth.assertThat
import androidx.savedstate.IgnoreWebTarget
import androidx.savedstate.serialization.utils.SavedStateSerializationBaseTest
import kotlin.test.Test

@IgnoreWebTarget
internal class SavedStateCodecClassDiscriminatorPolymorphicTest :
    SavedStateSerializationBaseTest(
        configuration =
            SavedStateConfiguration { classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC }
    ) {

    @Test
    fun testNullWithNullableStaticType() {
        doTestNullWithNullableStaticType {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=null]")
        }
    }

    @Test
    fun testNonNullWithNullableStaticType() {
        doTestNonNullWithNullableStaticType {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=true]")
        }
    }

    @Test
    fun testNullData() {
        doTestNullData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=null]")
        }
    }

    @Test
    fun testInt() {
        doTestInt {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7]")
        }
    }

    @Test
    fun testIntData() {
        doTestIntData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Test
    fun testLong() {
        doTestLong {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7]")
        }
    }

    @Test
    fun testLongData() {
        doTestLongData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Test
    fun testShort() {
        doTestShort {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7]")
        }
    }

    @Test
    fun testShortData() {
        doTestShortData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Test
    fun testByte() {
        doTestByte {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7]")
        }
    }

    @Test
    fun testByteData() {
        doTestByteData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Test
    fun testBoolean() {
        doTestBoolean {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=true]")
        }
    }

    @Test
    fun testBooleanData() {
        doTestBooleanData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=true]")
        }
    }

    @Test
    fun testChar() {
        doTestChar {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=a]")
        }
    }

    @Test
    fun testCharData() {
        doTestCharData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=a]")
        }
    }

    @Test
    fun testFloat() {
        doTestFloat {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7.0]")
        }
    }

    @Test
    fun testFloatData() {
        doTestFloatData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7.0]")
        }
    }

    @Test
    fun testDouble() {
        doTestDouble {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=7.0]")
        }
    }

    @Test
    fun testDoubleData() {
        doTestDoubleData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7.0]")
        }
    }

    @Test
    fun testIntList() {
        doTestIntList {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[1, 2, 3]]")
        }
    }

    @Test
    fun testListIntData() {
        doTestListIntData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[1, 2, 3]]")
        }
    }

    @Test
    fun testStringList() {
        doTestStringList {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[a, b, c]]")
        }
    }

    @Test
    fun testListStringData() {
        doTestListStringData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[a, b, c]]")
        }
    }

    @Test
    fun testBooleanArray() {
        doTestBooleanArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[true, false, true]]")
        }
    }

    @Test
    fun testBooleanArrayData() {
        doTestBooleanArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[true, false, true]]")
        }
    }

    @Test
    fun testCharArray() {
        doTestCharArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[a, b, c]]")
        }
    }

    @Test
    fun testCharArrayData() {
        doTestCharArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[a, b, c]]")
        }
    }

    @Test
    fun testDoubleArray() {
        doTestDoubleArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[1.0, 2.0, 3.0]]")
        }
    }

    @Test
    fun testDoubleArrayData() {
        doTestDoubleArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[1.0, 2.0, 3.0]]")
        }
    }

    @Test
    fun testFloatArray() {
        doTestFloatArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[1.0, 2.0, 3.0]]")
        }
    }

    @Test
    fun testFloatArrayData() {
        doTestFloatArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[1.0, 2.0, 3.0]]")
        }
    }

    @Test
    fun testIntArray() {
        doTestIntArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[1, 2, 3]]")
        }
    }

    @Test
    fun testIntArrayData() {
        doTestIntArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[1, 2, 3]]")
        }
    }

    @Test
    fun testLongArray() {
        doTestLongArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[1, 2, 3]]")
        }
    }

    @Test
    fun testLongArrayData() {
        doTestLongArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[1, 2, 3]]")
        }
    }

    @Test
    fun testStringArray() {
        doTestStringArray {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=[a, b, c]]")
        }
    }

    @Test
    fun testStringArrayData() {
        doTestStringArrayData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[a, b, c]]")
        }
    }

    @Test
    fun testBoxData() {
        doTestBoxData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=abc]")
        }
    }

    @Test
    fun testObject() {
        doTestObject {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[]")
        }
    }

    @Test
    fun testSealed() {
        doTestSealed {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[type=androidx.savedstate.serialization.utils.SealedImpl1, value=[value=1]]"
                )
        }
    }

    @Test
    fun testSealedData() {
        doTestSealedData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[base1=[type=androidx.savedstate.serialization.utils.SealedImpl1, value=[value=7]], base2=[type=androidx.savedstate.serialization.utils.SealedImpl2, value=[value=a]]]"
                )
        }
    }

    @Test
    fun testEnum() {
        doTestEnum {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=1]")
        }
    }

    @Test
    fun testEnumData() {
        doTestEnumData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[base1=0, base2=1]")
        }
    }

    @Test
    fun testPolymorphicInterface() {
        doTestPolymorphicInterface {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[type=androidx.savedstate.serialization.utils.PolymorphicInterfaceImpl1, value=[value=7]]"
                )
        }
    }

    @Test
    fun testPolymorphicInterfaceData() {
        doTestPolymorphicInterfaceData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[base1=[type=androidx.savedstate.serialization.utils.PolymorphicInterfaceImpl1, value=[value=7]], base2=[type=androidx.savedstate.serialization.utils.PolymorphicInterfaceImpl2, value=[value=a]]]"
                )
        }
    }

    @Test
    fun testPolymorphicClass() {
        doTestPolymorphicClass {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[type=androidx.savedstate.serialization.utils.PolymorphicClassImpl1, value=[value=7]]"
                )
        }
    }

    @Test
    fun testPolymorphicClassData() {
        doTestPolymorphicClassData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[base1=[type=androidx.savedstate.serialization.utils.PolymorphicClassImpl1, value=[value=7]], base2=[type=androidx.savedstate.serialization.utils.PolymorphicClassImpl2, value=[value=a]]]"
                )
        }
    }

    @Test
    fun testPolymorphicMixedData() {
        doTestPolymorphicMixedData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation)
                .isEqualTo(
                    "[base1=[type=androidx.savedstate.serialization.utils.PolymorphicClassImpl1, value=[value=2]], base2=[type=androidx.savedstate.serialization.utils.PolymorphicInterfaceImpl1, value=[value=3]]]"
                )
        }
    }

    @Test
    fun testPolymorphicNullMixedData() {
        doTestPolymorphicNullMixedData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[base1=null, base2=null]")
        }
    }

    @Test
    fun testContextual() {
        doTestContextual {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=a#b]")
        }
    }

    @Test
    fun testContextualData() {
        doTestContextualData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=[value=a#b]]")
        }
    }

    @Test
    fun testClassDiscriminatorConflict() {
        doTestClassDiscriminatorConflict()
    }

    @Test
    fun testSerialName() {
        doTestSerialName {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=123]")
        }
    }

    @Test
    fun testSerialNameData() {
        doTestSerialNameData {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[SerialName2=[value=456]]")
        }
    }
}
