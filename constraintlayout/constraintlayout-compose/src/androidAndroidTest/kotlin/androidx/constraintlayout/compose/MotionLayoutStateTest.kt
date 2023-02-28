/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Language("json5")
private const val SCENE_WITH_BOX =
    """
{
  ConstraintSets: {
    start: {
      box: {
        width: 50, height: 50,
        start: ['parent', 'start', 10],
        top: ['parent', 'top', 10]
      }
    },
    end: {
      box: {
        width: 50, height: 50,
        end: ['parent', 'end', 10],
        top: ['parent', 'top', 10]
      }
    }
  },
  Transitions: {
    default: {
      from: 'start',
      to: 'end'
    }
  }
}
"""

/**
 * Run with Pixel 3 API 30.
 */
@MediumTest
@OptIn(ExperimentalMotionApi::class)
@RunWith(AndroidJUnit4::class)
class MotionLayoutStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun teardown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun stateProgressTestAndInvalidateWithKey() {
        var statePointer: MotionLayoutState? = null
        val myKey = mutableStateOf(0)
        rule.setContent {
            // Instantiate the MotionLayoutState with an initial progress
            val motionState = rememberMotionLayoutState(initialProgress = 0.5f, key = myKey.value)
            statePointer = motionState
            MotionLayout(
                modifier = Modifier.fillMaxSize(),
                motionLayoutState = motionState,
                motionScene = MotionScene(SCENE_WITH_BOX)
            ) {
                Box(modifier = Modifier.layoutId("box"))
            }
            // Text composable to track the progress
            Text(text = "Progress: ${(motionState.currentProgress * 100).toInt()}")
        }
        rule.waitForIdle()
        // The Text Composable with the initial progress
        rule.onNodeWithText("Progress: 50").assertExists()

        rule.runOnUiThread {
            // Send a command, outside the original Compose context to animate to 100%
            statePointer?.animateTo(1f, tween(100))
        }

        rule.waitForIdle()
        // The Text Composable with the last observed progress
        rule.onNodeWithText("Progress: 100").assertExists()

        rule.runOnUiThread {
            // Invalidate the state by changing the key
            myKey.value = 1
        }
        rule.waitForIdle()
        // The Text Composable with the progress value reset to initial (50%)
        rule.onNodeWithText("Progress: 50").assertExists()
    }
}