/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.messaging.model;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ItemList;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/** Factory for creating {@link ConversationItem} and related data in tests */
public final class TestConversationFactory {
    private static final IconCompat TEST_SENDER_ICON =
            IconCompat.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));

    private static final Uri TEST_SENDER_URI =
            Uri.parse("http://foo.com/test/sender/uri");
    private static final ConversationCallback EMPTY_CONVERSATION_CALLBACK =
            new ConversationCallback() {
                @Override
                public void onMarkAsRead() {
                }

                @Override
                public void onTextReply(@NonNull String replyText) {
                }
            };

    // region Person
    /**
     * Creates a {@link Person.Builder} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link Person}.
     */
    public static Person.Builder createMinimalMessageSenderBuilder() {
        return new Person.Builder().setName("Person Name").setKey("sender_key");
    }

    /**
     * Creates a {@link Person} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link Person}.
     */
    private static Person createMinimalMessageSender() {
        return createMinimalMessageSenderBuilder().build();
    }

    /**
     * Creates a {@link Person.Builder} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link Person}.
     */
    public static Person.Builder createFullyPopulatedPersonBuilder() {
        return createMinimalMessageSenderBuilder()
                .setKey("Foo Person")
                .setIcon(TEST_SENDER_ICON)
                .setUri(TEST_SENDER_URI.toString())
                .setBot(true)
                .setImportant(true);
    }

    /**
     * Creates a {@link Person} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link Person}.
     */
    private static Person createFullyPopulatedPerson() {
        return createFullyPopulatedPersonBuilder().build();
    }
    // endregion

    // region Message
    /**
     * Creates a {@link CarMessage.Builder} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link CarMessage}.
     */
    public static CarMessage.Builder createMinimalMessageBuilder() {
        return new CarMessage.Builder()
                .setBody(CarText.create("Message body"));
    }

    /**
     * Creates a {@link CarMessage} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link CarMessage}.
     */
    public static CarMessage createMinimalMessage() {
        return createMinimalMessageBuilder().build();
    }

    /**
     * Creates a {@link CarMessage.Builder} instance for testing
     *
     * <p>This method populates every field in  {@link CarMessage.Builder}.
     */
    public static CarMessage.Builder createFullyPopulatedMessageBuilder() {
        return createMinimalMessageBuilder()
                .setSender(createFullyPopulatedPerson())
                .setRead(true)
                .setReceivedTimeEpochMillis(12345);
    }

    /**
     * Creates a {@link CarMessage} instance for testing
     *
     * <p>This method populates every field in  {@link CarMessage.Builder}.
     */
    public static CarMessage createFullyPopulatedMessage() {
        return createFullyPopulatedMessageBuilder().build();
    }
    // endregion

    // region ConversationItem
    /**
     * Creates a {@link ConversationItem.Builder} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link
     * ConversationItem.Builder}.
     */
    public static ConversationItem.Builder createMinimalConversationItemBuilder() {
        List<CarMessage> messages = new ArrayList<>(1);
        messages.add(createMinimalMessage());

        return new ConversationItem.Builder()
                .setId("conversation_id")
                .setTitle(CarText.create("Conversation Title"))
                .setSelf(createMinimalMessageSender())
                .setMessages(messages)
                .setConversationCallback(EMPTY_CONVERSATION_CALLBACK);
    }

    /**
     * Creates a {@link ConversationItem} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link ConversationItem}.
     */
    public static ConversationItem createMinimalConversationItem() {
        return createMinimalConversationItemBuilder().build();
    }

    /**
     * Creates a {@link ConversationItem.Builder} instance for testing
     *
     * <p>This method populates every field in {@link ConversationItem.Builder}.
     */
    public static ConversationItem.Builder createFullyPopulatedConversationItemBuilder() {
        return createMinimalConversationItemBuilder()
                // APP_ICON was chosen because it is easy to access in code
                // In the future, it may make more sense to add a "realistic" contact photo here.
                .setIcon(CarIcon.APP_ICON)
                .setGroupConversation(true);
    }

    /**
     * Creates a {@link ConversationItem} instance for testing
     *
     * <p>This method populates every field in {@link ConversationItem.Builder}.
     */
    public static ConversationItem createFullyPopulatedConversationItem() {
        return createFullyPopulatedConversationItemBuilder().build();
    }
    // endregion

    public static ItemList createItemListWithConversationItem() {
        return new ItemList.Builder().addItem(createMinimalConversationItem()).build();
    }

    private TestConversationFactory() {
        // Do not instantiate
    }
}
