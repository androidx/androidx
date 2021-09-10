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
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.car.app.TestUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.io.File;

/** Tests for {@link CarIcon}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarIconTest {
    private IconCompat mIcon;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mIcon = IconCompat.createWithResource(
                context, TestUtils.getTestDrawableResId(context, "ic_test_1"));
    }

    @Test
    public void build_withTint() {
        CarIcon carIcon = new CarIcon.Builder(mIcon).setTint(BLUE).build();

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isEqualTo(BLUE);
        assertThat(carIcon.getIcon()).isEqualTo(mIcon);
    }

    @Test
    public void build_noTint() {
        CarIcon carIcon = new CarIcon.Builder(mIcon).build();

        assertThat(carIcon.getType()).isEqualTo(TYPE_CUSTOM);
        assertThat(carIcon.getTint()).isNull();
        assertThat(mIcon).isEqualTo(carIcon.getIcon());
    }

    @Test
    public void newBuilder_fromStandard() {
        CarIcon carIcon = new CarIcon.Builder(BACK).setTint(GREEN).build();

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

        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();

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
                () -> new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)));
    }

    @Test
    public void custom_icon_unsupported_types() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CarIcon.Builder(
                        IconCompat.createWithAdaptiveBitmapContentUri("foo/bar")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CarIcon.Builder(IconCompat.createWithData(new byte[0], 1, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CarIcon.Builder(
                                IconCompat.createWithAdaptiveBitmap(
                                        Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8))));
    }

    @Test
    public void equals() {
        assertThat(BACK.equals(BACK)).isTrue();
        CarIcon carIcon = new CarIcon.Builder(mIcon).build();
        Context context = ApplicationProvider.getApplicationContext();

        assertThat(new CarIcon.Builder(IconCompat.createWithResource(
                context, TestUtils.getTestDrawableResId(context, "ic_test_1"))).build())
                .isEqualTo(carIcon);
    }

    @Test
    public void notEquals() {
        assertThat(new CarIcon.Builder(BACK).setTint(GREEN).build()).isNotEqualTo(BACK);
    }
}
