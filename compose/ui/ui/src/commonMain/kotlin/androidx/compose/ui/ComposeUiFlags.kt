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

import androidx.compose.ui.node.findNearestAncestor
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
 *      -assumevalues class androidx.compose.ui.ComposeUiFlags {
 *          public static int isRectTrackingEnabled return false
 *      }
 */
@ExperimentalComposeUiApi
object ComposeUiFlags {
    /**
     * This enables fixes for View focus. The changes are large enough to require a flag to allow
     * disabling them.
     */
    @field:Suppress("MutableBareField") @JvmField var isViewFocusFixEnabled: Boolean = false

    /**
     * This flag enables an alternate approach to fixing the issues addressed by the
     * [isViewFocusFixEnabled] flag.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isBypassUnfocusableComposeViewEnabled: Boolean = true

    /** Enable initial focus when a focusable is added to a screen with no focusable content. */
    @field:Suppress("MutableBareField")
    @JvmField
    var isInitialFocusOnFocusableAvailable: Boolean = false

    /**
     * Enable focus restoration, by always saving focus. This flag depends on
     * [isInitialFocusOnFocusableAvailable] also being true.
     */
    @field:Suppress("MutableBareField") @JvmField var isFocusRestorationEnabled: Boolean = false

    /** Flag for enabling indirect pointer event navigation gestures in Compose. */
    @field:Suppress("MutableBareField")
    @JvmField
    var isIndirectPointerNavigationGestureDetectorEnabled: Boolean = true

    /** Flag enables optimized focus change dispatching logic. */
    @field:Suppress("MutableBareField")
    @JvmField
    var isOptimizedFocusEventDispatchEnabled: Boolean = true

    /** This flag enables setting the shape semantics property in the graphicsLayer modifiers. */
    @field:Suppress("MutableBareField")
    @JvmField
    var isGraphicsLayerShapeSemanticsEnabled: Boolean = true

    /**
     * Enables a fix where [TraversableNode] traversal method [findNearestAncestor] will take into
     * consideration any delegates that might also be traversable.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isTraversableDelegatesFixEnabled: Boolean = true

    /**
     * Enables a change where off-screen children of the partially visible merging nodes (e.g. a
     * Text node of a Button) inside scrollable container are now also reported in the semantics
     * tree for Accessibility needs.
     *
     * Enabled is correct, and it should be enabled in all apps.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isAccessibilityShouldIncludeOffscreenChildrenEnabled: Boolean = true

    /**
     * Enables support of trackpad gesture events.
     *
     * If enabled, [androidx.compose.ui.input.pointer.PointerEvent]s can have type of
     * [androidx.compose.ui.input.pointer.PointerEventType.Pan] and
     * [androidx.compose.ui.input.pointer.PointerEventType.Scale], corresponding to gestures on a
     * trackpad.
     *
     * These trackpad gestures will also generally be treated as mouse, with the exact behavior
     * depending on platform specifics.
     */
    // TODO: b/475634969 remove the temporary flag
    @field:Suppress("MutableBareField")
    @JvmField
    var isTrackpadGestureHandlingEnabled: Boolean = true
}
