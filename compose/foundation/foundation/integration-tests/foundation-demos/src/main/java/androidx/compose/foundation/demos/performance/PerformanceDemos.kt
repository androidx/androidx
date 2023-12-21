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

package androidx.compose.foundation.demos.performance

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

/**
 * These demos are for triggering specific elements of Compose, to enable testing the performance
 * (runtime duration, allocations, etc) of those elements.
 */
val PerformanceDemos = DemoCategory(
    "Performance",
    listOf(
        ComposableDemo("Recomposition") { RecompositionDemo() },
        ComposableDemo("Drawing") { DrawingDemo() },
    )
)
