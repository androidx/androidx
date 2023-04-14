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
import androidx.annotation.VisibleForTesting
import androidx.credentials.PublicKeyCredential.Companion.BUNDLE_KEY_SUBTYPE
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base request class for registering a credential.
 *
 * An application can construct a subtype request and call [CredentialManager.createCredential] to
 * launch framework UI flows to collect consent and any other metadata needed from the user to
 * register a new user credential.
 *
 * @property preferImmediatelyAvailableCredentials true if you prefer the operation to return
 * immediately when there is no available credential creation offering instead of falling back to
 * discovering remote options, and false (preferably) otherwise.
 * @property origin the origin of a different application if the request is being made on behalf of
 * that application. For API level >=34, setting a non-null value for this parameter, will throw
 * a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present.
 */
abstract class CreateCredentialRequest internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val credentialData: Bundle,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val candidateQueryData: Bundle,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isSystemProviderRequired: Boolean,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isAutoSelectAllowed: Boolean,
    /** @hide */
    val displayInfo: DisplayInfo,
    val origin: String?,
    @get:JvmName("preferImmediatelyAvailableCredentials")
    val preferImmediatelyAvailableCredentials: Boolean,
) {

    init {
        credentialData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
        credentialData.putBoolean(
            BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
            preferImmediatelyAvailableCredentials
        )
        candidateQueryData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
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
    class DisplayInfo internal /** @hide */ constructor(
        val userId: CharSequence,
        val userDisplayName: CharSequence?,
        /** @hide */
        val credentialTypeIcon: Icon?,
        /** @hide */
        val preferDefaultProvider: String?,
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

        /**
         * Constructs a [DisplayInfo].
         *
         * @param userId the user id of the created credential
         * @param userDisplayName an optional display name in addition to the [userId] that may be
         * displayed next to the `userId` during the user consent to help your user better
         * understand the credential being created
         * @param preferDefaultProvider the preferred default provider component name to prioritize in the
         * selection UI flows. Your app must have the permission
         * android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS to specify this, or it
         * would not take effect. Also this bit may not take effect for Android API level 33 and
         * below, depending on the pre-34 provider(s) you have chosen.
         * @throws IllegalArgumentException If [userId] is empty
         */
        constructor(
            userId: CharSequence,
            userDisplayName: CharSequence?,
            preferDefaultProvider: String?
        ) : this(
            userId,
            userDisplayName,
            null,
            preferDefaultProvider,
        )

        init {
            require(userId.isNotEmpty()) { "userId should not be empty" }
        }

        /** @hide */
        @RequiresApi(23)
        fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putCharSequence(BUNDLE_KEY_USER_ID, userId)
            if (!TextUtils.isEmpty(userDisplayName)) {
                bundle.putCharSequence(BUNDLE_KEY_USER_DISPLAY_NAME, userDisplayName)
            }
            if (!TextUtils.isEmpty(preferDefaultProvider)) {
                bundle.putString(BUNDLE_KEY_DEFAULT_PROVIDER, preferDefaultProvider)
            }
            // Today the type icon is determined solely within this library right before the
            // request is passed into the framework. Later if needed a new API can be added for
            // custom SDKs to supply their own credential type icons.
            return bundle
        }

        /** @hide */
        companion object {
            /** @hide */
            const val BUNDLE_KEY_REQUEST_DISPLAY_INFO =
                "androidx.credentials.BUNDLE_KEY_REQUEST_DISPLAY_INFO"

            @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
            /** @hide */
            const val BUNDLE_KEY_USER_ID =
                "androidx.credentials.BUNDLE_KEY_USER_ID"

            @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
            /** @hide */
            const val BUNDLE_KEY_USER_DISPLAY_NAME =
                "androidx.credentials.BUNDLE_KEY_USER_DISPLAY_NAME"

            /** @hide */
            const val BUNDLE_KEY_CREDENTIAL_TYPE_ICON =
                "androidx.credentials.BUNDLE_KEY_CREDENTIAL_TYPE_ICON"

            /** @hide */
            const val BUNDLE_KEY_DEFAULT_PROVIDER =
                "androidx.credentials.BUNDLE_KEY_DEFAULT_PROVIDER"

            /**
             * Returns a RequestDisplayInfo from a `credentialData` Bundle, or otherwise `null` if
             * parsing fails.
             *
             * @hide
             */
            @JvmStatic
            @RequiresApi(23)
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

    /** @hide */
    companion object {
        internal const val BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS =
            "androidx.credentials.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS"
        /** @hide */
        const val BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED =
            "androidx.credentials.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED"

        /**
         * Attempts to parse the raw data into one of [CreatePasswordRequest],
         * [CreatePublicKeyCredentialRequest], and
         * [CreateCustomCredentialRequest]. Otherwise returns null.
         *
         * @hide
         */
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
                    credentialData.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                    origin,
                    credentialData.getBoolean(
                        BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS, false),
                )
            }
        }
    }
}
