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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Test {@link RegisteredMediaRouteProviderWatcher}. */
@RunWith(AndroidJUnit4.class)
public class RegisteredMediaRouteProviderWatcherTest {
    private Context mContext;
    private RegisteredMediaRouteProviderWatcher mProviderWatcher;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        RegisteredMediaRouteProviderWatcher.Callback callback =
                new RegisteredMediaRouteProviderWatcher.Callback() {
                    @Override
                    public void addProvider(@NonNull MediaRouteProvider provider) {}

                    @Override
                    public void removeProvider(@NonNull MediaRouteProvider provider) {}

                    @Override
                    public void releaseProviderController(
                            @NonNull RegisteredMediaRouteProvider provider,
                            @NonNull MediaRouteProvider.RouteController controller) {}
                };

        getInstrumentation()
                .runOnMainSync(
                        () -> mProviderWatcher =
                                new RegisteredMediaRouteProviderWatcher(mContext, callback));
    }

    @SmallTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void getMediaRoute2ProviderServices_restrictedToSelfProviders_shouldGetSelfProviders() {
        mProviderWatcher.setMediaTransferRestrictedToSelfProviders(true);
        assertTrue(mProviderWatcher.isMediaTransferRestrictedToSelfProvidersForTesting());
        List<ServiceInfo> serviceInfos = mProviderWatcher.getMediaRoute2ProviderServices();
        assertTrue(isSelfProvidersContained(serviceInfos));
    }

    @SmallTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void getMediaRoute2ProviderServices_notRestrictedToSelfProviders_shouldGetAllProvider() {
        mProviderWatcher.setMediaTransferRestrictedToSelfProviders(false);
        assertFalse(mProviderWatcher.isMediaTransferRestrictedToSelfProvidersForTesting());
        List<ServiceInfo> serviceInfos = mProviderWatcher.getMediaRoute2ProviderServices();
        assertTrue(isSelfProvidersContained(serviceInfos));
    }

    private boolean isSelfProvidersContained(List<ServiceInfo> serviceInfos) {
        for (ServiceInfo serviceInfo : serviceInfos) {
            if (TextUtils.equals(serviceInfo.packageName, mContext.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
