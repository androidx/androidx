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

package androidx.camera.featurecombinationquery.playservices;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;

import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatProvider;

import com.google.android.gms.camera.feature.combination.query.CombinationQuery;
import com.google.android.gms.camera.feature.combination.query.ModuleUnavailableException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A Google Play Services based {@link CameraDeviceSetupCompat} implementation.
 *
 * <p>This class is internal only and app cannot instantiate it directly. Instead app will
 * depend on the featurecombinationquery-play-services artifact to get an instance of
 * this class via the {@link CameraDeviceSetupCompatFactory#getCameraDeviceSetupCompat} API.
 */
public class PlayServicesCameraDeviceSetupCompatProvider implements
        CameraDeviceSetupCompatProvider {
    private static final String TAG =
            PlayServicesCameraDeviceSetupCompatProvider.class.getSimpleName();
    private static final int INITIALIZATION_TIMEOUT_MS = 500;
    /**
     * ConditionVariable that is opened after initialization of mQueryClient is complete. Allows
     * consumers of mQueryClient to wait until initialization is complete before attempting to
     * access it.
     */
    private static final ConditionVariable sInitCondVar = new ConditionVariable(false);
    private static final Object sLock = new Object();
    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    /**
     * Singleton {@link CombinationQuery} instance. This instance pulls in the implementation from
     * Play Services.
     */
    @Nullable
    private static volatile CombinationQuery sQueryClient = null;
    private static volatile boolean sInitialized = false;

    public PlayServicesCameraDeviceSetupCompatProvider(@NonNull Context context) {
        // Early throwaway check. sInitialized == false could mean this is the first instance of
        // PlayServicesCameraDeviceSetupCompatProvider created, or another instance is in the
        // process of initializing QueryClient.
        if (sInitialized) {
            return;
        }

        sExecutor.execute(() -> {
            synchronized (sLock) {
                if (sInitialized) {
                    return;
                }
                try {
                    sQueryClient = CombinationQuery.getCombinationQueryClient(
                            context.getApplicationContext());
                } catch (ModuleUnavailableException e) {
                    Log.e(TAG,
                            "Could not connect to Play Services Feature Combination Query Client.");
                } finally {
                    sInitialized = true;
                    sInitCondVar.open();
                }
            }
        });
    }

    @Override
    public @NonNull CameraDeviceSetupCompat getCameraDeviceSetupCompat(@NonNull String cameraId) {
        // This is needed to prevent situations where calls like
        // (new CameraDeviceSetupFactory(context)).getCameraDeviceSetupCompat(cameraId), or Kotlin's
        // more convenient CameraDeviceSetupFactory(context).getCameraDeviceSetupCompat(cameraId),
        // sometimes ends up creating PlayServicesCameraDeviceSetupCompat objects with null
        // QueryClient due to scheduling of the background task. This is an anti pattern, and will
        // block the calling thread until initialization is done.
        if (!sInitialized) {
            Log.w(TAG, "getCameraDeviceSetup called before connection to Play Store Services was "
                    + "completely initialized. Blocking until connection is resolved. To "
                    + "prevent this warning, consider initializing "
                    + "CameraDeviceSetupCompatFactory well ahead of calling "
                    + "getCameraDeviceSetupCompat.");
            if (!sInitCondVar.block(INITIALIZATION_TIMEOUT_MS)) {
                Log.w(TAG, "Could not connect to Play Services in " + INITIALIZATION_TIMEOUT_MS
                        + "ms. Proceeding as if database is missing.");
                return new PlayServicesCameraDeviceSetupCompat(null, cameraId);
            }
        }
        return new PlayServicesCameraDeviceSetupCompat(sQueryClient, cameraId);
    }
}
