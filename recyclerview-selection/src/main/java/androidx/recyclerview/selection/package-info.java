/*
 * Copyright 2018 The Android Open Source Project
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

/**
 * A {@link androidx.recyclerview.widget.RecyclerView RecyclerView} addon library providing
 * support for item selection. The library provides support for both touch
 * and mouse driven selection. Developers retain control over the visual representation,
 * and the policies controlling selection behavior (like which items are eligible
 * for selection, and how many items can be selected.)
 *
 * <p>
 * Want to add selection support to your RecyclerView? Here's how you do it:
 *
 * <p>
 * <b>Determine which selection key type to use, then build your KeyProvider</b>
 *
 * <p>
 * Developers must decide on the key type used to identify selected items. Support
 * is provided for three types: {@link android.os.Parcelable Parcelable},
 * {@link java.lang.String String}, and {@link java.lang.Long Long}.
 *
 * <p>
 * See
 * {@link androidx.recyclerview.selection.SelectionTracker.Builder SelectionTracker.Builder}
 * for more detailed advice on which key type to use for your selection keys.
 *
 * <p>
 * <b>Implement {@link androidx.recyclerview.selection.ItemDetailsLookup ItemDetailsLookup}
 * </b>
 *
 * <p>
 * This class provides the selection library code necessary access to information about
 * items associated with {@link android.view.MotionEvent}. This will likely
 * depend on concrete {@link androidx.recyclerview.widget.RecyclerView.ViewHolder
 * RecyclerView.ViewHolder} type employed by your application.
 *
 * <p>
 * <b>Update views used in RecyclerView to reflect selected state</b>
 *
 * <p>
 * When the user selects an item the library will record that in
 * {@link androidx.recyclerview.selection.SelectionTracker SelectionTracker}
 * then notify RecyclerView that the state of the item has changed. This
 * will ultimately cause the value to be rebound by way of
 * {@link androidx.recyclerview.widget.RecyclerView.Adapter#onBindViewHolder
 *     RecyclerView.Adapter#onBindViewHolder}. The item must then be updated
 *     to reflect the new selection status. Without this
 *     the user will not *see* that the item has been selected.
 *
 * <ul>
 *     <li>In Adapter#onBindViewHolder, set the "activated" status on view.
 *     Note that the status should be "activated" not "selected".
 *     See <a href="https://developer.android.com/reference/android/view/View.html#setActivated(boolean)">
 *         View.html#setActivated</a> for details.
 *     <li>Update the styling of the view to represent the activated status. This can be done
 *     with a
 *     <a href="https://developer.android.com/guide/topics/resources/color-list-resource.html">
 *         color state list</a>.
 * </ul>
 *
 * <p>
 * <b>Use {@link androidx.appcompat.view.ActionMode ActionMode} when there is a selection</b>
 *
 * <p>
 * Register a {@link androidx.recyclerview.selection.SelectionTracker.SelectionObserver}
 * to be notified when selection changes. When a selection is first created, start
 * {@link androidx.appcompat.view.ActionMode ActionMode} to represent this to the user,
 * and provide selection specific actions.
 *
 * <p>
 * <b>Interpreted secondary actions: Drag and Drop, and Item Activation</b>
 *
 * <p>
 * At the end of the event processing pipeline the library may determine that the user
 * is attempting to activate an item by tapping it, or is attempting to drag and drop
 * an item or set of selected items. React to these interpretations by registering a
 * respective listener. See
 * {@link androidx.recyclerview.selection.SelectionTracker.Builder SelectionTracker.Builder}
 * for details.
 *
 * <p>
 * <b>Assemble everything with
 * {@link androidx.recyclerview.selection.SelectionTracker.Builder SelectionTracker.Builder}
 * </b>
 *
 * <p>
 * Example usage (with {@code Long} selection keys:
 * <pre>SelectionTracker<Long> tracker = new SelectionTracker.Builder<>(
 *        "my-selection-id",
 *        recyclerView,
 *        new StableIdKeyProvider(recyclerView),
 *        new MyDetailsLookup(recyclerView),
 *        StorageStrategy.createLongStorage())
 *        .build();
 *</pre>
 *
 * <p>In order to build a SelectionTracker instance the supplied RecyclerView must be initialized
 * with an Adapter. Given this fact, you will probably need to inject the SelectionTracker
 * instance into your RecyclerView.Adapter after the Adapter is created, as it will be necessary
 * to consult selected status using SelectionTracker from the onBindViewHolder method.
 *
 * <p>
 * <b>Include Selection in Activity lifecycle events</b>
 *
 * <p>
 * In order to preserve state the author must the selection library in handling
 * of Activity lifecycle events. See SelectionTracker#onSaveInstanceState
 * and SelectionTracker#onRestoreInstanceState.
 *
 * <p>A unique selection id must be supplied to
 * {@link androidx.recyclerview.selection.SelectionTracker.Builder SelectionTracker.Builder}
 * constructor. This is necessary as an activity or fragment may have multiple distinct
 * selectable lists that may both need to be persisted in saved state.
 */

package androidx.recyclerview.selection;
