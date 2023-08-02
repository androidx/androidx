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

package androidx.ads.identifier.testing;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * Testing utilities for Advertising ID.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockPackageManagerHelper {

    private static final String OPEN_SETTINGS_ACTION =
            "androidx.ads.identifier.provider.OPEN_SETTINGS";

    @Mock
    private PackageManager mMockPackageManager;

    public MockPackageManagerHelper() {
        MockitoAnnotations.initMocks(this);
    }

    @NonNull
    public PackageManager getMockPackageManager() {
        return mMockPackageManager;
    }

    /** Mocks the {@link PackageManager#queryIntentServices(Intent, int)}. */
    @SuppressWarnings("deprecation")
    public void mockQueryGetAdIdServices(@NonNull List<ResolveInfo> resolveInfos) throws Exception {
        boolean supportMatchSystemOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        when(mMockPackageManager.queryIntentServices(hasAction(GET_AD_ID_ACTION),
                eq(supportMatchSystemOnly ? PackageManager.MATCH_SYSTEM_ONLY : 0)))
                .thenReturn(resolveInfos);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.serviceInfo.packageName;
            if (!supportMatchSystemOnly) {
                ApplicationInfo applicationInfo = new ApplicationInfo();
                applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
                when(mMockPackageManager.getApplicationInfo(packageName, 0))
                        .thenReturn(applicationInfo);
            }
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = packageName;
            when(mMockPackageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS))
                    .thenReturn(packageInfo);
        }
    }

    /** Mocks the {@link PackageManager#queryIntentActivities(Intent, int)}. */
    @SuppressWarnings("deprecation")
    public void mockQueryOpenSettingsActivities(@NonNull List<ResolveInfo> resolveInfos) {
        when(mMockPackageManager.queryIntentActivities(hasAction(OPEN_SETTINGS_ACTION), eq(0)))
                .thenReturn(resolveInfos);
    }

    private static Intent hasAction(final String action) {
        return argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Intent intent) {
                return intent != null && action.equals(intent.getAction());
            }
        });
    }

    /**
     * Creates a {@link ResolveInfo} which contains a {@link ServiceInfo} with given package name.
     */
    @NonNull
    public static ResolveInfo createServiceResolveInfo(@NonNull String packageName) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        return resolveInfo;
    }

    /**
     * Creates a {@link ResolveInfo} which contains a {@link ServiceInfo} with given package name
     * and service name.
     */
    @NonNull
    public static ResolveInfo createServiceResolveInfo(
            @NonNull String packageName, @NonNull String serviceName) {
        ResolveInfo resolveInfo = createServiceResolveInfo(packageName);
        resolveInfo.serviceInfo.name = serviceName;
        return resolveInfo;
    }

    /**
     * Creates a {@link ResolveInfo} which contains a {@link ActivityInfo} with given package
     * name and activity name.
     */
    @NonNull
    public static ResolveInfo createActivityResolveInfo(
            @NonNull String packageName, @NonNull String activityName) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = activityName;
        return resolveInfo;
    }
}
