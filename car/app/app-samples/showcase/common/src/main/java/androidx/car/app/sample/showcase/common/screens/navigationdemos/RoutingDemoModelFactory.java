/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.navigationdemos;

import static androidx.car.app.model.Action.FLAG_DEFAULT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_STRAIGHT;

import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
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

import org.jspecify.annotations.NonNull;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** A class that provides models for the routing demos. */
public class RoutingDemoModelFactory {
    private final @NonNull CarContext mCarContext;

    public RoutingDemoModelFactory(@NonNull CarContext carContext) {
        mCarContext = carContext;
    }

    /** Returns a sample {@link Alert}. */
    private @NonNull Alert createAlert() {
        CarText title = createCarText(R.string.navigation_alert_title);
        CarText subtitle = createCarText(R.string.navigation_alert_subtitle);
        CarIcon icon = CarIcon.ALERT;

        Action yesAction = createToastAction(R.string.yes_action_title,
                R.string.yes_action_toast_msg, FLAG_PRIMARY);
        Action noAction = createToastAction(R.string.no_action_title, R.string.no_action_toast_msg,
                FLAG_DEFAULT);

        return new Alert.Builder(/* alertId: */ 0, title, /* durationMillis: */ 10000)
                .setSubtitle(subtitle)
                .setIcon(icon)
                .addAction(yesAction)
                .addAction(noAction).setCallback(new AlertCallback() {
                    @Override
                    public void onCancel(int reason) {
                        if (reason == AlertCallback.REASON_TIMEOUT) {
                            showToast(R.string.alert_timeout_toast_msg);
                        }
                    }

                    @Override
                    public void onDismiss() {
                    }
                }).build();
    }

    /** Returns the current {@link Step} with information such as the cue text and images. */
    public @NonNull Step getCurrentStep() {
        SpannableString currentStepCueWithImage =
                createStringWithIcon(R.string.current_step_cue, R.drawable.ic_520, 7, 10);

        CarIcon currentTurnIcon = createCarIcon(R.drawable.arrow_right_turn);
        Maneuver currentManeuver =
                new Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT)
                        .setIcon(currentTurnIcon)
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
                .setLanesImage(createCarIcon(R.drawable.lanes))
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(straightNormal)
                .addLane(rightHighlighted)
                .build();
    }

    /** Returns the next {@link Step} with information such as the cue text and images. */
    public @NonNull Step getNextStep() {
        SpannableString nextStepCueWithImage =
                createStringWithIcon(R.string.next_step_cue, R.drawable.ic_i5, 0, 2);

        Maneuver nextManeuver = new Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
                .setIcon(createCarIcon(R.drawable.arrow_straight))
                .build();

        return new Step.Builder(nextStepCueWithImage).setManeuver(nextManeuver).build();
    }

    /**
     * Returns the action strip that contains a "bug report" button and "stop navigation" button.
     */
    public @NonNull ActionStrip getActionStrip(@NonNull OnClickListener onStopNavigation) {
        ActionStrip.Builder builder = new ActionStrip.Builder();
        if (mCarContext.getCarAppApiLevel() >= CarAppApiLevels.LEVEL_5) {
            CarColor actionButtonRed = CarColor.createCustom(0xffb40404, 0xffb40404);
            builder.addAction(
                    new Action.Builder()
                            .setFlags(FLAG_PRIMARY)
                            .setBackgroundColor(actionButtonRed)
                            .setOnClickListener(
                                    () -> mCarContext.getCarService(AppManager.class)
                                            .showAlert(createAlert()))
                            .setIcon(createCarIcon(R.drawable.ic_baseline_add_alert_24))
                            .build());
        }
        builder.addAction(
                createToastAction(R.drawable.ic_bug_report_24px, R.string.bug_reported_toast_msg));
        builder.addAction(
                new Action.Builder()
                        .setTitle(createCarText(R.string.stop_action_title))
                        .setOnClickListener(onStopNavigation)
                        .setFlags(Action.FLAG_IS_PERSISTENT)
                        .build());
        return builder.build();
    }

    /**
     * Returns the map action strip that contains pan and zoom buttons.
     */
    public @NonNull ActionStrip getMapActionStrip() {
        return new ActionStrip.Builder()
                .addAction(
                        createToastAction(R.drawable.ic_zoom_in_24, R.string.zoomed_in_toast_msg))
                .addAction(
                        createToastAction(R.drawable.ic_zoom_out_24, R.string.zoomed_out_toast_msg))
                .addAction(Action.PAN)
                .build();
    }

    /** Returns the {@link TravelEstimate} with time and distance information. */
    public @NonNull TravelEstimate getTravelEstimate() {
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
                .setTripText(createCarText(R.string.travel_est_trip_text))
                .setTripIcon(createCarIcon(R.drawable.ic_face_24px))
                .build();
    }

    private Action createToastAction(@StringRes int titleRes, @StringRes int toastStringRes,
            int flags) {
        return new Action.Builder()
                .setOnClickListener(() -> showToast(toastStringRes))
                .setTitle(createCarText(titleRes))
                .setFlags(flags)
                .build();
    }

    private Action createToastAction(@DrawableRes int iconRes, @StringRes int toastStringRes) {
        return new Action.Builder()
                .setOnClickListener(() -> showToast(toastStringRes))
                .setIcon(createCarIcon(iconRes))
                .build();
    }

    private void showToast(@StringRes int toastStringRes) {
        CarToast.makeText(
                        mCarContext,
                        mCarContext.getString(toastStringRes),
                        CarToast.LENGTH_SHORT)
                .show();
    }

    private SpannableString createStringWithIcon(@StringRes int stringRes,
            @DrawableRes int iconRes, int start, int end) {
        String text = mCarContext.getString(stringRes);
        CarIconSpan span = CarIconSpan.create(createCarIcon(iconRes), CarIconSpan.ALIGN_CENTER);
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private CarText createCarText(@StringRes int stringRes) {
        return CarText.create(mCarContext.getString(stringRes));
    }

    private CarIcon createCarIcon(@DrawableRes int iconRes) {
        return new CarIcon.Builder(IconCompat.createWithResource(mCarContext, iconRes)).build();
    }
}
