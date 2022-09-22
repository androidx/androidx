/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLParser
import androidx.constraintlayout.core.parser.CLParsingException
import androidx.constraintlayout.core.state.TransitionParser
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class MotionParserTest {
    @get:Rule
    val rule = createComposeRule()

    var displaySize: IntSize = IntSize.Zero

    // region sizing tests

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
        displaySize = ApplicationProvider
            .getApplicationContext<Context>().resources.displayMetrics.let {
                IntSize(it.widthPixels, it.heightPixels)
            }
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @OptIn(ExperimentalMotionApi::class)
    @Test
    fun testTransitionParseFailsSilently() {
        // We don't want applications to hard-crash when the parser sees an error
        var coreTransition = androidx.constraintlayout.core.state.Transition()
        val transitionContent = """
            {
              from: "start",
              to: "end",
              KeyFrames: {
                KeyAttributes: [{
                  target: ['id1'],
                  frames: [10, 20],
                  alpha: [0.5,],
                }],
              }
            }
        """.trimIndent()
        // Parsing transition throws an exception but the Composable should not crash the app
        assertFailsWith<CLParsingException> {
            TransitionParser.parse(CLParser.parse(transitionContent), coreTransition) { dp -> dp }
        }
        coreTransition = androidx.constraintlayout.core.state.Transition()
        rule.setContent {
            val transition = Transition(content = transitionContent)
            MotionLayout(
                modifier = Modifier.size(100.dp),
                start = ConstraintSet(
                    """
                    {
                      id1: {
                        width: 10, height: 10,
                        centerVertically: 'parent', start: ['parent', 'start', 0]
                      }
                    }
                """.trimIndent()
                ),
                end = ConstraintSet(
                    """
                    {
                      id1: {
                        width: 10, height: 10,
                        centerVertically: 'parent', end: ['parent', 'end', 0]
                      }
                    }
                """.trimIndent()
                ),
                transition = transition,
                progress = 0f
            ) {
                Box(modifier = Modifier.layoutId("id1"))
            }
        }
        // Test should finish without exceptions
        rule.waitForIdle()
    }
}