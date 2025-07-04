/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.privacysandbox.ondevicepersonalization.client

import android.content.ComponentName
import android.os.PersistableBundle

/**
 * The request of [OnDevicePersonalizationManager.executeInIsolatedService].
 *
 * @param service The component name of the service.
 * @param appParams Passed from the calling app to the Isolated Service. The expected contents of
 *   this parameter are defined by the service. The platform does not interpret this parameter.
 */
public class ExecuteInIsolatedServiceRequest(
    public val service: ComponentName,
    public val appParams: PersistableBundle = PersistableBundle.EMPTY,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ExecuteInIsolatedServiceRequest
        return this.service == that.service && this.appParams == that.appParams
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = 31 * hash + service.hashCode()
        hash = 31 * hash + appParams.hashCode()
        return hash
    }
}
