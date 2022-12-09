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
import androidx.credentials.Credential
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.R

/**
 * A password credential entry that is displayed on the account selector UI. This
 * entry denotes that a credential of type [PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
 * is available for the user.
 *
 * @property username the username of the account holding the password credential
 * @property displayName the displayName of the account holding the password credential
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects this entry
 * @property credential the [PublicKeyCredential] to be returned to the calling app when
 * the user selects this entry. Set this here only if no further interaction is required
 * @property lastUsedTimeMillis the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector
 *
 * @throws IllegalArgumentException if [username] is empty
 * @throws IllegalStateException if both [pendingIntent] and [credential] are null, or both
 * are non null
 *
 * @see CredentialEntry
 *
 * @hide
 */
@RequiresApi(34)
class PasswordCredentialEntry internal constructor(
    typeDisplayName: CharSequence,
    username: CharSequence,
    displayName: CharSequence?,
    pendingIntent: PendingIntent?,
    credential: Credential?,
    lastUsedTimeMillis: Long,
    icon: Icon
) : CredentialEntry(PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    typeDisplayName, username, displayName, pendingIntent, credential,
    lastUsedTimeMillis, icon
) {

    /**
     * Builder for [PasswordCredentialEntry]
     *
     * @property displayName the displayname of the account holding the credential
     * @property pendingIntent the [PendingIntent] to be invoked when the user selects this entry
     * @property credential the [PasswordCredential] to be returned to the calling app when
     * the user selects this entry
     * @property lastUsedTimeMillis the last used time of this entry
     * @property icon the icon to be displayed with this entry on the selector
     *
     * @hide
     */
    class Builder {
        // TODO("Add auto select")
        private val context: Context
        private val username: CharSequence
        private var displayName: CharSequence? = null
        private var pendingIntent: PendingIntent? = null
        private var credential: Credential? = null
        private var lastUsedTimeMillis: Long = 0
        private var icon: Icon? = null

        /**
         * @property username the username of the account holding the credential
         * @property pendingIntent the [PendingIntent] to be invoked when the entry is selected
         *
         * Providers should use this constructor when an additional activity is required
         * before returning the final [PasswordCredential]
         */
        constructor(context: Context, username: CharSequence, pendingIntent: PendingIntent) {
            this.context = context
            this.username = username
            this.pendingIntent = pendingIntent
        }

        /**
         * @property username the username of the account holding the credential
         * @property credential the [PasswordCredential] to be returned when the entry is selected
         *
         * Providers should use this constructor when the credential to be returned is available
         * and no additional activity is required.
         */
        constructor(context: Context, username: CharSequence, credential: PasswordCredential) {
            this.context = context
            this.username = username
            this.credential = credential
        }

        /** Sets a displayname to be shown on the UI with this entry */
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
        fun setLastUsedTimeMillis(lastUsedTimeMillis: Long): Builder {
            this.lastUsedTimeMillis = lastUsedTimeMillis
            return this
        }

        /** Builds an instance of [PasswordCredentialEntry] */
        fun build(): PasswordCredentialEntry {
            if (icon == null) {
                icon = Icon.createWithResource(context, R.drawable.ic_password)
            }
            val typeDisplayName = context.getString(
                R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL)
            return PasswordCredentialEntry(typeDisplayName,
                username, displayName, pendingIntent,
                credential, lastUsedTimeMillis, icon!!)
        }
    }
}