/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import android.view.View;
import android.view.ViewGroup;

/**
 * A Presenter is used to generate {@link View}s and bind Objects to them on
 * demand. It is closely related to concept of an {@link Adapter}, but is not
 * position-based.
 *
 * <p>
 * A trivial Presenter that takes a string and renders it into a {@link
 * TextView}:
 *
 * <pre class="prettyprint">
 * public class StringTextViewPresenter extends Presenter {
 *     // This class does not need a custom ViewHolder, since it does not use
 *     // a complex layout.
 *
 *     {@literal @}Override
 *     public ViewHolder createViewHolder(ViewGroup parent) {
 *         return new ViewHolder(new TextView(parent.getContext()));
 *     }
 *
 *     {@literal @}Override
 *     public void bindViewHolder(ViewHolder viewHolder, Object item) {
 *         String str = (String) item;
 *         TextView textView = (TextView) viewHolder.mView;
 *
 *         textView.setText(item);
 *     }
 *
 *     {@literal @}Override
 *     public void unbindViewHolder(ViewHolder viewHolder) {
 *         TextView textView = (TextView) viewHolder.mView;
 *         textView.setText(null);
 *     }
 * }
 * </pre>
 */
public abstract class Presenter {
    /**
     * ViewHolder can be subclassed and used to cache any view accessors needed
     * to improve binding performance (for example, results of findViewById)
     * without needing to subclass a View.
     */
    public static class ViewHolder {
        public final View view;

        public ViewHolder(View view) {
            this.view = view;
        }
    }

    /**
     * Creates a new {@link View}.
     */
    public abstract ViewHolder onCreateViewHolder(ViewGroup parent);

    /**
     * Binds a {@link View} to an item.
     */
    public abstract void onBindViewHolder(ViewHolder viewHolder, Object item);

    /**
     * Unbinds a {@link View} from an item. Any expensive references may be
     * released here, and any fields that are not bound for every item should be
     * cleared here.
     */
    public abstract void onUnbindViewHolder(ViewHolder viewHolder);
}
