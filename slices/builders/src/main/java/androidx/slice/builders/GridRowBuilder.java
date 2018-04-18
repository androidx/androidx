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

package androidx.slice.builders;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.PendingIntent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;
import androidx.slice.builders.impl.TemplateBuilderImpl;


/**
 * Builder to construct a grid row which may be added as a row to {@link ListBuilder}.
 * <p>
 * A grid row supports cells of vertically laid out content in a single row. Each cell can
 * contain a combination of text and images and is constructed using a {@link CellBuilder}.
 * <p>
 * A grid supports a couple of image types:
 * <ul>
 *     <li>{@link ListBuilder#ICON_IMAGE} - icon images are expected to be tintable and are
 *     shown at a standard icon size.</li>
 *     <li>{@link ListBuilder#SMALL_IMAGE} - small images are not tinted and are shown at
 *     a small size.</li>
 *     <li>{@link ListBuilder#LARGE_IMAGE} - large images are not tinted and are shown as
 *     large as they can be, in a {@link android.widget.ImageView.ScaleType#CENTER_CROP}</li>
 * </ul>
 * <p>
 * If the first row of your slice was created with {@link GridRowBuilder} then there are a couple
 * of extra considerations you should take for when the slice is displayed in different modes,
 * particularly:
 * <ul>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SHORTCUT} - ensure you use
 *     {@link #setPrimaryAction(SliceAction)} on grid row builder to specify the shortcut action,
 *     icon, and label. This action will also be activated when the whole row is clicked, unless
 *     the individual cells in the row have actions set on them.</li>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SMALL} - in small format there might not
 *     be enough space to display a cell containing two lines of text and an image, in this case
 *     only one line of text will be shown with the image, favoring text added via
 *     {@link CellBuilder#addTitleText(CharSequence)} or the first text added if no title was
 *     added.</li>
 * </ul>
 * <p>
 * If more cells are added to the grid row than can be displayed, the cells will be cut off. Using
 * {@link #addSeeMoreAction(PendingIntent)} you can specify an action to take the user to see the
 * rest of the content, this will take up space as a cell item in a row if added.
 *
 * @see ListBuilder#addGridRow(GridRowBuilder)
 */
public class GridRowBuilder extends TemplateSliceBuilder {

    private androidx.slice.builders.impl.GridRowBuilder mImpl;
    private boolean mHasSeeMore;

    /**
     * Create a builder which will construct a slice displayed in a grid format.
     * @param parent The builder constructing the parent slice.
     */
    public GridRowBuilder(@NonNull ListBuilder parent) {
        super(parent.getImpl().createGridBuilder());
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (androidx.slice.builders.impl.GridRowBuilder) impl;
    }

    /**
     * Add a cell to the grid builder.
     */
    @NonNull
    public GridRowBuilder addCell(@NonNull CellBuilder builder) {
        mImpl.addCell((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Add a cell to the grid builder.
     */
    @NonNull
    public GridRowBuilder addCell(@NonNull Consumer<CellBuilder> c) {
        CellBuilder b = new CellBuilder(this);
        c.accept(b);
        return addCell(b);
    }

    /**
     * If all content in a slice cannot be shown, the cell added here may be displayed where the
     * content is cut off.
     * <p>
     * This method should only be used if you want to display a custom cell to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom cell, the cell should have
     * {@link CellBuilder#setContentIntent(PendingIntent)} specified to take the user to an
     * activity to see all of the content.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public GridRowBuilder setSeeMoreCell(@NonNull CellBuilder builder) {
        if (mHasSeeMore) {
            throw new IllegalStateException("Trying to add see more cell when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreCell((TemplateBuilderImpl) builder.mImpl);
        mHasSeeMore = true;
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the cell added here may be displayed where the
     * content is cut off.
     * <p>
     * This method should only be used if you want to display a custom cell to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom cell, the cell should have
     * {@link CellBuilder#setContentIntent(PendingIntent)} specified to take the user to an
     * activity to see all of the content.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public GridRowBuilder setSeeMoreCell(@NonNull Consumer<CellBuilder> c) {
        CellBuilder b = new CellBuilder(this);
        c.accept(b);
        return setSeeMoreCell(b);
    }

    /**
     * If all content in a slice cannot be shown, a "see more" affordance may be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public GridRowBuilder setSeeMoreAction(@NonNull PendingIntent intent) {
        if (mHasSeeMore) {
            throw new IllegalStateException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreAction(intent);
        mHasSeeMore = true;
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the cell added here may be displayed where the
     * content is cut off.
     * <p>
     * This method should only be used if you want to display a custom cell to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom cell, the cell should have
     * {@link CellBuilder#setContentIntent(PendingIntent)} specified to take the user to an
     * activity to see all of the content.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     *
     * @deprecated TO BE REMOVED
     */
    @NonNull
    @Deprecated
    public GridRowBuilder addSeeMoreCell(@NonNull CellBuilder builder) {
        if (mHasSeeMore) {
            throw new IllegalStateException("Trying to add see more cell when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreCell((TemplateBuilderImpl) builder.mImpl);
        mHasSeeMore = true;
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the cell added here may be displayed where the
     * content is cut off.
     * <p>
     * This method should only be used if you want to display a custom cell to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom cell, the cell should have
     * {@link CellBuilder#setContentIntent(PendingIntent)} specified to take the user to an
     * activity to see all of the content.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     *
     * @deprecated TO BE REMOVED
     */
    @NonNull
    @Deprecated
    public GridRowBuilder addSeeMoreCell(@NonNull Consumer<CellBuilder> c) {
        CellBuilder b = new CellBuilder(this);
        c.accept(b);
        return addSeeMoreCell(b);
    }

    /**
     * If all content in a slice cannot be shown, a "see more" affordance may be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @NonNull
    public GridRowBuilder addSeeMoreAction(@NonNull PendingIntent intent) {
        if (mHasSeeMore) {
            throw new IllegalStateException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreAction(intent);
        mHasSeeMore = true;
        return this;
    }

    /**
     * Sets the intent to send when the whole grid row is clicked.
     * <p>
     * If all the cells in the grid have specified a
     * {@link CellBuilder#setPrimaryAction(SliceAction)} then the action set here on the
     * {@link GridRowBuilder} may not ever be invoked.
     * <p>
     * If this grid row is the first row in {@link ListBuilder}, the action
     * set here will be used to represent the slice when presented in
     * {@link androidx.slice.widget.SliceView#MODE_SHORTCUT}.
     */
    @NonNull
    public GridRowBuilder setPrimaryAction(@NonNull SliceAction action) {
        mImpl.setPrimaryAction(action);
        return this;
    }

    /**
     * Sets the content description for the entire grid row.
     */
    @NonNull
    public GridRowBuilder setContentDescription(@NonNull CharSequence description) {
        mImpl.setContentDescription(description);
        return this;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public androidx.slice.builders.impl.GridRowBuilder getImpl() {
        return mImpl;
    }

    /**
     * Builder to construct a cell. A cell can be added as an item to GridRowBuilder via
     * {@link GridRowBuilder#addCell(CellBuilder)}.
     * <p>
     * A cell supports up to two lines of text and one image. Content added to a cell will be
     * displayed in the order that the content is added to it. For example, the below code
     * would construct a cell with "First text", and image below it, and then "Second text" below
     * the image.
     *
     * <pre class="prettyprint">
     * CellBuilder cb = new CellBuilder(parent, sliceUri);
     * cb.addText("First text")
     *   .addImage(middleIcon)
     *   .addText("Second text");
     * </pre>
     * <p>
     * A cell supports a couple of image types:
     * <ul>
     *     <li>{@link ListBuilder#ICON_IMAGE} - icon images are expected to be tintable and are
     *     shown at a standard icon size.</li>
     *     <li>{@link ListBuilder#SMALL_IMAGE} - small images are not tinted and are shown at
     *     a small size.</li>
     *     <li>{@link ListBuilder#LARGE_IMAGE} - large images are not tinted and are shown as
     *     large as they can be, in a {@link android.widget.ImageView.ScaleType#CENTER_CROP}</li>
     * </ul>
     *
     * @see GridRowBuilder#addCell(CellBuilder)
     * @see ListBuilder#addGridRow(GridRowBuilder)
     * @see ListBuilder#ICON_IMAGE
     * @see ListBuilder#SMALL_IMAGE
     * @see ListBuilder#ICON_IMAGE
     */
    public static final class CellBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.GridRowBuilder.CellBuilder mImpl;

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param parent The builder constructing the parent slice.
         */
        public CellBuilder(@NonNull GridRowBuilder parent) {
            super(parent.mImpl.createGridRowBuilder());
        }

        /**
         * Create a builder which will construct a slice displayed as a cell in a grid.
         * @param uri Uri to tag for this slice.
         */
        public CellBuilder(@NonNull GridRowBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createGridRowBuilder(uri));
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.GridRowBuilder.CellBuilder) impl;
        }

        /**
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         */
        @NonNull
        public CellBuilder addText(@NonNull CharSequence text) {
            return addText(text, false /* isLoading */);
        }

        /**
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public CellBuilder addText(@Nullable CharSequence text, boolean isLoading) {
            mImpl.addText(text, isLoading);
            return this;
        }

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         */
        @NonNull
        public CellBuilder addTitleText(@NonNull CharSequence text) {
            return addTitleText(text, false /* isLoading */);
        }

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public CellBuilder addTitleText(@Nullable CharSequence text, boolean isLoading) {
            mImpl.addTitleText(text, isLoading);
            return this;
        }

        /**
         * Adds an image to the cell. There can be at most one image, the first one added will be
         * used, others will be ignored.
         *
         * @param image the image to display in the cell.
         * @param imageMode the mode that image should be displayed in.
         *
         * @see ListBuilder#ICON_IMAGE
         * @see ListBuilder#SMALL_IMAGE
         * @see ListBuilder#LARGE_IMAGE
         */
        @NonNull
        public CellBuilder addImage(@NonNull IconCompat image,
                @ListBuilder.ImageMode int imageMode) {
            return addImage(image, imageMode, false /* isLoading */);
        }

        /**
         * Adds an image to the cell. There can be at most one image, the first one added will be
         * used, others will be ignored.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param image the image to display in the cell.
         * @param imageMode the mode that image should be displayed in.
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         *
         * @see ListBuilder#ICON_IMAGE
         * @see ListBuilder#SMALL_IMAGE
         * @see ListBuilder#LARGE_IMAGE
         */
        @NonNull
        public CellBuilder addImage(@Nullable IconCompat image,
                @ListBuilder.ImageMode int imageMode, boolean isLoading) {
            mImpl.addImage(image, imageMode, isLoading);
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

        /**
         * Sets the content description for this cell.
         */
        @NonNull
        public CellBuilder setContentDescription(@NonNull CharSequence description) {
            mImpl.setContentDescription(description);
            return this;
        }
    }
}
