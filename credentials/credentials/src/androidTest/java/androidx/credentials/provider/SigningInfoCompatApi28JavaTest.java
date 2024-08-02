/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.SigningInfo;

import androidx.core.os.BuildCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
public class SigningInfoCompatApi28JavaTest {
    private SigningInfo mSigningInfo;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo =
                context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );
        assertNotNull(packageInfo.signingInfo);
        mSigningInfo = packageInfo.signingInfo;
    }

    @Test
    public void constructorAndGetters_success() {
        SigningInfoCompat signingInfoCompat = SigningInfoCompat.fromSigningInfo(mSigningInfo);

        assertThat(signingInfoCompat.getSigningCertificateHistory())
                .containsExactlyElementsIn(mSigningInfo.getSigningCertificateHistory()).inOrder();
        assertThat(signingInfoCompat.getApkContentsSigners())
                .containsExactlyElementsIn(mSigningInfo.getApkContentsSigners()).inOrder();
        if (BuildCompat.isAtLeastV()) {
            if (!signingInfoCompat.getPublicKeys().isEmpty()) {
                assertThat(signingInfoCompat.getPublicKeys()).containsExactlyElementsIn(
                        mSigningInfo.getPublicKeys()).inOrder();
            }
            assertThat(signingInfoCompat.getSchemeVersion())
                    .isEqualTo(mSigningInfo.getSchemeVersion());
        }
        assertThat(signingInfoCompat.hasPastSigningCertificates())
                .isEqualTo(mSigningInfo.hasPastSigningCertificates());
        assertThat(signingInfoCompat.hasMultipleSigners())
                .isEqualTo(mSigningInfo.hasMultipleSigners());
    }
}
