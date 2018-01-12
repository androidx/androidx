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

import static android.app.slice.Slice.SUBTYPE_MESSAGE;

import android.graphics.drawable.Icon;
import android.support.annotation.RestrictTo;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MessagingV1Impl extends TemplateBuilderImpl implements MessagingBuilder {

    /**
     */
    public MessagingV1Impl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void add(TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build(), SUBTYPE_MESSAGE);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {

    }

    /**
     */
    @Override
    public TemplateBuilderImpl createMessageBuilder() {
        return new MessageBuilder(this);
    }

    /**
     */
    public static final class MessageBuilder extends TemplateBuilderImpl
            implements MessagingBuilder.MessageBuilder {
        /**
         */
        public MessageBuilder(MessagingV1Impl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        @Override
        public void addSource(Icon source) {
            getBuilder().addIcon(source, android.app.slice.Slice.SUBTYPE_SOURCE);
        }

        /**
         */
        @Override
        public void addText(CharSequence text) {
            getBuilder().addText(text, null);
        }

        /**
         */
        @Override
        public void addTimestamp(long timestamp) {
            getBuilder().addTimestamp(timestamp, null);
        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {
        }
    }
}
