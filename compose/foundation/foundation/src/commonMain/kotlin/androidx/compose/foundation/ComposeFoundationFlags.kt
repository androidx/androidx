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

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.graphicsLayer
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
     * Selecting flag to enable the change in Fling Propagation behavior in nested Scrollables. When
     * this is true, an ongoing fling that causes the scrollable container to hit the bounds will be
     * cancelled so the next scrollable in the chain can take over and fling with velocity left. We
     * are doing a flagged roll out of this behavior change. A node that is detached during a fling
     * will be treated as a node that hit its bounds, that is, it will cancel its fling and
     * propagate the remaining velocity through onPostFling.
     */
    @Suppress("MutableBareField") @JvmField var NewNestedFlingPropagationEnabled = true

    /**
     * We have removed the implicit [graphicsLayer] from [BasicText]. This also affects the `Text`
     * composable in material modules.
     *
     * This change ideally improves the initial rendering performance of [BasicText] but it may have
     * negative effect on recomposition or redraw since [BasicText]s draw operations would not be
     * cached in a separate layer.
     */
    @JvmField @Suppress("MutableBareField") var RemoveBasicTextGraphicsLayerEnabled: Boolean = true

    /**
     * Selecting flag to enable Drag Gesture "Pick-up" on drag gesture detectors. This also applies
     * to Draggables and Scrollables which use gesture detectors as well. Any parent drag detector
     * will continue to monitor the event stream until the gesture terminates (all pointers are
     * lifted), if a child gives up an event, the parent gesture detector will "pick-up" and
     * continue the gesture until all pointers are up.
     */
    @Suppress("MutableBareField") @JvmField var DragGesturePickUpEnabled = true
}
