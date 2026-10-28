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

package androidx.compose.ui.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn

/**
 * Value of Wear's [LocalReduceMotion] observed by the last composition of
 * [WearReduceMotionPreview].
 */
internal var observedWearReduceMotion: Boolean? = null

@Preview
@Composable
fun WearReduceMotionPreview() {
    observedWearReduceMotion = LocalReduceMotion.current
}

/** Value of Wear's [LocalReduceMotion] observed by the last [WearReduceMotionObservingWrapper]. */
internal var observedWrapperWearReduceMotion: Boolean? = null

/** A [PreviewWrapperProvider] recording the value of Wear's [LocalReduceMotion] it sees. */
class WearReduceMotionObservingWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        observedWrapperWearReduceMotion = LocalReduceMotion.current
        content()
    }
}

internal val EXPAND_LIST_TOP_PADDING = 20.dp

/** Bottom content padding, like the space `ScreenScaffold` reserves for an `EdgeButton`. */
internal val EXPAND_LIST_BOTTOM_PADDING = 80.dp

@Composable
private fun TransformingLazyColumnWithPadding(modifier: Modifier) {
    TransformingLazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(top = EXPAND_LIST_TOP_PADDING, bottom = EXPAND_LIST_BOTTOM_PADDING),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(EXPAND_LONG_LIST_ITEM_COUNT) {
            Box(Modifier.fillMaxWidth().height(EXPAND_ITEM_HEIGHT))
        }
    }
}

/** A TransformingLazyColumn whose content padding must be entirely visible once expanded. */
@Preview(device = EXPAND_VERTICAL_DEVICE)
@Composable
fun ExpandTransformingLazyColumnWithPaddingPreview() {
    TransformingLazyColumnWithPadding(Modifier.fillMaxSize())
}
