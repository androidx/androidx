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

package androidx.ui.test

import android.view.MotionEvent
import androidx.test.filters.SmallTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if the [AndroidInputDispatcher.sendClick] gesture works
 */
@SmallTest
@RunWith(Parameterized::class)
class AndroidInputDispatcherSendClickTest(private val config: TestConfig) {
    data class TestConfig(
        val x: Float,
        val y: Float
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(0f, 10f, -10f, 1000000f).flatMap { x ->
                listOf(0f, 10f, -10f, 1000000f).map { y ->
                    TestConfig(x, y)
                }
            }
        }
    }

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val eventPeriod = 10L
    private val x get() = config.x
    private val y get() = config.y

    private lateinit var recorder: MotionEventRecorder
    private lateinit var subject: AndroidInputDispatcher

    @Before
    fun setUp() {
        recorder = MotionEventRecorder()
        subject = AndroidInputDispatcher(recorder.asCollectedProviders())
    }

    @After
    fun tearDown() {
        recorder.clear()
    }

    @Test
    fun testClick() {
        subject.sendClick(x, y)
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            assertThat(size).isEqualTo(2)
            assertThat(first().action).isEqualTo(MotionEvent.ACTION_DOWN)
            assertThat(last().action).isEqualTo(MotionEvent.ACTION_UP)
            assertThat(first().x).isEqualTo(x)
            assertThat(first().y).isEqualTo(y)
            assertThat(last().x).isEqualTo(x)
            assertThat(last().y).isEqualTo(y)
            assertThat(last().eventTime - first().eventTime).isEqualTo(eventPeriod)
        }
    }
}
