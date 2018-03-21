/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.paging.integration.testapp;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Sample item.
 */
class Item {
    public final int id;
    public final String text;
    @SuppressWarnings("WeakerAccess")
    public final int bgColor;

    Item(int id, String text, int bgColor) {
        this.id = id;
        this.text = text;
        this.bgColor = bgColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;
        return this.id == item.id
                && this.bgColor == item.bgColor
                && this.text.equals(item.text);
    }

    static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.id == newItem.id;
        }
    };
}
