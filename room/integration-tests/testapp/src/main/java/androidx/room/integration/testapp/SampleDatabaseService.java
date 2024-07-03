/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.integration.testapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ExperimentalRoomApi;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.SampleDatabase;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * For testing use of {@link SampleDatabase} in a remote process.
 */
public class SampleDatabaseService extends Service {

    private static final String DATABASE_NAME_PARAM = "db-name";

    private final ISampleDatabaseService.Stub mBinder = new ISampleDatabaseService.Stub() {

        @Override
        public int getPid() {
            return Process.myPid();
        }

        @Override
        public void insertCustomer(int id, String name, String lastName) {
            final Customer customer = new Customer();
            customer.setId(id);
            customer.setName(name);
            customer.setLastName(lastName);
            mDatabase.getCustomerDao().insert(customer);
        }

        @Override
        public boolean waitForCustomer(int id, String name, String lastName) {
            final CountDownLatch changed = new CountDownLatch(1);
            final InvalidationTracker.Observer observer = new InvalidationTracker.Observer(
                    Customer.class.getSimpleName()) {
                @Override
                public void onInvalidated(@NonNull Set<String> tables) {
                    if (mDatabase.getCustomerDao().contains(id, name, lastName)) {
                        changed.countDown();
                    }
                }
            };
            mDatabase.getInvalidationTracker().addObserver(observer);
            try {
                return changed.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return false;
            } finally {
                mDatabase.getInvalidationTracker().removeObserver(observer);
            }
        }
    };

    private SampleDatabase mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Creates the test service for the given database name
     * @param context The context to create the intent
     * @param databaseName The database name to be used
     * @return A new intent that can be used to connect to this service
     */
    @NonNull
    public static Intent intentFor(@NonNull Context context, @NonNull String databaseName) {
        Intent intent = new Intent(context, SampleDatabaseService.class);
        intent.putExtra(DATABASE_NAME_PARAM, databaseName);
        return intent;
    }

    @Nullable
    @Override
    @ExperimentalRoomApi
    public IBinder onBind(Intent intent) {
        String databaseName = intent.getStringExtra(DATABASE_NAME_PARAM);
        if (databaseName == null) {
            throw new IllegalArgumentException("Must pass database name in the intent");
        }
        if (mDatabase != null) {
            throw new IllegalStateException("Cannot re-use the same service for different tests");
        }
        mDatabase = Room.databaseBuilder(this, SampleDatabase.class, databaseName)
                .enableMultiInstanceInvalidation()
                .build();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mDatabase.close();
        return super.onUnbind(intent);
    }
}
