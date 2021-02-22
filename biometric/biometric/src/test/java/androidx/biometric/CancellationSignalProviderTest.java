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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.Build;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CancellationSignalProviderTest {
    @Mock private android.os.CancellationSignal mBiometricCancellationSignal;
    @Mock private androidx.core.os.CancellationSignal mFingerprintCancellationSignal;

    private CancellationSignalProvider.Injector mFieldMockInjector;
    private CancellationSignalProvider.Injector mNewMockInjector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFieldMockInjector = new CancellationSignalProvider.Injector() {
            @Override
            @NonNull
            public android.os.CancellationSignal getBiometricCancellationSignal() {
                return mBiometricCancellationSignal;
            }

            @Override
            @NonNull
            public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                return mFingerprintCancellationSignal;
            }
        };

        mNewMockInjector = new CancellationSignalProvider.Injector() {
            @Override
            @NonNull
            public android.os.CancellationSignal getBiometricCancellationSignal() {
                return mock(android.os.CancellationSignal.class);
            }

            @Override
            @NonNull
            public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                return mock(androidx.core.os.CancellationSignal.class);
            }
        };
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    public void testBiometricCancellationSignal_IsCached() {
        final CancellationSignalProvider provider = new CancellationSignalProvider();
        final android.os.CancellationSignal cancellationSignal =
                provider.getBiometricCancellationSignal();
        assertThat(provider.getBiometricCancellationSignal()).isEqualTo(cancellationSignal);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    public void testBiometricCancellationSignal_ReceivesCancel() {
        final CancellationSignalProvider provider =
                new CancellationSignalProvider(mFieldMockInjector);

        assertThat(provider.getBiometricCancellationSignal())
                .isEqualTo(mBiometricCancellationSignal);

        provider.cancel();

        verify(mBiometricCancellationSignal).cancel();
    }

    @Test
    public void testFingerprintCancellationSignal_IsCached() {
        final CancellationSignalProvider provider = new CancellationSignalProvider();
        final androidx.core.os.CancellationSignal cancellationSignal =
                provider.getFingerprintCancellationSignal();
        assertThat(provider.getFingerprintCancellationSignal()).isEqualTo(cancellationSignal);
    }

    @Test
    public void testFingerprintCancellationSignal_ReceivesCancel() {
        final CancellationSignalProvider provider =
                new CancellationSignalProvider(mFieldMockInjector);

        assertThat(provider.getFingerprintCancellationSignal())
                .isEqualTo(mFingerprintCancellationSignal);

        provider.cancel();

        verify(mFingerprintCancellationSignal).cancel();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    public void testBothCancellationSignals_ReceiveCancel() {
        final CancellationSignalProvider provider =
                new CancellationSignalProvider(mFieldMockInjector);

        assertThat(provider.getBiometricCancellationSignal())
                .isEqualTo(mBiometricCancellationSignal);
        assertThat(provider.getFingerprintCancellationSignal())
                .isEqualTo(mFingerprintCancellationSignal);

        provider.cancel();

        verify(mBiometricCancellationSignal).cancel();
        verify(mFingerprintCancellationSignal).cancel();
    }

    @Test
    public void testCancel_DoesNotCrash_WhenCancellationSignalsThrowNPE() {
        final CancellationSignalProvider provider =
                new CancellationSignalProvider(mNewMockInjector);

        final android.os.CancellationSignal biometricSignal =
                provider.getBiometricCancellationSignal();
        final androidx.core.os.CancellationSignal fingerprintSignal =
                provider.getFingerprintCancellationSignal();

        doThrow(NullPointerException.class).when(biometricSignal).cancel();
        doThrow(NullPointerException.class).when(fingerprintSignal).cancel();

        provider.cancel();

        assertThat(provider.getBiometricCancellationSignal()).isNotEqualTo(biometricSignal);
        assertThat(provider.getFingerprintCancellationSignal()).isNotEqualTo(fingerprintSignal);
    }
}
