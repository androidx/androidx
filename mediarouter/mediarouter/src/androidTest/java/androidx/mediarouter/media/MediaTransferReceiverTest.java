/*
 * Copyright 2026 The Android Open Source Project
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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link MediaRouter#isMediaTransferEnabled()}. */
@RunWith(AndroidJUnit4.class)
public class MediaTransferReceiverTest {

    @After
    public void tearDown() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker =
                                    MediaTransferReceiver::isDeclared;
                            MediaRouterTestHelper.resetMediaRouter();
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void isMediaTransferEnabled_rOrLaterManifestTrue_returnsTrue() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker = context -> true;
                            MediaRouter.getInstance(getApplicationContext());
                            assertTrue(MediaRouter.isMediaTransferEnabled());
                            assertNotNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void isMediaTransferEnabled_rOrLaterManifestFalse_returnsFalse() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker = context -> false;
                            MediaRouter.getInstance(getApplicationContext());
                            assertFalse(MediaRouter.isMediaTransferEnabled());
                            assertNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void isMediaTransferEnabled_rOrLaterManifestTrueParamsFalse_returnsFalse() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker = context -> true;
                            MediaRouter router = MediaRouter.getInstance(getApplicationContext());

                            MediaRouterParams params =
                                    new MediaRouterParams.Builder()
                                            .setMediaTransferReceiverEnabled(false)
                                            .build();
                            router.setRouterParams(params);

                            assertFalse(MediaRouter.isMediaTransferEnabled());
                            assertNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void isMediaTransferEnabled_rOrLaterManifestFalseParamsTrue_returnsTrue() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker = context -> false;
                            MediaRouter router = MediaRouter.getInstance(getApplicationContext());

                            MediaRouterParams params =
                                    new MediaRouterParams.Builder()
                                            .setMediaTransferReceiverEnabled(true)
                                            .build();
                            router.setRouterParams(params);

                            assertTrue(MediaRouter.isMediaTransferEnabled());
                            assertNotNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    public void isMediaTransferEnabled_preRManifestTrue_returnsFalse() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            GlobalMediaRouter.sMediaTransferDeclarationChecker = context -> true;
                            MediaRouter.getInstance(getApplicationContext());
                            assertFalse(MediaRouter.isMediaTransferEnabled());
                            assertNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    public void isMediaTransferEnabled_preRParamsTrue_returnsFalse() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MediaRouter router = MediaRouter.getInstance(getApplicationContext());

                            MediaRouterParams params =
                                    new MediaRouterParams.Builder()
                                            .setMediaTransferReceiverEnabled(true)
                                            .build();
                            router.setRouterParams(params);

                            assertFalse(MediaRouter.isMediaTransferEnabled());
                            assertNull(
                                    MediaRouter.getGlobalRouter()
                                            .getMediaRoute2ProviderForTesting());
                        });
    }
}
