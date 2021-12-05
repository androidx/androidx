/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

/**
 * Contains useful information about the currently displayed layout state of [ScalingLazyColumn].
 * For example you can get the list of currently displayed item.
 *
 * Use [ScalingLazyListState.layoutInfo] to retrieve this
 */
interface ScalingLazyListLayoutInfo {
    /**
     * The list of [ScalingLazyListItemInfo] representing all the currently visible items.
     */
    val visibleItemsInfo: List<ScalingLazyListItemInfo>

    /**
     * The start offset of the layout's viewport. You can think of it as a minimum offset which
     * would be visible. Usually it is 0, but it can be negative if a content padding was applied
     * as the content displayed in the content padding area is still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport. You can think of it as a maximum offset which
     * would be visible. Usually it is a size of the lazy list container plus a content padding.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /**
     * The total count of items passed to [ScalingLazyColumn].
     */
    val totalItemsCount: Int

    /**
     * The index of the item on to the center of the view, if there are two items around the center
     * line, the second one (higher index) is used. It's -1 if the list is empty.
     */
    val centralItemIndex: Int
}