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

package androidx.ui.core

import android.annotation.SuppressLint
import android.os.Binder
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.SpannableString
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.Serializable

@SmallTest
@RunWith(JUnit4::class)
class SavedStateDelegateTest {

    private val ContainerKey = 100
    private val SaveKey = "key"
    private val SaveValue = 5

    @Test
    fun simpleSaveAndRestore() {
        val delegateToSave = SavedStateDelegate { ContainerKey }
        delegateToSave.stopWaitingForStateRestoration()
        delegateToSave.savedStateRegistry!!.registerProvider(SaveKey) { SaveValue }
        val stateArray = SparseArray<Parcelable>()
        delegateToSave.dispatchSaveInstanceState(stateArray, Bundle())

        val delegateToRestore = SavedStateDelegate { ContainerKey }
        delegateToRestore.dispatchRestoreInstanceState(stateArray)
        val restoredValue = delegateToRestore.savedStateRegistry!!.consumeRestored(SaveKey)
        assertEquals(SaveValue, restoredValue)
    }

    @Test
    fun saveAndRestoreWhenTwoParentsShareTheSameStateArray() {
        // This emulates two different AndroidComposeViews used inside the same Activity
        val parentKey1 = 1
        val value1 = 1
        val parentKey2 = 2
        val value2 = 2
        val stateArray = SparseArray<Parcelable>()

        // save first view
        val delegateToSave1 = SavedStateDelegate { parentKey1 }
        delegateToSave1.stopWaitingForStateRestoration()
        delegateToSave1.savedStateRegistry!!.registerProvider(SaveKey) { value1 }
        delegateToSave1.dispatchSaveInstanceState(stateArray, Bundle())

        // save second view
        val delegateToSave2 = SavedStateDelegate { parentKey2 }
        delegateToSave2.stopWaitingForStateRestoration()
        delegateToSave2.savedStateRegistry!!.registerProvider(SaveKey) { value2 }
        delegateToSave2.dispatchSaveInstanceState(stateArray, Bundle())

        // restore first view
        val delegateToRestore1 = SavedStateDelegate { parentKey1 }
        delegateToRestore1.dispatchRestoreInstanceState(stateArray)
        val restoredValue1 = delegateToRestore1.savedStateRegistry!!.consumeRestored(SaveKey)
        assertEquals(value1, restoredValue1)

        // restore second view
        val delegateToRestore2 = SavedStateDelegate { parentKey2 }
        delegateToRestore2.dispatchRestoreInstanceState(stateArray)
        val restoredValue2 = delegateToRestore2.savedStateRegistry!!.consumeRestored(SaveKey)
        assertEquals(value2, restoredValue2)
    }

    @Test
    fun onRegistryReadyCalledAfterStopWaitingForStateRestoration() {
        var called = false
        val delegate = SavedStateDelegate { ContainerKey }
        delegate.setOnSaveRegistryAvailable { called = true }

        delegate.stopWaitingForStateRestoration()
        assertTrue(called)
    }

    @Test
    fun onRegistryReadyCalledAfterDispatchRestoreInstanceState() {
        var called = false
        val delegate = SavedStateDelegate { ContainerKey }
        delegate.setOnSaveRegistryAvailable { called = true }

        delegate.dispatchRestoreInstanceState(SparseArray())
        assertTrue(called)
    }

    @Test
    fun typesSupportedByBaseBundleCanBeSaved() {
        val delegate = SavedStateDelegate { ContainerKey }
        delegate.stopWaitingForStateRestoration()
        val registry = delegate.savedStateRegistry!!

        assertTrue(registry.canBeSaved(true))
        assertTrue(registry.canBeSaved(true.asBoxed()))
        assertTrue(registry.canBeSaved(true.asBoxed()))
        assertTrue(registry.canBeSaved(booleanArrayOf(true)))
        assertTrue(registry.canBeSaved(5.toDouble()))
        assertTrue(registry.canBeSaved(5.toDouble().asBoxed()))
        assertTrue(registry.canBeSaved(doubleArrayOf(5.toDouble())))
        assertTrue(registry.canBeSaved(5.toLong()))
        assertTrue(registry.canBeSaved(5.toLong().asBoxed()))
        assertTrue(registry.canBeSaved(longArrayOf(5.toLong())))
        assertTrue(registry.canBeSaved("string"))
        assertTrue(registry.canBeSaved(arrayOf("string")))
    }

    @Test
    fun typesSupportedByBundleCanBeSaved() {
        val delegate = SavedStateDelegate { ContainerKey }
        delegate.stopWaitingForStateRestoration()
        val registry = delegate.savedStateRegistry!!

        assertTrue(registry.canBeSaved(Binder()))
        assertTrue(registry.canBeSaved(Bundle()))
        assertTrue(registry.canBeSaved(5.toByte()))
        assertTrue(registry.canBeSaved(5.toByte().asBoxed()))
        assertTrue(registry.canBeSaved(byteArrayOf(5.toByte())))
        assertTrue(registry.canBeSaved(5.toChar()))
        assertTrue(registry.canBeSaved(5.toChar().asBoxed()))
        assertTrue(registry.canBeSaved(charArrayOf(5.toChar())))
        assertTrue(registry.canBeSaved(SpannableString("CharSequence")))
        assertTrue(registry.canBeSaved(arrayOf(SpannableString("CharSequence"))))
        assertTrue(registry.canBeSaved(5.toFloat()))
        assertTrue(registry.canBeSaved(5.toFloat().asBoxed()))
        assertTrue(registry.canBeSaved(floatArrayOf(5.toFloat())))
        assertTrue(registry.canBeSaved(arrayListOf(5)))
        assertTrue(registry.canBeSaved(CustomParcelable()))
        assertTrue(registry.canBeSaved(arrayOf(CustomParcelable())))
        assertTrue(registry.canBeSaved(arrayListOf(CustomParcelable())))
        assertTrue(registry.canBeSaved(CustomSerializable()))
        assertTrue(registry.canBeSaved(5.toShort()))
        assertTrue(registry.canBeSaved(Size(5, 5)))
        assertTrue(registry.canBeSaved(SizeF(5f, 5f)))
        assertTrue(registry.canBeSaved(SparseArray<Parcelable>().apply {
            put(5, CustomParcelable())
        }))
        assertTrue(registry.canBeSaved(arrayListOf("String")))
    }

    @Test
    fun customTypeCantBeSaved() {
        val delegate = SavedStateDelegate { ContainerKey }
        delegate.stopWaitingForStateRestoration()
        val registry = delegate.savedStateRegistry!!

        assertFalse(registry.canBeSaved(CustomClass()))
    }

    private fun Any?.asBoxed(): Any = this!!
}

private class CustomClass

private class CustomSerializable : Serializable

@SuppressLint("BanParcelableUsage")
private class CustomParcelable(parcel: Parcel? = null) : Parcelable {
    init {
        parcel?.readBoolean()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBoolean(true)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<CustomParcelable> {
                override fun createFromParcel(parcel: Parcel) = CustomParcelable(parcel)
                override fun newArray(size: Int) = arrayOfNulls<CustomParcelable?>(size)
            }
    }
}
