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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Confirmation
import androidx.wear.compose.material3.ConfirmationDefaults
import androidx.wear.compose.material3.FailureConfirmation
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.macrobenchmark.common.FIND_OBJECT_TIMEOUT_MS
import androidx.wear.compose.material3.macrobenchmark.common.R
import androidx.wear.compose.material3.macrobenchmark.common.numberedContentDescription

val ConfirmationScreen =
    object : BaselineProfileScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ButtonsForSubmenu(
                        listOf(
                            { showConfirmation -> Confirmation(showConfirmation) },
                            { showConfirmation -> LongTextConfirmation(showConfirmation) },
                            { showConfirmation -> SuccessConfirmation(showConfirmation) },
                            { showConfirmation -> FailureConfirmation(showConfirmation) },
                        )
                    )
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                for (i in 0..3) {
                    device
                        .wait(
                            Until.findObject(By.desc(numberedContentDescription(i))),
                            FIND_OBJECT_TIMEOUT_MS
                        )
                        .click()
                    device.waitForIdle()
                }
                device.wait(
                    Until.findObject(By.desc(numberedContentDescription(0))),
                    FIND_OBJECT_TIMEOUT_MS
                )
            }
    }

@Composable
private fun ButtonsForSubmenu(
    confirmations: List<@Composable (showConfirmation: MutableState<Boolean>) -> Unit>
) {
    confirmations.forEachIndexed { index, confirmation ->
        val showConfirmation = remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize()) {
            FilledTonalButton(
                modifier =
                    Modifier.align(Alignment.Center).semantics {
                        contentDescription = numberedContentDescription(index)
                    },
                onClick = { showConfirmation.value = true },
                label = { Text("Show Confirmation") }
            )
        }
        confirmation(showConfirmation)
    }
}

@Composable
private fun Confirmation(showConfirmation: MutableState<Boolean>) {
    Confirmation(
        show = showConfirmation.value,
        onDismissRequest = { showConfirmation.value = false },
        curvedText = ConfirmationDefaults.curvedText("Confirmed")
    ) {
        Icon(
            painterResource(R.drawable.ic_favorite_rounded),
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.IconSize),
        )
    }
}

@Composable
fun LongTextConfirmation(showConfirmation: MutableState<Boolean>) {
    Confirmation(
        show = showConfirmation.value,
        onDismissRequest = { showConfirmation.value = false },
        text = { Text(text = "Your message has been sent") },
    ) {
        Icon(
            painterResource(R.drawable.ic_favorite_rounded),
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.SmallIconSize),
        )
    }
}

@Composable
fun SuccessConfirmation(showConfirmation: MutableState<Boolean>) {
    SuccessConfirmation(
        show = showConfirmation.value,
        onDismissRequest = { showConfirmation.value = false }
    )
}

@Composable
fun FailureConfirmation(showConfirmation: MutableState<Boolean>) {
    FailureConfirmation(
        show = showConfirmation.value,
        onDismissRequest = { showConfirmation.value = false }
    )
}
