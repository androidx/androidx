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
// @exportToFramework:skipFile()

package androidx.appsearch.cts.app;

import android.content.Context;

import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.playservicesstorage.PlayServicesStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

public class GlobalSearchSessionGmsCtsTest extends GlobalSearchSessionCtsTestBase {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private boolean mIsGmsAvailable;
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName)
            throws Exception {
        ListenableFuture<AppSearchSession> searchSessionAsync =
                PlayServicesStorage.createSearchSessionAsync(
                        new PlayServicesStorage.SearchContext.Builder(mContext, dbName).build());
        mIsGmsAvailable = GmsTestUtil.isGmsAvailable(searchSessionAsync);

        // isGmsAvailable returns false when GMSCore or GMSCore AppSearch module are unavailable on
        // device. In this case we will not run the tests as they are expected to fail as the
        // service they are calling is unavailable.
        Assume.assumeTrue(mIsGmsAvailable);
        return searchSessionAsync;
    }

    @Override
    protected ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync()
            throws Exception {
        ListenableFuture<GlobalSearchSession> globalSearchSessionAsync =
                PlayServicesStorage.createGlobalSearchSessionAsync(
                        new PlayServicesStorage.GlobalSearchContext.Builder(mContext).build());
        mIsGmsAvailable = GmsTestUtil.isGmsAvailable(globalSearchSessionAsync);

        // isGmsAvailable returns false when GMSCore or GMSCore AppSearch module are unavailable on
        // device. In this case we will not run the tests as they are expected to fail as the
        // service they are calling is unavailable.
        Assume.assumeTrue(mIsGmsAvailable);
        return globalSearchSessionAsync;
    }

    @Override
    public void tearDown() throws Exception {
        if (mIsGmsAvailable) {
            super.tearDown();
        }
    }

    @Override
    @Ignore
    @Test
    public void testReportSystemUsage_ForbiddenFromNonSystem() {
        // TODO(b/208654892) : ReportSystemUsage is not yet needed by any clients of GMSCore
        //  AppSearch, once there is a requirement by any of the clients this will be added.
    }
}
