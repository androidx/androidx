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

package androidx.wear.compose.integration.demos

import androidx.wear.compose.foundation.samples.CurvedAndNormalText
import androidx.wear.compose.foundation.samples.SimpleCurvedRow

val WearFoundationDemos = DemoCategory(
    "Foundation",
    listOf(
        ComposableDemo("Curved Row") { CurvedRowDemo() },
        ComposableDemo("Simple") { SimpleCurvedRow() },
        ComposableDemo("Alignment") { CurvedRowAlignmentDemo() },
        ComposableDemo("Curved Text") { BasicCurvedTextDemo() },
        ComposableDemo("Curved and Normal Text") { CurvedAndNormalText() },
        ComposableDemo("Scrollable Column") { ScrollableColumnDemo() },
        ComposableDemo("Scrollable Row") { ScrollableRowDemo() },
    ),
)
