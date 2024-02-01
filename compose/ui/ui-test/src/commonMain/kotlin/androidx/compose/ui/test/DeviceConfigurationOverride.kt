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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Applies [DeviceConfigurationOverride] to the [content] under test to apply some configuration
 * override.
 *
 * This can be useful to locally test behavior in isolation that depends on properties that are
 * normally device-wide, such as [font scale][DeviceConfigurationOverride.Companion.FontScale],
 * [screen size][DeviceConfigurationOverride.Companion.ForcedSize] and
 * [layout direction][DeviceConfigurationOverride.Companion.LayoutDirection].
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideFontScaleSample
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideForcedSizeSample
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideLayoutDirectionSample
 */
@Composable
fun DeviceConfigurationOverride(
    override: DeviceConfigurationOverride,
    content: @Composable () -> Unit,
) = override.Override(content)

/**
 * The specification for an override applied to some piece of content.
 *
 * When wrapping content in [Override], some particular override will be locally applied to
 * the wrapped in order to test that content in isolation, without needing to configure the
 * entire device.
 */
fun interface DeviceConfigurationOverride {

    /**
     * A wrapper around [contentUnderTest] that applies some override.
     *
     * Implementations should call [contentUnderTest] exactly once.
     */
    // Composable parameters of Composable functions are normally slots for callers to inject
    // their content but this one is a special inverted-slot API. It's better to be explicit
    // with the naming.
    @Suppress("ComposableLambdaParameterNaming")
    @Composable
    fun Override(contentUnderTest: @Composable () -> Unit)

    companion object
}

/**
 * Combines this override with the [other] override into a single override, by
 * applying this override as the outer override first, then the [other] override as
 * an inner override, and then the content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideThenSample
 */
infix fun DeviceConfigurationOverride.then(
    other: DeviceConfigurationOverride
): DeviceConfigurationOverride =
    DeviceConfigurationOverride { contentUnderTest ->
        this.Override {
            other.Override {
                contentUnderTest()
            }
        }
    }

/**
 * A [DeviceConfigurationOverride] that overrides the available size for the contained content.
 *
 * This is only suitable for tests, since this will override [LocalDensity] to ensure that the
 * [size] is met (as opposed to `Modifier.requiredSize` which will result in clipping).
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideForcedSizeSample
 */
expect fun DeviceConfigurationOverride.Companion.ForcedSize(
    size: DpSize
): DeviceConfigurationOverride

/**
 * A [DeviceConfigurationOverride] that overrides the font scale for the contained content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideFontScaleSample
 */
expect fun DeviceConfigurationOverride.Companion.FontScale(
    fontScale: Float
): DeviceConfigurationOverride

/**
 * A [DeviceConfigurationOverride] that overrides the layout direction for the contained content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideLayoutDirectionSample
 */
expect fun DeviceConfigurationOverride.Companion.LayoutDirection(
    layoutDirection: LayoutDirection
): DeviceConfigurationOverride
