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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
@ExperimentalMaterial3ExpressiveApi
actual class ModalWideNavigationRailProperties
actual constructor(
    actual val shouldDismissOnBackPress: Boolean,
)

@Immutable
@ExperimentalMaterial3ExpressiveApi
actual object DismissibleModalWideNavigationRailDefaults {
    actual val Properties: ModalWideNavigationRailProperties = implementedInJetBrainsFork()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun ModalWideNavigationRailDialog(
    onDismissRequest: () -> Unit,
    properties: ModalWideNavigationRailProperties,
    onPredictiveBack: (Float) -> Unit,
    onPredictiveBackCancelled: () -> Unit,
    predictiveBackState: RailPredictiveBackState,
    content: @Composable () -> Unit
): Unit = implementedInJetBrainsFork()
