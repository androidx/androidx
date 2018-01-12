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

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import static androidx.app.slice.core.SliceHints.HINT_SUMMARY;
import static androidx.app.slice.core.SliceHints.SUBTYPE_TOGGLE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class ListBuilderV1Impl extends TemplateBuilderImpl implements ListBuilder {

    /**
     */
    public ListBuilderV1Impl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {

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
    public void addSummaryRow(TemplateBuilderImpl builder) {
        builder.getBuilder().addHints(HINT_SUMMARY);
        getBuilder().addSubSlice(builder.build(), null);
    }

    /**
     */
    @NonNull
    @Override
    public void setColor(int color) {
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

    /**
     */
    @Override
    public TemplateBuilderImpl createGridBuilder() {
        return new GridBuilderListV1Impl(createChildBuilder(), null);
    }

    /**
     */
    public static class RowBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.RowBuilder {

        private boolean mIsHeader;
        private PendingIntent mContentIntent;
        private SliceItem mTitleItem;
        private SliceItem mSubtitleItem;
        private SliceItem mStartItem;
        private ArrayList<SliceItem> mEndItems = new ArrayList<>();

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
        public void setIsHeader(boolean isHeader) {
            mIsHeader = isHeader;
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(long timeStamp) {
            mStartItem = new SliceItem(timeStamp, FORMAT_TIMESTAMP, null, new String[0]);
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(@NonNull Icon icon) {
            mStartItem = new SliceItem(icon, FORMAT_IMAGE, null, new String[0]);
        }

        /**
         */
        @NonNull
        @Override
        public void setTitleItem(@NonNull Icon icon, @NonNull PendingIntent action) {
            Slice actionSlice = new Slice.Builder(getBuilder()).addIcon(icon, null).build();
            mStartItem = new SliceItem(action, actionSlice, FORMAT_ACTION, null, new String[0]);
        }

        /**
         */
        @NonNull
        @Override
        public void setContentIntent(@NonNull PendingIntent action) {
            mContentIntent = action;
        }

        /**
         */
        @NonNull
        @Override
        public void setTitle(CharSequence title) {
            mTitleItem = new SliceItem(title, FORMAT_TEXT, null, new String[]{HINT_TITLE});
        }

        /**
         */
        @NonNull
        @Override
        public void setSubtitle(CharSequence subtitle) {
            mSubtitleItem = new SliceItem(subtitle, FORMAT_TEXT, null, new String[0]);
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(long timeStamp) {
            mEndItems.add(new SliceItem(timeStamp, FORMAT_TIMESTAMP, null, new String[0]));
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(@NonNull Icon icon) {
            mEndItems.add(new SliceItem(icon, FORMAT_IMAGE, null,
                    new String[]{HINT_NO_TINT, HINT_LARGE}));
        }

        /**
         */
        @NonNull
        @Override
        public void addEndItem(@NonNull Icon icon, @NonNull PendingIntent action) {
            Slice actionSlice = new Slice.Builder(getBuilder()).addIcon(icon, null).build();
            mEndItems.add(new SliceItem(action, actionSlice, FORMAT_ACTION, null, new String[0]));
        }

        /**
         */
        @NonNull
        @Override
        public void addToggle(@NonNull PendingIntent action, boolean isChecked,
                @NonNull Icon icon) {
            @Slice.SliceHint String[] hints = isChecked
                    ? new String[] {SUBTYPE_TOGGLE, HINT_SELECTED}
                    : new String[] {SUBTYPE_TOGGLE};
            Slice.Builder actionSliceBuilder = new Slice.Builder(getBuilder()).addHints(hints);
            if (icon != null) {
                actionSliceBuilder.addIcon(icon, null);
            }
            Slice actionSlice = actionSliceBuilder.build();
            mEndItems.add(new SliceItem(action, actionSlice, FORMAT_ACTION, null, hints));
        }

        /**
         */
        @Override
        public void apply(Slice.Builder b) {
            Slice.Builder wrapped = b;
            if (mContentIntent != null) {
                b = new Slice.Builder(wrapped);
            }
            if (mStartItem != null) {
                b.addItem(mStartItem);
            }
            if (mTitleItem != null) {
                b.addItem(mTitleItem);
            }
            if (mSubtitleItem != null) {
                b.addItem(mSubtitleItem);
            }
            for (int i = 0; i < mEndItems.size(); i++) {
                SliceItem item = mEndItems.get(i);
                b.addItem(item);
            }
            if (mContentIntent != null) {
                wrapped.addAction(mContentIntent, b.build(), null);
            }
            wrapped.addHints(mIsHeader ? null : HINT_LIST_ITEM);
        }
    }
}
