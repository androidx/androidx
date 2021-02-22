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

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_SOURCE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
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
    public void apply(@NonNull Slice.Builder builder) {
        if (mLastMessage != null) {
            Slice.Builder sb = new Slice.Builder(getBuilder()).addHints(HINT_LIST_ITEM);
            if (Build.VERSION.SDK_INT >= 23) {
                if (mLastMessage.mIcon != null) {
                    sb.addSubSlice(mLastMessage.mIcon);
                }
            }
            if (mLastMessage.mText != null) {
                sb.addText(mLastMessage.mText, null);
            }
            builder.addSubSlice(sb.build());
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
        Slice mIcon;
        CharSequence mText;
        long mTimestamp;

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
            mIcon = getBuilder().addIcon(IconCompat.createFromIcon(source),
                    SUBTYPE_SOURCE, HINT_NO_TINT).addHints(HINT_TITLE).build();
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
        public void apply(@NonNull Slice.Builder builder) {
        }
    }
}
