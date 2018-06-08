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

package androidx.car.widget;

import java.util.List;

/**
 * Supplies data as {@link ListItem}.
 */
public abstract class ListItemProvider {

    /**
     * Returns {@link ListItem} at requested position.
     */
    public abstract ListItem get(int position);

    /**
     * @return number of total items.
     */
    public abstract int size();

    /**
     * A simple provider that wraps around a list.
     */
    public static class ListProvider extends ListItemProvider {
        private final List<ListItem> mItems;

        public ListProvider(List<ListItem> items) {
            mItems = items;
        }

        @Override
        public ListItem get(int position) {
            return mItems.get(position);
        }

        @Override
        public int size() {
            return mItems.size();
        }
    }
}
