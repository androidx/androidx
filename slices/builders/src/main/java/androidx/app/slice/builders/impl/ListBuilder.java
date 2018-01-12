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
import android.support.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public interface ListBuilder {

    /**
     * Add a row to list builder.
     */
    void addRow(TemplateBuilderImpl impl);
    /**
     * Add a grid row to the list builder.
     */
    void addGrid(TemplateBuilderImpl impl);
    /**
     * Add a summary row for this template. The summary content is displayed
     * when the slice is displayed in small format.
     */
    void addSummaryRow(TemplateBuilderImpl builder);

    /**
     * Sets the color to tint items displayed by this template (e.g. icons).
     */
    void setColor(int color);

    /**
     * Create a builder that implements {@link RowBuilder}.
     */
    TemplateBuilderImpl createRowBuilder();
    /**
     * Create a builder that implements {@link RowBuilder}.
     */
    TemplateBuilderImpl createRowBuilder(Uri uri);

    /**
     * Create a builder that implements {@link GridBuilder}.
     */
    TemplateBuilderImpl createGridBuilder();

    /**
     */
    public interface RowBuilder {

        /**
         * Sets this row to be the header of the slice. This item will be displayed at the top of
         * the slice and other items in the slice will scroll below it.
         */
        void setIsHeader(boolean isHeader);

        /**
         * Sets the title item to be the provided timestamp. Only one timestamp can be added, if
         * one is already added this will throw {@link IllegalArgumentException}.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        void setTitleItem(long timeStamp);

        /**
         * Sets the title item to be the provided icon.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        void setTitleItem(Icon icon);

        /**
         * Sets the title item to be a tappable icon.
         * <p>
         * There can only be one title item, this will replace any other title
         * items that may have been set.
         */
        void setTitleItem(Icon icon, PendingIntent action);

        /**
         * Sets the action to be invoked if the user taps on the main content of the template.
         */
        void setContentIntent(PendingIntent action);

        /**
         * Sets the title text.
         */
        void setTitle(CharSequence title);

        /**
         * Sets the subtitle text.
         */
        void setSubtitle(CharSequence subtitle);

        /**
         * Adds a timestamp to be displayed at the end of the row.
         */
        void addEndItem(long timeStamp);

        /**
         * Adds an icon to be displayed at the end of the row.
         */
        void addEndItem(Icon icon);

        /**
         * Adds a tappable icon to be displayed at the end of the row.
         */
        void addEndItem(Icon icon, PendingIntent action);

        /**
         * Adds a toggle action to the template with custom icons to represent checked and unchecked
         * state.
         */
        void addToggle(PendingIntent action, boolean isChecked, Icon icon);
    }
}
