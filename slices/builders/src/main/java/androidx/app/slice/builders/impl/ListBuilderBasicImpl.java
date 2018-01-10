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
    public void setActions(TemplateBuilderImpl impl) {
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
    public void addSeeMoreRow(TemplateBuilderImpl builder) {
    }

    /**
     */
    @Override
    public void addSeeMoreAction(PendingIntent intent) {
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

    @Override
    public TemplateBuilderImpl createHeaderBuilder() {
        return new HeaderBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createHeaderBuilder(Uri uri) {
        return new HeaderBuilderImpl(uri);
    }

    @Override
    public TemplateBuilderImpl createActionBuilder() {
        return new ActionBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createActionBuilder(Uri uri) {
        return new ActionBuilderImpl(uri);
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
        public void addEndItem(Icon icon, boolean isLoading) {

        }

        /**
         */
        @Override
        public void addEndItem(Icon icon, PendingIntent action) {

        }

        /**
         */
        @Override
        public void addEndItem(Icon icon, PendingIntent action, boolean isLoading) {

        }

        /**
         */
        @Override
        public void addToggle(PendingIntent action, boolean isChecked, Icon icon) {

        }

        /**
         */
        @Override
        public void addToggle(PendingIntent action, boolean isChecked, Icon icon,
                boolean isLoading) {

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
        public void setTitleItem(Icon icon, boolean isLoading) {

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
        public void setTitleItem(Icon icon, PendingIntent action, boolean isLoading) {

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
        public void setTitle(CharSequence title, boolean isLoading) {

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
        public void setTitle(CharSequence title) {

        }

        /**
         */
        @Override
        public void setSubtitle(CharSequence subtitle) {

        }

        /**
         */
        @Override
        public void setSummarySubtitle(CharSequence summarySubtitle) {

        }

        /**
         */
        @Override
        public void setContentIntent(PendingIntent intent) {

        }
    }

    /**
     */
    public static class ActionBuilderImpl extends TemplateBuilderImpl
            implements ListBuilder.ActionBuilder {

        /**
         */
        public ActionBuilderImpl(@NonNull ListBuilderBasicImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        public ActionBuilderImpl(@NonNull Uri uri) {
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
        public void addAction(PendingIntent action, Icon actionIcon,
                CharSequence contentDescription, int priority) {

        }
    }
}
