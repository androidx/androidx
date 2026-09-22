/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Assert.assertTrue

@Sampled
fun runOnEachSnapshotApplySample() = runComposeUiTest {
    var itemCount by mutableIntStateOf(0)
    setContent {
        Column {
            BasicText("Count: $itemCount", Modifier.testTag("itemCount"))
            Box(Modifier.testTag("add").clickable { itemCount++ }) {
                BasicText("Add")
            }
            Box(Modifier.testTag("remove").clickable { itemCount-- }) {
                BasicText("Remove")
            }
        }
    }

    // Runs on each snapshot applied from now on, so register it before the actions to check
    runOnEachSnapshotApply {
        // Asserts on the state behind the UI, as blocks can't look up nodes
        assertTrue("Item count must never be negative", itemCount >= 0)
    }

    onNodeWithTag("add").performClick()
    onNodeWithTag("remove").performClick()
    // Checks what the UI shows at a checkpoint
    onNodeWithTag("itemCount").assertTextEquals("Count: 0")
}

@Sampled
fun runOnEachSnapshotApplyScopedSample() = runComposeUiTest {
    fun <R> ComposeUiTest.withSnapshotInvariant(assertion: () -> Unit, block: () -> R): R {
        val handle = runOnEachSnapshotApply(assertion)
        try {
            return block()
        } finally {
            handle.dispose()
        }
    }

    var itemCount by mutableIntStateOf(0)
    setContent {
        Column {
            BasicText("Count: $itemCount", Modifier.testTag("itemCount"))
            Box(Modifier.testTag("add").clickable { itemCount++ }) {
                BasicText("Add")
            }
        }
    }

    withSnapshotInvariant(assertion = { assertTrue(itemCount >= 0) }) {
        onNodeWithTag("add").performClick()
    }
}
