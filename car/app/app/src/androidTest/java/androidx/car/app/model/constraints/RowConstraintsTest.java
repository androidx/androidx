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

import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link RowConstraints}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RowConstraintsTest {
    @Test
    public void validate_clickListener() {
        RowConstraints constraints = RowConstraints.builder().setOnClickListenerAllowed(
                false).build();
        RowConstraints allowConstraints =
                RowConstraints.builder().setOnClickListenerAllowed(true).build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                Row.builder().setTitle("Title)").setOnClickListener(() -> {
                                }).build()));

        // Positive cases
        constraints.validateOrThrow(Row.builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(
                Row.builder().setTitle("Title").setOnClickListener(() -> {
                }).build());
    }

    @Test
    public void validate_toggle() {
        RowConstraints constraints = RowConstraints.builder().setToggleAllowed(false).build();
        RowConstraints allowConstraints = RowConstraints.builder().setToggleAllowed(true).build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                Row.builder()
                                        .setTitle("Title)")
                                        .setToggle(Toggle.builder(isChecked -> {
                                        }).build())
                                        .build()));

        // Positive cases
        constraints.validateOrThrow(Row.builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(
                Row.builder().setTitle("Title").setToggle(Toggle.builder(isChecked -> {
                }).build()).build());
    }

    @Test
    public void validate_images() {
        RowConstraints constraints = RowConstraints.builder().setImageAllowed(false).build();
        RowConstraints allowConstraints = RowConstraints.builder().setImageAllowed(true).build();
        CarIcon carIcon =
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                Row.builder().setTitle("Title)").setImage(carIcon).build()));

        // Positive cases
        constraints.validateOrThrow(Row.builder().setTitle("Title").build());
        allowConstraints.validateOrThrow(Row.builder().setTitle("Title").setImage(carIcon).build());
    }

    @Test
    public void validate_texts() {
        RowConstraints constraints = RowConstraints.builder().setMaxTextLinesPerRow(2).build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                Row.builder()
                                        .setTitle("Title)")
                                        .addText("text1")
                                        .addText("text2")
                                        .addText("text3")
                                        .build()));

        // Positive cases
        constraints.validateOrThrow(
                Row.builder().setTitle("Title").addText("text1").addText("text2").build());
    }
}
