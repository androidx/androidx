/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import static com.google.common.truth.Truth.assertThat;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        instrumentedPackages = { "androidx.camera.core.impl.utils" })
public class ExifTest {
    private static final InputStream FAKE_INPUT_STREAM =
            new InputStream() {
                @Override
                public int read() {
                    return 0;
                }
            };
    private Exif mExif;

    @Before
    public void setup() throws Exception {
        ShadowLog.stream = System.out;
        mExif = Exif.createFromInputStream(FAKE_INPUT_STREAM);
    }

    @Test
    public void defaultsAreExpectedValues() {
        assertThat(mExif.getRotation()).isEqualTo(0);
        assertThat(mExif.isFlippedHorizontally()).isFalse();
        assertThat(mExif.isFlippedVertically()).isFalse();
        assertThat(mExif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
        assertThat(mExif.getLocation()).isNull();
        assertThat(mExif.getDescription()).isNull();
    }

    @Test
    public void rotateProducesCorrectRotation() {
        assertThat(mExif.getRotation()).isEqualTo(0);
        mExif.rotate(90);
        assertThat(mExif.getRotation()).isEqualTo(90);
        mExif.rotate(90);
        assertThat(mExif.getRotation()).isEqualTo(180);
        mExif.rotate(90);
        assertThat(mExif.getRotation()).isEqualTo(270);
        mExif.rotate(90);
        assertThat(mExif.getRotation()).isEqualTo(0);
        mExif.rotate(-90);
        assertThat(mExif.getRotation()).isEqualTo(270);
        mExif.rotate(360);
        assertThat(mExif.getRotation()).isEqualTo(270);
        mExif.rotate(500 * 360 - 90);
        assertThat(mExif.getRotation()).isEqualTo(180);
    }

    @Test
    public void orientationCanBeSet() {
        int setOrientation = 90;

        mExif.setOrientation(setOrientation);

        assertThat(mExif.getOrientation()).isEqualTo(setOrientation);
    }

    @Test
    public void flipHorizontallyWillToggle() {
        assertThat(mExif.isFlippedHorizontally()).isFalse();
        mExif.flipHorizontally();
        assertThat(mExif.isFlippedHorizontally()).isTrue();
        mExif.flipHorizontally();
        assertThat(mExif.isFlippedHorizontally()).isFalse();
    }

    @Test
    public void flipVerticallyWillToggle() {
        assertThat(mExif.isFlippedVertically()).isFalse();
        mExif.flipVertically();
        assertThat(mExif.isFlippedVertically()).isTrue();
        mExif.flipVertically();
        assertThat(mExif.isFlippedVertically()).isFalse();
    }

    @Test
    public void flipAndRotateUpdatesHorizontalAndVerticalFlippedState() {
        assertThat(mExif.getRotation()).isEqualTo(0);
        assertThat(mExif.isFlippedHorizontally()).isFalse();
        assertThat(mExif.isFlippedVertically()).isFalse();

        mExif.rotate(-90);
        assertThat(mExif.getRotation()).isEqualTo(270);

        mExif.flipHorizontally();
        assertThat(mExif.getRotation()).isEqualTo(90);
        assertThat(mExif.isFlippedVertically()).isTrue();

        mExif.flipVertically();
        assertThat(mExif.getRotation()).isEqualTo(90);
        assertThat(mExif.isFlippedHorizontally()).isFalse();
        assertThat(mExif.isFlippedVertically()).isFalse();

        mExif.rotate(90);
        assertThat(mExif.getRotation()).isEqualTo(180);

        mExif.flipVertically();
        assertThat(mExif.getRotation()).isEqualTo(0);
        assertThat(mExif.isFlippedHorizontally()).isTrue();
        assertThat(mExif.isFlippedVertically()).isFalse();

        mExif.flipHorizontally();
        assertThat(mExif.getRotation()).isEqualTo(0);
        assertThat(mExif.isFlippedHorizontally()).isFalse();
        assertThat(mExif.isFlippedVertically()).isFalse();
    }

    @Test
    public void timestampCanBeAttachedAndRemoved() {
        assertThat(mExif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        mExif.attachTimestamp();
        assertThat(mExif.getTimestamp()).isNotEqualTo(Exif.INVALID_TIMESTAMP);

        mExif.removeTimestamp();
        assertThat(mExif.getTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
    }

    @Test
    public void attachedTimestampUsesSystemWallTime() {
        long beforeTimestamp = SystemClock.uptimeMillis();
        ShadowSystemClock.advanceBy(Duration.ofMillis(100));

        mExif.attachTimestamp();
        ShadowSystemClock.advanceBy(Duration.ofMillis(100));
        long afterTimestamp = SystemClock.uptimeMillis();

        // Check that the attached timestamp is in the closed range [beforeTimestamp,
        // afterTimestamp].
        long attachedTimestamp = mExif.getTimestamp();
        assertThat(attachedTimestamp).isAtLeast(beforeTimestamp);
        assertThat(attachedTimestamp).isAtMost(afterTimestamp);
    }

    @Test
    public void locationCanBeAttachedAndRemoved() {
        assertThat(mExif.getLocation()).isNull();

        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        mExif.attachLocation(location);
        assertThat(location.toString()).isEqualTo(mExif.getLocation().toString());

        mExif.removeLocation();
        assertThat(mExif.getLocation()).isNull();
    }

    @Test
    public void locationWithAltitudeCanBeAttached() {
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setAltitude(5.0);
        mExif.attachLocation(location);
        assertThat(location.toString()).isEqualTo(mExif.getLocation().toString());
    }

    @Test
    public void locationWithSpeedCanBeAttached() {
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setSpeed(5.0f);
        mExif.attachLocation(location);
        // Location loses precision when set through attachLocation(), so check the locations are
        // roughly equal first
        Location exifLocation = mExif.getLocation();
        assertThat(location.getSpeed()).isWithin(0.01f).of(exifLocation.getSpeed());
    }

    @Test
    public void descriptionCanBeAttachedAndRemoved() {
        assertThat(mExif.getDescription()).isNull();

        mExif.setDescription("Hello World");
        assertThat(mExif.getDescription()).isEqualTo("Hello World");

        mExif.setDescription(null);
        assertThat(mExif.getDescription()).isNull();
    }

    @Test
    public void saveUpdatesLastModifiedTimestampUnlessRemoved() {
        assertThat(mExif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        try {
            mExif.save();
        } catch (IOException e) {
            // expected
        }

        assertThat(mExif.getLastModifiedTimestamp()).isNotEqualTo(Exif.INVALID_TIMESTAMP);

        // removeTimestamp should also be clearing the last modified timestamp
        mExif.removeTimestamp();
        assertThat(mExif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);

        // Even when saving again
        try {
            mExif.save();
        } catch (IOException e) {
            // expected
        }

        assertThat(mExif.getLastModifiedTimestamp()).isEqualTo(Exif.INVALID_TIMESTAMP);
    }

    @Test
    public void toStringProducesNonNullString() {
        assertThat(mExif.toString()).isNotNull();
        mExif.setDescription("Hello World");
        mExif.attachTimestamp();
        Location location = new Location("TEST");
        location.setLatitude(22.3);
        location.setLongitude(114);
        location.setTime(System.currentTimeMillis() / 1000 * 1000);
        location.setAltitude(5.0);
        mExif.attachLocation(location);
        assertThat(mExif.toString()).isNotNull();
    }
}
