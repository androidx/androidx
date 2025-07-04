/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate

import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.core.os.bundleOf
import androidx.kruth.assertThat
import androidx.savedstate.serialization.SavedStateCodecTestUtils.encodeDecode
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.savedstate.serialization.serializers.JavaSerializableSerializer
import androidx.savedstate.serialization.serializers.ParcelableSerializer
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import androidx.savedstate.serialization.serializers.SizeFSerializer
import androidx.savedstate.serialization.serializers.SizeSerializer
import androidx.savedstate.serialization.serializers.SparseArraySerializer
import java.util.UUID
import kotlin.test.Test
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@ExperimentalSerializationApi
internal class SavedStateCodecAndroidTest : RobolectricTest() {
    @Test
    fun customSerializers() {
        val uuid = UUID.randomUUID()

        uuid.encodeDecode(MyUUIDSerializer()) {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo(uuid.toString())
        }
        Size(128, 256).encodeDecode(MySizeSerializer()) {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("width")).isEqualTo(128)
            assertThat(getInt("height")).isEqualTo(256)
        }

        @Serializable
        data class MyModel(
            @Serializable(with = MyUUIDSerializer::class) val uuid: UUID,
            @Serializable(with = MySizeSerializer::class) val size: Size,
        )
        val uuid2 = UUID.randomUUID()
        MyModel(uuid2, Size(3, 5)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getString("uuid")).isEqualTo(uuid2.toString())
            getSavedState("size").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getInt("width")).isEqualTo(3)
                assertThat(getInt("height")).isEqualTo(5)
            }
        }
    }

    @Test
    fun bundle() {
        @Suppress(
            "SERIALIZER_TYPE_INCOMPATIBLE"
        ) // The lint warning does not show up for external users.
        @Serializable
        class MyClass(@Serializable(with = SavedStateSerializer::class) val s: Bundle)
        MyClass(
                bundleOf(
                    "i" to 1,
                    "s" to "foo",
                    "a" to intArrayOf(1, 3, 5),
                    "ss" to bundleOf("s" to "bar"),
                )
            )
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.s.read { contentDeepEquals(original.s) }).isTrue()
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

        // Bundle at root.
        val origin = bundleOf("i" to 3, "s" to "foo", "d" to 3.14)
        val encoded = encodeToSavedState(SavedStateSerializer, origin)
        val restored = decodeFromSavedState(SavedStateSerializer, encoded)
        // Bundle's `equals` doesn't compare contents.
        encoded.read {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("i")).isEqualTo(3)
            assertThat(getString("s")).isEqualTo("foo")
            assertThat(getDouble("d")).isEqualTo(3.14)
        }
        assertThat(restored.read { contentDeepEquals(origin) }).isTrue()
        assertThat(restored).isNotSameInstanceAs(origin)
    }

    @Test
    fun sizeAndSizeF() {
        @Serializable
        data class MyModel(
            @Serializable(with = SizeSerializer::class) val size: Size,
            @Serializable(with = SizeFSerializer::class) val sizeF: SizeF,
        )

        MyModel(Size(128, 256), SizeF(1.23f, 4.56f)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getSize("size")).isEqualTo(Size(128, 256))
            assertThat(getSizeF("sizeF")).isEqualTo(SizeF(1.23f, 4.56f))
        }
    }

    @Test
    fun interfaceTypes() {
        @Serializable data class CharSequenceContainer(val value: CharSequence)
        CharSequenceContainer("foo").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequence("value")).isEqualTo("foo")
        }

        @Serializable
        data class SerializableContainer(
            @Serializable(with = CustomJavaSerializableSerializer::class)
            val value: java.io.Serializable
        )
        val myJavaSerializable = MyJavaSerializable(3, "foo", 3.14)
        SerializableContainer(myJavaSerializable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getJavaSerializable<MyJavaSerializable>("value"))
                .isEqualTo(myJavaSerializable)
        }

        @Serializable
        data class ParcelableContainer(
            @Serializable(with = CustomParcelableSerializer::class) val value: Parcelable
        )
        val myParcelable = MyParcelable(3, "foo", 3.14)
        ParcelableContainer(myParcelable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelable<MyParcelable>("value")).isEqualTo(myParcelable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Serializable data class IBinderContainer(val value: IBinder)
            val binder = Binder("foo")
            IBinderContainer(binder).encodeDecode(doMarshalling = false) {
                assertThat(size()).isEqualTo(1)
                assertThat(getBinder("value")).isEqualTo(binder)
            }
        } else {
            error("VERSION.SDK_INT < Q")
        }
    }

    @Test
    fun interfaceTypesWithoutExplicitSerializer() {
        @Serializable data class CharSequenceContainer(val value: CharSequence)
        CharSequenceContainer("foo").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequence("value")).isEqualTo("foo")
        }

        @Serializable data class SerializableContainer(val value: java.io.Serializable)
        val myJavaSerializable = MyJavaSerializable(3, "foo", 3.14)
        SerializableContainer(myJavaSerializable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getJavaSerializable<MyJavaSerializable>("value"))
                .isEqualTo(myJavaSerializable)
        }

        @Serializable data class ParcelableContainer(val value: Parcelable)
        val myParcelable = MyParcelable(3, "foo", 3.14)
        ParcelableContainer(myParcelable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelable<MyParcelable>("value")).isEqualTo(myParcelable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Serializable data class IBinderContainer(val value: IBinder)
            val binder = Binder("foo")
            IBinderContainer(binder).encodeDecode(doMarshalling = false) {
                assertThat(size()).isEqualTo(1)
                assertThat(getBinder("value")).isEqualTo(binder)
            }
        } else {
            error("VERSION.SDK_INT < Q")
        }
    }

    @Test
    fun concreteTypesInsteadOfInterfaceTypes() {
        @Serializable
        data class SerializableContainer(
            @Serializable(with = MyJavaSerializableAsJavaSerializableSerializer::class)
            val value: MyJavaSerializable
        )
        val myJavaSerializable = MyJavaSerializable(3, "foo", 3.14)
        SerializableContainer(myJavaSerializable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getJavaSerializable<MyJavaSerializable>("value"))
                .isEqualTo(myJavaSerializable)
        }

        @Serializable
        data class ParcelableContainer(
            @Serializable(with = MyParcelableSerializer::class) val value: MyParcelable
        )
        val myParcelable = MyParcelable(3, "foo", 3.14)
        ParcelableContainer(myParcelable).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelable<MyParcelable>("value")).isEqualTo(myParcelable)
        }
    }

    @Test
    fun collectionTypes() {
        @Serializable
        @Suppress("ArrayInDataClass")
        data class CharSequenceArrayContainer(val value: Array<out CharSequence>)
        val myCharSequenceArray = arrayOf(StringBuilder("foo"), StringBuilder("bar"))
        myCharSequenceArray.encodeDecode<Array<out CharSequence>>(
            checkDecoded = { decoded, original -> assertThat(decoded.contentEquals(original)) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getCharSequenceArray("").contentEquals(myCharSequenceArray)).isTrue()
            },
        )
        CharSequenceArrayContainer(myCharSequenceArray)
            .encodeDecode(
                checkDecoded = { decoded, original -> decoded.value.contentEquals(original.value) },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getCharSequenceArray("value")).isEqualTo(myCharSequenceArray)
                },
            )

        @Serializable
        @Suppress("ArrayInDataClass")
        data class ParcelableArrayContainer(val value: Array<out Parcelable>)
        val myParcelableArray = arrayOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableArray.encodeDecode<Array<out Parcelable>>(
            checkDecoded = { decoded, original -> assertThat(decoded.contentEquals(original)) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableArray<MyParcelable>("").contentEquals(myParcelableArray))
                    .isTrue()
            },
        )
        ParcelableArrayContainer(myParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original -> decoded.value.contentEquals(original.value) },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getParcelableArray<MyParcelable>("value"))
                        .isEqualTo(myParcelableArray)
                },
            )

        @Serializable data class CharSequenceListContainer(val value: List<CharSequence>)
        val myCharSequenceList = arrayListOf("foo", "bar")
        myCharSequenceList.encodeDecode<List<CharSequence>>(
            checkDecoded = { decoded, original -> assertThat(decoded).isEqualTo(original) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getCharSequenceList("")).isEqualTo(myCharSequenceList)
            },
        )
        CharSequenceListContainer(myCharSequenceList).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequenceList("value")).isEqualTo(myCharSequenceList)
        }

        @Serializable data class ParcelableListContainer(val value: List<Parcelable>)
        val myParcelableList =
            arrayListOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableList.encodeDecode<List<Parcelable>>(
            checkDecoded = { decoded, original -> assertThat(decoded).isEqualTo(original) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableList<MyParcelable>("")).isEqualTo(myParcelableList)
            },
        )
        ParcelableListContainer(myParcelableList).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelableList<MyParcelable>("value")).isEqualTo(myParcelableList)
        }

        @Serializable
        data class SparseParcelableArrayContainer(
            @Serializable(with = SparseArraySerializer::class)
            val value: SparseArray<out Parcelable>
        )
        val mySparseParcelableArray =
            SparseArray<MyParcelable>().apply {
                append(1, MyParcelable(3, "foo", 3.14))
                append(3, MyParcelable(4, "bar", 1.73))
            }
        mySparseParcelableArray.encodeDecode<SparseArray<out Parcelable>>(
            checkDecoded = { decoded, original ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    assertThat(decoded.contentEquals(original))
                } else {
                    error("VERSION.SDK_INT < S")
                }
            },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getSparseParcelableArray<Parcelable>(""))
                    .isEqualTo(mySparseParcelableArray)
            },
        )
        SparseParcelableArrayContainer(mySparseParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        decoded.value.contentEquals(original.value)
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getSparseParcelableArray<Parcelable>("value"))
                        .isEqualTo(mySparseParcelableArray)
                },
            )
    }

    @Test
    fun concreteTypesWithContextualSerializer() {
        @Serializable data class MyModel(@Contextual val size: Size, @Contextual val sizeF: SizeF)

        MyModel(Size(128, 256), SizeF(1.23f, 4.56f)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getSize("size")).isEqualTo(Size(128, 256))
            assertThat(getSizeF("sizeF")).isEqualTo(SizeF(1.23f, 4.56f))
        }

        @Serializable
        data class SparseParcelableArrayContainer(
            @Contextual val value: SparseArray<out Parcelable>
        )
        val mySparseParcelableArray =
            SparseArray<MyParcelable>().apply {
                append(1, MyParcelable(3, "foo", 3.14))
                append(3, MyParcelable(4, "bar", 1.73))
            }
        SparseParcelableArrayContainer(mySparseParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        decoded.value.contentEquals(original.value)
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getSparseParcelableArray<Parcelable>("value"))
                        .isEqualTo(mySparseParcelableArray)
                },
            )
    }

    @Test
    fun collectionTypesWithoutExplicitSerializer() {
        @Serializable
        @Suppress("ArrayInDataClass")
        data class CharSequenceArrayContainer(val value: Array<out CharSequence>)
        val myCharSequenceArray = arrayOf(StringBuilder("foo"), StringBuilder("bar"))
        myCharSequenceArray.encodeDecode<Array<out CharSequence>>(
            checkDecoded = { decoded, original -> assertThat(decoded.contentEquals(original)) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getCharSequenceArray("").contentEquals(myCharSequenceArray)).isTrue()
            },
        )
        CharSequenceArrayContainer(myCharSequenceArray)
            .encodeDecode(
                checkDecoded = { decoded, original -> decoded.value.contentEquals(original.value) },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getCharSequenceArray("value")).isEqualTo(myCharSequenceArray)
                },
            )

        @Serializable
        @Suppress("ArrayInDataClass")
        data class ParcelableArrayContainer(val value: Array<out Parcelable>)
        val myParcelableArray = arrayOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableArray.encodeDecode<Array<out Parcelable>>(
            checkDecoded = { decoded, original -> assertThat(decoded.contentEquals(original)) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableArray<MyParcelable>("").contentEquals(myParcelableArray))
                    .isTrue()
            },
        )
        ParcelableArrayContainer(myParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original -> decoded.value.contentEquals(original.value) },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getParcelableArray<MyParcelable>("value"))
                        .isEqualTo(myParcelableArray)
                },
            )

        @Serializable data class CharSequenceListContainer(val value: List<CharSequence>)
        val myCharSequenceList = arrayListOf("foo", "bar")
        myCharSequenceList.encodeDecode<List<CharSequence>>(
            checkDecoded = { decoded, original -> assertThat(decoded).isEqualTo(original) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getCharSequenceList("")).isEqualTo(myCharSequenceList)
            },
        )
        CharSequenceListContainer(myCharSequenceList).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequenceList("value")).isEqualTo(myCharSequenceList)
        }

        @Serializable data class ParcelableListContainer(val value: List<Parcelable>)
        val myParcelableList =
            arrayListOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableList.encodeDecode<List<Parcelable>>(
            checkDecoded = { decoded, original -> assertThat(decoded).isEqualTo(original) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableList<MyParcelable>("")).isEqualTo(myParcelableList)
            },
        )
        ParcelableListContainer(myParcelableList).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelableList<MyParcelable>("value")).isEqualTo(myParcelableList)
        }

        @Serializable
        data class SparseParcelableArrayContainer(
            @Serializable(with = SparseArraySerializer::class)
            val value: SparseArray<out Parcelable>
        )
        val mySparseParcelableArray =
            SparseArray<MyParcelable>().apply {
                append(1, MyParcelable(3, "foo", 3.14))
                append(3, MyParcelable(4, "bar", 1.73))
            }
        mySparseParcelableArray.encodeDecode(
            serializer = SparseArraySerializer(MyParcelableSerializer),
            checkDecoded = { decoded, original ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    assertThat(decoded.contentEquals(original))
                } else {
                    error("VERSION.SDK_INT < S")
                }
            },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getSparseParcelableArray<Parcelable>(""))
                    .isEqualTo(mySparseParcelableArray)
            },
        )
        SparseParcelableArrayContainer(mySparseParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        assertThat(decoded.value.contentEquals(original.value))
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getSparseParcelableArray<Parcelable>("value"))
                        .isEqualTo(mySparseParcelableArray)
                },
            )
    }

    @Test
    fun collectionTypesWithConcreteElement() {
        @Suppress("ArrayInDataClass")
        @Serializable
        data class ParcelableArrayContainer(
            val value: Array<@Serializable(with = MyParcelableSerializer::class) MyParcelable>
        )
        val myParcelableArray = arrayOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableArray.encodeDecode(serializer = ArraySerializer(MyParcelableSerializer)) {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelableArray<MyParcelable>("")).isEqualTo(myParcelableArray)
        }
        ParcelableArrayContainer(myParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.value.contentEquals(original.value)).isTrue()
                },
                checkEncoded = {
                    assertThat(
                        getParcelableArray<MyParcelable>("value").contentEquals(myParcelableArray)
                    )
                },
            )

        @Serializable
        data class ParcelableListContainer(
            // Unlike arrays this works as `List`s can be down-casted, e.g.
            // a `List<Parcelable>` can be casted to `List<MyParcelable>`.
            val value: List<@Serializable(with = MyParcelableSerializer::class) MyParcelable>
        )
        val myParcelableList =
            arrayListOf(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
        myParcelableList.encodeDecode<List<Parcelable>>(
            checkDecoded = { decoded, original -> assertThat(decoded).isEqualTo(original) },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableList<MyParcelable>("")).isEqualTo(myParcelableList)
            },
        )
        ParcelableListContainer(myParcelableList).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelableList<MyParcelable>("value")).isEqualTo(myParcelableList)
        }

        @Serializable
        data class SparseParcelableArrayContainer(
            // Unlike arrays this works as `SparseArray`s can be down-casted, e.g.
            // a `SparseArray<Parcelable>` can be casted to `SparseArray<MyParcelable>`.
            @Serializable(with = SparseArraySerializer::class)
            val value: SparseArray<@Serializable(with = MyParcelableSerializer::class) MyParcelable>
        )
        val mySparseParcelableArray =
            SparseArray<MyParcelable>().apply {
                append(1, MyParcelable(3, "foo", 3.14))
                append(3, MyParcelable(4, "bar", 1.73))
            }
        mySparseParcelableArray.encodeDecode(
            serializer = SparseArraySerializer(MyParcelableSerializer),
            checkDecoded = { decoded, original ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    assertThat(decoded.contentEquals(original))
                } else {
                    error("VERSION.SDK_INT < S")
                }
            },
            checkEncoded = {
                assertThat(size()).isEqualTo(1)
                assertThat(getSparseParcelableArray<Parcelable>(""))
                    .isEqualTo(mySparseParcelableArray)
            },
        )
        SparseParcelableArrayContainer(mySparseParcelableArray)
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        assertThat(decoded.value.contentEquals(original.value))
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getSparseParcelableArray<Parcelable>("value"))
                        .isEqualTo(mySparseParcelableArray)
                },
            )
    }

    @Test
    fun canUseBuiltInSerializersAutomatically() {
        Size(3, 5).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getSize("")).isEqualTo(Size(3, 5))
        }
        SizeF(3.14f, 4.732f).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getSizeF("")).isEqualTo(SizeF(3.14f, 4.732f))
        }
        StringBuilder("foo")
            .encodeDecode<CharSequence>(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.toString()).isEqualTo(original.toString())
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getCharSequence("").toString()).isEqualTo("foo")
                },
            )
        MyParcelable(3, "foo", 3.14).encodeDecode<Parcelable> {
            assertThat(size()).isEqualTo(1)
            assertThat(getParcelable<MyParcelable>("")).isEqualTo(MyParcelable(3, "foo", 3.14))
        }
        arrayOf<CharSequence>("foo", "bar").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequenceArray("")).isEqualTo(arrayOf<CharSequence>("foo", "bar"))
        }
        arrayOf<Parcelable>(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableArray<MyParcelable>(""))
                    .isEqualTo(
                        arrayOf<Parcelable>(
                            MyParcelable(3, "foo", 3.14),
                            MyParcelable(4, "bar", 1.73),
                        )
                    )
            }
        listOf<CharSequence>("foo", "bar").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getCharSequenceList("")).isEqualTo(listOf<CharSequence>("foo", "bar"))
        }
        listOf<Parcelable>(MyParcelable(3, "foo", 3.14), MyParcelable(4, "bar", 1.73))
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getParcelableList<MyParcelable>(""))
                    .isEqualTo(
                        listOf<Parcelable>(
                            MyParcelable(3, "foo", 3.14),
                            MyParcelable(4, "bar", 1.73),
                        )
                    )
            }

        SparseArray<Parcelable?>()
            .apply {
                append(1, MyParcelable(3, "foo", 3.14))
                append(3, MyParcelable(4, "bar", 1.73))
                append(5, null)
            }
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        assertThat(decoded.contentEquals(original)).isTrue()
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        assertThat(
                                getSparseParcelableArray<Parcelable>("")
                                    .contentEquals(
                                        SparseArray<Parcelable?>().apply {
                                            append(1, MyParcelable(3, "foo", 3.14))
                                            append(3, MyParcelable(4, "bar", 1.73))
                                            append(5, null)
                                        }
                                    )
                            )
                            .isTrue()
                    } else {
                        error("VERSION.SDK_INT < S")
                    }
                },
            )
    }

    @Test
    fun sparseArray() {
        val sparseArray =
            SparseArray<String?>().apply {
                put(1, "foo")
                put(3, "bar")
                put(5, null)
            }
        sparseArray.encodeDecode(
            checkDecoded = { decoded, original ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    assertThat(decoded.contentEquals(original)).isTrue()
                } else {
                    error("VERSION.SDK_INT < S")
                }
            },
            checkEncoded = {
                assertThat(size()).isEqualTo(2)
                assertThat(getIntList("keys")).isEqualTo(listOf(1, 3, 5))
                getSavedState("values").read {
                    assertThat(getString("0")).isEqualTo("foo")
                    assertThat(getString("1")).isEqualTo("bar")
                    assertThat(isNull("2")).isTrue()
                }
            },
        )
    }
}

private class MyUUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}

private class MySizeSerializer : KSerializer<Size> {
    override val descriptor: SerialDescriptor
        get() =
            buildClassSerialDescriptor("SizeDescriptor") {
                element("width", PrimitiveSerialDescriptor("width", PrimitiveKind.INT))
                element("height", PrimitiveSerialDescriptor("height", PrimitiveKind.INT))
            }

    override fun deserialize(decoder: Decoder): Size {
        return decoder.decodeStructure(descriptor) {
            var width = 0
            var height = 0
            while (true) {
                when (decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> width = decodeIntElement(descriptor, 0)
                    1 -> height = decodeIntElement(descriptor, 1)
                    else -> error("what?")
                }
            }
            Size(width, height)
        }
    }

    override fun serialize(encoder: Encoder, value: Size) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.width)
            encodeIntElement(descriptor, 1, value.height)
        }
    }
}

private data class MyJavaSerializable(val i: Int, val s: String, val d: Double) :
    java.io.Serializable

private data class MyParcelable(val i: Int, val s: String, val d: Double) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readString()!!, parcel.readDouble())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(i)
        parcel.writeString(s)
        parcel.writeDouble(d)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MyParcelable> {
        override fun createFromParcel(parcel: Parcel): MyParcelable {
            return MyParcelable(parcel)
        }

        override fun newArray(size: Int): Array<MyParcelable?> {
            return arrayOfNulls(size)
        }
    }
}

private class MyJavaSerializableAsJavaSerializableSerializer :
    JavaSerializableSerializer<MyJavaSerializable>()

private object MyParcelableSerializer : ParcelableSerializer<MyParcelable>()

private class CustomJavaSerializableSerializer : JavaSerializableSerializer<java.io.Serializable>()

private class CustomParcelableSerializer : ParcelableSerializer<Parcelable>()

private fun assertCharSequenceList(actual: List<CharSequence>, expect: List<CharSequence>) {
    assertThat(actual.size).isEqualTo(expect.size)
    for (i in actual.indices) {
        assertThat(actual[i].contentEquals(expect[i])).isTrue()
    }
}
