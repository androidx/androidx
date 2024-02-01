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

/**
 * This represents a single measured page in a [Pager] layout.
 */
sealed interface PageInfo {

    /**
     * The index of this page.
     */
    val index: Int

    /**
     * The key of the page which was passed to the [HorizontalPager] or [VerticalPager] composables.
     */
    val key: Any

    /**
     * The main axis offset of the item in pixels. It is relative to the start of the [Pager]
     * container.
     */
    val offset: Int
}
