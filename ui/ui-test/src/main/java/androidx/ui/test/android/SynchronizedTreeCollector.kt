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
import androidx.compose.runtime.onCommit
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResourceTimeoutException
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
    internal fun getAllSemanticsNodes(useUnmergedTree: Boolean): List<SemanticsNode> {
        ensureAndroidOwnerRegistryIsSetUp()

        // TODO(pavlis): Instead of returning a flatMap, let all consumers handle a tree
        //  structure. In case of multiple AndroidOwners, add a fake root
        waitForIdle()

        return AndroidOwnerRegistry.getOwners().also {
            // TODO(b/153632210): This check should be done by callers of collectOwners
            check(it.isNotEmpty()) { "No compose views found in the app. Is your Activity " +
                    "resumed?" }
        }.flatMap { it.semanticsOwner.getAllSemanticsNodes(useUnmergedTree) }
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
        check(!isOnUiThread()) {
            "Functions that involve synchronization (Assertions, Actions, Synchronization; " +
                    "e.g. assertIsSelected(), doClick(), runOnIdle()) cannot be run " +
                    "from the main thread. Did you nest such a function inside " +
                    "runOnIdle {}, runOnUiThread {} or setContent {}?"
        }

        registerComposeWithEspresso()
        // First wait for Android mechanisms to settle down
        runEspressoOnIdle()
        // Then wait until we have an AndroidOwner (in case an Activity is being started)
        waitForAndroidOwners()

        // TODO(b/155774664): waitForAndroidOwners() may be satisfied by an AndroidOwner from an
        //  Activity that is about to be paused, in cases where a new Activity is being started.
        //  That means that AndroidOwnerRegistry.getOwners() may still return an empty list
        //  between now and when the new Activity has created its AndroidOwner, even though
        //  waitForAndroidOwners() suggests that we are now guaranteed one.

        // And when we have an AndroidOwner, we need to wait until it has composed
        do {
            // First await the composition
            runEspressoOnIdle()
            // Than await onCommit callbacks, which are posted on the Choreographer
            waitForOnCommitCallbacks()
            // Keep doing this for as long as onCommit callbacks triggered new compositions
        } while (!ComposeIdlingResource.isIdle())
        // TODO(b/160399857): The above do-while loop works most of the times, but
        //  seems to still fail about 1% of the time, which is most likely explained
        //  by the race condition documented in waitForOnCommitCallbacks()
    }

    private fun runEspressoOnIdle() {
        fun rethrowWithMoreInfo(e: Throwable, wasMasterTimeout: Boolean) {
            var diagnosticInfo = ""
            val listOfIdlingResources = mutableListOf<String>()
            IdlingRegistry.getInstance().resources.forEach { resource ->
                if (resource is IdlingResourceWithDiagnostics) {
                    val message = resource.getDiagnosticMessageIfBusy()
                    if (message != null) {
                        diagnosticInfo += "$message \n"
                    }
                }
                listOfIdlingResources.add(resource.name)
            }
            if (diagnosticInfo.isNotEmpty()) {
                val prefix = if (wasMasterTimeout) {
                    "Global time out"
                } else {
                    "Idling resource timed out"
                }
                throw ComposeNotIdleException("$prefix: possibly due to compose being busy.\n" +
                        "$diagnosticInfo" +
                        "All registered idling resources: " +
                        "${listOfIdlingResources.joinToString(", ")}", e)
            }
            // No extra info, re-throw the original exception
            throw e
        }

        try {
            Espresso.onIdle()
        } catch (e: Throwable) {
            // Happens on the global time out, usually when master idling time out is less
            // or equal to dynamic idling time out or when the timeout is not due to individual
            // idling resource. This does not necessary mean that it can't be due to idling
            // resource being busy. So we try to check if it failed due to compose being busy and
            // add some extra information to the developer.
            val appNotIdleMaybe = tryToFindCause<AppNotIdleException>(e)
            if (appNotIdleMaybe != null) {
                rethrowWithMoreInfo(appNotIdleMaybe, wasMasterTimeout = true)
            }

            // Happens on idling resource taking too long. Espresso gives out which resources caused
            // it but it won't allow us to give any extra information. So we check if it was our
            // resource and give more info if we can.
            val resourceNotIdleMaybe = tryToFindCause<IdlingResourceTimeoutException>(e)
            if (resourceNotIdleMaybe != null) {
                rethrowWithMoreInfo(resourceNotIdleMaybe, wasMasterTimeout = false)
            }

            // No match, rethrow
            throw e
        }
    }

    /**
     * Tries to find if the given exception or any of its cause is of the type of the provided
     * throwable T. Returns null if there is no match. This is required as some exceptions end up
     * wrapped in Runtime or Concurrent exceptions.
     */
    private inline fun <reified T : Throwable> tryToFindCause(e: Throwable): Throwable? {
        var causeToCheck: Throwable? = e
        while (causeToCheck != null) {
            if (causeToCheck is T) {
                return causeToCheck
            }
            causeToCheck = causeToCheck.cause
        }
        return null
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
        // Race: between here and ..
        runOnUiThread {
            // .. here, the Choreographer may have just dispatched a frame, in which the onCommit
            // callbacks were executed. If those onCommits triggered a new composition X, that
            // composition is scheduled but not yet executed. Now we schedule our frame callback Y
            // in the line below, which will be the first one to execute on the next frame. Then
            // composition X is executed, which may schedule more onCommit callbacks. On the next
            // frame, Y is executed first, finishing our latch, waitForOnCommitCallbacks() ends
            // and ComposeIdlingResource.isIdle() is checked on the calling site. At this point,
            // Compose is considered idle because composition X is finished, and none of the
            // onCommit callbacks have executed yet that could trigger a new composition.
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

/**
 * Thrown in cases where Compose can't get idle in Espresso's defined time limit.
 */
class ComposeNotIdleException(message: String?, cause: Throwable?) : Throwable(message, cause)
