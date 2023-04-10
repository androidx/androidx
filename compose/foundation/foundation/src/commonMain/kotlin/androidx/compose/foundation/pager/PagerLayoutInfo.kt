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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize

@ExperimentalFoundationApi
internal interface PagerLayoutInfo {
    val visiblePagesInfo: List<PageInfo>
    val closestPageToSnapPosition: PageInfo?
    val pagesCount: Int
    val pageSize: Int
    val pageSpacing: Int
    val viewportStartOffset: Int
    val viewportEndOffset: Int
    val beforeContentPadding: Int
    val afterContentPadding: Int
    val viewportSize: IntSize
    val orientation: Orientation
    val reverseLayout: Boolean
}