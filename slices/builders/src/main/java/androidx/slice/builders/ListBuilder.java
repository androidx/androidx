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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.ListBuilderBasicImpl;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;
import androidx.slice.core.SliceHints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A slice can be constructed with ListBuilder.
 * <p>
 * A slice is a piece of templated app content that can be presented outside of the app
 * in a {@link androidx.slice.widget.SliceView}. To provide a slice you should implement a
 * {@link androidx.slice.SliceProvider} and use ListBuilder to construct your slice when
 * {@link androidx.slice.SliceProvider#onBindSlice(Uri)} is called.
 * <p>
 * ListBuilder allows you to construct a slice made up of rows of content. A list should
 * have at least one row of content as well as a primary {@link SliceAction}. The row types that
 * ListBuilder supports are:
 * <ul>
 *     <li>{@link HeaderBuilder} - A list can have one header which appears at the top of the list.
 *     The header can support showing title, subtitle, and a row action. A header can also have
 *     a summary of the contents of the slice which can be shown when not all of the slice can be
 *     displayed.
 *     </li>
 *     <li>{@link RowBuilder} - A basic row supports title, subtitle, timestamps, images, row
 *     action, icon actions, and toggle actions.
 *     </li>
 *     <li>{@link GridRowBuilder} - A grid row supports cells of vertically laid out content
 *     displayed in a single row.
 *     </li>
 *     <li>{@link RangeBuilder} - A range row supports displaying a horizontal progress indicator.
 *     </li>
 *     <li>{@link InputRangeBuilder} - An input range row supports displaying a horizontal slider
 *     allowing slider input (e.g. brightness or volume slider).
 *     </li>
 * </ul>
 * <p>
 * In addition to rows of content, ListBuilder can also have {@link SliceAction}s added to it. These
 * actions may appear differently on your slice depending on how the
 * {@link androidx.slice.widget.SliceView} is configured. Normally the actions would appear in
 * the header.
 * <p>
 * To ensure your slice is presented correctly you should consider the configurations
 * {@link androidx.slice.widget.SliceView} supports:
 * <ul>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SHORTCUT} - The primary {@link SliceAction}
 *     of the slice is used your primary action should contain an image and title representative
 *     of your slice. If providing a tintable icon, use {@link #setAccentColor(int)} to specify the
 *     color. If a header has been specified for the list, the primary action associated with it
 *     will be used, otherwise it will be the primary action associated with the first row of the
 *     list.
 *     </li>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SMALL} - Only a single row of content is
 *     displayed in small format. If a header has been specified it will be displayed. If no header
 *     was set, then the first row will be used and may appear differently depending on the row
 *     type and the configuration of {@link androidx.slice.widget.SliceView}.
 *     </li>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_LARGE} - As many rows of content are shown
 *     as possible. If the presenter of the slice allows scrolling then all rows of content will
 *     be displayed in a scrollable view.
 *     </li>
 * </ul>
 * <p>
 * Note that scrolling on SliceView can be disabled, in which case only the header and one or two
 * rows of content may be shown for your slice. If your slice contains many rows of content to
 * scroll through (e.g. list of wifi networks), consider using
 * {@link #addSeeMoreAction(PendingIntent)} to provide a link to open the activity associated with
 * the content.
 *
 * @see HeaderBuilder
 * @see RowBuilder
 * @see GridRowBuilder
 * @see RangeBuilder
 * @see InputRangeBuilder
 * @see SliceAction
 * @see androidx.slice.SliceProvider
 * @see androidx.slice.SliceProvider#onBindSlice(Uri)
 * @see androidx.slice.widget.SliceView
 */
public class ListBuilder extends TemplateSliceBuilder {

    private boolean mHasSeeMore;
    private androidx.slice.builders.impl.ListBuilder mImpl;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            LARGE_IMAGE, SMALL_IMAGE, ICON_IMAGE, UNKNOWN_IMAGE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageMode{}

    /**
     * Indicates that an image should be presented as an icon and it can be tinted.
     */
    public static final int ICON_IMAGE = SliceHints.ICON_IMAGE;
    /**
     * Indicates that an image should be presented in a smaller size and it shouldn't be tinted.
     */
    public static final int SMALL_IMAGE = SliceHints.SMALL_IMAGE;
    /**
     * Indicates that an image presented in a larger size and it shouldn't be tinted.
     */
    public static final int LARGE_IMAGE = SliceHints.LARGE_IMAGE;
    /**
     * Indicates that an image mode is unknown.
     */
    public static final int UNKNOWN_IMAGE = SliceHints.UNKNOWN_IMAGE;

    /**
     * Constant representing infinity.
     */
    public static final long INFINITY = SliceHints.INFINITY;

    /**
     * Create a builder which will construct a slice made up of rows of content.
     *
     * @param uri Uri to tag for this slice.
     *
     * @deprecated TO BE REMOVED; use {@link #ListBuilder(Context, Uri, long)}.
     */
    @Deprecated
    public ListBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
    }

    /**
     * Create a builder which will construct a slice that will display rows of content.
     * <p>
     * A slice requires an associated time-to-live, i.e. how long the data contained in the slice
     * can remain fresh. If your slice has content that is not time sensitive, set a TTL of
     * {@link #INFINITY}.
     *
     * @param uri Uri to tag for this slice.
     * @param ttl the length in milliseconds that the content in this slice can live for.
     */
    public ListBuilder(@NonNull Context context, @NonNull Uri uri, long ttl) {
        super(context, uri);
        mImpl.setTtl(ttl);
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (androidx.slice.builders.impl.ListBuilder) impl;
    }

    /**
     * Add a row to the list builder.
     */
    @NonNull
    public ListBuilder addRow(@NonNull RowBuilder builder) {
        mImpl.addRow((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Add a row to the list builder.
     */
    @NonNull
    public ListBuilder addRow(@NonNull Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return addRow(b);
    }

    /**
     * Add a grid row to the list builder.
     *
     * @deprecated TO BE REMOVED; use {@link #addGridRow(GridRowBuilder)} instead
     */
    @NonNull
    @Deprecated
    public ListBuilder addGrid(@NonNull GridBuilder builder) {
        mImpl.addGridRow((TemplateBuilderImpl) builder.getImpl());
        return this;
    }

    /**
     * Add a grid row to the list builder.
     *
     * @deprecated TO BE REMOVED; use {@link #addGridRow(GridRowBuilder)} instead
     */
    @NonNull
    @Deprecated
    public ListBuilder addGrid(@NonNull Consumer<GridBuilder> c) {
        GridBuilder b = new GridBuilder(this);
        c.accept(b);
        return addGrid(b);
    }

    /**
     * Add a grid row to the list builder.
     */
    @NonNull
    public ListBuilder addGridRow(@NonNull GridRowBuilder builder) {
        mImpl.addGridRow((TemplateBuilderImpl) builder.getImpl());
        return this;
    }

    /**
     * Add a grid row to the list builder.
     */
    @NonNull
    public ListBuilder addGridRow(@NonNull Consumer<GridRowBuilder> c) {
        GridRowBuilder b = new GridRowBuilder(this);
        c.accept(b);
        return addGridRow(b);
    }

    /**
     * Sets a header for this list builder. A list can have only one header. Setting a header allows
     * some flexibility in what's displayed in your slice when SliceView displays in
     * {@link androidx.slice.widget.SliceView#MODE_SMALL} and
     * {@link androidx.slice.widget.SliceView#MODE_SHORTCUT}.
     * <p>
     * In MODE_SMALL, the header row shown if one has been added. The header will also
     * display the {@link HeaderBuilder#setSummary(CharSequence)} text if it has been
     * specified, allowing a summary of otherwise hidden content to be shown.
     * <p>
     * In MODE_SHORTCUT, the primary action set using
     * {@link HeaderBuilder#setPrimaryAction(SliceAction)} will be used for the shortcut
     * representation.
     *
     * @see HeaderBuilder#setSummary(CharSequence)
     */
    @NonNull
    public ListBuilder setHeader(@NonNull HeaderBuilder builder) {
        mImpl.setHeader((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Sets a header for this list builder. A list can have only one header. Setting a header allows
     * some flexibility in what's displayed in your slice when SliceView displays in
     * {@link androidx.slice.widget.SliceView#MODE_SMALL} and
     * {@link androidx.slice.widget.SliceView#MODE_SHORTCUT}.
     * <p>
     * In MODE_SMALL, the header row shown if one has been added. The header will also
     * display the {@link HeaderBuilder#setSummary(CharSequence)} text if it has been
     * specified, allowing a summary of otherwise hidden content to be shown.
     * <p>
     * In MODE_SHORTCUT, the primary action set using
     * {@link HeaderBuilder#setPrimaryAction(SliceAction)} will be used for the shortcut
     * representation.
     *
     * @see HeaderBuilder#setSummary(CharSequence)
     */
    @NonNull
    public ListBuilder setHeader(@NonNull Consumer<HeaderBuilder> c) {
        HeaderBuilder b = new HeaderBuilder(this);
        c.accept(b);
        return setHeader(b);
    }

    /**
     * Adds an action to this list builder.
     * <p>
     * Actions added with this method are grouped together on the list template. These actions may
     * appear differently on the slice depending on how the {@link androidx.slice.widget.SliceView}
     * is configured. Generally these actions will be  displayed in the order they were added,
     * however, if not all actions can be displayed then actions with a higher priority may be shown
     * first.
     *
     * @see SliceAction
     * @see SliceAction#setPriority(int)
     * @see androidx.slice.widget.SliceView#setActions(List, int)
     */
    @NonNull
    public ListBuilder addAction(@NonNull SliceAction action) {
        mImpl.addAction(action);
        return this;
    }

    /**
     * @deprecated TO BE REMOVED; use {@link #setAccentColor(int)} instead.
     */
    @Deprecated
    @NonNull
    public ListBuilder setColor(@ColorInt int color) {
        return setAccentColor(color);
    }

    /**
     * Sets the color to use on tintable items within the list builder.
     * Things that might be tinted are:
     * <ul>
     *     <li>Any {@link SliceAction}s within your slice.
     *     </li>
     *     <li>UI controls such as {@link android.widget.Switch}s or {@link android.widget.SeekBar}s
     *     </li>
     *     <li>Tinting the {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation
     *     </li>
     * </ul>
     */
    @NonNull
    public ListBuilder setAccentColor(@ColorInt int color) {
        mImpl.setColor(color);
        return this;
    }

    /**
     * Sets keywords to associate with this slice.
     */
    @NonNull
    public ListBuilder setKeywords(List<String> keywords) {
        mImpl.setKeywords(keywords);
        return this;
    }


    /**
     * If all content in a slice cannot be shown, the row added here may be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * This method should only be used if you want to display a customized row to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom row, the row should have a content intent or action end item
     * specified to take the user to an activity to see all of the content. The row added here
     * will only appear when not all content can be displayed and it will not be styled any
     * differently from row built by {@link RowBuilder} normally.
     * </p>
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public ListBuilder setSeeMoreRow(@NonNull RowBuilder builder) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more row when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreRow((TemplateBuilderImpl) builder.mImpl);
        mHasSeeMore = true;
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the row added here may be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * This method should only be used if you want to display a customized row to indicate more
     * content, consider using {@link #addSeeMoreAction(PendingIntent)} otherwise. If you do
     * choose to specify a custom row, the row should have a content intent or action end item
     * specified to take the user to an activity to see all of the content. The row added here
     * will only appear when not all content can be displayed and it will not be styled any
     * differently from row built by {@link RowBuilder} normally.
     * </p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public ListBuilder setSeeMoreRow(@NonNull Consumer<RowBuilder> c) {
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
    public ListBuilder setSeeMoreAction(@NonNull PendingIntent intent) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreAction(intent);
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
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     *
     * @deprecated TO BE REMOVED
     */
    @NonNull
    @Deprecated
    public ListBuilder addSeeMoreRow(@NonNull RowBuilder builder) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more row when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreRow((TemplateBuilderImpl) builder.mImpl);
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
     *
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @NonNull
    public ListBuilder addSeeMoreRow(@NonNull Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return setSeeMoreRow(b);
    }

    /**
     * If all content in a slice cannot be shown, a "see more" affordance may be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     *
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @NonNull
    public ListBuilder addSeeMoreAction(@NonNull PendingIntent intent) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreAction(intent);
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
     * Builder to construct a range row which can be added to a {@link ListBuilder}.
     * <p>
     * A range row supports displaying a horizontal progress indicator.
     *
     * @see ListBuilder#addRange(Consumer)
     * @see ListBuilder#addRange(RangeBuilder)
     */
    public static class RangeBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.RangeBuilder mImpl;

        /**
         * Builder to construct a range row which can be added to a {@link ListBuilder}.
         * <p>
         * A range row supports displaying a horizontal progress indicator.
         *
         * @see ListBuilder#addRange(Consumer)
         * @see ListBuilder#addRange(RangeBuilder)
         */
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

        /**
         * Set the subtitle.
         */
        @NonNull
        public RangeBuilder setSubtitle(@NonNull CharSequence title) {
            mImpl.setSubtitle(title);
            return this;
        }

        /**
         * Set the primary action for this row.
         * <p>
         * The action specified here will be sent when the whole row is clicked. If this
         * is the first row in a {@link ListBuilder} this action will also be used to define
         * the {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation of the slice.
         */
        @NonNull
        public RangeBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        /**
         * Sets the content description.
         */
        @NonNull
        public RangeBuilder setContentDescription(@NonNull CharSequence description) {
            mImpl.setContentDescription(description);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.RangeBuilder) impl;
        }
    }

    /**
     * Builder to construct a input range row.
     * <p>
     * An input range row supports displaying a horizontal slider allowing slider input.
     *
     * @see ListBuilder#addInputRange(Consumer)
     * @see ListBuilder#addInputRange(InputRangeBuilder)
     */
    public static class InputRangeBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.InputRangeBuilder mImpl;

        /**
         * Builder to construct a input range row.
         * <p>
         * An input range row supports displaying a horizontal slider allowing slider input.
         *
         * @see ListBuilder#addInputRange(Consumer)
         * @see ListBuilder#addInputRange(InputRangeBuilder)
         */
        public InputRangeBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createInputRangeBuilder());
        }

        /**
         * Set the lower limit of the range. The default is 0.
         */
        @NonNull
        public InputRangeBuilder setMin(int min) {
            mImpl.setMin(min);
            return this;
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
         * Set the subtitle.
         */
        @NonNull
        public InputRangeBuilder setSubtitle(@NonNull CharSequence title) {
            mImpl.setSubtitle(title);
            return this;
        }

        /**
         * Set the {@link PendingIntent} to send when the current value is updated.
         *
         * @deprecated TO BE REMOVED; use {@link InputRangeBuilder#setInputAction(PendingIntent)}
         */
        @Deprecated
        @NonNull
        public InputRangeBuilder setAction(@NonNull PendingIntent action) {
            mImpl.setInputAction(action);
            return this;
        }

        /**
         * Set the {@link PendingIntent} to send when the current value is updated.
         */
        @NonNull
        public InputRangeBuilder setInputAction(@NonNull PendingIntent action) {
            mImpl.setInputAction(action);
            return this;
        }

        /**
         * @deprecated TO BE REMOVED
         */
        @NonNull
        @RequiresApi(23)
        public InputRangeBuilder setThumb(@NonNull Icon thumb) {
            return setThumb(IconCompat.createFromIcon(thumb));
        }

        /**
         * Set the {@link Icon} to be displayed as the thumb on the input range.
         */
        @NonNull
        public InputRangeBuilder setThumb(@NonNull IconCompat thumb) {
            mImpl.setThumb(thumb);
            return this;
        }

        /**
         * Set the primary action for this row.
         * <p>
         * The action specified here will be sent when the whole row is clicked, whereas
         * the action specified via {@link #setAction(PendingIntent)} is used when the slider
         * is interacted with. Additionally, if this is the first row in a {@link ListBuilder} this
         * action will also be used to define the
         * {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation of the slice.
         */
        @NonNull
        public InputRangeBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        /**
         * Sets the content description.
         */
        @NonNull
        public InputRangeBuilder setContentDescription(@NonNull CharSequence description) {
            mImpl.setContentDescription(description);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.InputRangeBuilder) impl;
        }
    }

    /**
     * Builder to construct a row. A row can be added as an item to ListBuilder via
     * {@link ListBuilder#addRow(RowBuilder)}.
     * <p>
     * A row supports:
     * <ul>
     *     <li>Title item - The title item appears with at the start of the row. There can only
     *     be one title item added to a row, and it could be a timestamp, image, or a
     *     {@link SliceAction}.
     *     </li>
     *     <li>Title - Single line of text formatted as a title, see
     *     {@link #setTitle(CharSequence)}.
     *     </li>
     *     <li>Subtitle - Single line of text below the title (if one exists) and is formatted as
     *     normal text, see {@link #setSubtitle(CharSequence)}.
     *     </li>
     *     <li>End item -  End items appear at the end of the row. There can be multiple end items
     *     that show depending on available width. End items can be a timestamp, image, or a
     *     tappable icon.
     *     </li>
     *     <li>Primary action - The primary action for the row, this is the action that will be sent
     *     when the row is clicked. This is set via {@link #setPrimaryAction(SliceAction)}. If this
     *     is the only row in a slice, the slice  action set here will be used to represent the
     *     slice in when slice view is displaying in
     *     {@link androidx.slice.widget.SliceView#MODE_SMALL}.
     *     </li>
     * </ul>
     * There are a couple of restrictions to how content can be added to a row:
     * <ul>
     *     <li>End items cannot contain a mixture of {@link SliceAction}s and Icons.</li>
     *     <li>There can only be one timestamp added to the row.</li>
     * </ul>
     *
     * @see ListBuilder#addRow(RowBuilder)
     */
    public static class RowBuilder extends TemplateSliceBuilder {

        private androidx.slice.builders.impl.ListBuilder.RowBuilder mImpl;

        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private boolean mHasDefaultToggle;
        private boolean mHasTimestamp;

        /**
         * Builder to construct a row.
         * @param parent The builder constructing the parent slice.
         */
        public RowBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createRowBuilder());
        }

        /**
         * Builder to construct a row.
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createRowBuilder(uri));
        }

        /**
         * Builder to construct a normal row.
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
         * @deprecated TO BE REMOVED.
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder setTitleItem(@NonNull Icon icon) {
            return setTitleItem(icon, ICON_IMAGE);
        }

        /**
         * @deprecated TO BE REMOVED.
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder setTitleItem(@Nullable Icon icon, boolean isLoading) {
            return setTitleItem(icon, ICON_IMAGE, isLoading);
        }

        /**
         * @deprecated TO BE REMOVED.
         */
        @Deprecated
        @RequiresApi(23)
        public RowBuilder setTitleItem(@NonNull Icon icon, @ImageMode int imageMode) {
            mImpl.setTitleItem(IconCompat.createFromIcon(icon), imageMode, false /* isLoading */);
            return this;
        }

        /**
         * @deprecated TO BE REMOVED.
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder setTitleItem(@Nullable Icon icon, @ImageMode int imageMode,
                boolean isLoading) {
            mImpl.setTitleItem(IconCompat.createFromIcon(icon), imageMode,
                    isLoading /* isLoading */);
            return this;
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this
         * will replace any other title items that may have been set.
         *
         * @deprecated TO BE REMOVED; use ListBuilder{@link #setTitleItem(Icon, int, boolean)}.
         */
        @Deprecated
        @NonNull
        public RowBuilder setTitleItem(@NonNull IconCompat icon) {
            return setTitleItem(icon, ICON_IMAGE);
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
         *
         * @deprecated TO BE REMOVED; use ListBuilder{@link #setTitleItem(Icon, int, boolean)}.
         */
        @Deprecated
        @NonNull
        public RowBuilder setTitleItem(@Nullable IconCompat icon, boolean isLoading) {
            return setTitleItem(icon, ICON_IMAGE, isLoading);
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this
         * will replace any other title items that may have been set.
         *
         * @param icon the image to display.
         * @param imageMode the mode that image should be displayed in.
         *
         * @see #ICON_IMAGE
         * @see #SMALL_IMAGE
         * @see #LARGE_IMAGE
         */
        public RowBuilder setTitleItem(@NonNull IconCompat icon, @ImageMode int imageMode) {
            mImpl.setTitleItem(icon, imageMode, false /* isLoading */);
            return this;
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this
         * will replace any other title items that may have been set.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.
         *
         * @param icon the image to display.
         * @param imageMode the mode that image should be displayed in.
         * @param isLoading whether this content is being loaded in the background.
         *
         * @see #ICON_IMAGE
         * @see #SMALL_IMAGE
         * @see #LARGE_IMAGE
         */
        @NonNull
        public RowBuilder setTitleItem(@Nullable IconCompat icon, @ImageMode int imageMode,
                boolean isLoading) {
            mImpl.setTitleItem(icon, imageMode, isLoading /* isLoading */);
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
         * The action specified here will be sent when the whole row is clicked.
         * <p>
         * If this is the first row in a {@link ListBuilder} this action will also be used to define
         * the {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation of the slice.
         */
        @NonNull
        public RowBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        /**
         * Sets the title for the row builder.
         */
        @NonNull
        public RowBuilder setTitle(@NonNull CharSequence title) {
            mImpl.setTitle(title);
            return this;
        }

        /**
         * Sets the title for the row builder.
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
         * Sets the subtitle for the row builder.
         */
        @NonNull
        public RowBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle for the row builder.
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
         * Adds a timestamp to the end items of the row builder. Only one timestamp can be added,
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
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder addEndItem(@NonNull Icon icon) {
            return addEndItem(icon, ICON_IMAGE, false /* isLoading */);
        }

        /**
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder addEndItem(@NonNull Icon icon, boolean isLoading) {
            return addEndItem(icon, ICON_IMAGE, isLoading);
        }

        /**
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder addEndItem(@NonNull Icon icon, @ImageMode int imageMode) {
            return addEndItem(icon, imageMode, false /* isLoading */);
        }

        /**
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        @NonNull
        @RequiresApi(23)
        public RowBuilder addEndItem(@Nullable Icon icon, @ImageMode int imageMode,
                boolean isLoading) {
            if (mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when an"
                        + "action has already been added. End items cannot have a mixture of "
                        + "actions and icons.");
            }
            mImpl.addEndItem(IconCompat.createFromIcon(icon), imageMode, isLoading);
            mHasEndImage = true;
            return this;
        }

        /**
         * Adds an icon to the end items of the row builder. A mixture of icons and actions
         * is not permitted. If an action has already been added this will throw
         * {@link IllegalArgumentException}.
         *
         * @deprecated use ListBuilder{@link #addEndItem(Icon, int)} instead.
         */
        @Deprecated
        @NonNull
        public RowBuilder addEndItem(@NonNull IconCompat icon) {
            return addEndItem(icon, ICON_IMAGE, false /* isLoading */);
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
         *
         * @deprecated use ListBuilder{@link #addEndItem(Icon, int, boolean)} instead.
         */
        @Deprecated
        @NonNull
        public RowBuilder addEndItem(@NonNull IconCompat icon, boolean isLoading) {
            return addEndItem(icon, ICON_IMAGE, isLoading);
        }

        /**
         * Adds an icon to the end items of the row builder.
         *
         * @param icon the image to display.
         * @param imageMode the mode that image should be displayed in.
         *
         * @see #ICON_IMAGE
         * @see #SMALL_IMAGE
         * @see #LARGE_IMAGE
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull IconCompat icon, @ImageMode int imageMode) {
            return addEndItem(icon, imageMode, false /* isLoading */);
        }

        /**
         * Adds an icon to the end items of the row builder.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.
         *
         * @param icon the image to display.
         * @param imageMode the mode that image should be displayed in.
         * @param isLoading whether this content is being loaded in the background.
         *
         * @see #ICON_IMAGE
         * @see #SMALL_IMAGE
         * @see #LARGE_IMAGE
         */
        @NonNull
        public RowBuilder addEndItem(@Nullable IconCompat icon, @ImageMode int imageMode,
                boolean isLoading) {
            if (mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when an"
                        + "action has already been added. End items cannot have a mixture of "
                        + "actions and icons.");
            }
            mImpl.addEndItem(icon, imageMode, isLoading);
            mHasEndImage = true;
            return this;
        }

        /**
         * Adds an action to the end items of the row builder. A mixture of icons and
         * actions is not permitted. If an icon has already been added, this will throw
         * {@link IllegalArgumentException}.
         */
        @NonNull
        public RowBuilder addEndItem(@NonNull SliceAction action) {
            return addEndItem(action, false /* isLoading */);
        }

        /**
         * Adds an action to the end items of the row builder. A mixture of icons and
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
            mHasDefaultToggle = action.getImpl().isDefaultToggle();
            mHasEndActionOrToggle = true;
            return this;
        }

        /**
         * Sets the content description for the row.
         */
        @NonNull
        public RowBuilder setContentDescription(@NonNull CharSequence description) {
            mImpl.setContentDescription(description);
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
     * Builder to construct a header row.
     * <p>
     * A header provides some additional functionality compared to a {@link RowBuilder}. Like
     * a row, a header has a title, subtitle, and primary action.
     * <p>
     * In addition to a row's title, subtitle, and primary action, a header also supports setting
     * a summary description of the list contents using
     * {@link HeaderBuilder#setSummary(CharSequence)}. This summary might be used when
     * the rest of the list content is not shown (e.g. if SliceView presenting slice is
     * configured to {@link androidx.slice.widget.SliceView#MODE_SMALL}.
     * <p>
     * The primary action specified by {@link HeaderBuilder#setPrimaryAction(SliceAction)} will
     * be used as the PendingIntent sent when header is clicked. This action is also used when
     * when SliceView displays in {@link androidx.slice.widget.SliceView#MODE_SHORTCUT}.
     * <p>
     * Unlike row builder, header builder does not support end items (e.g.
     * {@link RowBuilder#addEndItem(SliceAction)}). The header may be used to display actions set
     * on the list via {@link #addAction(SliceAction)}.
     *
     * @see ListBuilder#setHeader(HeaderBuilder)
     * @see ListBuilder#addAction(SliceAction)
     * @see SliceAction
     */
    public static class HeaderBuilder extends TemplateSliceBuilder {
        private androidx.slice.builders.impl.ListBuilder.HeaderBuilder mImpl;

        /**
         * Create builder for a header.
         */
        public HeaderBuilder(@NonNull ListBuilder parent) {
            super(parent.mImpl.createHeaderBuilder());
        }

        /**
         * Create builder for a header.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public HeaderBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            super(parent.mImpl.createHeaderBuilder(uri));
        }

        /**
         * Sets the title for the header builder.
         */
        @NonNull
        public HeaderBuilder setTitle(@NonNull CharSequence title) {
            return setTitle(title, false /* isLoading */);
        }

        /**
         * Sets the title for the header builder.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public HeaderBuilder setTitle(@NonNull CharSequence title, boolean isLoading) {
            mImpl.setTitle(title, isLoading);
            return this;
        }

        /**
         * Sets the subtitle for the header builder.
         */
        @NonNull
        public HeaderBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle for the header builder.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public HeaderBuilder setSubtitle(@NonNull CharSequence subtitle, boolean isLoading) {
            mImpl.setSubtitle(subtitle, isLoading);
            return this;
        }

        /**
         * Sets the summary subtitle to be shown in this header. If unset, the normal subtitle
         * will be used. The summary is used when the parent template is presented in a
         * small format.
         * @deprecated TO BE REMOVED; use {@link #setSummary(CharSequence)}
         */
        @NonNull
        public HeaderBuilder setSummarySubtitle(@NonNull CharSequence summarySubtitle) {
            return setSummary(summarySubtitle, false /* isLoading */);
        }

        /**
         * Sets the summary subtitle to be shown in this header. If unset, the normal subtitle
         * will be used. The summary is used when the parent template is presented in a
         * small format.
         * @deprecated TO BE REMOVED; use {@link #setSummary(CharSequence, boolean)}
         */
        @NonNull
        public HeaderBuilder setSummarySubtitle(@NonNull CharSequence summarySubtitle,
                boolean isLoading) {
            return setSummary(summarySubtitle, isLoading /* isLoading */);
        }

        /**
         * Sets the summary for the header builder. A summary is optional.
         * <p>
         * The summary should be a description of the contents of the list. This summary might be
         * used when the rest of the list content is not shown (e.g. if SliceView presenting slice
         * is configured to {@link androidx.slice.widget.SliceView#MODE_SMALL}.
         */
        @NonNull
        public HeaderBuilder setSummary(@NonNull CharSequence summary) {
            return setSummary(summary, false /* isLoading */);
        }

        /**
         * Sets the summary for the header builder. A summary is optional.
         * <p>
         * The summary should be a description of the contents of the list. This summary might be
         * used when the rest of the list content is not shown (e.g. if SliceView presenting slice
         * is configured to {@link androidx.slice.widget.SliceView#MODE_SMALL}.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public HeaderBuilder setSummary(@NonNull CharSequence summary, boolean isLoading) {
            mImpl.setSummary(summary, isLoading);
            return this;
        }

        /**
         * Sets the action to send when the header is clicked.
         * <p>
         * Additionally, the action specified here is used when the slice associated with this
         * header is displayed in {@link androidx.slice.widget.SliceView#MODE_SHORTCUT}.
         */
        @NonNull
        public HeaderBuilder setPrimaryAction(@NonNull SliceAction action) {
            mImpl.setPrimaryAction(action);
            return this;
        }

        /**
         * Sets the content description for the header.
         */
        @NonNull
        public HeaderBuilder setContentDescription(@NonNull CharSequence description) {
            mImpl.setContentDescription(description);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (androidx.slice.builders.impl.ListBuilder.HeaderBuilder) impl;
        }
    }
}
