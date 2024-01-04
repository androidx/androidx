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

package androidx.core.telecom.extensions

import androidx.annotation.RestrictTo
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.CountDownLatch

@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal class CapabilityExchangeListener() : ICapabilityExchangeListener.Stub() {
    internal val onCapabilitiesNegotiatedLatch = CountDownLatch(1)
    internal lateinit var negotiatedCapabilities: MutableList<Capability>

    override fun onCapabilitiesNegotiated(filteredCapabilities: MutableList<Capability>?) {
        filteredCapabilities?.let {
            negotiatedCapabilities = filteredCapabilities
            onCapabilitiesNegotiatedLatch.countDown()
        }
    }
}
