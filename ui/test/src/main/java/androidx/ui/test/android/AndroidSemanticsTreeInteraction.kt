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

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.Recomposer
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.SemanticsTreeInteraction
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Android specific implementation of [SemanticsTreeInteraction].
 *
 * Important highlight is that this implementation is using Espresso underneath to find the current
 * [Activity] that is visible on screen. So it does not rely on any references on activities being
 * held by your tests.
 *
 * @param throwOnRecomposeTimeout Will throw exception if waiting for recomposition timeouts.
 */
internal class AndroidSemanticsTreeInteraction internal constructor(
    private val throwOnRecomposeTimeOut: Boolean,
    private val selector: SemanticsConfiguration.() -> Boolean
) : SemanticsTreeInteraction() {

    /**
     * Whether after the latest performed action we waited for any pending changes in composition.
     * This is used in internal tests to verify that actions that are supposed to mutate the
     * hierarchy as really observed like that.
     */
    internal var hadPendingChangesAfterLastAction = false

    // we should not wait more than two frames, but two frames can be much more
    // than 32ms when we skip a few, so "better" 10x number should work here
    private val defaultRecomposeWaitTimeMs = 1000L

    private val handler = Handler(Looper.getMainLooper())

    override fun findAllMatching(): List<SemanticsNodeInteraction> {
        waitForIdleCompose()

        return findActivityAndTreeProvider()
            .treeProvider
            .getAllSemanticNodes()
            .map {
                SemanticsNodeInteraction(it, this)
            }
            .filter { node ->
                node.semanticsTreeNode.data.selector()
            }
    }

    override fun findOne(): SemanticsNodeInteraction {
        val foundNodes = findAllMatching()

        if (foundNodes.size != 1) {
            // TODO(b/133217292)
            throw AssertionError("Found '${foundNodes.size}' nodes but exactly '1' was expected!")
        }

        return foundNodes.first()
    }

    private fun performAction(action: (SemanticsTreeProvider) -> Unit) {
        val collectedInfo = findActivityAndTreeProvider()

        handler.post(object : Runnable {
            override fun run() {
                action.invoke(collectedInfo.treeProvider)
            }
        })

        // It might seem we don't need to wait for idle here as every query and action we make
        // already waits for idle before being executed. However since Espresso could be mixed in
        // these tests we rather wait to be recomposed before we leave this method to avoid
        // potential flakes.
        hadPendingChangesAfterLastAction = waitForIdleCompose()
    }

    /**
     * Waits for Compose runtime to be idle - meaning it has no pending changes.
     *
     * @return Whether the method had to wait for pending changes or not.
     */
    private fun waitForIdleCompose(): Boolean {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw Exception("Cannot be run on UI thread.")
        }

        var hadPendingChanges = false
        val latch = CountDownLatch(1)
        handler.post(object : Runnable {
            override fun run() {
                hadPendingChanges = Recomposer.hasPendingChanges()
                if (hadPendingChanges) {
                    scheduleIdleCheck(latch)
                } else {
                    latch.countDown()
                }
            }
        })
        val succeeded = latch.await(defaultRecomposeWaitTimeMs, TimeUnit.MILLISECONDS)
        if (throwOnRecomposeTimeOut && !succeeded) {
            throw RecomposeTimeOutException()
        }
        return hadPendingChanges
    }

    private fun scheduleIdleCheck(latch: CountDownLatch) {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            @SuppressLint("SyntheticAccessor")
            override fun doFrame(frameTimeNanos: Long) {
                if (Recomposer.hasPendingChanges()) {
                    scheduleIdleCheck(latch)
                } else {
                    latch.countDown()
                }
            }
        })
    }

    override fun sendClick(x: Float, y: Float) {
        performAction { treeProvider ->
            val eventDown = MotionEvent.obtain(
                SystemClock.uptimeMillis(), 10,
                MotionEvent.ACTION_DOWN, x, y, 0
            )
            treeProvider.sendEvent(eventDown)
            eventDown.recycle()

            val eventUp = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                10,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
            )
            treeProvider.sendEvent(eventUp)
            eventUp.recycle()
        }
    }

    override fun contains(semanticsConfiguration: SemanticsConfiguration): Boolean {
        waitForIdleCompose()

        return findActivityAndTreeProvider()
            .treeProvider
            .getAllSemanticNodes()
            .any { it.data == semanticsConfiguration }
    }

    private fun findActivityAndTreeProvider(): CollectedInfo {
        val viewForwarder = ViewForwarder()

        // Use Espresso to find the content view for us.
        // We can't use onView(instanceOf(SemanticsTreeProvider::class.java)) as Espresso throws
        // on multiple instances in the tree.
        Espresso.onView(
            ViewMatchers.withId(
                R.id.content
            )
        ).check(viewForwarder)

        if (viewForwarder.viewFound == null) {
            throw IllegalArgumentException("Couldn't find a Compose root in the view hierarchy. " +
                    "Are you using Compose in your Activity?")
        }

        val view = viewForwarder.viewFound!! as ViewGroup
        return CollectedInfo(view.context, collectSemanticTreeProviders(view))
    }

    private fun collectSemanticTreeProviders(
        contentViewGroup: ViewGroup
    ): AggregatedSemanticTreeProvider {
        val collectedRoots = mutableSetOf<SemanticsTreeProvider>()

        fun collectSemanticTreeProvidersInternal(parent: ViewGroup) {
            for (index in 0 until parent.childCount) {
                when (val child = parent.getChildAt(index)) {
                    is SemanticsTreeProvider -> collectedRoots.add(child)
                    is ViewGroup -> collectSemanticTreeProvidersInternal(child)
                    else -> { }
                }
            }
        }

        collectSemanticTreeProvidersInternal(contentViewGroup)
        return AggregatedSemanticTreeProvider(
            collectedRoots
        )
    }

    /**
     * There can be multiple Compose views in Android hierarchy and we want to interact with all of
     * them. This class merges all the semantics trees into one, hiding the fact that the API might
     * be interacting with several Compose roots.
     */
    private class AggregatedSemanticTreeProvider(
        private val collectedRoots: Set<SemanticsTreeProvider>
    ) : SemanticsTreeProvider {

        override fun getAllSemanticNodes(): List<SemanticsTreeNode> {
            // TODO(pavlis): Once we have a tree support we will just add a fake root parent here
            return collectedRoots.flatMap { it.getAllSemanticNodes() }
        }

        override fun sendEvent(event: MotionEvent) {
            // TODO(pavlis): This is not good.
            collectedRoots.first().sendEvent(event)
        }
    }

    /** A hacky way to retrieve views from Espresso matchers. */
    private class ViewForwarder : ViewAssertion {
        var viewFound: View? = null

        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            viewFound = view
        }
    }

    internal class RecomposeTimeOutException
        : Throwable("Waiting for recompose has exceeded the timeout!")

    private data class CollectedInfo(
        val context: Context,
        val treeProvider: SemanticsTreeProvider
    )
}