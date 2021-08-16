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

package androidx.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@SmallTest
class InputMergerFactoryTest {
    private lateinit var context: Context
    private lateinit var factory: InputMergerFactory
    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        factory = mock(InputMergerFactory::class.java)
        configuration = Configuration.Builder()
            .setInputMergerFactory(factory)
            .build()
    }

    @Test
    fun testInputMergerFactory() {
        val name = ArrayCreatingInputMerger::class.java.name
        val merger = configuration.inputMergerFactory.createInputMergerWithDefaultFallback(name)
        assertThat(merger, notNullValue())
        verify(factory, times(1)).createInputMerger(name)
    }

    @Test
    fun testInputMergerFactory2() {
        factory = object : InputMergerFactory() {
            override fun createInputMerger(className: String): InputMerger? {
                return OverwritingInputMerger()
            }
        }
        configuration = Configuration.Builder()
            .setInputMergerFactory(factory)
            .build()

        val name = ArrayCreatingInputMerger::class.java.name
        val merger = configuration.inputMergerFactory.createInputMergerWithDefaultFallback(name)
        val instanceCheck = merger is OverwritingInputMerger
        assertThat(merger, notNullValue())
        assertThat(instanceCheck, `is`(true))
    }
}
