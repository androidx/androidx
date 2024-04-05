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

package androidx.compose.material

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class SnackbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testQueueing() {
        var snackbarsShown = 0

        rule.setContent {
            val scope = rememberCoroutineScope()
            val state = remember { SnackbarHostState() }

            Scaffold(
                snackbarHost = { SnackbarHost(state) }
            ) {
                scope.launch {
                    (1..4).forEach {
                        state.showSnackbar(it.toString())
                        snackbarsShown = it
                    }
                }
            }
        }

        assertEquals(4, snackbarsShown)
    }
}