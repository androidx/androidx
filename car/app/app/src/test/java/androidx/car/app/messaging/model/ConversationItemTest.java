/*
 * Copyright 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link ConversationItem}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ConversationItemTest {
    /** Ensure the builder does not fail for the minimum set of required fields. */
    @Test
    public void build_withRequiredFieldsOnly() {
        TestConversationFactory.createMinimalConversationItem();

        // assert no crash
    }

    /** Ensure the builder does not fail when all fields are assigned. */
    @Test
    public void build_withAllFields() {
        TestConversationFactory.createFullyPopulatedConversationItem();

        // assert no crash
    }

    @Test
    public void build_throwsException_ifMessageListEmpty() {
        assertThrows(
                IllegalStateException.class,
                () -> TestConversationFactory.createMinimalConversationItemBuilder()
                        .setMessages(new ArrayList<>())
                        .build()
        );
    }

    public void build_throwsException_ifSenderNameMissing() {
        assertThrows(
                NullPointerException.class,
                () -> TestConversationFactory.createMinimalConversationItemBuilder()
                        .setSelf(TestConversationFactory.createMinimalMessageSenderBuilder()
                                .setName(null)
                                .build())
                        .build()
        );
    }

    public void build_throwsException_ifSenderKeyMissing() {
        assertThrows(
                NullPointerException.class,
                () -> TestConversationFactory.createMinimalConversationItemBuilder()
                        .setSelf(TestConversationFactory.createMinimalMessageSenderBuilder()
                                .setKey(null)
                                .build())
                        .build()
        );
    }

    // region .equals() & .hashCode()
    @Test
    public void equalsAndHashCode_areEqual_forMinimalConversationItem() {
        ConversationItem item1 =
                TestConversationFactory.createMinimalConversationItem();
        ConversationItem item2 =
                TestConversationFactory.createMinimalConversationItem();

        assertEqual(item1, item2);
    }

    @Test
    public void equalsAndHashCode_areEqual_forFullyPopulatedConversationItem() {
        ConversationItem item1 =
                TestConversationFactory.createFullyPopulatedConversationItem();
        ConversationItem item2 =
                TestConversationFactory.createFullyPopulatedConversationItem();

        assertEqual(item1, item2);
    }

    @Test
    public void equalsAndHashCode_produceCorrectResult_ifIndividualFieldDiffers() {
        // Create base item, for comparison
        ConversationItem fullyPopulatedItem =
                TestConversationFactory.createFullyPopulatedConversationItem();

        // Create various non-equal items
        ConversationItem modifiedId =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setId("Modified ID")
                        .build();
        ConversationItem modifiedTitle =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setTitle(CarText.create("Modified Title"))
                        .build();
        ConversationItem modifiedIcon =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setIcon(CarIcon.ALERT)
                        .build();
        ConversationItem modifiedGroupStatus =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setGroupConversation(!fullyPopulatedItem.isGroupConversation())
                        .build();
        ConversationItem modifiedSelf =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setSelf(
                                TestConversationFactory.createFullyPopulatedPersonBuilder().build())
                        .build();
        List<CarMessage> modifiedMessages = new ArrayList<>(1);
        modifiedMessages.add(
                TestConversationFactory
                        .createMinimalMessageBuilder()
                        .setBody(CarText.create("Modified Message Body"))
                        .build()
        );
        ConversationItem modifiedMessageList =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setMessages(modifiedMessages)
                        .build();
        ConversationItem modifiedConversationCallback =
                TestConversationFactory
                        .createFullyPopulatedConversationItemBuilder()
                        .setSelf(
                                TestConversationFactory.createMinimalMessageSenderBuilder().build())
                        .setConversationCallback(new ConversationCallback() {
                            @Override
                            public void onMarkAsRead() {

                            }

                            @Override
                            public void onTextReply(@NonNull String replyText) {

                            }
                        })
                        .build();

        // Verify (lack of) equality
        assertNotEqual(fullyPopulatedItem, modifiedId);
        assertNotEqual(fullyPopulatedItem, modifiedTitle);
        assertNotEqual(fullyPopulatedItem, modifiedIcon);
        assertNotEqual(fullyPopulatedItem, modifiedGroupStatus);
        assertNotEqual(fullyPopulatedItem, modifiedMessageList);
        assertNotEqual(fullyPopulatedItem, modifiedSelf);

        // NOTE: Conversation Callback does not affect equality
        assertEqual(fullyPopulatedItem, modifiedConversationCallback);
    }

    private void assertEqual(ConversationItem item1, ConversationItem item2) {
        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }

    private void assertNotEqual(ConversationItem item1, ConversationItem item2) {
        assertThat(item1).isNotEqualTo(item2);
        assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
    }
    // endregion
}
