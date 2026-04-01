/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.runtime.interfaces

import androidx.annotation.RestrictTo

/** Provides a way to request updates for XR services. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface XrServiceUpdateRequester {

    /** Supported service types. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public class ServiceType private constructor(private val value: Int) {
        public companion object {
            /** Projected service type. */
            @JvmField public val PROJECTED: ServiceType = ServiceType(0)
        }
    }

    /** Service type for the request. */
    public val serviceType: ServiceType

    /**
     * Requests update for an XR service.
     *
     * @return true if the request was accepted or false if the request was rejected
     */
    public suspend fun requestXrServiceUpdate(): Boolean
}
