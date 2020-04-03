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
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.ui.core.AndroidOwner
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.getAllSemanticsNodes
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Collects all [AndroidOwner]s that are part of the currently visible window.
 *
 * This operation is performed only after compose is idle via Espresso.
 */
internal object SynchronizedTreeCollector {
    /** ViewAssertion that asserts nothing (any View is valid) */
    private val noChecks = ViewAssertion { _, _ -> }

    /**
     * Collects all [AndroidOwner]s that are part of the currently visible window.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case Espresso hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun collectOwners(): CollectedOwners {
        registerComposeWithEspresso()
        val rootSearcher = RootSearcher()

        // Use Espresso to iterate over all roots and find all SemanticsTreeProviders
        // We can't use onView(instanceOf(SemanticsTreeProvider::class.java)) as Espresso throws
        // on multiple instances in the tree.
        Espresso.onView(isRoot())
            .inRoot(rootSearcher)
            .check(noChecks)

        require(rootSearcher.owners.isNotEmpty()) {
            "No SemanticsTreeProviders found. Is your Activity resumed?"
        }
        return CollectedOwners(rootSearcher.owners)
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
        registerComposeWithEspresso()
        Espresso.onIdle()
    }

    /**
     * Root matcher that can be used in [inRoot][androidx.test.espresso.ViewInteraction.inRoot]
     * to search all [AndroidOwner]s that are ultimately attached to the window.
     */
    private class RootSearcher : BaseMatcher<Root>() {
        private var isFirstRoot = true
        private var resumedActivity: Activity? = null

        val owners = mutableSetOf<AndroidOwner>()

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
                    owners.addAll(getOwners(view))
                    if (isFirstRoot) {
                        useRoot = true
                        isFirstRoot = false
                    }
                }
            }
            return useRoot
        }

        private fun getOwners(view: View): Set<AndroidOwner> {
            val owners = mutableSetOf<AndroidOwner>()

            fun getOwnersRecursive(view: View) {
                when (view) {
                    is AndroidOwner -> owners.add(view)
                    is ViewGroup -> {
                        repeat(view.childCount) { i ->
                            getOwnersRecursive(view.getChildAt(i))
                        }
                    }
                }
            }

            getOwnersRecursive(view)
            return owners
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
 * There can be multiple Compose views in the Android hierarchy and we want to interact with all
 * of them. This class merges all the [AndroidOwner]s into one, hiding the fact that the API
 * might be interacting with several Compose roots.
 */
internal data class CollectedOwners(val owners: Set<AndroidOwner>) {
    fun findActivity(): Activity {
        owners.forEach {
            val activity = it.view.context.getActivity()
            if (activity != null) {
                return activity
            }
        }
        throw AssertionError(
            "Out of ${owners.size} Owners, none were attached to an Activity"
        )
    }

    fun getAllSemanticNodes(): List<SemanticsNode> {
        // TODO(pavlis): Once we have a tree support we will just add a fake root parent here
        return owners.flatMap { it.semanticsOwner.getAllSemanticsNodes() }
    }
}
