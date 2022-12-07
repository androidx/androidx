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

import android.service.credentials.BeginCreateCredentialResponse
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * The response to a call.
 *
 * @property createEntries the list of [CreateEntry] that is presented to the user at the time
 * of credential creation. Each entry corresponds to an account or group where the user could
 * register the created with.
 * @throws IllegalArgumentException If [createEntries] is empty
 *
 * @hide
 */
@RequiresApi(34)
class BeginCreateCredentialProviderResponse constructor(
    val createEntries: List<CreateEntry>,
    val remoteEntry: RemoteEntry?,
    val header: CharSequence?
    ) {

    init {
        require(createEntries.isNotEmpty()) { "createEntries must not be empty" }
    }

    /** Builder for [BeginCreateCredentialProviderResponse]. */
    class Builder {
        // TODO("Add header and remote entry")
        private var createEntries: MutableList<CreateEntry> = mutableListOf()
        private var header: CharSequence? = null
        private var remoteEntry: RemoteEntry? = null

        /** Adds a [CreateEntry] to be displayed on the selector */
        fun addCreateEntry(createEntry: CreateEntry): Builder {
            createEntries.add(createEntry)
            return this
        }

        /** Sets a list of [CreateEntry] to be displayed on the selector */
        fun setCreateEntries(createEntries: List<CreateEntry>): Builder {
            this.createEntries = createEntries.toMutableList()
            return this
        }

        /** Sets a header to be displayed on the selector */
        fun setHeader(header: CharSequence?): Builder {
            this.header = header
            return this
        }

        /** Sets a [RemoteEntry] that denotes the flow can be completed on a remote device */
        fun setRemoteEntry(remoteEntry: RemoteEntry?): Builder {
            this.remoteEntry = remoteEntry
            return this
        }

        /**
         * Builds an instance of [BeginCreateCredentialProviderResponse]
         *
         * @throws IllegalArgumentException If [createEntries] is empty
         */
        fun build(): BeginCreateCredentialProviderResponse {
            return BeginCreateCredentialProviderResponse(createEntries, remoteEntry, header)
        }
    }

    companion object {
        private const val TAG = "CreateResponse"
        @JvmStatic
        internal fun toFrameworkClass(response: BeginCreateCredentialProviderResponse):
            BeginCreateCredentialResponse {
            val builder = BeginCreateCredentialResponse.Builder()
            response.createEntries.forEach {
                try {
                    builder.addCreateEntry(CreateEntry.toFrameworkClass(it))
                } catch (e: Exception) {
                    Log.i(TAG, "Issue while creating framework class: " + e.message)
                }
            }
            builder.setRemoteCreateEntry(response.remoteEntry?.let {
                RemoteEntry.toFrameworkCreateEntryClass(
                    it
                )
            })
            return builder.build()
        }
    }
}