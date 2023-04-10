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

import androidx.annotation.NonNull;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ItemList;
import androidx.core.app.Person;

import java.util.ArrayList;
import java.util.List;

/** Factory for creating {@link ConversationItem} and related data in tests */
public final class TestConversationFactory {
    private static final ConversationCallback EMPTY_CONVERSATION_CALLBACK =
            new ConversationCallback() {
                @Override
                public void onMarkAsRead() {
                }

                @Override
                public void onTextReply(@NonNull String replyText) {
                }
            };

    /**
     * Creates a {@link Person} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link Person}.
     */
    private static Person createMinimalPerson() {
        return new Person.Builder().setName("Person Name").build();
    }

    /**
     * Creates a {@link CarMessage} instance for testing
     *
     * <p>This method fills in the minimum required data to create a valid {@link CarMessage}.
     */
    private static CarMessage createMinimalMessage() {
        return new CarMessage.Builder()
                .setSender(createMinimalPerson())
                .setBody(CarText.create("Message body"))
                .build();
    }

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

    public static ItemList createItemListWithConversationItem() {
        return new ItemList.Builder().addItem(createMinimalConversationItem()).build();
    }

    private TestConversationFactory() {
        // Do not instantiate
    }
}
