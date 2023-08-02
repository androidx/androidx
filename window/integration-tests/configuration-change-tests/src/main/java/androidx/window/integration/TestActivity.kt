/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.window.integration

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * An [Activity] that handles some configuration changes and provides helper methods to
 * synchronize when the configuration has changed.
 */
class TestActivity : ComponentActivity(), View.OnLayoutChangeListener {
    private var layoutLatch = CountDownLatch(1)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView = View(this)
        setContentView(contentView)
        window.decorView.addOnLayoutChangeListener(this)
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        layoutLatch.countDown()
    }

    override fun onResume() {
        super.onResume()
        resumeLatch.countDown()
    }

    /**
     * Resets layout counter when waiting for a layout before calling [.waitForLayout].
     */
    fun resetLayoutCounter() {
        layoutLatch = CountDownLatch(1)
    }

    /**
     * Blocks and waits for the next layout.
     * [.resetLayoutCounter] must be called before this method.
     * @return {@code true} if the layout happened before the timeout count reaches zero and
     * {@code false} if the waiting time finishes before the layout occurs.
     */
    fun waitForLayout(): Boolean {
        return try {
            layoutLatch.await(3, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            false
        }
    }

    companion object {
        private var resumeLatch = CountDownLatch(1)

        /**
         * Resets layout counter when waiting for a layout before calling [.waitForOnResume].
         */
        @JvmStatic
        fun resetResumeCounter() {
            resumeLatch = CountDownLatch(1)
        }

        /**
         * Waits for onResume() to be called for any activity of this class.
         * This can be used to track activity re-creation.
         * @return {@code true} if the onResume() happened before the timeout counter reaches zero
         * and {@code false} if the waiting time finishes before the onResume() happens.
         */
        @JvmStatic
        fun waitForOnResume(): Boolean {
            return try {
                resumeLatch.await(3, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                false
            }
        }
    }
}
