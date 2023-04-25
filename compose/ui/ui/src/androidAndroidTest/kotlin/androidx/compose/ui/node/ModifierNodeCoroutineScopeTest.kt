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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeCoroutineScopeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun detach_doesNotCaptureStackTrace() {
        var exception: CancellationException? = null
        var shouldAttachNode by mutableStateOf(true)

        class TestNode : Modifier.Node() {
            override fun onAttach() {
                coroutineScope.launch {
                    try {
                        awaitCancellation()
                    } catch (e: CancellationException) {
                        exception = e
                    }
                }
            }
        }

        val testElement = object : ModifierNodeElement<TestNode>() {
            override fun create(): TestNode = TestNode()
            override fun update(node: TestNode) {}

            override fun hashCode(): Int = 0
            override fun equals(other: Any?): Boolean = other === this
        }

        rule.setContent {
            if (shouldAttachNode) {
                Box(Modifier.then(testElement))
            }
        }
        rule.runOnIdle {
            shouldAttachNode = false
        }

        rule.runOnIdle {
            assertThat(exception!!.stackTrace).isEmpty()
        }
    }
}