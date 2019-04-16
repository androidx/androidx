/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout

import androidx.ui.core.Dp
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Component that represents an empty space with given width and height.
 *
 * @param width width of the empty space
 * @param height height of the empty space
 */
@Composable
fun FixedSpacer(width: Dp, height: Dp) {
    <Container width height expanded=true />
}

/**
 * Component that represents an empty space with fixed width.
 * Height will expand to the parent's constraints.
 *
 * @param width width of the empty space
 */
@Composable
fun WidthSpacer(width: Dp) {
    <Container width expanded=true />
}

/**
 * Component that represents an empty space with fixed height.
 * Width will expand to the parent's constraints.
 *
 * @param height height of the empty space
 */
@Composable
fun HeightSpacer(height: Dp) {
    <Container height expanded=true />
}