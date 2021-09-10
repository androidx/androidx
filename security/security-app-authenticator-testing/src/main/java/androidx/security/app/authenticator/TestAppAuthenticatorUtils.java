/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.security.app.authenticator;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.util.Map;
import java.util.Set;

/**
 * An extension of the {@link AppAuthenticatorUtils} used by the {@link AppAuthenticator} that
 * can be injected into the {@code AppAuthenticator} to configure it to behave as required by the
 * test.
 *
 * <p>This test class supports changing the UID of a specified package and treating the package
 * as not being installed to allowing testing of error path scenarios.
 */
class TestAppAuthenticatorUtils extends AppAuthenticatorUtils {
    private Map<String, Integer> mPackageUids;
    private Set<String> mNotInstalledPackages;

    /**
     * Constructor; instances should be configured through the {@link Builder}.
     */
    TestAppAuthenticatorUtils(Context context, Map<String, Integer> packageUids,
            Set<String> exceptionPackages) {
        super(context);
        mPackageUids = packageUids;
        mNotInstalledPackages = exceptionPackages;
    }

    /**
     * Builder for a new {@link TestAppAuthenticatorUtils} that allows this test class to be
     * configured as required for the test.
     */
    static class Builder {
        private Context mContext;
        private Map<String, Integer> mPackageUids;
        private Set<String> mNotInstalledPackages;

        /**
         * Creates a new {@code Builder} with the specified {@link Context}.
         *
         * @param context the {@code Context} within which to create the new Builder
         */
        Builder(Context context) {
            mContext = context;
            mPackageUids = new ArrayMap<>();
            mNotInstalledPackages = new ArraySet<>();
        }

        /**
         * Sets the {@code uid} to be returned when the specified {@code packageName} is queried.
         *
         * @param packageName the name of the package to be configured
         * @param uid         the uid to return for the specified package
         * @return this instance of the {@code Builder}
         */
        Builder setUidForPackage(String packageName, int uid) {
            mPackageUids.put(packageName, uid);
            return this;
        }

        /**
         * Treats the provided {@code packageName} as not being installed; this will result in a
         * {@link PackageManager.NameNotFoundException} being thrown when this package is queried.
         *
         * @param packageName the name of the package to be treated as not installed
         * @return this instance of the {@code Builder}
         */
        Builder setPackageNotInstalled(String packageName) {
            mNotInstalledPackages.add(packageName);
            return this;
        }

        /**
         * Builds an extension of the {@link AppAuthenticatorUtils} that can be injected to satisfy
         * test requirements.
         *
         * @return a new {@link TestAppAuthenticatorUtils} that will respond to queries as
         * configured.
         */
        TestAppAuthenticatorUtils build() {
            return new TestAppAuthenticatorUtils(mContext, mPackageUids, mNotInstalledPackages);
        }
    }

    /**
     * Returns the UID configured for the specified {@code packageName}, or the calling UID if
     * the UID of the package has not been configured.
     *
     * @param packageName the name of the package to be queried
     * @return the UID of the specified package
     * @throws PackageManager.NameNotFoundException if this class has been configured to treat
     *                                              the specified package as not installed
     */
    @Override
    int getUidForPackage(String packageName) throws PackageManager.NameNotFoundException {
        if (mNotInstalledPackages.contains(packageName)) {
            throw new PackageManager.NameNotFoundException("Test configured to throw exception "
                    + "for package " + packageName);
        }
        if (mPackageUids.containsKey(packageName)) {
            return mPackageUids.get(packageName);
        }
        return getCallingUid();
    }
}
