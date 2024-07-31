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
import androidx.annotation.RequiresApi
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreateCustomCredentialRequest
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.RemoteEntry
import java.util.stream.Collectors

@RequiresApi(34)
internal class BeginCreateCredentialUtil {
    internal companion object {
        @JvmStatic
        internal fun convertToJetpackRequest(
            request: android.service.credentials.BeginCreateCredentialRequest
        ): BeginCreateCredentialRequest {
            return try {
                when (request.type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> {
                        BeginCreatePasswordCredentialRequest.createFrom(
                            request.data,
                            request.callingAppInfo?.let {
                                CallingAppInfo.create(it.packageName, it.signingInfo, it.origin)
                            }
                        )
                    }
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> {
                        BeginCreatePublicKeyCredentialRequest.createFrom(
                            request.data,
                            request.callingAppInfo?.let {
                                CallingAppInfo.create(it.packageName, it.signingInfo, it.origin)
                            }
                        )
                    }
                    else -> {
                        BeginCreateCustomCredentialRequest(
                            request.type,
                            request.data,
                            request.callingAppInfo?.let {
                                CallingAppInfo.create(it.packageName, it.signingInfo, it.origin)
                            }
                        )
                    }
                }
            } catch (e: FrameworkClassParsingException) {
                BeginCreateCustomCredentialRequest(
                    request.type,
                    request.data,
                    request.callingAppInfo?.let {
                        CallingAppInfo.create(it.packageName, it.signingInfo, it.origin)
                    }
                )
            }
        }

        fun convertToFrameworkResponse(
            response: BeginCreateCredentialResponse
        ): android.service.credentials.BeginCreateCredentialResponse {
            val frameworkBuilder =
                android.service.credentials.BeginCreateCredentialResponse.Builder()
            populateCreateEntries(frameworkBuilder, response.createEntries)
            populateRemoteEntry(frameworkBuilder, response.remoteEntry)
            return frameworkBuilder.build()
        }

        @SuppressLint("MissingPermission") // This is an internal util. Actual permission check
        // happens at the framework level
        private fun populateRemoteEntry(
            frameworkBuilder: android.service.credentials.BeginCreateCredentialResponse.Builder,
            remoteEntry: RemoteEntry?
        ) {
            if (remoteEntry == null) {
                return
            }
            frameworkBuilder.setRemoteCreateEntry(
                android.service.credentials.RemoteEntry(RemoteEntry.toSlice(remoteEntry))
            )
        }

        private fun populateCreateEntries(
            frameworkBuilder: android.service.credentials.BeginCreateCredentialResponse.Builder,
            createEntries: List<CreateEntry>
        ) {
            createEntries.forEach {
                val entrySlice = CreateEntry.toSlice(it)
                if (entrySlice != null) {
                    frameworkBuilder.addCreateEntry(
                        android.service.credentials.CreateEntry(entrySlice)
                    )
                }
            }
        }

        fun convertToFrameworkRequest(
            request: BeginCreateCredentialRequest
        ): android.service.credentials.BeginCreateCredentialRequest {
            var callingAppInfo: android.service.credentials.CallingAppInfo? = null
            if (request.callingAppInfo != null) {
                callingAppInfo =
                    android.service.credentials.CallingAppInfo(
                        request.callingAppInfo.packageName,
                        request.callingAppInfo.signingInfo,
                        request.callingAppInfo.origin
                    )
            }
            return android.service.credentials.BeginCreateCredentialRequest(
                request.type,
                request.candidateQueryData,
                callingAppInfo
            )
        }

        fun convertToJetpackResponse(
            frameworkResponse: android.service.credentials.BeginCreateCredentialResponse
        ): BeginCreateCredentialResponse {
            return BeginCreateCredentialResponse(
                createEntries =
                    frameworkResponse.createEntries
                        .stream()
                        .map { entry -> CreateEntry.fromSlice(entry.slice) }
                        .filter { entry -> entry != null }
                        .map { entry -> entry!! }
                        .collect(Collectors.toList()),
                remoteEntry =
                    frameworkResponse.remoteCreateEntry?.let { RemoteEntry.fromSlice(it.slice) }
            )
        }
    }
}
