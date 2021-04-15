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

package androidx.appsearch.debugview.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;

/**
 * Debug Activity for AppSearch.
 *
 * <p>This activity provides a view of all the documents that have been put into an application's
 * AppSearch database. The database is specified by creating an {@link android.content.Intent}
 * with a {@code String} extra containing key: {@code databaseName} and value: name of AppSearch
 * database.
 *
 * <p>To launch this activity, declare it in the application's manifest:
 * <pre>
 *     <activity android:name="androidx.appsearch.debugview.view.AppSearchDebugActivity" />
 * </pre>
 *
 * <p>Next, create an {@link android.content.Intent} with the {@code databaseName} to view
 * documents for, and start the activity:
 * <pre>
 *     Intent intent = new Intent(this, AppSearchDebugActivity.class);
 *     intent.putExtra("databaseName", DB_NAME);
 *     startActivity(intent);
 * </pre>
 *
 * <p><b>Note:</b> Debugging is currently only compatible with local storage.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppSearchDebugActivity extends AppCompatActivity {
    private static final String DB_INTENT_KEY = "databaseName";

    private String mDbName;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private ListeningExecutorService mBackgroundExecutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appsearchdebug);

        mBackgroundExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        mDbName = getIntent().getExtras().getString(DB_INTENT_KEY);
        mDebugAppSearchManager = DebugAppSearchManager.create(
                getApplicationContext(), mBackgroundExecutor, mDbName);
    }

    @Override
    protected void onStop() {
        Futures.whenAllSucceed(mDebugAppSearchManager).call(() -> {
            Futures.getDone(mDebugAppSearchManager).close();
            return null;
        }, mBackgroundExecutor);

        super.onStop();
    }

    /**
     * Gets the {@link DebugAppSearchManager} instance created by the activity.
     */
    @NonNull
    public ListenableFuture<DebugAppSearchManager> getDebugAppSearchManager() {
        return mDebugAppSearchManager;
    }

    /**
     * Gets the {@link ListeningExecutorService} instance created by the activity.
     */
    @NonNull
    public ListeningExecutorService getBackgroundExecutor() {
        return mBackgroundExecutor;
    }
}
