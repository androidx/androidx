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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.PublicKeyCredential
import java.util.Collections

/**
 * An entry on the selector, denoting that the credential will be retrieved from a remote device.
 * A public key credential entry that is displayed on the account selector UI.
 *
 * Once this entry is selected, the corresponding [pendingIntent] will be invoked. The provider
 * can then show any activity they wish to. Before finishing the activity, provider must
 * set the final [androidx.credentials.GetCredentialResponse] through the
 * [PendingIntentHandler.setGetCredentialResponse] helper API.
 *
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects
 * this entry
 *
 * See [android.service.credentials.BeginGetCredentialResponse] for usage details.
 */
@RequiresApi(34)
class RemoteCredentialEntry constructor(
    val pendingIntent: PendingIntent,
    beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption
    ) : android.service.credentials.CredentialEntry(
    beginGetPublicKeyCredentialOption,
    toSlice(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL, pendingIntent,
    beginGetPublicKeyCredentialOption)
    ) {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }
    @Suppress("AcronymName")
    companion object CREATOR {
        private const val TAG = "RemoteEntry"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_PENDING_INTENT"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_OPTION_BUNDLE =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_OPTION_BUNDLE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_OPTION_ID"

        /** @hide */
        @JvmStatic
        internal fun toSlice(
            type: String,
            pendingIntent: PendingIntent,
            beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption
        ): Slice {
            // TODO("Put the right spec and version value")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(type, 1))
            sliceBuilder.addAction(pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(), /*subType=*/null)
                .addBundle(beginGetPublicKeyCredentialOption.candidateQueryData,
                    /*subType=*/null,
                    listOf(SLICE_HINT_OPTION_BUNDLE))
                .addText(beginGetPublicKeyCredentialOption.id,
                    /*subType=*/null,
                    listOf(SLICE_HINT_OPTION_ID))
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [RemoteCredentialEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         *
         * @hide
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): RemoteCredentialEntry? {
            var beginGetPublicKeyCredentialOptionBundle: Bundle? = null
            var beginGetPublicKeyCredentialOptionId: CharSequence? = null
            var pendingIntent: PendingIntent? = null
            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_OPTION_BUNDLE)) {
                    beginGetPublicKeyCredentialOptionBundle = it.bundle
                } else if (it.hasHint(SLICE_HINT_OPTION_ID)) {
                    beginGetPublicKeyCredentialOptionId = it.text
                }
            }
            return try {
                RemoteCredentialEntry(pendingIntent!!,
                    BeginGetPublicKeyCredentialOption.createFrom(
                        beginGetPublicKeyCredentialOptionBundle!!,
                        beginGetPublicKeyCredentialOptionId!!.toString()
                    )
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        @JvmField val CREATOR: Parcelable.Creator<RemoteCredentialEntry> = object :
            Parcelable.Creator<RemoteCredentialEntry> {
            override fun createFromParcel(p0: Parcel?): RemoteCredentialEntry? {
                val baseEntry =
                    android.service.credentials.CredentialEntry.CREATOR.createFromParcel(p0)
                return fromSlice(baseEntry.slice)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<RemoteCredentialEntry?> {
                return arrayOfNulls(size)
            }
        }
    }
}