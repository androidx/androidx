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

package androidx.privacysandbox.ads.adservices.signals

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/**
 * The request object for updateSignals.
 *
 * <p>{@code updateUri} is the only parameter. It represents the URI that the service will reach out
 * to retrieve the signals updates.
 */
@ExperimentalFeatures.Ext12OptIn
class UpdateSignalsRequest(val updateUri: Uri) {
    /** Checks whether two [UpdateSignalsRequest] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateSignalsRequest) return false
        return this.updateUri == other.updateUri
    }

    /** Returns the hash of the [UpdateSignalsRequest] object's data. */
    override fun hashCode(): Int {
        return updateUri.hashCode()
    }

    /** Return the string representation of the [UpdateSignalsRequest] */
    override fun toString(): String {
        return "UpdateSignalsRequest: updateUri=$updateUri"
    }
}
