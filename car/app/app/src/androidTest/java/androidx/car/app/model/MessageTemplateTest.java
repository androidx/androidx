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

package androidx.car.app.model;

import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link MessageTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class MessageTemplateTest {

    private final String mTitle = "header";
    private final String mDebugMessage = "debugMessage";
    private final Throwable mCause = new IllegalStateException("bad");
    private final String mMessage = "foo";
    private final Action mAction = Action.BACK;
    private final CarIcon mIcon = CarIcon.ALERT;

    @Test
    public void emptyMessage_throws() {
        assertThrows(
                IllegalStateException.class, () -> MessageTemplate.builder("").setTitle(
                        mTitle).build());
    }

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));
        assertThrows(
                IllegalArgumentException.class,
                () -> MessageTemplate.builder("hello").setTitle(mTitle).setIcon(carIcon));
    }

    @Test
    public void noHeaderTitleOrAction_throws() {
        assertThrows(IllegalStateException.class, () -> MessageTemplate.builder(mMessage).build());

        // Positive cases.
        MessageTemplate.builder(mMessage).setTitle(mTitle).build();
        MessageTemplate.builder(mMessage).setHeaderAction(mAction).build();
    }

    @Test
    public void createDefault_valuesAreNull() {
        MessageTemplate template = MessageTemplate.builder(mMessage).setTitle(mTitle).build();
        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().getText()).isEqualTo("header");
        assertThat(template.getIcon()).isNull();
        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getActionList()).isNull();
        assertThat(template.getDebugMessage()).isNull();
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        MessageTemplate.builder(mMessage)
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void createWithContents_hasProperValuesSet() {
        Throwable exception = new IllegalStateException();
        CarIcon icon = BACK;
        Action action = Action.builder().setOnClickListener(() -> {
        }).setTitle("foo").build();

        MessageTemplate template =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .setDebugCause(exception)
                        .setIcon(icon)
                        .setActions(ImmutableList.of(action))
                        .build();

        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().toString()).isEqualTo(mTitle);
        assertThat(template.getDebugMessage().toString()).isEqualTo(
                Log.getStackTraceString(exception));
        assertThat(template.getIcon()).isEqualTo(icon);
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
        assertThat(template.getActionList().getList()).containsExactly(action);
    }

    @Test
    public void equals() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setHeaderAction(Action.BACK)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setHeaderAction(Action.BACK)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isEqualTo(template2);
    }

    @Test
    public void notEquals_differentDebugMessage() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage("yo")
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentCause() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(new IllegalStateException("something else bad"))
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentMessage() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setMessage(mMessage)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder("bar")
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentHeaderAction() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setHeaderAction(Action.BACK)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setHeaderAction(Action.APP_ICON)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentActions() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction, mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentIcon() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(CarIcon.ERROR)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentTitle() {
        MessageTemplate template1 =
                MessageTemplate.builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                MessageTemplate.builder(mMessage)
                        .setTitle("Header2")
                        .setDebugMessage(mDebugMessage)
                        .setDebugCause(mCause)
                        .setActions(ImmutableList.of(mAction))
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }
}
