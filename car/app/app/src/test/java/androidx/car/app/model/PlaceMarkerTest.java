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

package androidx.car.app.model;

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.car.app.TestUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PlaceMarker}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PlaceMarkerTest {

    @Test
    public void create_throws_invalidLabelLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlaceMarker.Builder().setLabel("Blah").build());
    }

    @Test
    public void setColor_withImageTypeIcon_throws() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        assertThrows(
                IllegalStateException.class,
                () -> new PlaceMarker.Builder()
                        .setIcon(icon, PlaceMarker.TYPE_IMAGE)
                        .setColor(CarColor.SECONDARY)
                        .build());
    }

    @Test
    public void create_throws_invalidCarIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlaceMarker.Builder().setIcon(carIcon, PlaceMarker.TYPE_IMAGE));
    }

    @Test
    public void createInstance() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        PlaceMarker marker1 = new PlaceMarker.Builder()
                .setIcon(icon, PlaceMarker.TYPE_ICON)
                .setLabel("foo")
                .setColor(CarColor.SECONDARY)
                .build();
        assertThat(marker1.getIcon()).isEqualTo(icon);
        assertThat(marker1.getIconType()).isEqualTo(PlaceMarker.TYPE_ICON);
        assertThat(marker1.getColor()).isEqualTo(CarColor.SECONDARY);
        assertThat(marker1.getLabel().toString()).isEqualTo("foo");
    }

    @Test
    public void equals() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        PlaceMarker marker = new PlaceMarker.Builder()
                .setIcon(carIcon, PlaceMarker.TYPE_ICON)
                .setLabel("foo")
                .setColor(CarColor.SECONDARY)
                .build();

        assertThat(new PlaceMarker.Builder()
                .setIcon(carIcon, PlaceMarker.TYPE_ICON)
                .setLabel("foo")
                .setColor(CarColor.SECONDARY)
                .build())
                .isEqualTo(marker);
    }

    @Test
    public void notEquals_differentIcon() {
        PlaceMarker marker = new PlaceMarker.Builder().setIcon(BACK,
                PlaceMarker.TYPE_IMAGE).build();

        assertThat(new PlaceMarker.Builder().setIcon(ALERT, PlaceMarker.TYPE_IMAGE).build())
                .isNotEqualTo(marker);
    }

    @Test
    public void notEquals_differentLabel() {
        PlaceMarker marker = new PlaceMarker.Builder().setLabel("foo").build();

        assertThat(new PlaceMarker.Builder().setLabel("bar").build()).isNotEqualTo(marker);
    }

    @Test
    public void notEquals_differentBackgroundColor() {
        PlaceMarker marker = new PlaceMarker.Builder().setColor(CarColor.SECONDARY).build();

        assertThat(new PlaceMarker.Builder().setColor(CarColor.BLUE).build()).isNotEqualTo(marker);
    }
}
