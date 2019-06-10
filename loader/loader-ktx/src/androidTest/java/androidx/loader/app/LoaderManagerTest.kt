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

package androidx.loader.app

import android.content.Context
import androidx.loader.app.test.LoaderOwner
import androidx.loader.content.Loader
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class LoaderManagerTest {

    private lateinit var loaderManager: LoaderManager

    @Before
    fun setup() {
        loaderManager = LoaderManager.getInstance(LoaderOwner())
    }

    @Test
    fun testInitLoader() {
        val countDownLatch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            loaderManager.initLoader(0, StringLoader()) {
                countDownLatch.countDown()
            }
        }
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testInitLoaderWithReset() {
        val countDownLatch = CountDownLatch(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            loaderManager.initLoader(0, StringLoader(), { countDownLatch.countDown() }) {
                countDownLatch.countDown()
            }
            loaderManager.destroyLoader(0)
        }
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testRestartLoader() {
        val countDownLatch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            loaderManager.restartLoader(0, StringLoader()) {
                countDownLatch.countDown()
            }
        }
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    class StringLoader : Loader<String>(mock(Context::class.java)) {
        override fun onStartLoading() {
            deliverResult("test")
        }
    }
}
