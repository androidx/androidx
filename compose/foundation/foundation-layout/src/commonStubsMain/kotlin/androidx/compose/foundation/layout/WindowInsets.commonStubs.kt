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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path

actual val WindowInsets.Companion.captionBar: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.ime: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.statusBars: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.systemBars: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.waterfall: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.cutoutPath: Path?
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable get() = implementedInJetBrainsFork()

actual val WindowInsets.Companion.safeContent: WindowInsets
    @Composable get() = implementedInJetBrainsFork()
