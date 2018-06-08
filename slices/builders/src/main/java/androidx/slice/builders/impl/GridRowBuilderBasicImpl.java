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

import android.app.PendingIntent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.SliceAction;


/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class GridRowBuilderBasicImpl extends TemplateBuilderImpl implements GridRowBuilder {

    /**
     */
    public GridRowBuilderBasicImpl(@NonNull ListBuilderBasicImpl parent) {
        super(parent.createChildBuilder(), null);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createGridRowBuilder() {
        return new CellBuilder(this);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createGridRowBuilder(Uri uri) {
        return new CellBuilder(uri);
    }

    /**
     */
    @Override
    public void addCell(TemplateBuilderImpl impl) {
        // TODO: Consider extracting some grid content for the basic version.
    }

    /**
     */
    @Override
    public void setSeeMoreCell(TemplateBuilderImpl impl) {
    }

    /**
     */
    @Override
    public void setSeeMoreAction(PendingIntent intent) {
    }

    /**
     */
    @Override
    public void setPrimaryAction(SliceAction action) {
    }

    /**
     */
    @Override
    public void setContentDescription(CharSequence description) {
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {

    }

    /**
     */
    public static final class CellBuilder extends TemplateBuilderImpl implements
            GridRowBuilder.CellBuilder {

        /**
         */
        public CellBuilder(@NonNull GridRowBuilderBasicImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        public CellBuilder(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        @NonNull
        @Override
        public void addText(@NonNull CharSequence text) {
        }

        /**
         */
        @Override
        public void addText(@Nullable CharSequence text, boolean isLoading) {
        }

        /**
         */
        @NonNull
        @Override
        public void addTitleText(@NonNull CharSequence text) {
        }

        /**
         */
        @NonNull
        @Override
        public void addTitleText(@Nullable CharSequence text, boolean isLoading) {
        }

        /**
         */
        @NonNull
        @Override
        public void addImage(@NonNull IconCompat image, int imageMode) {
        }

        /**
         */
        @NonNull
        @Override
        public void addImage(@Nullable IconCompat image, int imageMode, boolean isLoading) {
        }

        /**
         */
        @NonNull
        @Override
        public void setContentIntent(@NonNull PendingIntent intent) {
        }

        /**
         */
        @Override
        public void setContentDescription(CharSequence description) {
        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {

        }
    }
}
