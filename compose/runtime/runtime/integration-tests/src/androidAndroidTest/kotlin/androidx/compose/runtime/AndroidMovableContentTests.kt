/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidMovableContentTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testMovableContentParameterInBoxWithConstraints() {
        var state by mutableStateOf(false)
        var lastSeen: Boolean? = null
        val content = movableContentOf { parameter: Boolean ->
            val content = @Composable {
                lastSeen = parameter
            }
            Container(content)
        }

        // Infrastructure to avoid throwing a failure on the UI thread.
        val failureReasons = mutableListOf<String>()
        fun failed(reason: String) {
            failureReasons.add(reason)
        }

        fun phase(description: String): Phase {
            return object : Phase {
                override fun expect(actual: Any?): Result {
                    return object : Result {
                        override fun toEqual(expected: Any?) {
                            if (expected != actual) {
                                failed("$description, expected $actual to be $expected")
                            }
                        }
                    }
                }
            }
        }

        compose {
            if (state) {
                content(true)
            } else {
                BoxWithConstraints {
                    content(false)
                }
            }
        }.then {
            phase("In initial composition").expect(lastSeen).toEqual(state)
            state = true
        }.then {
            phase("When setting state to true").expect(lastSeen).toEqual(state)
            state = false
        }.then {
            phase("When setting state to false").expect(lastSeen).toEqual(state)
        }.done()

        assertEquals("", failureReasons.joinToString())
    }
}

@Composable
fun Container(content: @Composable () -> Unit) {
    content()
}

interface Phase {
    fun expect(actual: Any?): Result
}

interface Result {
    fun toEqual(expected: Any?)
}