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

package android.support.v17.leanback.widget;

/**
 * This callback class which will be passed as the parameter for setItems method to compute the
 * difference between the old items and new items.
 *
 * @param <T> The type of the item in list.
 */
public abstract class DiffCallback<T> {

    /**
     * This method is used to provide a standard to judge if two items are the same or not.
     * Will be used by DiffUtil.calculateDiff method.
     *
     * @param oldItem Previous item.
     * @param newItem New item.
     * @return If two items are the same or not.
     */
    public abstract boolean areItemsTheSame(T oldItem, T newItem);

    /**
     * This method is used to provide a standard to judge if two items have the same content or
     * not. Will be used by DiffUtil.calculateDiff method.
     *
     * @param oldItem Previous item.
     * @param newItem New item.
     * @return If two items have the same content.
     */
    public abstract boolean areContentsTheSame(T oldItem, T newItem);
}
