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

package androidx.biometric.utils;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.KeyguardManager;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class KeyguardUtilsTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private Context mContext;
    @Mock
    private KeyguardManager mKeyguardManager;

    @Test
    public void testGetsKeyguardManager_OnApi23AndAbove() {
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(mKeyguardManager);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isEqualTo(mKeyguardManager);
    }

    @Test
    public void testGetKeyguardManager_HandlesNull_OnApi23AndAbove() {
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(null);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isNull();
    }

    @Test
    public void testIsDeviceSecuredWithCredential_HandlesNullKeyguardManager() {
        assertThat(KeyguardUtils.isDeviceSecuredWithCredential(mContext)).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsDeviceSecuredWithCredential_CorrectlyReturnsTrue_OnApi23AndAbove() {
        when(mContext.getSystemService(any(Class.class))).thenReturn(mKeyguardManager);
        when(mKeyguardManager.isDeviceSecure()).thenReturn(true);
        assertThat(KeyguardUtils.isDeviceSecuredWithCredential(mContext)).isTrue();
    }
}
