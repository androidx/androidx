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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

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
public class KeyguardUtilsTest {
    @Mock private AccessibilityService mAccessibilityService;
    @Mock private Context mContext;
    @Mock private KeyguardManager mKeyguardManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testGetsKeyguardManager_OnApi23AndAbove() {
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(mKeyguardManager);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isEqualTo(mKeyguardManager);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void testGetsKeyguardManager_OnApi22AndBelow() {
        when(mContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(mKeyguardManager);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isEqualTo(mKeyguardManager);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testGetKeyguardManager_HandlesNull_OnApi23AndAbove() {
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(null);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isNull();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void testGetKeyguardManager_HandlesNull_OnApi22AndBelow() {
        when(mContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(null);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isNull();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void testGetKeyguardManager_HandlesWrongService_OnApi22AndBelow() {
        when(mContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(mAccessibilityService);
        assertThat(KeyguardUtils.getKeyguardManager(mContext)).isNull();
    }

    @Test
    public void testIsDeviceSecuredWithCredential_HandlesNullKeyguardManager() {
        assertThat(KeyguardUtils.isDeviceSecuredWithCredential(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testIsDeviceSecuredWithCredential_CorrectlyReturnsTrue_OnApi23AndAbove() {
        when(mContext.getSystemService(any(Class.class))).thenReturn(mKeyguardManager);
        when(mKeyguardManager.isDeviceSecure()).thenReturn(true);
        assertThat(KeyguardUtils.isDeviceSecuredWithCredential(mContext)).isTrue();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN, maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void testIsDeviceSecuredWithCredential_CorrectlyReturnsTrue_OnApi16To22() {
        when(mContext.getSystemService(anyString())).thenReturn(mKeyguardManager);
        when(mKeyguardManager.isKeyguardSecure()).thenReturn(true);
        assertThat(KeyguardUtils.isDeviceSecuredWithCredential(mContext)).isTrue();
    }
}
