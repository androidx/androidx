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

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.SemanticsTreeProvider

/**
 * Collects all [SemanticsTreeProvider]s that are part of the currently visible window.
 *
 * This operation is performed only after compose is idle via Espresso.
 */
internal object SynchronizedTreeCollector {
    /**
     * Collects all [SemanticsTreeProvider]s that are part of the currently visible window.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case Espresso hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun collectSemanticsProviders(): CollectedProviders {
        ComposeIdlingResource.registerSelfIntoEspresso()
        val viewForwarder = ViewForwarder()

        // Use Espresso to find the content view for us.
        // We can't use onView(instanceOf(SemanticsTreeProvider::class.java)) as Espresso throws
        // on multiple instances in the tree.
        Espresso.onView(
            ViewMatchers.withId(
                android.R.id.content
            )
        ).check(viewForwarder)

        if (viewForwarder.viewFound == null) {
            throw IllegalArgumentException("Couldn't find a Compose root in the view hierarchy. " +
                    "Are you using Compose in your Activity?")
        }

        val view = viewForwarder.viewFound!! as ViewGroup
        return CollectedProviders(view.context, collectSemanticTreeProviders(view))
    }

    /**
     * Waits for compose to be idle.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case Espresso hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun waitForIdle() {
        ComposeIdlingResource.registerSelfIntoEspresso()
        Espresso.onIdle()
    }

    private fun collectSemanticTreeProviders(
        contentViewGroup: ViewGroup
    ): Set<SemanticsTreeProvider> {
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
        return collectedRoots
    }

    /** A hacky way to retrieve views from Espresso matchers. */
    private class ViewForwarder : ViewAssertion {
        var viewFound: View? = null

        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            viewFound = view
        }
    }
}

/**
 * There can be multiple Compose views in Android hierarchy and we want to interact with all of
 * them. This class merges all the semantics trees into one, hiding the fact that the API might
 * be interacting with several Compose roots.
 */
internal data class CollectedProviders(
    val context: Context,
    val treeProviders: Set<SemanticsTreeProvider>
) {
    fun getAllSemanticNodes(): List<SemanticsTreeNode> {
        // TODO(pavlis): Once we have a tree support we will just add a fake root parent here
        return treeProviders.flatMap { it.getAllSemanticNodes() }
    }

    fun sendEvent(event: MotionEvent) {
        // TODO: This seems wrong. Optimally this should be any { }. As any view that does not
        // handle the event should return false. However our AndroidComposeViews all return true
        // If we put any {} here it breaks MultipleComposeRootsTest.
        treeProviders.forEach { it.sendEvent(event) }
    }
}