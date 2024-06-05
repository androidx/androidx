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

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.playservicesstorage.PlayServicesStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assume;

import java.util.concurrent.ExecutorService;

public class AppSearchSessionGmsCtsTest extends AppSearchSessionCtsTestBase {

    private boolean mIsGmsAvailable;
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName)
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ListenableFuture<AppSearchSession> appSearchSessionListenableFuture =
                PlayServicesStorage.createSearchSessionAsync(
                        new PlayServicesStorage.SearchContext.Builder(context, dbName).build());
        mIsGmsAvailable = GmsTestUtil.isGmsAvailable(appSearchSessionListenableFuture);

        // isGmsAvailable returns false when GMSCore or GMSCore AppSearch module are unavailable on
        // device. In this case we will not run the tests as they are expected to fail as the
        // service they are calling is unavailable.
        Assume.assumeTrue(mIsGmsAvailable);
        return appSearchSessionListenableFuture;
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName,
            @NonNull ExecutorService unused) throws Exception {
        // Executor is not required for PlayServicesAppSearch.
        return createSearchSessionAsync(dbName);
    }

    @Override
    public void tearDown() throws Exception {
        if (mIsGmsAvailable) {
            super.tearDown();
        }
    }
}
