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

import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.function.Consumer;

import androidx.app.slice.Slice;

/**
 * Builder to construct slice content in a messaging format.
 */
public class MessagingSliceBuilder extends TemplateSliceBuilder {

    /**
     * The maximum number of messages that will be retained in the Slice itself (the
     * number displayed is up to the platform).
     */
    public static final int MAXIMUM_RETAINED_MESSAGES = 50;

    public MessagingSliceBuilder(@NonNull Uri uri) {
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
    public MessagingSliceBuilder add(MessageBuilder builder) {
        getBuilder().addSubSlice(builder.build());
        return this;
    }

    /**
     * Add a subslice to this builder.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public MessagingSliceBuilder add(Consumer<MessageBuilder> c) {
        MessageBuilder b = new MessageBuilder(this);
        c.accept(b);
        return add(b);
    }

    /**
     * Builder for adding a message to {@link MessagingSliceBuilder}.
     */
    public static final class MessageBuilder extends TemplateSliceBuilder {
        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public MessageBuilder(MessagingSliceBuilder parent) {
            super(parent.createChildBuilder());
        }

        /**
         * Add the icon used to display contact in the messaging experience
         */
        public MessageBuilder addSource(Icon source) {
            getBuilder().addIcon(source, android.app.slice.Slice.SUBTYPE_SOURCE);
            return this;
        }

        /**
         * Add the text to be used for this message.
         */
        public MessageBuilder addText(CharSequence text) {
            getBuilder().addText(text, null);
            return this;
        }

        /**
         * Add the time at which this message arrived in ms since Unix epoch
         */
        public MessageBuilder addTimestamp(long timestamp) {
            getBuilder().addTimestamp(timestamp, null);
            return this;
        }

        @Override
        public void apply(Slice.Builder builder) {
        }
    }
}
