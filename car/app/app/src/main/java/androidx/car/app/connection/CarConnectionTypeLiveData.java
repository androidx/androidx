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

package androidx.car.app.connection;

import static androidx.car.app.connection.CarConnection.ACTION_CAR_CONNECTION_UPDATED;
import static androidx.car.app.connection.CarConnection.CAR_CONNECTION_STATE;
import static androidx.car.app.utils.LogTags.TAG_CONNECTION_TO_CAR;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.car.app.connection.CarConnection.ConnectionType;
import androidx.lifecycle.LiveData;

/**
 * A {@link LiveData} that will query once while being observed and only again if it gets updates
 * via a broadcast.
 */
final class CarConnectionTypeLiveData extends LiveData<@ConnectionType Integer> {
    @VisibleForTesting
    static final String CAR_CONNECTION_AUTHORITY = "androidx.car.app.connection";

    private static final int QUERY_TOKEN = 42;
    private static final Uri PROJECTION_HOST_URI = new Uri.Builder().scheme("content").authority(
            CAR_CONNECTION_AUTHORITY).build();

    private final Context mContext;
    private final AsyncQueryHandler mQueryHandler;
    private final CarConnectionBroadcastReceiver mBroadcastReceiver;

    CarConnectionTypeLiveData(Context context) {
        mContext = context;

        mQueryHandler = new CarConnectionQueryHandler(
                context.getContentResolver());
        mBroadcastReceiver = new CarConnectionBroadcastReceiver();
    }

    @Override
    public void onActive() {
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(ACTION_CAR_CONNECTION_UPDATED));
        queryForState();
    }

    @Override
    public void onInactive() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        mQueryHandler.cancelOperation(QUERY_TOKEN);
    }

    void queryForState() {
        mQueryHandler.startQuery(/* token= */ QUERY_TOKEN, /* cookie= */ null,
                /* uri */ PROJECTION_HOST_URI,
                /* projection= */ new String[]{CAR_CONNECTION_STATE}, /* selection= */ null,
                /* selectionArgs= */ null, /* orderBy= */ null);
    }

    class CarConnectionQueryHandler extends AsyncQueryHandler {
        CarConnectionQueryHandler(ContentResolver resolver) {
            super(resolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor response) {
            if (response == null) {
                Log.w(TAG_CONNECTION_TO_CAR, "Null response from content provider when checking "
                        + "connection to the car, treating as disconnected");
                postValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
                return;
            }

            int carConnectionTypeColumn = response.getColumnIndex(CAR_CONNECTION_STATE);
            if (carConnectionTypeColumn < 0) {
                Log.e(TAG_CONNECTION_TO_CAR, "Connection to car response is missing the "
                        + "connection type, treating as disconnected");
                postValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
                return;
            }

            if (!response.moveToNext()) {
                Log.e(TAG_CONNECTION_TO_CAR, "Connection to car response is empty, treating as "
                        + "disconnected");
                postValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
                return;
            }

            postValue(response.getInt(carConnectionTypeColumn));
        }
    }

    class CarConnectionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            queryForState();
        }
    }
}
