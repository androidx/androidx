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

package androidx.car.app.sample.navigation.common.model;

import static androidx.car.app.navigation.model.LaneDirection.SHAPE_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_STRAIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_DEPART;
import static androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION;
import static androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION_STRAIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_FERRY_BOAT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_FERRY_TRAIN;
import static androidx.car.app.navigation.model.Maneuver.TYPE_FORK_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_FORK_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_KEEP_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_KEEP_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_MERGE_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_MERGE_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED;
import static androidx.car.app.navigation.model.Maneuver.TYPE_NAME_CHANGE;
import static androidx.car.app.navigation.model.Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_OFF_RAMP_SLIGHT_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_OFF_RAMP_SLIGHT_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_NORMAL_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_SHARP_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_SHARP_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_SLIGHT_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_SLIGHT_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_U_TURN_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ON_RAMP_U_TURN_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_CCW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_CW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_EXIT_CCW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_EXIT_CW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_STRAIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_SHARP_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_SHARP_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_SLIGHT_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_TURN_SLIGHT_RIGHT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_UNKNOWN;
import static androidx.car.app.navigation.model.Maneuver.TYPE_U_TURN_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_U_TURN_RIGHT;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.sample.navigation.common.R;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Example scripts to "control" navigation within the app.
 *
 * <p>The script takes the form of a list of instructions which can be passed to different parts of
 * the app. This is the central location where scripts are stored.
 *
 * <p>The scripts start with a setup phase where all destinations and steps are added to the
 * instruction list. Then navigation updates are added for each step to simulate driving.
 *
 * <p>>The setup phases consists of:
 *
 * <ul>
 *   <li>Start Navigation
 *   <li>Add the destination information.
 *   <li>Add 4 intermediate steps to the destination.
 * </ul>
 *
 * <p>The navigation phase consists of
 *
 * <ul>
 *   <li>Add positions along the route getting closer to the step.
 *   <li>Once the step is reached, pop the step. If more steps remain go back to adding more
 *       positions.
 *   <li>When no more steps remain set the arrived state.
 *   <li>End Navigation
 * </ul>
 *
 * <p>There are several helper functions including {@link #generateTripUpdateSequence} which
 * interpolates a straight path and generates all the updates for a step.
 */
public class DemoScripts {

    private static final long INSTRUCTION_NO_ELAPSED_TIME = 0;
    private static final int SPEED_METERS_PER_SEC = 5;
    private static final int DISTANCE_METERS = 450;

    /** Create instructions for home. */
    @NonNull
    public static List<Instruction> getNavigateHome(@NonNull CarContext carContext) {
        ArrayList<Instruction> instructions = new ArrayList<>();

        DateTimeWithZone arrivalTimeAtDestination = getCurrentDateTimeZoneWithOffset(30);

        CarIcon lanesImage =
                new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.lanes))
                        .build();
        CarIcon junctionImage =
                new CarIcon.Builder(
                        IconCompat.createWithResource(
                                carContext,
                                R.drawable.junction_image))
                        .build();

        Lane straightNormal =
                new Lane.Builder()
                        .addDirection(LaneDirection.create(SHAPE_STRAIGHT, false))
                        .build();
        Lane rightHighlighted =
                new Lane.Builder()
                        .addDirection(LaneDirection.create(SHAPE_NORMAL_RIGHT, true))
                        .build();

        int step1IconResourceId =
                getTurnIconResourceId(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE);
        Step step1 =
                new Step.Builder("State Street")
                        .setManeuver(
                                getManeuverWithExitNumberAndAngle(
                                        carContext,
                                        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE,
                                        step1IconResourceId,
                                        2,
                                        270))
                        .setRoad("State Street")
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(rightHighlighted)
                        .setLanesImage(lanesImage)
                        .build();
        int step2IconResourceId = getTurnIconResourceId(Maneuver.TYPE_TURN_NORMAL_LEFT);
        Step step2 =
                new Step.Builder("Kirkland Way")
                        .setManeuver(
                                getManeuver(
                                        carContext,
                                        Maneuver.TYPE_TURN_NORMAL_LEFT,
                                        step2IconResourceId))
                        .setRoad("Kirkland Way")
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(rightHighlighted)
                        .setLanesImage(lanesImage)
                        .build();
        int step3IconResourceId = getTurnIconResourceId(Maneuver.TYPE_TURN_NORMAL_RIGHT);
        Step step3 =
                new Step.Builder("6th Street.")
                        .setManeuver(
                                getManeuver(
                                        carContext,
                                        Maneuver.TYPE_TURN_NORMAL_RIGHT,
                                        step3IconResourceId))
                        .setRoad("6th Street.")
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(straightNormal)
                        .addLane(rightHighlighted)
                        .setLanesImage(lanesImage)
                        .build();
        int step4IconResourceId = getTurnIconResourceId(Maneuver.TYPE_DESTINATION_RIGHT);
        Step step4 =
                new Step.Builder("Google Kirkland.")
                        .setManeuver(
                                getManeuver(
                                        carContext, TYPE_DESTINATION_RIGHT, step4IconResourceId))
                        .setRoad("Google Kirkland.")
                        .build();

        // Start the navigation and add destination and steps.
        instructions.add(
                Instruction.builder(Instruction.Type.START_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .build());

        Destination destination =
                new Destination.Builder().setName("Work").setAddress("747 6th St.").build();
        instructions.add(
                Instruction.builder(
                        Instruction.Type.ADD_DESTINATION_NAVIGATION,
                        INSTRUCTION_NO_ELAPSED_TIME)
                        .setDestination(destination)
                        .build());

        instructions.add(
                Instruction.builder(Instruction.Type.SET_REROUTING, TimeUnit.SECONDS.toMillis(5))
                        .setDestinationTravelEstimate(
                                new TravelEstimate.Builder(
                                        Distance.create(350, Distance.UNIT_METERS),
                                        arrivalTimeAtDestination)
                                        .setRemainingTimeSeconds(
                                                /* remainingTimeSeconds= */ DISTANCE_METERS
                                                        / SPEED_METERS_PER_SEC)
                                        .build())
                        .setNotification(
                                true,
                                carContext.getString(R.string.navigation_rerouting),
                                null,
                                R.drawable.ic_launcher)
                        .build());

        instructions.add(
                Instruction.builder(
                        Instruction.Type.ADD_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .setStep(step1)
                        .build());
        instructions.add(
                Instruction.builder(
                        Instruction.Type.ADD_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .setStep(step2)
                        .build());
        instructions.add(
                Instruction.builder(
                        Instruction.Type.ADD_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .setStep(step3)
                        .build());
        instructions.add(
                Instruction.builder(
                        Instruction.Type.ADD_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .setStep(step4)
                        .build());

        // Add trip positions for each step.
        int updateDistanceRemaining = DISTANCE_METERS;
        instructions.addAll(
                generateTripUpdateSequence(
                        /* count= */ 4,
                        /* startDestinationDistanceRemaining= */ updateDistanceRemaining,
                        /* startStepDistanceRemaining= */ 100,
                        arrivalTimeAtDestination,
                        "3rd Street",
                        junctionImage,
                        /* showLanes= */ true,
                        "onto State Street",
                        SPEED_METERS_PER_SEC,
                        step1IconResourceId));
        instructions.add(
                Instruction.builder(
                        Instruction.Type.POP_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .build());
        updateDistanceRemaining -= 100;

        instructions.addAll(
                generateTripUpdateSequence(
                        /* count= */ 6,
                        /* startDestinationDistanceRemaining= */ updateDistanceRemaining,
                        /* startStepDistanceRemaining= */ 150,
                        arrivalTimeAtDestination,
                        "State Street",
                        junctionImage,
                        /* showLanes= */ true,
                        "onto Kirkland Way",
                        SPEED_METERS_PER_SEC,
                        step2IconResourceId));
        instructions.add(
                Instruction.builder(
                        Instruction.Type.POP_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .build());
        updateDistanceRemaining -= 150;

        instructions.addAll(
                generateTripUpdateSequence(
                        /* count= */ 4,
                        /* startDestinationDistanceRemaining= */ updateDistanceRemaining,
                        /* startStepDistanceRemaining= */ 100,
                        arrivalTimeAtDestination,
                        "Kirkland Way",
                        junctionImage,
                        /* showLanes= */ true,
                        "onto 6th Street",
                        SPEED_METERS_PER_SEC,
                        step3IconResourceId));
        instructions.add(
                Instruction.builder(
                        Instruction.Type.POP_STEP_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .build());
        updateDistanceRemaining -= 100;

        instructions.addAll(
                generateTripUpdateSequence(
                        /* count= */ 4,
                        /* startDestinationDistanceRemaining= */ updateDistanceRemaining,
                        /* startStepDistanceRemaining= */ 100,
                        arrivalTimeAtDestination,
                        "6th Street",
                        /* junctionImage= */ null,
                        /* showLanes= */ false,
                        "to Google Kirkland on right",
                        SPEED_METERS_PER_SEC,
                        step4IconResourceId));

        // Set arrived state and then stop navigation.
        instructions.add(
                Instruction.builder(Instruction.Type.SET_ARRIVED, TimeUnit.SECONDS.toMillis(5))
                        .build());

        instructions.add(
                Instruction.builder(Instruction.Type.POP_DESTINATION_NAVIGATION,
                        INSTRUCTION_NO_ELAPSED_TIME)
                        .build());
        instructions.add(
                Instruction.builder(Instruction.Type.END_NAVIGATION, INSTRUCTION_NO_ELAPSED_TIME)
                        .build());
        return instructions;
    }

    private static DateTimeWithZone getCurrentDateTimeZoneWithOffset(int offsetSeconds) {
        GregorianCalendar startTime = new GregorianCalendar();
        GregorianCalendar destinationETA = (GregorianCalendar) startTime.clone();
        destinationETA.add(Calendar.SECOND, offsetSeconds);
        return getDateTimeZone(destinationETA);
    }

    /** Convenience function to create the date formmat. */
    private static DateTimeWithZone getDateTimeZone(GregorianCalendar calendar) {
        Date date = calendar.getTime();
        TimeZone timeZone = calendar.getTimeZone();

        long timeSinceEpochMillis = date.getTime();
        long timeZoneOffsetSeconds =
                MILLISECONDS.toSeconds(timeZone.getOffset(timeSinceEpochMillis));
        String zoneShortName = "PST";

        return DateTimeWithZone.create(
                timeSinceEpochMillis, (int) timeZoneOffsetSeconds, zoneShortName);
    }

    /**
     * Generates all the updates for a particular step interpolating along a straight line.
     *
     * @param count                             number of instructions to generate until the next
     *                                          step
     * @param startDestinationDistanceRemaining the distance until the final destination at the
     *                                          start of the sequence
     * @param startStepDistanceRemaining        the distance until the next step at the start of the
     *                                          sequence
     * @param arrivalTimeAtDestination          the arrival time at the destination
     * @param currentRoad                       the name of the road currently being travelled
     * @param junctionImage                     photo realistic image of upcoming turn
     * @param showLanes                         indicates if the lane info should be shown for
     *                                          this maneuver
     * @param speed                             meters/second being traveled
     * @return sequence of instructions until the next step
     */
    private static List<Instruction> generateTripUpdateSequence(
            int count,
            int startDestinationDistanceRemaining,
            int startStepDistanceRemaining,
            DateTimeWithZone arrivalTimeAtDestination,
            String currentRoad,
            @Nullable CarIcon junctionImage,
            boolean showLanes,
            String nextInstruction,
            int speed,
            int notificationIcon) {
        List<Instruction> sequence = new ArrayList<>(count);
        int destinationDistanceRemaining = startDestinationDistanceRemaining;
        int stepDistanceRemaining = startStepDistanceRemaining;
        int distanceIncrement = startStepDistanceRemaining / count;
        boolean notify = true;

        for (int i = 0; i < count; i++) {
            Distance remainingDistance =
                    Distance.create(stepDistanceRemaining, Distance.UNIT_METERS);
            TravelEstimate destinationTravelEstimate =
                    new TravelEstimate.Builder(
                            Distance.create(
                                    destinationDistanceRemaining, Distance.UNIT_METERS),
                            arrivalTimeAtDestination)
                            .setRemainingTimeSeconds(destinationDistanceRemaining / speed)
                            .setRemainingTimeColor(CarColor.YELLOW)
                            .setRemainingDistanceColor(CarColor.GREEN)
                            .build();
            TravelEstimate stepTravelEstimate =
                    new TravelEstimate.Builder(
                            remainingDistance,
                            getCurrentDateTimeZoneWithOffset(distanceIncrement))
                            .setRemainingTimeSeconds(/* remainingTimeSeconds= */ distanceIncrement)
                            .build();
            String notificationTitle = String.format("%dm", stepDistanceRemaining);
            Instruction.Builder instruction =
                    Instruction.builder(
                            Instruction.Type.SET_TRIP_POSITION_NAVIGATION,
                            TimeUnit.SECONDS.toMillis(distanceIncrement / speed))
                            .setStepRemainingDistance(remainingDistance)
                            .setStepTravelEstimate(stepTravelEstimate)
                            .setDestinationTravelEstimate(destinationTravelEstimate)
                            .setRoad(currentRoad)
                            .setNotification(
                                    notify, notificationTitle, nextInstruction, notificationIcon);
            // Don't show lanes in the first and last part of the maneuver. In the middle part of
            // the
            // maneuver use the passed parameter to determine if lanes should be shown.
            if (i == 0) {
                instruction.setShouldShowLanes(false).setShouldShowNextStep(true);
            } else if (i == 1) {
                instruction.setShouldShowLanes(showLanes).setShouldShowNextStep(true);
            } else if (i == 2) {
                instruction.setShouldShowLanes(showLanes).setShouldShowNextStep(false);
            } else {
                instruction
                        .setShouldShowLanes(false)
                        .setShouldShowNextStep(false)
                        .setJunctionImage(junctionImage);
            }
            sequence.add(instruction.build());

            destinationDistanceRemaining -= distanceIncrement;
            stepDistanceRemaining -= distanceIncrement;
            notify = false;
        }
        return sequence;
    }

    /** Returns a maneuver with image selected from resources. */
    private static Maneuver getManeuver(
            @NonNull CarContext carContext, int type, int iconResourceId) {
        return new Maneuver.Builder(type).setIcon(getCarIcon(carContext, iconResourceId)).build();
    }

    /**
     * Returns a maneuver that includes an exit number and angle with image selected from resources.
     */
    private static Maneuver getManeuverWithExitNumberAndAngle(
            @NonNull CarContext carContext,
            int type,
            int iconResourceId,
            int exitNumber,
            int exitAngle) {
        return new Maneuver.Builder(type)
                .setIcon(getCarIcon(carContext, iconResourceId))
                .setRoundaboutExitNumber(exitNumber)
                .setRoundaboutExitAngle(exitAngle)
                .build();
    }

    /** Generates a {@link CarIcon} representing the turn. */
    private static CarIcon getCarIcon(@NonNull CarContext carContext, int resourceId) {
        return new CarIcon.Builder(IconCompat.createWithResource(carContext, resourceId)).build();
    }

    private static int getTurnIconResourceId(int type) {
        int resourceId = R.drawable.ic_launcher;
        switch (type) {
            case TYPE_TURN_NORMAL_LEFT:
                resourceId = R.drawable.ic_turn_normal_left;
                break;
            case TYPE_TURN_NORMAL_RIGHT:
                resourceId = R.drawable.ic_turn_normal_right;
                break;
            case TYPE_UNKNOWN:
            case TYPE_DEPART:
            case TYPE_STRAIGHT:
                resourceId = R.drawable.ic_turn_name_change;
                break;
            case TYPE_DESTINATION:
            case TYPE_DESTINATION_STRAIGHT:
            case TYPE_DESTINATION_RIGHT:
            case TYPE_DESTINATION_LEFT:
                resourceId = R.drawable.ic_turn_destination;
                break;
            case TYPE_NAME_CHANGE:
                resourceId = R.drawable.ic_turn_name_change;
                break;
            case TYPE_KEEP_LEFT:
            case TYPE_TURN_SLIGHT_LEFT:
                resourceId = R.drawable.ic_turn_slight_left;
                break;
            case TYPE_KEEP_RIGHT:
            case TYPE_TURN_SLIGHT_RIGHT:
                resourceId = R.drawable.ic_turn_slight_right;
                break;
            case TYPE_TURN_SHARP_LEFT:
                resourceId = R.drawable.ic_turn_sharp_left;
                break;
            case TYPE_TURN_SHARP_RIGHT:
                resourceId = R.drawable.ic_turn_sharp_right;
                break;
            case TYPE_U_TURN_LEFT:
                resourceId = R.drawable.ic_turn_u_turn_left;
                break;
            case TYPE_U_TURN_RIGHT:
                resourceId = R.drawable.ic_turn_u_turn_right;
                break;
            case TYPE_ON_RAMP_SLIGHT_LEFT:
            case TYPE_ON_RAMP_NORMAL_LEFT:
            case TYPE_ON_RAMP_SHARP_LEFT:
            case TYPE_ON_RAMP_U_TURN_LEFT:
            case TYPE_OFF_RAMP_SLIGHT_LEFT:
            case TYPE_OFF_RAMP_NORMAL_LEFT:
            case TYPE_FORK_LEFT:
                resourceId = R.drawable.ic_turn_fork_left;
                break;
            case TYPE_ON_RAMP_SLIGHT_RIGHT:
            case TYPE_ON_RAMP_NORMAL_RIGHT:
            case TYPE_ON_RAMP_SHARP_RIGHT:
            case TYPE_ON_RAMP_U_TURN_RIGHT:
            case TYPE_OFF_RAMP_SLIGHT_RIGHT:
            case TYPE_OFF_RAMP_NORMAL_RIGHT:
            case TYPE_FORK_RIGHT:
                resourceId = R.drawable.ic_turn_fork_right;
                break;
            case TYPE_MERGE_LEFT:
            case TYPE_MERGE_RIGHT:
            case TYPE_MERGE_SIDE_UNSPECIFIED:
                resourceId = R.drawable.ic_turn_merge_symmetrical;
                break;
            case TYPE_ROUNDABOUT_ENTER_CW:
            case TYPE_ROUNDABOUT_ENTER_CCW:
            case TYPE_ROUNDABOUT_EXIT_CW:
            case TYPE_ROUNDABOUT_EXIT_CCW:
                resourceId = R.drawable.ic_turn_name_change;
                break;
            case TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW:
            case TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE:
                resourceId = R.drawable.ic_roundabout_cw;
                break;
            case TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW:
            case TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE:
                resourceId = R.drawable.ic_roundabout_ccw;
                break;
            case TYPE_FERRY_BOAT:
            case TYPE_FERRY_TRAIN:
                resourceId = R.drawable.ic_turn_name_change;
                break;
            default:
                throw new IllegalStateException("Unexpected maneuver type: " + type);
        }
        return resourceId;
    }

    private DemoScripts() {
    }
}
