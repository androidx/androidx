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

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpecs;
import androidx.app.slice.builders.impl.GridBuilderBasicImpl;
import androidx.app.slice.builders.impl.GridBuilderListV1Impl;
import androidx.app.slice.builders.impl.TemplateBuilderImpl;

/**
 * Builder to construct a row of slice content in a grid format.
 * <p>
 * A grid row is composed of cells, each cell can have a combination of text and images. For more
 * details see {@link CellBuilder}.
 * </p>
 */
public class GridBuilder extends TemplateSliceBuilder {

    private androidx.app.slice.builders.impl.GridBuilder mImpl;

    /**
     * Create a builder which will construct a slice displayed in a grid format.
     * @param uri Uri to tag for this slice.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public GridBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(new Slice.Builder(uri), context);
    }

    /**
     * Create a builder which will construct a slice displayed in a grid format.
     * @param parent The builder constructing the parent slice.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public GridBuilder(@NonNull ListBuilder parent) {
        super(parent.getImpl().createGridBuilder());
    }

    /**
     */
    public GridBuilder(@NonNull Uri uri) {
        super(uri);
        throw new RuntimeException("Stub, to be removed");
    }

    /**
     */
    public GridBuilder(@NonNull TemplateSliceBuilder z) {
        super((Uri) null);
        throw new RuntimeException("Stub, to be removed");
    }

    @Override
    @NonNull
    public Slice build() {
        return mImpl.buildIndividual();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Override
    protected TemplateBuilderImpl selectImpl() {
        if (checkCompatible(SliceSpecs.GRID)) {
            return new GridBuilderListV1Impl(getBuilder(), SliceSpecs.GRID);
        } else if (checkCompatible(SliceSpecs.BASIC)) {
            return new GridBuilderBasicImpl(getBuilder(), SliceSpecs.GRID);
        }
        return null;
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (androidx.app.slice.builders.impl.GridBuilder) impl;
    }

    /**
     * Add a cell to the grid builder.
     */
    @NonNull
    public GridBuilder addCell(@NonNull CellBuilder builder) {
        mImpl.addCell((TemplateBuilderImpl) builder.mImpl);
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
     * @hide
     */
    @RestrictTo(LIBRARY)
    public androidx.app.slice.builders.impl.GridBuilder getImpl() {
        return mImpl;
    }

    /**
     * Sub-builder to construct a cell to be displayed in a grid.
     * <p>
     * Content added to a cell will be displayed in order vertically, for example the below code
     * would construct a cell with "First text", and image below it, and then "Second text" below
     * the image.
     *
     * <pre class="prettyprint">
     * CellBuilder cb = new CellBuilder(parent, sliceUri);
     * cb.addText("First text")
     *   .addImage(middleIcon)
     *   .addText("Second text");
     * </pre>
     *
     * A cell can have at most two text items and one image.
     * </p>
     */
    public static final class CellBuilder extends TemplateSliceBuilder {
        private androidx.app.slice.builders.impl.GridBuilder.CellBuilder mImpl;

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param parent The builder constructing the parent slice.
         */
        public CellBuilder(@NonNull GridBuilder parent) {
            super(parent.mImpl.createGridBuilder());
        }

        /**
         */
        public CellBuilder(@NonNull Uri uri) {
            super(uri);
            throw new RuntimeException("Stub, to be removed");
        }

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param uri Uri to tag for this slice.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public CellBuilder(@NonNull GridBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createGridBuilder(uri));
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.app.slice.builders.impl.GridBuilder.CellBuilder) impl;
        }

        /**
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         */
        @NonNull
        public CellBuilder addText(@NonNull CharSequence text) {
            mImpl.addText(text);
            return this;
        }

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         */
        @NonNull
        public CellBuilder addTitleText(@NonNull CharSequence text) {
            mImpl.addTitleText(text);
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
            mImpl.addLargeImage(image);
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
            mImpl.addImage(image);
            return this;
        }

        /**
         * Sets the action to be invoked if the user taps on this cell in the row.
         */
        @NonNull
        public CellBuilder setContentIntent(@NonNull PendingIntent intent) {
            mImpl.setContentIntent(intent);
            return this;
        }
    }
}
