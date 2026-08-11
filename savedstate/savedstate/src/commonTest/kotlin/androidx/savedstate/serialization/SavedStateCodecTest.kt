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
import androidx.kruth.assertThrows
import androidx.savedstate.IgnoreWebTarget
import androidx.savedstate.RobolectricTest
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateCodecTestUtils.encodeDecode
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import androidx.savedstate.write
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

@IgnoreWebTarget
@ExperimentalSerializationApi
internal class SavedStateCodecTest : RobolectricTest() {
    @Test
    fun primitiveByte() {
        Byte.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Byte.MIN_VALUE.toInt())
        }
        Byte.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Byte.MAX_VALUE.toInt())
        }
    }

    @Test
    fun primitiveShort() {
        Short.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Short.MIN_VALUE.toInt())
        }
        Short.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Short.MAX_VALUE.toInt())
        }
    }

    @Test
    fun primitiveInt() {
        Int.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
        Int.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MAX_VALUE)
        }
    }

    @Test
    fun primitiveLong() {
        Long.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(Long.MIN_VALUE)
        }
        Long.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(Long.MAX_VALUE)
        }
    }

    @Test
    fun primitiveFloat() {
        Float.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloat("")).isEqualTo(Float.MIN_VALUE)
        }
        Float.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloat("")).isEqualTo(Float.MAX_VALUE)
        }
    }

    @Test
    fun primitiveDouble() {
        Double.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDouble("")).isEqualTo(Double.MIN_VALUE)
        }
        Double.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDouble("")).isEqualTo(Double.MAX_VALUE)
        }
    }

    @Test
    fun primitiveChar() {
        Char.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getChar("")).isEqualTo(Char.MIN_VALUE)
        }
        Char.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getChar("")).isEqualTo(Char.MAX_VALUE)
        }
    }

    @Test
    fun primitiveBoolean() {
        false.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBoolean("")).isEqualTo(false)
        }
        true.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBoolean("")).isEqualTo(true)
        }
    }

    @Test
    fun primitiveString() {
        ""
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getString("")).isEqualTo("")
            }
        "foo"
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getString("")).isEqualTo("foo")
            }
    }

    @Test
    fun primitiveEnum() {
        MyEnum.A.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        MyEnum.B.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(1)
        }
        MyEnum.C.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(2)
        }
    }

    @Test
    fun valueClassUByte() {
        UByte.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UByte.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
    }

    @Test
    fun valueClassUShort() {
        UShort.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UShort.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
    }

    @Test
    fun valueClassUInt() {
        UInt.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UInt.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
    }

    @Test
    fun valueClassULong() {
        ULong.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(0L)
        }
        ULong.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(-1L)
        }
    }

    @Test
    fun valueClassString() {
        MyValueClassToString("foo").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo("foo")
        }
    }

    @Test
    fun compositePair() {
        Pair(3, "foo").encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("first")).isEqualTo(3)
            assertThat(getString("second")).isEqualTo("foo")
        }
    }

    @Test
    fun compositeTriple() {
        Triple(3, "foo", 3.14).encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("first")).isEqualTo(3)
            assertThat(getString("second")).isEqualTo("foo")
            assertThat(getDouble("third")).isEqualTo(3.14)
        }
    }

    @Test
    fun compositeDuration() {
        Duration.ZERO.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo(Duration.ZERO.toIsoString())
        }
        Duration.INFINITE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo(Duration.INFINITE.toIsoString())
        }
    }

    @Test
    fun compositeUnit() {
        Unit.encodeDecode()
    }

    @Test
    fun compositeObject() {
        MyObject.encodeDecode()
    }

    @Test
    fun arrayInt() {
        intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntArray("")).isEqualTo(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE))
        }
    }

    @Test
    fun arrayLong() {
        longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLongArray("")).isEqualTo(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE))
        }
    }

    @Test
    fun arrayBoolean() {
        booleanArrayOf(false, true).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBooleanArray("")).isEqualTo(booleanArrayOf(false, true))
        }
    }

    @Test
    fun arrayChar() {
        charArrayOf(Char.MIN_VALUE, Char.MAX_VALUE).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharArray("")).isEqualTo(charArrayOf(Char.MIN_VALUE, Char.MAX_VALUE))
        }
    }

    @Test
    fun arrayFloat() {
        floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloatArray("")).isEqualTo(floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE))
        }
    }

    @Test
    fun arrayDouble() {
        doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDoubleArray(""))
                .isEqualTo(doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE))
        }
    }

    @Test
    fun arrayString() {
        arrayOf("a", "b").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getStringArray("")).isEqualTo(arrayOf("a", "b"))
        }
        // We still decompose nullable String arrays.
        arrayOf("a", null).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("0")).isEqualTo("a")
            assertThat(isNull("1")).isTrue()
        }
    }

    @Test
    fun arrayCustomSerializer() {
        MyColor(0, 128, 255).encodeDecode(serializer = MyColorIntArraySerializer) {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntArray("")).isEqualTo(intArrayOf(0, 128, 255))
        }
    }

    @Test
    fun listInt() {
        emptyList<Int>().encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntList("")).isEqualTo(emptyList<Int>())
        }
        listOf(1, 2, 3).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntList("")).isEqualTo(listOf(1, 2, 3))
        }
    }

    @Test
    fun listString() {
        listOf("a", "b", "c").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getStringList("")).isEqualTo(listOf("a", "b", "c"))
        }
    }

    @Test
    fun listBoolean() {
        listOf(true, false, true).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBooleanArray("")).isEqualTo(booleanArrayOf(true, false, true))
        }
    }

    @Test
    fun listLong() {
        listOf(1L, 2L, 3L).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLongArray("")).isEqualTo(longArrayOf(1L, 2L, 3L))
        }
    }

    @Test
    fun listFloat() {
        listOf(1.0f, 2.0f, 3.0f).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloatArray("")).isEqualTo(floatArrayOf(1.0f, 2.0f, 3.0f))
        }
    }

    @Test
    fun listDouble() {
        listOf(1.0, 2.0, 3.0).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDoubleArray("")).isEqualTo(doubleArrayOf(1.0, 2.0, 3.0))
        }
    }

    @Test
    fun listChar() {
        listOf('a', 'b', 'c').encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharArray("")).isEqualTo(charArrayOf('a', 'b', 'c'))
        }
    }

    @Test
    fun listNullable() {
        listOf("a", null, "c").encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getString("0")).isEqualTo("a")
            assertThat(isNull("1")).isTrue()
            assertThrows(IllegalArgumentException::class) { getString("1") }
                .hasMessageThat()
                .contains(keyOrValueNotFoundErrorMessage("1"))
            assertThat(getString("2")).isEqualTo("c")
        }
        listOf(1, 2, null, 4, 5, null).encodeDecode {
            assertThat(size()).isEqualTo(6)
            assertThat(getInt("0")).isEqualTo(1)
            assertThat(getInt("1")).isEqualTo(2)
            assertThat(isNull("2")).isTrue()
            assertThat(getInt("3")).isEqualTo(4)
            assertThat(getInt("4")).isEqualTo(5)
            assertThat(isNull("5")).isTrue()
        }
    }

    @Test
    fun listNested() {
        listOf(listOf(1, 2), listOf(3, 4)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getIntList("0")).isEqualTo(listOf(1, 2))
            assertThat(getIntList("1")).isEqualTo(listOf(3, 4))
        }
        listOf(listOf(emptyList(), listOf(1, 2)), listOf(listOf(3, 4))).encodeDecode {
            assertThat(size()).isEqualTo(2)
            getSavedState("0").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getIntList("0")).isEqualTo(emptyList<Int>())
                assertThat(getIntList("1")).isEqualTo(listOf(1, 2))
            }
            getSavedState("1").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getIntList("0")).isEqualTo(listOf(3, 4))
            }
        }
        @Serializable data class MyComponent(val list: List<String>)
        @Serializable data class MyContainer(val myComponent: MyComponent)
        MyContainer(MyComponent(listOf("foo", "bar"))).encodeDecode {
            assertThat(size()).isEqualTo(1)
            getSavedState("myComponent").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getStringList("list")).isEqualTo(listOf("foo", "bar"))
            }
        }
    }

    @Test
    fun listCustomSerializer() {
        val myDelegatedList = MyDelegatedList(arrayListOf(1, 3, 5))
        myDelegatedList.encodeDecode(serializer = serializer<MyDelegatedList<Int>>()) {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntList("values")).isEqualTo(listOf(1, 3, 5))
        }
        myDelegatedList.encodeDecode(serializer = serializer<List<Int>>()) {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntList("")).isEqualTo(listOf(1, 3, 5))
        }
    }

    @Test
    fun setOrdered() {
        val list = (0..99).toList()
        setOf(*list.toTypedArray()).encodeDecode {
            assertThat(size()).isEqualTo(100)
            list.forEach { index -> assertThat(getInt(index.toString())).isEqualTo(index) }
        }
    }

    @Test
    fun setUnordered() {
        val list = (0..99).toList()
        hashSetOf(*list.toTypedArray()).encodeDecode {
            assertThat(size()).isEqualTo(100)
            val values = buildList { list.forEach { index -> add(getInt(index.toString())) } }
            assertThat(values.sorted()).isEqualTo(list)
        }
    }

    @Test
    fun setDuplicateDecoding() {
        assertThat(
                decodeFromSavedState<Set<Int>>(
                    savedState {
                        putInt("0", 1)
                        putInt("1", 3)
                        putInt("2", 3)
                    }
                )
            )
            .isEqualTo(setOf(1, 3))
    }

    @Test
    fun mapEmpty() {
        emptyMap<Int, String>().encodeDecode()
    }

    @Test
    fun mapNonNull() {
        mapOf<Int, String>(123 to "foo", 456 to "bar").encodeDecode {
            assertThat(size()).isEqualTo(4)
            assertThat(getInt("0")).isEqualTo(123)
            assertThat(getString("1")).isEqualTo("foo")
            assertThat(getInt("2")).isEqualTo(456)
            assertThat(getString("3")).isEqualTo("bar")
        }
    }

    @Test
    fun mapNullable() {
        mapOf<Int?, String?>(123 to null, null to "bar").encodeDecode {
            assertThat(size()).isEqualTo(4)
            assertThat(getInt("0")).isEqualTo(123)
            assertThat(isNull("1")).isTrue()
            assertThat(isNull("2")).isTrue()
            assertThat(getString("3")).isEqualTo("bar")
        }
    }

    @Test
    fun recursiveType() {
        MyTreeNode(3, MyTreeNode(5), MyTreeNode(7)).encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("value")).isEqualTo(3)
            getSavedState("left").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(5)
            }
            getSavedState("right").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(7)
            }
        }
    }

    @Test
    fun typeAlias() {
        MyTypeAliasToInt.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
        MyNestedTypeAlias.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
    }

    @Test
    fun sealedClass() {
        // Should use base type for encoding/decoding.
        Node.Add(Node.Operand(3), Node.Operand(5)).encodeDecode<Node> {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("type")).isEqualTo("androidx.savedstate.serialization.Node.Add")
            getSavedState("value").read {
                getSavedState("lhs").read {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getInt("value")).isEqualTo(3)
                }
                getSavedState("rhs").read {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getInt("value")).isEqualTo(5)
                }
            }
        }
    }

    @Test
    fun defaultBasic() {
        @Serializable data class A(val i: Int = 3)
        // We don't encode default values by default.
        A().encodeDecode()
        A(i = 5).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("i")).isEqualTo(5)
        }
    }

    @Test
    fun defaultNullable() {
        // Nullable with default value.
        @Serializable data class B(val s: String? = "foo", val i: Int)
        B(i = 3).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("i")).isEqualTo(3)
        }
        B(s = null, i = 3).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(isNull("s")).isTrue()
        }
        B(s = "bar", i = 3).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("s")).isEqualTo("bar")
        }
        // The value of `s` is the same as its default value so it's omitted from encoding.
        B(s = "foo", i = 3).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("i")).isEqualTo(3)
        }
    }

    @Test
    fun defaultNullableWithoutDefault() {
        // Nullable without default value
        @Serializable data class C(val s: String?)
        C(s = "bar").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("s")).isEqualTo("bar")
        }
    }

    @Test
    fun defaultEncodeDefault() {
        // Default value is encoded with `@EncodeDefault`.
        @Serializable
        data class D(
            val i: Int = 3,
            @EncodeDefault(EncodeDefault.Mode.ALWAYS) val s: String? = "foo",
        )
        D(i = 5).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("i")).isEqualTo(5)
            assertThat(getString("s")).isEqualTo("foo")
        }
    }

    @Test
    fun defaultNullAsDefault() {
        // Nullable with null as default value.
        @Serializable data class E(val s: String? = null)
        // Even though we encode `null`s in general as we don't encode default values
        // nothing is encoded.
        E().encodeDecode()
    }

    @Test
    fun defaultNullableInParent() {
        // Nullable in parent
        G(i = 3).encodeDecode<F> {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("type")).isEqualTo("androidx.savedstate.serialization.G")
            getSavedState("value").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("i")).isEqualTo(3)
            }
        }
    }

    @Test
    fun savedStateObject() {
        @Serializable
        data class MyClass(@Serializable(with = SavedStateSerializer::class) val s: SavedState)
        MyClass(
                savedState {
                    putInt("i", 1)
                    putString("s", "foo")
                    putIntArray("a", intArrayOf(1, 3, 5))
                    putSavedState("ss", savedState { putString("s", "bar") })
                }
            )
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.s.read { contentDeepEquals(original.s) })
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    getSavedState("s").read {
                        assertThat(size()).isEqualTo(4)
                        assertThat(getInt("i")).isEqualTo(1)
                        assertThat(getString("s")).isEqualTo("foo")
                        assertThat(getIntArray("a")).isEqualTo(intArrayOf(1, 3, 5))
                        getSavedState("ss").read {
                            assertThat(size()).isEqualTo(1)
                            assertThat(getString("s")).isEqualTo("bar")
                        }
                    }
                },
            )

        val origin = savedState {
            putInt("i", 1)
            putString("s", "foo")
            putIntArray("a", intArrayOf(1, 3, 5))
        }
        val encoded = encodeToSavedState(SavedStateSerializer, origin)
        val restored = decodeFromSavedState(SavedStateSerializer, encoded)

        encoded.read {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("i")).isEqualTo(1)
            assertThat(getString("s")).isEqualTo("foo")
            assertThat(getIntArray("a")).isEqualTo(intArrayOf(1, 3, 5))
        }
        assertThat(restored.read { contentDeepEquals(origin) }).isTrue()
        assertThat(restored).isNotSameInstanceAs(origin)
    }

    @Test
    fun serialNameAnnotation() {
        @Serializable data class MyModel(@SerialName("foo") val i: Int)
        MyModel(3).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("foo")).isEqualTo(3)
        }
    }

    // Users shouldn't do this. The test is just to document the behavior.
    @Test
    fun typeMismatch() {
        assertThrows(IllegalArgumentException::class) {
                decodeFromSavedState<Int>(savedState { putBoolean("", true) })
            }
            .hasMessageThat()
            .contains(keyOrValueNotFoundErrorMessage(""))
        assertThrows(IllegalArgumentException::class) {
                decodeFromSavedState<String>(savedState { putBoolean("", true) })
            }
            .hasMessageThat()
            .contains(keyOrValueNotFoundErrorMessage(""))
        @Serializable data class Foo(val i: Int)
        @Serializable data class Bar(val i: Int)
        assertEquals(Bar(3), decodeFromSavedState<Bar>(encodeToSavedState(Foo(3))))
    }

    @Test
    fun decodeMissingKey() {
        assertThrows(IllegalArgumentException::class) { decodeFromSavedState<Int>(savedState()) }
            .hasMessageThat()
            .contains(keyOrValueNotFoundErrorMessage(""))
    }

    // This is not ideal. The test is just to document the behavior.
    @Test
    fun illegalWrite() {
        val savedState = encodeToSavedState(3)
        savedState.write { putString("", "foo") }
        // Got exception instead of `3` because the savedState got manipulated after
        // encoding.
        assertThrows<IllegalArgumentException> { decodeFromSavedState<Int>(savedState) }
            .hasMessageThat()
            .contains(keyOrValueNotFoundErrorMessage(""))
    }

    @Test
    fun serializerMissingThrowsException() {
        assertThrows<SerializationException> {
                MyColor(1, 3, 5).encodeDecode {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getIntArray("")).isEqualTo(intArrayOf(1, 3, 5))
                }
            }
            .hasMessageThat()
            .contains("Serializer for class 'MyColor' is not found.")
    }

    @Test
    fun contextualSerialization() {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                contextual(MyColor::class, MyColorIntArraySerializer)
            }
        }

        // Specifying `ContextualSerializer` explicitly.
        MyColor(1, 3, 5).encodeDecode(
            configuration = config,
            serializer = ContextualSerializer(MyColor::class),
        ) {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntArray("")).isEqualTo(intArrayOf(1, 3, 5))
        }
    }

    @Test
    fun contextualSerializationImplicit() {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                contextual(MyColor::class, MyColorIntArraySerializer)
            }
        }

        // Fallback to contextual serializer as `MyColor` doesn't have an associated serializer.
        MyColor(1, 3, 5).encodeDecode(configuration = config) {
            assertThat(size()).isEqualTo(1)
            assertThat(getIntArray("")).isEqualTo(intArrayOf(1, 3, 5))
        }
    }

    @Test
    fun polymorphicSerialization() {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(Shape::class) {
                    subclass(Circle::class, Circle.serializer())
                    subclass(Rectangle::class, Rectangle.serializer())
                }
            }
        }

        Circle(3).encodeDecode<Shape>(
            configuration = config,
            // This is needed only in Kotlin/Native.
            serializer = PolymorphicSerializer(Shape::class),
        ) {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("type")).isEqualTo("androidx.savedstate.serialization.Circle")
            getSavedState("value").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("radius")).isEqualTo(3)
            }
        }
        Rectangle(3, 5).encodeDecode<Shape>(
            configuration = config,
            // This is needed only in Kotlin/Native.
            serializer = PolymorphicSerializer(Shape::class),
        ) {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("type")).isEqualTo("androidx.savedstate.serialization.Rectangle")
            getSavedState("value").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getInt("width")).isEqualTo(3)
                assertThat(getInt("height")).isEqualTo(5)
            }
        }
    }

    // Encode a `List<Any>` with `SerializersModule`.
    @Test
    fun polymorphicSerializationContextual() {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                contextual(Any::class, PolymorphicSerializer(Any::class))
                polymorphic(Any::class) {
                    subclass(String::class)
                    subclass(Int::class)
                }
            }
        }
        listOf("foo", 123).encodeDecode<List<Any>>(configuration = config) {
            assertThat(size()).isEqualTo(2)
            getSavedState("0").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getString("type")).isEqualTo("kotlin.String")
                assertThat(getString("value")).isEqualTo("foo")
            }
            getSavedState("1").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getString("type")).isEqualTo("kotlin.Int")
                assertThat(getInt("value")).isEqualTo(123)
            }
        }
    }

    @Test
    fun builtInSerializer() {
        savedState {
                putString("name", "foo")
                putInt("age", 99)
            }
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.read { contentDeepEquals(original) }).isTrue()
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(2)
                    assertThat(getString("name")).isEqualTo("foo")
                    assertThat(getInt("age")).isEqualTo(99)
                },
            )
    }

    @Test
    fun contextualSerializer() {
        savedState {
                putString("name", "foo")
                putInt("age", 99)
            }
            .encodeDecode(
                serializer = ContextualSerializer(SavedState::class),
                checkDecoded = { decoded, original ->
                    assertThat(decoded.read { contentDeepEquals(original) }).isTrue()
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(2)
                    assertThat(getString("name")).isEqualTo("foo")
                    assertThat(getInt("age")).isEqualTo(99)
                },
            )
    }

    @Test
    fun nullableDataClass() {
        // Check serialization of nullable types. Test null values and non-null values.
        // Serialization of null does not depend on the type. The codec always
        // writes a null marker.
        // If NullableSerializer works for specific types, it works for all types.
        @Serializable data class MyModel(val id: Int, val name: String)

        val original = MyModel(id = 42, name = "SavedState")

        // Test non-null value with static type T
        original.encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("id")).isEqualTo(42)
            assertThat(getString("name")).isEqualTo("SavedState")
        }

        // Test non-null value with static type T?
        original.encodeDecode<MyModel?> {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("id")).isEqualTo(42)
            assertThat(getString("name")).isEqualTo("SavedState")
        }

        // Test null value with static type T?
        null.encodeDecode<MyModel?>(
            checkDecoded = { decoded, _ -> assertThat(decoded).isNull() },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(isNull("")).isTrue()
            },
        )
    }
}

private fun keyOrValueNotFoundErrorMessage(key: String): String {
    return "No valid saved state was found for the key '$key'. It may be missing, null, or not " +
        "of the expected type. This can occur if the value was saved with a different type or if " +
        "the saved state was modified unexpectedly."
}

@Serializable
data class MyTreeNode<T>(
    val value: T,
    val left: MyTreeNode<T>? = null,
    val right: MyTreeNode<T>? = null,
)

// `@Serializable` is needed for using the enum as root in native and js.
@Serializable
enum class MyEnum {
    A,
    B,
    C,
}

@Serializable @JvmInline private value class MyValueClassToString(val value: String)

private typealias MyTypeAliasToInt = Int

private typealias MyNestedTypeAlias = MyTypeAliasToInt

@Serializable
private sealed class Node {
    @Serializable data class Add(val lhs: Operand, val rhs: Operand) : Node()

    @Serializable data class Operand(val value: Int) : Node()
}

@Serializable
private class MyDelegatedList<E>(val values: ArrayList<E>) : MutableList<E> by values {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MyDelegatedList<*>
        return values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}

@Serializable
object MyObject {
    val foo = "bar"
}

private data class MyColor(val r: Int, val g: Int, val b: Int)

@OptIn(ExperimentalSerializationApi::class)
private object MyColorIntArraySerializer : KSerializer<MyColor> {
    private val delegateSerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("MyColor", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: MyColor) {
        val data = value.run { intArrayOf(r, g, b) }
        encoder.encodeSerializableValue(delegateSerializer, data)
    }

    override fun deserialize(decoder: Decoder): MyColor {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return MyColor(array[0], array[1], array[2])
    }
}

@Serializable
private sealed class F {
    val s: String? = null
}

@Serializable private data class G(val i: Int) : F()

interface Shape

@Serializable data class Circle(val radius: Int) : Shape

@Serializable data class Rectangle(val width: Int, val height: Int) : Shape
