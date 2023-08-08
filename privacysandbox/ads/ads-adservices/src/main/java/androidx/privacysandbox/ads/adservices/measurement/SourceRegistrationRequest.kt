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

package androidx.privacysandbox.ads.adservices.measurement

import android.net.Uri
import android.view.InputEvent
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/**
 * Class to hold input to measurement source registration calls from web context.
 *
 * @param registrationUris [List] of Registration [Uri]s to fetch sources.
 * @param inputEvent User Interaction [InputEvent] used by the AttributionReporting API to
 * distinguish clicks from views.
 */
@ExperimentalFeatures.RegisterSourceOptIn
class SourceRegistrationRequest constructor(
    val registrationUris: List<Uri>,
    val inputEvent: InputEvent? = null
    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceRegistrationRequest) return false
        return this.registrationUris == other.registrationUris &&
            this.inputEvent == other.inputEvent
    }

    override fun hashCode(): Int {
        var hash = registrationUris.hashCode()
        if (inputEvent != null) {
            hash = 31 * hash + inputEvent.hashCode()
        }
        return hash
    }

    override fun toString(): String {
        val vals = "RegistrationUris=[$registrationUris], InputEvent=$inputEvent"
        return "AppSourcesRegistrationRequest { $vals }"
    }

    /**
     * Builder for [SourceRegistrationRequest].
     *
     * @param registrationUris source registration request [Uri]
     */
    class Builder(
        private val registrationUris: List<Uri>
    ) {
        private var inputEvent: InputEvent? = null

        /**
         * Setter for input event.
         *
         * @param inputEvent User Interaction InputEvent used by the AttributionReporting API to
         *     distinguish clicks from views.
         * @return builder
         */
        fun setInputEvent(inputEvent: InputEvent): Builder = apply {
            this.inputEvent = inputEvent
        }

        /** Pre-validates parameters and builds [SourceRegistrationRequest]. */
        fun build(): SourceRegistrationRequest {
            return SourceRegistrationRequest(
                registrationUris,
                inputEvent
            )
        }
    }
}
