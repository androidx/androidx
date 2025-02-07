/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionConstants
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class TestSandboxedUiAdapter(private val signalOptions: Set<String> = setOf("option")) :
    AbstractSandboxedUiAdapter() {
    var isSessionOpened = false
    var internalClient: SandboxedUiAdapter.SessionClient? = null
    var testSession: TestSession? = null
    var isZOrderOnTop = true
    var sessionConstants: SessionConstants? = null

    // When set to true, the onSessionOpened callback will only be invoked when specified
    // by the test. This is to test race conditions when the session is being loaded.
    var delayOpenSessionCallback = false
    private val openSessionLatch = CountDownLatch(1)
    private val resizeLatch = CountDownLatch(1)
    private val configChangedLatch = CountDownLatch(1)
    private val sessionClosedLatch = CountDownLatch(1)

    override fun openSession(
        context: Context,
        sessionConstants: SessionConstants,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        internalClient = client
        testSession = TestSession(context, signalOptions)
        clientExecutor.execute {
            if (!delayOpenSessionCallback) {
                client.onSessionOpened(testSession!!)
            }
            isSessionOpened = true
            this.isZOrderOnTop = isZOrderOnTop
            this.sessionConstants = sessionConstants
            openSessionLatch.countDown()
        }
    }

    internal fun sendOnSessionOpened() {
        internalClient?.onSessionOpened(testSession!!)
    }

    internal fun assertSessionOpened() {
        Truth.assertThat(
                openSessionLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
            )
            .isTrue()
    }

    internal fun assertSessionNotOpened() {
        Truth.assertThat(
                openSessionLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
            )
            .isFalse()
    }

    internal fun wasNotifyResizedCalled(): Boolean {
        return resizeLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
    }

    internal fun wasOnConfigChangedCalled(): Boolean {
        return configChangedLatch.await(
            SandboxedSdkViewTest.UI_INTENSIVE_TIMEOUT,
            TimeUnit.MILLISECONDS
        )
    }

    internal fun assertSessionNotClosed() {
        Truth.assertThat(
                sessionClosedLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
            )
            .isFalse()
    }

    internal fun assertSessionClosed() {
        Truth.assertThat(
                sessionClosedLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
            )
            .isTrue()
    }

    inner class TestSession(context: Context, override val signalOptions: Set<String>) :
        SandboxedUiAdapter.Session {
        var zOrderChangedLatch: CountDownLatch = CountDownLatch(1)
        var shortestGapBetweenUiChangeEvents = Long.MAX_VALUE
        private var notifyUiChangedLatch: CountDownLatch = CountDownLatch(1)
        private var latestUiChange: Bundle = Bundle()
        private var hasReceivedFirstUiChange = false
        private var timeReceivedLastUiChange = SystemClock.elapsedRealtime()
        override val view: View = View(context)

        fun requestResize(width: Int, height: Int) {
            internalClient?.onResizeRequested(width, height)
        }

        override fun notifyResized(width: Int, height: Int) {
            resizeLatch.countDown()
        }

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
            this@TestSandboxedUiAdapter.isZOrderOnTop = isZOrderOnTop
            zOrderChangedLatch.countDown()
        }

        override fun notifyConfigurationChanged(configuration: Configuration) {
            configChangedLatch.countDown()
        }

        override fun close() {
            sessionClosedLatch.countDown()
        }

        override fun notifyUiChanged(uiContainerInfo: Bundle) {
            if (hasReceivedFirstUiChange) {
                shortestGapBetweenUiChangeEvents =
                    java.lang.Long.min(
                        shortestGapBetweenUiChangeEvents,
                        SystemClock.elapsedRealtime() - timeReceivedLastUiChange
                    )
            }
            hasReceivedFirstUiChange = true
            timeReceivedLastUiChange = SystemClock.elapsedRealtime()
            latestUiChange = uiContainerInfo
            notifyUiChangedLatch.countDown()
        }

        fun assertNoSubsequentUiChanges() {
            notifyUiChangedLatch = CountDownLatch(1)
            Truth.assertThat(
                    notifyUiChangedLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
                )
                .isFalse()
        }

        /**
         * Performs the action specified in the Runnable, and waits for the next UI change.
         *
         * Throws an [AssertionError] if no UI change is reported.
         */
        fun runAndRetrieveNextUiChange(runnable: Runnable): SandboxedSdkViewUiInfo {
            notifyUiChangedLatch = CountDownLatch(1)
            runnable.run()
            Truth.assertThat(
                    notifyUiChangedLatch.await(SandboxedSdkViewTest.TIMEOUT, TimeUnit.MILLISECONDS)
                )
                .isTrue()
            return SandboxedSdkViewUiInfo.fromBundle(latestUiChange)
        }
    }
}
