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

package androidx.compose.material3.adaptive.layout

<<<<<<<< HEAD:compose/foundation/foundation/src/skikoMain/kotlin/androidx/compose/foundation/text/BasicText.skiko.kt
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon

internal actual fun Modifier.textPointerHoverIcon(
    selectionRegistrar: SelectionRegistrar?
): Modifier = if (selectionRegistrar == null) this else pointerHoverIcon(textPointerIcon)
========
internal const val GOLDEN_MATERIAL3_ADAPTIVE = "compose/material3/adaptive"
>>>>>>>> 24205d80e8a006d8226bd21c8770c33cfdf2b2c9:compose/material3/adaptive/adaptive-layout/src/androidInstrumentedTest/kotlin/androidx/compose/material3/adaptive/layout/GoldenCommon.kt
