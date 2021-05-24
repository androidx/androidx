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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link MessageTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MessageTemplateTest {

    private final String mTitle = "header";
    private final String mDebugMessage = "debugMessage";
    private final Throwable mCause = new IllegalStateException("bad");
    private final String mMessage = "foo";
    private final Action mAction = Action.BACK;
    private final CarIcon mIcon = CarIcon.ALERT;
    private final ActionStrip mActionStrip = new ActionStrip.Builder().addAction(mAction).build();

    @Test
    public void emptyMessage_throws() {
        assertThrows(
                IllegalStateException.class, () -> new MessageTemplate.Builder("").setTitle(
                        mTitle).build());
    }

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageTemplate.Builder("hello").setTitle(mTitle).setIcon(carIcon));
    }

    @Test
    public void isLoadingWithIcon_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new MessageTemplate.Builder("hello").setLoading(true).setIcon(mIcon).build());
    }

    @Test
    public void noHeaderTitleOrAction_throws() {
        assertThrows(IllegalStateException.class,
                () -> new MessageTemplate.Builder(mMessage).build());

        // Positive cases.
        new MessageTemplate.Builder(mMessage).setTitle(mTitle).build();
        new MessageTemplate.Builder(mMessage).setHeaderAction(mAction).build();
    }

    @Test
    public void moreThanTwoActions_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new MessageTemplate.Builder(mMessage)
                        .addAction(mAction)
                        .addAction(mAction)
                        .addAction(mAction));
    }

    @Test
    public void createDefault_valuesAreNull() {
        MessageTemplate template = new MessageTemplate.Builder(mMessage).setTitle(mTitle).build();
        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().toString()).isEqualTo("header");
        assertThat(template.getIcon()).isNull();
        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getActions()).isEmpty();
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.getDebugMessage()).isNull();
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MessageTemplate.Builder(mMessage)
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void createWithContents_hasProperValuesSet() {
        Throwable exception = new IllegalStateException();
        CarIcon icon = BACK;
        Action action = new Action.Builder().setOnClickListener(() -> {
        }).setTitle("foo").build();

        MessageTemplate template =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .setDebugMessage(exception)
                        .setIcon(icon)
                        .addAction(action)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().toString()).isEqualTo(mTitle);
        assertThat(template.getDebugMessage().toString()).isEqualTo(
                Log.getStackTraceString(exception));
        assertThat(template.getIcon()).isEqualTo(icon);
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
        assertThat(template.getActions()).containsExactly(action);
        assertThat(template.getActionStrip()).isEqualTo(mActionStrip);
    }

    @Test
    public void create_messageHasTextVariants() {
        List<CharSequence> variants = new ArrayList<>();
        variants.add("This is a long message that only fits in a large screen");
        variants.add("This is a short message");
        CarText message =
                new CarText.Builder(variants.get(0)).addVariant(variants.get(1)).build();

        MessageTemplate template =
                new MessageTemplate.Builder(message)
                        .setTitle(mTitle)
                        .build();

        assertThat(template.getMessage().toCharSequence().toString()).isEqualTo(variants.get(0));
        assertThat(template.getMessage().getVariants().size()).isEqualTo(1);
        assertThat(template.getMessage().getVariants().get(0).toString()).isEqualTo(
                variants.get(1));
    }

    @Test
    public void equals() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isEqualTo(template2);
    }

    @Test
    public void notEquals_differentDebugMessage() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage("yo")
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentCause() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(new IllegalStateException("something else bad"))
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentMessage() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder("bar")
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentHeaderAction() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .setHeaderAction(Action.APP_ICON)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentActions() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentActionStrip() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(new ActionStrip.Builder()
                                .addAction(Action.BACK)
                                .addAction(Action.APP_ICON)
                                .build())
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentIcon() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(CarIcon.ERROR)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentTitle() {
        MessageTemplate template1 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();
        MessageTemplate template2 =
                new MessageTemplate.Builder(mMessage)
                        .setTitle("Header2")
                        .setDebugMessage(mDebugMessage)
                        .setDebugMessage(mCause)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .setIcon(mIcon)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }
}
