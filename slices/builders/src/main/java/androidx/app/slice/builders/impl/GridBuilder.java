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
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import androidx.app.slice.builders.SliceAction;

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
     * Add a cell to this builder. Expected to be a builder from {@link #createGridBuilder}.
     */
    void addCell(TemplateBuilderImpl impl);

    /**
     * If all content in a slice cannot be shown, the cell added here will be displayed where the
     * content is cut off. This cell should have an affordance to take the user to an activity to
     * see all of the content. Expected to be a builder from {@link #createGridBuilder}.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void addSeeMoreCell(TemplateBuilderImpl impl);

    /**
     * If all content in a slice cannot be shown, a "see more" affordance will be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void addSeeMoreAction(PendingIntent intent);

    /**
     * Sets the action to be invoked if the user taps on the main content of the template.
     */
    void setPrimaryAction(SliceAction action);

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
         * Adds text to the cell. There can be at most two text items, the first two added
         * will be used, others will be ignored.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.
         */
        @NonNull
        void addText(@Nullable CharSequence text, boolean isLoading);

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         */
        @NonNull
        void addTitleText(@NonNull CharSequence text);

        /**
         * Adds text to the cell. Text added with this method will be styled as a title.
         * There can be at most two text items, the first two added will be used, others
         * will be ignored.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.
         */
        @NonNull
        void addTitleText(@Nullable CharSequence text, boolean isLoading);

        /**
         * Adds an image to the cell that should be displayed as large as the cell allows.
         * There can be at most one image, the first one added will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        void addLargeImage(@NonNull Icon image);

        /**
         * Adds an image to the cell that should be displayed as large as the cell allows.
         * There can be at most one image, the first one added will be used, others will be ignored.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.
         */
        @NonNull
        void addLargeImage(@Nullable Icon image, boolean isLoading);

        /**
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         *
         * @param image the image to display in the cell.
         */
        @NonNull
        void addImage(@NonNull Icon image);

        /**
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.l.
         */
        @NonNull
        void addImage(@NonNull Icon image, boolean isLoading);

        /**
         * Sets the action to be invoked if the user taps on this cell in the row.
         */
        @NonNull
        void setContentIntent(@NonNull PendingIntent intent);
    }
}
