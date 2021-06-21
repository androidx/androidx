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

package androidx.car.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.utils.LogTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An activity for use by the car app library to perform actions such as requesting permissions.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class CarAppInternalActivity extends ComponentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processInternal(getIntent());
    }

    private void processInternal(@Nullable Intent intent) {
        if (intent != null && CarContext.REQUEST_PERMISSIONS_ACTION.equals(intent.getAction())) {
            requestPermissions(intent);
        } else {
            Log.e(LogTags.TAG,
                    "Unexpected intent action for CarAppInternalActivity: " + (intent == null
                            ? "null Intent" : intent.getAction()));
            finish();
        }
    }

    private void requestPermissions(Intent intent) {
        Bundle extras = intent.getExtras();
        IBinder binder =
                extras.getBinder(CarContext.EXTRA_ON_REQUEST_PERMISSIONS_RESULT_LISTENER_KEY);
        IOnRequestPermissionsListener listener =
                IOnRequestPermissionsListener.Stub.asInterface(binder);
        String[] permissions = extras.getStringArray(CarContext.EXTRA_PERMISSIONS_KEY);

        if (listener == null || permissions == null) {
            Log.e(LogTags.TAG, "Intent to request permissions is missing the callback binder");
            finish();
            return;
        }

        ActivityResultLauncher<String[]> launcher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        grantedMap -> {
                            List<String> granted = new ArrayList<>();
                            List<String> rejected = new ArrayList<>();

                            for (Map.Entry<String, Boolean> entry : grantedMap.entrySet()) {
                                Boolean isGranted = entry.getValue();
                                if (isGranted != null && isGranted) {
                                    granted.add(entry.getKey());
                                } else {
                                    rejected.add(entry.getKey());
                                }
                            }

                            try {
                                listener.onRequestPermissionsResult(granted.toArray(new String[0]),
                                        rejected.toArray(new String[0]));
                            } catch (RemoteException e) {
                                // This is impossible since it is running in the same process.
                                throw new IllegalStateException(e);
                            }

                            finish();
                        });
        launcher.launch(permissions);
    }
}
