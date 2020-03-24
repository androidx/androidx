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
        val rootSearcher = RootSearcher()

        // Use Espresso to iterate over all roots and find all SemanticsTreeProviders
        // We can't use onView(instanceOf(SemanticsTreeProvider::class.java)) as Espresso throws
        // on multiple instances in the tree.
        Espresso.onView(isRoot())
            .inRoot(rootSearcher)
            .check(noChecks)

        require(rootSearcher.foundSemanticsTreeProviders) {
            "No SemanticsTreeProviders found. Is your Activity resumed?"
        }
        return CollectedProviders(rootSearcher.semanticsTreeProviders)
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

    /**
     * Root matcher that can be used in [inRoot][androidx.test.espresso.ViewInteraction.inRoot]
     * to search all [SemanticsTreeProvider]s that are ultimately attached to the window.
     */
    private class RootSearcher : BaseMatcher<Root>() {
        private var isFirstRoot = true
        private var resumedActivity: Activity? = null
        private val treeProviders = mutableSetOf<SemanticsTreeProvider>()

        val semanticsTreeProviders: Set<SemanticsTreeProvider> get() = treeProviders
        val foundSemanticsTreeProviders get() = treeProviders.isNotEmpty()

        override fun describeTo(description: Description?) {
            description?.appendText("Root iterator")
        }

        override fun matches(item: Any?): Boolean {
            var useRoot = false
            if (item is Root) {
                val view = item.decorView.findViewById<View>(android.R.id.content) ?: return false
                val hostActivity = view.context.getActivity()

                // TODO(b/151835993): Instead of finding out if the activity that hosts the view
                //  is resumed by making assumptions on the iteration order, collect all
                //  SemanticsTreeProviders, from them take the Owner (add owner: Owner to
                //  SemanticsTreeProvider), from them get the LifecycleOwner and find out if that
                //  is resumed. Then only add the SemanticsTreeProvider if the corresponding
                //  LifecycleOwner is resumed.

                if (resumedActivity == null) {
                    // While we don't enforce views have a LifecycleOwner yet,
                    // assume that the resumed activity is listed first
                    resumedActivity = hostActivity
                }

                if (hostActivity == resumedActivity) {
                    treeProviders.addAll(getTreeProviders(view))
                    if (isFirstRoot) {
                        useRoot = true
                        isFirstRoot = false
                    }
                }
            }
            return useRoot
        }

        private fun getTreeProviders(view: View): Set<SemanticsTreeProvider> {
            val treeProviders = mutableSetOf<SemanticsTreeProvider>()

            fun getOwnersRecursive(view: View) {
                when (view) {
                    is SemanticsTreeProvider -> treeProviders.add(view)
                    is ViewGroup -> {
                        repeat(view.childCount) { i ->
                            getOwnersRecursive(view.getChildAt(i))
                        }
                    }
                }
            }

            getOwnersRecursive(view)
            return treeProviders
        }
    }
}

// Recursively search for the Activity context through (possible) ContextWrappers
private fun Context.getActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.getActivity()
        else -> null
    }
}

/**
 * There can be multiple Compose views in Android hierarchy and we want to interact with all of
 * them. This class merges all the semantics trees into one, hiding the fact that the API might
 * be interacting with several Compose roots.
 */
internal data class CollectedProviders(val treeProviders: Set<SemanticsTreeProvider>) {
    fun findActivity(): Activity {
        treeProviders.forEach {
            if (it is View) {
                val activity = it.context.getActivity()
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
