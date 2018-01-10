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
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpecs;
import androidx.app.slice.builders.impl.ListBuilderBasicImpl;
import androidx.app.slice.builders.impl.ListBuilderV1Impl;
import androidx.app.slice.builders.impl.TemplateBuilderImpl;

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
 *         row to display in small format see {@link #addSummaryRow(RowBuilder)}.</li>
 *     <li>Large - As many rows of content are shown as possible. If the presenter of the slice
 *         allows scrolling then all rows of content will be displayed in a scrollable view.</li>
 * </ul>
 * </p>
 *
 * @see RowBuilder
 */
public class ListBuilder extends TemplateSliceBuilder {

    private boolean mHasSummary;
    private androidx.app.slice.builders.impl.ListBuilder mImpl;

    /**
     */
    public ListBuilder(@NonNull Uri uri) {
        super(uri);
        throw new RuntimeException("Stub, to be removed");
    }

    /**
     * Create a builder which will construct a slice that will display rows of content.
     * @param uri Uri to tag for this slice.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (androidx.app.slice.builders.impl.ListBuilder) impl;
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
     * Add a summary row for this template. The summary content is displayed
     * when the slice is displayed in small format.
     * <p>
     * Only one summary row can be added, this throws {@link IllegalArgumentException} if
     * called more than once.
     * </p>
     */
    public ListBuilder addSummaryRow(RowBuilder builder) {
        if (mHasSummary) {
            throw new IllegalArgumentException("Trying to add summary row when one has "
                    + "already been added");
        }
        mImpl.addSummaryRow((TemplateBuilderImpl) builder.mImpl);
        mHasSummary = true;
        return this;
    }

    /**
     * Add a summary row for this template. The summary content is displayed
     * when the slice is displayed in small format.
     * <p>
     * Only one summary row can be added, this throws {@link IllegalArgumentException} if
     * called more than once.
     * </p>
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ListBuilder addSummaryRow(Consumer<RowBuilder> c) {
        if (mHasSummary) {
            throw new IllegalArgumentException("Trying to add summary row when one has "
                    + "already been added");
        }
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        mImpl.addSummaryRow((TemplateBuilderImpl) b.mImpl);
        mHasSummary = true;
        return this;
    }

    /**
     * Sets the color to tint items displayed by this template (e.g. icons).
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public ListBuilder setColor(int color) {
        mImpl.setColor(color);
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
    public androidx.app.slice.builders.impl.ListBuilder getImpl() {
        return mImpl;
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

        private androidx.app.slice.builders.impl.ListBuilder.RowBuilder mImpl;

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
         */
        public RowBuilder(@NonNull Uri uri) {
            super(uri);
            throw new RuntimeException("Stub, to be removed");
        }

        /**
         * Create a builder which will construct a slice displayed in a row format.
         * @param uri Uri to tag for this slice.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public RowBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createRowBuilder(uri));
        }

        /**
         * Create a builder which will construct a slice displayed in a row format.
         * @param uri Uri to tag for this slice.
         * @hide
         */
        @RestrictTo(LIBRARY)
        public RowBuilder(@NonNull Context context, @NonNull Uri uri) {
            super(new ListBuilder(context, uri).mImpl.createRowBuilder(uri));
        }

        /**
         * Sets this row to be the header of the slice. This item will be displayed at the top of
         * the slice and other items in the slice will scroll below it.
         */
        @NonNull
        public RowBuilder setIsHeader(boolean isHeader) {
            mImpl.setIsHeader(isHeader);
            return this;
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
         * Sets the title item to be the provided icon.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        @NonNull
        public RowBuilder setTitleItem(@NonNull Icon icon) {
            mImpl.setTitleItem(icon);
            return this;
        }

        /**
         * Sets the title item to be a tappable icon.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        @NonNull
        public RowBuilder setTitleItem(@NonNull Icon icon, @NonNull PendingIntent action) {
            mImpl.setTitleItem(icon, action);
            return this;
        }

        /**
         * Sets the action to be invoked if the user taps on the main content of the template.
         */
        @NonNull
        public RowBuilder setContentIntent(@NonNull PendingIntent action) {
            mImpl.setContentIntent(action);
            return this;
        }

        /**
         * Sets the title text.
         */
        @NonNull
        public RowBuilder setTitle(CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }

        /**
         * Sets the subtitle text.
         */
        @NonNull
        public RowBuilder setSubtitle(CharSequence subtitle) {
            mImpl.setSubtitle(subtitle);
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
         * Adds an icon to be displayed at the end of the row. A mixture of icons and tappable
         * icons is not permitted. If an action has already been added this will throw
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull Icon icon) {
            if (mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when an"
                        + "action has already been added. End items cannot have a mixture of "
                        + "tappable icons and icons.");
            }
            mImpl.addEndItem(icon);
            mHasEndImage = true;
            return this;
        }

        /**
         * Adds a tappable icon to be displayed at the end of the row. A mixture of icons and
         * tappable icons is not permitted. If an icon has already been added, this will throw
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull Icon icon, @NonNull PendingIntent action) {
            if (mHasEndImage) {
                throw new IllegalArgumentException("Trying to add an action to end items when an"
                        + "icon has already been added. End items cannot have a mixture of "
                        + "tappable icons and icons.");
            }
            mImpl.addEndItem(icon, action);
            mHasEndActionOrToggle = true;
            return this;
        }

        /**
         * Adds a toggle action to be displayed at the end of the row. A mixture of icons and
         * tappable icons is not permitted. If an icon has already been added, this will throw an
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addToggle(@NonNull PendingIntent action, boolean isChecked) {
            return addToggleInternal(action, isChecked, null);
        }

        /**
         * Adds a toggle action to be displayed with custom icons to represent checked and
         * unchecked state at the end of the row. A mixture of icons and tappable icons is not
         * permitted. If an icon has already been added, this will throw an
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addToggle(@NonNull PendingIntent action, boolean isChecked,
                @NonNull Icon icon) {
            return addToggleInternal(action, isChecked, icon);
        }

        private RowBuilder addToggleInternal(@NonNull PendingIntent action, boolean isChecked,
                @Nullable Icon icon) {
            if (mHasEndImage) {
                throw new IllegalStateException("Trying to add a toggle to end items when an "
                        + "icon has already been added. End items cannot have a mixture of "
                        + "tappable icons and icons.");
            }
            if (mHasDefaultToggle) {
                throw new IllegalStateException("Only one non-custom toggle can be added "
                        + "in a single row. If you would like to include multiple toggles "
                        + "in a row, set a custom icon for each toggle.");
            }
            mImpl.addToggle(action, isChecked, icon);
            mHasDefaultToggle = icon == null;
            mHasEndActionOrToggle = true;
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.app.slice.builders.impl.ListBuilder.RowBuilder) impl;
        }

        /**
         */
        public void apply(Slice.Builder builder) {
            throw new RuntimeException("Stub, to be removed");
        }
    }
}
