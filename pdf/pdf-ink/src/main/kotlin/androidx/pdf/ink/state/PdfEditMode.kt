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

package androidx.pdf.ink.state

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef

/** Represents the current editing state of the PDF document. */
@SuppressLint("BanParcelableUsage")
internal sealed interface PdfEditMode : Parcelable {

    /** Edit mode is disabled; the user is in viewing mode. */
    object Disabled : PdfEditMode {
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(EDITING_DISABLED)
        }

        override fun describeContents(): Int = 0

        @JvmField
        val CREATOR =
            object : Parcelable.Creator<Disabled> {
                override fun createFromParcel(parcel: Parcel) = Disabled

                override fun newArray(size: Int) = arrayOfNulls<Disabled>(size)
            }
    }

    /**
     * Edit mode is enabled for a specific [journey].
     *
     * @property journey The current editing journey. Defaults to [EDITING_JOURNEY_ANNOTATIONS]
     */
    data class Enabled(@EditingJourney val journey: Int = EDITING_JOURNEY_ANNOTATIONS) :
        PdfEditMode {
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(EDITING_ENABLED)
            parcel.writeInt(journey)
        }

        override fun describeContents(): Int = 0

        @JvmField
        val CREATOR =
            object : Parcelable.Creator<Enabled> {
                override fun createFromParcel(parcel: Parcel) = Enabled(parcel.readInt())

                override fun newArray(size: Int) = arrayOfNulls<Enabled>(size)
            }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDITING_JOURNEY_ANNOTATIONS, EDITING_JOURNEY_FORM_FILLING)
    annotation class EditingJourney

    companion object {
        private const val EDITING_DISABLED: Int = 0
        private const val EDITING_ENABLED: Int = 1
        internal const val EDITING_JOURNEY_ANNOTATIONS: Int = 0
        internal const val EDITING_JOURNEY_FORM_FILLING: Int = 1

        val CREATOR =
            object : Parcelable.Creator<PdfEditMode> {
                override fun createFromParcel(parcel: Parcel): PdfEditMode {
                    return when (parcel.readInt()) {
                        EDITING_ENABLED -> Enabled(parcel.readInt())
                        else -> Disabled
                    }
                }

                override fun newArray(size: Int) = arrayOfNulls<PdfEditMode>(size)
            }
    }
}
