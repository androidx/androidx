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
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.remotecallback.RemoteCallback;
import androidx.slice.Slice;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.ListBuilderBasicImpl;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;
import androidx.slice.core.SliceHints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: include image examples in the example section when we can (b/111412886)
/**
 * Builder for constructing slices composed of rows of content.
 * <p>
 * A slice is a piece of templated app content that can be presented outside of the app providing
 * the slice in a {@link androidx.slice.widget.SliceView}. To provide a slice you should implement a
 * {@link androidx.slice.SliceProvider} and use ListBuilder to construct your slice when
 * {@link androidx.slice.SliceProvider#onBindSlice(Uri)} is called.
 * </p>
 * <p>
 * ListBuilder allows you to construct a slice made up of rows of content. The row types that
 * ListBuilder supports are:
 * <ul>
 *     <li>{@link HeaderBuilder} - The first row of your slice should be a header. The header
 *     supports a title, subtitle, and tappable row action. A header can also have a summary of the
 *     contents of the slice which can be shown when not all of the slice can be displayed.
 *     </li>
 *     <li>{@link RowBuilder} - A basic row supports title, subtitle, timestamps, and various action
 *     types.
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
 * </p>
 * <b>Handling modes</b>
 * <p>
 * Slices are meant to be presented outside of the app providing them, the slice presenter could be
 * an Android system surface or another application. The presenter will normally use a
 * {@link androidx.slice.widget.SliceView} to display a slice. SliceView supports a couple of
 * different modes. These modes are not controlled by the app providing the slice, but
 * rather by the surface that is presenting the slice. To ensure your slice is presented
 * correctly you should consider the different modes SliceView supports:
 * <ul>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SMALL} - Only the first row of content is
 *     displayed in small format, normally this will be the header. If no header was set, then the
 *     first row will be used and may appear differently depending on the row type and the
 *     configuration of {@link androidx.slice.widget.SliceView}.
 *     </li>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_LARGE} - As many rows of content are shown
 *     as possible. If the presenter of the slice allows scrolling then all rows of content will
 *     be displayed in a scrollable view.
 *     </li>
 *     <li>{@link androidx.slice.widget.SliceView#MODE_SHORTCUT} - In shortcut mode only a tappable
 *     image is displayed. The image and action used to represent this will be the primary action
 *     of your slice, i.e. {@link HeaderBuilder#setPrimaryAction(SliceAction)}.
 *     </li>
 * </ul>
 * </p>
 * <b>Specifying actions</b>
 * <p>
 * In addition to rows of content, ListBuilder can also have {@link SliceAction}s added to
 * it. These actions may appear differently on your slice depending on how
 * {@link androidx.slice.widget.SliceView} is configured. Normally the actions appear as icons in
 * the header of the slice.
 * </p>
 * <b>How much content to add</b>
 * <p>
 * There is no limit to the number of rows added to ListBuilder, however, the contents of a slice
 * should be related to a primary task, action, or set of information. For example: it might make
 * sense for a slice to manage wi-fi state to have a row for each available network, this might
 * result in a large number of rows but each of these rows serve utility for the primary purpose
 * of the slice which is managing wi-fi.
 * </p>
 * <p>
 * Note that scrolling on SliceView can be disabled, in which case only the header and one or two
 * rows of content may be shown for your slice. If your slice contains many rows of content to
 * scroll through (e.g. list of wifi networks), consider using
 * {@link #setSeeMoreAction(PendingIntent)} to provide a link to open the activity associated with
 * the content.
 * </p>
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
@RequiresApi(19)
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
     * Indicates that an image should be presented as an icon and it can be tinted.</p>
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_INHERIT,
            View.LAYOUT_DIRECTION_LOCALE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutDirection{}

    /**
     * Create a ListBuilder for constructing slice content.
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

    /**
     * Create a ListBuilder for constructing slice content.
     * <p>
     * A slice requires an associated time-to-live, i.e. how long the data contained in the slice
     * can remain fresh. If your slice has content that is not time sensitive, set {@link Duration}
     * to null and the TTL will be {@link #INFINITY}.
     *
     * @param uri Uri to tag for this slice.
     * @param ttl the {@link Duration} that the content in this slice can live for.
     */
    @RequiresApi(26)
    public ListBuilder(@NonNull Context context, @NonNull Uri uri, @Nullable Duration ttl) {
        super(context, uri);
        mImpl.setTtl(ttl);
    }

    /**
     * Construct the slice defined by this ListBuilder.
     * <p>
     * Note that a ListBuilder requires a row containing a piece of text that is not created
     * from a {@link GridRowBuilder}. If the first row added does not fulfill this requirement,
     * build the slice will throw {@link IllegalStateException}.
     * <p>
     * Note that a ListBuilder requires a primary action, this can be set on any of the rows added
     * to the list. If a primary action has not been set on any of the rows, building this slice
     * will throw {@link IllegalStateException}.
     *
     * @see HeaderBuilder#setPrimaryAction(SliceAction)
     * @see RowBuilder#setPrimaryAction(SliceAction)
     * @see GridRowBuilder#setPrimaryAction(SliceAction)
     * @see InputRangeBuilder#setPrimaryAction(SliceAction)
     * @see RangeBuilder#setPrimaryAction(SliceAction)
     */
    @NonNull
    @Override
    public Slice build() {
        return ((TemplateBuilderImpl) mImpl).build();
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
        mImpl.addRow(builder);
        return this;
    }

    /**
     * Add a grid row to the list builder.
     * <p>
     * Note that grid rows cannot be the first row in your slice. Adding a grid row first without
     * calling {@link #setHeader(HeaderBuilder)} will result in {@link IllegalStateException} when
     * the slice is built.
     */
    @NonNull
    public ListBuilder addGridRow(@NonNull GridRowBuilder builder) {
        mImpl.addGridRow(builder);
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
    public ListBuilder setHeader(@NonNull HeaderBuilder builder) {
        mImpl.setHeader(builder);
        return this;
    }

    /**
     * Adds an action to this list builder.
     * <p>
     * Actions added with this method are grouped together on the list template and represented
     * as tappable icons or images. These actions may appear differently on the slice depending on
     * how the {@link androidx.slice.widget.SliceView}  is configured. Generally these actions will
     * be  displayed in the order they were added, however, if not all actions can be displayed
     * then actions with a higher priority may be shown first.
     * <p>
     * These actions are only displayed when the slice is displayed in
     * {@link androidx.slice.widget.SliceView#MODE_LARGE} or
     * {@link androidx.slice.widget.SliceView#MODE_SMALL}.
     * <p>
     * These actions differ from a slice's primary action. The primary action
     * is the {@link SliceAction} set on the first row of the slice, normally from
     * {@link HeaderBuilder#setPrimaryAction(SliceAction)}.
     *
     * @see SliceAction
     * @see SliceAction#setPriority(int)
     * @see androidx.slice.widget.SliceView#setSliceActions(List)
     */
    @NonNull
    public ListBuilder addAction(@NonNull SliceAction action) {
        mImpl.addAction(action);
        return this;
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
    public ListBuilder setKeywords(Set<String> keywords) {
        mImpl.setKeywords(keywords);
        return this;
    }

    /**
     * Sets the desired layout direction for this slice.
     *
     * @param layoutDirection the layout direction to set.
     */
    @NonNull
    public ListBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
        mImpl.setLayoutDirection(layoutDirection);
        return this;
    }

    /**
     * If all content in a slice cannot be shown, the row added here may be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * This method should only be used if you want to display a customized row to indicate more
     * content, consider using {@link #setSeeMoreAction(PendingIntent)} otherwise. If you do
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
        mImpl.setSeeMoreRow(builder);
        mHasSeeMore = true;
        return this;
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
     * If all content in a slice cannot be shown, a "see more" affordance may be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    @NonNull
    public ListBuilder setSeeMoreAction(@NonNull RemoteCallback callback) {
        if (mHasSeeMore) {
            throw new IllegalArgumentException("Trying to add see more action when one has "
                    + "already been added");
        }
        mImpl.setSeeMoreAction(callback.toPendingIntent());
        mHasSeeMore = true;
        return this;
    }

    /**
     * Sets whether this slice indicates an error, i.e. the normal contents of this slice are
     * unavailable and instead the slice contains a message indicating an error.
     */
    @NonNull
    public ListBuilder setIsError(boolean isError) {
        mImpl.setIsError(isError);
        return this;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Override
    protected TemplateBuilderImpl selectImpl(Uri uri) {
        if (checkCompatible(SliceSpecs.LIST, uri)) {
            return new ListBuilderV1Impl(getBuilder(), SliceSpecs.LIST, getClock());
        } else if (checkCompatible(SliceSpecs.BASIC, uri)) {
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
     * <p>
     * If {@link InputRangeBuilder#setValue(int)} is not between
     * {@link InputRangeBuilder#setMin(int)} and {@link InputRangeBuilder#setMax(int)}, this
     * will throw {@link IllegalArgumentException}.
     */
    @NonNull
    public ListBuilder addInputRange(@NonNull InputRangeBuilder b) {
        mImpl.addInputRange(b);
        return this;
    }

    /**
     * Add a range row to the list builder.
     * <p>
     * If {@link RangeBuilder#setValue(int)} is not between 0 and
     * {@link RangeBuilder#setMax(int)}, this will throw {@link IllegalArgumentException}.
     */
    @NonNull
    public ListBuilder addRange(@NonNull RangeBuilder rangeBuilder) {
        mImpl.addRange(rangeBuilder);
        return this;
    }

    /**
     * Add a selection row to the list builder.
     */
    @NonNull
    public ListBuilder addSelection(@NonNull SelectionBuilder selectionBuilder) {
        mImpl.addSelection(selectionBuilder);
        return this;
    }

    /**
     * Builder to construct a range row which can be added to a {@link ListBuilder}.
     * <p>
     * A range row supports displaying a horizontal progress indicator.
     *
     * @see ListBuilder#addRange(RangeBuilder)
     */
    public static class RangeBuilder {

        private int mValue;
        private int mMax = 100;
        private boolean mValueSet = false;
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private SliceAction mPrimaryAction;
        private CharSequence mContentDescription;
        private int mLayoutDirection = -1;

        /**
         * Builder to construct a range row which can be added to a {@link ListBuilder}.
         * <p>
         * A range row supports displaying a horizontal progress indicator.
         *
         * @see ListBuilder#addRange(RangeBuilder)
         */
        public RangeBuilder() {
        }

        /**
         * Set the upper limit of the range. The default is 100.
         */
        @NonNull
        public RangeBuilder setMax(int max) {
            mMax = max;
            return this;
        }

        /**
         * Set the current value of the range.
         *
         * @param value the value of the range, between 0 and {@link #setMax(int)}.
         */
        @NonNull
        public RangeBuilder setValue(int value) {
            mValueSet = true;
            mValue = value;
            return this;
        }

        /**
         * Set the title.
         */
        @NonNull
        public RangeBuilder setTitle(@NonNull CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Set the subtitle.
         */
        @NonNull
        public RangeBuilder setSubtitle(@NonNull CharSequence title) {
            mSubtitle = title;
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
            mPrimaryAction = action;
            return this;
        }

        /**
         * Sets the content description.
         */
        @NonNull
        public RangeBuilder setContentDescription(@NonNull CharSequence description) {
            mContentDescription = description;
            return this;
        }

        /**
         * Sets the desired layout direction for the content in this row.
         *
         * @param layoutDirection the layout direction to set.
         */
        @NonNull
        public RangeBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getValue() {
            return mValue;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getMax() {
            return mMax;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isValueSet() {
            return mValueSet;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public SliceAction getPrimaryAction() {
            return mPrimaryAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getLayoutDirection() {
            return mLayoutDirection;
        }
    }

    /**
     * Builder to construct a input range row.
     * <p>
     * An input range row supports displaying a horizontal slider allowing slider input.
     *
     * @see ListBuilder#addInputRange(InputRangeBuilder)
     */
    public static class InputRangeBuilder {

        private int mMin = 0;
        private int mMax = 100;
        private int mValue = 0;
        private boolean mValueSet = false;
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private PendingIntent mAction;
        private PendingIntent mInputAction;
        private IconCompat mThumb;
        private SliceAction mPrimaryAction;
        private CharSequence mContentDescription;
        private int mLayoutDirection = -1;

        /**
         * Builder to construct a input range row.
         * <p>
         * An input range row supports displaying a horizontal slider allowing slider input.
         *
         * @see ListBuilder#addInputRange(InputRangeBuilder)
         */
        public InputRangeBuilder() {
        }

        /**
         * Set the lower limit of the range. The default is 0.
         */
        @NonNull
        public InputRangeBuilder setMin(int min) {
            mMin = min;
            return this;
        }

        /**
         * Set the upper limit of the range. The default is 100.
         */
        @NonNull
        public InputRangeBuilder setMax(int max) {
            mMax = max;
            return this;
        }

        /**
         * Set the current value of the range.
         *
         * @param value the value of the range, between {@link #setMin(int)}
         *              and {@link #setMax(int)}.
         */
        @NonNull
        public InputRangeBuilder setValue(int value) {
            mValueSet = true;
            mValue = value;
            return this;
        }

        /**
         * Set the title.
         */
        @NonNull
        public InputRangeBuilder setTitle(@NonNull CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Set the subtitle.
         */
        @NonNull
        public InputRangeBuilder setSubtitle(@NonNull CharSequence title) {
            mSubtitle = title;
            return this;
        }

        /**
         * Set the {@link PendingIntent} to send when the current value is updated.
         */
        @NonNull
        public InputRangeBuilder setInputAction(@NonNull PendingIntent action) {
            mInputAction = action;
            return this;
        }

        /**
         * Set the {@link PendingIntent} to send when the current value is updated.
         */
        @NonNull
        public InputRangeBuilder setInputAction(@NonNull RemoteCallback callback) {
            mInputAction = callback.toPendingIntent();
            return this;
        }

        /**
         * Set the {@link Icon} to be displayed as the thumb on the input range.
         */
        @NonNull
        public InputRangeBuilder setThumb(@NonNull IconCompat thumb) {
            mThumb = thumb;
            return this;
        }

        /**
         * Set the primary action for this row.
         * <p>
         * The action specified here will be sent when the whole row is clicked, whereas
         * the action specified via {@link #setInputAction(PendingIntent)} is used when the
         * slider is interacted with. Additionally, if this is the first row in a
         * {@link ListBuilder} this action will also be used to define the
         * {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation of the slice.
         */
        @NonNull
        public InputRangeBuilder setPrimaryAction(@NonNull SliceAction action) {
            mPrimaryAction = action;
            return this;
        }

        /**
         * Sets the content description.
         */
        @NonNull
        public InputRangeBuilder setContentDescription(@NonNull CharSequence description) {
            mContentDescription = description;
            return this;
        }

        /**
         * Sets the desired layout direction for the content in this row.
         *
         * @param layoutDirection the layout direction to set.
         */
        @NonNull
        public InputRangeBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getMin() {
            return mMin;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getMax() {
            return mMax;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getValue() {
            return mValue;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isValueSet() {
            return mValueSet;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public PendingIntent getAction() {
            return mAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public PendingIntent getInputAction() {
            return mInputAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public IconCompat getThumb() {
            return mThumb;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public SliceAction getPrimaryAction() {
            return mPrimaryAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getLayoutDirection() {
            return mLayoutDirection;
        }
    }

    /**
     * Builder to construct a row. A row can be added as an item to ListBuilder via
     * {@link ListBuilder#addRow(RowBuilder)}.
     * <p>
     * A row supports:
     * <ul>
     *     <li>Title item - The title item can be a timestamp, image, or a {@link SliceAction}.
     *     It appears with at the start of the row. There can only be one title item added to a row.
     *     </li>
     *     <li>Title - Single line of text formatted as a title, see
     *     {@link #setTitle(CharSequence)}.
     *     </li>
     *     <li>Subtitle - Single line of text below the title (if one exists) and is formatted as
     *     normal text, see {@link #setSubtitle(CharSequence)}.
     *     </li>
     *     <li>End item - End items appear at the end of the row. There can be multiple end items
     *     that show depending on available width. End items can be a timestamp, image, or a
     *     tappable icon.
     *     </li>
     *     <li>Primary action - The primary action for the row, this is the action that will be sent
     *     when the row is clicked. This is set via {@link #setPrimaryAction(SliceAction)}. If this
     *     is the only row or first row of the slice, then the action set here will be used to
     *     represent the slice shown in {@link androidx.slice.widget.SliceView#MODE_SMALL}.
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
    public static class RowBuilder {

        private final Uri mUri;
        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private boolean mHasDefaultToggle;
        private boolean mHasTimestamp;
        private long mTimeStamp = -1;
        private boolean mTitleItemLoading;
        private int mTitleImageMode;
        private IconCompat mTitleIcon;
        private SliceAction mTitleAction;
        private SliceAction mPrimaryAction;
        private CharSequence mTitle;
        private boolean mTitleLoading;
        private CharSequence mSubtitle;
        private boolean mSubtitleLoading;
        private CharSequence mContentDescription;
        private int mLayoutDirection = -1;
        private List<Object> mEndItems = new ArrayList<>();
        private List<Integer> mEndTypes = new ArrayList<>();
        private List<Boolean> mEndLoads = new ArrayList<>();
        private boolean mTitleActionLoading;

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int TYPE_TIMESTAMP = 0;
        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int TYPE_ICON = 1;
        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int TYPE_ACTION = 2;

        /**
         * Builder to construct a row.
         */
        public RowBuilder() {
            mUri = null;
        }

        /**
         * Builder to construct a normal row.
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(Uri uri) {
            mUri = uri;
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
            mTimeStamp = timeStamp;
            mHasTimestamp = true;
            return this;
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
            return setTitleItem(icon, imageMode, false /* isLoading */);
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
            mTitleAction = null;
            mTitleIcon = icon;
            mTitleImageMode = imageMode;
            mTitleItemLoading = isLoading;
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
            mTitleAction = action;
            mTitleIcon = null;
            mTitleImageMode = 0;
            mTitleActionLoading = isLoading;
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
            mPrimaryAction = action;
            return this;
        }

        /**
         * Sets the title for the row builder. A title should fit on a single line and is ellipsized
         * if too long.
         */
        @NonNull
        public RowBuilder setTitle(@NonNull CharSequence title) {
            return setTitle(title, false);
        }

        /**
         * Sets the title for the row builder. A title should fit on a single line and is ellipsized
         * if too long.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setTitle(@Nullable CharSequence title, boolean isLoading) {
            mTitle = title;
            mTitleLoading = isLoading;
            return this;
        }

        /**
         * Sets the subtitle for the row builder. A subtitle should fit on a single line and is
         * ellipsized if too long.
         */
        @NonNull
        public RowBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle for the row builder. A subtitle should fit on a single line and is
         * ellipsized if too long.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public RowBuilder setSubtitle(@Nullable CharSequence subtitle, boolean isLoading) {
            mSubtitle = subtitle;
            mSubtitleLoading = isLoading;
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
            mEndItems.add(timeStamp);
            mEndTypes.add(TYPE_TIMESTAMP);
            mEndLoads.add(false);
            mHasTimestamp = true;
            return this;
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
            mEndItems.add(new Pair<>(icon, imageMode));
            mEndTypes.add(TYPE_ICON);
            mEndLoads.add(isLoading);
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
            mEndItems.add(action);
            mEndTypes.add(TYPE_ACTION);
            mEndLoads.add(isLoading);
            mHasDefaultToggle = action.getImpl().isDefaultToggle();
            mHasEndActionOrToggle = true;
            return this;
        }

        /**
         * Sets the content description for the row.
         */
        @NonNull
        public RowBuilder setContentDescription(@NonNull CharSequence description) {
            mContentDescription = description;
            return this;
        }

        /**
         * Sets the desired layout direction for the content in this row.
         *
         * @param layoutDirection the layout direction to set.
         */
        @NonNull
        public RowBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public Uri getUri() {
            return mUri;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean hasEndActionOrToggle() {
            return mHasEndActionOrToggle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean hasEndImage() {
            return mHasEndImage;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean hasDefaultToggle() {
            return mHasDefaultToggle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean hasTimestamp() {
            return mHasTimestamp;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public long getTimeStamp() {
            return mTimeStamp;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isTitleItemLoading() {
            return mTitleItemLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getTitleImageMode() {
            return mTitleImageMode;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public IconCompat getTitleIcon() {
            return mTitleIcon;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public SliceAction getTitleAction() {
            return mTitleAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public SliceAction getPrimaryAction() {
            return mPrimaryAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isTitleLoading() {
            return mTitleLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isSubtitleLoading() {
            return mSubtitleLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getLayoutDirection() {
            return mLayoutDirection;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public List<Object> getEndItems() {
            return mEndItems;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public List<Integer> getEndTypes() {
            return mEndTypes;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public List<Boolean> getEndLoads() {
            return mEndLoads;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isTitleActionLoading() {
            return mTitleActionLoading;
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
    public static class HeaderBuilder {
        private final Uri mUri;
        private CharSequence mTitle;
        private boolean mTitleLoading;
        private CharSequence mSubtitle;
        private boolean mSubtitleLoading;
        private CharSequence mSummary;
        private boolean mSummaryLoading;
        private SliceAction mPrimaryAction;
        private CharSequence mContentDescription;
        private int mLayoutDirection;

        /**
         * Create builder for a header.
         */
        public HeaderBuilder() {
            mUri = null;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public HeaderBuilder(Uri uri) {
            mUri = uri;
        }

        /**
         * Sets the title for the header builder. A title should be representative of the
         * contents of the slice and fit on a single line.
         */
        @NonNull
        public HeaderBuilder setTitle(@NonNull CharSequence title) {
            return setTitle(title, false /* isLoading */);
        }

        /**
         * Sets the title for the header builder. A title should be representative of the
         * contents of the slice and fit on a single line.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public HeaderBuilder setTitle(@NonNull CharSequence title, boolean isLoading) {
            mTitle = title;
            mTitleLoading = isLoading;
            return this;
        }

        /**
         * Sets the subtitle for the header builder. The subtitle should fit on a single line.
         */
        @NonNull
        public HeaderBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle for the header builder. The subtitle should fit on a single line.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         * @param isLoading indicates whether the app is doing work to load the added content in the
         *                  background or not.
         */
        @NonNull
        public HeaderBuilder setSubtitle(@NonNull CharSequence subtitle, boolean isLoading) {
            mSubtitle = subtitle;
            mSubtitleLoading = isLoading;
            return this;
        }

        /**
         * Sets the summary for the header builder. A summary is optional and should fit on
         * a single line and is ellipsized if too long.
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
         * Sets the summary for the header builder. A summary is optional and should fit on
         * a single line and is ellipsized if too long.
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
            mSummary = summary;
            mSummaryLoading = isLoading;
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
            mPrimaryAction = action;
            return this;
        }

        /**
         * Sets the content description for the header.
         */
        @NonNull
        public HeaderBuilder setContentDescription(@NonNull CharSequence description) {
            mContentDescription = description;
            return this;
        }

        /**
         * Sets the desired layout direction for the content in this row.
         *
         * @param layoutDirection the layout direction to set.
         */
        @NonNull
        public HeaderBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public Uri getUri() {
            return mUri;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isTitleLoading() {
            return mTitleLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isSubtitleLoading() {
            return mSubtitleLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getSummary() {
            return mSummary;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isSummaryLoading() {
            return mSummaryLoading;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public SliceAction getPrimaryAction() {
            return mPrimaryAction;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getLayoutDirection() {
            return mLayoutDirection;
        }
    }
}
