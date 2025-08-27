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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.jvm.JvmField

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
 *              ComposeFoundationFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.runtime.ComposeFoundationFlags {
 *          public static boolean SomeFeatureEnabled return false
 *      }
 */
@ExperimentalFoundationApi
object ComposeFoundationFlags {
    /**
     * Whether to use more immediate coroutine dispatching in [detectTapGestures] and
     * [detectTapAndPress], true by default.
     */
    @Suppress("MutableBareField")
    @JvmField
    var isDetectTapGesturesImmediateCoroutineDispatchEnabled = true

    /**
     * Whether to use the new context menu API and default implementations in
     * [SelectionContainer][androidx.compose.foundation.text.selection.SelectionContainer], and all
     * [BasicTextField][androidx.compose.foundation.text.BasicTextField]s. If false, the previous
     * context menu that has no public APIs will be used instead.
     */
    @Suppress("MutableBareField") @JvmField var isNewContextMenuEnabled = true

    /**
     * Whether to use the new smart selection feature in
     * [androidx.compose.foundation.text.selection.SelectionContainer] and all
     * [androidx.compose.foundation.text.BasicTextField]s.
     */
    @Suppress("MutableBareField") @JvmField var isSmartSelectionEnabled = true

    /**
     * Selecting flag to enable the use of new PausableComposition in lazy layout prefetch. This
     * change allows us to distribute work we need to do during the prefetch better, for example we
     * can only perform the composition for parts of the LazyColumn's next item during one ui frame,
     * and then continue composing the rest of it in the next frames.
     */
    @Suppress("MutableBareField") @JvmField var isPausableCompositionInPrefetchEnabled = false

    /**
     * Flag that enables an optimized implementation for the [clickable] overload without an
     * [Indication] parameter. This also applies to [combinedClickable],
     * [androidx.compose.foundation.selection.selectable], and
     * [androidx.compose.foundation.selection.toggleable], which also use [clickable]. When this
     * flag is true, [clickable] will no longer use [androidx.compose.ui.composed], which leads to
     * improved performance and allows for composables with a [clickable] modifier to skip. However,
     * this means that only [IndicationNodeFactory] instances can be supported - if a
     * non-[IndicationNodeFactory] instance is provided to [LocalIndication], [clickable] will crash
     * at runtime. To resolve this either migrate the [Indication] implementation used to a
     * [IndicationNodeFactory], or use the other [clickable] overload with an explicit [Indication]
     * parameter - this flag can be disabled as a temporary migration aid.
     */
    @Suppress("MutableBareField") @JvmField var isNonComposedClickableEnabled = true

    /**
     * With this flag on we don't use suspend pointer input as part of Modifier.clickable
     * implementation as an optimization.
     */
    @Suppress("MutableBareField") @JvmField var isNonSuspendingPointerInputInClickableEnabled = true

    /**
     * With this flag on, the new BasicTextField and the other new TextFields that accept
     * [androidx.compose.foundation.text.input.TextFieldState] changes their behavior of how they
     * process arrow/Dpad keys received from input devices such as hardware keyboards, gamepads, or
     * TV remotes. The new behavior is to always move the cursor first until it reaches the start or
     * the end of the text. Then the TextField allows to move the focus to the next focusable
     * element on the screen.
     */
    @Suppress("MutableBareField") @JvmField var isTextFieldDpadNavigationEnabled = true

    /**
     * Controls the behavior of any scrollable container in terms of how it operates to keep the
     * focused child in view when the container's viewport shrinks. When this flag is enabled, the
     * scrollable polls the focused area among its descendants to find the most up-to-date area
     * which should be kept in view. Otherwise, each focused child notifies the scrollable of its
     * focus area through [onFocusedBoundsChanged].
     */
    @Suppress("MutableBareField")
    @JvmField
    var isKeepInViewFocusObservationChangeEnabled: Boolean = true

    /**
     * With this flag on we don't use suspend pointer input as part of drag related modifiers as an
     * optimization. This will affect [androidx.compose.foundation.gestures.scrollable],
     * [androidx.compose.foundation.gestures.draggable],
     * [androidx.compose.foundation.gestures.draggable2D] and
     * [androidx.compose.foundation.gestures.anchoredDraggable].
     */
    @Suppress("MutableBareField") @JvmField var isNonSuspendingPointerInputInDraggableEnabled = true

    /**
     * Used to enable support for 2 dimensional mouse wheel event in scrollable modifier and
     * scrollable components
     */
    @Suppress("MutableBareField") @JvmField var isMouseWheel1DAxisLockingEnabled: Boolean = true

    /**
     * With this flag on, Pager will use Cache Window as the default prefetching strategy, instead
     * of 1 item in the direction of the scroll. The window used will be 1 view port AFTER the
     * currently composed items, this includes visible and items composed through beyond bounds.
     */
    @Suppress("MutableBareField") @JvmField var isCacheWindowForPagerEnabled = true
}
