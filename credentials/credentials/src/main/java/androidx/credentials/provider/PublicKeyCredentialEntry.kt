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
import android.graphics.drawable.Icon
import androidx.annotation.RequiresApi
import androidx.credentials.Credential
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential

/**
 * A public key credential entry that is displayed on the account selector UI.
 * This entry denotes that a credential of type [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL]
 * is available for the user.
 *
 * @property username the username of the account holding the public key credential
 * @property displayName the displayName of the account holding the public key credential
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects this entry
 * @property credential the [PublicKeyCredential] to be returned to the calling app when
 * the user selects this entry. Set this only if no further interaction is required
 * @property lastUsedTimeMillis the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector
 *
 * @throws IllegalArgumentException if [username] is empty
 * @throws IllegalStateException if both [pendingIntent] and [credential] are null, or both
 * are non null
 *
 * @hide
 */
@RequiresApi(34)
class PublicKeyCredentialEntry internal constructor(
    username: CharSequence,
    displayName: CharSequence?,
    pendingIntent: PendingIntent?,
    credential: Credential?,
    lastUsedTime: Long,
    icon: Icon?
) : CredentialEntry(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL, username, displayName,
    pendingIntent, credential, lastUsedTime, icon
) {

    /**
     * Builder for [PublicKeyCredentialEntry]
     *
     * @hide
     */
    class Builder {
        // TODO("Add autoSelect")
        private val username: CharSequence
        private var displayName: CharSequence? = null
        private var pendingIntent: PendingIntent? = null
        private var credential: Credential? = null
        private var lastUsedTime: Long = 0
        private var icon: Icon? = null

        /**
         * @param username the username of the account holding the credential
         * @param pendingIntent the [PendingIntent] to be invoked when the entry is selected
         *
         * Providers should use this constructor when an additional activity is required
         * before returning the final [PasswordCredential]
         */
        constructor(username: CharSequence, pendingIntent: PendingIntent) {
            this.username = username
            this.pendingIntent = pendingIntent
        }

        // TODO("Consider removing this constructor for public key but hold till 2-phase get flow")
        /**
         * @param username the username of the account holding the credential
         * @param credential the [PasswordCredential] to be returned when the entry is selected
         *
         * Providers should use this constructor when the credential to be returned is available
         * and no additional activity is required.
         */
        constructor(username: CharSequence, credential: PasswordCredential) {
            this.username = username
            this.credential = credential
        }

        /** Sets a displayName to be shown on the UI with this entry */
        fun setDisplayName(displayName: CharSequence?): Builder {
            this.displayName = displayName
            return this
        }

        /** Sets the icon to be shown on the UI with this entry */
        fun setIcon(icon: Icon?): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets the last used time of this account
         *
         * This information will be used to sort the entries on the selector.
         */
        fun setLastUsedTime(lastUsedTime: Long): Builder {
            this.lastUsedTime = lastUsedTime
            return this
        }

        /** Builds an instance of [PublicKeyCredentialEntry] */
        fun build(): PublicKeyCredentialEntry {
            return PublicKeyCredentialEntry(
                username, displayName, pendingIntent,
                credential, lastUsedTime, icon)
        }
    }
}