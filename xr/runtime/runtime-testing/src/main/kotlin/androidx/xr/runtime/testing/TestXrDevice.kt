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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.runtime.XrDevice

/**
 * Represents a simulated [XrDevice] in a Robolectric test that uses [XrDeviceTestRule].
 *
 * Use [XrDeviceTestRule.createTestXrDevice] to simulate a device that is connected to the host
 * device. Use [context] to get the [XrDevice] of the simulated device, and the other properties to
 * check the values that its [XrDevice] APIs return.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TestXrDevice
internal constructor(
    /**
     * The [Context] associated with the simulated device.
     *
     * Pass this context to [XrDevice.getCurrentDevice] to get the [XrDevice] of the simulated
     * device. It is a device context, as returned by [Context.createDeviceContext], so
     * [Context.getDeviceId] returns the ID of the simulated device, and Android APIs that depend on
     * the device association of a context treat it as a context of that device.
     */
    public val context: Context,
    /** The ID of the display of the simulated device. */
    public val primaryDisplayId: Int,
)
