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

package androidx.activity

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S_V2)
class OnBackPressedDispatcherInvokerTest {

    @Test
    fun testSimpleInvoker() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker = object : OnBackInvokedDispatcher {
            override fun registerOnBackInvokedCallback(p0: OnBackInvokedCallback, p1: Int) {
                registerCount++
            }

            override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                unregisterCount++
            }
        }

        val dispatcher = OnBackPressedDispatcher()

        dispatcher.setOnBackInvokedDispatcher(invoker)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.remove()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerEnableDisable() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker = object : OnBackInvokedDispatcher {
            override fun registerOnBackInvokedCallback(p0: OnBackInvokedCallback, p1: Int) {
                registerCount++
            }

            override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                unregisterCount++
            }
        }

        val dispatcher = OnBackPressedDispatcher()

        dispatcher.setOnBackInvokedDispatcher(invoker)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(2)
    }

    @Test
    fun testCallbackEnabledDisabled() {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                TODO("Not yet implemented")
            }
        }

        callback.isEnabled = true
        callback.isEnabled = false
    }

    @Test
    fun testInvokerAddDisabledCallback() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker = object : OnBackInvokedDispatcher {
            override fun registerOnBackInvokedCallback(p0: OnBackInvokedCallback, p1: Int) {
                registerCount++
            }

            override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                unregisterCount++
            }
        }

        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { }
        }

        val dispatcher = OnBackPressedDispatcher()

        dispatcher.setOnBackInvokedDispatcher(invoker)

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(0)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerAddEnabledCallbackBeforeSet() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker = object : OnBackInvokedDispatcher {
            override fun registerOnBackInvokedCallback(p0: OnBackInvokedCallback, p1: Int) {
                registerCount++
            }

            override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                unregisterCount++
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        }

        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(callback)

        dispatcher.setOnBackInvokedDispatcher(invoker)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }
}
