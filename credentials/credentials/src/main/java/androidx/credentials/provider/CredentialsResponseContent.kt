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
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Objects

/**
 * Content to be presented to the user on the account selector UI, including credential entries,
 * and provider related actions.
 *
 * @property credentialEntries the list of [CredentialEntry] to be shown on the selector UI
 * @property actions the list of [Action] entries to be shown on the selector UI
 * @throws IllegalStateException if both [credentialEntries] and [actions] are empty
 *
 * @hide
 */
@RequiresApi(34)
class CredentialsResponseContent internal constructor(
    val credentialEntries: List<CredentialEntry>,
    val actions: List<Action>
    ) {

    init {
        check(!(credentialEntries.isEmpty() && actions.isEmpty())) {
            ("credentialEntries and actions must not both be empty")
        }
    }

    /**
     * Builds an instance of [CredentialsResponseContent].
     */
    class Builder {
        // TODO("Add remote entry")
        private var credentialEntries: MutableList<CredentialEntry> = mutableListOf()
        private var actions: MutableList<Action> = mutableListOf()

        /**
         * Add a [CredentialEntry] to the [CredentialsResponseContent], to be shown on the
         * selector UI as an option that can return a credential on selection.
         */
        fun addCredentialEntry(credentialEntry: CredentialEntry): Builder {
            credentialEntries.add(Objects.requireNonNull(credentialEntry))
            return this
        }

        /**
         * Set a list a [CredentialEntry] to the [CredentialsResponseContent], to be shown on the
         * selector UI as options that can return a credential on selection.
         */
        fun setCredentialEntries(
            credentialEntryList: List<CredentialEntry>
        ): Builder {
            this.credentialEntries = credentialEntryList.toMutableList()
            return this
        }

        /**
         * Add an [Action] to the [CredentialsResponseContent], to be shown on the selector
         * UI. An [Action] can be used to offer non-credential actions to the user such as
         * opening the app, managing credentials etc. When selected, the corresponding
         * [PendingIntent] is fired, that can start a provider activity.
         */
        fun addAction(action: Action): Builder {
            actions.add(Objects.requireNonNull(action, "action must not be null"))
            return this
        }

        /**
         * Sets a list of [Action] to the [CredentialsResponseContent], to be shown on the selector
         * UI. An [Action] can be used to offer non-credential actions to the user such as
         * opening the app, managing credentials etc. When selected, the corresponding
         * [PendingIntent] is fired, that can start a provider activity.
         */
        fun setActions(actionList: List<Action>): Builder {
            this.actions = actionList.toMutableList()
            return this
        }

        /** Builds an instance of [CredentialsResponseContent]
         *
         * @throws IllegalStateException if both [credentialEntries] and [actions] are empty
         */
        fun build(): CredentialsResponseContent {
            return CredentialsResponseContent(credentialEntries, actions)
        }
    }

    companion object {
        private const val TAG = "ResponseContent"
        @JvmStatic
        internal fun toFrameworkClass(credentialsResponseContent: CredentialsResponseContent):
            android.service.credentials.CredentialsResponseContent {
            val builder = android.service.credentials.CredentialsResponseContent.Builder()

            // Add all the credential entries
            credentialsResponseContent.credentialEntries.forEach {
                try {
                    builder.addCredentialEntry(
                        CredentialEntry.toFrameworkClass(it))
                } catch (e: Exception) {
                    Log.i(TAG, "Issue parsing a credentialEntry: " + e.message)
                }
            }

            // Add all the actions
            credentialsResponseContent.actions.forEach {
                try {
                    builder.addAction(Action.toFrameworkClass(it))
                } catch (e: Exception) {
                    Log.i(TAG, "Issue parsing an action: " + e.message)
                }
            }
            return builder.build()
        }
    }
}