/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MessageTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class LongMessageTemplateTest {

    private final String mTitle = "header";
    private final String mMessage = "foo";
    private final Action mAction =
            new Action.Builder().setTitle("Action").setOnClickListener(
                    ParkedOnlyOnClickListener.create(() -> {
                    })).build();
    private final ActionStrip mActionStrip = new ActionStrip.Builder().addAction(mAction).build();

    @Test
    public void emptyMessage_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LongMessageTemplate.Builder("").setTitle(mTitle).build());
    }

    @Test
    public void noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new LongMessageTemplate.Builder(mMessage).build());

        // Positive cases.
        new LongMessageTemplate.Builder(mMessage).setTitle(mTitle).build();
        new LongMessageTemplate.Builder(mMessage).setHeaderAction(Action.APP_ICON).build();
    }

    @Test
    public void header_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage).setTitle(title));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new LongMessageTemplate.Builder(mMessage).setTitle(title2).build();
    }

    @Test
    public void moreThanTwoActions_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage)
                        .addAction(mAction)
                        .addAction(mAction)
                        .addAction(mAction));
    }
    @Test
    public void twoPrimaryActions_throws() {
        Action primaryAction = new Action.Builder().setTitle("primaryAction")
                .setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {})
                )
                .setFlags(FLAG_PRIMARY).build();

        assertThrows(IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage)
                        .addAction(primaryAction)
                        .addAction(primaryAction)
                        .build());
    }

    @Test
    public void action_unsupportedSpans_throws() {
        CharSequence title1 = TestUtils.getCharSequenceWithClickableSpan("Title");
        Action action1 =
                new Action.Builder().setTitle(title1).setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {
                        })).build();
        assertThrows(IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage).addAction(action1));
        CarText title2 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        Action action2 =
                new Action.Builder().setTitle(title2).setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {
                        })).build();
        assertThrows(IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage).addAction(action2));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithColorSpan("Title");
        Action action3 =
                new Action.Builder().setTitle(title3).setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {
                        })).build();
        new LongMessageTemplate.Builder(mMessage).setTitle("Title").addAction(action3).build();
        CarText title4 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        Action action4 =
                new Action.Builder().setTitle(title4).setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {
                        })).build();
        new LongMessageTemplate.Builder(mMessage).setTitle("Title").addAction(action4).build();
    }

    @Test
    public void createDefault_valuesAreNull() {
        LongMessageTemplate template = new LongMessageTemplate.Builder(mMessage)
                .setTitle(mTitle)
                .build();

        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().toString()).isEqualTo("header");
        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getActions()).isEmpty();
        assertThat(template.getActionStrip()).isNull();
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage)
                        .setHeaderAction(
                                new Action.Builder()
                                        .setTitle("Action")
                                        .setOnClickListener(() -> { })
                                        .build())
                        .build());
    }

    @Test
    public void createWithContents_hasProperValuesSet() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        LongMessageTemplate template = new LongMessageTemplate.Builder(mMessage)
                .setTitle(mTitle)
                .setHeaderAction(Action.BACK)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template.getMessage().toString()).isEqualTo(mMessage);
        assertThat(template.getTitle().toString()).isEqualTo(mTitle);
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
        assertThat(template.getActions()).containsExactly(mAction);
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void createInstance_notParkedOnlyAction_throws() {
        Action action = new Action.Builder()
                .setOnClickListener(() -> { })
                .setTitle("foo").build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new LongMessageTemplate.Builder(mMessage)
                        .setTitle("Title")
                        .addAction(action));
    }

    @Test
    public void equals() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template1).isEqualTo(template2);
    }

    @Test
    public void notEquals_differentMessage() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder("bar")
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentHeaderAction() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.APP_ICON)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentActions() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentActionStrip() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(new ActionStrip.Builder()
                                .addAction(Action.BACK)
                                .addAction(Action.APP_ICON)
                                .build())
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentTitle() {
        LongMessageTemplate template1 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle(mTitle)
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();
        LongMessageTemplate template2 =
                new LongMessageTemplate.Builder(mMessage)
                        .setTitle("yo")
                        .setHeaderAction(Action.BACK)
                        .addAction(mAction)
                        .setActionStrip(mActionStrip)
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }
}
