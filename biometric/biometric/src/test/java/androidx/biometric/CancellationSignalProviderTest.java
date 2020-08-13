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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@LargeTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CancellationSignalProviderTest {
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
        final android.os.CancellationSignal cancellationSignal =
                mock(android.os.CancellationSignal.class);
        final CancellationSignalProvider.Injector injector =
                new CancellationSignalProvider.Injector() {
                    @Override
                    @NonNull
                    public android.os.CancellationSignal getBiometricCancellationSignal() {
                        return cancellationSignal;
                    }

                    @Override
                    @NonNull
                    public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                        return mock(androidx.core.os.CancellationSignal.class);
                    }
                };
        final CancellationSignalProvider provider = new CancellationSignalProvider(injector);

        assertThat(provider.getBiometricCancellationSignal()).isEqualTo(cancellationSignal);

        provider.cancel();

        verify(cancellationSignal).cancel();
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
        final androidx.core.os.CancellationSignal cancellationSignal =
                mock(androidx.core.os.CancellationSignal.class);
        final CancellationSignalProvider.Injector injector =
                new CancellationSignalProvider.Injector() {
                    @Override
                    @NonNull
                    public android.os.CancellationSignal getBiometricCancellationSignal() {
                        return mock(android.os.CancellationSignal.class);
                    }

                    @Override
                    @NonNull
                    public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                        return  cancellationSignal;
                    }
                };
        final CancellationSignalProvider provider = new CancellationSignalProvider(injector);

        assertThat(provider.getFingerprintCancellationSignal()).isEqualTo(cancellationSignal);

        provider.cancel();

        verify(cancellationSignal).cancel();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    public void testBothCancellationSignals_ReceiveCancel() {
        final android.os.CancellationSignal biometricCancellationSignal =
                mock(android.os.CancellationSignal.class);
        final androidx.core.os.CancellationSignal fingerprintCancellationSignal =
                mock(androidx.core.os.CancellationSignal.class);
        final CancellationSignalProvider.Injector injector =
                new CancellationSignalProvider.Injector() {
                    @Override
                    @NonNull
                    public android.os.CancellationSignal getBiometricCancellationSignal() {
                        return biometricCancellationSignal;
                    }

                    @Override
                    @NonNull
                    public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                        return fingerprintCancellationSignal;
                    }
                };
        final CancellationSignalProvider provider = new CancellationSignalProvider(injector);

        assertThat(provider.getBiometricCancellationSignal())
                .isEqualTo(biometricCancellationSignal);
        assertThat(provider.getFingerprintCancellationSignal())
                .isEqualTo(fingerprintCancellationSignal);

        provider.cancel();

        verify(biometricCancellationSignal).cancel();
        verify(fingerprintCancellationSignal).cancel();
    }
}
