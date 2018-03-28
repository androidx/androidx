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
import androidx.slice.builders.SliceAction;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public interface GridRowBuilder {
    /**
     * Create an TemplateBuilderImpl that implements {@link CellBuilder}.
     */
    TemplateBuilderImpl createGridRowBuilder();

    /**
     * Create an TemplateBuilderImpl that implements {@link CellBuilder} with the specified Uri.
     */
    TemplateBuilderImpl createGridRowBuilder(Uri uri);

    /**
     * Add a cell to this builder. Expected to be a builder from {@link #createGridRowBuilder}.
     */
    void addCell(TemplateBuilderImpl impl);

    /**
     * If all content in a slice cannot be shown, the cell added here will be displayed where the
     * content is cut off. This cell should have an affordance to take the user to an activity to
     * see all of the content. Expected to be a builder from {@link #createGridRowBuilder}.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void setSeeMoreCell(TemplateBuilderImpl impl);

    /**
     * If all content in a slice cannot be shown, a "see more" affordance will be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void setSeeMoreAction(PendingIntent intent);

    /**
     * Sets the action to be invoked if the user taps on the main content of the template.
     */
    void setPrimaryAction(SliceAction action);

    /**
     * Sets the content description for the entire grid row.
     */
    void setContentDescription(CharSequence description);

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
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         *  @param image the image to display in the cell.
         * @param imageMode the mode that image should be displayed in.
         */
        @NonNull
        void addImage(@NonNull IconCompat image, int imageMode);

        /**
         * Adds an image to the cell. There can be at most one image, the first one added
         * will be used, others will be ignored.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work
         * to load this content in the background, in this case the template displays a placeholder
         * until updated.l.
         */
        @NonNull
        void addImage(@NonNull IconCompat image, int imageMode, boolean isLoading);

        /**
         * Sets the action to be invoked if the user taps on this cell in the row.
         */
        @NonNull
        void setContentIntent(@NonNull PendingIntent intent);

        /**
         * Sets the content description for this cell.
         */
        void setContentDescription(CharSequence description);
    }
}
