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
import android.content.Context
import android.graphics.drawable.Icon
import androidx.annotation.RequiresApi
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.R

/**
 * A public key credential entry that is displayed on the account selector UI.
 * This entry denotes that a credential of type [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL]
 * is available for the user.
 *
 * @property username the username of the account holding the public key credential
 * @property displayName the displayName of the account holding the public key credential
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects this entry
 * @property lastUsedTimeMillis the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector
 *
 * @throws IllegalArgumentException if [username] is empty, or [pendingIntent] is null
 * are non null
 *
 * @hide
 */
@RequiresApi(34)
class PublicKeyCredentialEntry internal constructor(
    typeDisplayName: CharSequence,
    username: CharSequence,
    displayName: CharSequence?,
    pendingIntent: PendingIntent,
    lastUsedTime: Long,
    icon: Icon,
    autoSelectAllowed: Boolean
) : CredentialEntry(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL, typeDisplayName, username, displayName,
    pendingIntent, lastUsedTime, icon, autoSelectAllowed
) {

    /**
     * Builder for [PublicKeyCredentialEntry]
     *
     * @hide
     */
    class Builder {
        // TODO("Add autoSelect")
        private val context: Context
        private val username: CharSequence
        private var displayName: CharSequence? = null
        private var pendingIntent: PendingIntent? = null
        private var lastUsedTimeMillis: Long = 0
        private var icon: Icon? = null
        private var autoSelectAllowed: Boolean = false

        /**
         * @param username the username of the account holding the credential
         * @param pendingIntent the [PendingIntent] to be invoked when the entry is selected
         *
         * Providers should use this constructor when an additional activity is required
         * before returning the final [PasswordCredential]
         */
        constructor(context: Context, username: CharSequence, pendingIntent: PendingIntent) {
            this.context = context
            this.username = username
            this.pendingIntent = pendingIntent
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
         * Sets whether the entry should be auto-selected.
         * The value is fale by default
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAutoSelectAllowed(autoSelectAllowed: Boolean): Builder {
            this.autoSelectAllowed = autoSelectAllowed
            return this
        }

        /**
         * Sets the last used time of this account
         *
         * This information will be used to sort the entries on the selector.
         */
        fun setLastUsedTimeMillis(lastUsedTimeMillis: Long): Builder {
            this.lastUsedTimeMillis = lastUsedTimeMillis
            return this
        }

        /** Builds an instance of [PublicKeyCredentialEntry] */
        fun build(): PublicKeyCredentialEntry {
            if (icon == null) {
                icon = Icon.createWithResource(context, R.drawable.ic_password)
            }
            val typeDisplayName = context.getString(
                R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL)
            return PublicKeyCredentialEntry(typeDisplayName,
                username, displayName, pendingIntent!!,
                lastUsedTimeMillis, icon!!, autoSelectAllowed)
        }
    }
}
