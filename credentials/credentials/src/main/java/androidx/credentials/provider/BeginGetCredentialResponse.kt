/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.provider.Action.Companion.marshall
import androidx.credentials.provider.Action.Companion.unmarshallActionList
import androidx.credentials.provider.AuthenticationAction.Companion.marshall
import androidx.credentials.provider.AuthenticationAction.Companion.unmarshallAuthActionList
import androidx.credentials.provider.CredentialEntry.Companion.marshallToIntent
import androidx.credentials.provider.CredentialEntry.Companion.unmarshallCredentialEntries
import androidx.credentials.provider.RemoteEntry.Companion.marshall
import androidx.credentials.provider.RemoteEntry.Companion.unmarshallRemoteEntry
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * Response from a credential provider to [BeginGetCredentialRequest], containing credential entries
 * and other associated entries/data to be shown on the account selector UI.
 *
 * @param credentialEntries the list of credential entries to be shown on the selector UI, whereby
 *   each entry is set to provide a potential credential corresponding to a given
 *   [BeginGetCredentialOption] from the original [BeginGetCredentialRequest]
 * @param actions the list of action entries to be shown on the selector UI, whereby each entry is
 *   set to provide an action that the user can perform before retrieving the credential, e.g.
 *   selecting a credential from a provider UI
 * @param authenticationActions the list of authentication actions to be shown on the selector UI,
 *   whereby each entry is set to denote an account/group that is currently locked and cannot return
 *   any credentials, allowing the user to select one of these entries and unlock another set of
 *   credentials
 * @param remoteEntry the entry that is set to allow retrieving a credential from another device
 * @constructor constructs an instance of [BeginGetCredentialResponse]
 */
class BeginGetCredentialResponse
constructor(
    val credentialEntries: List<CredentialEntry> = listOf(),
    val actions: List<Action> = listOf(),
    val authenticationActions: List<AuthenticationAction> = listOf(),
    val remoteEntry: RemoteEntry? = null
) {
    /** Builder for [BeginGetCredentialResponse]. */
    class Builder {
        private var credentialEntries: MutableList<CredentialEntry> = mutableListOf()
        private var actions: MutableList<Action> = mutableListOf()
        private var authenticationActions: MutableList<AuthenticationAction> = mutableListOf()
        private var remoteEntry: RemoteEntry? = null

        /**
         * Sets a remote credential entry to be shown on the UI. Provider must set this if they wish
         * to get the credential from a different device.
         *
         * When constructing the [CredentialEntry] object, the pending intent must be set such that
         * it leads to an activity that can provide UI to fulfill the request on a remote device.
         * When user selects this [remoteEntry], the system will invoke the pending intent set on
         * the [CredentialEntry].
         *
         * <p> Once the remote credential flow is complete, the [android.app.Activity] result should
         * be set to [android.app.Activity#RESULT_OK] and an extra with the
         * [CredentialProviderService#EXTRA_GET_CREDENTIAL_RESPONSE] key should be populated with a
         * [android.credentials.Credential] object.
         *
         * <p> Note that as a provider service you will only be able to set a remote entry if :
         * - Provider service possesses the [android.Manifest.permission.PROVIDE_REMOTE_CREDENTIALS]
         *   permission.
         * - Provider service is configured as the provider that can provide remote entries.
         *
         * If the above conditions are not met, setting back [BeginGetCredentialResponse] on the
         * callback from [CredentialProviderService#onBeginGetCredential] will throw a
         * [SecurityException].
         */
        fun setRemoteEntry(remoteEntry: RemoteEntry?): Builder {
            this.remoteEntry = remoteEntry
            return this
        }

        /** Adds a [CredentialEntry] to the list of entries to be displayed on the UI. */
        fun addCredentialEntry(entry: CredentialEntry): Builder {
            credentialEntries.add(entry)
            return this
        }

        /** Sets the list of credential entries to be displayed on the account selector UI. */
        fun setCredentialEntries(entries: List<CredentialEntry>): Builder {
            credentialEntries = entries.toMutableList()
            return this
        }

        /**
         * Adds an [Action] to the list of actions to be displayed on the UI.
         *
         * <p> An [Action] must be used for independent user actions, such as opening the app,
         * intenting directly into a certain app activity etc. The pending intent set with the
         * [action] must invoke the corresponding activity.
         */
        fun addAction(action: Action): Builder {
            this.actions.add(action)
            return this
        }

        /** Sets the list of actions to be displayed on the UI. */
        fun setActions(actions: List<Action>): Builder {
            this.actions = actions.toMutableList()
            return this
        }

        /**
         * Add an authentication entry to be shown on the UI. Providers must set this entry if the
         * corresponding account is locked and no underlying credentials can be returned.
         *
         * <p> When the user selects this [authenticationAction], the system invokes the
         * corresponding pending intent. Once the authentication action activity is launched, and
         * the user is authenticated, providers should create another response with
         * [BeginGetCredentialResponse] using this time adding the unlocked credentials in the form
         * of [CredentialEntry]'s.
         *
         * <p>The new response object must be set on the authentication activity's result. The
         * result code should be set to [android.app.Activity#RESULT_OK] and the
         * [CredentialProviderService#EXTRA_BEGIN_GET_CREDENTIAL_RESPONSE] extra should be set with
         * the new fully populated [BeginGetCredentialResponse] object.
         */
        fun addAuthenticationAction(authenticationAction: AuthenticationAction): Builder {
            this.authenticationActions.add(authenticationAction)
            return this
        }

        /** Sets the list of authentication entries to be displayed on the account selector UI. */
        fun setAuthenticationActions(authenticationEntries: List<AuthenticationAction>): Builder {
            this.authenticationActions = authenticationEntries.toMutableList()
            return this
        }

        /** Builds a [BeginGetCredentialResponse] instance. */
        fun build(): BeginGetCredentialResponse {
            return BeginGetCredentialResponse(
                credentialEntries.toList(),
                actions.toList(),
                authenticationActions.toList(),
                remoteEntry
            )
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        private const val REQUEST_KEY = "androidx.credentials.provider.BeginGetCredentialResponse"

        @JvmStatic
        fun asBundle(bundle: Bundle, response: BeginGetCredentialResponse) {
            bundle.putParcelable(
                REQUEST_KEY,
                BeginGetCredentialUtil.convertToFrameworkResponse(response)
            )
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialResponse? {
            val frameworkResponse =
                bundle.getParcelable(
                    REQUEST_KEY,
                    android.service.credentials.BeginGetCredentialResponse::class.java
                )
            if (frameworkResponse != null) {
                return BeginGetCredentialUtil.convertToJetpackResponse(frameworkResponse)
            }
            return null
        }
    }

    @RequiresApi(23)
    private object Api23Impl {
        @JvmStatic
        fun asBundle(bundle: Bundle, response: BeginGetCredentialResponse) {
            response.credentialEntries.marshallToIntent(bundle)
            response.actions.marshall(bundle)
            response.authenticationActions.marshall(bundle)
            response.remoteEntry?.marshall(bundle)
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialResponse? {
            val credentialEntries = bundle.unmarshallCredentialEntries()
            val actions = bundle.unmarshallActionList()
            val authenticationActions = bundle.unmarshallAuthActionList()
            val remoteEntry = bundle.unmarshallRemoteEntry()
            if (
                credentialEntries.isEmpty() &&
                    actions.isEmpty() &&
                    authenticationActions.isEmpty() &&
                    remoteEntry == null
            ) {
                return null
            }
            return BeginGetCredentialResponse(
                credentialEntries,
                actions,
                authenticationActions,
                remoteEntry
            )
        }
    }

    companion object {
        /**
         * Helper method to convert the class to a parcelable [Bundle], in case the class instance
         * needs to be sent across a process. Consumers of this method should use [fromBundle] to
         * reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        fun asBundle(response: BeginGetCredentialResponse): Bundle {
            val bundle = Bundle()
            if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.asBundle(bundle, response)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.asBundle(bundle, response)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [BeginGetCredentialResponse].
         */
        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialResponse? {
            return if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.fromBundle(bundle)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.fromBundle(bundle)
            } else null
        }
    }
}
