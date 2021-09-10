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

package androidx.slice.builders.impl;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_ERROR;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static android.app.slice.Slice.SUBTYPE_MAX;
import static android.app.slice.Slice.SUBTYPE_RANGE;
import static android.app.slice.Slice.SUBTYPE_VALUE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_BUNDLE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.Slice.SUBTYPE_RANGE_MODE;
import static androidx.slice.builders.ListBuilder.INFINITY;
import static androidx.slice.builders.ListBuilder.RANGE_MODE_DETERMINATE;
import static androidx.slice.builders.ListBuilder.RANGE_MODE_STAR_RATING;
import static androidx.slice.core.SliceHints.HINT_END_OF_SECTION;
import static androidx.slice.core.SliceHints.SUBTYPE_HOST_EXTRAS;
import static androidx.slice.core.SliceHints.SUBTYPE_MILLIS;
import static androidx.slice.core.SliceHints.SUBTYPE_MIN;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import androidx.slice.SystemClock;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RangeBuilder;
import androidx.slice.builders.ListBuilder.RatingBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SelectionBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.core.SliceQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class ListBuilderImpl extends TemplateBuilderImpl implements ListBuilder {
    private List<Slice> mSliceActions;
    private Set<String> mKeywords;
    private Slice mSliceHeader;
    private boolean mIsError;
    private boolean mFirstRowChecked;
    private boolean mIsFirstRowTypeValid;
    private boolean mFirstRowHasText;
    private Bundle mHostExtras;

    public ListBuilderImpl(@Nullable final Slice.Builder b, @Nullable final SliceSpec spec) {
        this(b, spec, new SystemClock());
    }

    /**
     */
    public ListBuilderImpl(@Nullable final Slice.Builder b, @Nullable final SliceSpec spec,
            @NonNull final Clock clock) {
        super(b, spec, clock);
    }

    /**
     */
    @Override
    public void apply(@NonNull final Slice.Builder builder) {
        builder.addLong(getClock().currentTimeMillis(), SUBTYPE_MILLIS, HINT_LAST_UPDATED);
        if (mSliceHeader != null) {
            builder.addSubSlice(mSliceHeader);
        }
        if (mSliceActions != null) {
            Slice.Builder sb = new Slice.Builder(builder);
            for (int i = 0; i < mSliceActions.size(); i++) {
                sb.addSubSlice(mSliceActions.get(i));
            }
            builder.addSubSlice(sb.addHints(HINT_ACTIONS).build());
        }
        if (mIsError) {
            builder.addHints(HINT_ERROR);
        }
        if (mKeywords != null) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            for (String keyword : mKeywords) {
                sb.addText(keyword, null);
            }
            getBuilder().addSubSlice(sb.addHints(HINT_KEYWORDS).build());
        }
        if (mHostExtras != null) {
            builder.addItem(new SliceItem(mHostExtras, FORMAT_BUNDLE, SUBTYPE_HOST_EXTRAS,
                    new String[0]));
        }
    }

    /**
     * Construct the slice.
     */
    @Override
    @NonNull
    public Slice build() {
        Slice slice = super.build();
        boolean isLoading = SliceQuery.find(slice, null, HINT_PARTIAL, null) != null;
        boolean isEmpty = SliceQuery.find(slice, FORMAT_SLICE, HINT_LIST_ITEM, null) == null;
        String[] hints = new String[] {HINT_SHORTCUT, HINT_TITLE};
        SliceItem action = SliceQuery.find(slice, FORMAT_ACTION, hints, null);
        List<SliceItem> possiblePrimaries = SliceQuery.findAll(slice, FORMAT_SLICE, hints, null);
        if (!isLoading && !isEmpty && action == null
                && (possiblePrimaries == null || possiblePrimaries.isEmpty())) {
            throw new IllegalStateException("A slice requires a primary action; ensure one of your "
                    + "builders has called #setPrimaryAction with a valid SliceAction.");
        }
        if (mFirstRowChecked && !mIsFirstRowTypeValid) {
            throw new IllegalStateException("A slice cannot have the first row be"
                    + " constructed from a GridRowBuilder, consider using #setHeader.");
        }
        if (mFirstRowChecked && !mFirstRowHasText) {
            throw new IllegalStateException("A slice requires the first row to have some text.");
        }
        return slice;
    }

    /**
     * Add a row to list builder.
     */
    @Override
    public void addRow(@NonNull RowBuilder builder) {
        RowBuilderImpl impl = new RowBuilderImpl(createChildBuilder());
        impl.fillFrom(builder);
        checkRow(true, impl.hasText());
        addRow(impl);
    }

    /**
     * Add a row to list builder.
     */
    public void addRow(@NonNull RowBuilderImpl builder) {
        checkRow(true, builder.hasText());
        builder.getBuilder().addHints(HINT_LIST_ITEM);
        if (builder.isEndOfSection()) {
            builder.getBuilder().addHints(HINT_END_OF_SECTION);
        }
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @Override
    public void addGridRow(@NonNull GridRowBuilder builder) {
        checkRow(false, false);
        GridRowBuilderListV1Impl impl = new GridRowBuilderListV1Impl(this, builder);
        impl.getBuilder().addHints(HINT_LIST_ITEM);
        getBuilder().addSubSlice(impl.build());
    }

    /**
     */
    @Override
    public void setHeader(@NonNull HeaderBuilder builder) {
        mIsFirstRowTypeValid = true;
        mFirstRowHasText = true;
        mFirstRowChecked = true;
        HeaderBuilderImpl impl = new HeaderBuilderImpl(this);
        impl.fillFrom(builder);
        mSliceHeader = impl.build();
    }

    /**
     */
    @Override
    public void addAction(@NonNull SliceAction action) {
        if (mSliceActions == null) {
            mSliceActions = new ArrayList<>();
        }
        Slice.Builder b = new Slice.Builder(getBuilder()).addHints(HINT_ACTIONS);
        mSliceActions.add(action.buildSlice(b));
    }

    @Override
    public void addRating(@NonNull final RatingBuilder builder) {
        RatingBuilderImpl impl = new RatingBuilderImpl(createChildBuilder(), builder);
        checkRow(true, impl.hasText());
        getBuilder().addSubSlice(impl.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addInputRange(@NonNull final InputRangeBuilder builder) {
        InputRangeBuilderImpl impl = new InputRangeBuilderImpl(createChildBuilder(), builder);
        checkRow(true, impl.hasText());
        getBuilder().addSubSlice(impl.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addRange(@NonNull final RangeBuilder builder) {
        RangeBuilderImpl impl = new RangeBuilderImpl(createChildBuilder(), builder);
        checkRow(true, impl.hasText());
        getBuilder().addSubSlice(impl.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addSelection(@NonNull final SelectionBuilder builder) {
        if (getSpec().canRender(SliceSpecs.LIST_V2)) {
            getBuilder().addSubSlice(
                    new SelectionBuilderListV2Impl(createChildBuilder(), builder).build(),
                    SUBTYPE_SELECTION);
        } else {
            getBuilder().addSubSlice(
                    new SelectionBuilderBasicImpl(createChildBuilder(), builder).build());
        }
    }

    /**
     */
    @Override
    public void setSeeMoreRow(@NonNull final RowBuilder builder) {
        RowBuilderImpl impl = new RowBuilderImpl(createChildBuilder());
        impl.fillFrom(builder);
        impl.getBuilder().addHints(HINT_SEE_MORE);
        getBuilder().addSubSlice(impl.build());
    }

    /**
     */
    @Override
    public void setSeeMoreAction(@NonNull final PendingIntent intent) {
        getBuilder().addSubSlice(
                new Slice.Builder(getBuilder())
                        .addHints(HINT_SEE_MORE)
                        .addAction(intent, new Slice.Builder(getBuilder())
                                .addHints(HINT_SEE_MORE).build(), null)
                        .build());
    }

    /**
     */
    @Override
    public void setColor(@ColorInt int color) {
        getBuilder().addInt(color, SUBTYPE_COLOR);
    }

    /**
     */
    @Override
    public void setKeywords(@NonNull Set<String> keywords) {
        mKeywords = keywords;
    }

    /**
     */
    @Override
    public void setTtl(long ttl) {
        long expiry = ttl == INFINITY ? INFINITY : getClock().currentTimeMillis() + ttl;
        getBuilder().addTimestamp(expiry, SUBTYPE_MILLIS, HINT_TTL);
    }

    @Override
    @RequiresApi(26)
    public void setTtl(@Nullable Duration ttl) {
        setTtl(ttl == null ? INFINITY : ttl.toMillis());
    }

    @Override
    public void setIsError(boolean isError) {
        mIsError = isError;
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getBuilder().addInt(layoutDirection, SUBTYPE_LAYOUT_DIRECTION);
    }

    @Override
    @RequiresApi(21)
    public void setHostExtras(@NonNull PersistableBundle extras) {
        mHostExtras = ConvertPersistableBundleApi21Impl.toBundle(extras);
    }

    @RequiresApi(21)
    private static class ConvertPersistableBundleApi21Impl {
        private ConvertPersistableBundleApi21Impl() {}
        static Bundle toBundle(@NonNull PersistableBundle extras) {
            return new Bundle(extras);
        }
    }


    /**
     * There are some requirements that first row of a list is not a grid row and has some text.
     * This method helps check whether first row fulfils these requirements.
     */
    private void checkRow(boolean isTypeValid, boolean hasText) {
        if (!mFirstRowChecked) {
            mFirstRowChecked = true;
            mIsFirstRowTypeValid = isTypeValid;
            mFirstRowHasText = hasText;
        }
    }

    /**
     * Builder to construct an input row.
     */
    public static class RangeBuilderImpl extends TemplateBuilderImpl {
        protected int mMin = 0;
        protected int mMax = 100;
        protected int mValue = 0;
        protected boolean mValueSet = false;
        @Nullable
        protected CharSequence mTitle;
        @Nullable
        protected CharSequence mSubtitle;
        @Nullable
        protected CharSequence mContentDescr;
        @Nullable
        protected SliceAction mPrimaryAction;
        protected int mLayoutDir = -1;
        private int mMode = RANGE_MODE_DETERMINATE;
        private Slice mStartItem;

        RangeBuilderImpl(Slice.Builder sb, RangeBuilder builder) {
            super(sb, null);
            if (builder != null) {
                mValueSet = builder.isValueSet();
                mMax = builder.getMax();
                mValue = builder.getValue();
                mTitle = builder.getTitle();
                mSubtitle = builder.getSubtitle();
                mContentDescr = builder.getContentDescription();
                mPrimaryAction = builder.getPrimaryAction();
                mLayoutDir = builder.getLayoutDirection();
                mMode = builder.getMode();
                if (builder.getTitleIcon() != null) {
                    setTitleItem(builder.getTitleIcon(), builder.getTitleImageMode(),
                            builder.isTitleItemLoading());
                }
            }
        }

        void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null, parseImageMode(imageMode, isLoading));
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        @Override
        public void apply(@NonNull Slice.Builder builder) {
            if (!mValueSet) {
                // Unset, make it whatever min is
                mValue = mMin;
            }
            if (!(mMin <= mValue && mValue <= mMax && mMin < mMax))  {
                throw new IllegalArgumentException(
                        "Invalid range values, min=" + mMin + ", value=" + mValue + ", max=" + mMax
                + " ensure value falls within (min, max) and min < max.");
            }

            if (mStartItem != null) {
                builder.addSubSlice(mStartItem);
            }
            if (mTitle != null) {
                builder.addText(mTitle, null, HINT_TITLE);
            }
            if (mSubtitle != null) {
                builder.addText(mSubtitle, null);
            }
            if (mContentDescr != null) {
                builder.addText(mContentDescr, SUBTYPE_CONTENT_DESCRIPTION);
            }
            if (mPrimaryAction != null) {
                mPrimaryAction.setPrimaryAction(builder);
            }
            if (mLayoutDir != -1) {
                builder.addInt(mLayoutDir, SUBTYPE_LAYOUT_DIRECTION);
            }
            builder.addHints(HINT_LIST_ITEM)
                    .addInt(mMin, SUBTYPE_MIN)
                    .addInt(mMax, SUBTYPE_MAX)
                    .addInt(mValue, SUBTYPE_VALUE)
                    .addInt(mMode, SUBTYPE_RANGE_MODE);
        }
        boolean hasText() {
            return mTitle != null || mSubtitle != null;
        }
    }

    /**
     * Builder to construct an input range row.
     */
    public static class InputRangeBuilderImpl extends RangeBuilderImpl {
        private final PendingIntent mAction;
        private final IconCompat mThumb;
        private Slice mStartItem;
        private final ArrayList<Slice> mEndItems = new ArrayList<>();

        InputRangeBuilderImpl(Slice.Builder sb, InputRangeBuilder builder) {
            super(sb, null);
            mValueSet = builder.isValueSet();
            mMin = builder.getMin();
            mMax = builder.getMax();
            mValue = builder.getValue();
            mTitle = builder.getTitle();
            mSubtitle = builder.getSubtitle();
            mContentDescr = builder.getContentDescription();
            mPrimaryAction = builder.getPrimaryAction();
            mLayoutDir = builder.getLayoutDirection();
            mAction = builder.getInputAction();
            mThumb = builder.getThumb();
            if (builder.getTitleIcon() != null) {
                setTitleItem(builder.getTitleIcon(), builder.getTitleImageMode(),
                        builder.isTitleItemLoading());
            }
            List<Object> endItems = builder.getEndItems();
            List<Integer> endTypes = builder.getEndTypes();
            List<Boolean> endLoads = builder.getEndLoads();
            for (int i = 0; i < endItems.size(); i++) {
                if (endTypes.get(i) == InputRangeBuilder.TYPE_ACTION) {
                    addEndItem((SliceAction) endItems.get(i), endLoads.get(i));
                }
            }
        }

        @Override
        void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null, parseImageMode(imageMode, isLoading));
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        private void addEndItem(@NonNull SliceAction action, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(action.buildSlice(sb));
        }

        @Override
        public void apply(@NonNull Slice.Builder builder) {
            if (mAction == null) {
                throw new IllegalStateException("Input ranges must have an associated action.");
            }
            Slice.Builder sb = new Slice.Builder(builder);
            super.apply(sb);
            if (mThumb != null) {
                sb.addIcon(mThumb, null);
            }
            builder.addAction(mAction, sb.build(), SUBTYPE_RANGE).addHints(HINT_LIST_ITEM);
            if (mStartItem != null) {
                builder.addSubSlice(mStartItem);
            }
            for (int i = 0; i < mEndItems.size(); i++) {
                builder.addSubSlice(mEndItems.get(i));
            }
        }
    }

    /**
     * Builder to construct an input range row.
     */
    public static class RatingBuilderImpl extends TemplateBuilderImpl {
        private final PendingIntent mAction;
        protected int mMin = 0;
        protected int mMax = 100;
        protected int mValue = 0;
        @Nullable
        protected CharSequence mTitle;
        @Nullable
        protected CharSequence mSubtitle;
        @Nullable
        protected CharSequence mContentDescr;
        @Nullable
        protected SliceAction mPrimaryAction;
        protected boolean mValueSet = false;
        private Slice mStartItem;

        RatingBuilderImpl(Slice.Builder sb, RatingBuilder builder) {
            super(sb, null);
            mValueSet = builder.isValueSet();
            mMin = builder.getMin();
            mMax = builder.getMax();
            mValue = (int) builder.getValue();
            mTitle = builder.getTitle();
            mSubtitle = builder.getSubtitle();
            mContentDescr = builder.getContentDescription();
            mPrimaryAction = builder.getPrimaryAction();
            mAction = builder.getInputAction();
            if (builder.getTitleIcon() != null) {
                setTitleItem(builder.getTitleIcon(), builder.getTitleImageMode(),
                        builder.isTitleItemLoading());
            }
        }

        void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null, parseImageMode(imageMode, isLoading));
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        @Override
        public void apply(@NonNull Slice.Builder builder) {
            if (mAction == null) {
                throw new IllegalStateException("Star rating must have an associated action.");
            }

            if (!mValueSet) {
                // Unset, make it outside whatever min is, to represent an unset value
                mValue = mMin - 1;
            }
            if (mTitle != null) {
                builder.addText(mTitle, null, HINT_TITLE);
            }
            if (mSubtitle != null) {
                builder.addText(mSubtitle, null);
            }
            if (mContentDescr != null) {
                builder.addText(mContentDescr, SUBTYPE_CONTENT_DESCRIPTION);
            }
            if (mPrimaryAction != null) {
                mPrimaryAction.setPrimaryAction(builder);
            }
            if (mStartItem != null) {
                builder.addSubSlice(mStartItem);
            }
            Slice.Builder sb = new Slice.Builder(builder);

            sb.addHints(HINT_LIST_ITEM)
                    .addInt(mMin, SUBTYPE_MIN)
                    .addInt(mMax, SUBTYPE_MAX)
                    .addInt(mValue, SUBTYPE_VALUE)
                    .addInt(RANGE_MODE_STAR_RATING, SUBTYPE_RANGE_MODE);
            builder.addAction(mAction, sb.build(), SUBTYPE_RANGE).addHints(HINT_LIST_ITEM);
        }

        boolean hasText() {
            return mTitle != null || mSubtitle != null;
        }
    }

    /**
     *
     */
    public static class RowBuilderImpl extends TemplateBuilderImpl {

        private boolean mIsEndOfSection;
        private SliceAction mPrimaryAction;
        private SliceItem mTitleItem;
        private SliceItem mSubtitleItem;
        private Slice mStartItem;
        private final ArrayList<Slice> mEndItems = new ArrayList<>();
        private CharSequence mContentDescr;

        /**
         */
        private RowBuilderImpl(@NonNull ListBuilderImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        private RowBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        RowBuilderImpl(Slice.Builder builder) {
            super(builder, null);
        }

        @SuppressWarnings("unchecked")
        void fillFrom(RowBuilder builder) {
            if (builder.getUri() != null) {
                setBuilder(new Slice.Builder(builder.getUri()));
            }
            setPrimaryAction(builder.getPrimaryAction());
            mIsEndOfSection = builder.isEndOfSection();
            if (builder.getLayoutDirection() != -1) {
                setLayoutDirection(builder.getLayoutDirection());
            }
            if (builder.getTitleAction() != null || builder.isTitleActionLoading()) {
                setTitleItem(builder.getTitleAction(), builder.isTitleActionLoading());
            } else if (builder.getTitleIcon() != null || builder.isTitleItemLoading()) {
                setTitleItem(builder.getTitleIcon(), builder.getTitleImageMode(),
                        builder.isTitleItemLoading());
            } else if (builder.getTimeStamp() != -1L) {
                setTitleItem(builder.getTimeStamp());
            }
            if (builder.getTitle() != null || builder.isTitleLoading()) {
                setTitle(builder.getTitle(), builder.isTitleLoading());
            }
            if (builder.getSubtitle() != null || builder.isSubtitleLoading()) {
                setSubtitle(builder.getSubtitle(), builder.isSubtitleLoading());
            }
            if (builder.getContentDescription() != null) {
                setContentDescription(builder.getContentDescription());
            }
            List<Object> endItems = builder.getEndItems();
            List<Integer> endTypes = builder.getEndTypes();
            List<Boolean> endLoads = builder.getEndLoads();
            for (int i = 0; i < endItems.size(); i++) {
                switch (endTypes.get(i)) {
                    case RowBuilder.TYPE_TIMESTAMP:
                        addEndItem((Long) endItems.get(i));
                        break;
                    case RowBuilder.TYPE_ACTION:
                        addEndItem((SliceAction) endItems.get(i), endLoads.get(i));
                        break;
                    case RowBuilder.TYPE_ICON:
                        Pair<IconCompat, Integer> pair =
                                (Pair<IconCompat, Integer>) endItems.get(i);
                        addEndItem(pair.first, pair.second, endLoads.get(i));
                        break;
                }
            }
        }

        /**
         */
        private void setTitleItem(long timeStamp) {
            mStartItem = new Slice.Builder(getBuilder())
                    .addTimestamp(timeStamp, null).addHints(HINT_TITLE).build();
        }

        /**
         */
        protected void setTitleItem(@NonNull final IconCompat icon, final int imageMode) {
            setTitleItem(icon, imageMode, false /* isLoading */);
        }

        /**
         */
        private void setTitleItem(@NonNull final IconCompat icon, final int imageMode,
                final boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null, parseImageMode(imageMode, isLoading));
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        /**
         */
        private void setTitleItem(@NonNull final SliceAction action, final boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addHints(HINT_TITLE);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = action.buildSlice(sb);
        }

        /**
         */
        private void setPrimaryAction(@NonNull SliceAction action) {
            mPrimaryAction = action;
        }

        /**
         */
        private void setTitle(@NonNull final CharSequence title, final boolean isLoading) {
            mTitleItem = new SliceItem(title, FORMAT_TEXT, null, new String[] {HINT_TITLE});
            if (isLoading) {
                mTitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        protected void setSubtitle(@NonNull final CharSequence subtitle) {
            setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         */
        private void setSubtitle(@NonNull final CharSequence subtitle, final boolean isLoading) {
            mSubtitleItem = new SliceItem(subtitle, FORMAT_TEXT, null, new String[0]);
            if (isLoading) {
                mSubtitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        protected void addEndItem(long timeStamp) {
            mEndItems.add(new Slice.Builder(getBuilder()).addTimestamp(timeStamp,
                    null).build());
        }

        /**
         */
        private void addEndItem(@NonNull final IconCompat icon, final int imageMode,
                final boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null, parseImageMode(imageMode, isLoading));
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(sb.build());
        }

        /**
         */
        private void addEndItem(@NonNull final SliceAction action, final boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(action.buildSlice(sb));
        }

        private void setContentDescription(CharSequence description) {
            mContentDescr = description;
        }

        private void setLayoutDirection(int layoutDirection) {
            getBuilder().addInt(layoutDirection, SUBTYPE_LAYOUT_DIRECTION);
        }

        /**
         */
        public boolean isEndOfSection() {
            return mIsEndOfSection;
        }

        boolean hasText() {
            return mTitleItem != null || mSubtitleItem != null;
        }

        /**
         */
        @Override
        public void apply(@NonNull Slice.Builder b) {
            if (mStartItem != null) {
                b.addSubSlice(mStartItem);
            }
            if (mTitleItem != null) {
                b.addItem(mTitleItem);
            }
            if (mSubtitleItem != null) {
                b.addItem(mSubtitleItem);
            }
            for (int i = 0; i < mEndItems.size(); i++) {
                Slice item = mEndItems.get(i);
                b.addSubSlice(item);
            }
            if (mContentDescr != null) {
                b.addText(mContentDescr, SUBTYPE_CONTENT_DESCRIPTION);
            }
            if (mPrimaryAction != null) {
                mPrimaryAction.setPrimaryAction(b);
            }
        }
    }

    /**
     */
    public static class HeaderBuilderImpl extends TemplateBuilderImpl {

        private SliceItem mTitleItem;
        private SliceItem mSubtitleItem;
        private SliceItem mSummaryItem;
        private SliceAction mPrimaryAction;
        private CharSequence mContentDescr;

        /**
         */
        HeaderBuilderImpl(@NonNull ListBuilderImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        private HeaderBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        void fillFrom(HeaderBuilder builder) {
            if (builder.getUri() != null) {
                setBuilder(new Slice.Builder(builder.getUri()));
            }
            setPrimaryAction(builder.getPrimaryAction());
            if (builder.getLayoutDirection() != -1) {
                setLayoutDirection(builder.getLayoutDirection());
            }
            if (builder.getTitle() != null || builder.isTitleLoading()) {
                setTitle(builder.getTitle(), builder.isTitleLoading());
            }
            if (builder.getSubtitle() != null || builder.isSubtitleLoading()) {
                setSubtitle(builder.getSubtitle(), builder.isSubtitleLoading());
            }
            if (builder.getSummary() != null || builder.isSummaryLoading()) {
                setSummary(builder.getSummary(), builder.isSummaryLoading());
            }
            if (builder.getContentDescription() != null) {
                setContentDescription(builder.getContentDescription());
            }
        }

        /**
         */
        @Override
        public void apply(@NonNull Slice.Builder b) {
            if (mTitleItem != null) {
                b.addItem(mTitleItem);
            }
            if (mSubtitleItem != null) {
                b.addItem(mSubtitleItem);
            }
            if (mSummaryItem != null) {
                b.addItem(mSummaryItem);
            }
            if (mContentDescr != null) {
                b.addText(mContentDescr, SUBTYPE_CONTENT_DESCRIPTION);
            }
            if (mPrimaryAction != null) {
                mPrimaryAction.setPrimaryAction(b);
            }
            if (mSubtitleItem == null && mTitleItem == null) {
                throw new IllegalStateException("Header requires a title or subtitle to be set.");
            }
        }

        /**
         */
        private void setTitle(CharSequence title, boolean isLoading) {
            mTitleItem = new SliceItem(title, FORMAT_TEXT, null, new String[] {HINT_TITLE});
            if (isLoading) {
                mTitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        private void setSubtitle(CharSequence subtitle, boolean isLoading) {
            mSubtitleItem = new SliceItem(subtitle, FORMAT_TEXT, null, new String[0]);
            if (isLoading) {
                mSubtitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        private void setSummary(CharSequence summarySubtitle, boolean isLoading) {
            mSummaryItem = new SliceItem(summarySubtitle, FORMAT_TEXT, null,
                    new String[] {HINT_SUMMARY});
            if (isLoading) {
                mSummaryItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        private void setPrimaryAction(SliceAction action) {
            mPrimaryAction = action;
        }

        /**
         */
        private void setContentDescription(CharSequence description) {
            mContentDescr = description;
        }

        private void setLayoutDirection(int layoutDirection) {
            getBuilder().addInt(layoutDirection, SUBTYPE_LAYOUT_DIRECTION);
        }
    }
}
