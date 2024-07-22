/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.Row
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(35)
class SensitiveContentModifierTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var androidComposeView: View

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun assertModifierEquals() {
        val modifier = Modifier.sensitiveContent()
        Assert.assertEquals(modifier, modifier)
        Assert.assertEquals(modifier, Modifier.sensitiveContent())
        Assert.assertNotEquals(modifier, Modifier.sensitiveContent(false))
    }

    @Test
    fun testSensitiveContent() {
        rule.setContent {
            Text("User name", modifier = Modifier.sensitiveContent())
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testNonSensitiveContent() {
        rule.setContent {
            Text("Hello world!")
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testDynamicSensitiveModifier() {
        var isContentSensitive by mutableStateOf(true)
        rule.setContent {
            Text(
                "Dynamic sensitive Content",
                modifier = Modifier.sensitiveContent(isContentSensitive)
            )
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }

        // update modifier as non sensitive and verify
        rule.runOnIdle { isContentSensitive = false }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testMultipleSensitiveComposable() {
        var isContentSensitive by mutableStateOf(true)
        var isContentSensitive2 by mutableStateOf(true)
        rule.setContent {
            Row {
                Text(
                    "Dynamic sensitive Content",
                    modifier = if (isContentSensitive) Modifier.sensitiveContent() else Modifier
                )
                Text(
                    "Dynamic sensitive Content",
                    modifier = if (isContentSensitive2) Modifier.sensitiveContent() else Modifier
                )
            }
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }

        // mark one composable non sensitive and verify
        rule.runOnIdle { isContentSensitive = false }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }

        // mark another composable non sensitive and verify
        rule.runOnIdle { isContentSensitive2 = false }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }

        // mark one composable sensitive again and verify
        rule.runOnIdle { isContentSensitive2 = true }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testRemoveSensitiveModifier() {
        var isModifierIncluded by mutableStateOf(true)
        rule.setContent {
            Text(
                "Dynamic sensitive Content",
                modifier = if (isModifierIncluded) Modifier.sensitiveContent() else Modifier
            )
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }

        // remove sensitive modifier from composable and verify
        rule.runOnIdle { isModifierIncluded = false }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testAddSensitiveModifier() {
        var isModifierIncluded by mutableStateOf(false)
        rule.setContent {
            Text(
                "Dynamic sensitive Content",
                modifier = if (isModifierIncluded) Modifier.sensitiveContent() else Modifier
            )
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }

        // add sensitive modifier to the composable and verify
        rule.runOnIdle { isModifierIncluded = true }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testRemoveSensitiveComposable() {
        var isSensitiveComposableIncluded by mutableStateOf(true)
        rule.setContent {
            if (isSensitiveComposableIncluded) {
                Text("Dynamic sensitive Content", modifier = Modifier.sensitiveContent())
            }
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }

        // remove sensitive composable and verify
        rule.runOnIdle { isSensitiveComposableIncluded = false }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }
    }

    @Test
    fun testAddSensitiveComposable() {
        var isSensitiveComposableIncluded by mutableStateOf(false)
        rule.setContent {
            if (isSensitiveComposableIncluded) {
                Text("Dynamic sensitive Content", modifier = Modifier.sensitiveContent())
            }
            androidComposeView = LocalView.current
        }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_AUTO,
                androidComposeView.getContentSensitivity()
            )
        }

        // add sensitive composable and verify
        rule.runOnIdle { isSensitiveComposableIncluded = true }
        rule.runOnIdle {
            Assert.assertEquals(
                View.CONTENT_SENSITIVITY_SENSITIVE,
                androidComposeView.getContentSensitivity()
            )
        }
    }
}
