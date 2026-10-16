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

package androidx.glance.adaptive.core.ui.action

import android.app.Activity
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.util.BundleUtils

/** Defines an interaction triggered when a user taps a widget element. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class GlanceWidgetAction internal constructor() {

    /**
     * Invokes [actionCallbackClass] in the background with [parameters] when tapped.
     *
     * @param actionCallbackClass [GlanceAdaptiveActionCallback] implementation class with a public
     *   no-arg constructor
     * @param parameters optional [Bundle] arguments passed to
     *   [GlanceAdaptiveActionCallback.onAction]
     */
    public class Callback(
        public val actionCallbackClass: Class<out GlanceAdaptiveActionCallback>,
        parameters: Bundle = Bundle.EMPTY,
    ) : GlanceWidgetAction() {

        public val parameters: Bundle = Bundle(parameters)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Callback) return false
            return actionCallbackClass == other.actionCallbackClass &&
                BundleUtils.areBundlesEqual(parameters, other.parameters)
        }

        override fun hashCode(): Int {
            var result = actionCallbackClass.name.hashCode()
            result = 31 * result + BundleUtils.bundleHashCode(parameters)
            return result
        }

        override fun toString(): String =
            "GlanceWidgetAction.Callback(actionCallbackClass=${actionCallbackClass.name}, " +
                "parameters=$parameters)"
    }

    /**
     * Launches [targetActivityClass] with [parameters] as intent extras when tapped.
     *
     * @param targetActivityClass [Activity] class to start
     * @param parameters optional [Bundle] extras attached to the launch intent
     */
    public class LaunchActivity(
        public val targetActivityClass: Class<out Activity>,
        parameters: Bundle = Bundle.EMPTY,
    ) : GlanceWidgetAction() {

        public val parameters: Bundle = Bundle(parameters)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LaunchActivity) return false
            return targetActivityClass == other.targetActivityClass &&
                BundleUtils.areBundlesEqual(parameters, other.parameters)
        }

        override fun hashCode(): Int {
            var result = targetActivityClass.name.hashCode()
            result = 31 * result + BundleUtils.bundleHashCode(parameters)
            return result
        }

        override fun toString(): String =
            "GlanceWidgetAction.LaunchActivity(targetActivityClass=${targetActivityClass.name}, " +
                "parameters=$parameters)"
    }

    /**
     * Opens [uri] via an `ACTION_VIEW` activity intent when tapped.
     *
     * @param uri deep link or web URI string to open
     */
    public class LaunchDeeplink(public val uri: String) : GlanceWidgetAction() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LaunchDeeplink) return false
            return uri == other.uri
        }

        override fun hashCode(): Int = uri.hashCode()

        override fun toString(): String = "GlanceWidgetAction.LaunchDeeplink(uri=$uri)"
    }
}
