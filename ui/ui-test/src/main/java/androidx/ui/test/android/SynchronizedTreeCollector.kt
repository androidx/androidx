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

import android.view.Choreographer
import androidx.compose.onCommit
import androidx.test.espresso.Espresso
import androidx.ui.core.AndroidOwner
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.getAllSemanticsNodes
import androidx.ui.test.isOnUiThread
import androidx.ui.test.runOnUiThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Collects all [SemanticsNode]s that are part of all compose hierarchies hosted by resumed
 * activities.
 *
 * This operation is performed only after compose is idle via Espresso.
 */
internal object SynchronizedTreeCollector {
    /**
     * Collects all [SemanticsNode]s from all compose hierarchies. Can only be used when using
     * [ComposeTestRule][androidx.ui.test.ComposeTestRule].
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case Espresso hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun collectAllSemanticsNodes(): List<SemanticsNode> {
        ensureAndroidOwnerRegistryIsSetUp()

        // TODO(pavlis): Instead of returning a flatMap, let all consumers handle a tree
        //  structure. In case of multiple AndroidOwners, add a fake root
        waitForIdle()

        return AndroidOwnerRegistry.getOwners().also {
            // TODO(b/153632210): This check should be done by callers of collectOwners
            check(it.isNotEmpty()) { "No AndroidOwners found. Is your Activity resumed?" }
        }.flatMap { it.semanticsOwner.getAllSemanticsNodes() }
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
        // First wait for Android mechanisms to settle down
        Espresso.onIdle()
        // Then wait until we have an AndroidOwner (in case an Activity is being started)
        waitForAndroidOwners()
        // And when we have an AndroidOwner, we need to wait until it has composed
        Espresso.onIdle()

        // TODO(b/155774664): waitForAndroidOwners() may be satisfied by an AndroidOwner from an
        //  Activity that is about to be paused, in cases where a new Activity is being started.
        //  That means that AndroidOwnerRegistry.getOwners() may still return an empty list
        //  between now and when the new Activity has created its AndroidOwner, even though
        //  waitForAndroidOwners() suggests that we are now guaranteed one.

        // Wait for onCommit callbacks last, as they might be posted while waiting for idle
        waitForOnCommitCallbacks()
    }

    private fun ensureAndroidOwnerRegistryIsSetUp() {
        check(AndroidOwnerRegistry.isSetUp) {
            "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
                    "with composables"
        }
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

    private fun waitForAndroidOwners() {
        ensureAndroidOwnerRegistryIsSetUp()

        fun hasAndroidOwners(): Boolean = AndroidOwnerRegistry.getOwners().isNotEmpty()

        if (!hasAndroidOwners()) {
            val latch = CountDownLatch(1)
            val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
                override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
                    if (hasAndroidOwners()) {
                        latch.countDown()
                    }
                }
            }
            try {
                AndroidOwnerRegistry.addOnRegistrationChangedListener(listener)
                if (!hasAndroidOwners()) {
                    latch.await(2, TimeUnit.SECONDS)
                }
            } finally {
                AndroidOwnerRegistry.removeOnRegistrationChangedListener(listener)
            }
        }
    }
}
