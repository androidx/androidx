/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.contextaware

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ContextAwareHelperTest {
    private val contextAware = TestContextAware()

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun addOnContextAvailableListener() {
        var callbackCount = 0
        val listener = OnContextAvailableListener {
            callbackCount++
        }
        contextAware.addOnContextAvailableListener(listener)
        assertThat(contextAware.peekAvailableContext()).isNull()
        contextAware.dispatchOnContextAvailable()

        assertThat(callbackCount).isEqualTo(1)
        assertThat(contextAware.peekAvailableContext()).isNotNull()
    }

    @Test
    fun removeOnContextAvailableListener() {
        var callbackCount = 0
        val listener = OnContextAvailableListener {
            callbackCount++
        }
        contextAware.addOnContextAvailableListener(listener)
        contextAware.dispatchOnContextAvailable()

        assertThat(callbackCount).isEqualTo(1)

        // Now remove the listener and check that the count doesn't increase
        contextAware.removeOnContextAvailableListener(listener)
        contextAware.dispatchOnContextAvailable()

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun reentrantRemove() {
        var callbackCount = 0
        val listener = object : OnContextAvailableListener {
            override fun onContextAvailable(context: Context) {
                callbackCount++
                contextAware.removeOnContextAvailableListener(this)
            }
        }
        contextAware.addOnContextAvailableListener(listener)
        contextAware.dispatchOnContextAvailable()

        assertThat(callbackCount).isEqualTo(1)

        callbackCount = 0
        contextAware.dispatchOnContextAvailable()

        assertThat(callbackCount).isEqualTo(0)
    }

    @Test
    fun postAvailableAddOnContextAvailableListener() {
        contextAware.dispatchOnContextAvailable()
        var callbackCount = 0
        val listener = OnContextAvailableListener {
            callbackCount++
        }
        contextAware.addOnContextAvailableListener(listener)

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun postClearAddOnContextAvailableListener() {
        contextAware.dispatchOnContextAvailable()
        contextAware.clearAvailableContext()
        var callbackCount = 0
        val listener = OnContextAvailableListener {
            callbackCount++
        }
        contextAware.addOnContextAvailableListener(listener)

        assertThat(callbackCount).isEqualTo(0)
    }

    @Test
    fun alreadyAvailable() = runBlocking(Dispatchers.Main) {
        val contextAware = TestContextAware()
        contextAware.dispatchOnContextAvailable()
        var result = "initial"
        val receivedResult = contextAware.withContextAvailable {
            result
        }
        result = "after"
        assertThat(receivedResult).isEqualTo("initial")
    }

    @Test
    fun suspending() = runBlocking(Dispatchers.Main) {
        val contextAware = TestContextAware()
        var result = "initial"
        launch {
            contextAware.dispatchOnContextAvailable()
            result = "post dispatch"
        }
        val receivedResult = contextAware.withContextAvailable {
            result
        }
        contextAware.addOnContextAvailableListener {
            result = "after"
        }
        assertThat(receivedResult).isEqualTo("initial")
    }
}

class TestContextAware : ContextAware {
    private val contextAwareHelper = ContextAwareHelper()

    override fun peekAvailableContext() = contextAwareHelper.peekAvailableContext()

    override fun addOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.addOnContextAvailableListener(listener)
    }

    override fun removeOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.removeOnContextAvailableListener(listener)
    }

    fun dispatchOnContextAvailable() {
        contextAwareHelper.dispatchOnContextAvailable(
            ApplicationProvider.getApplicationContext()
        )
    }

    fun clearAvailableContext() {
        contextAwareHelper.clearAvailableContext()
    }
}
