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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Maneuver}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ManeuverTest {

    @Test
    public void createInstance_non_roundabout() {
        int type = TYPE_STRAIGHT;

        Maneuver maneuver = new Maneuver.Builder(type).setIcon(CarIcon.APP_ICON).build();
        assertThat(type).isEqualTo(maneuver.getType());
        assertThat(CarIcon.APP_ICON).isEqualTo(maneuver.getIcon());
    }

    @Test
    public void createInstance_non_roundabout_invalid_type() {
        int typeHigh = 1000;
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(typeHigh).setIcon(CarIcon.APP_ICON).build());

        int typeLow = -1;
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(typeLow).setIcon(CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_non_roundabout_roundabout_type() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW).setIcon(
                        CarIcon.APP_ICON).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW).setIcon(
                        CarIcon.APP_ICON).build());
        assertThrows(IllegalArgumentException.class,
                () -> new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE)
                        .setIcon(CarIcon.APP_ICON)
                        .build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE)
                        .setIcon(CarIcon.APP_ICON)
                        .build());
    }

    @Test
    public void createInstance_roundabout_only_exit_number() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
        int roundaboutExitNumber = 2;

        Maneuver maneuver =
                new Maneuver.Builder(type)
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
                        new Maneuver.Builder(typeHigh)
                                .setRoundaboutExitNumber(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());

        int typeLow = -1;
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Maneuver.Builder(typeLow).setRoundaboutExitNumber(1).setIcon(
                                CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_roundabout_non_roundabout_type() {
        int type = TYPE_STRAIGHT;
        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(type).setRoundaboutExitNumber(1).setIcon(
                        CarIcon.APP_ICON).build());
    }

    @Test
    public void createInstance_roundabout_invalid_exit() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;
        int roundaboutExitNumber = 0;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Maneuver.Builder(type)
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
                new Maneuver.Builder(type)
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
    public void createInstance_roundabout_with_angle_optionalExitNumber() {
        int type = TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE;
        int roundaboutExitAngle = 270;

        Maneuver maneuver =
                new Maneuver.Builder(type)
                        .setRoundaboutExitAngle(roundaboutExitAngle)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        assertThat(type).isEqualTo(maneuver.getType());
        assertThat(roundaboutExitAngle).isEqualTo(maneuver.getRoundaboutExitAngle());
        assertThat(CarIcon.APP_ICON).isEqualTo(maneuver.getIcon());
    }

    @Test
    public void createInstance_roundabout_with_angle_invalid_type() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                                .setRoundaboutExitAngle(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_CW)
                                .setRoundaboutExitNumber(1)
                                .setRoundaboutExitAngle(1)
                                .setIcon(CarIcon.APP_ICON)
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
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
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Maneuver.Builder(TYPE_STRAIGHT).setIcon(carIcon).build());
    }

    @Test
    public void equals() {
        Maneuver maneuver = new Maneuver.Builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build();

        assertThat(new Maneuver.Builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build())
                .isEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentType() {
        Maneuver maneuver = new Maneuver.Builder(TYPE_DESTINATION_LEFT).setIcon(
                CarIcon.APP_ICON).build();

        assertThat(new Maneuver.Builder(TYPE_STRAIGHT).setIcon(CarIcon.APP_ICON).build())
                .isNotEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentImage() {
        Maneuver maneuver = new Maneuver.Builder(TYPE_DESTINATION_LEFT).setIcon(
                CarIcon.APP_ICON).build();

        assertThat(new Maneuver.Builder(TYPE_DESTINATION_LEFT).setIcon(CarIcon.ALERT).build())
                .isNotEqualTo(maneuver);
    }

    @Test
    public void notEquals_differentRoundaboutExit() {
        Maneuver maneuver =
                new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
                        .setRoundaboutExitNumber(1)
                        .setIcon(CarIcon.APP_ICON)
                        .build();

        assertThat(
                new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW)
                        .setRoundaboutExitNumber(2)
                        .setIcon(CarIcon.APP_ICON)
                        .build())
                .isNotEqualTo(maneuver);
    }
}
