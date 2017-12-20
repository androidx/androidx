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

package androidx.app.slice.builders;

import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

import androidx.app.slice.Slice;

/**
 * Builder to construct a row of slice content in a grid format.
 * <p>
 * A grid row is composed of cells, each cell can have a combination of text and images. For more
 * details see {@link CellBuilder}.
 * </p>
 */
public class GridBuilder extends TemplateSliceBuilder {

    /**
     * Create a builder which will construct a slice displayed in a grid format.
     * @param uri Uri to tag for this slice.
     */
    public GridBuilder(@NonNull Uri uri) {
        super(new Slice.Builder(uri));
    }

    /**
     * Create a builder which will construct a slice displayed in a grid format.
     * @param parent The builder constructing the parent slice.
     */
    public GridBuilder(@NonNull TemplateSliceBuilder parent) {
        super(new Slice.Builder(parent.getBuilder()));
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void apply(Slice.Builder builder) {
    }

    @Override
    @NonNull
    public Slice build() {
        return new Slice.Builder(getBuilder()).addHints(HINT_HORIZONTAL, HINT_LIST_ITEM)
                .addSubSlice(getBuilder()
                .addHints(HINT_HORIZONTAL, HINT_LIST_ITEM).build()).build();
    }

    /**
     * Add a cell to the grid builder.
     */
    @NonNull
    public GridBuilder addCell(@NonNull CellBuilder builder) {
        getBuilder().addSubSlice(builder.build());
        return this;
    }

    /**
     * Add a cell to the grid builder.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @NonNull
    public GridBuilder addCell(@NonNull Consumer<CellBuilder> c) {
        CellBuilder b = new CellBuilder(this);
        c.accept(b);
        return addCell(b);
    }

    /**
     * Sub-builder to construct a cell to be displayed in a grid.
     * <p>
     * Content added to a cell will be displayed in order vertically, for example the below code
     * would construct a cell with "First text", and image below it, and then "Second text" below
     * the image.
     *
     * <pre class="prettyprint">
     * CellBuilder cb = new CellBuilder(sliceUri);
     * cb.addText("First text")
     *   .addImage(middleIcon)
     *   .addText("Second text");
     * </pre>
     *
     * A cell can have at most two text items and one image.
     * </p>
     */
    public static final class CellBuilder extends TemplateSliceBuilder {

        private PendingIntent mContentIntent;

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param parent The builder constructing the parent slice.
         */
        public CellBuilder(@NonNull GridBuilder parent) {
            super(parent.createChildBuilder());
        }

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param uri Uri to tag for this slice.
         */
        public CellBuilder(@NonNull Uri uri) {
            super(new Slice.Builder(uri));
        }

        /**
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         */
        @NonNull
        public CellBuilder addText(@NonNull CharSequence text) {
            getBuilder().addText(text, null);
            return this;
        }

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         */
        @NonNull
        public CellBuilder addTitleText(@NonNull CharSequence text) {
            getBuilder().addText(text, null, HINT_LARGE);
            return this;
        }

        /**
         * Adds an image to the cell that should be displayed as large as the cell allows.
         * There can be at most one image, the first one added will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        public CellBuilder addLargeImage(@NonNull Icon image) {
            getBuilder().addIcon(image, null, HINT_LARGE);
            return this;
        }

        /**
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        public CellBuilder addImage(@NonNull Icon image) {
            getBuilder().addIcon(image, null);
            return this;
        }

        /**
         * Sets the action to be invoked if the user taps on this cell in the row.
         */
        @NonNull
        public CellBuilder setContentIntent(@NonNull PendingIntent intent) {
            mContentIntent = intent;
            return this;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public void apply(Slice.Builder b) {
        }

        @Override
        @NonNull
        public Slice build() {
            if (mContentIntent != null) {
                return new Slice.Builder(getBuilder())
                        .addHints(HINT_HORIZONTAL, HINT_LIST_ITEM)
                        .addAction(mContentIntent, getBuilder().build(), null)
                        .build();
            }
            return getBuilder().addHints(HINT_HORIZONTAL, HINT_LIST_ITEM).build();
        }
    }
}
