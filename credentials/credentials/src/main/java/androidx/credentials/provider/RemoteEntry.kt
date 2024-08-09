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
@file:Suppress("deprecation") // For usage of Slice

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.util.Collections

/**
 * An entry on the selector, denoting that the credential request will be completed on a remote
 * device.
 *
 * Once this entry is selected, the corresponding [pendingIntent] will be invoked. The provider can
 * then show any activity they wish to while establishing a connection with a different device and
 * retrieving a credential. Before finishing the activity, provider must set the final
 * [androidx.credentials.GetCredentialResponse] through the
 * [PendingIntentHandler.setGetCredentialResponse] helper API, or a
 * [androidx.credentials.CreateCredentialResponse] through the
 * [PendingIntentHandler.setCreateCredentialResponse] helper API depending on whether it is a get or
 * create flow.
 *
 * See [android.service.credentials.BeginGetCredentialResponse] for usage details.
 *
 * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
 *   authentication entry on the UI, must be created with flag [PendingIntent.FLAG_MUTABLE] so that
 *   the system can add the complete request to the extras of the associated intent
 * @constructor constructs an instance of [RemoteEntry]
 * @throws NullPointerException If [pendingIntent] is null
 */
class RemoteEntry(val pendingIntent: PendingIntent) {

    /**
     * A builder for [RemoteEntry]
     *
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     */
    class Builder constructor(private val pendingIntent: PendingIntent) {
        /** Builds an instance of [RemoteEntry] */
        fun build(): RemoteEntry {
            return RemoteEntry(pendingIntent)
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromRemoteEntry(remoteEntry: android.service.credentials.RemoteEntry): RemoteEntry? {
            val slice = remoteEntry.slice
            return fromSlice(slice)
        }
    }

    companion object {
        private const val TAG = "RemoteEntry"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.remoteEntry.SLICE_HINT_PENDING_INTENT"

        private const val SLICE_SPEC_TYPE = "RemoteEntry"

        private const val REVISION_ID = 1

        /**
         * Converts an instance of [RemoteEntry] to a [Slice].
         *
         * This method is only expected to be called on an API > 28 impl, hence returning null for
         * other levels as the visibility is only restricted to the library.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @JvmStatic
        fun toSlice(remoteEntry: RemoteEntry): Slice {
            val pendingIntent = remoteEntry.pendingIntent
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(SLICE_SPEC_TYPE, REVISION_ID))
            sliceBuilder.addAction(
                pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/ null
            )
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [RemoteEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
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

        /**
         * Converts a framework [android.service.credentials.RemoteEntry] class to a Jetpack
         * [RemoteEntry] class
         *
         * Note that this API is not needed in a general credential creation/retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct an
         * instance of [RemoteEntry] to populate the [BeginGetCredentialResponse] or
         * [BeginCreateCredentialResponse].
         *
         * @param remoteEntry the instance of framework action class to be converted
         */
        @JvmStatic
        fun fromRemoteEntry(remoteEntry: android.service.credentials.RemoteEntry): RemoteEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromRemoteEntry(remoteEntry)
            }
            return null
        }

        private const val EXTRA_REMOTE_ENTRY_PENDING_INTENT =
            "androidx.credentials.provider.extra.REMOTE_ENTRY_PENDING_INTENT"

        /** Marshall the remote entry data through an intent. */
        internal fun RemoteEntry.marshall(bundle: Bundle) {
            bundle.putParcelable(EXTRA_REMOTE_ENTRY_PENDING_INTENT, this.pendingIntent)
        }

        internal fun Bundle.unmarshallRemoteEntry(): RemoteEntry? {
            val pendingIntent: PendingIntent =
                this.getParcelable(EXTRA_REMOTE_ENTRY_PENDING_INTENT) ?: return null
            return RemoteEntry(pendingIntent)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteEntry) return false
        return this.pendingIntent == other.pendingIntent
    }

    override fun hashCode(): Int {
        return pendingIntent.hashCode()
    }
}
