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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.List;

/** Tests for {@link Pane}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PaneTest {
    @Test
    public void createEmptyRows_throws() {
        assertThrows(IllegalStateException.class, () -> new Pane.Builder().build());

        // Positive case
        new Pane.Builder().setLoading(true).build();
    }

    @Test
    public void isLoading_withRows_throws() {
        Row row = createRow(1);
        assertThrows(
                IllegalStateException.class, () -> new Pane.Builder().addRow(row).setLoading(
                        true).build());

        // Positive case
        new Pane.Builder().addRow(row).build();
    }

    @Test
    public void addRow() {
        Row row = createRow(1);
        Pane pane = new Pane.Builder().addRow(row).build();
        assertThat(pane.getRows()).containsExactly(row);
    }

    @Test
    public void addRow_multiple() {
        Row row1 = createRow(1);
        Row row2 = createRow(2);
        Row row3 = createRow(3);
        Pane pane = new Pane.Builder().addRow(row1).addRow(row2).addRow(row3).build();
        assertThat(pane.getRows()).containsExactly(row1, row2, row3);
    }

    @Test
    public void setActionList() {
        Action action1 = createAction(1);
        Action action2 = createAction(2);
        List<Action> actions = Arrays.asList(action1, action2);
        Pane pane =
                new Pane.Builder().addRow(
                        new Row.Builder().setTitle("Title").build()).addAction(action1).addAction(
                        action2).build();
        assertThat(pane.getActions()).containsExactlyElementsIn(actions);
    }

    @Test
    public void setActions_throwsIfNullAction() {
        assertThrows(
                NullPointerException.class,
                () -> new Pane.Builder().addAction(null).build());
    }

    @Test
    public void setImage() {
        Pane pane =
                new Pane.Builder()
                        .addRow(new Row.Builder().setTitle("Title").build())
                        .setImage(CarIcon.APP_ICON)
                        .build();
        assertThat(pane.getImage()).isEqualTo(CarIcon.APP_ICON);
    }

    @Test
    public void setImage_throwsIfNull() {
        assertThrows(
                NullPointerException.class,
                () -> new Pane.Builder().setImage(null));
    }

    @Test
    public void equals() {
        Pane pane =
                new Pane.Builder()
                        .setLoading(false)
                        .addAction(Action.APP_ICON)
                        .addAction(Action.BACK)
                        .addRow(new Row.Builder().setTitle("Title").build())
                        .setImage(CarIcon.APP_ICON)
                        .build();

        assertThat(pane)
                .isEqualTo(
                        new Pane.Builder()
                                .setLoading(false)
                                .addAction(Action.APP_ICON)
                                .addAction(Action.BACK)
                                .addRow(new Row.Builder().setTitle("Title").build())
                                .setImage(CarIcon.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentLoading() {
        Pane pane =
                new Pane.Builder().setLoading(false).addRow(
                        new Row.Builder().setTitle("Title").build()).build();

        assertThat(pane).isNotEqualTo(new Pane.Builder().setLoading(true).build());
    }

    @Test
    public void notEquals_differentActionsAdded() {
        Row row = new Row.Builder().setTitle("Title").build();
        Pane pane =
                new Pane.Builder()
                        .addRow(row)
                        .addAction(Action.APP_ICON)
                        .addAction(Action.BACK)
                        .build();

        assertThat(pane)
                .isNotEqualTo(
                        new Pane.Builder().addRow(row).addAction(Action.APP_ICON).build());
    }

    @Test
    public void notEquals_differentRow() {
        Pane pane = new Pane.Builder().addRow(new Row.Builder().setTitle("Title").build()).build();

        assertThat(pane)
                .isNotEqualTo(
                        new Pane.Builder()
                                .addRow(new Row.Builder().setTitle("Title").setOnClickListener(
                                        () -> {
                                        }).build())
                                .build());
    }

    @Test
    public void notEquals_differentImage() {
        Row row = new Row.Builder().setTitle("Title").build();
        Pane pane =
                new Pane.Builder()
                        .addRow(row)
                        .setImage(CarIcon.APP_ICON)
                        .build();

        assertThat(pane)
                .isNotEqualTo(
                        new Pane.Builder().addRow(row).setImage(CarIcon.BACK).build());
    }

    private static Row createRow(int suffix) {
        return new Row.Builder().setTitle("The title " + suffix).addText(
                "The subtitle " + suffix).build();
    }

    private static Action createAction(int suffix) {
        return new Action.Builder().setTitle("Action " + suffix).setOnClickListener(() -> {
        }).build();
    }
}
