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

import static androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_CW;
import static androidx.car.app.navigation.model.Maneuver.TYPE_STRAIGHT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.car.app.model.CarIcon;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link Maneuver}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ManeuverTest {

    @Test
    public void createInstance_non_roundabout() {
        int type = TYPE_STRAIGHT;

        Maneuver maneuver = Maneuver.builder(type).setIcon(CarIcon.APP_ICON).build();
        assertThat(type).isEqualTo(maneuver.getType());
        assertThat(CarIcon.APP_ICON).isEqualTo(maneuver.getIcon());
    }

    @Test
    public void createInstance_non_roundabout_invalid_type() {
        int typeHigh = 1000;
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(typeHigh).setIcon(CarIcon.APP_ICON).build());

        int typeLow = -1;
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(typeLow).setIcon(CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_non_roundabout_roundabout_type() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW).setIcon(
                        CarIcon.APP_ICON).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW).setIcon(
                        CarIcon.APP_ICON).build());
        assertThrows(IllegalArgumentException.class,
                () -> Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE)
                        .setIcon(CarIcon.APP_ICON)
                        .build());
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE)
                        .setIcon(CarIcon.APP_ICON)
                        .build());
    }

    @Test
    public void createInstance_roundabout_only_exit_number() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
        int roundaboutExitNumber = 2;

        Maneuver maneuver =
                Maneuver.builder(type)
                        .setRoundaboutExitNumber(roundaboutExitNumber)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        assertThat(type).isEqualTo(maneuver.getType());
        assertThat(roundaboutExitNumber).isEqualTo(maneuver.getRoundaboutExitNumber());
        assertThat(CarIcon.APP_ICON).isEqualTo(maneuver.getIcon());
    }

    @Test
    public void createInstance_roundabout_invalid_type() {
        int typeHigh = 1000;
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(typeHigh)
                                .setRoundaboutExitNumber(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());

        int typeLow = -1;
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(typeLow).setRoundaboutExitNumber(1).setIcon(
                                CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_roundabout_non_roundabout_type() {
        int type = TYPE_STRAIGHT;
        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(type).setRoundaboutExitNumber(1).setIcon(
                        CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_roundabout_invalid_exit() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
        int roundaboutExitNumber = 0;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(type)
                                .setRoundaboutExitNumber(roundaboutExitNumber)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
    }

    @Test
    public void createInstance_roundabout_with_angle() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE;
        int roundaboutExitNumber = 3;
        int roundaboutExitAngle = 270;

        Maneuver maneuver =
                Maneuver.builder(type)
                        .setRoundaboutExitNumber(roundaboutExitNumber)
                        .setRoundaboutExitAngle(roundaboutExitAngle)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        assertThat(type).isEqualTo(maneuver.getType());
        assertThat(roundaboutExitNumber).isEqualTo(maneuver.getRoundaboutExitNumber());
        assertThat(roundaboutExitAngle).isEqualTo(maneuver.getRoundaboutExitAngle());
        assertThat(CarIcon.APP_ICON).isEqualTo(maneuver.getIcon());
    }

    @Test
    public void createInstance_roundabout_with_angle_invalid_type() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(TYPE_STRAIGHT)
                                .setRoundaboutExitNumber(1)
                                .setRoundaboutExitAngle(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(TYPE_ROUNDABOUT_ENTER_CW)
                                .setRoundaboutExitNumber(1)
                                .setRoundaboutExitAngle(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
                                .setRoundaboutExitNumber(1)
                                .setRoundaboutExitAngle(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
    }

    @Test
    public void createInstance_invalid_carIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));

        assertThrows(
                IllegalArgumentException.class,
                () -> Maneuver.builder(TYPE_STRAIGHT).setIcon(carIcon).build());
    }

    @Test
    public void equals() {
        Maneuver maneuver = Maneuver.builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build();

        assertThat(Maneuver.builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build())
                .isEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentType() {
        Maneuver maneuver = Maneuver.builder(TYPE_DESTINATION_LEFT).setIcon(
                CarIcon.APP_ICON).build();

        assertThat(Maneuver.builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build())
                .isNotEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentImage() {
        Maneuver maneuver = Maneuver.builder(TYPE_DESTINATION_LEFT).setIcon(
                CarIcon.APP_ICON).build();

        assertThat(Maneuver.builder(TYPE_DESTINATION_LEFT).setIcon(CarIcon.ALERT).build())
                .isNotEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentRoundaboutExit() {
        Maneuver maneuver =
                Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
                        .setRoundaboutExitNumber(1)
                        .setIcon(CarIcon.APP_ICON)
                        .build();

        assertThat(
                Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
                        .setRoundaboutExitNumber(2)
                        .setIcon(CarIcon.APP_ICON)
                        .build())
                .isNotEqualTo(maneuver);
    }
}
