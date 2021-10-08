/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.connection;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarConnection}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarConnectionTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getType_projection() {
        assertThat(new CarConnection(mContext).getType()).isInstanceOf(
                CarConnectionTypeLiveData.class);
    }

    @Test
    public void getType_automotive() {
        shadowOf(mContext.getPackageManager()).setSystemFeature(FEATURE_AUTOMOTIVE, true);

        assertThat(new CarConnection(mContext).getType()).isInstanceOf(
                AutomotiveCarConnectionTypeLiveData.class);
    }
}
