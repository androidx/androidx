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

package androidx.glance.adaptive.core.util

import android.os.Bundle
import android.util.SparseArray
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BundleUtilsTest {

    @Test
    fun areBundlesEqual_sameInstance_returnsTrue() {
        val bundle = Bundle().apply { putString("key", "value") }
        assertThat(BundleUtils.areBundlesEqual(bundle, bundle)).isTrue()
    }

    @Test
    fun areBundlesEqual_bothEmpty_returnsTrue() {
        assertThat(BundleUtils.areBundlesEqual(Bundle(), Bundle())).isTrue()
    }

    @Test
    fun areBundlesEqual_oneEmptyOneNonEmpty_returnsFalse() {
        val empty = Bundle()
        val nonEmpty = Bundle().apply { putString("key", "val") }
        assertThat(BundleUtils.areBundlesEqual(empty, nonEmpty)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(nonEmpty, empty)).isFalse()
    }

    @Test
    fun areBundlesEqual_differentSizes_returnsFalse() {
        val bundle1 = Bundle().apply { putString("a", "1") }
        val bundle2 =
            Bundle().apply {
                putString("a", "1")
                putString("b", "2")
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, bundle2)).isFalse()
    }

    @Test
    fun areBundlesEqual_differentKeys_returnsFalse() {
        val bundle1 = Bundle().apply { putString("a", "1") }
        val bundle2 = Bundle().apply { putString("b", "1") }
        assertThat(BundleUtils.areBundlesEqual(bundle1, bundle2)).isFalse()
    }

    @Test
    fun areBundlesEqual_differentKeysSameSize_returnsFalse() {
        val bundle1 =
            Bundle().apply {
                putString("a", "1")
                putString("b", "2")
            }
        val bundle2 =
            Bundle().apply {
                putString("a", "1")
                putString("c", "2")
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, bundle2)).isFalse()
    }

    @Test
    fun areBundlesEqual_primitiveValuesEqual_returnsTrue() {
        val bundle1 = createSamplePrimitiveValueBundle()
        val bundle2 = createSamplePrimitiveValueBundle()
        assertThat(BundleUtils.areBundlesEqual(bundle1, bundle2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isEqualTo(BundleUtils.bundleHashCode(bundle2))
    }

    @Test
    fun areBundlesEqual_primitiveValuesDifferent_returnsFalse() {
        val bundle1 = createSamplePrimitiveValueBundle()

        val diffStr = createSamplePrimitiveValueBundle().apply { putString("str", "world") }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffStr)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffStr))

        val diffInt = createSamplePrimitiveValueBundle().apply { putInt("int", 99) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffInt)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffInt))

        val diffBool = createSamplePrimitiveValueBundle().apply { putBoolean("bool", false) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffBool)).isFalse()

        val diffFloat = createSamplePrimitiveValueBundle().apply { putFloat("float", 9.9f) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffFloat)).isFalse()

        val diffDouble = createSamplePrimitiveValueBundle().apply { putDouble("double", 9.9) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffDouble)).isFalse()

        val diffLong = createSamplePrimitiveValueBundle().apply { putLong("long", 999L) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffLong)).isFalse()

        val diffByte = createSamplePrimitiveValueBundle().apply { putByte("byte", 99.toByte()) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffByte)).isFalse()

        val diffShort = createSamplePrimitiveValueBundle().apply { putShort("short", 99.toShort()) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffShort)).isFalse()

        val diffChar = createSamplePrimitiveValueBundle().apply { putChar("char", 'z') }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffChar)).isFalse()
    }

    @Test
    fun areBundlesEqual_differentTypesForSameKey_returnsFalse() {
        val strBundle = Bundle().apply { putString("key", "42") }
        val intBundle = Bundle().apply { putInt("key", 42) }
        assertThat(BundleUtils.areBundlesEqual(strBundle, intBundle)).isFalse()

        val bundleWithBundle = Bundle().apply { putBundle("key", Bundle()) }
        val bundleWithString = Bundle().apply { putString("key", "test") }
        assertThat(BundleUtils.areBundlesEqual(bundleWithBundle, bundleWithString)).isFalse()

        val byteArrayBundle = Bundle().apply { putByteArray("key", byteArrayOf(1, 2)) }
        val intArrayBundle = Bundle().apply { putIntArray("key", intArrayOf(1, 2)) }
        assertThat(BundleUtils.areBundlesEqual(byteArrayBundle, intArrayBundle)).isFalse()

        val arrayBundle = Bundle().apply { putParcelableArray("key", arrayOf(Bundle())) }
        val listBundle = Bundle().apply { putParcelableArrayList("key", arrayListOf(Bundle())) }
        assertThat(BundleUtils.areBundlesEqual(arrayBundle, listBundle)).isFalse()
    }

    @Test
    fun areBundlesEqual_primitiveArraysEqual_returnsTrue() {
        val bundle1 = createSamplePrimitiveArrayBundle()
        val bundle2 = createSamplePrimitiveArrayBundle()
        assertThat(BundleUtils.areBundlesEqual(bundle1, bundle2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isEqualTo(BundleUtils.bundleHashCode(bundle2))
    }

    @Test
    fun areBundlesEqual_emptyPrimitiveArrays_returnsTrue() {
        val b1 =
            Bundle().apply {
                putByteArray("b", byteArrayOf())
                putIntArray("i", intArrayOf())
            }
        val b2 =
            Bundle().apply {
                putByteArray("b", byteArrayOf())
                putIntArray("i", intArrayOf())
            }
        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
    }

    @Test
    fun areBundlesEqual_primitiveArraysDifferent_returnsFalse() {
        val bundle1 = createSamplePrimitiveArrayBundle()

        // Same keys and count, but each primitive array has differing content
        val diffByte =
            createSamplePrimitiveArrayBundle().apply { putByteArray("byteArr", byteArrayOf(1, 9)) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffByte)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffByte))

        val diffShort =
            createSamplePrimitiveArrayBundle().apply {
                putShortArray("shortArr", shortArrayOf(3, 9))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffShort)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffShort))

        val diffChar =
            createSamplePrimitiveArrayBundle().apply {
                putCharArray("charArr", charArrayOf('a', 'z'))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffChar)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffChar))

        val diffInt =
            createSamplePrimitiveArrayBundle().apply { putIntArray("intArr", intArrayOf(5, 9)) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffInt)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffInt))

        val diffLong =
            createSamplePrimitiveArrayBundle().apply {
                putLongArray("longArr", longArrayOf(7L, 9L))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffLong)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffLong))

        val diffFloat =
            createSamplePrimitiveArrayBundle().apply {
                putFloatArray("floatArr", floatArrayOf(1.0f, 9.0f))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffFloat)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffFloat))

        val diffDouble =
            createSamplePrimitiveArrayBundle().apply {
                putDoubleArray("doubleArr", doubleArrayOf(3.0, 9.0))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffDouble)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffDouble))

        val diffBool =
            createSamplePrimitiveArrayBundle().apply {
                putBooleanArray("boolArr", booleanArrayOf(true, true))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffBool)).isFalse()
        assertThat(BundleUtils.bundleHashCode(bundle1))
            .isNotEqualTo(BundleUtils.bundleHashCode(diffBool))
    }

    @Test
    fun areBundlesEqual_primitiveArraysDifferentLength_returnsFalse() {
        val bundle1 = createSamplePrimitiveArrayBundle()

        val diffByteLen =
            createSamplePrimitiveArrayBundle().apply {
                putByteArray("byteArr", byteArrayOf(1, 2, 3))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffByteLen)).isFalse()

        val diffShortLen =
            createSamplePrimitiveArrayBundle().apply { putShortArray("shortArr", shortArrayOf(3)) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffShortLen)).isFalse()

        val diffCharLen =
            createSamplePrimitiveArrayBundle().apply { putCharArray("charArr", charArrayOf('a')) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffCharLen)).isFalse()

        val diffIntLen =
            createSamplePrimitiveArrayBundle().apply { putIntArray("intArr", intArrayOf(5, 6, 7)) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffIntLen)).isFalse()

        val diffLongLen =
            createSamplePrimitiveArrayBundle().apply { putLongArray("longArr", longArrayOf(7L)) }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffLongLen)).isFalse()

        val diffFloatLen =
            createSamplePrimitiveArrayBundle().apply {
                putFloatArray("floatArr", floatArrayOf(1.0f, 2.0f, 3.0f))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffFloatLen)).isFalse()

        val diffDoubleLen =
            createSamplePrimitiveArrayBundle().apply {
                putDoubleArray("doubleArr", doubleArrayOf(3.0))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffDoubleLen)).isFalse()

        val diffBoolLen =
            createSamplePrimitiveArrayBundle().apply {
                putBooleanArray("boolArr", booleanArrayOf(true, false, true))
            }
        assertThat(BundleUtils.areBundlesEqual(bundle1, diffBoolLen)).isFalse()
    }

    @Test
    fun areBundlesEqual_stringArrays_worksCorrectly() {
        val b1 = Bundle().apply { putStringArray("arr", arrayOf("a", "b", "c")) }
        val b2 = Bundle().apply { putStringArray("arr", arrayOf("a", "b", "c")) }
        val diffElem = Bundle().apply { putStringArray("arr", arrayOf("a", "x", "c")) }
        val diffLen = Bundle().apply { putStringArray("arr", arrayOf("a", "b")) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, diffElem)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(b1, diffLen)).isFalse()
    }

    @Test
    fun areBundlesEqual_nestedArrays_worksCorrectly() {
        val nested1: Array<Array<String>> = arrayOf(arrayOf("a", "b"), arrayOf("c", "d"))
        val nested2: Array<Array<String>> = arrayOf(arrayOf("a", "b"), arrayOf("c", "d"))
        val nestedDiff: Array<Array<String>> = arrayOf(arrayOf("a", "b"), arrayOf("c", "diff"))

        val b1 = Bundle().apply { putSerializable("arr", nested1) }
        val b2 = Bundle().apply { putSerializable("arr", nested2) }
        val bDiff = Bundle().apply { putSerializable("arr", nestedDiff) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, bDiff)).isFalse()
    }

    @Test
    fun areBundlesEqual_stringLists_worksCorrectly() {
        val b1 = Bundle().apply { putStringArrayList("list", arrayListOf("a", "b", "c")) }
        val b2 = Bundle().apply { putStringArrayList("list", arrayListOf("a", "b", "c")) }
        val diffElem = Bundle().apply { putStringArrayList("list", arrayListOf("a", "x", "c")) }
        val diffSize = Bundle().apply { putStringArrayList("list", arrayListOf("a", "b")) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, diffElem)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(b1, diffSize)).isFalse()
    }

    @Test
    fun areBundlesEqual_integerLists_worksCorrectly() {
        val b1 = Bundle().apply { putIntegerArrayList("list", arrayListOf(1, 2, 3)) }
        val b2 = Bundle().apply { putIntegerArrayList("list", arrayListOf(1, 2, 3)) }
        val diffElem = Bundle().apply { putIntegerArrayList("list", arrayListOf(1, 9, 3)) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, diffElem)).isFalse()
    }

    @Test
    fun areBundlesEqual_nestedBundles_worksCorrectly() {
        val inner1 = Bundle().apply { putString("key", "val") }
        val inner2 = Bundle().apply { putString("key", "val") }
        val diffInner = Bundle().apply { putString("key", "diff") }

        val b1 = Bundle().apply { putBundle("nested", inner1) }
        val b2 = Bundle().apply { putBundle("nested", inner2) }
        val bDiff = Bundle().apply { putBundle("nested", diffInner) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, bDiff)).isFalse()
    }

    @Test
    fun areBundlesEqual_deeplyNestedBundles_worksCorrectly() {
        val deep1 =
            Bundle().apply {
                putBundle(
                    "level1",
                    Bundle().apply {
                        putBundle("level2", Bundle().apply { putString("key", "value") })
                    },
                )
            }
        val deep2 =
            Bundle().apply {
                putBundle(
                    "level1",
                    Bundle().apply {
                        putBundle("level2", Bundle().apply { putString("key", "value") })
                    },
                )
            }
        val deepDiff =
            Bundle().apply {
                putBundle(
                    "level1",
                    Bundle().apply {
                        putBundle("level2", Bundle().apply { putString("key", "different") })
                    },
                )
            }

        assertThat(BundleUtils.areBundlesEqual(deep1, deep2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(deep1)).isEqualTo(BundleUtils.bundleHashCode(deep2))
        assertThat(BundleUtils.areBundlesEqual(deep1, deepDiff)).isFalse()
    }

    @Test
    fun areBundlesEqual_arraysAndListsOfBundles_worksCorrectly() {
        val inner1 = Bundle().apply { putString("k", "v") }
        val inner2 = Bundle().apply { putString("k", "v") }
        val diffInner = Bundle().apply { putString("k", "v2") }

        val arrayBundle1 = Bundle().apply { putParcelableArray("arr", arrayOf(inner1)) }
        val arrayBundle2 = Bundle().apply { putParcelableArray("arr", arrayOf(inner2)) }
        val arrayBundleDiff = Bundle().apply { putParcelableArray("arr", arrayOf(diffInner)) }
        val arrayBundleDiffSize =
            Bundle().apply { putParcelableArray("arr", arrayOf(inner1, inner2)) }

        assertThat(BundleUtils.areBundlesEqual(arrayBundle1, arrayBundle2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(arrayBundle1))
            .isEqualTo(BundleUtils.bundleHashCode(arrayBundle2))
        assertThat(BundleUtils.areBundlesEqual(arrayBundle1, arrayBundleDiff)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(arrayBundle1, arrayBundleDiffSize)).isFalse()

        val listBundle1 = Bundle().apply { putParcelableArrayList("list", arrayListOf(inner1)) }
        val listBundle2 = Bundle().apply { putParcelableArrayList("list", arrayListOf(inner2)) }
        val listBundleDiff =
            Bundle().apply { putParcelableArrayList("list", arrayListOf(diffInner)) }
        val listBundleDiffSize =
            Bundle().apply { putParcelableArrayList("list", arrayListOf(inner1, inner2)) }

        assertThat(BundleUtils.areBundlesEqual(listBundle1, listBundle2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(listBundle1))
            .isEqualTo(BundleUtils.bundleHashCode(listBundle2))
        assertThat(BundleUtils.areBundlesEqual(listBundle1, listBundleDiff)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(listBundle1, listBundleDiffSize)).isFalse()
    }

    @Test
    fun areBundlesEqual_nullValues_worksCorrectly() {
        val b1 = Bundle().apply { putString("key", null) }
        val b2 = Bundle().apply { putString("key", null) }
        val bNonNull = Bundle().apply { putString("key", "notNull") }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, bNonNull)).isFalse()
        assertThat(BundleUtils.areBundlesEqual(bNonNull, b1)).isFalse()
    }

    @Test
    fun areBundlesEqual_nullElementsInArrayAndList_worksCorrectly() {
        val b1 = Bundle().apply { putStringArray("arr", arrayOf("a", null, "c")) }
        val b2 = Bundle().apply { putStringArray("arr", arrayOf("a", null, "c")) }
        val bDiff = Bundle().apply { putStringArray("arr", arrayOf("a", "b", "c")) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, bDiff)).isFalse()

        val listB1 = Bundle().apply { putStringArrayList("list", arrayListOf("a", null, "c")) }
        val listB2 = Bundle().apply { putStringArrayList("list", arrayListOf("a", null, "c")) }
        val listDiff = Bundle().apply { putStringArrayList("list", arrayListOf("a", "b", "c")) }

        assertThat(BundleUtils.areBundlesEqual(listB1, listB2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(listB1)).isEqualTo(BundleUtils.bundleHashCode(listB2))
        assertThat(BundleUtils.areBundlesEqual(listB1, listDiff)).isFalse()
    }

    @Test
    fun areBundlesEqual_andHashCode_keyInsertionOrderIndependent() {
        val b1 =
            Bundle().apply {
                putString("z", "last")
                putString("a", "first")
                putInt("m", 50)
            }
        val b2 =
            Bundle().apply {
                putInt("m", 50)
                putString("a", "first")
                putString("z", "last")
            }
        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
    }

    @Test
    fun bundleHashCode_emptyBundle_returnsZero() {
        assertThat(BundleUtils.bundleHashCode(Bundle())).isEqualTo(0)
    }

    @Test
    fun areBundlesEqual_sparseArraysEqual_returnsTrue() {
        val sa1 =
            SparseArray<Bundle>().apply {
                put(1, Bundle().apply { putString("k", "v") })
                put(10, Bundle().apply { putInt("num", 42) })
            }
        val sa2 =
            SparseArray<Bundle>().apply {
                put(1, Bundle().apply { putString("k", "v") })
                put(10, Bundle().apply { putInt("num", 42) })
            }
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
    }

    @Test
    fun areBundlesEqual_emptySparseArrays_returnsTrue() {
        val sa1 = SparseArray<Bundle>()
        val sa2 = SparseArray<Bundle>()
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
    }

    @Test
    fun areBundlesEqual_sparseArraysDifferentValues_returnsFalse() {
        val sa1 = SparseArray<Bundle>().apply { put(1, Bundle().apply { putString("k", "v1") }) }
        val sa2 = SparseArray<Bundle>().apply { put(1, Bundle().apply { putString("k", "v2") }) }
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isFalse()
        assertThat(BundleUtils.bundleHashCode(b1)).isNotEqualTo(BundleUtils.bundleHashCode(b2))
    }

    @Test
    fun areBundlesEqual_sparseArraysDifferentKeys_returnsFalse() {
        val sa1 = SparseArray<Bundle>().apply { put(1, Bundle().apply { putString("k", "v") }) }
        val sa2 = SparseArray<Bundle>().apply { put(2, Bundle().apply { putString("k", "v") }) }
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isFalse()
    }

    @Test
    fun areBundlesEqual_sparseArraysDifferentSize_returnsFalse() {
        val sa1 =
            SparseArray<Bundle>().apply {
                put(1, Bundle().apply { putString("k", "v") })
                put(2, Bundle())
            }
        val sa2 = SparseArray<Bundle>().apply { put(1, Bundle().apply { putString("k", "v") }) }
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isFalse()
    }

    @Test
    fun areBundlesEqual_sparseArraysNestedSparseArrays_worksCorrectly() {
        val innerSparse1 =
            SparseArray<Bundle>().apply { put(5, Bundle().apply { putString("a", "b") }) }
        val innerSparse2 =
            SparseArray<Bundle>().apply { put(5, Bundle().apply { putString("a", "b") }) }
        val innerSparseDiff =
            SparseArray<Bundle>().apply { put(5, Bundle().apply { putString("a", "c") }) }

        val innerBundle1 = Bundle().apply { putSparseParcelableArray("nested", innerSparse1) }
        val innerBundle2 = Bundle().apply { putSparseParcelableArray("nested", innerSparse2) }
        val innerBundleDiff = Bundle().apply { putSparseParcelableArray("nested", innerSparseDiff) }

        val sa1 = SparseArray<Bundle>().apply { put(1, innerBundle1) }
        val sa2 = SparseArray<Bundle>().apply { put(1, innerBundle2) }
        val saDiff = SparseArray<Bundle>().apply { put(1, innerBundleDiff) }

        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }
        val bDiff = Bundle().apply { putSparseParcelableArray("sparse", saDiff) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
        assertThat(BundleUtils.areBundlesEqual(b1, bDiff)).isFalse()
    }

    @Test
    fun areBundlesEqual_sparseArrayInsertionOrderIndependent() {
        val sa1 =
            SparseArray<Bundle>().apply {
                put(10, Bundle().apply { putString("a", "b") })
                put(1, Bundle().apply { putString("c", "d") })
            }
        val sa2 =
            SparseArray<Bundle>().apply {
                put(1, Bundle().apply { putString("c", "d") })
                put(10, Bundle().apply { putString("a", "b") })
            }
        val b1 = Bundle().apply { putSparseParcelableArray("sparse", sa1) }
        val b2 = Bundle().apply { putSparseParcelableArray("sparse", sa2) }

        assertThat(BundleUtils.areBundlesEqual(b1, b2)).isTrue()
        assertThat(BundleUtils.bundleHashCode(b1)).isEqualTo(BundleUtils.bundleHashCode(b2))
    }

    private fun createSamplePrimitiveValueBundle(): Bundle =
        Bundle().apply {
            putString("str", "hello")
            putInt("int", 42)
            putBoolean("bool", true)
            putFloat("float", 1.5f)
            putDouble("double", 2.5)
            putLong("long", 100L)
            putByte("byte", 8.toByte())
            putShort("short", 16.toShort())
            putChar("char", 'c')
        }

    private fun createSamplePrimitiveArrayBundle(): Bundle =
        Bundle().apply {
            putByteArray("byteArr", byteArrayOf(1, 2))
            putShortArray("shortArr", shortArrayOf(3, 4))
            putCharArray("charArr", charArrayOf('a', 'b'))
            putIntArray("intArr", intArrayOf(5, 6))
            putLongArray("longArr", longArrayOf(7L, 8L))
            putFloatArray("floatArr", floatArrayOf(1.0f, 2.0f))
            putDoubleArray("doubleArr", doubleArrayOf(3.0, 4.0))
            putBooleanArray("boolArr", booleanArrayOf(true, false))
        }
}
