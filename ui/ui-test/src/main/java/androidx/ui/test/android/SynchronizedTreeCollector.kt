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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsNode
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Collects all [SemanticsTreeProvider]s that are part of the currently visible window.
 *
 * This operation is performed only after compose is idle via Espresso.
 */
internal object SynchronizedTreeCollector {
    /** ViewAssertion that asserts nothing (any View is valid) */
    private val noChecks = ViewAssertion { _, _ -> }

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
        val rootForwarder = RootForwarder()

        // Use Espresso to iterate over all roots and find all SemanticsTreeProviders
        // We can't use onView(instanceOf(SemanticsTreeProvider::class.java)) as Espresso throws
        // on multiple instances in the tree.
        Espresso.onView(isRoot())
            .inRoot(rootForwarder)
            .check(noChecks)

        if (!rootForwarder.foundRoots) {
            throw IllegalArgumentException("No root views found. Is your Activity resumed?")
        }

        val semanticTreeProviders = rootForwarder.collectSemanticTreeProviders()
        if (semanticTreeProviders.isEmpty()) {
            throw IllegalArgumentException(
                "Couldn't find a Compose root in the view hierarchy. Are you using Compose in " +
                        "your Activity?"
            )
        }
        return CollectedProviders(semanticTreeProviders)
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

    /** Scans the entire view hierarchy rooted at [view] for SemanticsTreeProviders */
    private fun collectSemanticTreeProviders(view: View): Set<SemanticsTreeProvider> {
        val collectedRoots = mutableSetOf<SemanticsTreeProvider>()

        fun collectSemanticTreeProvidersInternal(view: View) {
            when (view) {
                is SemanticsTreeProvider -> collectedRoots.add(view)
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        collectSemanticTreeProvidersInternal(view.getChildAt(i))
                    }
                }
            }
        }

        collectSemanticTreeProvidersInternal(view)
        return collectedRoots
    }

    /** A hacky way to retrieve root views from Espresso matchers. */
    private class RootForwarder : BaseMatcher<Root>() {
        var isFirstRoot = true
        val rootViews = mutableListOf<View>()
        val foundRoots get() = rootViews.isNotEmpty()

        fun collectSemanticTreeProviders(): Set<SemanticsTreeProvider> {
            return rootViews.fold(mutableSetOf(), { acc, view ->
                acc.also { it.addAll(collectSemanticTreeProviders(view)) }
            })
        }

        override fun describeTo(description: Description?) {
            description?.appendText("Root iterator")
        }

        override fun matches(item: Any?): Boolean {
            var useRoot = false
            if (item is Root) {
                val view = item.decorView.findViewById<View>(android.R.id.content) ?: return false
                rootViews.add(view)
                if (isFirstRoot) {
                    useRoot = true
                    isFirstRoot = false
                }
            }
            return useRoot
        }
    }
}

/**
 * There can be multiple Compose views in Android hierarchy and we want to interact with all of
 * them. This class merges all the semantics trees into one, hiding the fact that the API might
 * be interacting with several Compose roots.
 */
internal data class CollectedProviders(val treeProviders: Set<SemanticsTreeProvider>) {
    // Recursively search for the Activity context through (possible) ContextWrappers
    private val Context.activity: Activity?
        get() {
            return when (this) {
                is Activity -> this
                is ContextWrapper -> this.baseContext.activity
                else -> null
            }
        }

    fun findActivity(): Activity {
        treeProviders.forEach {
            if (it is View) {
                val activity = it.context.activity
                if (activity != null) {
                    return activity
                }
            }
        }
        throw AssertionError(
            "Out of ${treeProviders.size} SemanticsTreeProviders, none were attached to an Activity"
        )
    }

    fun getAllSemanticNodes(): List<SemanticsNode> {
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
