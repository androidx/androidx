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

import static androidx.car.app.model.CarColor.BLUE;
import static androidx.car.app.model.CarColor.DEFAULT;
import static androidx.car.app.model.CarColor.GREEN;
import static androidx.car.app.model.CarIcon.BACK;
import static androidx.car.app.model.CarIcon.TYPE_BACK;
import static androidx.car.app.model.CarIcon.TYPE_CUSTOM;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/** Tests for {@link CarIcon}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class CarIconTest {
    private IconCompat mIcon;

    @Before
    public void setup() {
        mIcon =
                IconCompat.createWithResource(
                        ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1);
    }

    @Test
    public void of() {
        CarIcon carIcon = CarIcon.of(mIcon);

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isNull();
        assertThat(carIcon.getIcon()).isEqualTo(mIcon);
    }

    @Test
    public void build_withTint() {
        CarIcon carIcon = CarIcon.builder(mIcon).setTint(BLUE).build();

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isEqualTo(BLUE);
        assertThat(carIcon.getIcon()).isEqualTo(mIcon);
    }

    @Test
    public void build_noTint() {
        CarIcon carIcon = CarIcon.builder(mIcon).build();

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isNull();
        assertThat(mIcon).isEqualTo(carIcon.getIcon());
    }

    @Test
    public void newBuilder_fromStandard() {
        CarIcon carIcon = BACK.newBuilder().setTint(GREEN).build();

        assertThat(carIcon.getType()).isEqualTo(TYPE_BACK);
        assertThat(carIcon.getTint()).isEqualTo(GREEN);
        assertThat(carIcon.getIcon()).isEqualTo(BACK.getIcon());
    }

    @Test
    public void standard_defaultTint() {
        assertThat(BACK.getTint()).isEqualTo(DEFAULT);
    }

    // TODO(shiufai): Add content uri equality test once we support content URI.
    @Test
    public void icon_from_uri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();

        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isNull();
        assertThat(carIcon.getIcon().getType()).isEqualTo(IconCompat.TYPE_URI);
    }

    @Test
    public void custom_icon_unsupported_scheme() {
        // Create an icon URI with "file://" scheme.
        Uri iconUri = Uri.fromFile(new File("foo/bar"));

        assertThrows(
                IllegalArgumentException.class,
                () -> CarIcon.builder(IconCompat.createWithContentUri(iconUri)));
    }

    @Test
    public void custom_icon_unsupported_types() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CarIcon.builder(IconCompat.createWithAdaptiveBitmapContentUri("foo/bar")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CarIcon.builder(IconCompat.createWithData(new byte[0], 1, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CarIcon.builder(
                                IconCompat.createWithAdaptiveBitmap(
                                        Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8))));
    }

    @Test
    public void equals() {
        assertThat(BACK.equals(BACK)).isTrue();
        CarIcon carIcon = CarIcon.of(mIcon);

        assertThat(
                CarIcon.of(
                        IconCompat.createWithResource(
                                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1)))
                .isEqualTo(carIcon);
    }

    @Test
    public void notEquals() {
        assertThat(BACK.newBuilder().setTint(GREEN).build()).isNotEqualTo(BACK);
    }
}
