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

package androidx.privacysandbox.ads.adservices.measurement

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.view.InputEvent
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.measurement.WebSourceParams.Companion.convertWebSourceParams

/**
 * Class to hold input to measurement source registration calls from web context.
 *
 * @param webSourceParams Registration info to fetch sources.
 * @param topOriginUri Top level origin of publisher.
 * @param inputEvent User Interaction {@link InputEvent} used by the AttributionReporting API to
 *   distinguish clicks from views.
 * @param appDestination App destination of the source. It is the android app {@link Uri} where
 *   corresponding conversion is expected. At least one of app destination or web destination is
 *   required.
 * @param webDestination Web destination of the source. It is the website {@link Uri} where
 *   corresponding conversion is expected. At least one of app destination or web destination is
 *   required.
 * @param verifiedDestination Verified destination by the caller. This is where the user actually
 *   landed.
 */
class WebSourceRegistrationRequest
public constructor(
    val webSourceParams: List<WebSourceParams>,
    val topOriginUri: Uri,
    val inputEvent: InputEvent? = null,
    val appDestination: Uri? = null,
    val webDestination: Uri? = null,
    val verifiedDestination: Uri? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSourceRegistrationRequest) return false
        return this.webSourceParams == other.webSourceParams &&
            this.webDestination == other.webDestination &&
            this.appDestination == other.appDestination &&
            this.topOriginUri == other.topOriginUri &&
            this.inputEvent == other.inputEvent &&
            this.verifiedDestination == other.verifiedDestination
    }

    override fun hashCode(): Int {
        var hash = webSourceParams.hashCode()
        hash = 31 * hash + topOriginUri.hashCode()
        if (inputEvent != null) {
            hash = 31 * hash + inputEvent.hashCode()
        }
        if (appDestination != null) {
            hash = 31 * hash + appDestination.hashCode()
        }
        if (webDestination != null) {
            hash = 31 * hash + webDestination.hashCode()
        }
        // Since topOriginUri is non-null.
        hash = 31 * hash + topOriginUri.hashCode()
        if (inputEvent != null) {
            hash = 31 * hash + inputEvent.hashCode()
        }
        if (verifiedDestination != null) {
            hash = 31 * hash + verifiedDestination.hashCode()
        }
        return hash
    }

    override fun toString(): String {
        val vals =
            "WebSourceParams=[$webSourceParams], TopOriginUri=$topOriginUri, " +
                "InputEvent=$inputEvent, AppDestination=$appDestination, " +
                "WebDestination=$webDestination, VerifiedDestination=$verifiedDestination"
        return "WebSourceRegistrationRequest { $vals }"
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices():
        android.adservices.measurement.WebSourceRegistrationRequest {
        return android.adservices.measurement.WebSourceRegistrationRequest.Builder(
                convertWebSourceParams(webSourceParams),
                topOriginUri
            )
            .setWebDestination(webDestination)
            .setAppDestination(appDestination)
            .setInputEvent(inputEvent)
            .setVerifiedDestination(verifiedDestination)
            .build()
    }

    /**
     * Builder for [WebSourceRegistrationRequest].
     *
     * @param webSourceParams source parameters containing source registration parameters, the list
     *   should not be empty
     * @param topOriginUri source publisher [Uri]
     */
    public class Builder(
        private val webSourceParams: List<WebSourceParams>,
        private val topOriginUri: Uri
    ) {
        private var inputEvent: InputEvent? = null
        private var appDestination: Uri? = null
        private var webDestination: Uri? = null
        private var verifiedDestination: Uri? = null

        /**
         * Setter for input event.
         *
         * @param inputEvent User Interaction InputEvent used by the AttributionReporting API to
         *   distinguish clicks from views.
         * @return builder
         */
        fun setInputEvent(inputEvent: InputEvent): Builder = apply { this.inputEvent = inputEvent }

        /**
         * Setter for app destination. It is the android app {@link Uri} where corresponding
         * conversion is expected. At least one of app destination or web destination is required.
         *
         * @param appDestination app destination [Uri]
         * @return builder
         */
        fun setAppDestination(appDestination: Uri?): Builder = apply {
            this.appDestination = appDestination
        }

        /**
         * Setter for web destination. It is the website {@link Uri} where corresponding conversion
         * is expected. At least one of app destination or web destination is required.
         *
         * @param webDestination web destination [Uri]
         * @return builder
         */
        fun setWebDestination(webDestination: Uri?): Builder = apply {
            this.webDestination = webDestination
        }

        /**
         * Setter for verified destination.
         *
         * @param verifiedDestination verified destination
         * @return builder
         */
        fun setVerifiedDestination(verifiedDestination: Uri?): Builder = apply {
            this.verifiedDestination = verifiedDestination
        }

        /** Pre-validates parameters and builds [WebSourceRegistrationRequest]. */
        fun build(): WebSourceRegistrationRequest {
            return WebSourceRegistrationRequest(
                webSourceParams,
                topOriginUri,
                inputEvent,
                appDestination,
                webDestination,
                verifiedDestination
            )
        }
    }
}
