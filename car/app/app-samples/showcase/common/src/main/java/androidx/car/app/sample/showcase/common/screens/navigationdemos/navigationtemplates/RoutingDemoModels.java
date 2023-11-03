/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.navigationdemos.navigationtemplates;

import static androidx.car.app.model.Action.FLAG_DEFAULT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_STRAIGHT;

import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Alert;
import androidx.car.app.model.AlertCallback;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarIconSpan;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** A class that provides models for the routing demos. */
public abstract class RoutingDemoModels {

    private RoutingDemoModels() {
    }

    /** Returns a sample {@link Alert}. */
    @NonNull
    private static Alert createAlert(@NonNull CarContext carContext) {
        CarText title =
                CarText.create(carContext.getString(R.string.navigation_alert_title));
        CarText subtitle =
                CarText.create(carContext.getString(R.string.navigation_alert_subtitle));
        CarIcon icon = CarIcon.ALERT;

        CarText yesTitle = CarText.create(carContext.getString(R.string.yes_action_title));
        Action yesAction = new Action.Builder().setTitle(yesTitle).setOnClickListener(
                () -> CarToast.makeText(
                                carContext,
                                carContext.getString(
                                        R.string.yes_action_toast_msg),
                                CarToast.LENGTH_SHORT)
                        .show()).setFlags(FLAG_PRIMARY).build();

        CarText noTitle = CarText.create(carContext.getString(R.string.no_action_title));
        Action noAction = new Action.Builder().setTitle(noTitle).setOnClickListener(
                () -> CarToast.makeText(
                                carContext,
                                carContext.getString(
                                        R.string.no_action_toast_msg),
                                CarToast.LENGTH_SHORT)
                        .show()).setFlags(FLAG_DEFAULT).build();

        return new Alert.Builder(/* alertId: */ 0, title, /* durationMillis: */ 10000)
                .setSubtitle(subtitle)
                .setIcon(icon)
                .addAction(yesAction)
                .addAction(noAction).setCallback(new AlertCallback() {
                    @Override
                    public void onCancel(int reason) {
                        if (reason == AlertCallback.REASON_TIMEOUT) {
                            CarToast.makeText(
                                            carContext,
                                            carContext.getString(
                                                    R.string.alert_timeout_toast_msg),
                                            CarToast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onDismiss() {
                    }
                }).build();
    }

    /** Returns the current {@link Step} with information such as the cue text and images. */
    @NonNull
    public static Step getCurrentStep(@NonNull CarContext carContext) {
        // Create the cue text, and span the "520" text with a highway sign image.
        String currentStepCue = carContext.getString(R.string.current_step_cue);
        SpannableString currentStepCueWithImage = new SpannableString(currentStepCue);
        CarIconSpan highwaySign =
                CarIconSpan.create(
                        new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        carContext, R.drawable.ic_520))
                                .build(),
                        CarIconSpan.ALIGN_CENTER);
        currentStepCueWithImage.setSpan(highwaySign, 7, 10, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        CarIcon currentTurnIcon =
                new CarIcon.Builder(
                        IconCompat.createWithResource(
                                carContext, R.drawable.arrow_right_turn))
                        .build();
        Maneuver currentManeuver =
                new Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT)
                        .setIcon(currentTurnIcon)
                        .build();

        CarIcon lanesImage =
                new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.lanes))
                        .build();

        Lane straightNormal =
                new Lane.Builder()
                        .addDirection(LaneDirection.create(SHAPE_STRAIGHT, false))
                        .build();
        Lane rightHighlighted =
                new Lane.Builder()
                        .addDirection(LaneDirection.create(SHAPE_NORMAL_RIGHT, true))
                        .build();

        return new Step.Builder(currentStepCueWithImage)
                .setManeuver(currentManeuver)
                .setLanesImage(lanesImage)
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(rightHighlighted)
                .build();
    }

    /** Returns the next {@link Step} with information such as the cue text and images. */
    @NonNull
    public static Step getNextStep(@NonNull CarContext carContext) {
        // Create the cue text, and span the "I5" text with an image.
        String nextStepCue = carContext.getString(R.string.next_step_cue);
        SpannableString nextStepCueWithImage = new SpannableString(nextStepCue);
        CarIconSpan highwaySign =
                CarIconSpan.create(
                        new CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_i5))
                                .build(),
                        CarIconSpan.ALIGN_CENTER);
        nextStepCueWithImage.setSpan(highwaySign, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        CarIcon nextTurnIcon =
                new CarIcon.Builder(
                        IconCompat.createWithResource(
                                carContext, R.drawable.arrow_straight))
                        .build();
        Maneuver nextManeuver =
                new Maneuver.Builder(Maneuver.TYPE_STRAIGHT).setIcon(nextTurnIcon).build();

        return new Step.Builder(nextStepCueWithImage).setManeuver(nextManeuver).build();
    }

    /**
     * Returns the action strip that contains a "bug report" button and "stop navigation" button.
     */
    @NonNull
    public static ActionStrip getActionStrip(
            @NonNull CarContext carContext, @NonNull OnClickListener onStopNavigation) {
        ActionStrip.Builder builder = new ActionStrip.Builder();
        if (carContext.getCarAppApiLevel() >= CarAppApiLevels.LEVEL_5) {
            @ColorInt int actionButtonRed = 0xffb40404;
            builder.addAction(
                    new Action.Builder()
                            .setFlags(FLAG_PRIMARY)
                            .setBackgroundColor(
                                    CarColor.createCustom(actionButtonRed, actionButtonRed))
                            .setOnClickListener(
                                    () -> carContext.getCarService(AppManager.class)
                                            .showAlert(createAlert(carContext)))
                            .setIcon(new CarIcon.Builder(
                                    IconCompat.createWithResource(
                                            carContext,
                                            R.drawable.ic_baseline_add_alert_24))
                                    .build())
                            .build());
        }
        builder.addAction(
                new Action.Builder()
                        .setOnClickListener(
                                () -> CarToast.makeText(
                                                carContext,
                                                carContext.getString(
                                                        R.string.bug_reported_toast_msg),
                                                CarToast.LENGTH_SHORT)
                                        .show())
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                carContext,
                                                R.drawable.ic_bug_report_24px))
                                        .build())
                        .build());
        builder.addAction(
                new Action.Builder()
                        .setTitle(carContext.getString(R.string.stop_action_title))
                        .setOnClickListener(onStopNavigation)
                        .setFlags(Action.FLAG_IS_PERSISTENT)
                        .build());
        return builder.build();
    }

    /**
     * Returns the map action strip that contains pan and zoom buttons.
     */
    @NonNull
    public static ActionStrip getMapActionStrip(
            @NonNull CarContext carContext) {
        return new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        carContext,
                                                        carContext.getString(
                                                                R.string.zoomed_in_toast_msg),
                                                        CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        carContext,
                                                        R.drawable.ic_zoom_in_24))
                                                .build())
                                .build())
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        carContext,
                                                        carContext.getString(
                                                                R.string.zoomed_out_toast_msg),
                                                        CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        carContext,
                                                        R.drawable.ic_zoom_out_24))
                                                .build())
                                .build())
                .addAction(Action.PAN)
                .build();
    }

    /** Returns the {@link TravelEstimate} with time and distance information. */
    @NonNull
    public static TravelEstimate getTravelEstimate(@NonNull CarContext carContext) {
        // Calculate the time to destination from the current time.
        long nowUtcMillis = System.currentTimeMillis();
        long timeToDestinationMillis = TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(55);

        return new TravelEstimate.Builder(
                // The estimated distance to the destination.
                Distance.create(112, Distance.UNIT_KILOMETERS),

                // Arrival time at the destination with the destination time zone.
                DateTimeWithZone.create(
                        nowUtcMillis + timeToDestinationMillis,
                        TimeZone.getTimeZone("US/Eastern")))
                .setRemainingTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(timeToDestinationMillis))
                .setRemainingTimeColor(CarColor.YELLOW)
                .setRemainingDistanceColor(CarColor.RED)
                .setTripText(CarText.create(carContext.getString(R.string.travel_est_trip_text)))
                .setTripIcon(new CarIcon.Builder(
                        IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_face_24px))
                        .build())
                .build();
    }
}
