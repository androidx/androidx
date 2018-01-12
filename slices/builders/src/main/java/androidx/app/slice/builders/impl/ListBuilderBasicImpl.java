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


import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class ListBuilderBasicImpl extends TemplateBuilderImpl implements ListBuilder {

    /**
     */
    public ListBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void addRow(TemplateBuilderImpl impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void addGrid(TemplateBuilderImpl impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void addSummaryRow(TemplateBuilderImpl builder) {
        RowBuilderImpl row = (RowBuilderImpl) builder;
        if (row.mIcon != null) {
            getBuilder().addIcon(row.mIcon, null);
        }
        if (row.mTitle != null) {
            getBuilder().addText(row.mTitle, null, android.app.slice.Slice.HINT_TITLE);
        }
        if (row.mSubtitle != null) {
            getBuilder().addText(row.mSubtitle, null);
        }
    }

    /**
     */
    @Override
    public void setColor(int color) {

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
        return new GridBuilderBasicImpl(createChildBuilder(), null);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {

    }

    /**
     */
    public static class RowBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.RowBuilder {
        private Icon mIcon;
        private CharSequence mTitle;
        private CharSequence mSubtitle;

        /**
         */
        public RowBuilderImpl(@NonNull ListBuilderBasicImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        public RowBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        @Override
        public void addEndItem(Icon icon) {

        }

        /**
         */
        @Override
        public void addEndItem(Icon icon, PendingIntent action) {

        }

        /**
         */
        @Override
        public void addToggle(PendingIntent action, boolean isChecked, Icon icon) {

        }

        /**
         */
        @Override
        public void setIsHeader(boolean isHeader) {

        }

        /**
         */
        @Override
        public void setTitleItem(long timeStamp) {

        }

        /**
         */
        @Override
        public void setTitleItem(Icon icon) {
            mIcon = icon;
        }

        /**
         */
        @Override
        public void setTitleItem(Icon icon, PendingIntent action) {
            mIcon = icon;
        }

        /**
         */
        @Override
        public void setContentIntent(PendingIntent action) {

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
        public void addEndItem(long timeStamp) {

        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {

        }
    }
}
