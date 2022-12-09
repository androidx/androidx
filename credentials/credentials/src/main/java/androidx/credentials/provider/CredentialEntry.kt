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
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.provider.Action.Companion.toSlice
import androidx.credentials.provider.CreateEntry.Companion.toSlice
import java.util.Collections

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
    // TODO("Add credential type display name for both CredentialEntry & CreateEntry")
    val type: String,
    val typeDisplayName: CharSequence,
    val username: CharSequence,
    val displayName: CharSequence?,
    val pendingIntent: PendingIntent?,
    val credential: Credential?,
    // TODO("Consider using Instant or other strongly typed time data type")
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
        private const val TAG = "CredentialEntry"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_TYPE_DISPLAY_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_USERNAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_USER_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_DISPLAYNAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CREDENTIAL_TYPE_DISPLAY_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"

        @JvmStatic
        internal fun toSlice(credentialEntry: CredentialEntry): Slice {
            // TODO("Put the right revision value")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(
                credentialEntry.type, 1))
                .addText(credentialEntry.typeDisplayName, /*subType=*/null,
                    listOf(SLICE_HINT_TYPE_DISPLAY_NAME))
                .addText(credentialEntry.username, /*subType=*/null,
                    listOf(SLICE_HINT_USERNAME))
                .addText(credentialEntry.displayName, /*subType=*/null,
                    listOf(SLICE_HINT_DISPLAYNAME))
                .addLong(credentialEntry.lastUsedTimeMillis, /*subType=*/null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS))
            if (credentialEntry.icon != null) {
                sliceBuilder.addIcon(credentialEntry.icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
            }
            if (credentialEntry.pendingIntent != null) {
                sliceBuilder.addAction(credentialEntry.pendingIntent,
                    Slice.Builder(sliceBuilder)
                        .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                        .build(),
                    /*subType=*/null)
            }
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [CredentialEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CredentialEntry? {
            var typeDisplayName: CharSequence? = null
            var username: CharSequence? = null
            var displayName: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var lastUsedTimeMillis: Long = 0

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_TYPE_DISPLAY_NAME)) {
                    typeDisplayName = it.text
                } else if (it.hasHint(SLICE_HINT_USERNAME)) {
                    username = it.text
                } else if (it.hasHint(SLICE_HINT_DISPLAYNAME)) {
                    displayName = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTimeMillis = it.long
                }
            }

            return try {
                CredentialEntry(slice.spec!!.type, typeDisplayName!!, username!!,
                    displayName, pendingIntent!!,
                    /*credential=*/null,
                    lastUsedTimeMillis, icon)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
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
                        credentialEntry.type,
                        credentialEntry.credential.data)
                ).build()
            } else {
                throw FrameworkClassParsingException()
            }
        }
    }
}