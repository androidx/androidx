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

package androidx.webkit;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A class for testing common usages for ProfileStore, Profile.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class MultiProfileTest {

    ProfileStore mProfileStore;

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        mProfileStore = WebkitUtils.onMainThreadSync(ProfileStore::getInstance);
    }

    /**
     * Tests creation and the created profile is ready to use.
     */
    @Test
    public void testCreateProfile() {
        WebkitUtils.onMainThreadSync(() -> {
            Profile createdProfile = mProfileStore.getOrCreateProfile("Test");

            Assert.assertNotNull(createdProfile);
            Assert.assertEquals(createdProfile.getName(), "Test");
            Assert.assertNotNull(createdProfile.getCookieManager());
            Assert.assertNotNull(createdProfile.getGeolocationPermissions());
            Assert.assertNotNull(createdProfile.getWebStorage());
            Assert.assertNotNull(createdProfile.getServiceWorkerController());
            Assert.assertEquals(mProfileStore.getAllProfileNames().size(), 2);
        });
    }

    /**
     * Tests getting, verifying the default profile.
     */
    @Test
    public void testGetDefaultProfile() {
        WebkitUtils.onMainThreadSync(() -> {
            Profile defaultProfile = mProfileStore.getProfile(Profile.DEFAULT_PROFILE_NAME);

            Assert.assertNotNull(defaultProfile);
            Assert.assertEquals(defaultProfile.getName(), Profile.DEFAULT_PROFILE_NAME);
        });
    }

    /**
     * Test profile deletion with three inner asserts.
     */
    @Test
    public void testDeleteProfile() {
        WebkitUtils.onMainThreadSync(
                () -> {
                    // DeleteProfileInUseFails
                    mProfileStore.getOrCreateProfile("Test");

                    Assert.assertThrows(
                            IllegalStateException.class,
                            () -> mProfileStore.deleteProfile("Test"));

                    // DeleteProfileNonExistent
                    Assert.assertFalse(mProfileStore.deleteProfile("Not-Found"));

                    // DeleteDefaultProfileFails
                    Assert.assertThrows(
                            IllegalArgumentException.class,
                            () -> mProfileStore.deleteProfile(Profile.DEFAULT_PROFILE_NAME));
                });
    }

}
