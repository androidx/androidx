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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.core.SliceHints.HINT_KEYWORDS;

import android.app.PendingIntent;
import android.net.Uri;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.builders.SliceAction;

import java.util.List;

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
    public void addGridRow(TemplateBuilderImpl impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void addAction(SliceAction impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setHeader(TemplateBuilderImpl impl) {
        // Do nothing.
    }

    @Override
    public void addInputRange(TemplateBuilderImpl builder) {
        // Do nothing.
    }

    @Override
    public void addRange(TemplateBuilderImpl builder) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setSeeMoreRow(TemplateBuilderImpl builder) {
    }

    /**
     */
    @Override
    public void setSeeMoreAction(PendingIntent intent) {
    }

    /**
     */
    @Override
    public void setColor(@ColorInt int color) {
    }

    /**
     */
    @Override
    public void setKeywords(List<String> keywords) {
        Slice.Builder sb = new Slice.Builder(getBuilder());
        for (int i = 0; i < keywords.size(); i++) {
            sb.addText(keywords.get(i), null);
        }
        getBuilder().addSubSlice(sb.addHints(HINT_KEYWORDS).build());
    }

    /**
     */
    @Override
    public void setTtl(long ttl) {
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
        return new GridRowBuilderBasicImpl(this);
    }

    @Override
    public TemplateBuilderImpl createHeaderBuilder() {
        return new HeaderBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createHeaderBuilder(Uri uri) {
        return new HeaderBuilderImpl(uri);
    }

    @Override
    public TemplateBuilderImpl createInputRangeBuilder() {
        return new ListBuilderV1Impl.InputRangeBuilderImpl(getBuilder());
    }

    @Override
    public TemplateBuilderImpl createRangeBuilder() {
        return new ListBuilderV1Impl.RangeBuilderImpl(getBuilder());
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
        public void addEndItem(SliceAction action) {

        }

        /**
         */
        @Override
        public void addEndItem(SliceAction action, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setContentDescription(CharSequence description) {

        }

        /**
         */
        @Override
        public void setTitleItem(long timeStamp) {

        }

        /**
         */
        @Override
        public void setTitleItem(IconCompat icon, int imageMode) {

        }

        /**
         */
        @Override
        public void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setTitleItem(SliceAction action) {
        }

        /**
         */
        @Override
        public void setTitleItem(SliceAction action, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setPrimaryAction(SliceAction action) {

        }

        /**
         */
        @Override
        public void setTitle(CharSequence title) {
        }

        /**
         */
        @Override
        public void setTitle(CharSequence title, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle) {
        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle, boolean isLoading) {

        }

        /**
         */
        @Override
        public void addEndItem(long timeStamp) {

        }

        /**
         */
        @Override
        public void addEndItem(IconCompat icon, int imageMode) {

        }

        /**
         */
        @Override
        public void addEndItem(IconCompat icon, int imageMode, boolean isLoading) {

        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {

        }
    }

    /**
     */
    public static class HeaderBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.HeaderBuilder {

        /**
         */
        public HeaderBuilderImpl(@NonNull ListBuilderBasicImpl parent) {
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
        public void apply(Slice.Builder builder) {

        }

        /**
         */
        @Override
        public void setTitle(CharSequence title, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setSummary(CharSequence summarySubtitle, boolean isLoading) {

        }

        /**
         */
        @Override
        public void setPrimaryAction(SliceAction action) {

        }

        @Override
        public void setContentDescription(CharSequence description) {

        }
    }
}
