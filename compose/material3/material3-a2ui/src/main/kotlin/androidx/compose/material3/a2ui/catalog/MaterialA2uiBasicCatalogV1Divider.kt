/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Divider"` component. */
internal object MaterialA2uiBasicCatalogV1Divider : A2uiBasicCatalogV1.Divider {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        axis: A2uiBasicCatalogV1.Divider.Axis,
        modifier: Modifier,
    ) {
        when (axis) {
            A2uiBasicCatalogV1.Divider.Axis.Horizontal -> HorizontalDivider(modifier = modifier)
            A2uiBasicCatalogV1.Divider.Axis.Vertical -> VerticalDivider(modifier = modifier)
        }
    }
}
