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

package androidx.mediarouter.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import android.os.Build;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test {@link MediaRouterParams}. */
@RunWith(AndroidJUnit4.class)
public class MediaRouterParamsTest {

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    public void mediaRouterParamsBuilder_androidQOrBelow() {
        verifyMediaRouterParamsBuilder(/* isAndroidROrAbove= */ false);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void mediaRouterParamsBuilder_androidROrAbove() {
        verifyMediaRouterParamsBuilder(/* isAndroidROrAbove= */ true);
    }

    private void verifyMediaRouterParamsBuilder(boolean isAndroidROrAbove) {
        final int dialogType = MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP;
        final boolean isOutputSwitcherEnabled = true;
        final boolean transferToLocalEnabled = true;
        final boolean transferReceiverEnabled = false;
        final boolean mediaTransferRestrictedToSelfProviders = true;
        final Bundle extras = new Bundle();
        extras.putString(TEST_KEY, TEST_VALUE);

        MediaRouterParams params =
                new MediaRouterParams.Builder()
                        .setDialogType(dialogType)
                        .setOutputSwitcherEnabled(isOutputSwitcherEnabled)
                        .setTransferToLocalEnabled(transferToLocalEnabled)
                        .setMediaTransferReceiverEnabled(transferReceiverEnabled)
                        .setMediaTransferRestrictedToSelfProviders(
                                mediaTransferRestrictedToSelfProviders)
                        .setExtras(extras)
                        .build();

        assertEquals(dialogType, params.getDialogType());

        if (isAndroidROrAbove) {
            assertEquals(isOutputSwitcherEnabled, params.isOutputSwitcherEnabled());
            assertEquals(transferToLocalEnabled, params.isTransferToLocalEnabled());
            assertEquals(transferReceiverEnabled, params.isMediaTransferReceiverEnabled());
            assertEquals(
                    mediaTransferRestrictedToSelfProviders,
                    params.isMediaTransferRestrictedToSelfProviders());
        } else {
            // Earlier than Android R, output switcher cannot be enabled.
            // Same for transfer to local.
            assertFalse(params.isOutputSwitcherEnabled());
            assertFalse(params.isTransferToLocalEnabled());
            assertFalse(params.isMediaTransferReceiverEnabled());
            assertFalse(params.isMediaTransferRestrictedToSelfProviders());
        }

        extras.remove(TEST_KEY);
        assertEquals(TEST_VALUE, params.getExtras().getString(TEST_KEY));

        // Tests copy constructor of builder
        MediaRouterParams copiedParams = new MediaRouterParams.Builder(params).build();
        assertEquals(params.getDialogType(), copiedParams.getDialogType());
        assertEquals(params.isOutputSwitcherEnabled(), copiedParams.isOutputSwitcherEnabled());
        assertEquals(params.isTransferToLocalEnabled(), copiedParams.isTransferToLocalEnabled());
        assertEquals(
                params.isMediaTransferReceiverEnabled(),
                copiedParams.isMediaTransferReceiverEnabled());
        assertEquals(
                params.isMediaTransferRestrictedToSelfProviders(),
                copiedParams.isMediaTransferRestrictedToSelfProviders());
        assertBundleEquals(params.getExtras(), copiedParams.getExtras());
    }

    /** Asserts that two Bundles are equal. */
    public static void assertBundleEquals(Bundle expected, Bundle observed) {
        if (expected == null || observed == null) {
            assertSame(expected, observed);
        }
        assertEquals(expected.size(), observed.size());
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), observed.get(key));
        }
    }
}
