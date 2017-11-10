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

package androidx.app.slice.builders;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

/**
 * Builder to construct slice content.
 * <p>
 * Use this builder for showing rows of content which is composed of text, images, and actions. For
 * more details {@see RowBuilder}.
 * </p>
 * <p>
 * Slices can be displayed in different formats:
 * <ul>
 *     <li>Shortcut - The slice is displayed as an icon with a text label.</li>
 *     <li>Small - Only a single row of content is displayed in small format, to specify which
 *         row to display in small format see {@link RowBuilder#setIsHeader(boolean)}.</li>
 *     <li>Large - As many rows of content are shown as possible. If the presenter of the slice
 *         allows scrolling then all rows of content will be displayed in a scrollable view.</li>
 * </ul>
 * </p>
 *
 * @see RowBuilder
 */
public class ListBuilder extends TemplateSliceBuilder {

    public ListBuilder(@NonNull Uri uri) {
        super(uri);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void apply(androidx.app.slice.Slice.Builder builder) {

    }

    /**
     * Add a subslice to this builder.
     */
    public ListBuilder add(RowBuilder builder) {
        getBuilder().addSubSlice(builder.build());
        return this;
    }

    /**
     * Add a subslice to this builder.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ListBuilder add(Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return add(b);
    }

    /**
     * Sets the color to tint items displayed by this template (e.g. icons).
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListBuilder setColor(int color) {
        getBuilder().addColor(color, null);
        return this;
    }
}
