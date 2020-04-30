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
import android.view.Choreographer
import androidx.compose.onCommit
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.ui.core.AndroidOwner
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.getAllSemanticsNodes
import androidx.ui.test.isOnUiThread
import androidx.ui.test.runOnUiThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Collects all [AndroidOwner]s that are part of the currently visible window.
 *
 * This operation is performed only after compose is idle via Espresso.
 */
internal object SynchronizedTreeCollector {
    /**
     * Collects all [AndroidOwner]s that are part of the currently visible window. Can only be
     * used when using [ComposeTestRule][androidx.ui.test.ComposeTestRule]
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case Espresso hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun collectOwners(): CollectedOwners {
        waitForIdle()
        check(AndroidOwnerRegistry.isSetup) {
            "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
                    "with composables"
        }
        return CollectedOwners(AndroidOwnerRegistry.getAllOwners().filterTo(mutableSetOf()) {
            // lifecycleOwner can only be null if it.view is not yet attached, and since owners
            // are only in the registry when they're attached we don't care about the
            // lifecycleOwner being null.
            val lifecycleOwner = it.lifecycleOwner ?: return@filterTo false
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED
        }.also {
            // TODO(b/153632210): This check should be done by callers of collectOwners
            check(it.isNotEmpty()) { "No AndroidOwners found. Is your Activity resumed?" }
        })
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
        waitForOnCommitCallbacks()
    }

    /**
     * Waits for all scheduled [onCommit] callbacks to be executed.
     */
    private fun waitForOnCommitCallbacks() {
        require(!isOnUiThread())
        val latch = CountDownLatch(1)
        runOnUiThread {
            Choreographer.getInstance().postFrameCallbackDelayed({ latch.countDown() }, 1)
        }
        latch.await(1, TimeUnit.SECONDS)
    }
}

/**
 * There can be multiple Compose views in the Android hierarchy and we want to interact with all
 * of them. This class merges all the [AndroidOwner]s into one, hiding the fact that the API
 * might be interacting with several Compose roots.
 */
internal data class CollectedOwners(val owners: Set<AndroidOwner>) {
    // Recursively search for the Activity context through (possible) ContextWrappers
    private fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

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
