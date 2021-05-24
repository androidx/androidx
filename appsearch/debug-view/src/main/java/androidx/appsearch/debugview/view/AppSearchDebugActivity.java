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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.fragment.app.FragmentActivity;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executors;

/**
 * Debug Activity for AppSearch.
 *
 * <p>This activity provides a view of all the documents that have been put into an application's
 * AppSearch database. The database is specified by creating an {@link android.content.Intent}
 * with extras specifying the database name and the AppSearch storage type.
 *
 * <p>To launch this activity, declare it in the application's manifest:
 * <pre>
 *     <activity android:name="androidx.appsearch.debugview.view.AppSearchDebugActivity" />
 * </pre>
 *
 * <p>Next, create an {@link android.content.Intent} from the activity that will launch the debug
 * activity. Add the database name as an extra with key: {@link #DB_INTENT_KEY} and the storage
 * type, which can be either {@link #STORAGE_TYPE_LOCAL} or {@link #STORAGE_TYPE_PLATFORM} with
 * key: {@link #STORAGE_TYPE_INTENT_KEY}.
 *
 * <p>Example of launching the debug activity for local storage:
 * <pre>
 *     Intent intent = new Intent(this, AppSearchDebugActivity.class);
 *     intent.putExtra(AppSearchDebugActivity.DB_INTENT_KEY, DB_NAME);
 *     intent.putExtra(AppSearchDebugActivity.STORAGE_TYPE_INTENT_KEY,
 *             AppSearchDebugActivity.STORAGE_TYPE_LOCAL);
 *     startActivity(intent);
 * </pre>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppSearchDebugActivity extends FragmentActivity {
    private static final String TAG = "AppSearchDebugActivity";
    public static final String DB_INTENT_KEY = "databaseName";
    public static final String STORAGE_TYPE_INTENT_KEY = "storageType";

    private String mDbName;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private ListeningExecutorService mBackgroundExecutor;

    @IntDef(value = {
            STORAGE_TYPE_LOCAL,
            STORAGE_TYPE_PLATFORM,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StorageType {
    }

    public static final int STORAGE_TYPE_LOCAL = 0;
    public static final int STORAGE_TYPE_PLATFORM = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appsearchdebug);

        mBackgroundExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        mDbName = getIntent().getExtras().getString(DB_INTENT_KEY);
        @StorageType int storageType =
                getIntent().getExtras().getInt(STORAGE_TYPE_INTENT_KEY);
        try {
            mDebugAppSearchManager = DebugAppSearchManager.create(
                    getApplicationContext(), mBackgroundExecutor, mDbName, storageType);
        } catch (AppSearchException e) {
            Toast.makeText(getApplicationContext(),
                    "Failed to initialize AppSearch: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to initialize AppSearch.", e);
        }

        MenuFragment menuFragment = new MenuFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, menuFragment)
                .addToBackStack(/*name=*/null)
                .commit();
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
