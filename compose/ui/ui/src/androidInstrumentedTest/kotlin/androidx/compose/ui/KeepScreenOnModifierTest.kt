/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class KeepScreenOnModifierTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var composeView: View

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun flagSetWhenAdded() {
        rule.setContent {
            Box(Modifier.keepScreenOn())
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }
    }

    @Test
    fun flagRemovedWhenRemoved() {
        var attach by mutableStateOf(true)

        rule.setContent {
            if (attach) {
                Box(Modifier.keepScreenOn())
            }
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        attach = false

        rule.runOnIdle { assertFalse(composeView.keepScreenOn) }
    }

    @Test
    fun flagRemainsWhenRemovedInParent() {
        var attach by mutableStateOf(true)

        rule.setContent {
            Box {
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                Box(Modifier.keepScreenOn())
            }
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        attach = false

        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }
    }

    @Test
    fun flagRemainsWhenRemovedInChild() {
        var attach by mutableStateOf(true)

        rule.setContent {
            Box {
                Box(Modifier.keepScreenOn())
                Box {
                    if (attach) {
                        Box(Modifier.keepScreenOn())
                    }
                }
            }
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        attach = false

        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }
    }

    @Test
    fun flagRemainsWhenRemovedInSibling() {
        var attach by mutableStateOf(true)

        rule.setContent {
            Box {
                Box(Modifier.keepScreenOn())
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        attach = false

        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }
    }

    @Test
    fun flagRemovedWhenAllRemoved() {
        var attach by mutableStateOf(true)

        rule.setContent {
            Box {
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
            composeView = LocalView.current
        }
        rule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        attach = false

        rule.runOnIdle { assertFalse(composeView.keepScreenOn) }
    }
}
