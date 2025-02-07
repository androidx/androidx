/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy.layout

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Provides all the needed info about the items which could be later composed and displayed as
 * children or [LazyLayout].
 *
 * Note: this interface is a part of [LazyLayout] harness that allows for building custom lazy
 * layouts. LazyLayout and all corresponding APIs are still under development and are subject to
 * change.
 */
@Stable
internal interface LazyLayoutItemProvider {

    /** The total number of items in the lazy layout (visible or not). */
    val itemCount: Int

    /** The item for the given [index] and [key]. */
    @Composable fun Item(index: Int, key: Any)

    /**
     * Returns the content type for the item on this index. It is used to improve the item
     * compositions reusing efficiency. Note that null is a valid type and items of such type will
     * be considered compatible.
     */
    fun getContentType(index: Int): Any? = null

    /**
     * Returns the key for the item on this index.
     *
     * @see getDefaultLazyLayoutKey which you can use if the user didn't provide a key.
     */
    fun getKey(index: Int): Any = getDefaultLazyLayoutKey(index)

    /**
     * Get index for given key. The index is not guaranteed to be known for all keys in layout for
     * optimization purposes, but must be present for elements in current viewport. If the key is
     * not present in the layout or near current viewport, return -1.
     */
    fun getIndex(key: Any): Int = -1
}

/**
 * Finds a position of the item with the given key in the lists. This logic allows us to detect when
 * there were items added or removed before our current first item.
 */
internal fun LazyLayoutItemProvider.findIndexByKey(
    key: Any?,
    lastKnownIndex: Int,
): Int {
    if (key == null || itemCount == 0) {
        // there were no real item during the previous measure
        return lastKnownIndex
    }
    if (lastKnownIndex < itemCount && key == getKey(lastKnownIndex)) {
        // this item is still at the same index
        return lastKnownIndex
    }
    val newIndex = getIndex(key)
    if (newIndex != -1) {
        return newIndex
    }
    // fallback to the previous index if we don't know the new index of the item
    return lastKnownIndex
}

internal fun getDefaultLazyLayoutKey(index: Int): Any = DefaultLazyKey(index)

@SuppressLint("BanParcelableUsage")
private data class DefaultLazyKey(private val index: Int) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<DefaultLazyKey> =
            object : Parcelable.Creator<DefaultLazyKey> {
                override fun createFromParcel(parcel: Parcel) = DefaultLazyKey(parcel.readInt())

                override fun newArray(size: Int) = arrayOfNulls<DefaultLazyKey?>(size)
            }
    }
}
