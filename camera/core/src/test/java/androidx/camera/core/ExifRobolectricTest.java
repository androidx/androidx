/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ExifRobolectricTest {
    private static final InputStream FAKE_INPUT_STREAM =
            new InputStream() {
                @Override
                public int read() throws IOException {
                    return 0;
                }
            };
    private Exif exif;

    @Before
    public void setup() throws Exception {
        ShadowLog.stream = System.out;
        exif = Exif.createFromInputStream(FAKE_INPUT_STREAM);
    }

    @Test
    public void defaultsAreExpectedValues() {
        assertThat(exif.getRotation()).isEqualTo(0);
        assertThat(exif.isFlippedHorizontally()).isFalse();
        assertThat(exif.isFlippedVertically()).isFalse();
        assertThat(exif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
        assertThat(exif.getLocation()).isNull();
        assertThat(exif.getDescription()).isNull();
    }

    @Test
    public void rotateProducesCorrectRotation() {
        assertThat(exif.getRotation()).isEqualTo(0);
        exif.rotate(90);
        assertThat(exif.getRotation()).isEqualTo(90);
        exif.rotate(90);
        assertThat(exif.getRotation()).isEqualTo(180);
        exif.rotate(90);
        assertThat(exif.getRotation()).isEqualTo(270);
        exif.rotate(90);
        assertThat(exif.getRotation()).isEqualTo(0);
        exif.rotate(-90);
        assertThat(exif.getRotation()).isEqualTo(270);
        exif.rotate(360);
        assertThat(exif.getRotation()).isEqualTo(270);
        exif.rotate(500 * 360 - 90);
        assertThat(exif.getRotation()).isEqualTo(180);
    }

    @Test
    public void flipHorizontallyWillToggle() {
        assertThat(exif.isFlippedHorizontally()).isFalse();
        exif.flipHorizontally();
        assertThat(exif.isFlippedHorizontally()).isTrue();
        exif.flipHorizontally();
        assertThat(exif.isFlippedHorizontally()).isFalse();
    }

    @Test
    public void flipVerticallyWillToggle() {
        assertThat(exif.isFlippedVertically()).isFalse();
        exif.flipVertically();
        assertThat(exif.isFlippedVertically()).isTrue();
        exif.flipVertically();
        assertThat(exif.isFlippedVertically()).isFalse();
    }

    @Test
    public void flipAndRotateUpdatesHorizontalAndVerticalFlippedState() {
        assertThat(exif.getRotation()).isEqualTo(0);
        assertThat(exif.isFlippedHorizontally()).isFalse();
        assertThat(exif.isFlippedVertically()).isFalse();

        exif.rotate(-90);
        assertThat(exif.getRotation()).isEqualTo(270);

        exif.flipHorizontally();
        assertThat(exif.getRotation()).isEqualTo(90);
        assertThat(exif.isFlippedVertically()).isTrue();

        exif.flipVertically();
        assertThat(exif.getRotation()).isEqualTo(90);
        assertThat(exif.isFlippedHorizontally()).isFalse();
        assertThat(exif.isFlippedVertically()).isFalse();

        exif.rotate(90);
        assertThat(exif.getRotation()).isEqualTo(180);

        exif.flipVertically();
        assertThat(exif.getRotation()).isEqualTo(0);
        assertThat(exif.isFlippedHorizontally()).isTrue();
        assertThat(exif.isFlippedVertically()).isFalse();

        exif.flipHorizontally();
        assertThat(exif.getRotation()).isEqualTo(0);
        assertThat(exif.isFlippedHorizontally()).isFalse();
        assertThat(exif.isFlippedVertically()).isFalse();
    }

    @Test
    public void timestampCanBeAttachedAndRemoved() {
        assertThat(exif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        exif.attachTimestamp();
        assertThat(exif.getTimestamp()).isNotEqualTo(Exif.INVALID_TIMESTAMP);

        exif.removeTimestamp();
        assertThat(exif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
    }

    @Test
    public void attachedTimestampUsesSystemWallTime() {
        long beforeTimestamp = System.currentTimeMillis();

        // The Exif class is instrumented since it's in the androidx.* namespace.
        // Set the ShadowSystemClock to match the real system clock.
        ShadowSystemClock.setNanoTime(System.currentTimeMillis() * 1000 * 1000);
        exif.attachTimestamp();
        long afterTimestamp = System.currentTimeMillis();

        // Check that the attached timestamp is in the closed range [beforeTimestamp,
        // afterTimestamp].
        long attachedTimestamp = exif.getTimestamp();
        assertThat(attachedTimestamp).isAtLeast(beforeTimestamp);
        assertThat(attachedTimestamp).isAtMost(afterTimestamp);
    }

    @Test
    public void locationCanBeAttachedAndRemoved() {
        assertThat(exif.getLocation()).isNull();

        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        exif.attachLocation(location);
        assertThat(location.toString()).isEqualTo(exif.getLocation().toString());

        exif.removeLocation();
        assertThat(exif.getLocation()).isNull();
    }

    @Test
    public void locationWithAltitudeCanBeAttached() {
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setAltitude(5.0);
        exif.attachLocation(location);
        assertThat(location.toString()).isEqualTo(exif.getLocation().toString());
    }

    @Test
    public void locationWithSpeedCanBeAttached() {
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setSpeed(5.0f);
        exif.attachLocation(location);
        // Location loses precision when set through attachLocation(), so check the locations are
        // roughly equal first
        Location exifLocation = exif.getLocation();
        assertThat(location.getSpeed()).isWithin(0.01f).of(exifLocation.getSpeed());

        // Remove speed and compare the rest by string
        exifLocation.removeSpeed();
        location.removeSpeed();
        assertThat(location.toString()).isEqualTo(exifLocation.toString());
    }

    @Test
    public void descriptionCanBeAttachedAndRemoved() {
        assertThat(exif.getDescription()).isNull();

        exif.setDescription("Hello World");
        assertThat(exif.getDescription()).isEqualTo("Hello World");

        exif.setDescription(null);
        assertThat(exif.getDescription()).isNull();
    }

    @Test
    public void saveUpdatesLastModifiedTimestampUnlessRemoved() {
        assertThat(exif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        try {
            exif.save();
        } catch (IOException e) {
            // expected
        }

        assertThat(exif.getLastModifiedTimestamp()).isNotEqualTo(Exif.INVALID_TIMESTAMP);

        // removeTimestamp should also be clearing the last modified timestamp
        exif.removeTimestamp();
        assertThat(exif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        // Even when saving again
        try {
            exif.save();
        } catch (IOException e) {
            // expected
        }

        assertThat(exif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
    }

    @Test
    public void toStringProducesNonNullString() {
        assertThat(exif.toString()).isNotNull();
        exif.setDescription("Hello World");
        exif.attachTimestamp();
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setAltitude(5.0);
        exif.attachLocation(location);
        assertThat(exif.toString()).isNotNull();
    }
}
