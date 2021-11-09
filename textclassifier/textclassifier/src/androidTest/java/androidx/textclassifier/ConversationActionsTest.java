/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.Person;
import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConversationActionsTest {
    private static final String ID = "ID";
    private static final String TEXT = "TEXT";
    private static final Person PERSON = new Person.Builder().setKey(TEXT).build();
    private static final Long TIME = System.currentTimeMillis();
    private static final float FLOAT_TOLERANCE = 0.01f;

    private static final Bundle EXTRAS = new Bundle();
    private static final PendingIntent PENDING_INTENT = PendingIntent.getActivity(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), 0, new Intent(),
            PendingIntent.FLAG_IMMUTABLE);

    private static final RemoteActionCompat REMOTE_ACTION = new RemoteActionCompat(
            IconCompat.createWithData(new byte[0], 0, 0),
            TEXT,
            TEXT,
            PENDING_INTENT);

    static {
        EXTRAS.putString(TEXT, TEXT);
    }

    @Test
    public void testMessage_full() {
        ConversationActions.Message message = createFullMessage();

        ConversationActions.Message recovered =
                ConversationActions.Message.createFromBundle(message.toBundle());

        assertFullMessage(message);
        assertFullMessage(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testMessage_full_toFromPlatform() {
        ConversationActions.Message message = createFullMessage();

        ConversationActions.Message recovered =
                ConversationActions.Message.fromPlatform(message.toPlatform());

        assertFullMessage(message);
        assertFullMessage(recovered);
    }

    @Test
    public void testMessage_minimal() {
        ConversationActions.Message message = createMinimalMessage();

        ConversationActions.Message recovered =
                ConversationActions.Message.createFromBundle(message.toBundle());

        assertMinimalMessage(message);
        assertMinimalMessage(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testMessage_minimal_toFromPlatform() {
        ConversationActions.Message message = createMinimalMessage();

        ConversationActions.Message recovered =
                ConversationActions.Message.fromPlatform(message.toPlatform());

        assertMinimalMessage(message);
        assertMinimalMessage(recovered);
    }

    @Test
    public void testRequest_minimal() {
        ConversationActions.Request request = createMinimalRequest();

        ConversationActions.Request recovered =
                ConversationActions.Request.createFromBundle(request.toBundle());

        assertMinimalRequest(request);
        assertMinimalRequest(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testRequest_minimal_toFromPlatform() {
        ConversationActions.Request request = createMinimalRequest();

        ConversationActions.Request recovered =
                ConversationActions.Request.fromPlatform(request.toPlatform());

        assertMinimalRequest(request);
        assertMinimalRequest(recovered);
    }

    @Test
    public void testRequest_full() {
        ConversationActions.Request request = createFullRequest();

        ConversationActions.Request recovered =
                ConversationActions.Request.createFromBundle(request.toBundle());

        assertFullRequest(request);
        assertFullRequest(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testRequest_full_toFromPlatform() {
        ConversationActions.Request request = createFullRequest();

        ConversationActions.Request recovered =
                ConversationActions.Request.fromPlatform(request.toPlatform());

        assertFullRequest(request);
        assertFullRequest(recovered);
    }

    @Test
    public void testConversationAction_minimal() {
        ConversationAction conversationAction = createMinimalConversationAction();

        ConversationAction recovered =
                ConversationAction.createFromBundle(conversationAction.toBundle());

        assertMinimalConversationAction(conversationAction);
        assertMinimalConversationAction(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testConversationAction_minimal_toFromPlatform() {
        ConversationAction conversationAction = createMinimalConversationAction();

        ConversationAction recovered =
                ConversationAction.fromPlatform(conversationAction.toPlatform());

        assertMinimalConversationAction(conversationAction);
        assertMinimalConversationAction(recovered);
    }

    @Test
    public void testConversationAction_full() {
        ConversationAction conversationAction = createFullConversationAction();

        ConversationAction recovered =
                ConversationAction.createFromBundle(conversationAction.toBundle());

        assertFullConversationAction(conversationAction);
        assertFullConversationAction(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testConversationAction_full_toFromPlatform() {
        ConversationAction conversationAction = createFullConversationAction();

        ConversationAction recovered =
                ConversationAction.fromPlatform(conversationAction.toPlatform());

        assertFullConversationAction(conversationAction);
        assertFullConversationAction(recovered);
    }

    @Test
    public void testConversationActions_full() {
        ConversationActions conversationActions = createFullConversationActions();

        ConversationActions recovered =
                ConversationActions.createFromBundle(conversationActions.toBundle());

        assertFullConversationActions(conversationActions);
        assertFullConversationActions(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testConversationActions_full_toFromPlatform() {
        ConversationActions conversationActions = createFullConversationActions();

        ConversationActions recovered =
                ConversationActions.fromPlatform(conversationActions.toPlatform());

        assertFullConversationActions(conversationActions);
        assertFullConversationActions(recovered);
    }

    @Test
    public void testConversationActions_minimal() {
        ConversationActions conversationActions = createMinimalConversationActions();

        ConversationActions recovered =
                ConversationActions.createFromBundle(conversationActions.toBundle());

        assertMinimalConversationActions(conversationActions);
        assertMinimalConversationActions(recovered);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testConversationActions_minimal_toFromPlatform() {
        ConversationActions conversationActions = createMinimalConversationActions();

        ConversationActions recovered =
                ConversationActions.fromPlatform(conversationActions.toPlatform());

        assertMinimalConversationActions(conversationActions);
        assertMinimalConversationActions(recovered);
    }

    private ConversationActions.Message createFullMessage() {
        return new ConversationActions.Message.Builder(PERSON)
                .setText(TEXT)
                .setExtras(EXTRAS)
                .setReferenceTime(TIME)
                .build();
    }

    private void assertFullMessage(ConversationActions.Message message) {
        assertThat(message.getText().toString()).isEqualTo(TEXT);
        assertPerson(message.getAuthor());
        assertThat(message.getExtras().keySet()).containsExactly(TEXT);
        assertThat(message.getReferenceTime()).isEqualTo(TIME);
    }

    private ConversationActions.Message createMinimalMessage() {
        return new ConversationActions.Message.Builder(PERSON).build();
    }

    private void assertMinimalMessage(ConversationActions.Message message) {
        assertPerson(message.getAuthor());
        assertThat(message.getExtras().isEmpty()).isTrue();
        assertThat(message.getReferenceTime()).isNull();
    }

    private ConversationActions.Request createMinimalRequest() {
        ConversationActions.Message message =
                new ConversationActions.Message.Builder(PERSON)
                        .setText(TEXT)
                        .build();
        return new ConversationActions.Request.Builder(Collections.singletonList(message))
                .build();
    }

    private void assertMinimalRequest(ConversationActions.Request request) {
        assertThat(request.getConversation()).hasSize(1);
        assertThat(request.getConversation().get(0).getText().toString()).isEqualTo(TEXT);
        assertPerson(request.getConversation().get(0).getAuthor());
        assertThat(request.getHints()).isEmpty();
        assertThat(request.getMaxSuggestions()).isEqualTo(-1);
        assertThat(request.getTypeConfig()).isNotNull();
    }

    private ConversationActions.Request createFullRequest() {
        ConversationActions.Message message =
                new ConversationActions.Message.Builder(PERSON)
                        .setText(TEXT)
                        .build();
        TextClassifier.EntityConfig typeConfig =
                new TextClassifier.EntityConfig.Builder()
                        .includeTypesFromTextClassifier(false)
                        .build();
        return new ConversationActions.Request.Builder(Collections.singletonList(message))
                .setHints(
                        Collections.singletonList(
                                ConversationActions.Request.HINT_FOR_IN_APP))
                .setMaxSuggestions(10)
                .setTypeConfig(typeConfig)
                .setExtras(EXTRAS)
                .build();
    }

    private void assertFullRequest(ConversationActions.Request request) {
        assertThat(request.getConversation()).hasSize(1);
        assertThat(request.getConversation().get(0).getText().toString()).isEqualTo(TEXT);
        assertPerson(request.getConversation().get(0).getAuthor());
        assertThat(request.getHints()).containsExactly(ConversationActions.Request.HINT_FOR_IN_APP);
        assertThat(request.getMaxSuggestions()).isEqualTo(10);
        assertThat(request.getTypeConfig().shouldIncludeTypesFromTextClassifier()).isFalse();
        assertThat(request.getExtras().keySet()).containsExactly(TEXT);
    }

    private ConversationAction createMinimalConversationAction() {
        return new ConversationAction.Builder(
                ConversationAction.TYPE_CALL_PHONE)
                .build();
    }

    private void assertMinimalConversationAction(
            ConversationAction conversationAction) {
        assertThat(conversationAction.getAction()).isNull();
        assertThat(conversationAction.getConfidenceScore()).isWithin(FLOAT_TOLERANCE).of(0.0f);
        assertThat(conversationAction.getType()).isEqualTo(ConversationAction.TYPE_CALL_PHONE);
    }

    private ConversationAction createFullConversationAction() {
        return new ConversationAction.Builder(
                ConversationAction.TYPE_CALL_PHONE)
                .setConfidenceScore(1.0f)
                .setTextReply(TEXT)
                .setAction(REMOTE_ACTION)
                .setExtras(EXTRAS)
                .build();
    }

    private void assertFullConversationAction(
            ConversationAction conversationAction) {
        assertThat(conversationAction.getAction().getTitle()).isEqualTo(TEXT);
        assertThat(conversationAction.getConfidenceScore()).isWithin(FLOAT_TOLERANCE).of(1.0f);
        assertThat(conversationAction.getType()).isEqualTo(ConversationAction.TYPE_CALL_PHONE);
        assertThat(conversationAction.getTextReply()).isEqualTo(TEXT);
        assertThat(conversationAction.getExtras().keySet()).containsExactly(TEXT);
    }

    private void assertMinimalConversationActions(ConversationActions conversationActions) {
        assertThat(conversationActions.getConversationActions()).hasSize(1);
        assertThat(conversationActions.getConversationActions().get(0).getType())
                .isEqualTo(ConversationAction.TYPE_CALL_PHONE);
        assertThat(conversationActions.getId()).isNull();
    }

    private void assertFullConversationActions(ConversationActions conversationActions) {
        assertThat(conversationActions.getConversationActions()).hasSize(1);
        assertThat(conversationActions.getConversationActions().get(0).getType())
                .isEqualTo(ConversationAction.TYPE_CALL_PHONE);
        assertThat(conversationActions.getId()).isEqualTo(ID);
    }

    private ConversationActions createFullConversationActions() {
        ConversationAction conversationAction =
                new ConversationAction.Builder(
                        ConversationAction.TYPE_CALL_PHONE)
                        .build();
        return new ConversationActions(Arrays.asList(conversationAction), ID);
    }

    private ConversationActions createMinimalConversationActions() {
        ConversationAction conversationAction =
                new ConversationAction.Builder(
                        ConversationAction.TYPE_CALL_PHONE)
                        .build();
        return new ConversationActions(Collections.singletonList(conversationAction), null);
    }

    private void assertPerson(Person person) {
        assertThat(person.getKey()).isEqualTo(PERSON.getKey());
    }
}
