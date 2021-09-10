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

package androidx.car.app.model.constraints;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RowConstraints}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RowConstraintsTest {
    @Test
    public void validate_clickListener() {
        RowConstraints constraints = new RowConstraints.Builder().setOnClickListenerAllowed(
                false).build();
        RowConstraints allowConstraints =
                new RowConstraints.Builder().setOnClickListenerAllowed(true).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new Row.Builder().setTitle("Title)").setOnClickListener(() -> {
                        }).build()));

        // Positive cases
        constraints.validateOrThrow(new Row.Builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(
                new Row.Builder().setTitle("Title").setOnClickListener(() -> {
                }).build());
    }

    @Test
    public void validate_toggle() {
        RowConstraints constraints = new RowConstraints.Builder().setToggleAllowed(false).build();
        RowConstraints allowConstraints = new RowConstraints.Builder().setToggleAllowed(
                true).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new Row.Builder()
                                .setTitle("Title)")
                                .setToggle(new Toggle.Builder(isChecked -> {
                                }).build())
                                .build()));

        // Positive cases
        constraints.validateOrThrow(new Row.Builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(
                new Row.Builder().setTitle("Title").setToggle(new Toggle.Builder(isChecked -> {
                }).build()).build());
    }

    @Test
    public void validate_images() {
        RowConstraints constraints = new RowConstraints.Builder().setImageAllowed(false).build();
        RowConstraints allowConstraints = new RowConstraints.Builder().setImageAllowed(
                true).build();
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");

        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new Row.Builder().setTitle("Title)").setImage(carIcon).build()));

        // Positive cases
        constraints.validateOrThrow(new Row.Builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(new Row.Builder().setTitle("Title").setImage(
                carIcon).build());
    }

    @Test
    public void validate_texts() {
        RowConstraints constraints = new RowConstraints.Builder().setMaxTextLinesPerRow(2).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new Row.Builder()
                                .setTitle("Title)")
                                .addText("text1")
                                .addText("text2")
                                .addText("text3")
                                .build()));

        // Positive cases
        constraints.validateOrThrow(
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").build());
    }
}
