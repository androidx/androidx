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

package androidx.pdf.util;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Information about the installed app (package). */
// TODO: Clean up this class to get package name alternatively
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppInfo {
    private static final String TAG = "AppInfo";
    private static final String NO_PACKAGE_NAME = "no.pkg";
    private static final String NO_VERSION = "no-version";

    private static AppInfo sAppInfo = new AppInfo();
    private final String mAppVersion;
    private final String mPackageName;

    /** Singleton-style getter for the {@link AppInfo} instance. Always non-null. */
    @NonNull
    public static AppInfo get() {
        return sAppInfo;
    }

    private AppInfo(Context appContext) {
        PackageManager pm = appContext.getPackageManager();
        String pkg = appContext.getPackageName();
        PackageInfo pi = getPackageInfo(pm, pkg);
        mAppVersion = pi.versionName != null ? pi.versionName : NO_VERSION;
        mPackageName = pi.packageName != null ? pi.packageName : NO_PACKAGE_NAME;
    }

    private AppInfo() {
        mPackageName = NO_PACKAGE_NAME;
        mAppVersion = NO_VERSION;
    }

    @NonNull
    public String getAppVersion() {
        return mAppVersion;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    private static PackageInfo getPackageInfo(PackageManager pkgManager, String pkg) {
        try {
            return pkgManager.getPackageInfo(pkg, 0);
        } catch (NameNotFoundException e) {
            ErrorLog.log(TAG, String.format("Can't find our own package info?? %s", pkg), e);
            return new PackageInfo() {
                {
                    packageName = NO_PACKAGE_NAME;
                    versionName = NO_VERSION;
                }
            };
        }
    }
}
