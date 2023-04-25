/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.TabContentsConstraints;

import java.util.Objects;

/**
 * Represents the contents to display for a selected tab in a {@link TabTemplate}.
 */
@CarProtocol
@RequiresCarApi(6)
@KeepFields
public class TabContents implements Content {
    /**
     * Content ID for TabContents
     *
     * <p>This Content ID will be used to refresh the displayed template in the TabContents.
     */
    public static final String CONTENT_ID = "TAB_CONTENTS_CONTENT_ID";

    @Nullable
    private final Template mTemplate;

    /**
     * Returns the static content ID associated with TabContents.
     *
     * @see TabContents#CONTENT_ID
     */

    @NonNull
    @Override
    public String getContentId() {
        return CONTENT_ID;
    }

    /** Returns the wrapped {@link Template} to display as the contents. */
    @NonNull
    public Template getTemplate() {
        return requireNonNull(mTemplate);
    }

    @NonNull
    @Override
    public String toString() {
        return "[template: " + mTemplate + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTemplate);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TabContents)) {
            return false;
        }
        TabContents otherTabContents = (TabContents) other;

        return Objects.equals(mTemplate, otherTabContents.mTemplate);
    }

    TabContents(TabContents.Builder builder) {
        mTemplate = builder.mTemplate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private TabContents() {
        mTemplate = null;
    }

    /** A builder of {@link TabContents}. */
    public static final class Builder {
        @NonNull
        Template mTemplate;

        /**
         * Constructs the {@link TabContents} defined by this builder.
         */
        @NonNull
        public TabContents build() {
            return new TabContents(this);
        }

        /**
         * Creates a {@link TabContents.Builder} instance using the given {@link Template} to
         * display as contents.
         *
         * <h4>Requirements</h4>
         *
         * There should be no title, Header{@link Action} or {@link ActionStrip} set on the
         * template.
         * The host will ignore these.
         *
         * @throws NullPointerException     if {@code template} is null
         * @throws IllegalArgumentException if {@code template} does not meet the requirements
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull Template template) {
            TabContentsConstraints.DEFAULT.validateOrThrow(requireNonNull(template));
            mTemplate = template;
        }
    }
}
