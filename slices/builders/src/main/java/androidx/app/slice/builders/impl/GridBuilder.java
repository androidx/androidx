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

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public interface GridBuilder {
    /**
     * Create an TemplateBuilderImpl that implements {@link CellBuilder}.
     */
    TemplateBuilderImpl createGridBuilder();

    /**
     * Create an TemplateBuilderImpl that implements {@link CellBuilder} with the specified Uri.
     */
    TemplateBuilderImpl createGridBuilder(Uri uri);

    /**
     * Add a cell to this builder. Expected to be a builder from {@link #createGridBuilder};
     */
    void addCell(TemplateBuilderImpl impl);

    /**
     * Builds a standalone slice of this grid builder (i.e. not contained within a List).
     */
    Slice buildIndividual();

    /**
     */
    interface CellBuilder {
        /**
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         */
        @NonNull
        void addText(@NonNull CharSequence text);

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         */
        @NonNull
        void addTitleText(@NonNull CharSequence text);

        /**
         * Adds an image to the cell that should be displayed as large as the cell allows.
         * There can be at most one image, the first one added will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        void addLargeImage(@NonNull Icon image);

        /**
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        void addImage(@NonNull Icon image);

        /**
         * Sets the action to be invoked if the user taps on this cell in the row.
         */
        @NonNull
        void setContentIntent(@NonNull PendingIntent intent);
    }
}
