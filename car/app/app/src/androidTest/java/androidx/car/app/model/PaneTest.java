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

import androidx.car.app.utils.Logger;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/** Tests for {@link Pane}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PaneTest {
    @Test
    public void createEmptyRows_throws() {
        assertThrows(IllegalStateException.class, () -> Pane.builder().build());

        // Positive case
        Pane.builder().setLoading(true).build();
    }

    @Test
    public void isLoading_withRows_throws() {
        Row row = createRow(1);
        assertThrows(
                IllegalStateException.class, () -> Pane.builder().addRow(row).setLoading(
                        true).build());

        // Positive case
        Pane.builder().addRow(row).build();
    }

    @Test
    public void addRow() {
        Row row = createRow(1);
        Pane pane = Pane.builder().addRow(row).build();
        assertThat(pane.getRows()).containsExactly(row);
    }

    @Test
    public void clearRows() {
        Row row = createRow(1);
        Pane pane = Pane.builder().addRow(row).addRow(row).clearRows().addRow(row).build();
        assertThat(pane.getRows()).hasSize(1);
    }

    @Test
    public void addRow_multiple() {
        Row row1 = createRow(1);
        Row row2 = createRow(2);
        Row row3 = createRow(3);
        Pane pane = Pane.builder().addRow(row1).addRow(row2).addRow(row3).build();
        assertThat(pane.getRows()).containsExactly(row1, row2, row3);
    }

    @Test
    public void setActions() {
        Action action1 = createAction(1);
        Action action2 = createAction(2);
        List<Action> actions = Arrays.asList(action1, action2);
        Pane pane =
                Pane.builder().addRow(Row.builder().setTitle("Title").build()).setActions(
                        actions).build();
        assertActions(pane.getActionList(), actions);
    }

    @Test
    public void setActions_throwsIfNullAction() {
        Action action1 = createAction(1);
        assertThrows(
                IllegalArgumentException.class,
                () -> Pane.builder().setActions(Arrays.asList(action1, null)).build());
    }

    @Test
    public void validate_isRefresh() {
        Logger logger = message -> {
        };
        Row.Builder row = Row.builder().setTitle("Title1");

        Pane.Builder builder = Pane.builder().setLoading(true);
        Pane pane = builder.build();
        assertThat(pane.isRefresh(builder.build(), logger)).isTrue();

        // Going from loading state to new content is allowed.
        Pane paneWithRows = Pane.builder().addRow(row.build()).build();
        assertThat(paneWithRows.isRefresh(pane, logger)).isTrue();

        // Text updates are disallowed.
        Pane paneWithDifferentTitle = Pane.builder().addRow(row.setTitle("Title2").build()).build();
        Pane paneWithDifferentText = Pane.builder().addRow(row.addText("Text").build()).build();
        assertThat(paneWithDifferentTitle.isRefresh(paneWithRows, logger)).isFalse();
        assertThat(paneWithDifferentText.isRefresh(paneWithRows, logger)).isFalse();

        // Additional rows are disallowed.
        Pane paneWithTwoRows = Pane.builder().addRow(row.build()).addRow(row.build()).build();
        assertThat(paneWithTwoRows.isRefresh(paneWithRows, logger)).isFalse();

        // Going from content to loading state is disallowed.
        assertThat(pane.isRefresh(paneWithRows, logger)).isFalse();
    }

    @Test
    public void validate_isRefresh_differentSpansAreIgnored() {
        Logger logger = message -> {
        };
        SpannableString textWithDistanceSpan = new SpannableString("Text");
        textWithDistanceSpan.setSpan(
                DistanceSpan.create(Distance.create(1000, Distance.UNIT_KILOMETERS)),
                /* start= */ 0,
                /* end= */ 1,
                /* flags= */ 0);
        SpannableString textWithDurationSpan = new SpannableString("Text");
        textWithDurationSpan.setSpan(DurationSpan.create(1), 0, /* end= */ 1, /* flags= */ 0);

        Pane pane1 =
                Pane.builder()
                        .addRow(
                                Row.builder().setTitle(textWithDistanceSpan).addText(
                                        textWithDurationSpan).build())
                        .build();
        Pane pane2 =
                Pane.builder()
                        .addRow(
                                Row.builder().setTitle(textWithDurationSpan).addText(
                                        textWithDistanceSpan).build())
                        .build();
        Pane pane3 =
                Pane.builder().addRow(Row.builder().setTitle("Text2").addText(
                        "Text2").build()).build();

        assertThat(pane2.isRefresh(pane1, logger)).isTrue();
        assertThat(pane3.isRefresh(pane1, logger)).isFalse();
    }

    @Test
    public void equals() {
        Pane pane =
                Pane.builder()
                        .setLoading(false)
                        .setActions(ImmutableList.of(Action.APP_ICON, Action.BACK))
                        .addRow(Row.builder().setTitle("Title").build())
                        .build();

        assertThat(pane)
                .isEqualTo(
                        Pane.builder()
                                .setLoading(false)
                                .setActions(ImmutableList.of(Action.APP_ICON, Action.BACK))
                                .addRow(Row.builder().setTitle("Title").build())
                                .build());
    }

    @Test
    public void notEquals_differentLoading() {
        Pane pane =
                Pane.builder().setLoading(false).addRow(
                        Row.builder().setTitle("Title").build()).build();

        assertThat(pane).isNotEqualTo(Pane.builder().setLoading(true).build());
    }

    @Test
    public void notEquals_differentActionsAdded() {
        Row row = Row.builder().setTitle("Title").build();
        Pane pane =
                Pane.builder()
                        .addRow(row)
                        .setActions(ImmutableList.of(Action.APP_ICON, Action.BACK))
                        .build();

        assertThat(pane)
                .isNotEqualTo(
                        Pane.builder().addRow(row).setActions(
                                ImmutableList.of(Action.APP_ICON)).build());
    }

    @Test
    public void notEquals_differentRow() {
        Pane pane = Pane.builder().addRow(Row.builder().setTitle("Title").build()).build();

        assertThat(pane)
                .isNotEqualTo(
                        Pane.builder()
                                .addRow(Row.builder().setTitle("Title").setOnClickListener(() -> {
                                }).build())
                                .build());
    }

    private static Row createRow(int suffix) {
        return Row.builder().setTitle("The title " + suffix).addText(
                "The subtitle " + suffix).build();
    }

    private static Action createAction(int suffix) {
        return Action.builder().setTitle("Action " + suffix).setOnClickListener(() -> {
        }).build();
    }

    private static void assertActions(Object obj, List<Action> expectedActions) {
        ActionList actionList = (ActionList) obj;
        assertThat(actionList.getList()).containsExactlyElementsIn(expectedActions);
    }
}
