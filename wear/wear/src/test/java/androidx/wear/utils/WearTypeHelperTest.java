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

package androidx.wear.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(WearUtilsTestRunner.class)
@DoNotInstrument // Stop Robolectric instrumenting this class due to it being in package "android".
public class WearTypeHelperTest {
    private ShadowPackageManager mShadowPackageManager = null;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
    }

    private void setSystemFeatureChina(boolean value) {
        mShadowPackageManager.setSystemFeature(WearTypeHelper.CHINA_SYSTEM_FEATURE, value);
    }

    @Test
    @Config(sdk = 28)
    public void test_isChinaDevice() {
        setSystemFeatureChina(true);

        assertTrue(WearTypeHelper.isChinaDevice(mContext));
    }

    @Test
    @Config(sdk = 28)
    public void test_isROWDevice() {
        setSystemFeatureChina(false);

        assertFalse(WearTypeHelper.isChinaDevice(mContext));
    }
}
