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

package androidx.car.app.navigation.model;

import static androidx.car.app.TestUtils.assertDateTimeWithZoneEquals;
import static androidx.car.app.TestUtils.createDateTimeWithZone;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build.VERSION_CODES;

import androidx.car.app.model.CarColor;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tests for {@link TravelEstimate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TravelEstimateTest {
    private final DateTimeWithZone mArrivalTime =
            createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
    private final Distance mRemainingDistance =
            Distance.create(/* displayDistance= */ 100, Distance.UNIT_METERS);
    private final Duration mRemainingTime = Duration.ofHours(10);

    @Test
    @Config(minSdk = VERSION_CODES.O)
    public void create_duration() {
        ZonedDateTime arrivalTime = ZonedDateTime.parse("2020-05-14T19:57:00-07:00[US/Pacific]");
        TravelEstimate travelEstimate =
                TravelEstimate.create(mRemainingDistance, mRemainingTime, arrivalTime);

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(mRemainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(mRemainingTime.getSeconds());
        assertDateTimeWithZoneEquals(arrivalTime, travelEstimate.getArrivalTimeAtDestination());
    }

    @Test
    public void create() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        Duration remainingTime = Duration.ofHours(10);
        TravelEstimate travelEstimate =
                TravelEstimate.create(remainingDistance, remainingTime.getSeconds(), arrivalTime);

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(remainingTime.getSeconds());
        assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
        assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    public void create_custom_remainingTimeColor() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        Duration remainingTime = Duration.ofHours(10);

        List<CarColor> allowedColors = new ArrayList<>();
        allowedColors.add(CarColor.DEFAULT);
        allowedColors.add(CarColor.PRIMARY);
        allowedColors.add(CarColor.SECONDARY);
        allowedColors.add(CarColor.RED);
        allowedColors.add(CarColor.GREEN);
        allowedColors.add(CarColor.BLUE);
        allowedColors.add(CarColor.YELLOW);

        for (CarColor carColor : allowedColors) {
            TravelEstimate travelEstimate =
                    TravelEstimate.builder(remainingDistance, remainingTime.getSeconds(),
                            arrivalTime)
                            .setRemainingTimeColor(carColor)
                            .build();

            assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
            assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(
                    remainingTime.getSeconds());
            assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
            assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(carColor);
        }
    }

    @Test
    public void create_custom_remainingDistanceColor() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        Duration remainingTime = Duration.ofHours(10);

        List<CarColor> allowedColors = new ArrayList<>();
        allowedColors.add(CarColor.DEFAULT);
        allowedColors.add(CarColor.PRIMARY);
        allowedColors.add(CarColor.SECONDARY);
        allowedColors.add(CarColor.RED);
        allowedColors.add(CarColor.GREEN);
        allowedColors.add(CarColor.BLUE);
        allowedColors.add(CarColor.YELLOW);

        for (CarColor carColor : allowedColors) {
            TravelEstimate travelEstimate =
                    TravelEstimate.builder(remainingDistance, remainingTime.getSeconds(),
                            arrivalTime)
                            .setRemainingDistanceColor(carColor)
                            .build();

            assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
            assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(
                    remainingTime.getSeconds());
            assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
            assertThat(travelEstimate.getRemainingDistanceColor()).isEqualTo(carColor);
        }
    }

    @Test
    public void create_custom_remainingTimeColor_invalid_throws() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        Duration remainingTime = Duration.ofHours(10);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TravelEstimate.builder(remainingDistance, remainingTime.getSeconds(),
                                arrivalTime)
                                .setRemainingTimeColor(CarColor.createCustom(1, 2)));
    }

    @Test
    public void equals() {
        TravelEstimate travelEstimate = TravelEstimate.create(mRemainingDistance,
                mRemainingTime.getSeconds(), mArrivalTime);

        assertThat(travelEstimate)
                .isEqualTo(
                        TravelEstimate.create(mRemainingDistance, mRemainingTime.getSeconds(),
                                mArrivalTime));
    }

    @Test
    public void notEquals_differentRemainingDistance() {
        TravelEstimate travelEstimate =
                TravelEstimate.create(mRemainingDistance, mRemainingTime.getSeconds(),
                        mArrivalTime);

        assertThat(travelEstimate)
                .isNotEqualTo(
                        TravelEstimate.create(
                                Distance.create(/* displayDistance= */ 200, Distance.UNIT_METERS),
                                mRemainingTime.getSeconds(),
                                mArrivalTime));
    }

    @Test
    public void notEquals_differentRemainingTime() {
        TravelEstimate travelEstimate =
                TravelEstimate.create(mRemainingDistance, mRemainingTime.getSeconds(),
                        mArrivalTime);

        assertThat(travelEstimate)
                .isNotEqualTo(
                        TravelEstimate.create(mRemainingDistance, mRemainingTime.getSeconds() + 1,
                                mArrivalTime));
    }

    @Test
    public void notEquals_differentArrivalTime() {
        TravelEstimate travelEstimate =
                TravelEstimate.create(mRemainingDistance, mRemainingTime.getSeconds(),
                        mArrivalTime);

        assertThat(travelEstimate)
                .isNotEqualTo(
                        TravelEstimate.create(
                                mRemainingDistance,
                                mRemainingTime.getSeconds(),
                                createDateTimeWithZone("2020-04-14T15:57:01", "US/Pacific")));
    }

    @Test
    public void notEquals_differentRemainingTimeColor() {
        TravelEstimate travelEstimate =
                TravelEstimate.builder(mRemainingDistance, mRemainingTime.getSeconds(),
                        mArrivalTime)
                        .setRemainingTimeColor(CarColor.YELLOW)
                        .build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        TravelEstimate.builder(mRemainingDistance, mRemainingTime.getSeconds(),
                                mArrivalTime)
                                .setRemainingTimeColor(CarColor.GREEN)
                                .build());
    }
}
