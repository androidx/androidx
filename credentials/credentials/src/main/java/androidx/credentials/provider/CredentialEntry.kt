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

import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base class for a credential entry that is displayed on the account selector UI.
 * Each entry corresponds to an account that can provide a credential
 *
 * @property type the type of the credential
 * @property username the username of the account holding the credential
 * @property displayName the displayName of the account holding the credential
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects this entry
 * @property credential the [Credential] to be returned to the calling app when the user selects
 * this entry
 * only one of the selector
 * @property lastUsedTimeMillis the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector
 *
 * @throws IllegalArgumentException If [type] or [username] is empty
 * @throws IllegalStateException If both [pendingIntent] and [credential] are null, or both
 * are non null
 *
 * @hide
 */
@RequiresApi(34)
open class CredentialEntry constructor(
    val type: String,
    val username: CharSequence,
    val displayName: CharSequence?,
    val pendingIntent: PendingIntent?,
    val credential: Credential?,
    // TODO("Consider using Instant or other strongly typed time ddta type")
    val lastUsedTimeMillis: Long,
    val icon: Icon?
    ) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
        require(username.isNotEmpty()) { "type must not be empty" }
        check(!((pendingIntent == null && credential == null) ||
            (pendingIntent != null && credential != null))) {
            "Must set either pendingIntent or credential, and not both or none"
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_USER_NAME = "HINT_USER_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_DISPLAY_NAME = "HINT_CREDENTIAL_TYPE_DISPLAY_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_LAST_USED_TIME_MILLIS = "HINT_LAST_USED_TIME_MILLIS"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_ICON = "HINT_PROFILE_ICON"

        @JvmStatic
        internal fun toSlice(credentialEntry: CredentialEntry): Slice {
            // TODO("Put the right spec and version value")
            return Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
                .addText(credentialEntry.username, /*subType=*/null,
                    listOf(SLICE_HINT_USER_NAME))
                .addText(credentialEntry.displayName, /*subType=*/null,
                    listOf(SLICE_HINT_DISPLAY_NAME))
                .addLong(credentialEntry.lastUsedTimeMillis, /*subType=*/null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS))
                .addIcon(credentialEntry.icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
                .build()
        }

        internal fun toFrameworkClass(credentialEntry: CredentialEntry):
            android.service.credentials.CredentialEntry {
            return if (credentialEntry.pendingIntent != null) {
                android.service.credentials.CredentialEntry.Builder(
                    credentialEntry.type, toSlice(credentialEntry),
                    credentialEntry.pendingIntent
                ).build()
            } else if (credentialEntry.credential != null) {
                android.service.credentials.CredentialEntry.Builder(
                    credentialEntry.type, toSlice(credentialEntry),
                    android.credentials.Credential(
                        credentialEntry.type, credentialEntry.credential.data)
                ).build()
            } else {
                throw FrameworkClassParsingException()
            }
        }
    }
}