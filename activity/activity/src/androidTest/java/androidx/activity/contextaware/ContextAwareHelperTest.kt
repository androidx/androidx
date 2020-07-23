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
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ContextAwareHelperTest {
    private val contextAware = TestContextAware()

    @Test
    fun addOnContextAvailableListener() {
        var receivedContextAware: ContextAware? = null
        val listener = OnContextAvailableListener { contextAware, _, _ ->
            receivedContextAware = contextAware
        }
        contextAware.addOnContextAvailableListener(listener)
        contextAware.dispatchOnContextAvailable()

        assertThat(receivedContextAware).isSameInstanceAs(contextAware)
    }

    @Test
    fun removeOnContextAvailableListener() {
        var callbackCount = 0
        val listener = OnContextAvailableListener { _, _, _ ->
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
            override fun onContextAvailable(
                contextAware: ContextAware,
                context: Context,
                savedInstanceState: Bundle?
            ) {
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
}

class TestContextAware : ContextAware {
    private val contextAwareHelper = ContextAwareHelper(this)

    override fun addOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.addOnContextAvailableListener(listener)
    }

    override fun removeOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.removeOnContextAvailableListener(listener)
    }

    fun dispatchOnContextAvailable(savedInstanceState: Bundle? = null) {
        contextAwareHelper.dispatchOnContextAvailable(
            ApplicationProvider.getApplicationContext(), savedInstanceState)
    }
}