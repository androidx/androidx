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

import android.app.slice.Slice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.PasswordCredential.Companion.TYPE_PASSWORD_CREDENTIAL
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL

/**
 * Base class for a credential entry to be displayed on
 * the selector.
 */
abstract class CredentialEntry internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    val beginGetCredentialOption: BeginGetCredentialOption,
    val affiliatedDomain: CharSequence? = null,
) {

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            CredentialEntry? {
            val slice = credentialEntry.slice
            return fromSlice(slice)
        }
    }
    companion object {

        /**
         * Converts a framework [android.service.credentials.CredentialEntry] class to a Jetpack
         * [CredentialEntry] class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct
         * an instance of [CredentialEntry] to populate the [BeginGetCredentialResponse].
         *
         * @param credentialEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            CredentialEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCredentialEntry(credentialEntry)
            }
            return null
        }

        @JvmStatic
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun fromSlice(slice: Slice): CredentialEntry? {
            return try {
                when (slice.spec?.type) {
                    TYPE_PASSWORD_CREDENTIAL -> PasswordCredentialEntry.fromSlice(slice)!!
                    TYPE_PUBLIC_KEY_CREDENTIAL -> PublicKeyCredentialEntry.fromSlice(slice)!!
                    else -> CustomCredentialEntry.fromSlice(slice)!!
                }
            } catch (e: Exception) {
                // Try CustomCredentialEntry.fromSlice one last time in case the cause was a failed
                // password / passkey parsing attempt.
                CustomCredentialEntry.fromSlice(slice)
            }
        }

        @JvmStatic
        @RequiresApi(28)
        internal fun toSlice(entry: CredentialEntry): Slice? {
            when (entry) {
                is PasswordCredentialEntry -> return PasswordCredentialEntry.toSlice(entry)
                is PublicKeyCredentialEntry -> return PublicKeyCredentialEntry.toSlice(entry)
                is CustomCredentialEntry -> return CustomCredentialEntry.toSlice(entry)
            }
            return null
        }
    }
}
