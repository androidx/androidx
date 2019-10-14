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
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
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
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.builders.ListBuilder.INFINITY;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.SUBTYPE_MILLIS;
import static androidx.slice.core.SliceHints.SUBTYPE_MIN;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION;

import android.app.PendingIntent;
import android.net.Uri;

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

    public ListBuilderImpl(Slice.Builder b, SliceSpec spec) {
        this(b, spec, new SystemClock());
    }

    /**
     */
    public ListBuilderImpl(Slice.Builder b, SliceSpec spec, Clock clock) {
        super(b, spec, clock);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
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
    }

    /**
     * Construct the slice.
     */
    @Override
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
    @NonNull
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
    @NonNull
    public void addRow(@NonNull RowBuilderImpl builder) {
        checkRow(true, builder.hasText());
        builder.getBuilder().addHints(HINT_LIST_ITEM);
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @NonNull
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
    public void addInputRange(InputRangeBuilder builder) {
        InputRangeBuilderImpl impl = new InputRangeBuilderImpl(createChildBuilder(), builder);
        checkRow(true, impl.hasText());
        getBuilder().addSubSlice(impl.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addRange(RangeBuilder builder) {
        RangeBuilderImpl impl = new RangeBuilderImpl(createChildBuilder(), builder);
        checkRow(true, impl.hasText());
        getBuilder().addSubSlice(impl.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addSelection(SelectionBuilder builder) {
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
    public void setSeeMoreRow(RowBuilder builder) {
        RowBuilderImpl impl = new RowBuilderImpl(createChildBuilder());
        impl.fillFrom(builder);
        impl.getBuilder().addHints(HINT_SEE_MORE);
        getBuilder().addSubSlice(impl.build());
    }

    /**
     */
    @Override
    public void setSeeMoreAction(PendingIntent intent) {
        getBuilder().addSubSlice(
                new Slice.Builder(getBuilder())
                        .addHints(HINT_SEE_MORE)
                        .addAction(intent, new Slice.Builder(getBuilder())
                                .addHints(HINT_SEE_MORE).build(), null)
                        .build());
    }

    /**
     */
    @NonNull
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
        protected CharSequence mTitle;
        protected CharSequence mSubtitle;
        protected CharSequence mContentDescr;
        protected SliceAction mPrimaryAction;
        protected int mLayoutDir = -1;

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
            }
        }

        @Override
        public void apply(Slice.Builder builder) {
            if (!mValueSet) {
                // Unset, make it whatever min is
                mValue = mMin;
            }
            if (!(mMin <= mValue && mValue <= mMax && mMin < mMax))  {
                throw new IllegalArgumentException(
                        "Invalid range values, min=" + mMin + ", value=" + mValue + ", max=" + mMax
                + " ensure value falls within (min, max) and min < max.");
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
                    .addInt(mValue, SUBTYPE_VALUE);
        }
        boolean hasText() {
            return mTitle != null || mSubtitle != null;
        }
    }

    /**
     * Builder to construct an input range row.
     */
    public static class InputRangeBuilderImpl extends RangeBuilderImpl {
        private PendingIntent mAction;
        private IconCompat mThumb;

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
        }

        @Override
        public void apply(Slice.Builder builder) {
            if (mAction == null) {
                throw new IllegalStateException("Input ranges must have an associated action.");
            }
            Slice.Builder sb = new Slice.Builder(builder);
            super.apply(sb);
            if (mThumb != null) {
                sb.addIcon(mThumb, null);
            }
            builder.addAction(mAction, sb.build(), SUBTYPE_RANGE).addHints(HINT_LIST_ITEM);
        }
    }

    /**
     */
    public static class RowBuilderImpl extends TemplateBuilderImpl {

        private SliceAction mPrimaryAction;
        private SliceItem mTitleItem;
        private SliceItem mSubtitleItem;
        private Slice mStartItem;
        private ArrayList<Slice> mEndItems = new ArrayList<>();
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
        @NonNull
        private void setTitleItem(long timeStamp) {
            mStartItem = new Slice.Builder(getBuilder())
                    .addTimestamp(timeStamp, null).addHints(HINT_TITLE).build();
        }

        /**
         */
        @NonNull
        protected void setTitleItem(IconCompat icon, int imageMode) {
            setTitleItem(icon, imageMode, false /* isLoading */);
        }

        /**
         */
        @NonNull
        private void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
            ArrayList<String> hints = new ArrayList<>();
            if (imageMode != ICON_IMAGE) {
                hints.add(HINT_NO_TINT);
            }
            if (imageMode == LARGE_IMAGE) {
                hints.add(HINT_LARGE);
            }
            if (isLoading) {
                hints.add(HINT_PARTIAL);
            }
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null /* subType */, hints);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        /**
         */
        @NonNull
        private void setTitleItem(@NonNull SliceAction action) {
            setTitleItem(action, false /* isLoading */);
        }

        /**
         */
        private void setTitleItem(SliceAction action, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addHints(HINT_TITLE);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = action.buildSlice(sb);
        }

        /**
         */
        @NonNull
        private void setPrimaryAction(@NonNull SliceAction action) {
            mPrimaryAction = action;
        }

        /**
         */
        @NonNull
        private void setTitle(CharSequence title) {
            setTitle(title, false /* isLoading */);
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
        @NonNull
        protected void setSubtitle(CharSequence subtitle) {
            setSubtitle(subtitle, false /* isLoading */);
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
        @NonNull
        protected void addEndItem(long timeStamp) {
            mEndItems.add(new Slice.Builder(getBuilder()).addTimestamp(timeStamp,
                    null, new String[0]).build());
        }

        /**
         */
        @NonNull
        private void addEndItem(IconCompat icon, int imageMode) {
            addEndItem(icon, imageMode, false /* isLoading */);
        }

        /**
         */
        @NonNull
        private void addEndItem(IconCompat icon, int imageMode, boolean isLoading) {
            ArrayList<String> hints = new ArrayList<>();
            if (imageMode != ICON_IMAGE) {
                hints.add(HINT_NO_TINT);
            }
            if (imageMode == LARGE_IMAGE) {
                hints.add(HINT_LARGE);
            }
            if (isLoading) {
                hints.add(HINT_PARTIAL);
            }
            Slice.Builder sb = new Slice.Builder(getBuilder())
                    .addIcon(icon, null /* subType */, hints);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(sb.build());
        }

        /**
         */
        @NonNull
        private void addEndItem(@NonNull SliceAction action) {
            addEndItem(action, false /* isLoading */);
        }

        /**
         */
        private void addEndItem(@NonNull SliceAction action, boolean isLoading) {
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

        boolean hasText() {
            return mTitleItem != null || mSubtitleItem != null;
        }

        /**
         */
        @Override
        public void apply(Slice.Builder b) {
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
        public void apply(Slice.Builder b) {
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
