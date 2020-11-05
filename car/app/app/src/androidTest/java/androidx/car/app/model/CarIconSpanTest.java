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
import android.net.Uri;

import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link CarIconSpan}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class CarIconSpanTest {
    private IconCompat mIcon;

    @Before
    public void setup() {
        mIcon =
                IconCompat.createWithResource(
                        ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1);
    }

    @Test
    public void constructor() {
        CarIcon carIcon = CarIcon.of(mIcon);
        CarIconSpan span = CarIconSpan.create(carIcon);

        assertThat(span.getIcon()).isEqualTo(carIcon);
    }

    @Test
    public void constructor_invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));
        assertThrows(IllegalArgumentException.class, () -> CarIconSpan.create(carIcon));
    }

    @Test
    public void equals() {
        CarIcon carIcon = CarIcon.of(mIcon);
        CarIconSpan span1 = CarIconSpan.create(carIcon);
        CarIconSpan span2 = CarIconSpan.create(carIcon);

        assertThat(span2).isEqualTo(span1);
    }

    @Test
    public void notEquals() {
        CarIcon carIcon = CarIcon.of(mIcon);
        CarIconSpan span1 = CarIconSpan.create(carIcon);
        CarIconSpan span2 = CarIconSpan.create(CarIcon.ALERT);

        assertThat(span2).isNotEqualTo(span1);
    }
}
