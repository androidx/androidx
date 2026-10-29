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
public object AndroidComposeUiFlags {

    /**
     * This flag enables using the View's handler for semantics processing instead of the Main
     * Looper. This avoids crashes in environments where Compose is used on a non-main thread.
     */
    // TODO remove me b/486998514
    @field:Suppress("MutableBareField")
    @JvmField
    public var isViewBasedSemanticsHandlerEnabled: Boolean = true

    /** This flag enables the Android Framework implementation of VelocityTracker. */
    // TODO: b/483449576
    @field:Suppress("MutableBareField")
    @JvmField
    public var isFrameworkVelocityTrackerEnabled: Boolean = false

    /** Guard for Hardware Navigation system default behavior in Compose. */
    // TODO(b/520209822): Cleanup once proven stable.
    @field:Suppress("MutableBareField")
    @JvmField
    public var isHardwareNavigationHandlingEnabled: Boolean = false

    /**
     * If enabled, the creation of the container for AndroidViews is delayed until an AndroidView is
     * added.
     */
    // TODO: b/529483648
    @field:Suppress("MutableBareField")
    @JvmField
    public var isDelayAndroidViewsHandlerCreationEnabled: Boolean = false

    /**
     * This flag forces scroll capture to center the content being rendered even if it's already
     * visible.
     */
    // TODO: remove and close b/509934021
    @field:Suppress("MutableBareField")
    @JvmField
    public var isAlwaysScrollDuringScrollCaptureEnabled: Boolean = true

    /** Enables using out of frame scheduler instead of Choreographer for text input events. */
    // TODO(b/513525072): Cleanup once proven stable.
    @field:Suppress("MutableBareField")
    @JvmField
    public var isOutOfFrameSchedulerForTextInputEventsEnabled: Boolean = true

    /**
     * This flag enables performance improvements in accessibility, such as caching accessibility
     * state and deferring listener registration.
     */
    // TODO: remove me b/529420099
    @field:Suppress("MutableBareField")
    @JvmField
    public var isAccessibilityPerformanceEnabled: Boolean = true

    /**
     * With this flag on, a node that gains input focus while it is outside the accessibility tree
     * (for example a lazy list item composed by BeyondBoundsLayout for keyboard navigation) or
     * while it is not visible to the user dispatches TYPE_VIEW_FOCUSED once it becomes visible,
     * typically after it has been scrolled into the viewport. This lets screen readers follow
     * keyboard focus into items that were off-screen when they were focused.
     */
    // TODO: b/447669491 - Remove this flag once the behavior has shipped.
    @field:Suppress("MutableBareField")
    @JvmField
    public var isScrollAccessibilityFocusEventEnabled: Boolean = true

    /**
     * Restricts touch-exploration hover forwarding to interop views, so that an embedded
     * AndroidView only receives hover events when it is the front-most hit, matching pointer input
     * z-ordering, instead of receiving them whenever the point falls within its View bounds.
     *
     * When disabled, hover is forwarded to the interop layer unconditionally, which allows a view
     * that is visually covered by Compose content to take accessibility focus.
     */
    // TODO: b/565033965 - Remove this flag once the behavior has rolled out and proven stable.
    @field:Suppress("MutableBareField")
    @JvmField
    internal var isInteropHoverZOrderEnabled: Boolean = true

    /**
     * If enabled, [android.R.id.accessibilityActionShowOnScreen] accounts for the scrollable
     * container's content padding (such as `LazyColumn(contentPadding = ...)`) when calculating the
     * scroll delta required to make a target node fully visible.
     */
    // TODO: b/568388723
    @field:Suppress("MutableBareField")
    @JvmField
    public var isShowOnScreenContentPaddingEnabled: Boolean = true

    /**
     * Enables dispatching nested scroll events from [androidx.compose.ui.viewinterop.AndroidView]
     * to parent Compose nested scroll connections when the root hosted `View` has
     * `isNestedScrollingEnabled` set to `false` by default (such as `FrameLayout` or
     * `FragmentContainerView` wrapping a scrolling child `View`).
     */
    // TODO(b/541378736): Cleanup feature flag once proven stable.
    @field:Suppress("MutableBareField")
    @JvmField
    public var isNestedScrollInteropViewHolderFixEnabled: Boolean = true

    /**
     * Whether to use Modifier.Node for [androidx.compose.ui.input.pointer.pointerInteropFilter]. If
     * false, the legacy Modifier.composed implementation will be used instead.
     */
    // TODO: b/573988321
    @field:Suppress("MutableBareField")
    @JvmField
    public var isPointerInteropFilterNodeEnabled: Boolean = true
}
