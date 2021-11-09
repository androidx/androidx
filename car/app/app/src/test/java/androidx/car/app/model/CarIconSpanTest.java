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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.car.app.TestUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarIconSpan}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarIconSpanTest {
    private IconCompat mIcon;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mIcon = IconCompat.createWithResource(
                context, TestUtils.getTestDrawableResId(context, "ic_test_1"));
    }

    @Test
    public void constructor() {
        CarIcon carIcon = new CarIcon.Builder(mIcon).build();
        CarIconSpan span = CarIconSpan.create(carIcon);

        assertThat(span.getIcon()).isEqualTo(carIcon);
    }

    @Test
    public void constructor_invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(IllegalArgumentException.class, () -> CarIconSpan.create(carIcon));
    }

    @Test
    public void equals() {
        CarIcon carIcon = new CarIcon.Builder(mIcon).build();
        CarIconSpan span1 = CarIconSpan.create(carIcon);
        CarIconSpan span2 = CarIconSpan.create(carIcon);

        assertThat(span2).isEqualTo(span1);
    }

    @Test
    public void notEquals() {
        CarIcon carIcon = new CarIcon.Builder(mIcon).build();
        CarIconSpan span1 = CarIconSpan.create(carIcon);
        CarIconSpan span2 = CarIconSpan.create(CarIcon.ALERT);

        assertThat(span2).isNotEqualTo(span1);
    }
}
