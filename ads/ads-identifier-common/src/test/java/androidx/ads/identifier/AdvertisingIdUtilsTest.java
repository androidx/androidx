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

package androidx.ads.identifier;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class AdvertisingIdUtilsTest {
    @Test
    public void selectServiceByPriority() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("c.normal.1", false, 1, packageManager),
                createPackageInfo("y.normal.0", false, 0, packageManager),
                createPackageInfo("x.normal.0", false, 0, packageManager),
                createPackageInfo("z.high.2", true, 2, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "z.high.2",
                "x.normal.0",
                "y.normal.0",
                "c.normal.1"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_firstInstallTime() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("com.a", false, 2, packageManager),
                createPackageInfo("com.b", false, 9, packageManager),
                createPackageInfo("com.c", false, 7, packageManager),
                createPackageInfo("com.d", false, 10, packageManager),
                createPackageInfo("com.e", false, 0, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "com.e",
                "com.a",
                "com.c",
                "com.b",
                "com.d"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_packageName() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("com.abc.id", false, 0, packageManager),
                createPackageInfo("com.abc", false, 0, packageManager),
                createPackageInfo("org.example", false, 0, packageManager),
                createPackageInfo("com.abcde", false, 0, packageManager),
                createPackageInfo("com.abcde_id", false, 0, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "com.abc",
                "com.abc.id",
                "com.abcde",
                "com.abcde_id",
                "org.example"
        ).inOrder();
    }

    private List<String> getPriorityList(List<ResolveInfo> resolveInfos,
            PackageManager packageManager) {
        List<String> result = new ArrayList<>();
        while (resolveInfos.size() > 0) {
            final ServiceInfo serviceInfo =
                    AdvertisingIdUtils.selectServiceByPriority(resolveInfos, packageManager);

            result.add(serviceInfo.packageName);

            resolveInfos.removeIf(resolveInfo -> resolveInfo.serviceInfo == serviceInfo);
        }
        return result;
    }

    @Test
    public void selectServiceByPriority_inputNull() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        ServiceInfo serviceInfo =
                AdvertisingIdUtils.selectServiceByPriority(null, packageManager);

        assertThat(serviceInfo).isNull();
    }

    @Test
    public void selectServiceByPriority_inputEmpty() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        ServiceInfo serviceInfo =
                AdvertisingIdUtils.selectServiceByPriority(Collections.emptyList(), packageManager);

        assertThat(serviceInfo).isNull();
    }

    private ResolveInfo createPackageInfo(String packageName,
            boolean requestHighPriority, long firstInstallTime, PackageManager packageManager)
            throws Exception {
        PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.packageName = packageName;
        if (requestHighPriority) {
            packageInfo.requestedPermissions =
                    new String[]{AdvertisingIdUtils.HIGH_PRIORITY_PERMISSION};
        }
        packageInfo.firstInstallTime = firstInstallTime;

        mockGetPackageInfo(packageInfo, packageManager);

        ResolveInfo resolveInfo = mock(ResolveInfo.class);
        resolveInfo.serviceInfo = mock(ServiceInfo.class);
        resolveInfo.serviceInfo.packageName = packageName;
        return resolveInfo;
    }

    private void mockGetPackageInfo(PackageInfo packageInfo, PackageManager packageManager)
            throws Exception {
        when(packageManager.getPackageInfo(eq(packageInfo.packageName),
                eq(PackageManager.GET_PERMISSIONS))).thenReturn(packageInfo);
    }
}
