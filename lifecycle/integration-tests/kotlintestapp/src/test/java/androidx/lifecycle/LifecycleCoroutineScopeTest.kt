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

package androidx.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * To ensure consistent behavior, we run these tests both on androidTest and test
 */
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class LifecycleCoroutineScopeTest : LifecycleCoroutineScopeTestBase() {
    private val mainExecutor = Executors.newSingleThreadExecutor()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
    }

    @After
    fun clearMainDispatcher() {
        mainExecutor.shutdownNow()
        mainExecutor.awaitTermination(10, TimeUnit.SECONDS)
        Dispatchers.resetMain()
    }
}
