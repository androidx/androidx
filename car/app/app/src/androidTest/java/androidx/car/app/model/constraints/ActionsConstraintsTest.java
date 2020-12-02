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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/** Tests for {@link ActionsConstraints}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ActionsConstraintsTest {
    @Test
    public void createEmpty() {
        ActionsConstraints constraints = ActionsConstraints.builder().build();

        assertThat(constraints.getMaxActions()).isEqualTo(Integer.MAX_VALUE);
        assertThat(constraints.getRequiredActionTypes()).isEmpty();
    }

    @Test
    public void create_requiredExceedsMaxAllowedActions() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ActionsConstraints.builder()
                                .setMaxActions(1)
                                .addRequiredActionType(Action.TYPE_BACK)
                                .addRequiredActionType(Action.TYPE_CUSTOM)
                                .build());
    }

    @Test
    public void create_requiredAlsoDisallowed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ActionsConstraints.builder()
                                .addRequiredActionType(Action.TYPE_BACK)
                                .addDisallowedActionType(Action.TYPE_BACK)
                                .build());
    }

    @Test
    public void createConstraints() {
        ActionsConstraints constraints =
                ActionsConstraints.builder()
                        .setMaxActions(2)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build();

        assertThat(constraints.getMaxActions()).isEqualTo(2);
        assertThat(constraints.getRequiredActionTypes()).containsExactly(Action.TYPE_CUSTOM);
        assertThat(constraints.getDisallowedActionTypes()).containsExactly(Action.TYPE_BACK);
    }

    @Test
    public void validateActions() {
        ActionsConstraints constraints =
                ActionsConstraints.builder()
                        .setMaxActions(2)
                        .setMaxCustomTitles(1)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build();

        CarIcon carIcon =
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));
        Action actionWithIcon = TestUtils.createAction(null, carIcon);
        Action actionWithTitle = TestUtils.createAction("Title", carIcon);

        // Positive case: instance that fits the 2-max-actions, only-1-has-title constraint.
        constraints.validateOrThrow(
                ActionStrip.builder()
                        .addAction(actionWithIcon)
                        .addAction(actionWithTitle)
                        .build()
                        .getActions());
        // Positive case: empty list is okay when there are no required types
        ActionsConstraints.builder().setMaxActions(2).build().validateOrThrow(
                Collections.emptyList());

        // Missing required type.
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                ActionStrip.builder().addAction(
                                        Action.APP_ICON).build().getActions()));

        // Disallowed type
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                ActionStrip.builder().addAction(Action.BACK).build().getActions()));

        // Over max allowed actions
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                ActionStrip.builder()
                                        .addAction(Action.APP_ICON)
                                        .addAction(actionWithIcon)
                                        .addAction(actionWithTitle)
                                        .build()
                                        .getActions()));

        // Over max allowed actions with title
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        constraints.validateOrThrow(
                                ActionStrip.builder()
                                        .addAction(actionWithTitle)
                                        .addAction(actionWithTitle)
                                        .build()
                                        .getActions()));
    }
}
