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

import java.util.List;

/**
 * List receiving interface implemented by {@link ListAdapter}, {@link ListAdapterHelper},
 * {@link android.arch.paging.PagedListAdapter}, and
 * {@link android.arch.paging.PagedListAdapterHelper}.
 * <p>
 * {@link LiveListAdapterUtil} can be used to bind {@code LiveData<List>} or
 * {@code LiveData<PagedList>} to anything that implements this interface, like the above classes.
 *
 * @param <L>
 */
public interface ListReceiver<L extends List<?>> {
    /**
     * Sets the current list on the receiver.
     * <p>
     * In the case of Adapters and AdapterHelpers, this indicates the new list should be
     * presented by the Adapter.
     *
     * @param list The new list.
     */
    void setList(L list);
}
