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

package androidx.app.slice.builders.impl;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import static androidx.app.slice.core.SliceHints.SUBTYPE_MAX;
import static androidx.app.slice.core.SliceHints.SUBTYPE_RANGE;
import static androidx.app.slice.core.SliceHints.SUBTYPE_VALUE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.SliceSpec;
import androidx.app.slice.builders.SliceAction;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class ListBuilderV1Impl extends TemplateBuilderImpl implements ListBuilder {

    private List<Slice> mSliceActions;
    private Slice mSliceHeader;

    /**
     */
    public ListBuilderV1Impl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
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
    }

    /**
     * Add a row to list builder.
     */
    @NonNull
    @Override
    public void addRow(@NonNull TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @NonNull
    @Override
    public void addGrid(@NonNull TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @Override
    public void setHeader(@NonNull TemplateBuilderImpl builder) {
        mSliceHeader = builder.build();
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
    public void addInputRange(TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build(), SUBTYPE_RANGE);
    }

    @Override
    public void addRange(TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build(), SUBTYPE_RANGE);
    }

    /**
     */
    @Override
    public void addSeeMoreRow(TemplateBuilderImpl builder) {
        builder.getBuilder().addHints(HINT_SEE_MORE);
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @Override
    public void addSeeMoreAction(PendingIntent intent) {
        getBuilder().addSubSlice(
                new Slice.Builder(getBuilder())
                        .addHints(HINT_SEE_MORE)
                        .addAction(intent, new Slice.Builder(getBuilder())
                                .addHints(HINT_SEE_MORE).build(), null)
                        .build());
    }


    /**
     * Builder to construct an input row.
     */
    public static class RangeBuilderImpl extends TemplateBuilderImpl implements RangeBuilder {
        private int mMax = 100;
        private int mValue = 0;
        private CharSequence mTitle;

        public RangeBuilderImpl(Slice.Builder sb) {
            super(sb, null);
        }

        @Override
        public void setMax(int max) {
            mMax = max;
        }

        @Override
        public void setValue(int value) {
            mValue = value;
        }

        @Override
        public void setTitle(@NonNull CharSequence title) {
            mTitle = title;
        }

        @Override
        public void apply(Slice.Builder builder) {
            if (mTitle != null) {
                builder.addText(mTitle, null, HINT_TITLE);
            }
            builder.addHints(HINT_LIST_ITEM)
                    .addInt(mMax, SUBTYPE_MAX)
                    .addInt(mValue, SUBTYPE_VALUE);
        }
    }

    /**
     * Builder to construct an input range row.
     */
    public static class InputRangeBuilderImpl
            extends RangeBuilderImpl implements InputRangeBuilder {
        private PendingIntent mAction;
        private Icon mThumb;

        public InputRangeBuilderImpl(Slice.Builder sb) {
            super(sb);
        }

        @Override
        public void setAction(@NonNull PendingIntent action) {
            mAction = action;
        }

        @Override
        public void setThumb(@NonNull Icon thumb) {
            mThumb = thumb;
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
    @NonNull
    @Override
    public void setColor(@ColorInt int color) {
        getBuilder().addInt(color, SUBTYPE_COLOR);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createRowBuilder() {
        return new RowBuilderImpl(this);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createRowBuilder(Uri uri) {
        return new RowBuilderImpl(uri);
    }


    @Override
    public TemplateBuilderImpl createInputRangeBuilder() {
        return new InputRangeBuilderImpl(createChildBuilder());
    }

    @Override
    public TemplateBuilderImpl createRangeBuilder() {
        return new RangeBuilderImpl(createChildBuilder());
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createGridBuilder() {
        return new GridBuilderListV1Impl(this);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createHeaderBuilder() {
        return new HeaderBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createHeaderBuilder(Uri uri) {
        return new HeaderBuilderImpl(uri);
    }

    /**
     */
    public static class RowBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.RowBuilder {

        private SliceAction mPrimaryAction;
        private SliceItem mTitleItem;
        private SliceItem mSubtitleItem;
        private Slice mStartItem;
        private ArrayList<Slice> mEndItems = new ArrayList<>();

        /**
         */
        public RowBuilderImpl(@NonNull ListBuilderV1Impl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        public RowBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        public RowBuilderImpl(Slice.Builder builder) {
            super(builder, null);
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(long timeStamp) {
            mStartItem = new Slice.Builder(getBuilder())
                    .addTimestamp(timeStamp, null).addHints(HINT_TITLE).build();
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(@NonNull Icon icon) {
            setTitleItem(icon, false /* isLoading */);
        }

        /**
         */
        @Override
        public void setTitleItem(@Nullable Icon icon, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addIcon(icon, null /* subtype */);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = sb.addHints(HINT_TITLE).build();
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(@NonNull SliceAction action) {
            setTitleItem(action, false /* isLoading */);
        }

        /**
         */
        @Override
        public void setTitleItem(SliceAction action, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addHints(HINT_TITLE);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mStartItem = action.buildSlice(sb);
        }

        /**
         */
        @NonNull
        @Override
        public void setPrimaryAction(@NonNull SliceAction action) {
            mPrimaryAction = action;
        }

        /**
         */
        @NonNull
        @Override
        public void setTitle(CharSequence title) {
            setTitle(title, false /* isLoading */);
        }

        /**
         */
        @Override
        public void setTitle(CharSequence title, boolean isLoading) {
            mTitleItem = new SliceItem(title, FORMAT_TEXT, null, new String[] {HINT_TITLE});
            if (isLoading) {
                mTitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        @NonNull
        @Override
        public void setSubtitle(CharSequence subtitle) {
            setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle, boolean isLoading) {
            mSubtitleItem = new SliceItem(subtitle, FORMAT_TEXT, null, new String[0]);
            if (isLoading) {
                mSubtitleItem.addHint(HINT_PARTIAL);
            }
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(long timeStamp) {
            mEndItems.add(new Slice.Builder(getBuilder()).addTimestamp(timeStamp,
                    null, new String[0]).build());
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(@NonNull Icon icon) {
            addEndItem(icon, false /* isLoading */);
        }

        /**
         */
        @Override
        public void addEndItem(Icon icon, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addIcon(icon, null /* subType */,
                    HINT_NO_TINT, HINT_LARGE);
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(sb.build());
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(@NonNull SliceAction action) {
            addEndItem(action, false /* isLoading */);
        }

        /**
         */
        @Override
        public void addEndItem(@NonNull SliceAction action, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            if (isLoading) {
                sb.addHints(HINT_PARTIAL);
            }
            mEndItems.add(action.buildSlice(sb));
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
            if (mPrimaryAction != null) {
                Slice.Builder sb = new Slice.Builder(
                        getBuilder()).addHints(HINT_TITLE, HINT_SHORTCUT);
                b.addSubSlice(mPrimaryAction.buildSlice(sb), null);
            }
            b.addHints(HINT_LIST_ITEM);
        }
    }

    /**
     */
    public static class HeaderBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.HeaderBuilder {

        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private CharSequence mSummarySubtitle;
        private SliceAction mPrimaryAction;

        /**
         */
        public HeaderBuilderImpl(@NonNull ListBuilderV1Impl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        public HeaderBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        @Override
        public void apply(Slice.Builder b) {
            if (mTitle != null) {
                b.addText(mTitle, null /* subtype */, HINT_TITLE);
            }
            if (mSubtitle != null) {
                b.addText(mSubtitle, null /* subtype */);
            }
            if (mSummarySubtitle != null) {
                b.addText(mSummarySubtitle, null /* subtype */, HINT_SUMMARY);
            }
            if (mPrimaryAction != null) {
                Slice.Builder sb = new Slice.Builder(
                        getBuilder()).addHints(HINT_TITLE, HINT_SHORTCUT);
                b.addSubSlice(mPrimaryAction.buildSlice(sb), null /* subtype */);
            }
        }

        /**
         */
        @Override
        public void setTitle(CharSequence title) {
            mTitle = title;
        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle) {
            mSubtitle = subtitle;
        }

        /**
         */
        @Override
        public void setSummarySubtitle(CharSequence summarySubtitle) {
            mSummarySubtitle = summarySubtitle;
        }

        /**
         */
        @Override
        public void setPrimaryAction(SliceAction action) {
            mPrimaryAction = action;
        }
    }
}
