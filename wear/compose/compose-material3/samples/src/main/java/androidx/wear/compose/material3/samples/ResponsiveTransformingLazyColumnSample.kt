/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.ResponsiveItemType
import androidx.wear.compose.material3.lazy.ResponsiveTransformingLazyColumn
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Sampled
@Preview
@Composable
fun SimpleResponsiveTransformingLazyColumnSample() {
    val transformationSpec = rememberTransformationSpec()
    ResponsiveTransformingLazyColumn {
        item(itemType = ResponsiveItemType.ListHeader) {
            ListHeader(modifier = Modifier.transformedHeight(this, transformationSpec)) {
                Text("Header")
            }
        }
        items(count = 10, itemType = { ResponsiveItemType.Button }) { index ->
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text("Item $index")
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun ResponsiveTransformingLazyColumnWithSnapSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()

    ResponsiveTransformingLazyColumn(
        state = state,
        rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollableState = state),
        flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = state),
    ) {
        item(itemType = ResponsiveItemType.ListHeader) {
            ListHeader(modifier = Modifier.transformedHeight(this, transformationSpec)) {
                Text("Header")
            }
        }
        items(count = 20, itemType = { ResponsiveItemType.Button }) { index ->
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text("Item $index")
            }
        }
    }
}
