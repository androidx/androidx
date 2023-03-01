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
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import java.util.Collections

/**
 * An entry on the selector, denoting that the credential request will be completed on a remote
 * device.
 *
 * Once this entry is selected, the corresponding [pendingIntent] will be invoked. The provider
 * can then show any activity they wish to. Before finishing the activity, provider must
 * set the final [androidx.credentials.GetCredentialResponse] through the
 * [PendingIntentHandler.setGetCredentialResponse] helper API, or a
 * [androidx.credentials.CreateCredentialResponse] through the
 * [PendingIntentHandler.setCreateCredentialResponse] helper API depending on whether it is a get
 * or create flow.
 *
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects
 * this entry
 *
 * See [android.service.credentials.BeginGetCredentialResponse] for usage details.
 */
@RequiresApi(34)
class RemoteEntry constructor(
    val pendingIntent: PendingIntent
    ) : android.service.credentials.RemoteEntry(
    toSlice(pendingIntent)
    ) {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }
    @Suppress("AcronymName")
    companion object {
        private const val TAG = "RemoteEntry"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_PENDING_INTENT"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_OPTION_ID"

        /** @hide */
        @JvmStatic
        internal fun toSlice(
            pendingIntent: PendingIntent
        ): Slice {
            // TODO("Put the right spec and version value")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
            sliceBuilder.addAction(pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(), /*subType=*/null)
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [RemoteEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         *
         * @hide
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): RemoteEntry? {
            var pendingIntent: PendingIntent? = null
            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                }
            }
            return try {
                RemoteEntry(pendingIntent!!)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        @JvmField val CREATOR: Parcelable.Creator<RemoteEntry> = object :
            Parcelable.Creator<RemoteEntry> {
            override fun createFromParcel(p0: Parcel?): RemoteEntry? {
                val baseEntry =
                    android.service.credentials.CredentialEntry.CREATOR.createFromParcel(p0)
                return fromSlice(baseEntry.slice)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<RemoteEntry?> {
                return arrayOfNulls(size)
            }
        }
    }
}