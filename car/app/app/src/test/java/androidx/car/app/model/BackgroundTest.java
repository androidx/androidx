/*
 * Copyright 2026 The Android Open Source Project
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

import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for the {@link Background} class. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class BackgroundTest {
    @Test
    public void setAndGetImage() {
        CarIcon image = new CarIcon.Builder(
                IconCompat.createWithContentUri(Uri.parse("content://test"))).build();
        Background background = new Background.Builder().setImage(image).build();
        assertThat(background.getImage()).isEqualTo(image);
    }

    @Test
    public void setImage_notCustom_throwsIllegalArgumentException() {
        org.junit.Assert.assertThrows(IllegalArgumentException.class,
                () -> new Background.Builder().setImage(CarIcon.APP_ICON));
    }

    @Test
    public void build_noImage_throwsIllegalStateException() {
        org.junit.Assert.assertThrows(IllegalStateException.class,
                () -> new Background.Builder().build());
    }

    @Test
    public void equals() {
        CarIcon image = new CarIcon.Builder(
                IconCompat.createWithContentUri(Uri.parse("content://test"))).build();
        Background background = new Background.Builder().setImage(image).build();

        assertThat(new Background.Builder().setImage(image).build()).isEqualTo(background);
    }

    @Test
    public void notEquals_differentImage() {
        CarIcon image1 = new CarIcon.Builder(
                IconCompat.createWithContentUri(Uri.parse("content://test1"))).build();
        Background background = new Background.Builder().setImage(image1).build();

        CarIcon image2 = new CarIcon.Builder(
                IconCompat.createWithContentUri(Uri.parse("content://test2"))).build();

        assertThat(new Background.Builder().setImage(image2).build()).isNotEqualTo(background);
    }
}
