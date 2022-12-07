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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.Action.Companion.toSlice
import java.util.Collections

/**
 * An entry on the selector, denoting that the credential will be retrieved from a remote device.
 *
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects
 * this entry
 *
 * See [CredentialsResponseContent] for usage details.
 *
 * @hide
 */
@RequiresApi(34)
class RemoteEntry constructor(
    // TODO("Add a PublicKeyRemoteEntry as a derived class and set the type there")
    val type: String = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    val pendingIntent: PendingIntent,
    ) {
    companion object {
        private const val TAG = "RemoteEntry"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TYPE =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_TYPE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_PENDING_INTENT"
        @JvmStatic
        fun toSlice(remoteEntry: RemoteEntry): Slice {
            // TODO("Put the right spec and version value")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(remoteEntry.type, 1))
            sliceBuilder.addAction(remoteEntry.pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/null)
            return sliceBuilder.build()
        }

        @JvmStatic
        internal fun toFrameworkCredentialEntryClass(remoteEntry: RemoteEntry):
            android.service.credentials.CredentialEntry {
            return android.service.credentials.CredentialEntry.Builder(
                remoteEntry.type,
                toSlice(remoteEntry),
                remoteEntry.pendingIntent).build()
        }

        @JvmStatic
        internal fun toFrameworkCreateEntryClass(remoteEntry: RemoteEntry):
            android.service.credentials.CreateEntry {
            return android.service.credentials.CreateEntry(
                toSlice(remoteEntry),
                remoteEntry.pendingIntent)
        }

        /**
         * Returns an instance of [RemoteEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): RemoteEntry? {
            val type = slice.spec!!.type
            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    return try {
                        RemoteEntry(type, it.action)
                    } catch (e: Exception) {
                        Log.i(TAG, "fromSlice failed with: " + e.message)
                        null
                    }
                }
            }
            return null
        }
    }
}