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

package androidx.slice.builders;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.MessagingBasicImpl;
import androidx.slice.builders.impl.MessagingBuilder;
import androidx.slice.builders.impl.MessagingListV1Impl;
import androidx.slice.builders.impl.MessagingV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;

/**
 * Builder to construct slice content in a messaging format.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class MessagingSliceBuilder extends TemplateSliceBuilder {

    /**
     * The maximum number of messages that will be retained in the Slice itself (the
     * number displayed is up to the platform).
     */
    public static final int MAXIMUM_RETAINED_MESSAGES = 50;

    private MessagingBuilder mBuilder;

    /**
     * Create a MessagingSliceBuilder with the specified uri.
     */
    public MessagingSliceBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
    }

    /**
     * Add a subslice to this builder.
     */
    public MessagingSliceBuilder add(MessageBuilder builder) {
        mBuilder.add((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    /**
     * Add a subslice to this builder.
     */
    public MessagingSliceBuilder add(Consumer<MessageBuilder> c) {
        MessageBuilder b = new MessageBuilder(this);
        c.accept(b);
        return add(b);
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mBuilder = (MessagingBuilder) impl;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Override
    protected TemplateBuilderImpl selectImpl() {
        if (checkCompatible(SliceSpecs.MESSAGING)) {
            return new MessagingV1Impl(getBuilder(), SliceSpecs.MESSAGING);
        } else if (checkCompatible(SliceSpecs.LIST)) {
            return new MessagingListV1Impl(getBuilder(), SliceSpecs.LIST);
        } else if (checkCompatible(SliceSpecs.BASIC)) {
            return new MessagingBasicImpl(getBuilder(), SliceSpecs.BASIC);
        }
        return null;
    }

    /**
     * Builder for adding a message to {@link MessagingSliceBuilder}.
     */
    public static final class MessageBuilder extends TemplateSliceBuilder {

        private MessagingBuilder.MessageBuilder mImpl;

        /**
         * Creates a MessageBuilder with the specified parent.
         */
        public MessageBuilder(MessagingSliceBuilder parent) {
            super(parent.mBuilder.createMessageBuilder());
        }

        /**
         * Add the icon used to display contact in the messaging experience
         */
        @RequiresApi(23)
        public MessageBuilder addSource(Icon source) {
            mImpl.addSource(source);
            return this;
        }

        /**
         * Add the icon used to display contact in the messaging experience
         */
        public MessageBuilder addSource(IconCompat source) {
            if (Build.VERSION.SDK_INT >= 23) {
                mImpl.addSource(source.toIcon());
            }
            return this;
        }

        /**
         * Add the text to be used for this message.
         */
        public MessageBuilder addText(CharSequence text) {
            mImpl.addText(text);
            return this;
        }

        /**
         * Add the time at which this message arrived in ms since Unix epoch
         */
        public MessageBuilder addTimestamp(long timestamp) {
            mImpl.addTimestamp(timestamp);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl impl) {
            mImpl = (MessagingBuilder.MessageBuilder) impl;
        }
    }
}
