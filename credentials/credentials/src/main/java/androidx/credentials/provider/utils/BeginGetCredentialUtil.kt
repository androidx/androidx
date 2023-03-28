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

package androidx.credentials.provider.utils

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.credentials.provider.BeginGetCredentialOption
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.provider.Action
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.RemoteEntry
import java.util.stream.Collectors

@RequiresApi(34)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BeginGetCredentialUtil {
    companion object {
        @JvmStatic
        internal fun convertToJetpackRequest(
            request: android.service.credentials.BeginGetCredentialRequest
        ): BeginGetCredentialRequest {
            val beginGetCredentialOptions: MutableList<BeginGetCredentialOption> =
                mutableListOf()
            request.beginGetCredentialOptions.forEach {
                beginGetCredentialOptions.add(
                    BeginGetCredentialOption.createFrom(
                        it.id, it.type, it.candidateQueryData
                    )
                )
            }
            return BeginGetCredentialRequest(
                callingAppInfo = request.callingAppInfo,
                beginGetCredentialOptions = beginGetCredentialOptions
            )
        }

        fun convertToFrameworkResponse(response: BeginGetCredentialResponse):
            android.service.credentials.BeginGetCredentialResponse {
            val frameworkBuilder = android.service.credentials.BeginGetCredentialResponse.Builder()
            populateCredentialEntries(frameworkBuilder, response.credentialEntries)
            populateActionEntries(frameworkBuilder, response.actions)
            populateAuthenticationEntries(frameworkBuilder, response.authenticationActions)
            populateRemoteEntry(frameworkBuilder, response.remoteEntry)
            return frameworkBuilder.build()
        }

        @SuppressLint("MissingPermission")
        private fun populateRemoteEntry(
            frameworkBuilder: android.service.credentials.BeginGetCredentialResponse.Builder,
            remoteEntry: RemoteEntry?
        ) {
            if (remoteEntry == null) {
                return
            }
            frameworkBuilder.setRemoteCredentialEntry(
                android.service.credentials.RemoteEntry(RemoteEntry.toSlice(remoteEntry))
            )
        }

        private fun populateAuthenticationEntries(
            frameworkBuilder: android.service.credentials.BeginGetCredentialResponse.Builder,
            authenticationActions: List<AuthenticationAction>
        ) {
            authenticationActions.forEach {
                frameworkBuilder.addAuthenticationAction(
                    android.service.credentials.Action(
                        AuthenticationAction.toSlice(it)
                    )
                )
            }
        }

        private fun populateActionEntries(
            builder: android.service.credentials.BeginGetCredentialResponse.Builder,
            actionEntries: List<Action>
        ) {
            actionEntries.forEach {
                builder.addAction(
                    android.service.credentials.Action(
                        Action.toSlice(it)
                    )
                )
            }
        }

        private fun populateCredentialEntries(
            builder: android.service.credentials.BeginGetCredentialResponse.Builder,
            credentialEntries: List<CredentialEntry>
        ) {
            credentialEntries.forEach {
                builder.addCredentialEntry(
                    android.service.credentials.CredentialEntry(
                        android.service.credentials.BeginGetCredentialOption(
                            it.beginGetCredentialOption.id,
                            it.type,
                            Bundle.EMPTY
                        ),
                        it.slice
                    )
                )
            }
        }

        fun convertToFrameworkRequest(request: BeginGetCredentialRequest):
            android.service.credentials.BeginGetCredentialRequest {
            return android.service.credentials.BeginGetCredentialRequest.Builder()
                .setCallingAppInfo(request.callingAppInfo)
                .setBeginGetCredentialOptions(request.beginGetCredentialOptions.stream()
                    .map { option -> convertToJetpackBeginOption(option) }
                    .collect(Collectors.toList()))
                .build()
        }

        private fun convertToJetpackBeginOption(option: BeginGetCredentialOption):
            android.service.credentials.BeginGetCredentialOption {
            return android.service.credentials.BeginGetCredentialOption(option.id, option.type,
                option.candidateQueryData)
        }

        fun convertToJetpackResponse(
            response: android.service.credentials.BeginGetCredentialResponse
        ): BeginGetCredentialResponse {
            return BeginGetCredentialResponse(
                credentialEntries = response.credentialEntries.stream()
                    .map { entry -> CredentialEntry.createFrom(entry.slice) }
                    .filter { entry -> entry != null }
                    .map { entry -> entry!! }
                    .collect(Collectors.toList()),
                actions = response.actions.stream()
                    .map { entry -> Action.fromSlice(entry.slice) }
                    .filter { entry -> entry != null }
                    .map { entry -> entry!! }
                    .collect(Collectors.toList()),
                authenticationActions = response.authenticationActions.stream()
                    .map { entry -> AuthenticationAction.fromSlice(entry.slice) }
                    .filter { entry -> entry != null }
                    .map { entry -> entry!! }
                    .collect(Collectors.toList()),
                remoteEntry =
                response.remoteCredentialEntry?.let { RemoteEntry.fromSlice(it.slice) }
            )
        }
    }
}