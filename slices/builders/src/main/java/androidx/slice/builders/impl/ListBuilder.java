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
import android.os.PersistableBundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RangeBuilder;
import androidx.slice.builders.ListBuilder.RatingBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SelectionBuilder;
import androidx.slice.builders.SliceAction;

import java.time.Duration;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public interface ListBuilder {

    /**
     * Add a row to list builder.
     */
    void addRow(@NonNull RowBuilder impl);

    /**
     * Add a grid row to the list builder.
     */
    void addGridRow(@NonNull GridRowBuilder impl);

    /**
     * Adds a header to this template.
     * <p>
     * The header should contain a title that is representative of the content in this slice along
     * with an intent that links to the app activity associated with this content.
     */
    void setHeader(@NonNull HeaderBuilder impl);

    /**
     * Adds an action to this template. Actions added with this method are grouped together and
     * may be shown on the template in large or small formats.
     */
    void addAction(@NonNull SliceAction action);

    /**
     * Add an star rating row to the list builder.
     */
    void addRating(@NonNull RatingBuilder builder);

    /**
     * Add an input range row to the list builder.
     */
    void addInputRange(@NonNull InputRangeBuilder builder);

    /**
     * Add a range row to the list builder.
     */
    void addRange(@NonNull RangeBuilder builder);

    /**
     * Add a selection row to the list builder.
     */
    void addSelection(@NonNull SelectionBuilder builder);

    /**
     * If all content in a slice cannot be shown, the row added here will be displayed where the
     * content is cut off. This row should have an affordance to take the user to an activity to
     * see all of the content.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void setSeeMoreRow(@NonNull RowBuilder builder);

    /**
     * If all content in a slice cannot be shown, a "see more" affordance will be displayed where
     * the content is cut off. The action added here should take the user to an activity to see
     * all of the content, and will be invoked when the "see more" affordance is tapped.
     * <p>
     * Only one see more affordance can be added, this throws {@link IllegalStateException} if
     * a row or action has been previously added.
     * </p>
     */
    void setSeeMoreAction(@NonNull PendingIntent intent);

    /**
     * Sets the color to tint items displayed by this template (e.g. icons).
     */
    void setColor(@ColorInt int color);

    /**
     * Sets keywords to associate with this slice.
     */
    void setKeywords(@NonNull Set<String> keywords);

    /**
     * Sets the time-to-live for this slice, i.e. how long the data contained in the slice
     * can remain fresh.
     *
     * @param ttl the length in milliseconds that this content can live for.
     */
    void setTtl(long ttl);

    /**
     * Sets the time-to-live for this slice, i.e. how long the data contained in the slice
     * can remain fresh.
     *
     * @param ttl the {@link Duration} that this content can live for. Null duration indicates
     *            infinite time-to-live.
     */
    @RequiresApi(26)
    void setTtl(@Nullable Duration ttl);

    /**
     * Sets whether this slice indicates an error, i.e. the normal contents of this slice are
     * unavailable and instead the slice contains a message indicating an error.
     */
    void setIsError(boolean isError);

    /**
     * Sets the desired layout direction for the content in this slice.
     */
    void setLayoutDirection(int layoutDirection);

    /**
     * Sets additional information to be passed to the host of the slice.
     *
     * @param extras The Bundle of extras to add to this slice.
     */
    @RequiresApi(21)
    void setHostExtras(@NonNull PersistableBundle extras);
}

