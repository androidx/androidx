/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.os

import android.content.Context
import android.graphics.Rect
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Size
import android.util.SizeF
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@SmallTest
class BundleTest {
    @Suppress("DEPRECATION")
    @Test fun bundleOfValid() {
        val bundleValue = Bundle()
        val charSequenceValue = "hey"
        val parcelableValue = Rect(1, 2, 3, 4)
        val serializableValue = AtomicInteger(1)
        val binderValue = object : IBinder by Binder() {}

        val bundle = bundleOf(
            "null" to null,

            "boolean" to true,
            "byte" to 1.toByte(),
            "char" to 'a',
            "double" to 1.0,
            "float" to 1f,
            "int" to 1,
            "long" to 1L,
            "short" to 1.toShort(),

            "bundle" to bundleValue,
            "charSequence" to charSequenceValue,
            "parcelable" to parcelableValue,
            "binder" to binderValue,

            "booleanArray" to booleanArrayOf(),
            "byteArray" to byteArrayOf(),
            "charArray" to charArrayOf(),
            "doubleArray" to doubleArrayOf(),
            "floatArray" to floatArrayOf(),
            "intArray" to intArrayOf(),
            "longArray" to longArrayOf(),
            "shortArray" to shortArrayOf(),

            "parcelableArray" to arrayOf(parcelableValue),
            "stringArray" to arrayOf("hey"),
            "charSequenceArray" to arrayOf<CharSequence>("hey"),
            "serializableArray" to arrayOf(serializableValue),

            "serializable" to serializableValue
        )

        assertEquals(26, bundle.size())

        assertNull(bundle["null"])

        assertEquals(true, bundle["boolean"])
        assertEquals(1.toByte(), bundle["byte"])
        assertEquals('a', bundle["char"])
        assertEquals(1.0, bundle["double"])
        assertEquals(1f, bundle["float"])
        assertEquals(1, bundle["int"])
        assertEquals(1L, bundle["long"])
        assertEquals(1.toShort(), bundle["short"])

        assertSame(bundleValue, bundle["bundle"])
        assertSame(charSequenceValue, bundle["charSequence"])
        assertSame(parcelableValue, bundle["parcelable"])
        assertSame(binderValue, bundle["binder"])

        assertArrayEquals(booleanArrayOf(), bundle["booleanArray"] as BooleanArray)
        assertArrayEquals(byteArrayOf(), bundle["byteArray"] as ByteArray)
        assertArrayEquals(charArrayOf(), bundle["charArray"] as CharArray)
        assertArrayEquals(doubleArrayOf(), bundle["doubleArray"] as DoubleArray, 0.0)
        assertArrayEquals(floatArrayOf(), bundle["floatArray"] as FloatArray, 0f)
        assertArrayEquals(intArrayOf(), bundle["intArray"] as IntArray)
        assertArrayEquals(longArrayOf(), bundle["longArray"] as LongArray)
        assertArrayEquals(shortArrayOf(), bundle["shortArray"] as ShortArray)

        assertThat(bundle["parcelableArray"] as Array<*>).asList().containsExactly(parcelableValue)
        assertThat(bundle["stringArray"] as Array<*>).asList().containsExactly("hey")
        assertThat(bundle["charSequenceArray"] as Array<*>).asList().containsExactly("hey")
        assertThat(bundle["serializableArray"] as Array<*>).asList()
            .containsExactly(serializableValue)

        assertSame(serializableValue, bundle["serializable"])
    }

    @SdkSuppress(minSdkVersion = 21)
    @Suppress("DEPRECATION")
    @Test fun bundleOfValidApi21() {
        val sizeValue = Size(1, 1)
        val sizeFValue = SizeF(1f, 1f)

        val bundle = bundleOf(
            "size" to sizeValue,
            "sizeF" to sizeFValue
        )

        assertSame(sizeValue, bundle["size"])
        assertSame(sizeFValue, bundle["sizeF"])
    }

    @Test fun bundleOfInvalid() {
        assertThrows<IllegalArgumentException> {
            bundleOf("nope" to View(ApplicationProvider.getApplicationContext() as Context))
        }.hasMessageThat().isEqualTo("Illegal value type android.view.View for key \"nope\"")

        assertThrows<IllegalArgumentException> {
            bundleOf(
                "nopes" to arrayOf(View(ApplicationProvider.getApplicationContext() as Context))
            )
        }.hasMessageThat().isEqualTo("Illegal value array type android.view.View for key \"nopes\"")
    }

    @Test fun bundleOfEmpty() {
        assertEquals(0, bundleOf().size())
    }
}
