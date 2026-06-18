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

package androidx.xr.compose.material3

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.LocalAnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.LocalThreePaneScaffoldOverride
import androidx.compose.material3.adaptive.navigationsuite.LocalNavigationSuiteScaffoldOverride
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * Clients can wrap their Compose hierarchy in this function to dynamically enable XR components
 * when in the proper environment.
 *
 * The [overrideEnabler] param determines whether each component will use an XR version.
 */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class, ExperimentalMaterial3Api::class)
@Composable
public fun EnableXrComponentOverrides(
    overrideEnabler: XrComponentOverrideEnabler = DefaultXrComponentOverrideEnabler,
    content: @Composable () -> Unit,
) {
    if (Build.VERSION.SDK_INT < 34) {
        content()
        return
    }
    val context = XrComponentOverrideEnablerContextImpl

    // Override CompositionLocals for all ComponentOverrides, as specified by the provided enabler.
    val componentOverrides =
        buildList<ProvidedValue<*>> {
            with(overrideEnabler) {
                val shouldOverrideNavigationSuiteScaffold =
                    context.shouldOverrideComponent(XrComponentOverride.NavigationSuiteScaffold)
                if (shouldOverrideNavigationSuiteScaffold) {
                    add(
                        LocalNavigationSuiteScaffoldOverride provides
                            XrNavigationSuiteScaffoldOverride
                    )
                }
                if (context.shouldOverrideComponent(XrComponentOverride.ThreePaneScaffold)) {
                    add(LocalThreePaneScaffoldOverride provides XrThreePaneScaffoldOverride)
                    add(LocalAnimatedPaneOverride provides XrAnimatedPaneOverride)
                }
            }
        }
    CompositionLocalProvider(values = componentOverrides.toTypedArray(), content = content)
}

/** Interface that a client can provide to enable/disable XR overrides on a per-component basis. */
@ExperimentalMaterial3XrApi
public interface XrComponentOverrideEnabler {
    /**
     * Used to determine whether the XR version of a given component should be used.
     *
     * @param component the component that may or may not use the XR version
     * @return whether the XR version of this component should be used
     */
    @Composable
    @ExperimentalMaterial3XrApi
    public fun XrComponentOverrideEnablerContext.shouldOverrideComponent(
        component: XrComponentOverride
    ): Boolean
}

/** Information about the current XR environment. */
@ExperimentalMaterial3XrApi
public sealed interface XrComponentOverrideEnablerContext {
    /** Whether the user is in an environment that supports XR spatialization. */
    @ExperimentalMaterial3XrApi @get:Composable public val isSpatializationEnabled: Boolean
}

/** The set of Material Components that can be overridden on XR. */
@ExperimentalMaterial3XrApi
@JvmInline
public value class XrComponentOverride private constructor(private val name: String) {
    public companion object {
        /** Material3 Adaptive NavigationSuiteScaffold. */
        @ExperimentalMaterial3XrApi
        public val NavigationSuiteScaffold: XrComponentOverride =
            XrComponentOverride("NavigationSuiteScaffold")

        /** Material3 Adaptive ThreePaneScaffold. */
        @ExperimentalMaterial3XrApi
        public val ThreePaneScaffold: XrComponentOverride = XrComponentOverride("ThreePaneScaffold")
    }
}

@ExperimentalMaterial3XrApi
private object XrComponentOverrideEnablerContextImpl : XrComponentOverrideEnablerContext {
    override val isSpatializationEnabled: Boolean
        @Composable get() = LocalSpatialCapabilities.current.isSpatialUiEnabled
}

@OptIn(ExperimentalMaterial3XrApi::class)
private object DefaultXrComponentOverrideEnabler : XrComponentOverrideEnabler {
    @Composable
    override fun XrComponentOverrideEnablerContext.shouldOverrideComponent(
        component: XrComponentOverride
    ): Boolean = isSpatializationEnabled
}
