/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link Intents}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
public class IntentsTest {
    Context mContext;
    PackageManager mPackageManager;
    @Mock
    Intent mIntent;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ((Application) ApplicationProvider.getApplicationContext()).getBaseContext();
        mPackageManager = mContext.getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mContext.getPackageName();
        shadowOf(mPackageManager).installPackage(packageInfo);
    }

    @Test
    public void testToLongString() {
        assertThat(Intents.toLongString(null)).isEmpty();
        assertThat(Intents.toLongString(mIntent)).isEqualTo("null, component null");
        when(mIntent.getAction()).thenReturn("SEND");
        assertThat(Intents.toLongString(mIntent)).isEqualTo("SEND, component null");
        ComponentName component = new ComponentName("PKG", "CLS");
        when(mIntent.getComponent()).thenReturn(component);
        assertThat(Intents.toLongString(mIntent)).isEqualTo("SEND, CLS, PKG");
    }
}
