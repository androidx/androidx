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
@file:JvmName("ComposeRuntimeFlags")

package androidx.compose.ui

import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

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
 *              ComposeUiFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.runtime.ComposeUiFlags {
 *          public static int isRectTrackingEnabled return false
 *      }
 */
@ExperimentalComposeUiApi
object ComposeUiFlags {
    /**
     * With this flag on, during layout we will do some additional work to store the minimum
     * bounding rectangles for all Layout Nodes. This introduces some additional maintenance burden,
     * but will be used in the future to enable certain features that are not possible to do
     * efficiently at this point, as well as speed up some other areas of the system such as
     * semantics, focus, pointer input, etc. If significant performance overhead is noticed during
     * layout phases, it is possible that the addition of this tracking is the culprit.
     */
    @Suppress("MutableBareField") @JvmField var isRectTrackingEnabled: Boolean = true

    /**
     * With this flag on, the new semantic version of Autofill APIs will be enabled. Turning this
     * flag off will disable the new Semantic Autofill APIs, and the new refactored semantics.
     */
    @Suppress("MutableBareField") @JvmField var isSemanticAutofillEnabled: Boolean = true

    /**
     * This enables fixes for View focus. The changes are large enough to require a flag to allow
     * disabling them.
     */
    @Suppress("MutableBareField") @JvmField var isViewFocusFixEnabled: Boolean = false

    /**
     * This flag enables an alternate approach to fixing the issues addressed by the
     * [isViewFocusFixEnabled] flag.
     */
    @Suppress("MutableBareField")
    @JvmField
    var isBypassUnfocusableComposeViewEnabled: Boolean = true

    /**
     * This flag enables a fix for b/378570682. For API >=26. We attempt to manually find the next
     * focusable item for 1-D focus search cases when Compose does not have any focusable content.
     */
    @Suppress("MutableBareField") @JvmField var isPre26FocusFinderFixEnabled: Boolean = false

    /**
     * This flag enables a fix for b/388590015. The view system ignores an invalid prevFocusRect
     * when requestFocus is called, so we support this behavior in Compose too.
     */
    @Suppress("MutableBareField") @JvmField var isIgnoreInvalidPrevFocusRectEnabled: Boolean = true

    /**
     * When an embedded view that is focused is removed from the hierarchy, it triggers a
     * requestFocus() which tries to re-assign focus before the previous composition is complete.
     * This flag enables a fix for this issue.
     */
    @Deprecated("This flag is no longer needed.")
    @Suppress("MutableBareField", "unused")
    @JvmField
    var isRemoveFocusedViewFixEnabled: Boolean = false

    /**
     * Enable WindowInsets rulers:
     * * `SystemBarsRulers`
     * * `ImeRulers`
     * * `StatusBarsRulers`
     * * `NavigationBarsRulers`
     * * `CaptionBarRulers`
     * * `MandatorySystemGesturesRulers`
     * * `TappableElementRulers`
     * * `WaterfallRulers`
     * * `SafeDrawingRulers`
     * * `SafeGesturesRulers`
     * * `SafeContentRulers`
     */
    // off for b/410868572
    @Suppress("MutableBareField") @JvmField var areWindowInsetsRulersEnabled = true

    /**
     * With this flag on, when an AccessibilityService performs ACTION_FOCUS on a Composable node,
     * if it is in touch mode, it will exit touch mode first, then try to request focus on the node.
     */
    @Deprecated("This flag is no longer needed.")
    @Suppress("MutableBareField", "unused")
    @JvmField
    var isFocusActionExitsTouchModeEnabled: Boolean = false

    /**
     * With this flag on, Modifier.focusRestorer() will not pin the item that needs to be restored.
     * Users are responsible for providing a key for the item that needs to be restored b/330696779.
     */
    @Deprecated("This flag is no longer needed.")
    @Suppress("MutableBareField", "unused")
    @JvmField
    var isNoPinningInFocusRestorationEnabled: Boolean = false

    /**
     * With this flag on, SubcomposeLayout will deactivate not used content slots outside of the
     * frame, not as part of a regular recomposition phase. It allows to not block the drawing
     * phase, improving the scrolling performance for lazy layouts.
     */
    @Suppress("MutableBareField") @JvmField var isOutOfFrameDeactivationEnabled: Boolean = true

    /** Enable clearing focus when a focused item is removed from a lazyList. */
    @Deprecated("This flag is no longer needed.")
    @Suppress("MutableBareField", "unused")
    @JvmField
    var isClearFocusOnResetEnabled: Boolean = false

    /**
     * With this flag on, the adaptive refresh rate (ARR) feature will be enabled. A preferred frame
     * rate can be set on a Composable through frame rate modifier: [Modifier.preferredFrameRate]
     */
    @Suppress("MutableBareField") @JvmField var isAdaptiveRefreshRateEnabled: Boolean = true

    /** Flag for enabling indirect touch event navigation gestures in Compose. */
    @Suppress("MutableBareField")
    @JvmField
    var isIndirectTouchNavigationGestureDetectorEnabled: Boolean = true

    /** This flag enables setting the shape semantics property in the graphicsLayer modifiers. */
    @Suppress("MutableBareField") @JvmField var isGraphicsLayerShapeSemanticsEnabled: Boolean = true

    /** Flag for enabling the performance optimization for content capture. */
    @Suppress("MutableBareField") @JvmField var isContentCaptureOptimizationEnabled: Boolean = true

    /**
     * Flag for enabling nested scroll interop fix for propagating integers, this fixes an issue
     * with interop between compose and views nested scroll where small deltas with the wrong sign
     * were being scaled up due to the rounding used.
     */
    @Suppress("MutableBareField")
    @JvmField
    var isNestedScrollInteropIntegerPropagationEnabled: Boolean = true

    /** This flag enables clearing focus on pointer down by default. */
    @Suppress("MutableBareField") @JvmField var isClearFocusOnPointerDownEnabled: Boolean = true
}
