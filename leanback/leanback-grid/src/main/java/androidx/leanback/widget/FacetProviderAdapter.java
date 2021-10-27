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
package androidx.leanback.widget;

import androidx.annotation.Nullable;

/**
 * Optional interface that implemented by
 * {@link androidx.recyclerview.widget.RecyclerView.Adapter} to
 * query {@link FacetProvider} for a given item view type within Adapter. Note that
 * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} may also implement
 * {@link FacetProvider} which has a higher priority than the one returned
 * from{@link #getFacetProvider(int)}.
 * <p>
 * A typical use case of {@link FacetProvider} is that VerticalGridView/HorizontalGridView retrieves
 * {@link ItemAlignmentFacet} for a ViewHolder or a item view type.
 */
public interface FacetProviderAdapter {
    /**
     * Queries {@link FacetProvider} for a given type within Adapter.
     *
     * @param type type of the item.
     * @return Facet provider for the type.
     */
    @Nullable
    FacetProvider getFacetProvider(int type);
}
