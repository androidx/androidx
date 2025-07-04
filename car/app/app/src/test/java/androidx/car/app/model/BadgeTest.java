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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BadgeTest {
    @Test
    public void build_withDotAndIcon() {
        Badge b = new Badge.Builder().setHasDot(true).setIcon(CarIcon.ALERT).build();

        assertThat(b.hasDot()).isEqualTo(true);
        assertThat(b.getIcon()).isEqualTo(CarIcon.ALERT);
    }

    @Test
    public void build_withIconAndIconBackgroundColor() {
        Badge b = new Badge.Builder()
                .setIcon(CarIcon.ALERT)
                .setIconBackgroundColor(CarColor.GREEN)
                .build();

        assertThat(b.getIcon()).isEqualTo(CarIcon.ALERT);
        assertThat(b.getIconBackgroundColor()).isEqualTo(CarColor.GREEN);
    }

    @Test
    public void build_withDotAndDotColor() {
        Badge b = new Badge.Builder().setHasDot(true).setDotColor(CarColor.PRIMARY).build();

        assertThat(b.hasDot()).isEqualTo(true);
        assertThat(b.getDotColor()).isEqualTo(CarColor.PRIMARY);
    }

    @Test
    public void build_emptyThrowsException() {
        assertThrows(IllegalStateException.class, () -> new Badge.Builder().build());
    }

    @Test
    public void build_setHasDotFalse_ThrowsException() {
        assertThrows(IllegalStateException.class,
                () -> new Badge.Builder().setHasDot(false).build());
    }

    @Test
    public void build_setDotColorWithoutDot_ThrowsException() {
        assertThrows(IllegalStateException.class,
                () -> new Badge.Builder()
                        .setIcon(CarIcon.ALERT)
                        .setDotColor(CarColor.PRIMARY).build());
    }

    @Test
    public void build_setIconBackgroundColorWithoutIcon_ThrowsException() {
        assertThrows(IllegalStateException.class,
                () -> new Badge.Builder()
                        .setHasDot(true)
                        .setIconBackgroundColor(CarColor.PRIMARY).build());
    }

    @Test
    public void equals() {
        Badge b1 = new Badge.Builder()
                .setHasDot(true)
                .setDotColor(CarColor.PRIMARY)
                .setIcon(CarIcon.ALERT).build();
        Badge b2 = new Badge.Builder()
                .setHasDot(true)
                .setDotColor(CarColor.PRIMARY)
                .setIcon(CarIcon.ALERT).build();

        assertThat(b1.equals(b2)).isTrue();
    }

    @Test
    public void notEquals_differentProperty() {
        Badge b1 = new Badge.Builder().setHasDot(true).build();
        Badge b2 = new Badge.Builder().setIcon(CarIcon.ALERT).build();

        assertThat(b1.equals(b2)).isFalse();
    }

    @Test
    public void notEquals_differentDotColor() {
        Badge b1 = new Badge.Builder().setHasDot(true).setDotColor(CarColor.BLUE).build();
        Badge b2 = new Badge.Builder().setHasDot(true).setDotColor(CarColor.RED).build();

        assertThat(b1.equals(b2)).isFalse();
    }

    @Test
    public void notEquals_differentIcons() {
        Badge b1 = new Badge.Builder().setIcon(CarIcon.ALERT).build();
        Badge b2 = new Badge.Builder().setIcon(CarIcon.ERROR).build();

        assertThat(b1.equals(b2)).isFalse();
    }

    @Test
    public void string() {
        Badge b = new Badge.Builder().setHasDot(true).build();

        assertThat(b.toString()).isEqualTo(
                "[hasDot: true, dotColor: null, icon: null, iconBackgroundColor: null]");
    }
}
