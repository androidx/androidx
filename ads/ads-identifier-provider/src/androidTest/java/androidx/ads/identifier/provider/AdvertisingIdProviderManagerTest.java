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

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;
import static androidx.ads.identifier.provider.AdvertisingIdProviderManager.OPEN_SETTINGS_ACTION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.Lists;
import com.google.common.truth.Correspondence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.concurrent.Callable;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvertisingIdProviderManagerTest {

    private static final Correspondence<AdvertisingIdProviderInfo, AdvertisingIdProviderInfo>
            PROVIDER_INFO_EQUALITY = Correspondence.from(
            AdvertisingIdProviderManagerTest::isProviderInfoEqual, "is equivalent to");

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PackageManager mMockPackageManager;

    private Context mContext;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mContext = new ContextWrapper(context) {
            @Override
            public PackageManager getPackageManager() {
                return mMockPackageManager;
            }
        };
    }

    private ResolveInfo createServiceResolveInfo(String packageName) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        return resolveInfo;
    }

    private ResolveInfo createActivityResolveInfo(String packageName, String name) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = name;
        return resolveInfo;
    }

    private void mockQueryIntentServices(List<ResolveInfo> resolveInfos) throws Exception {
        boolean supportMatchSystemOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        int flags = supportMatchSystemOnly ? PackageManager.MATCH_SYSTEM_ONLY : 0;
        when(mMockPackageManager.queryIntentServices(
                argThat(intent -> intent != null && GET_AD_ID_ACTION.equals(intent.getAction())),
                eq(flags))).thenReturn(resolveInfos);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (!supportMatchSystemOnly) {
                ApplicationInfo applicationInfo = new ApplicationInfo();
                applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
                when(mMockPackageManager.getApplicationInfo(resolveInfo.serviceInfo.packageName, 0))
                        .thenReturn(applicationInfo);
            }
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = resolveInfo.serviceInfo.packageName;
            when(mMockPackageManager.getPackageInfo(packageInfo.packageName,
                    PackageManager.GET_PERMISSIONS)).thenReturn(packageInfo);
        }
    }

    private void mockQueryIntentActivities(List<ResolveInfo> resolveInfos) {
        when(mMockPackageManager.queryIntentActivities(
                argThat(intent -> intent != null
                        && OPEN_SETTINGS_ACTION.equals(intent.getAction())),
                eq(0))).thenReturn(resolveInfos);
    }

    @Test
    public void getAllAdIdProviders_onlySelf() throws Exception {
        mockQueryIntentServices(
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
        mockQueryIntentServices(
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
        mockQueryIntentServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a")));

        mockQueryIntentActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
                                        .setClassName("com.a", "A"))
                                .build());
    }

    @Test
    public void getAllAdIdProviders_twoOtherProviders() throws Exception {
        mockQueryIntentServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a"),
                        createServiceResolveInfo("com.b")));

        mockQueryIntentActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
                                        .setClassName("com.a", "A"))
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.b")
                                .build());
    }

    @Test
    public void getAllAdIdProviders_extraOpenIntent() throws Exception {
        mockQueryIntentServices(
                Lists.newArrayList(
                        createServiceResolveInfo(mContext.getPackageName()),
                        createServiceResolveInfo("com.a")));

        mockQueryIntentActivities(
                Lists.newArrayList(
                        createActivityResolveInfo(mContext.getPackageName(), "Activity"),
                        createActivityResolveInfo("com.a", "A"),
                        createActivityResolveInfo("com.b", "B")));

        assertThat(AdvertisingIdProviderManager.getAdvertisingIdProviders(mContext))
                .comparingElementsUsing(PROVIDER_INFO_EQUALITY)
                .containsExactly(
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName(mContext.getPackageName())
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
                                        .setClassName(mContext.getPackageName(), "Activity"))
                                .setHighestPriority(true)
                                .build(),
                        AdvertisingIdProviderInfo.builder()
                                .setPackageName("com.a")
                                .setSettingsIntent(new Intent(OPEN_SETTINGS_ACTION)
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
