/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.provider

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.service.credentials.BeginGetCredentialOption
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi

/**
 * Custom version of [BeginGetCredentialOption] for custom credential types.
 *
 * Providers must use [BeginGetCredentialOption.getType] to determine the credential type
 * of this custom option parameters, in order to populate a list of [CustomCredentialEntry] to be
 * set on the [android.service.credentials.BeginGetCredentialResponse].
 */
@RequiresApi(34)
class BeginGetCustomCredentialOption internal constructor(
    type: String,
    candidateQueryData: Bundle,
) : BeginGetCredentialOption(
    type,
    candidateQueryData
) {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }
    @Suppress("AcronymName")
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<BeginGetCustomCredentialOption> = object :
            Parcelable.Creator<BeginGetCustomCredentialOption> {
            override fun createFromParcel(p0: Parcel?): BeginGetCustomCredentialOption {
                val baseOption = BeginGetCredentialOption.CREATOR.createFromParcel(p0)
                return BeginGetCustomCredentialOption(baseOption.type,
                    baseOption.candidateQueryData)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<BeginGetCustomCredentialOption?> {
                return arrayOfNulls(size)
            }
        }
    }
}