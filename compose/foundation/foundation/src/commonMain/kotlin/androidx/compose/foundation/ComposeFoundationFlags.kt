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
 *      -assumevalues class androidx.compose.foundation.ComposeFoundationFlags {
 *          public static boolean SomeFeatureEnabled return false
 *      }
 */
@ExperimentalFoundationApi
object ComposeFoundationFlags {
    /**
     * Whether to use more immediate coroutine dispatching in [detectTapGestures] and
     * [detectTapAndPress], true by default.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isDetectTapGesturesImmediateCoroutineDispatchEnabled = true

    /**
     * Whether to use the new context menu API and default implementations in
     * [SelectionContainer][androidx.compose.foundation.text.selection.SelectionContainer], and all
     * [BasicTextField][androidx.compose.foundation.text.BasicTextField]s. If false, the previous
     * context menu that has no public APIs will be used instead.
     */
    @field:Suppress("MutableBareField") @JvmField var isNewContextMenuEnabled = true

    /**
     * Whether to use the new smart selection feature in
     * [androidx.compose.foundation.text.selection.SelectionContainer] and all
     * [androidx.compose.foundation.text.BasicTextField]s.
     */
    @field:Suppress("MutableBareField") @JvmField var isSmartSelectionEnabled = true

    /**
     * Selecting flag to enable the use of new PausableComposition in lazy layout prefetch. This
     * change allows us to distribute work we need to do during the prefetch better, for example we
     * can only perform the composition for parts of the LazyColumn's next item during one ui frame,
     * and then continue composing the rest of it in the next frames.
     */
    @field:Suppress("MutableBareField") @JvmField var isPausableCompositionInPrefetchEnabled = true

    /**
     * With this flag on, Pager will use Cache Window as the default prefetching strategy, instead
     * of 1 item in the direction of the scroll. The window used will be 1 view port AFTER the
     * currently composed items, this includes visible and items composed through beyond bounds.
     */
    @field:Suppress("MutableBareField") @JvmField var isCacheWindowForPagerEnabled = true

    /**
     * When Pager was used with a keyboard in RTL the pages would bounce indefinitely due to the
     * bring into view animation. If this flag is off the fix for that behavior will be disabled.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isBringIntoViewRltBouncyBehaviorInPagerFixEnabled: Boolean = true

    /**
     * If this flag is enabled, for lazy layout implementations that use
     * [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow], if the dataset changes, the
     * window mechanism will understand that it needs to re-fill the window from scratch. This is
     * because there is no good way for the window to know that a possible non-visible item has
     * changed. For instance, if C and D are 2 items in the cache window and later they're removed
     * from the dataset, the cache window won't know it until it tries to prefetch them.
     */
    @field:Suppress("MutableBareField") @JvmField var isCacheWindowRefillFixEnabled = true
}
