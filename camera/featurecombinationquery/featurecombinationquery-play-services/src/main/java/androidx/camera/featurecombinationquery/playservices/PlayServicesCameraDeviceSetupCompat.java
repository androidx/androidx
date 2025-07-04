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

import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_SUPPORTED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNDEFINED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNSUPPORTED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.SOURCE_PLAY_SERVICES;

import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory;
import androidx.camera.featurecombinationquery.SessionConfigurationLegacy;

import com.google.android.gms.camera.feature.combination.query.CombinationQuery;
import com.google.android.gms.camera.feature.combination.query.QueryResult;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A Google Play Services based {@link CameraDeviceSetupCompat} implementation.
 *
 * <p>This class is internal only and app cannot instantiate it directly. Instead app will
 * depend on the featurecombinationquery-play-services artifact to get an instance of
 * this class via the {@link CameraDeviceSetupCompatFactory#getCameraDeviceSetupCompat} API.
 */
public class PlayServicesCameraDeviceSetupCompat implements CameraDeviceSetupCompat {
    private static final String TAG = PlayServicesCameraDeviceSetupCompat.class.getSimpleName();

    @Nullable
    private final CombinationQuery mQueryClient;
    private final String mCameraId;

    public PlayServicesCameraDeviceSetupCompat(@Nullable CombinationQuery queryClient,
            @NonNull String cameraId) {
        this.mQueryClient = queryClient;
        this.mCameraId = cameraId;
    }

    @Override
    public @NonNull SupportQueryResult isSessionConfigurationSupported(
            @NonNull SessionConfiguration sessionConfig) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // SessionConfiguration was added in Android P. We have no way to query stuff before
            // that. Return early.
            return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES, 0);
        }

        if (mQueryClient == null) {
            return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES, 0);
        }

        try {
            QueryResult result = mQueryClient.isSessionConfigurationSupported(mCameraId,
                    sessionConfig);
            return fromPlayServicesQueryResult(result);
        } catch (RemoteException e) {
            Log.e(TAG, "Connection to play services broken. Failed to query feature combination",
                    e);
            return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES, 0);
        }
    }

    @Override
    public @NonNull SupportQueryResult isSessionConfigurationSupportedLegacy(
            @NonNull SessionConfigurationLegacy sessionConfig) {
        if (mQueryClient == null) {
            return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES, 0);
        }

        try {
            QueryResult result = mQueryClient.isSessionConfigurationSupportedLegacy(mCameraId,
                    sessionConfig.getOutputConfigurations(),
                    sessionConfig.getSessionParameters().asMap());
            return fromPlayServicesQueryResult(result);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not query play services for querying feature combination", e);
            return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES, 0);
        }
    }

    private static @NonNull SupportQueryResult fromPlayServicesQueryResult(
            @NonNull QueryResult queryResult) {
        switch (queryResult.getResult()) {
            case QueryResult.Result.NOT_SUPPORTED:
                return new SupportQueryResult(RESULT_UNSUPPORTED, SOURCE_PLAY_SERVICES,
                        queryResult.getTimestamp());
            case QueryResult.Result.SUPPORTED:
                return new SupportQueryResult(RESULT_SUPPORTED, SOURCE_PLAY_SERVICES,
                        queryResult.getTimestamp());
            default:
                return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_PLAY_SERVICES,
                        queryResult.getTimestamp());
        }
    }
}
