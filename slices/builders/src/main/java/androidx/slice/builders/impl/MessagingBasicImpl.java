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
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class MessagingBasicImpl extends TemplateBuilderImpl implements
        MessagingBuilder {
    private MessageBuilder mLastMessage;

    /**
     */
    public MessagingBasicImpl(Slice.Builder builder, SliceSpec spec) {
        super(builder, spec);
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
        if (mLastMessage != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (mLastMessage.mIcon != null) {
                    builder.addIcon(IconCompat.createFromIcon(mLastMessage.mIcon), null);
                }
            }
            if (mLastMessage.mText != null) {
                builder.addText(mLastMessage.mText, null);
            }
        }
    }

    /**
     */
    @Override
    public void add(TemplateBuilderImpl builder) {
        MessageBuilder b = (MessageBuilder) builder;
        if (mLastMessage == null || mLastMessage.mTimestamp < b.mTimestamp) {
            mLastMessage = b;
        }
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

        @RequiresApi(23)
        private Icon mIcon;
        private CharSequence mText;
        private long mTimestamp;

        /**
         */
        public MessageBuilder(MessagingBasicImpl parent) {
            this(parent.createChildBuilder());
        }

        /**
         */
        private MessageBuilder(Slice.Builder builder) {
            super(builder, null);
        }

        /**
         */
        @Override
        @RequiresApi(23)
        public void addSource(Icon source) {
            mIcon = source;
        }

        /**
         */
        @Override
        public void addText(CharSequence text) {
            mText = text;
        }

        /**
         */
        @Override
        public void addTimestamp(long timestamp) {
            mTimestamp = timestamp;
        }

        /**
         */
        @Override
        public void apply(Slice.Builder builder) {
        }
    }
}
