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

import android.os.Bundle
import android.service.credentials.BeginGetCredentialOption
import android.service.credentials.BeginGetCredentialRequest
import androidx.annotation.RequiresApi
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption

/**
 * @hide
 */
@RequiresApi(34)
class BeginGetCredentialUtil {
    companion object {
        @JvmStatic
        internal fun convertToStructuredRequest(request: BeginGetCredentialRequest):
            BeginGetCredentialRequest {
            val beginGetCredentialOptions: MutableList<BeginGetCredentialOption> =
                mutableListOf()
            request.beginGetCredentialOptions.forEach {
                beginGetCredentialOptions.add(convertRequestOption(
                    it.type,
                    it.candidateQueryData, it.id)
                )
            }
            return BeginGetCredentialRequest.Builder()
                .setCallingAppInfo(request.callingAppInfo)
                .setBeginGetCredentialOptions(beginGetCredentialOptions)
                .build()
        }
        @JvmStatic
        internal fun convertRequestOption(
            type: String,
            candidateQueryData: Bundle,
            id: String
        ):
            BeginGetCredentialOption {
            return when (type) {
                PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> {
                    BeginGetPasswordOption(candidateQueryData, id)
                }
                PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> {
                    BeginGetPublicKeyCredentialOption.createFrom(candidateQueryData, id)
                }
                else -> {
                    BeginGetCredentialOption(id, type, candidateQueryData)
                }
            }
        }
    }
}