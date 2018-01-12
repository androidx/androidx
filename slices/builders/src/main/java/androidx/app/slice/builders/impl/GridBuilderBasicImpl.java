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
public class GridBuilderBasicImpl extends TemplateBuilderImpl implements GridBuilder {

    /**
     */
    public GridBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createGridBuilder() {
        return new CellBuilder(this);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createGridBuilder(Uri uri) {
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
    public Slice buildIndividual() {
        // Empty slice, nothing useful from a grid to basic.
        return getBuilder().build();
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {

    }

    /**
     */
    public static final class CellBuilder extends TemplateBuilderImpl implements
            GridBuilder.CellBuilder {

        /**
         */
        public CellBuilder(@NonNull GridBuilderBasicImpl parent) {
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
        @NonNull
        @Override
        public void addTitleText(@NonNull CharSequence text) {
        }

        /**
         */
        @NonNull
        @Override
        public void addLargeImage(@NonNull Icon image) {
        }

        /**
         */
        @NonNull
        @Override
        public void addImage(@NonNull Icon image) {
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
        public void apply(Slice.Builder builder) {

        }
    }
}
