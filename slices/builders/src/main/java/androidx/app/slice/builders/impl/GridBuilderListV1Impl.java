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

package androidx.app.slice.builders.impl;

import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
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
public class GridBuilderListV1Impl extends TemplateBuilderImpl implements GridBuilder {

    /**
     */
    public GridBuilderListV1Impl(@NonNull Slice.Builder builder, SliceSpec spec) {
        super(builder, spec);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
        builder.addHints(HINT_HORIZONTAL, HINT_LIST_ITEM);
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
    public void addCell(TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build());
    }

    /**
     */
    @Override
    public Slice buildIndividual() {
        return new Slice.Builder(getBuilder()).addHints(HINT_HORIZONTAL, HINT_LIST_ITEM)
                .addSubSlice(getBuilder()
                        .addHints(HINT_HORIZONTAL, HINT_LIST_ITEM).build()).build();
    }

    /**
     */
    public static final class CellBuilder extends TemplateBuilderImpl implements
            GridBuilder.CellBuilder {

        private PendingIntent mContentIntent;

        /**
         */
        public CellBuilder(@NonNull GridBuilderListV1Impl parent) {
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
            getBuilder().addText(text, null);
        }

        /**
         */
        @NonNull
        @Override
        public void addTitleText(@NonNull CharSequence text) {
            getBuilder().addText(text, null, HINT_LARGE);
        }

        /**
         */
        @NonNull
        @Override
        public void addLargeImage(@NonNull Icon image) {
            getBuilder().addIcon(image, null, HINT_LARGE);
        }

        /**
         */
        @NonNull
        @Override
        public void addImage(@NonNull Icon image) {
            getBuilder().addIcon(image, null);
        }

        /**
         */
        @NonNull
        @Override
        public void setContentIntent(@NonNull PendingIntent intent) {
            mContentIntent = intent;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public void apply(Slice.Builder b) {
        }

        /**
         */
        @Override
        @NonNull
        public Slice build() {
            if (mContentIntent != null) {
                return new Slice.Builder(getBuilder())
                        .addHints(HINT_HORIZONTAL, HINT_LIST_ITEM)
                        .addAction(mContentIntent, getBuilder().build(), null)
                        .build();
            }
            return getBuilder().addHints(HINT_HORIZONTAL, HINT_LIST_ITEM).build();
        }
    }
}
