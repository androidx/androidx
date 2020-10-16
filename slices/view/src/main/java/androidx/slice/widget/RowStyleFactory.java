/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.widget;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.slice.SliceItem;

/**
 * Factory to return different styles for child views of a slice.
 */
public interface RowStyleFactory {
    /**
     * Returns the style resource to use for this child.
     *
     * @return Style resource or 0 if the default style should be used.
     */
    @StyleRes int getRowStyleRes(@NonNull SliceItem rowItem);
}
