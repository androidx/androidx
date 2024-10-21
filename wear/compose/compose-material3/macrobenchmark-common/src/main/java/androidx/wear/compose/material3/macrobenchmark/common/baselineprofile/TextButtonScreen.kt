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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.samples.FilledTextButtonSample
import androidx.wear.compose.material3.samples.FilledTonalTextButtonSample
import androidx.wear.compose.material3.samples.FilledVariantTextButtonSample
import androidx.wear.compose.material3.samples.LargeFilledTonalTextButtonSample
import androidx.wear.compose.material3.samples.OutlinedTextButtonSample
import androidx.wear.compose.material3.samples.TextButtonSample
import androidx.wear.compose.material3.samples.TextButtonWithOnLongClickSample

@OptIn(ExperimentalLayoutApi::class)
val TextButtonScreen =
    object : BaselineProfileScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow {
                        TextButtonSample()
                        FilledTonalTextButtonSample()
                        FilledTextButtonSample()
                        FilledVariantTextButtonSample()
                        OutlinedTextButtonSample()
                        TextButtonWithOnLongClickSample {}
                        LargeFilledTonalTextButtonSample()
                    }
                }
            }
    }
