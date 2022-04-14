/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.glance

import android.app.Activity
import androidx.compose.ui.unit.sp
import androidx.glance.action.ActionModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.StartActivityAction
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.runTestingComposition
import androidx.glance.text.TextStyle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ButtonTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun createComposableButton() = fakeCoroutineScope.runTest {
        val stringKey = ActionParameters.Key<String>("test")
        val intKey = ActionParameters.Key<Int>("test2")
        val string = "testString"
        val int = 12

        val root = runTestingComposition {
            Button(
                text = "button", onClick = actionStartActivity<Activity>(
                    actionParametersOf(stringKey to string, intKey to int)
                ), enabled = true
            )
        }

        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableButton>(root.children[0])
        assertThat(child.text).isEqualTo("button")
        val action =
            assertIs<StartActivityAction>(child.modifier.findModifier<ActionModifier>()?.action)
        assertThat(child.enabled).isTrue()
        assertThat(action.parameters.asMap()).hasSize(2)
        assertThat(action.parameters[stringKey]).isEqualTo(string)
        assertThat(action.parameters[intKey]).isEqualTo(int)
    }

    @Test
    fun createDisabledButton() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Button(text = "button", onClick = actionStartActivity<Activity>(), enabled = false)
        }

        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableButton>(root.children[0])
        assertThat(child.text).isEqualTo("button")
        assertThat(child.modifier.findModifier<ActionModifier>()).isNull()
        assertThat(child.enabled).isFalse()
    }

    @Test
    fun toEmittableText() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Button(
                text = "button",
                onClick = actionStartActivity<Activity>(),
                modifier = GlanceModifier.fillMaxSize(),
                maxLines = 3,
                style = TextStyle(fontSize = 12.sp)
            )
        }

        val child = assertIs<EmittableButton>(root.children.single())
        val asText = child.toEmittableText()

        assertThat(asText.text).isEqualTo("button")
        assertThat(asText.modifier).isEqualTo(child.modifier)
        assertThat(asText.style).isEqualTo(TextStyle(fontSize = 12.sp))
        assertThat(asText.maxLines).isEqualTo(3)
    }
}
