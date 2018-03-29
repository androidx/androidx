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

import android.graphics.drawable.Icon;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public interface MessagingBuilder {
    /**
     * Add a subslice to this builder.
     */
    void add(TemplateBuilderImpl builder);

    /**
     * Create a builder that implements {@link MessageBuilder}
     */
    TemplateBuilderImpl createMessageBuilder();

    /**
     */
    public interface MessageBuilder {

        /**
         * Add the icon used to display contact in the messaging experience
         */
        @RequiresApi(23)
        void addSource(Icon source);

        /**
         * Add the text to be used for this message.
         */
        void addText(CharSequence text);

        /**
         * Add the time at which this message arrived in ms since Unix epoch
         */
        void addTimestamp(long timestamp);
    }
}
