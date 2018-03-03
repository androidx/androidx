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

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.ListBuilderBasicImpl;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;

/**
 * Builder to construct slice content in a list format.
 * <p>
 * Use this builder for showing rows of content which is composed of text, images, and actions. For
 * more details see {@link RowBuilder}.
 * </p>
 * <p>
 * Slices can be displayed in different formats:
 * <ul>
 *     <li>Shortcut - The slice is displayed as an icon with a text label.</li>
 *     <li>Small - Only a single row of content is displayed in small format, to specify which
 *         row to display in small format see {@link #setHeader(HeaderBuilder)}.</li>
 *     <li>Large - As many rows of content are shown as possible. If the presenter of the slice
 *         allows scrolling then all rows of content will be displayed in a scrollable view.</li>
 * </ul>
 * </p>
 *
 * @see RowBuilder
 */
public class ListBuilder extends TemplateSliceBuilder {

    private boolean mHasSeeMore;
    private androidx.slice.builders.impl.ListBuilder mImpl;

    /**
     * Create a builder which will construct a slice that will display rows of content.
     * @param uri Uri to tag for this slice.
     */
    public ListBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (androidx.slice.builders.impl.ListBuilder) impl;
    }

    /**
     * Add a row to list builder.
     */
    @NonNull
    public ListBuilder addRow(@NonNull RowBuilder builder) {
        mImpl.addRow((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Add a row the list builder.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    public ListBuilder addRow(@NonNull Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return addRow(b);
    }

    /**
     * Add a grid row to the list builder.
     */
    @NonNull
    public ListBuilder addGrid(@NonNull GridBuilder builder) {
        mImpl.addGrid((TemplateBuilderImpl) builder.getImpl());
        return this;
    }

    /**
     * Add a grid row to the list builder.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    public ListBuilder addGrid(@NonNull Consumer<GridBuilder> c) {
        GridBuilder b = new GridBuilder(this);
        c.accept(b);
        return addGrid(b);
    }

    /**
     * Adds a header to this template.
     * <p>
     * The header should contain a title that is representative of the content in this slice along
     * with an intent that links to the app activity associated with this content.
     */
    @NonNull
    public ListBuilder setHeader(@NonNull HeaderBuilder builder) {
        mImpl.setHeader((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Adds a header to this template.
     * <p>
     * The header should contain a title that is representative of the content in this slice along
     * with an intent that links to the app activity associated with this content.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    public ListBuilder setHeader(@NonNull Consumer<HeaderBuilder> c) {
        HeaderBuilder b = new HeaderBuilder(this);
        c.accept(b);
        return setHeader(b);
    }

    /**
     * Adds an action to this template. Actions added with this method are grouped together and
     * may be shown on the template in large or small formats. Generally these actions will be
     * displayed in the order they were added, however, if not all actions can be displayed then
     * actions with a higher priority may be shown first.
     *
     * @see SliceAction
     * @see SliceAction#setPriority(int)
     */
    @NonNull
    public ListBuilder addAction(@NonNull SliceAction action) {
        mImpl.addAction(action);
        return this;
    }

    /**
     * Sets the color to tint items displayed by this template (e.g. icons).
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public ListBuilder setColor(@ColorInt int color) {
        mImpl.setColor(color);
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the row added here may be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * This method should only be used if you want to display a custom row to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom row, the row should have a content intent or action end item
     * specified to take the user to an activity to see all of the content.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public ListBuilder addSeeMoreRow(@NonNull RowBuilder builder) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more row when one has "
                    + "already been added");
        }
        mImpl.addSeeMoreRow((TemplateBuilderImpl) builder.mImpl);
        mHasSeeMore = true;
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the row added here may be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * This method should only be used if you want to display a custom row to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom row, the row should have a content intent or action end item
     * specified to take the user to an activity to see all of the content.
     * </p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    public ListBuilder addSeeMoreRow(@NonNull Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return addSeeMoreRow(b);
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
    public ListBuilder addSeeMoreAction(@NonNull PendingIntent intent) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.addSeeMoreAction(intent);
        mHasSeeMore = true;
        return this;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Override
    protected TemplateBuilderImpl selectImpl() {
        if (checkCompatible(SliceSpecs.LIST)) {
            return new ListBuilderV1Impl(getBuilder(), SliceSpecs.LIST);
        } else if (checkCompatible(SliceSpecs.BASIC)) {
            return new ListBuilderBasicImpl(getBuilder(), SliceSpecs.BASIC);
        }
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public androidx.slice.builders.impl.ListBuilder getImpl() {
        return mImpl;
    }

    /**
     * Add an input range row to the list builder.
     */
    @NonNull
    public ListBuilder addInputRange(@NonNull InputRangeBuilder b) {
        mImpl.addInputRange((TemplateBuilderImpl) b.mImpl);
        return this;
    }

    /**
     * Add an input range row to the list builder.
     */
    @NonNull
    public ListBuilder addInputRange(@NonNull Consumer<InputRangeBuilder> c) {
        InputRangeBuilder inputRangeBuilder = new InputRangeBuilder(this);
        c.accept(inputRangeBuilder);
        return addInputRange(inputRangeBuilder);
    }

    /**
     * Add a range row to the list builder.
     */
    @NonNull
    public ListBuilder addRange(@NonNull RangeBuilder rangeBuilder) {
        mImpl.addRange((TemplateBuilderImpl) rangeBuilder.mImpl);
        return this;
    }

    /**
     * Add a range row to the list builder.
     */
    @NonNull
    public ListBuilder addRange(@NonNull Consumer<RangeBuilder> c) {
        RangeBuilder rangeBuilder = new RangeBuilder(this);
        c.accept(rangeBuilder);
        return addRange(rangeBuilder);
    }

    /**
     * Builder to construct a range row.
     */
    public static class RangeBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.RangeBuilder mImpl;

        public RangeBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createRangeBuilder());
        }

        /**
         * Set the upper limit of the range. The default is 100.
         */
        @NonNull
        public RangeBuilder setMax(int max) {
            mImpl.setMax(max);
            return this;
        }

        /**
         * Set the current value of the range.
         */
        @NonNull
        public RangeBuilder setValue(int value) {
            mImpl.setValue(value);
            return this;
        }

        /**
         * Set the title.
         */
        @NonNull
        public RangeBuilder setTitle(@NonNull CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.RangeBuilder) impl;
        }
    }

    /**
     * Builder to construct a input range row.
     */
    public static class InputRangeBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.InputRangeBuilder mImpl;

        public InputRangeBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createInputRangeBuilder());
        }

        /**
         * Set the upper limit of the range. The default is 100.
         */
        @NonNull
        public InputRangeBuilder setMax(int max) {
            mImpl.setMax(max);
            return this;
        }

        /**
         * Set the current value of the range.
         */
        @NonNull
        public InputRangeBuilder setValue(int value) {
            mImpl.setValue(value);
            return this;
        }

        /**
         * Set the title.
         */
        @NonNull
        public InputRangeBuilder setTitle(@NonNull CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }


        /**
         * Set the {@link PendingIntent} to send when the current value is updated.
         */
        @NonNull
        public InputRangeBuilder setAction(@NonNull PendingIntent action) {
            mImpl.setAction(action);
            return this;
        }

        /**
         * Set the {@link Icon} to be displayed as the thumb on the input range.
         */
        @NonNull
        public InputRangeBuilder setThumb(@NonNull Icon thumb) {
            mImpl.setThumb(thumb);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.InputRangeBuilder) impl;
        }
    }

    /**
     * Sub-builder to construct a row of slice content.
     * <p>
     * Row content can have:
     * <ul>
     *     <li>Title item - This is only displayed if this is a list item in a large template, it
     *     will not be shown if this template is being used for small format. The item appears at
     *     the start of the template. There can only be one title item displayed, and it could be a
     *     timestamp, image, or a tappable icon.</li>
     *     <li>Title - Formatted as a title.</li>
     *     <li>Subtitle - Appears below the title (if one exists) and is formatted as normal text.
     *     </li>
     *     <li>End item -  Appears at the end of the template, there can be multiple end items but
     *     they are only shown if there's enough space. End items can be a timestamp, image, or a
     *     tappable icon.</li>
     * </ul>
     * </p>
     *
     * @see ListBuilder
     */
    public static class RowBuilder extends TemplateSliceBuilder {

        private androidx.slice.builders.impl.ListBuilder.RowBuilder mImpl;

        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private boolean mHasDefaultToggle;
        private boolean mHasTimestamp;

        /**
         * Create a builder which will construct a slice displayed in a row format.
         * @param parent The builder constructing the parent slice.
         */
        public RowBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createRowBuilder());
        }

        /**
         * Create a builder which will construct a slice displayed in a row format.
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createRowBuilder(uri));
        }

        /**
         * Create a builder which will construct a slice displayed in a row format.
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(@NonNull Context context, @NonNull Uri uri) {
            super(new ListBuilder(context, uri).mImpl.createRowBuilder(uri));
        }

        /**
         * Sets the title item to be the provided timestamp. Only one timestamp can be added, if
         * one is already added this will throw {@link IllegalArgumentException}.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        @NonNull
        public RowBuilder setTitleItem(long timeStamp) {
            if (mHasTimestamp) {
                throw new IllegalArgumentException("Trying to add a timestamp when one has "
                        + "already been added");
            }
            mImpl.setTitleItem(timeStamp);
            mHasTimestamp = true;
            return this;
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this
         * will replace any other title items that may have been set.
         */
        @NonNull
        public RowBuilder setTitleItem(@NonNull Icon icon) {
            mImpl.setTitleItem(icon);
            return this;
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this
         * will replace any other title items that may have been set.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setTitleItem(@Nullable Icon icon, boolean isLoading) {
            mImpl.setTitleItem(icon, isLoading);
            return this;
        }

        /**
         * Sets the title item to be a tappable icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         */
        @NonNull
        public RowBuilder setTitleItem(@NonNull SliceAction action) {
            return setTitleItem(action, false /* isLoading */);
        }

        /**
         * Sets the title item to be a tappable icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setTitleItem(@NonNull SliceAction action, boolean isLoading) {
            mImpl.setTitleItem(action, isLoading);
            return this;
        }

        /**
         * Sets the action to be invoked if the user taps on the main content of the template.
         */
        @NonNull
        public RowBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        /**
         * Sets the title text.
         */
        @NonNull
        public RowBuilder setTitle(@NonNull CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }

        /**
         * Sets the title text.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setTitle(@Nullable CharSequence title, boolean isLoading) {
            mImpl.setTitle(title, isLoading);
            return this;
        }

        /**
         * Sets the subtitle text.
         */
        @NonNull
        public RowBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle text.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setSubtitle(@Nullable CharSequence subtitle, boolean isLoading) {
            mImpl.setSubtitle(subtitle, isLoading);
            return this;
        }

        /**
         * Adds a timestamp to be displayed at the end of the row. Only one timestamp can be added,
         * if one is already added this will throw {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(long timeStamp) {
            if (mHasTimestamp) {
                throw new IllegalArgumentException("Trying to add a timestamp when one has "
                        + "already been added");
            }
            mImpl.addEndItem(timeStamp);
            mHasTimestamp = true;
            return this;
        }

        /**
         * Adds an icon to be displayed at the end of the row. A mixture of icons and actions
         * is not permitted. If an action has already been added this will throw
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull Icon icon) {
            return addEndItem(icon, false /* isLoading */);
        }

        /**
         * Adds an icon to be displayed at the end of the row. A mixture of icons and actions
         * is not permitted. If an action has already been added this will throw
         * {@link IllegalArgumentException}.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull Icon icon, boolean isLoading) {
            if (mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when an"
                        + "action has already been added. End items cannot have a mixture of "
                        + "actions and icons.");
            }
            mImpl.addEndItem(icon, isLoading);
            mHasEndImage = true;
            return this;
        }

        /**
         * Adds an action to display at the end of the row. A mixture of icons and
         * actions is not permitted. If an icon has already been added, this will throw
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull SliceAction action) {
            return addEndItem(action, false /* isLoading */);
        }

        /**
         * Adds an action to be displayed at the end of the row. A mixture of icons and
         * actions is not permitted. If an icon has already been added, this will throw
         * {@link IllegalArgumentException}.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull SliceAction action, boolean isLoading) {
            if (mHasEndImage) {
                throw new IllegalArgumentException("Trying to add an action to end items when an"
                        + "icon has already been added. End items cannot have a mixture of "
                        + "actions and icons.");
            }
            if (mHasDefaultToggle) {
                throw new IllegalStateException("Only one non-custom toggle can be added "
                        + "in a single row. If you would like to include multiple toggles "
                        + "in a row, set a custom icon for each toggle.");
            }
            mImpl.addEndItem(action, isLoading);
            mHasDefaultToggle = action.isDefaultToggle();
            mHasEndActionOrToggle = true;
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.RowBuilder) impl;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public androidx.slice.builders.impl.ListBuilder.RowBuilder getImpl() {
            return mImpl;
        }

    }

    /**
     * Builder to construct a header. A header is displayed at the top of a list and can have
     * a title, subtitle, and an action.
     *
     * @see ListBuilder#setHeader(HeaderBuilder)
     */
    public static class HeaderBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.HeaderBuilder mImpl;

        /**
         * Create builder for header templates.
         */
        public HeaderBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createHeaderBuilder());
        }

        /**
         * Create builder for header templates.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public HeaderBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createHeaderBuilder(uri));
        }

        /**
         * Sets the title to be shown in this header.
         */
        @NonNull
        public HeaderBuilder setTitle(@NonNull CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }

        /**
         * Sets the subtitle to be shown in this header.
         */
        @NonNull
        public HeaderBuilder setSubtitle(@NonNull CharSequence subtitle) {
            mImpl.setSubtitle(subtitle);
            return this;
        }

        /**
         * Sets the summary subtitle to be shown in this header. If unset, the normal subtitle
         * will be used. The summary is used when the parent template is presented in a
         * small format.
         */
        @NonNull
        public HeaderBuilder setSummarySubtitle(@NonNull CharSequence summarySubtitle) {
            mImpl.setSummarySubtitle(summarySubtitle);
            return this;
        }

        /**
         * Sets the action to invoke when the header is activated.
         */
        @NonNull
        public HeaderBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.HeaderBuilder) impl;
        }
    }
}
