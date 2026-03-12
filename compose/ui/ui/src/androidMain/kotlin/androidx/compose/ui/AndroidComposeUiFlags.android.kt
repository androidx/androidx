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

package androidx.compose.ui

/**
 * This is a collection of flags which are used to guard against regressions in some of the
 * "riskier" refactors or new feature support that is added to this module. These flags are always
 * "on" in the published artifact of this module, however these flags allow end consumers of this
 * module to toggle them "off" in case this new path is causing a regression.
 *
 * These flags are considered temporary, and there should be no expectation for these flags be
 * around for an extended period of time. If you have a regression that one of these flags fixes, it
 * is strongly encouraged for you to file a bug ASAP.
 *
 * **Usage:**
 *
 * In order to turn a feature off in a debug environment, it is recommended to set this to false in
 * as close to the initial loading of the application as possible. Changing this value after compose
 * library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              AndroidComposeUiFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.ui.AndroidComposeUiFlags {
 *          public static int isSharedComposeViewContextEnabled return false
 *      }
 */
@ExperimentalComposeUiApi
object AndroidComposeUiFlags {
    /**
     * This flag enables ComposeViewContext to be created automatically and used across ComposeViews
     * within the same hierarchy. With the flag disabled, ComposeViewContext will only be created
     * when explicitly provided to a ComposeView.
     */
    // TODO: b/479834257
    @field:Suppress("MutableBareField")
    @JvmField
    var isSharedComposeViewContextEnabled: Boolean = true

    /** This moves WindowInfo into the shared ComposeViewContext. */
    // TODO: b/479837249
    @field:Suppress("MutableBareField") @JvmField var isSharedWindowInfoEnabled: Boolean = true

    /** This moves AccessibilityManager into the shared ComposeViewContext. */
    @field:Suppress("MutableBareField")
    @JvmField
    // TODO: b/479845566
    var isSharedAccessibilityManagerEnabled: Boolean = true

    /** This moves DrawScope and CanvasHolder into the shared ComposeViewContext. */
    // TODO: b/479849019
    @field:Suppress("MutableBareField") @JvmField var isSharedDrawingEnabled: Boolean = true

    /** This moves ViewConfiguration into the shared ComposeViewContext. */
    @field:Suppress("MutableBareField")
    @JvmField
    // TODO: b/479890645
    var isSharedViewConfigurationEnabled: Boolean = true

    /** This moves Clipboard-related instances into the shared ComposeViewContext. */
    @field:Suppress("MutableBareField")
    @JvmField
    // TODO: b/479895130
    var isSharedClipboardManagerEnabled: Boolean = true

    /**
     * This flag enables support for walking up nested scrolling in response to
     * android.R.id.accessibilityActionShowOnScreen from Accessibility.
     *
     * Enabled is correct nested scrolling behavior and it should be enabled in all apps.
     */
    // TODO: b/474650559
    @field:Suppress("MutableBareField")
    @JvmField
    var isAccessibilityShowOnScreenNestedScrollingEnabled: Boolean = true

    /** This moves Haptics-related instances into the shared ComposeViewContext. */
    // TODO: b/479895628
    @field:Suppress("MutableBareField") @JvmField var isSharedHapticsEnabled: Boolean = true

    /**
     * This flag enables using the View's handler for semantics processing instead of the Main
     * Looper. This avoids crashes in environments where Compose is used on a non-main thread.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    // TODO remove me b/486998514
    var isViewBasedSemanticsHandlerEnabled: Boolean = true

    /** This moves Font-related instances into the shared ComposeViewContext. */
    // TODO remove me b/479898293
    @field:Suppress("MutableBareField") @JvmField var isSharedFontEnabled: Boolean = true
}
