/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.android

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.test.InputDispatcher
import androidx.ui.test.SemanticsPredicate
import androidx.ui.test.SemanticsTreeInteraction
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import androidx.ui.unit.toRect

/**
 * Android specific implementation of [SemanticsTreeInteraction].
 *
 * Important highlight is that this implementation is using Espresso underneath to find the current
 * [Activity] that is visible on screen. So it does not rely on any references on activities being
 * held by your tests.
 */
internal class AndroidSemanticsTreeInteraction internal constructor(
    selector: SemanticsPredicate
) : SemanticsTreeInteraction(selector) {

    private val handler = Handler(Looper.getMainLooper())

    override fun performAction(action: (SemanticsTreeProvider) -> Unit) {
        val collectedInfo = SynchronizedTreeCollector.collectSemanticsProviders()

        handler.post(object : Runnable {
            override fun run() {
                collectedInfo.treeProviders.forEach {
                    action.invoke(it)
                }
            }
        })

        // Since we have our idling resource registered into Espresso we can leave from here
        // before synchronizing. It can however happen that if a developer needs to perform assert
        // on some variable change (e.g. click set the right value) they will fail unless they run
        // that assert as part of composeTestRule.runOnIdleCompose { }. Obviously any shared
        // variable should not be asserted from other thread but if we would waitForIdle here we
        // would mask lots of these issues.
    }

    override fun sendInput(action: (InputDispatcher) -> Unit) {
        action(AndroidInputDispatcher(SynchronizedTreeCollector.collectSemanticsProviders()))
    }

    override fun getAllSemanticsNodes(): List<SemanticsNode> {
        return SynchronizedTreeCollector.collectSemanticsProviders().getAllSemanticNodes()
    }

    override fun isInScreenBounds(rectangle: PxBounds): Boolean {
        val displayMetrics = SynchronizedTreeCollector.collectSemanticsProviders()
            .findActivity()
            .resources
            .displayMetrics

        val bottomRight = PxPosition(
            displayMetrics.widthPixels.px,
            displayMetrics.heightPixels.px
        )
        return rectangle.top >= 0.px &&
                rectangle.left >= 0.px &&
                rectangle.right <= bottomRight.x &&
                rectangle.bottom <= bottomRight.y
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun captureNodeToBitmap(node: SemanticsNode): Bitmap {
        val collectedInfo = SynchronizedTreeCollector.collectSemanticsProviders()

        val exists = contains(node.id)
        if (!exists) {
            throw AssertionError("The required node is no longer in the tree!")
        }

        val window = collectedInfo.findActivity().window

        // TODO(pavlis): Consider doing assertIsDisplayed here. Will need to move things around.

        // TODO(pavlis): Make sure that the Activity actually hosts the view. As in case of popup
        // it wouldn't. This will require us rewriting the structure how we collect the nodes.

        // TODO(pavlis): Add support for popups. So if we find composable hosted in popup we can
        // grab its reference to its window (need to add a hook to popup).

        return captureRegionToBitmap(node.globalBounds.toRect(), handler, window)
    }
}