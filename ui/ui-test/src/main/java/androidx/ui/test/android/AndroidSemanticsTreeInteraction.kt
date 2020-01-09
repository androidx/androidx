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
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.ui.core.PxPosition
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.px
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.engine.geometry.Rect
import androidx.ui.test.InputDispatcher
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.SemanticsTreeInteraction

/**
 * Android specific implementation of [SemanticsTreeInteraction].
 *
 * Important highlight is that this implementation is using Espresso underneath to find the current
 * [Activity] that is visible on screen. So it does not rely on any references on activities being
 * held by your tests.
 */
internal class AndroidSemanticsTreeInteraction internal constructor(
    private val selector: SemanticsConfiguration.() -> Boolean
) : SemanticsTreeInteraction {

    private val handler = Handler(Looper.getMainLooper())

    override fun findAllMatching(): List<SemanticsNodeInteraction> {
        return SynchronizedTreeCollector.collectSemanticsProviders()
            .getAllSemanticNodes()
            .map {
                SemanticsNodeInteraction(it, this)
            }
            .filter { node ->
                node.semanticsTreeNode.data.selector()
            }
    }

    override fun findOne(): SemanticsNodeInteraction {
        val foundNodes = SynchronizedTreeCollector.collectSemanticsProviders()
            .getAllSemanticNodes()
            .filter { node ->
                node.data.selector()
            }.toList()

        return SemanticsNodeInteraction(foundNodes, this)
    }

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

    override fun contains(semanticsConfiguration: SemanticsConfiguration): Boolean {
        return SynchronizedTreeCollector.collectSemanticsProviders()
            .getAllSemanticNodes()
            .any { it.data == semanticsConfiguration }
    }

    override fun isInScreenBounds(rectangle: Rect): Boolean {
        val displayMetrics = SynchronizedTreeCollector.collectSemanticsProviders()
            .context
            .resources
            .displayMetrics

        val bottomRight = PxPosition(
            displayMetrics.widthPixels.px,
            displayMetrics.heightPixels.px
        )
        val screenRect = Rect.fromLTWH(
            0.px.value,
            0.px.value,
            bottomRight.x.value,
            bottomRight.y.value
        )

        return screenRect.contains(rectangle.getTopLeft()) &&
                screenRect.contains(rectangle.getBottomRight())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun captureNodeToBitmap(node: SemanticsTreeNode): Bitmap {
        val collectedInfo = SynchronizedTreeCollector.collectSemanticsProviders()

        // TODO: Share this code with contains() somehow?
        val exists = collectedInfo
            .getAllSemanticNodes()
            .any { it.data == node.data }
        if (!exists) {
            throw AssertionError("The required node is no longer in the tree!")
        }

        // Recursively search for the Activity context through (possible) ContextWrappers
        fun Context.findActivity(): Activity {
            return when (this) {
                is Activity -> this
                is ContextWrapper -> this.baseContext.findActivity()
                else -> {
                    // TODO(pavlis): Espresso might have the windows already somewhere
                    //  internally ...
                    throw AssertionError(
                        "The context ($this) assigned to your composable holder view cannot " +
                                "be cast to Activity. So this function can't access its window " +
                                "to capture the bitmap. ${collectedInfo.context}"
                    )
                }
            }
        }

        val window = collectedInfo.context.findActivity().window

        // TODO(pavlis): Consider doing assertIsDisplayed here. Will need to move things around.

        // TODO(pavlis): Make sure that the Activity actually hosts the view. As in case of popup
        // it wouldn't. This will require us rewriting the structure how we collect the nodes.

        // TODO(pavlis): Add support for popups. So if we find composable hosted in popup we can
        // grab its reference to its window (need to add a hook to popup).

        return captureRegionToBitmap(node.globalRect!!, handler, window)
    }
}