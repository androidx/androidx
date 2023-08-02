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
import static androidx.car.app.navigation.model.TravelEstimate.REMAINING_TIME_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.car.app.TestUtils;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Tests for {@link TravelEstimate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TravelEstimateTest {
    private final DateTimeWithZone mArrivalTime =
            createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
    private final Distance mRemainingDistance =
            Distance.create(/* displayDistance= */ 100, Distance.UNIT_METERS);
    private final long mRemainingTime = TimeUnit.HOURS.toMillis(10);
    private final CarText mTripInformation = CarText.create("Batter Level is Low");

    @Test
    public void build_default_to_unknown_time() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        long remainingTime = TimeUnit.HOURS.toMillis(10);
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(remainingDistance, arrivalTime).build();

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(REMAINING_TIME_UNKNOWN);
        assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
        assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    public void create_duration() {
        ZonedDateTime arrivalTime = ZonedDateTime.parse("2020-05-14T19:57:00-07:00[US/Pacific]");
        Duration remainingTime = Duration.ofHours(10);

        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance, arrivalTime).setRemainingTime(
                        remainingTime).build();

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(mRemainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(remainingTime.getSeconds());
        assertDateTimeWithZoneEquals(arrivalTime, travelEstimate.getArrivalTimeAtDestination());
    }

    @Test
    public void create() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        long remainingTime = TimeUnit.HOURS.toMillis(10);
        CarIcon tripIcon = CarIcon.APP_ICON;
        CarText tripInformation = CarText.create("Pick Up Alice");
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(remainingDistance, arrivalTime).setRemainingTimeSeconds(
                        TimeUnit.MILLISECONDS.toSeconds(remainingTime)).setTripIcon(
                        tripIcon).setTripText(tripInformation).build();

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(
                TimeUnit.MILLISECONDS.toSeconds(remainingTime));
        assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
        assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(CarColor.DEFAULT);
        assertThat(travelEstimate.getTripText()).isEqualTo(tripInformation);
        assertThat(travelEstimate.getTripIcon()).isEqualTo(tripIcon);
    }

    @Test
    public void create_unknown_remaining_time_in_seconds() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(remainingDistance, arrivalTime).setRemainingTimeSeconds(
                        REMAINING_TIME_UNKNOWN).build();

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(REMAINING_TIME_UNKNOWN);
        assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
        assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    public void create_unknown_remaining_time() {
        ZonedDateTime arrivalTime = ZonedDateTime.parse("2020-05-14T19:57:00-07:00[US/Pacific]");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(
                        remainingDistance, arrivalTime).setRemainingTime(
                        Duration.ofSeconds(REMAINING_TIME_UNKNOWN)).build();

        assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
        assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(REMAINING_TIME_UNKNOWN);
        assertDateTimeWithZoneEquals(arrivalTime, travelEstimate.getArrivalTimeAtDestination());
        assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    public void create_invalid_remaining_time() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        assertThrows(
                IllegalArgumentException.class,
                () -> new TravelEstimate.Builder(remainingDistance,
                        arrivalTime).setRemainingTimeSeconds(-2));
    }

    @Test
    public void create_custom_remainingTimeColor() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        long remainingTime = TimeUnit.HOURS.toMillis(10);

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
                    new TravelEstimate.Builder(remainingDistance,
                            arrivalTime)
                            .setRemainingTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(remainingTime))
                            .setRemainingTimeColor(carColor)
                            .build();

            assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
            assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(
                    TimeUnit.MILLISECONDS.toSeconds(remainingTime));
            assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
            assertThat(travelEstimate.getRemainingTimeColor()).isEqualTo(carColor);
        }
    }

    @Test
    public void create_custom_remainingDistanceColor() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        long remainingTime = TimeUnit.HOURS.toMillis(10);

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
                    new TravelEstimate.Builder(remainingDistance,
                            arrivalTime)
                            .setRemainingTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(remainingTime))
                            .setRemainingDistanceColor(carColor)
                            .build();

            assertThat(travelEstimate.getRemainingDistance()).isEqualTo(remainingDistance);
            assertThat(travelEstimate.getRemainingTimeSeconds()).isEqualTo(
                    TimeUnit.MILLISECONDS.toSeconds(remainingTime));
            assertThat(travelEstimate.getArrivalTimeAtDestination()).isEqualTo(arrivalTime);
            assertThat(travelEstimate.getRemainingDistanceColor()).isEqualTo(carColor);
        }
    }

    @Test
    public void create_custom_remainingTimeColor_invalid_throws() {
        DateTimeWithZone arrivalTime = createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific");
        Distance remainingDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        long remainingTime = TimeUnit.HOURS.toMillis(10);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TravelEstimate.Builder(remainingDistance,
                                arrivalTime)
                                .setRemainingTimeSeconds(
                                        TimeUnit.MILLISECONDS.toSeconds(remainingTime))
                                .setRemainingTimeColor(CarColor.createCustom(1, 2)));
    }

    @Test
    public void create_invalidCarIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime).setTripIcon(carIcon));
    }

    @Test
    public void create_tripInformation_unsupportedSpans() {
        CharSequence text = TestUtils.getCharSequenceWithIconSpan("Text");
        CarText tripInformation = CarText.create(text);
        assertThrows(
                IllegalArgumentException.class,
                () -> new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime).setTripText(tripInformation));

        // ColorSpan do not throw
        CharSequence text2 = TestUtils.getCharSequenceWithColorSpan("Text");
        CarText tripInformation2 = CarText.create(text2);
        new TravelEstimate.Builder(mRemainingDistance,
                mArrivalTime).setTripText(tripInformation2).build();
    }

    @Test
    public void equals() {
        TravelEstimate travelEstimate = new TravelEstimate.Builder(mRemainingDistance,
                mArrivalTime).setRemainingTimeSeconds(
                TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).setTripIcon(
                CarIcon.APP_ICON).setTripText(mTripInformation).build();

        assertThat(travelEstimate)
                .isEqualTo(
                        new TravelEstimate.Builder(mRemainingDistance,
                                mArrivalTime).setRemainingTimeSeconds(
                                TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).setTripIcon(
                                CarIcon.APP_ICON).setTripText(mTripInformation).build());
    }

    @Test
    public void notEquals_differentRemainingDistance() {
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime).setRemainingTimeSeconds(
                        TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(
                                Distance.create(/* displayDistance= */ 200, Distance.UNIT_METERS),
                                mArrivalTime).setRemainingTimeSeconds(
                                TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).build());
    }

    @Test
    public void notEquals_differentRemainingTime() {
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime).setRemainingTimeSeconds(
                        TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(mRemainingDistance,
                                mArrivalTime).setRemainingTimeSeconds(
                                TimeUnit.MILLISECONDS.toSeconds(mRemainingTime) + 1).build());
    }

    @Test
    public void notEquals_differentArrivalTime() {
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime).setRemainingTimeSeconds(
                        TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(
                                mRemainingDistance,
                                createDateTimeWithZone("2020-04-14T15:57:01",
                                        "US/Pacific")).setRemainingTimeSeconds(
                                TimeUnit.MILLISECONDS.toSeconds(mRemainingTime)).build());
    }

    @Test
    public void notEquals_differentRemainingTimeColor() {
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime)
                        .setRemainingTimeSeconds(TimeUnit.MILLISECONDS.toSeconds(mRemainingTime))
                        .setRemainingTimeColor(CarColor.YELLOW)
                        .build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(mRemainingDistance,
                                mArrivalTime)
                                .setRemainingTimeSeconds(
                                        TimeUnit.MILLISECONDS.toSeconds(mRemainingTime))
                                .setRemainingTimeColor(CarColor.GREEN)
                                .build());
    }

    @Test
    public void notEquals_differentTripIcon() {
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime)
                        .setTripIcon(CarIcon.APP_ICON)
                        .build();

        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(mRemainingDistance,
                                mArrivalTime)
                                .setTripIcon(CarIcon.ALERT)
                                .build());
    }

    @Test
    public void notEquals_differentTripInformation() {
        CarText tripInformation1 = CarText.create("test1");
        TravelEstimate travelEstimate =
                new TravelEstimate.Builder(mRemainingDistance,
                        mArrivalTime)
                        .setTripText(tripInformation1)
                        .build();

        CarText tripInformation2 = CarText.create("test2");
        assertThat(travelEstimate)
                .isNotEqualTo(
                        new TravelEstimate.Builder(mRemainingDistance,
                                mArrivalTime)
                                .setTripText(tripInformation2)
                                .build());
    }
}
