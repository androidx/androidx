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

import static org.junit.Assert.assertThrows;

import androidx.car.app.model.CarIcon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;

/** Tests for {@link ConversationItem}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ConversationItemTest {
    /** Ensure the builder does not fail for the minimum set of required fields. */
    @Test
    public void build_withRequiredFieldsOnly() {
        TestConversationFactory.createMinimalConversationItemBuilder().build();

        // assert no crash
    }

    /** Ensure the builder does not fail when all fields are assigned. */
    @Test
    public void build_withAllFields() {
        TestConversationFactory.createMinimalConversationItemBuilder()
                .setIcon(CarIcon.APP_ICON) // icon is chosen arbitrarily for testing purposes
                .setGroupConversation(true)
                .build();

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
}
