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
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.VectorAssetBuilder
import androidx.ui.material.icons.Icons
import androidx.ui.unit.dp

@Sampled
@Composable
@Suppress("LocalVariableName", "UNUSED_VARIABLE")
fun AppIcons() {
    val MyAppIcons = Icons.Rounded
    SomeComposable(icon = MyAppIcons.Menu)
}

@Sampled
@Composable
fun DrawIcon() {
    DrawVector(vectorImage = Icons.Rounded.Menu)
}

@Composable
private fun SomeComposable(icon: VectorAsset) { DrawVector(icon) }

// TODO: b/146212531 remove once main code has merged
private val Icons.Rounded.Menu get() = VectorAssetBuilder("", 0.dp, 0.dp, 24f, 24f).build()
