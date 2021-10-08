/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier.provider;

import static androidx.ads.identifier.testing.MockPackageManagerHelper.createActivityResolveInfo;
import static androidx.ads.identifier.testing.MockPackageManagerHelper.createServiceResolveInfo;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.ads.identifier.testing.MockPackageManagerHelper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.Lists;
import com.google.common.truth.Correspondence;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class AdvertisingIdProviderManagerTest {

    private static final Correspondence<AdvertisingIdProviderInfo, AdvertisingIdProviderInfo>
            PROVIDER_INFO_EQUALITY = Correspondence.from(
            AdvertisingIdProviderManagerTest::isProviderInfoEqual, "is equivalent to");

    private MockPackageManagerHelper mMockPackageManagerHelper = new MockPackageManagerHelper();

    private Context mContext;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mContext = new ContextWrapper(context) {
            @Override
            public PackageManager getPackageManager() {
                return mMockPackageManagerHelper.getMockPackageManager();
            }
        };
    }

    @Test
    public void getAllAdIdProviders_onlySelf() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(
                Lists.newArrayList(createServiceResolveInfo(mContext.getPackageName())));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setHighestPriority(true)
                                .build());
    }

    @Test
    public void getAllAdIdProviders_noProvider() {
        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext)).isEmpty();
    }

    @Test
    public void getAllAdIdProviders() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder().setPackageName("com.a").build());
    }

    @Test
    public void getAllAdIdProviders_withOpenIntent() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a")));

        mMockPackageManagerHelper.mockQueryOpenSettingsActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName("com.a", "A"))
                                .build());
    }

    @Test
    public void getAllAdIdProviders_twoOtherProviders() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a"),
                        createServiceResolveInfo("com.b")));

        mMockPackageManagerHelper.mockQueryOpenSettingsActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName("com.a", "A"))
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.b")
                                .build());
    }

    @Test
    public void getAllAdIdProviders_extraOpenIntent() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a")));

        mMockPackageManagerHelper.mockQueryOpenSettingsActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A"),
                        createActivityResolveInfo("com.b", "B")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(
                                        androidx.ads.identifier.provider
                                                .AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION)
                                        .setClassName("com.a", "A"))
                                .build());
    }

    @Test
    public void registerProviderCallable() {
        Callable<AdvertisingIdProvider> providerCallable = () -> null;

        AdvertisingIdProviderManager.registerProviderCallable(providerCallable);

        assertThat(AdvertisingIdProviderManager.getProviderCallable())
                .isSameInstanceAs(providerCallable);
    }

    @Test(expected = NullPointerException.class)
    public void registerProviderCallable_null() {
        AdvertisingIdProviderManager.registerProviderCallable(null);
    }

    @Test
    public void clearProviderCallable() {
        Callable<AdvertisingIdProvider> providerCallable = () -> null;

        AdvertisingIdProviderManager.registerProviderCallable(providerCallable);
        AdvertisingIdProviderManager.clearProviderCallable();

        assertThat(AdvertisingIdProviderManager.getProviderCallable()).isNull();
    }

    private static boolean isProviderInfoEqual(
            AdvertisingIdProviderInfo actual, AdvertisingIdProviderInfo expected) {
        return actual.getPackageName().equals(expected.getPackageName())
                && (actual.getSettingsIntent() == null ? expected.getSettingsIntent() == null
                : actual.getSettingsIntent().filterEquals(expected.getSettingsIntent()))
                && actual.isHighestPriority() == expected.isHighestPriority();
    }
}
