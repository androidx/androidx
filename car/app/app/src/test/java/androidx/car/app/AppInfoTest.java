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

package androidx.car.app;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.car.app.versioning.CarAppApiLevels;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AppInfoTest {
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private final ApplicationInfo mApplicationInfo = new ApplicationInfo();

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getApplicationInfo(isNull(), anyInt()))
                .thenReturn(mApplicationInfo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_minApiLevel_mustNotUseNullMetadata() {
        mApplicationInfo.metaData = null;
        AppInfo.create(mContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_minApiLevel_mustNotMissMetadataKeyMinApiLevelManifestKey() {
        mApplicationInfo.metaData = new Bundle();
        AppInfo.create(mContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_minApiLevel_cannotBeLowerThanOldest() {
        int minApiLevel = CarAppApiLevels.getOldest() - 1;
        mApplicationInfo.metaData = new Bundle();
        mApplicationInfo.metaData.putInt(AppInfo.MIN_API_LEVEL_METADATA_KEY, minApiLevel);
        AppInfo.create(mContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_minApiLevel_cannotBeHigherThanLatest() {
        int minApiLevel = CarAppApiLevels.getLatest() + 1;
        mApplicationInfo.metaData = new Bundle();
        mApplicationInfo.metaData.putInt(AppInfo.MIN_API_LEVEL_METADATA_KEY, minApiLevel);
        AppInfo.create(mContext);
    }

    @Test
    public void retrieveMinApiLevel_isReadFromManifest() {
        int minApiLevel = 123;
        mApplicationInfo.metaData = new Bundle();
        mApplicationInfo.metaData.putInt(AppInfo.MIN_API_LEVEL_METADATA_KEY, minApiLevel);
        assertThat(AppInfo.retrieveMinCarAppApiLevel(mContext)).isEqualTo(minApiLevel);
    }
}
