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

package androidx.wear.watchface.complications.data

import android.os.Parcel
import android.os.Parcelable
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth

internal class ParcelableSubject(metadata: FailureMetadata?, private val subject: Parcelable?) :
    Subject(metadata, subject) {

    private class ParcelableSubjectFactory :
        Factory<ParcelableSubject, Parcelable> {
        override fun createSubject(
            metadata: FailureMetadata?,
            subject: Parcelable?
        ) = ParcelableSubject(metadata, subject)
    }

    fun hasSameSerializationAs(parcelable: Parcelable) {
        check("hasSameSerializationAs()").that(subject).isNotNull()
        check("hasSameSerializationAs()").that(parcelable).isNotNull()
        check("hasSameSerializationAs()").that(serializeParcelable(subject!!))
            .isEqualTo(serializeParcelable(parcelable))
    }

    fun hasDifferentSerializationAs(parcelable: Parcelable) {
        check("hasDifferentSerializationAs()").that(subject).isNotNull()
        check("hasDifferentSerializationAs()").that(parcelable).isNotNull()
        check("hasDifferentSerializationAs()").that(serializeParcelable(subject!!))
            .isNotEqualTo(serializeParcelable(parcelable))
    }

    private fun serializeParcelable(parcelable: Parcelable) =
        Parcel.obtain().apply {
            parcelable.writeToParcel(this, 0)
        }.marshall()

    internal companion object {
        @JvmStatic
        fun assertThat(parcelable: Parcelable): ParcelableSubject {
            return Truth.assertAbout(FACTORY).that(parcelable)
        }

        @JvmField
        val FACTORY: Factory<ParcelableSubject, Parcelable> =
            ParcelableSubjectFactory()
    }
}
