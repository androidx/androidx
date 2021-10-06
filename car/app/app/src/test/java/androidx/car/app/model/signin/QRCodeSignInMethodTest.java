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

package androidx.car.app.model.signin;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.car.app.annotations.ExperimentalCarApi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link QRCodeSignInMethod}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class QRCodeSignInMethodTest {
    private final Uri mUri = Uri.parse("http://www.youtube.com/watch?v=dQw4w9WgXcQ");

    @OptIn(markerClass = ExperimentalCarApi.class)
    @Test
    public void create_defaultValues() {
        QRCodeSignInMethod signIn = new QRCodeSignInMethod(mUri);
        assertThat(signIn.getUri()).isEqualTo(mUri);
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    @Test
    public void equals() {
        QRCodeSignInMethod signIn = new QRCodeSignInMethod(mUri);
        assertThat(signIn).isEqualTo(new QRCodeSignInMethod(mUri));
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    @Test
    public void notEquals_differentUri() {
        QRCodeSignInMethod signIn = new QRCodeSignInMethod(mUri);
        assertThat(signIn).isNotEqualTo(new QRCodeSignInMethod(Uri.parse("google.com")));
    }
}
