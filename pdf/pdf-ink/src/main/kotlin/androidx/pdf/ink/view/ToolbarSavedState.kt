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

package androidx.pdf.ink.view

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState
import androidx.core.os.ParcelCompat
import androidx.customview.view.AbsSavedState
import androidx.pdf.ink.view.state.AnnotationToolbarState

/** A custom SavedState class to store the Parcelable AnnotationToolbarState. */
internal class ToolbarSavedState : AbsSavedState {
    var toolbarState: AnnotationToolbarState? = null

    constructor(superState: Parcelable?) : super(superState ?: BaseSavedState.EMPTY_STATE)

    constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
        toolbarState =
            ParcelCompat.readParcelable(source, loader, AnnotationToolbarState::class.java)
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(toolbarState, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.ClassLoaderCreator<ToolbarSavedState> =
            object : Parcelable.ClassLoaderCreator<ToolbarSavedState> {
                override fun createFromParcel(
                    source: Parcel,
                    loader: ClassLoader?,
                ): ToolbarSavedState {
                    return ToolbarSavedState(source, loader)
                }

                override fun createFromParcel(source: Parcel): ToolbarSavedState {
                    return ToolbarSavedState(source, null)
                }

                override fun newArray(size: Int): Array<ToolbarSavedState?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
