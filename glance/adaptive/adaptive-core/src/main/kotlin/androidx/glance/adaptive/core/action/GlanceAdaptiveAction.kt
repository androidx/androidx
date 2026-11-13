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

package androidx.glance.adaptive.core.action

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.glance.adaptive.core.util.BundleUtils

/**
 * Base class for user interaction actions attached to interactive
 * [androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate] elements.
 *
 * Subclasses ([CallbackAction], [LaunchAction], [DeeplinkAction]) describe the action to perform
 * when the user taps an element, and the library translates it into the appropriate host trigger
 * for each target surface.
 */
public abstract class GlanceAdaptiveAction internal constructor() {

    /**
     * Invokes [actionCallbackClass] in the background with [parameters] when tapped.
     *
     * @property actionCallbackClass [GlanceAdaptiveActionCallback] implementation class with a
     *   public zero-argument constructor.
     * @property parameters optional [Bundle] arguments passed to
     *   [GlanceAdaptiveActionCallback.onAction].
     */
    public class CallbackAction(
        public val actionCallbackClass: Class<out GlanceAdaptiveActionCallback>,
        parameters: Bundle = Bundle.EMPTY,
    ) : GlanceAdaptiveAction() {

        public val parameters: Bundle =
            if (parameters === Bundle.EMPTY) Bundle.EMPTY else Bundle(parameters)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CallbackAction) return false
            return actionCallbackClass == other.actionCallbackClass &&
                BundleUtils.areBundlesEqual(parameters, other.parameters)
        }

        override fun hashCode(): Int {
            var result = actionCallbackClass.hashCode()
            result = 31 * result + BundleUtils.bundleHashCode(parameters)
            return result
        }

        override fun toString(): String =
            "GlanceAdaptiveAction.CallbackAction(actionCallbackClass=${actionCallbackClass.name}, " +
                "parameters=$parameters)"

        public companion object {
            /**
             * Creates a [CallbackAction] that invokes [T] in the background with [parameters] when
             * tapped.
             *
             * @param T [GlanceAdaptiveActionCallback] implementation class with a public
             *   zero-argument constructor.
             * @param parameters optional [Bundle] arguments passed to
             *   [GlanceAdaptiveActionCallback.onAction].
             */
            public inline operator fun <reified T : GlanceAdaptiveActionCallback> invoke(
                parameters: Bundle = Bundle.EMPTY
            ): CallbackAction = CallbackAction(T::class.java, parameters)
        }
    }

    /**
     * Launches [targetActivityClass] with [parameters] as intent extras when tapped.
     *
     * @property targetActivityClass [Activity] class to start, declared in the application's
     *   `AndroidManifest.xml`.
     * @property parameters optional [Bundle] extras attached to the launch intent.
     */
    public class LaunchAction(
        public val targetActivityClass: Class<out Activity>,
        parameters: Bundle = Bundle.EMPTY,
    ) : GlanceAdaptiveAction() {

        public val parameters: Bundle =
            if (parameters === Bundle.EMPTY) Bundle.EMPTY else Bundle(parameters)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LaunchAction) return false
            return targetActivityClass == other.targetActivityClass &&
                BundleUtils.areBundlesEqual(parameters, other.parameters)
        }

        override fun hashCode(): Int {
            var result = targetActivityClass.hashCode()
            result = 31 * result + BundleUtils.bundleHashCode(parameters)
            return result
        }

        override fun toString(): String =
            "GlanceAdaptiveAction.LaunchAction(targetActivityClass=${targetActivityClass.name}, " +
                "parameters=$parameters)"

        public companion object {
            /**
             * Creates a [LaunchAction] that starts [T] with [parameters] as intent extras when
             * tapped.
             *
             * @param T [Activity] class to start, declared in the application's
             *   `AndroidManifest.xml`.
             * @param parameters optional [Bundle] extras attached to the launch intent.
             */
            public inline operator fun <reified T : Activity> invoke(
                parameters: Bundle = Bundle.EMPTY
            ): LaunchAction = LaunchAction(T::class.java, parameters)
        }
    }

    /**
     * Opens [uri] via an [android.content.Intent.ACTION_VIEW] activity intent when tapped.
     *
     * @property uri deep link or web [Uri] to open.
     */
    public class DeeplinkAction(public val uri: Uri) : GlanceAdaptiveAction() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DeeplinkAction) return false
            return uri == other.uri
        }

        override fun hashCode(): Int = uri.hashCode()

        override fun toString(): String = "GlanceAdaptiveAction.DeeplinkAction(uri=$uri)"
    }
}
