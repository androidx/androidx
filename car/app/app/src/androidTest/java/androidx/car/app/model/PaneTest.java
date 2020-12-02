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
