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

package android.support.v7.recyclerview.extensions;

import android.arch.paging.AsyncPagedListDiffer;
import android.support.v7.util.DiffUtil;

/**
 * Callback that informs {@link AsyncPagedListDiffer} how to compute list updates when using
 * {@link android.support.v7.util.DiffUtil} on a background thread.
 * <p>
 * The AdapterHelper will pass items from different lists to this callback in order to implement
 * the {@link android.support.v7.util.DiffUtil.Callback} it uses to compute differences between
 * lists.
 *
 * @param <T> Type of items to compare.
 *
 * @deprecated use {@link DiffUtil.ItemCallback DiffUtil.ItemCallback} directly starting in 27.1.0
 */
@Deprecated
public abstract class DiffCallback<T> extends DiffUtil.ItemCallback<T> {
}
