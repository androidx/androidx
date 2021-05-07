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

package androidx.car.app.sample.showcase.common.navigation.routing;

import static androidx.car.app.navigation.model.LaneDirection.SHAPE_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_STRAIGHT;

import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarIconSpan;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** A class that provides models for the routing demos. */
public abstract class RoutingDemoModels {

    /** Returns the current {@link Step} with information such as the cue text and images. */
    @NonNull
    public static Step getCurrentStep(@NonNull CarContext carContext) {
        // Create the cue text, and span the "520" text with a highway sign image.
        String currentStepCue = "Roy st 520";
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
        String nextStepCue = "I5 Aurora Ave N";
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
        return new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                carContext,
                                                "Bug reported!",
                                                CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        carContext,
                                                        R.drawable.ic_bug_report_24px))
                                                .build())
                                .build())
                .addAction(
                        new Action.Builder()
                                .setTitle("Stop")
                                .setOnClickListener(onStopNavigation)
                                .build())
                .build();
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
                                                "Zoomed in",
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
                                                "Zoomed out",
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
    public static TravelEstimate getTravelEstimate() {
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
                .build();
    }

    private RoutingDemoModels() {
    }
}
