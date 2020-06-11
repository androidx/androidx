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
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import androidx.annotation.RequiresApi
import androidx.ui.savedinstancestate.UiSavedStateRegistry
import java.io.Serializable

/**
 * Creates [savedStateRegistry] with the restored values when the restored state is ready and
 * saves the values when the View requests.
 *
 * We can't use the default way of doing it when we have unique id on [AndroidComposeView] and
 * react on [View.onSaveInstanceState] and [View.onRestoreInstanceState] because we dynamically
 * create [AndroidComposeView]s and there is no way to have a unique id given there are could be
 * any number of [AndroidComposeView]s inside the same Activity. If we use [View.generateViewId]
 * this id will not survive Activity recreation.
 * But it is reasonable to ask our users to have an unique id on the parent ViewGroup in which we
 * compose our [AndroidComposeView]. If Activity.setContent is used then it will be a View with
 * [android.R.id.content], if ViewGroup.setContent is used then we will ask users to provide an
 * id for this ViewGroup. If @GenerateView will be used then we will ask users to set an id on
 * this generated View.
 * Then how exactly we will use the parent id? We can't just set the same id for our
 * [AndroidComposeView] as the id should be unique. It is also not safe to generate an id based
 * on the parent id via some tricky rule as this generated id can potentially collide with some
 * other id used in the same Activity.
 * But instead, if we use [View.dispatchSaveInstanceState] and [View.dispatchRestoreInstanceState]
 * we have an access for the global map of id to state and we can use here some id which is not id
 * of our [AndroidComposeView], but our global and unique [StatesMapId] and our custom
 * [SparseArray] as a value where keys are our parent ids and values our real state for this
 * [AndroidComposeView]. It allows two different [AndroidComposeView] to reuse the same
 * [SparseArray] stored with [StatesMapId], but both of them store their states by their own key -
 * parent id.
 */
internal class SavedStateDelegate(val parentIdProvider: () -> Int) {

    private var onRegistryAvailable: ((UiSavedStateRegistry) -> Unit)? = null

    /**
     * The current instance of [UiSavedStateRegistry]. If it's null you can wait for it to became
     * available using [setOnSaveRegistryAvailable].
     */
    var savedStateRegistry: UiSavedStateRegistry? = null
        private set

    /**
     * We usually wait for [dispatchRestoreInstanceState] callback to restore the state,
     * but when there is nothing to restore this callback will not be called.
     * It will be called when there is no need to wait for [dispatchRestoreInstanceState]
     * anymore and we can create an empty [UiSavedStateRegistry].
     */
    fun stopWaitingForStateRestoration() {
        if (savedStateRegistry == null) {
            savedStateRegistry = UiSavedStateRegistry(null) { canBeSavedToBundle(it) }
        }
        val callback = onRegistryAvailable
        if (callback != null) {
            onRegistryAvailable = null
            callback(savedStateRegistry!!)
        }
    }

    /**
     * Allows other components to be notified when the [UiSavedStateRegistry] became available.
     */
    fun setOnSaveRegistryAvailable(callback: (UiSavedStateRegistry) -> Unit) {
        check(savedStateRegistry == null)
        onRegistryAvailable = callback
    }

    /**
     * To be called by the [AndroidComposeView] in the corresponding callback.
     */
    fun dispatchSaveInstanceState(
        array: SparseArray<Parcelable>,
        superState: Parcelable
    ) {
        val containerId = parentIdProvider()
        if (containerId != View.NO_ID) {
            val arrayHolder = array.get(StatesMapId) as? ParcelableSparseArrayHolder
                ?: ParcelableSparseArrayHolder().also { array.put(StatesMapId, it) }
            val ourState = ComposeViewSavedState(superState, savedStateRegistry?.performSave())
            arrayHolder.array.put(containerId, ourState)
        }
    }

    /**
     * To be called by the [AndroidComposeView] in the corresponding callback.
     *
     * @return super state to be passed into super.onRestoreInstanceState()
     */
    fun dispatchRestoreInstanceState(array: SparseArray<Parcelable>): Parcelable? {
        var restoredState: ComposeViewSavedState? = null

        val containerId = parentIdProvider()
        if (containerId != View.NO_ID) {
            val arrayHolder = array.get(StatesMapId) as? ParcelableSparseArrayHolder
            if (arrayHolder != null) {
                restoredState = arrayHolder.array.get(containerId) as? ComposeViewSavedState
            }
        }

        if (savedStateRegistry == null) {
            savedStateRegistry = UiSavedStateRegistry(restoredState?.map) { canBeSavedToBundle(it) }
            stopWaitingForStateRestoration()
        }

        return restoredState?.superState
    }
}

/**
 * Unique id used by [SavedStateDelegate]s of all [AndroidComposeView]s within the same Activity.
 */
private val StatesMapId = R.id.compose_saved_states_map

/**
 * Checks that [value] can be stored inside [Bundle].
 */
private fun canBeSavedToBundle(value: Any): Boolean {
    for (cl in AcceptableClasses) {
        if (cl.isInstance(value)) {
            return true
        }
    }
    return false
}

/**
 * Contains Classes which can be stored inside [Bundle].
 *
 * Some of the classes are not added separately because:
 *
 * This classes implement Serializable:
 * - Arrays (DoubleArray, BooleanArray, IntArray, LongArray, ByteArray, FloatArray, ShortArray,
 * CharArray, Array<Parcelable, Array<String>)
 * - ArrayList
 * - Primitives (Boolean, Int, Long, Double, Float, Byte, Short, Char) will be boxed when casted
 * to Any, and all the boxed classes implements Serializable.
 * This class implements Parcelable:
 * - Bundle
 *
 * Note: it is simplified copy of the array from SavedStateHandle (lifecycle-viewmodel-savedstate).
 */
private val AcceptableClasses = arrayOf(
    Serializable::class.java,
    Parcelable::class.java,
    CharSequence::class.java,
    SparseArray::class.java,
    Binder::class.java,
    Size::class.java,
    SizeF::class.java
)

private class ComposeViewSavedState : View.BaseSavedState {

    val map: Map<String, Any>

    constructor(superState: Parcelable, map: Map<String, Any>?) : super(superState) {
        this.map = map ?: emptyMap()
    }

    private constructor(parcel: Parcel) : super(parcel) {
        map = readMap(parcel, null)
    }

    @RequiresApi(24)
    private constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
        map = readMap(parcel, loader)
    }

    private fun readMap(parcel: Parcel, loader: ClassLoader?): Map<String, Any> {
        val map = hashMapOf<String, Any>()
        val array: Array<Any>? = parcel.readArray(loader ?: javaClass.classLoader)
        if (array != null) {
            check(array.size.rem(2) == 0)
            var index = 0
            while (index < array.size) {
                val key = array[index] as String
                val value = array[index + 1]
                map[key] = value
                index += 2
            }
        }
        return map
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        val array = arrayOfNulls<Any?>(map.size * 2)
        var index = 0
        map.forEach {
            array[index] = it.key
            array[index + 1] = it.value
            index += 2
        }
        out.writeArray(array)
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ComposeViewSavedState> =
            object : Parcelable.ClassLoaderCreator<ComposeViewSavedState> {
                override fun createFromParcel(parcel: Parcel, loader: ClassLoader) =
                    if (Build.VERSION.SDK_INT >= 24) {
                        ComposeViewSavedState(parcel, loader)
                    } else {
                        ComposeViewSavedState(parcel)
                    }

                override fun createFromParcel(parcel: Parcel) = ComposeViewSavedState(parcel)

                override fun newArray(size: Int) = arrayOfNulls<ComposeViewSavedState?>(size)
            }
    }
}

@SuppressLint("BanParcelableUsage")
private class ParcelableSparseArrayHolder : Parcelable {

    val array: SparseArray<Parcelable>

    constructor() {
        array = SparseArray()
    }

    private constructor(parcel: Parcel, loader: ClassLoader?) {
        array = parcel.readSparseArray<Parcelable>(loader ?: javaClass.classLoader)!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSparseArray(array)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableSparseArrayHolder> =
            object : Parcelable.ClassLoaderCreator<ParcelableSparseArrayHolder> {
                override fun createFromParcel(parcel: Parcel, loader: ClassLoader) =
                    ParcelableSparseArrayHolder(parcel, loader)

                override fun createFromParcel(parcel: Parcel) =
                    ParcelableSparseArrayHolder(parcel, null)

                override fun newArray(size: Int) = arrayOfNulls<ParcelableSparseArrayHolder?>(size)
            }
    }
}
