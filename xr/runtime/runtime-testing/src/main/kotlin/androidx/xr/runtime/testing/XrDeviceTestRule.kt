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

package androidx.xr.runtime.testing

import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory
import org.junit.rules.ExternalResource

/** JUnit Rule containing properties that affect the results of [XrDevice] capability APIs. */
public class XrDeviceTestRule : ExternalResource() {
    internal var capabilityProvider: FakeXrDeviceCapabilityProvider? = null

    /**
     * The result of [XrDevice.getPreferredDisplayBlendMode].
     *
     * Tests can set this property to control the value returned by
     * [XrDevice.getPreferredDisplayBlendMode] during the test execution.
     */
    public var preferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.ALPHA_BLEND
        set(value) {
            field = value
            capabilityProvider?.preferredDisplayBlendMode = value.toInternal()
        }

    internal fun DisplayBlendMode.toInternal() =
        when (this) {
            DisplayBlendMode.ALPHA_BLEND ->
                androidx.xr.runtime.interfaces.DisplayBlendMode.ALPHA_BLEND
            DisplayBlendMode.ADDITIVE -> androidx.xr.runtime.interfaces.DisplayBlendMode.ADDITIVE
            else -> androidx.xr.runtime.interfaces.DisplayBlendMode.NO_DISPLAY
        }

    override fun before() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = this
    }

    override fun after() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = null
    }
}
