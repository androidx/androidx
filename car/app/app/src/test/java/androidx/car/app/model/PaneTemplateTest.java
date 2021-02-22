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
                () -> new PaneTemplate.Builder(getPane()).build());

        // Positive cases.
        new PaneTemplate.Builder(getPane()).setTitle("Title").build();
        new PaneTemplate.Builder(getPane()).setHeaderAction(Action.BACK).build();
    }

    @Test
    public void createInstance_setPane() {
        Pane pane = getPane();
        PaneTemplate template = new PaneTemplate.Builder(pane).setTitle("Title").build();
        assertThat(template.getPane()).isEqualTo(pane);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PaneTemplate.Builder(getPane())
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PaneTemplate template = new PaneTemplate.Builder(getPane()).setHeaderAction(
                Action.BACK).build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PaneTemplate template =
                new PaneTemplate.Builder(getPane()).setTitle("Title").setActionStrip(
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

    private static Pane getPane() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        Row row2 = new Row.Builder().setTitle("Oranges").build();
        return new Pane.Builder().addRow(row1).addRow(row2).build();
    }
}
