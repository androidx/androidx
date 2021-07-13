/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package androidx.leanback.widget;

/**
 * This is the query interface to supply optional features(aka facets) on an object without the need
 * of letting the object to subclass or implement java interfaces. Facets allow leanback to
 * re-compose optional features from leanback ViewHolder to RecyclerView ViewHolder. A typical
 * "facet" class is {@link ItemAlignmentFacet} that defines how to align a ViewHolder inside
 * VerticalGridView or HorizontalGridView.
 * A FacetProvider could be retrieved from two sources by VerticalGridView/HorizontalGridView in
 * the following order.
 * <ul>
 * <li>
 *     <p>
 *     ViewHolder based facet:
 *     </p>
 *     <p>
 *     RecyclerView.ViewHolder can implement FacetProvider. If app uses leanback
 *     Presenter.ViewHolder, the facet of Presenter.ViewHolder will be relayed by
 *     ItemBridgeAdapter.ViewHolder which is a wrapper of Presenter.ViewHolder.
 *     ViewHolder based facet is used less frequently than item view type based facet because
 *     in most cases ViewHolders of same type share the same alignment definition.
 *     </p>
 *     <p>
 *     For example, app calls viewHolder.setFacet(ItemAlignmentFacet.class, itemAlignmentFacet) to
 *     set alignment of the ViewHolder instance.
 *     </p>
 * </li>
 * <li>
 *     <p>
 *     RecyclerView item view type based facet:
 *     </p>
 *     <p>
 *     RecyclerView.Adapter can implement {@link FacetProviderAdapter} which returns FacetProvider
 *     for each item view type. If app uses leanback ObjectAdapter and Presenter, app wraps
 *     the ObjectAdapter and Presenter using {@link ItemBridgeAdapter}.  The implementation of
 *     {@link ItemBridgeAdapter#getFacetProvider(int)} will return the FacetProvider implemented
 *     by {@link Presenter} which is mapped to the item view type.
 *     </p>
 *     <p>
 *     For example, app calls presenter.setFacet(ItemAlignmentFacet.class, itemAlignmentFacet) to
 *     set alignment of all ViewHolders created by this Presenter.
 *     </p>
 * </li>
 * </ul>
 */
public interface FacetProvider {

    /**
     * Queries optional implemented facet.
     * @param facetClass  Facet classes to query,  examples are: class of
     *                    {@link ItemAlignmentFacet}.
     * @return Facet implementation for the facetClass or null if feature not implemented.
     */
    public Object getFacet(Class<?> facetClass);

}
