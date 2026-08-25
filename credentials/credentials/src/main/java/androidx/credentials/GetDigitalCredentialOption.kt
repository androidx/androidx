/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.IntDef
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.internal.RequestValidationHelper

/**
 * A request to retrieve the user's digital credential, normally used for verification or sign-in
 * purpose.
 *
 * Note that this option cannot be combined with other types of options in a single
 * [GetCredentialRequest].
 *
 * @property requestJson the request in the JSON format; the latest format is defined at
 *   https://wicg.github.io/digital-credentials/#the-digitalcredentialrequestoptions-dictionary
 * @property uiWarningLevelHint the warning level hint indicating how the credential selector UI
 *   should be presented; defaults to [UI_WARNING_LEVEL_HINT_NO_ISSUES] where the standard Android
 *   Credential Selector is displayed. Set this to an elevated level if the request asks for
 *   sensitive user information so that Android displays additional warnings in the UI
 */
@ExperimentalDigitalCredentialApi
class GetDigitalCredentialOption
internal constructor(
    val requestJson: String,
    val uiWarningLevelHint: @UiWarningLevelHint Int,
    requestData: Bundle,
    candidateQueryData: Bundle,
    isSystemProviderRequired: Boolean,
    isAutoSelectAllowed: Boolean,
    allowedProviders: Set<ComponentName>,
    typePriorityHint: @PriorityHints Int,
) :
    CredentialOption(
        type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
        requestData = requestData,
        candidateQueryData = candidateQueryData,
        isSystemProviderRequired = isSystemProviderRequired,
        isAutoSelectAllowed = isAutoSelectAllowed,
        allowedProviders = allowedProviders,
        typePriorityHint = typePriorityHint,
    ) {

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "credentialJson must not be empty, and must be a valid JSON"
        }
    }

    /**
     * Constructs a `GetDigitalCredentialOption`.
     *
     * Note that this option cannot be combined with other types of options in a single
     * [GetCredentialRequest].
     *
     * @param requestJson the request in the JSON format; the latest format is defined at
     *   https://wicg.github.io/digital-credentials/#the-digitalcredentialrequestoptions-dictionary
     * @param uiWarningLevelHint the warning level hint indicating how the credential selector UI
     *   should be presented; defaults to [UI_WARNING_LEVEL_HINT_NO_ISSUES] where the standard
     *   Android Credential Selector is displayed. Set this to an elevated level if the request asks
     *   for sensitive user information so that Android displays additional warnings in the UI
     * @throws IllegalArgumentException if the `credentialJson` is not a valid json
     */
    @JvmOverloads
    constructor(
        requestJson: String,
        uiWarningLevelHint: @UiWarningLevelHint Int = UI_WARNING_LEVEL_HINT_NO_ISSUES,
    ) : this(
        requestJson = requestJson,
        uiWarningLevelHint = uiWarningLevelHint,
        requestData = toBundle(requestJson, uiWarningLevelHint),
        candidateQueryData = Bundle(),
        isSystemProviderRequired = false,
        isAutoSelectAllowed = false,
        allowedProviders = emptySet(),
        typePriorityHint = PRIORITY_PASSKEY_OR_SIMILAR,
    )

    /** UI warning level hints for [GetDigitalCredentialOption]. */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                UI_WARNING_LEVEL_HINT_NO_ISSUES,
                UI_WARNING_LEVEL_HINT_CAUTION,
                UI_WARNING_LEVEL_HINT_HIGH_RISK,
            ]
    )
    internal annotation class UiWarningLevelHint

    /** Companion constants / helpers for [GetDigitalCredentialOption]. */
    companion object {
        /**
         * Indicates that the standard Android Credential Selector UI should be displayed with no
         * additional warnings.
         */
        const val UI_WARNING_LEVEL_HINT_NO_ISSUES = 0
        /**
         * Indicates a request is asking for an unusually high amount of sensitive user information,
         * or data that falls outside typical usage patterns.
         *
         * The UI should render additional warnings highlighting the sensitive data being requested.
         */
        const val UI_WARNING_LEVEL_HINT_CAUTION = 1

        /**
         * Indicates strong signals that the request may be fraudulent, malicious, or highly
         * dangerous to the user.
         *
         * The UI should present maximum friction, clearly alerting the user of the severe risk. It
         * should strongly discourage the user from proceeding.
         */
        const val UI_WARNING_LEVEL_HINT_HIGH_RISK = 2

        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        internal const val BUNDLE_KEY_UI_WARNING_LEVEL_HINT =
            "androidx.credentials.BUNDLE_KEY_UI_WARNING_LEVEL_HINT"

        @JvmStatic
        internal fun toBundle(
            requestJson: String,
            uiWarningLevelHint: @UiWarningLevelHint Int,
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putInt(BUNDLE_KEY_UI_WARNING_LEVEL_HINT, uiWarningLevelHint)
            return bundle
        }

        @JvmStatic
        internal fun createFrom(
            requestData: Bundle,
            candidateQueryData: Bundle,
            requireSystemProvider: Boolean,
            allowedProviders: Set<ComponentName>,
        ): GetDigitalCredentialOption {
            try {
                val requestJson = requestData.getString(BUNDLE_KEY_REQUEST_JSON)!!
                val uiWarningLevelHint =
                    requestData.getInt(
                        BUNDLE_KEY_UI_WARNING_LEVEL_HINT,
                        UI_WARNING_LEVEL_HINT_NO_ISSUES,
                    )
                return GetDigitalCredentialOption(
                    requestJson = requestJson,
                    uiWarningLevelHint = uiWarningLevelHint,
                    requestData = requestData,
                    candidateQueryData = candidateQueryData,
                    isSystemProviderRequired = requireSystemProvider,
                    isAutoSelectAllowed =
                        requestData.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                    allowedProviders = allowedProviders,
                    typePriorityHint =
                        requestData.getInt(
                            BUNDLE_KEY_TYPE_PRIORITY_VALUE,
                            PRIORITY_PASSKEY_OR_SIMILAR,
                        ),
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
