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
import android.content.pm.Signature;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(maxSdkVersion = 27)
public class SigningInfoCompatApi21JavaTest {
    private List<Signature> mSignatures;

    @Before
    @SuppressWarnings("deprecation")
    public void setup() throws PackageManager.NameNotFoundException {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo =
                context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNATURES
                );
        assertNotNull(packageInfo.signatures);
        mSignatures = Arrays.asList(packageInfo.signatures);
    }

    @Test
    public void constructorAndGetters_success() {
        SigningInfoCompat signingInfoCompat = SigningInfoCompat.fromSignatures(mSignatures);

        assertThat(signingInfoCompat.getSigningCertificateHistory()).containsExactlyElementsIn(
                mSignatures).inOrder();
        assertThat(signingInfoCompat.getApkContentsSigners()).isEmpty();
        assertThat(signingInfoCompat.hasPastSigningCertificates()).isFalse();
        assertThat(signingInfoCompat.hasMultipleSigners()).isFalse();
    }
}
