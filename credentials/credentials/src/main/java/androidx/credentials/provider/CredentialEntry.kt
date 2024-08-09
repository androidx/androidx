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
@file:Suppress("deprecation") // For usage of Slice

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.slice.Slice
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.PasswordCredential.Companion.TYPE_PASSWORD_CREDENTIAL
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL
import androidx.credentials.R
import androidx.credentials.provider.CustomCredentialEntry.Companion.marshall
import androidx.credentials.provider.PasswordCredentialEntry.Companion.marshall
import androidx.credentials.provider.PublicKeyCredentialEntry.Companion.marshall

/**
 * Base class for a credential entry to be displayed on the selector.
 *
 * The [entryGroupId] allows the credential selector display to, in the case of multiple entries
 * across providers that have the same [entryGroupId] value, trim down to a single, most recently
 * used provider on the primary card, meant for quick authentication. This will also be used for
 * entry grouping display logic. However, if the user desires, it is possible to expand back the
 * entries and select the provider of their choice. This should be something directly linked to the
 * credential (e.g. [PublicKeyCredentialEntry] and [PasswordCredentialEntry] utilize 'username'),
 * and should allow variance only as far as the case of letters (i.e. Foo@gmail.com and
 * foo@gmail.com). These guidelines should be followed in cases where [CustomCredentialEntry] are
 * created.
 *
 * (RestrictTo property) type the type of the credential associated with this entry, e.g. a
 * [BeginGetPasswordOption] will have type [TYPE_PASSWORD_CREDENTIAL]
 *
 * @property beginGetCredentialOption the option from the original [BeginGetCredentialRequest], for
 *   which this credential entry is being added
 * @property entryGroupId an ID used for deduplication or to group entries during display
 * @property affiliatedDomain the user visible affiliated domain, a CharSequence representation of a
 *   web domain or an app package name that the given credential in this entry is associated with
 *   when it is different from the requesting entity, default null
 * @property isDefaultIconPreferredAsSingleProvider when set to true, the UI prefers to render the
 *   default credential type icon when you are the only available provider; see individual
 *   subclasses for these default icons (e.g. for [PublicKeyCredentialEntry], it is based on
 *   [R.drawable.ic_password])
 * @property biometricPromptData the data that is set optionally to utilize a credential manager
 *   flow that directly handles the biometric verification and presents back the response; set to
 *   null by default, so if not opted in, the embedded biometric prompt flow will not show
 */
abstract class CredentialEntry
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) open val type: String,
    val beginGetCredentialOption: BeginGetCredentialOption,
    val entryGroupId: CharSequence,
    val isDefaultIconPreferredAsSingleProvider: Boolean,
    val affiliatedDomain: CharSequence? = null,
    val biometricPromptData: BiometricPromptData? = null,
) {
    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromCredentialEntry(
            credentialEntry: android.service.credentials.CredentialEntry
        ): CredentialEntry? {
            val slice = credentialEntry.slice
            return fromSlice(slice)
        }
    }

    @RequiresApi(35)
    internal object Api35Impl {
        @JvmStatic
        fun toSlice(entry: CredentialEntry): Slice? {
            when (entry) {
                is PasswordCredentialEntry -> return PasswordCredentialEntry.toSlice(entry)
                is PublicKeyCredentialEntry -> return PublicKeyCredentialEntry.toSlice(entry)
                is CustomCredentialEntry -> return CustomCredentialEntry.toSlice(entry)
            }
            return null
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CredentialEntry? {
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
    }

    @RequiresApi(28)
    internal object Api28Impl {
        @JvmStatic
        fun toSlice(entry: CredentialEntry): Slice? {
            when (entry) {
                is PasswordCredentialEntry -> return PasswordCredentialEntry.toSlice(entry)
                is PublicKeyCredentialEntry -> return PublicKeyCredentialEntry.toSlice(entry)
                is CustomCredentialEntry -> return CustomCredentialEntry.toSlice(entry)
            }
            return null
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CredentialEntry? {
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
    }

    companion object {
        internal const val TRUE_STRING = "true"
        internal const val FALSE_STRING = "false"
        internal const val REVISION_ID = 1
        internal const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_TYPE_DISPLAY_NAME"
        internal const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_USER_NAME"
        internal const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CREDENTIAL_TYPE_DISPLAY_NAME"
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"
        internal const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_ALLOWED"
        internal const val SLICE_HINT_IS_DEFAULT_ICON_PREFERRED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_IS_DEFAULT_ICON_PREFERRED"
        internal const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"
        internal const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"
        internal const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"
        internal const val SLICE_HINT_AFFILIATED_DOMAIN =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AFFILIATED_DOMAIN"
        internal const val SLICE_HINT_DEDUPLICATION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEDUPLICATION_ID"
        internal const val SLICE_HINT_BIOMETRIC_PROMPT_DATA =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_BIOMETRIC_PROMPT_DATA"
        internal const val SLICE_HINT_ALLOWED_AUTHENTICATORS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_ALLOWED_AUTHENTICATORS"
        internal const val SLICE_HINT_CRYPTO_OP_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CRYPTO_OP_ID"

        /**
         * Converts a framework [android.service.credentials.CredentialEntry] class to a Jetpack
         * [CredentialEntry] class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct an
         * instance of [CredentialEntry] to populate the [BeginGetCredentialResponse].
         *
         * @param credentialEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCredentialEntry(
            credentialEntry: android.service.credentials.CredentialEntry
        ): CredentialEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCredentialEntry(credentialEntry)
            }
            return null
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun fromSlice(slice: Slice): CredentialEntry? {
            return if (Build.VERSION.SDK_INT >= 35) {
                Api35Impl.fromSlice(slice)
            } else if (Build.VERSION.SDK_INT >= 28) {
                Api28Impl.fromSlice(slice)
            } else {
                null
            }
        }

        @JvmStatic
        internal fun toSlice(entry: CredentialEntry): Slice? {
            return if (Build.VERSION.SDK_INT >= 35) {
                Api35Impl.toSlice(entry)
            } else if (Build.VERSION.SDK_INT >= 28) {
                Api28Impl.toSlice(entry)
            } else {
                null
            }
        }

        internal const val EXTRA_CREDENTIAL_ENTRY_SIZE =
            "androidx.credentials.provider.extra.CREDENTIAL_ENTRY_SIZE"
        internal const val EXTRA_CREDENTIAL_ENTRY_ENTRY_TYPE_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_ENTRY_TYPE_"
        internal const val EXTRA_CREDENTIAL_ENTRY_ENTRY_GROUP_ID_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_ENTRY_ENTRY_GROUP_ID_"
        internal const val EXTRA_CREDENTIAL_ENTRY_IS_DEFAULT_ICON_PREFERRED_AS_SINGLE_PROV_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_ENTRY_IS_DEFAULT_ICON_PREFERRED_AS_SINGLE_PROV_"
        internal const val EXTRA_CREDENTIAL_ENTRY_AFFILIATED_DOMAIN_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_ENTRY_AFFILIATED_DOMAIN_"
        internal const val EXTRA_CREDENTIAL_ENTRY_OPTION_ID_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_ID_"
        internal const val EXTRA_CREDENTIAL_ENTRY_OPTION_TYPE_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_TYPE_"
        internal const val EXTRA_CREDENTIAL_ENTRY_OPTION_DATA_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_DATA_"
        internal const val EXTRA_CREDENTIAL_ENTRY_PENDING_INTENT_PREFIX =
            "androidx.credentials.provider.extra.PENDING_INTENT_"
        internal const val EXTRA_CREDENTIAL_ENTRY_IS_AUTO_SELECT_ALLOWED_PREFIX =
            "androidx.credentials.provider.extra.IS_AUTO_SELECT_ALLOWED_"
        internal const val EXTRA_CREDENTIAL_ENTRY_IS_AUTO_SELECT_ALLOWED_FROM_OPTION_PREFIX =
            "androidx.credentials.provider.extra.IS_AUTO_SELECT_ALLOWED_FROM_OPTION_"
        internal const val EXTRA_CREDENTIAL_ENTRY_LAST_USED_TIME_PREFIX =
            "androidx.credentials.provider.extra.LAST_USED_TIME_"
        internal const val EXTRA_CREDENTIAL_ENTRY_HAS_DEFAULT_ICON_PREFIX =
            "androidx.credentials.provider.extra.HAS_DEFAULT_ICON_"
        internal const val EXTRA_CREDENTIAL_TITLE_PREFIX =
            "androidx.credentials.provider.extra.TITLE_"
        internal const val EXTRA_CREDENTIAL_SUBTITLE_PREFIX =
            "androidx.credentials.provider.extra.SUBTITLE_"
        internal const val EXTRA_CREDENTIAL_TYPE_DISPLAY_NAME_PREFIX =
            "androidx.credentials.provider.extra.TYPE_DISPLAY_NAME_"
        internal const val EXTRA_CREDENTIAL_TYPE_ICON_PREFIX =
            "androidx.credentials.provider.extra.ICON_"

        /** Marshall a list of credential entries through an intent. */
        @RequiresApi(23)
        internal fun List<CredentialEntry>.marshall(bundle: Bundle) {
            bundle.putInt(EXTRA_CREDENTIAL_ENTRY_SIZE, this.size)
            for (i in indices) {
                when (val entry = this[i]) {
                    is PasswordCredentialEntry -> entry.marshall(bundle, i)
                    is PublicKeyCredentialEntry -> entry.marshall(bundle, i)
                    is CustomCredentialEntry -> entry.marshall(bundle, i)
                }
            }
        }

        internal fun CredentialEntry.marshallCommonProperties(bundle: Bundle, index: Int) {
            bundle.putString("$EXTRA_CREDENTIAL_ENTRY_ENTRY_TYPE_PREFIX$index", this.type)
            bundle.putString(
                "$EXTRA_CREDENTIAL_ENTRY_OPTION_ID_PREFIX$index",
                this.beginGetCredentialOption.id
            )
            bundle.putString(
                "$EXTRA_CREDENTIAL_ENTRY_OPTION_TYPE_PREFIX$index",
                this.beginGetCredentialOption.type
            )
            bundle.putBundle(
                "$EXTRA_CREDENTIAL_ENTRY_OPTION_DATA_PREFIX$index",
                this.beginGetCredentialOption.candidateQueryData
            )
            bundle.putCharSequence(
                "$EXTRA_CREDENTIAL_ENTRY_ENTRY_GROUP_ID_PREFIX$index",
                this.entryGroupId
            )
            bundle.putBoolean(
                "$EXTRA_CREDENTIAL_ENTRY_IS_DEFAULT_ICON_PREFERRED_AS_SINGLE_PROV_PREFIX$index",
                this.isDefaultIconPreferredAsSingleProvider
            )
            this.affiliatedDomain?.let {
                bundle.putCharSequence("$EXTRA_CREDENTIAL_ENTRY_AFFILIATED_DOMAIN_PREFIX$index", it)
            }
        }

        @RequiresApi(23)
        internal fun Bundle.unmarshallCredentialEntries(): List<CredentialEntry> {
            val entries = mutableListOf<CredentialEntry>()
            val size = this.getInt(EXTRA_CREDENTIAL_ENTRY_SIZE, 0)
            for (index in 0 until size) {
                val type =
                    this.getString("$EXTRA_CREDENTIAL_ENTRY_ENTRY_TYPE_PREFIX$index")
                        ?: return emptyList()
                val entry: CredentialEntry =
                    when (type) {
                        TYPE_PASSWORD_CREDENTIAL -> PasswordCredentialEntry.unmarshall(this, index)
                        TYPE_PUBLIC_KEY_CREDENTIAL ->
                            PublicKeyCredentialEntry.unmarshall(this, index)
                        else -> CustomCredentialEntry.unmarshall(this, index, type)
                    } ?: return emptyList()
                entries.add(entry)
            }
            return entries
        }
    }
}
