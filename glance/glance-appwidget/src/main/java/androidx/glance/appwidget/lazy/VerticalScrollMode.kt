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

package androidx.glance.appwidget.lazy

import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Dp

/**
 * Defines what sort of vertical scrolling behavior is used by [LazyColumn]. On Api 36+, snap
 * scrolling is available.
 */
public sealed interface VerticalScrollMode {

    /** The default LazyColumn behavior: standard scrolling. */
    public object Normal : VerticalScrollMode

    /** Items will snap into place */
    @RequiresApi(36) public object SnapScroll : VerticalScrollMode

    /**
     * Items will snap into place and match the height of the parent view.
     *
     * @param initialChildHeight How large to make a child. This value is used before a measure pass
     *   has been run. The container will always try to make the child match its height. Pass in the
     *   intended height of the parent container here.
     */
    @RequiresApi(36)
    public class SnapScrollMatchHeight(public val initialChildHeight: Dp) : VerticalScrollMode
}
