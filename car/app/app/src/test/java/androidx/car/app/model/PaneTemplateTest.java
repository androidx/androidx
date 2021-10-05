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

import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PaneTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PaneTemplateTest {

    @Test
    public void pane_moreThanMaxActions_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaneTemplate.Builder(TestUtils.createPane(2, 3)).setTitle(
                        "Title").build());

        // Positive cases.
        new PaneTemplate.Builder(TestUtils.createPane(2, 2)).setTitle("Title").build();
    }

    @Test
    public void pane_action_unsupportedSpans_throws() {
        CharSequence title1 = TestUtils.getCharSequenceWithClickableSpan("Title");
        Action action1 = new Action.Builder().setTitle(title1).build();
        Pane pane1 = getPane().addAction(action1).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaneTemplate.Builder(pane1).setTitle("Title").build());

        CarText title2 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        Action action2 = new Action.Builder().setTitle(title2).build();
        Pane pane2 = getPane().addAction(action2).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaneTemplate.Builder(pane2).setTitle("Title").build());

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithColorSpan("Title");
        Action action3 = new Action.Builder().setTitle(title3).build();
        Pane pane3 = getPane().addAction(action3).build();
        new PaneTemplate.Builder(pane3).setTitle("Title").build();

        CarText title4 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        Action action4 = new Action.Builder().setTitle(title4).build();
        Pane pane4 = getPane().addAction(action4).build();
        new PaneTemplate.Builder(pane4).setTitle("Title").build();
    }

    @Test
    public void pane_moreThanMaxTexts_throws() {
        Row rowExceedsMaxTexts =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PaneTemplate.Builder(
                                new Pane.Builder().addRow(rowExceedsMaxTexts).build())
                                .setTitle("Title")
                                .build());

        // Positive cases.
        new PaneTemplate.Builder(new Pane.Builder().addRow(rowMeetingMaxTexts).build())
                .setTitle("Title")
                .build();
    }

    @Test
    public void pane_moreThanMaxPrimaryButtons_throws() {
        Action primaryAction = new Action.Builder().setTitle("primaryAction")
                                       .setOnClickListener(() -> {})
                                       .setFlags(FLAG_PRIMARY).build();
        Row rowMeetingMaxTexts =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").build();

        Pane paneExceedsMaxPrimaryAction =
                new Pane.Builder()
                        .addAction(primaryAction)
                        .addAction(primaryAction)
                        .addRow(rowMeetingMaxTexts)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaneTemplate.Builder(paneExceedsMaxPrimaryAction)
                              .setTitle("Title")
                              .build());
    }

    @Test
    public void pane_toggleOrClickListener_throws() {
        Row rowWithToggle =
                new Row.Builder().setTitle("Title").setToggle(new Toggle.Builder(isChecked -> {
                }).build()).build();
        Row rowWithClickListener = new Row.Builder().setTitle("Title").setOnClickListener(() -> {
        }).build();
        Row rowMeetingRestrictions =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PaneTemplate.Builder(new Pane.Builder().addRow(rowWithToggle).build())
                                .setTitle("Title")
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PaneTemplate.Builder(
                                new Pane.Builder().addRow(rowWithClickListener).build())
                                .setTitle("Title")
                                .build());

        // Positive cases.
        new PaneTemplate.Builder(new Pane.Builder().addRow(rowMeetingRestrictions).build())
                .setTitle("Title")
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(IllegalStateException.class,
                () -> new PaneTemplate.Builder(getPane().build()).build());

        // Positive cases.
        new PaneTemplate.Builder(getPane().build()).setTitle("Title").build();
        new PaneTemplate.Builder(getPane().build()).setHeaderAction(Action.BACK).build();
    }

    @Test
    public void createInstance_header_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaneTemplate.Builder(getPane().build()).setTitle(title));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new PaneTemplate.Builder(getPane().build()).setTitle(title2).build();
    }

    @Test
    public void createInstance_setPane() {
        Pane pane = getPane().build();
        PaneTemplate template = new PaneTemplate.Builder(pane).setTitle("Title").build();
        assertThat(template.getPane()).isEqualTo(pane);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PaneTemplate.Builder(getPane().build())
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PaneTemplate template = new PaneTemplate.Builder(getPane().build()).setHeaderAction(
                Action.BACK).build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PaneTemplate template =
                new PaneTemplate.Builder(getPane().build()).setTitle("Title").setActionStrip(
                        actionStrip).build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void equals() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                new PaneTemplate.Builder(pane)
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(actionStrip)
                        .setTitle(title)
                        .build();

        assertThat(template)
                .isEqualTo(
                        new PaneTemplate.Builder(pane)
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentPane() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                new PaneTemplate.Builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        new PaneTemplate.Builder(
                                new Pane.Builder().addRow(
                                        new Row.Builder().setTitle("Title2").build()).build())
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();

        PaneTemplate template = new PaneTemplate.Builder(pane).setHeaderAction(Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(new PaneTemplate.Builder(pane).setHeaderAction(
                        Action.APP_ICON).build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                new PaneTemplate.Builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        new PaneTemplate.Builder(pane)
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                new PaneTemplate.Builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        new PaneTemplate.Builder(pane).setActionStrip(actionStrip).setTitle(
                                "bar").build());
    }

    private static Pane.Builder getPane() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        Row row2 = new Row.Builder().setTitle("Oranges").build();
        return new Pane.Builder().addRow(row1).addRow(row2);
    }
}
