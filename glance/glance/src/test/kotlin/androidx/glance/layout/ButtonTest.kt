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
package androidx.glance.layout

import android.app.Activity
import androidx.glance.action.ActionModifier
import androidx.glance.action.LaunchActivityAction
import androidx.glance.action.launchActivityAction
import androidx.glance.findModifier
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ButtonTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableButton() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Button(text = "button", onClick = launchActivityAction<Activity>(), enabled = true)
        }

        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableButton>(root.children[0])
        assertThat(child.text).isEqualTo("button")
        assertIs<LaunchActivityAction>(child.modifier.findModifier<ActionModifier>()?.action)
        assertThat(child.enabled).isTrue()
    }

    @Test
    fun createDisabledButton() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Button(text = "button", onClick = launchActivityAction<Activity>(), enabled = false)
        }

        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableButton>(root.children[0])
        assertThat(child.text).isEqualTo("button")
        assertThat(child.modifier.findModifier<ActionModifier>()).isNull()
        assertThat(child.enabled).isFalse()
    }
}
