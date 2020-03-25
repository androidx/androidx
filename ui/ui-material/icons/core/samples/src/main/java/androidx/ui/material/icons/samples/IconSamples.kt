/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.icons.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.VectorPainter
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.rounded.Menu

@Sampled
@Composable
@Suppress("LocalVariableName")
fun AppIcons() {
    val MyAppIcons = Icons.Rounded
    SomeComposable(icon = MyAppIcons.Menu)
}

@Sampled
@Composable
fun DrawIcon() {
    Icon(Icons.Rounded.Menu)
}

@Composable
private fun SomeComposable(icon: VectorAsset) {
    Box(Modifier.paint(VectorPainter(icon)))
}
