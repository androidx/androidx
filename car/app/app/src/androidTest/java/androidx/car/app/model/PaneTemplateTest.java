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

import android.text.SpannableString;

import androidx.car.app.TestUtils;
import androidx.car.app.test.R;
import androidx.car.app.utils.Logger;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PaneTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PaneTemplateTest {

    @Test
    public void pane_moreThanMaxActions_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PaneTemplate.builder(TestUtils.createPane(2, 3)).setTitle("Title").build());

        // Positive cases.
        PaneTemplate.builder(TestUtils.createPane(2, 2)).setTitle("Title").build();
    }

    @Test
    public void pane_moreThanMaxTexts_throws() {
        Row rowExceedsMaxTexts =
                Row.builder().setTitle("Title").addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                Row.builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaneTemplate.builder(Pane.builder().addRow(rowExceedsMaxTexts).build())
                                .setTitle("Title")
                                .build());

        // Positive cases.
        PaneTemplate.builder(Pane.builder().addRow(rowMeetingMaxTexts).build())
                .setTitle("Title")
                .build();
    }

    @Test
    public void pane_toggleOrClickListener_throws() {
        Row rowWithToggle =
                Row.builder().setTitle("Title").setToggle(Toggle.builder(isChecked -> {
                }).build()).build();
        Row rowWithClickListener = Row.builder().setTitle("Title").setOnClickListener(() -> {
        }).build();
        Row rowMeetingRestrictions =
                Row.builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaneTemplate.builder(Pane.builder().addRow(rowWithToggle).build())
                                .setTitle("Title")
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaneTemplate.builder(Pane.builder().addRow(rowWithClickListener).build())
                                .setTitle("Title")
                                .build());

        // Positive cases.
        PaneTemplate.builder(Pane.builder().addRow(rowMeetingRestrictions).build())
                .setTitle("Title")
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(IllegalStateException.class, () -> PaneTemplate.builder(getPane()).build());

        // Positive cases.
        PaneTemplate.builder(getPane()).setTitle("Title").build();
        PaneTemplate.builder(getPane()).setHeaderAction(Action.BACK).build();
    }

    @Test
    public void createInstance_setPane() {
        Pane pane = getPane();
        PaneTemplate template = PaneTemplate.builder(pane).setTitle("Title").build();
        assertThat(template.getPane()).isEqualTo(pane);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaneTemplate.builder(getPane())
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PaneTemplate template = PaneTemplate.builder(getPane()).setHeaderAction(
                Action.BACK).build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        PaneTemplate template =
                PaneTemplate.builder(getPane()).setTitle("Title").setActionStrip(
                        actionStrip).build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void validate_isRefresh() {
        Logger logger = message -> {
        };
        Row.Builder row = Row.builder().setTitle("Row1");
        PaneTemplate template =
                PaneTemplate.builder(Pane.builder().addRow(row.build()).build()).setTitle(
                        "Title").build();

        assertThat(template.isRefresh(template, logger)).isTrue();

        // Going from loading state to new content is allowed.
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(Pane.builder().setLoading(true).build())
                                .setTitle("Title")
                                .build(),
                        logger))
                .isTrue();

        // Other allowed mutable states.
        SpannableString stringWithSpan = new SpannableString("Row1");
        stringWithSpan.setSpan(DurationSpan.create(1), 0, /* end= */ 1, /* flags= */ 0);
        IconCompat icon = IconCompat.createWithResource(
                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1);
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(
                                Pane.builder()
                                        .addRow(
                                                row.setImage(CarIcon.of(icon))
                                                        .setTitle(stringWithSpan)
                                                        .build())
                                        .build())
                                .setTitle("Title")
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build(),
                        logger))
                .isTrue();

        // Title updates are disallowed.
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(Pane.builder().addRow(row.build()).build())
                                .setTitle("Title2")
                                .build(),
                        logger))
                .isFalse();

        // Text updates are disallowed.
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(
                                Pane.builder().addRow(row.setTitle("Row2").build()).build())
                                .setTitle("Title")
                                .build(),
                        logger))
                .isFalse();
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(
                                Pane.builder().addRow(row.addText("Text").build()).build())
                                .setTitle("Title")
                                .build(),
                        logger))
                .isFalse();

        // Additional rows are disallowed.
        assertThat(
                template.isRefresh(
                        PaneTemplate.builder(Pane.builder().addRow(row.build()).addRow(
                                row.build()).build())
                                .setTitle("Title")
                                .build(),
                        logger))
                .isFalse();

        // Going from content to loading state is disallowed.
        assertThat(
                PaneTemplate.builder(Pane.builder().setLoading(true).build())
                        .setTitle("Title")
                        .build()
                        .isRefresh(template, logger))
                .isFalse();
    }

    @Test
    public void equals() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                PaneTemplate.builder(pane)
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(actionStrip)
                        .setTitle(title)
                        .build();

        assertThat(template)
                .isEqualTo(
                        PaneTemplate.builder(pane)
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentPane() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                PaneTemplate.builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        PaneTemplate.builder(
                                Pane.builder().addRow(
                                        Row.builder().setTitle("Title2").build()).build())
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();

        PaneTemplate template = PaneTemplate.builder(pane).setHeaderAction(Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(PaneTemplate.builder(pane).setHeaderAction(Action.APP_ICON).build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                PaneTemplate.builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        PaneTemplate.builder(pane)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "foo";

        PaneTemplate template =
                PaneTemplate.builder(pane).setActionStrip(actionStrip).setTitle(title).build();

        assertThat(template)
                .isNotEqualTo(
                        PaneTemplate.builder(pane).setActionStrip(actionStrip).setTitle(
                                "bar").build());
    }

    private static Pane getPane() {
        Row row1 = Row.builder().setTitle("Bananas").build();
        Row row2 = Row.builder().setTitle("Oranges").build();
        return Pane.builder().addRow(row1).addRow(row2).build();
    }
}
