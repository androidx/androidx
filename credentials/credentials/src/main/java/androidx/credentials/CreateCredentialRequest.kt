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

package androidx.credentials

import android.graphics.drawable.Icon
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.PublicKeyCredential.Companion.BUNDLE_KEY_SUBTYPE
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base request class for registering a credential.
 *
 * An application can construct a subtype request and call [CredentialManager.createCredential] to
 * launch framework UI flows to collect consent and any other metadata needed from the user to
 * register a new user credential.
 */
abstract class CreateCredentialRequest internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val credentialData: Bundle,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val candidateQueryData: Bundle,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val isSystemProviderRequired: Boolean,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val isAutoSelectAllowed: Boolean,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val displayInfo: DisplayInfo,
    val origin: String?,
) {

    init {
        @Suppress("UNNECESSARY_SAFE_CALL")
        credentialData?.let {
            credentialData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
        }
    }

    /**
     * Information that may be used for display purposes when rendering UIs to collect the user
     * consent and choice.
     *
     * @property userId the user identifier of the created credential
     * @property userDisplayName an optional display name in addition to the [userId] that may be
     * displayed next to the `userId` during the user consent to help your user better understand
     * the credential being created
     */
    class DisplayInfo internal constructor(
        val userId: CharSequence,
        val userDisplayName: CharSequence?,
        @get:RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        val credentialTypeIcon: Icon?,
        @get:RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        val defaultProvider: String?,
    ) {

        /**
         * Constructs a [DisplayInfo].
         *
         * @param userId the user id of the created credential
         * @param userDisplayName an optional display name in addition to the [userId] that may be
         * displayed next to the `userId` during the user consent to help your user better
         * understand the credential being created
         * @throws IllegalArgumentException If [userId] is empty
         */
        @JvmOverloads constructor(
            userId: CharSequence,
            userDisplayName: CharSequence? = null,
        ) : this(
            userId,
            userDisplayName,
            null,
            null,
        )

        init {
            require(userId.isNotEmpty()) { "userId should not be empty" }
        }

        @RequiresApi(23)
        internal fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putCharSequence(BUNDLE_KEY_USER_ID, userId)
            if (!TextUtils.isEmpty(userDisplayName)) {
                bundle.putCharSequence(BUNDLE_KEY_USER_DISPLAY_NAME, userDisplayName)
            }
            if (!TextUtils.isEmpty(defaultProvider)) {
                bundle.putString(BUNDLE_KEY_DEFAULT_PROVIDER, defaultProvider)
            }
            // Today the type icon is determined solely within this library right before the
            // request is passed into the framework. Later if needed a new API can be added for
            // custom SDKs to supply their own credential type icons.
            return bundle
        }

        internal companion object {
            @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
            const val BUNDLE_KEY_REQUEST_DISPLAY_INFO =
                "androidx.credentials.BUNDLE_KEY_REQUEST_DISPLAY_INFO"
            @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
            const val BUNDLE_KEY_USER_ID =
                "androidx.credentials.BUNDLE_KEY_USER_ID"

            internal const val BUNDLE_KEY_USER_DISPLAY_NAME =
                "androidx.credentials.BUNDLE_KEY_USER_DISPLAY_NAME"
            @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
            const val BUNDLE_KEY_CREDENTIAL_TYPE_ICON =
                "androidx.credentials.BUNDLE_KEY_CREDENTIAL_TYPE_ICON"

            internal const val BUNDLE_KEY_DEFAULT_PROVIDER =
                "androidx.credentials.BUNDLE_KEY_DEFAULT_PROVIDER"

            /**
             * Returns a RequestDisplayInfo from a `credentialData` Bundle, or otherwise `null` if
             * parsing fails.
             */
            @JvmStatic
            @RequiresApi(23)
            @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
            @Suppress("DEPRECATION") // bundle.getParcelable(key)
            fun parseFromCredentialDataBundle(from: Bundle): DisplayInfo? {
                return try {
                    val displayInfoBundle = from.getBundle(BUNDLE_KEY_REQUEST_DISPLAY_INFO)!!
                    val userId = displayInfoBundle.getCharSequence(BUNDLE_KEY_USER_ID)
                    val displayName =
                        displayInfoBundle.getCharSequence(BUNDLE_KEY_USER_DISPLAY_NAME)
                    val icon: Icon? =
                        displayInfoBundle.getParcelable(BUNDLE_KEY_CREDENTIAL_TYPE_ICON)
                    val defaultProvider: String? =
                        displayInfoBundle.getString(BUNDLE_KEY_DEFAULT_PROVIDER)
                    DisplayInfo(userId!!, displayName, icon, defaultProvider)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    internal companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        const val BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED =
            "androidx.credentials.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED"

        /**
         * Attempts to parse the raw data into one of [CreatePasswordRequest],
         * [CreatePublicKeyCredentialRequest], and
         * [CreateCustomCredentialRequest]. Otherwise returns null.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        @JvmStatic
        @RequiresApi(23)
        fun createFrom(
            type: String,
            credentialData: Bundle,
            candidateQueryData: Bundle,
            requireSystemProvider: Boolean,
            origin: String? = null,
        ): CreateCredentialRequest? {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        CreatePasswordRequest.createFrom(credentialData, origin)

                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        when (credentialData.getString(BUNDLE_KEY_SUBTYPE)) {
                            CreatePublicKeyCredentialRequest
                                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST ->
                                CreatePublicKeyCredentialRequest.createFrom(credentialData, origin)

                            else -> throw FrameworkClassParsingException()
                        }

                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a request with
                // the raw framework values.
                CreateCustomCredentialRequest(
                    type,
                    credentialData,
                    candidateQueryData,
                    requireSystemProvider,
                    DisplayInfo.parseFromCredentialDataBundle(
                        credentialData
                    ) ?: return null,
                    credentialData.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false)
                )
            }
        }
    }
}
