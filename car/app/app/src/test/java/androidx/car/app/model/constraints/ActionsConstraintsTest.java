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
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.Collections;

/** Tests for {@link ActionsConstraints}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ActionsConstraintsTest {
    @Test
    public void createEmpty() {
        ActionsConstraints constraints = new ActionsConstraints.Builder().build();

        assertThat(constraints.getMaxActions()).isEqualTo(Integer.MAX_VALUE);
        assertThat(constraints.getRequiredActionTypes()).isEmpty();
    }

    @Test
    public void create_requiredExceedsMaxAllowedActions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .setMaxActions(1)
                        .addRequiredActionType(Action.TYPE_BACK)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .build());
    }

    @Test
    public void create_requiredAlsoDisallowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .addRequiredActionType(Action.TYPE_BACK)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build());
    }

    @Test
    public void create_allowedAlsoDisallowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .addAllowedActionType(Action.TYPE_BACK)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build());
    }

    @Test
    public void create_allowedAndDisallowedSet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionsConstraints.Builder()
                        .addAllowedActionType(Action.TYPE_PAN)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .build());
    }

    @Test
    public void createConstraints() {
        ActionsConstraints constraints =
                new ActionsConstraints.Builder()
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
                new ActionsConstraints.Builder()
                        .setMaxActions(2)
                        .setMaxCustomTitles(1)
                        .addRequiredActionType(Action.TYPE_CUSTOM)
                        .addDisallowedActionType(Action.TYPE_BACK)
                        .setOnClickListenerAllowed(true)
                        .build();

        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action actionWithIcon = TestUtils.createAction(null, carIcon);
        Action actionWithTitle = TestUtils.createAction("Title", carIcon);

        // Positive case: instance that fits the 2-max-actions, only-1-has-title constraint.
        constraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(actionWithIcon)
                        .addAction(actionWithTitle)
                        .build()
                        .getActions());
        // Positive case: empty list is okay when there are no required types
        new ActionsConstraints.Builder().setMaxActions(2).build().validateOrThrow(
                Collections.emptyList());

        // Missing required type.
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder().addAction(
                                Action.APP_ICON).build().getActions()));

        // Disallowed type
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder().addAction(Action.BACK).build().getActions()));

        // Over max allowed actions
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(Action.APP_ICON)
                                .addAction(actionWithIcon)
                                .addAction(actionWithTitle)
                                .build()
                                .getActions()));

        // Over max allowed actions with title
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(actionWithTitle)
                                .addAction(actionWithTitle)
                                .build()
                                .getActions()));

        ActionsConstraints constraintsNoOnClick =
                new ActionsConstraints.Builder().setOnClickListenerAllowed(false).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> constraintsNoOnClick.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(actionWithIcon)
                                .build()
                                .getActions()));

        // Positive case: OnClickListener disallowed only for custom action types and passes for
        // standard action types like the back action.
        constraintsNoOnClick.validateOrThrow(
                new ActionStrip.Builder().addAction(Action.BACK).build().getActions());

        ActionsConstraints constraintsAllowPan =
                new ActionsConstraints.Builder().addAllowedActionType(Action.TYPE_PAN).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> constraintsAllowPan.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(Action.BACK)
                                .build()
                                .getActions()));
        //Positive case: Only allows pan action types
        constraintsAllowPan.validateOrThrow(
                new ActionStrip.Builder().addAction(Action.PAN).build().getActions());

        // Background color
        ActionsConstraints constraintsRequireBackgroundColor =
                new ActionsConstraints.Builder()
                        .setRequireActionIcons(true)
                        .setRequireActionBackgroundColor(true)
                        .setOnClickListenerAllowed(true)
                        .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> constraintsRequireBackgroundColor.validateOrThrow(
                        Arrays.asList(actionWithIcon)));

        // Positive case: Custom icon with background color
        Action actionWithBackgroundColor = TestUtils.createAction(carIcon, CarColor.BLUE);
        constraintsRequireBackgroundColor.validateOrThrow(Arrays.asList(actionWithBackgroundColor));

        // Positive case: Standard icon
        constraintsRequireBackgroundColor.validateOrThrow(
                new ActionStrip.Builder().addAction(Action.APP_ICON).build().getActions());
    }

    @Test
    public void validateNavigationActionConstraints() {
        // same constraints with ACTIONS_CONSTRAINTS_NAVIGATION
        ActionsConstraints navigationConstraints =
                new ActionsConstraints.Builder()
                        .setMaxActions(4)
                        .setMaxPrimaryActions(1)
                        .setRestrictBackgroundColorToPrimaryAction(true)
                        .setMaxCustomTitles(4)
                        .setTitleTextConstraints(CarTextConstraints.TEXT_AND_ICON)
                        .setOnClickListenerAllowed(true)
                        .build();

        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action action1 = TestUtils.createAction("Title1", carIcon);
        Action action2 = TestUtils.createAction("Title2", carIcon);
        Action action3 = TestUtils.createAction("Title3", carIcon);
        Action action4 = TestUtils.createAction("Title4", carIcon);
        Action action5 = TestUtils.createAction("Title5", carIcon);
        Action actionWithBackgroundColor =
                new Action.Builder(action1).setBackgroundColor(CarColor.BLUE).build();
        Action primaryAction1 = new Action.Builder(action1).setFlags(Action.FLAG_PRIMARY).build();
        Action primaryAction2 = new Action.Builder(primaryAction1).build();
        Action primaryActionWithBackgroundColor =
                new Action.Builder(primaryAction1).setBackgroundColor(CarColor.BLUE).build();

        // Positive case: instance that fits 4 max actions, both can have title and icon
        navigationConstraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(action1)
                        .addAction(action2)
                        .addAction(action3)
                        .addAction(action4)
                        .build()
                        .getActions());

        // Over Max Allowed Actions
        assertThrows(
                IllegalArgumentException.class,
                () -> navigationConstraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(action1)
                                .addAction(action2)
                                .addAction(action3)
                                .addAction(action4)
                                .addAction(action5)
                                .build()
                                .getActions()));

        // Negative case: Multiple primary actions
        assertThrows(
                IllegalArgumentException.class,
                () -> navigationConstraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(action1)
                                .addAction(action2)
                                .addAction(primaryAction1)
                                .addAction(primaryAction2)
                                .build()
                                .getActions()));

        // Negative case: Background color on non-primary action
        assertThrows(
                IllegalArgumentException.class,
                () -> navigationConstraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(actionWithBackgroundColor)
                                .build()
                                .getActions()));

        // Positive case: Background color on primary action
        navigationConstraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(primaryActionWithBackgroundColor)
                        .build()
                        .getActions());

    }

    @Test
    public void validateOrThrow_whenRestrictBackgroundColorToPrimaryAction() {
        ActionsConstraints constraints =
                new ActionsConstraints.Builder()
                        .setMaxCustomTitles(1)
                        .setMaxActions(1)
                        .setMaxPrimaryActions(1)
                        .setRestrictBackgroundColorToPrimaryAction(true)
                        .build();
        Action actionWithBackground =
                new Action.Builder().setTitle("Test title").setBackgroundColor(
                        CarColor.BLUE).build();
        Action primaryActionWithBackground =
                new Action.Builder(actionWithBackground).setFlags(Action.FLAG_PRIMARY).build();
        // Negative case: Background color on non-primary action
        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(
                        new ActionStrip.Builder()
                                .addAction(actionWithBackground)
                                .build()
                                .getActions()));
        // Positive case: Background color on primary action
        constraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(primaryActionWithBackground)
                        .build()
                        .getActions());
    }

    @Test
    public void validateOrThrow_requireActionBackgroundColor() {
        ActionsConstraints constraints =
                new ActionsConstraints.Builder()
                        .setMaxCustomTitles(1)
                        .setMaxActions(1)
                        .setMaxPrimaryActions(1)
                        .setRequireActionBackgroundColor(true)
                        .setRestrictBackgroundColorToPrimaryAction(true)
                        .build();
        Action actionWithBackground =
                new Action.Builder().setTitle("Test title").setBackgroundColor(
                        CarColor.BLUE).build();
        // Positive case: Background color on non-primary action
        constraints.validateOrThrow(
                new ActionStrip.Builder()
                        .addAction(actionWithBackground)
                        .build()
                        .getActions());
    }
}
