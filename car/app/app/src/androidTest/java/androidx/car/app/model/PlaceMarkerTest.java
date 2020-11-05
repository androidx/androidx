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

import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PlaceMarker}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PlaceMarkerTest {

    @Test
    public void create_throws_invalidLabelLength() {
        assertThrows(IllegalArgumentException.class,
                () -> PlaceMarker.builder().setLabel("Blah").build());
    }

    @Test
    public void setColor_withImageTypeIcon_throws() {
        CarIcon icon =
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));
        assertThrows(
                IllegalStateException.class,
                () ->
                        PlaceMarker.builder()
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
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));
        assertThrows(
                IllegalArgumentException.class,
                () -> PlaceMarker.builder().setIcon(carIcon, PlaceMarker.TYPE_IMAGE));
    }

    @Test
    public void createInstance() {
        CarIcon icon =
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));
        PlaceMarker marker1 =
                PlaceMarker.builder()
                        .setIcon(icon, PlaceMarker.TYPE_ICON)
                        .setLabel("foo")
                        .setColor(CarColor.SECONDARY)
                        .build();
        assertThat(marker1.getIcon()).isEqualTo(icon);
        assertThat(marker1.getIconType()).isEqualTo(PlaceMarker.TYPE_ICON);
        assertThat(marker1.getColor()).isEqualTo(CarColor.SECONDARY);
        assertThat(marker1.getLabel().getText()).isEqualTo("foo");
    }

    @Test
    public void isDefaultMarker() {
        assertThat(PlaceMarker.isDefaultMarker(null)).isFalse();
        assertThat(PlaceMarker.isDefaultMarker(PlaceMarker.builder().setLabel("foo").build()))
                .isFalse();

        assertThat(PlaceMarker.isDefaultMarker(PlaceMarker.getDefault())).isTrue();
        assertThat(PlaceMarker.isDefaultMarker(PlaceMarker.builder().build())).isTrue();
    }

    @Test
    public void equals() {
        CarIcon carIcon =
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));
        PlaceMarker marker =
                PlaceMarker.builder()
                        .setIcon(carIcon, PlaceMarker.TYPE_ICON)
                        .setLabel("foo")
                        .setColor(CarColor.SECONDARY)
                        .build();

        assertThat(
                PlaceMarker.builder()
                        .setIcon(carIcon, PlaceMarker.TYPE_ICON)
                        .setLabel("foo")
                        .setColor(CarColor.SECONDARY)
                        .build())
                .isEqualTo(marker);
    }

    @Test
    public void notEquals_differentIcon() {
        PlaceMarker marker = PlaceMarker.builder().setIcon(BACK, PlaceMarker.TYPE_IMAGE).build();

        assertThat(PlaceMarker.builder().setIcon(ALERT, PlaceMarker.TYPE_IMAGE).build())
                .isNotEqualTo(marker);
    }

    @Test
    public void notEquals_differentLabel() {
        PlaceMarker marker = PlaceMarker.builder().setLabel("foo").build();

        assertThat(PlaceMarker.builder().setLabel("bar").build()).isNotEqualTo(marker);
    }

    @Test
    public void notEquals_differentBackgroundColor() {
        PlaceMarker marker = PlaceMarker.builder().setColor(CarColor.SECONDARY).build();

        assertThat(PlaceMarker.builder().setColor(CarColor.BLUE).build()).isNotEqualTo(marker);
    }
}
