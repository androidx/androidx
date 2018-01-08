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

import android.graphics.drawable.Icon;
import android.support.annotation.RestrictTo;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class MessagingListV1Impl extends TemplateBuilderImpl implements MessagingBuilder{

    private final ListBuilderV1Impl mListBuilder;

    /**
     */
    public MessagingListV1Impl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
        mListBuilder = new ListBuilderV1Impl(b, spec);
    }

    /**
     */
    @Override
    public void add(TemplateBuilderImpl builder) {
        MessageBuilder b = (MessageBuilder) builder;
        mListBuilder.addRow(b.mListBuilder);
    }

    /**
     */
    @Override
    public TemplateBuilderImpl createMessageBuilder() {
        return new MessageBuilder(this);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
        mListBuilder.apply(builder);
    }

    /**
     */
    public static final class MessageBuilder extends TemplateBuilderImpl
            implements MessagingBuilder.MessageBuilder {
        private final ListBuilderV1Impl.RowBuilderImpl mListBuilder;

        /**
         */
        public MessageBuilder(MessagingListV1Impl parent) {
            this(parent.createChildBuilder());
        }

        private MessageBuilder(Slice.Builder builder) {
            super(builder, null);
            mListBuilder = new ListBuilderV1Impl.RowBuilderImpl(builder);
        }

        /**
         */
        @Override
        public void addSource(Icon source) {
            mListBuilder.setTitleItem(source);
        }

        /**
         */
        @Override
        public void addText(CharSequence text) {
            mListBuilder.setSubtitle(text);
        }

        /**
         */
        @Override
        public void addTimestamp(long timestamp) {
            mListBuilder.addEndItem(timestamp);
        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {
            mListBuilder.apply(builder);
        }
    }
}
