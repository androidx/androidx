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

package androidx.camera.featurecombinationquery;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link CameraDeviceSetupCompat} instances.
 */
public class CameraDeviceSetupCompatFactory {

    private static final String PLAY_SERVICES_IMPL_KEY =
            "androidx.camera.featurecombinationquery.PLAY_SERVICES_IMPL_PROVIDER_KEY";

    private final Context mContext;

    // Cached provider for Play Services implementation.
    @Nullable
    private CameraDeviceSetupCompatProvider mPlayServicesProvider;
    // Cached provider for Camera2 implementation.
    @Nullable
    private CameraDeviceSetupCompatProvider mCamera2Provider;

    /**
     * Creates a new instance of {@link CameraDeviceSetupCompatFactory}.
     *
     * @param context The context to use for creating {@link CameraDeviceSetupCompat} instances.
     */
    public CameraDeviceSetupCompatFactory(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Gets a new instance of {@link CameraDeviceSetupCompat} for the given camera ID.
     *
     * <p> The returned instance aggregates the results from both the Play Services and the
     * Android framework. It first checks if a Play Services implementation exists, and if so,
     * return the query result from the Play Services implementation. If no Play Services
     * implementation exists or the result from Play Services is undefined, the returned instance
     * will then query Android framework for the result, if running on a new enough version.
     * Sample code:
     *
     * <pre><code>
     * // Query the compatibility before opening the camera.
     * CameraDeviceSetupCompatFactory factory = new CameraDeviceSetupCompatFactory(context);
     * CameraDeviceSetupCompat cameraDeviceSetupCompat
     *     = factory.getCameraDeviceSetupCompat(cameraId);
     * Result result
     *     = cameraDeviceSetupCompat.isSessionConfigurationSupported(sessionConfiguration);
     * boolean supported = result.getValue() == CameraDeviceSetupCompat.RESULT_SUPPORTED;
     * </code></pre>
     *
     * <p> To include the Play Services as a source, the app must depend on the
     * camera-feature-combination-query-play-services artifact.
     *
     * <p> If the return value is
     * {@link CameraDeviceSetupCompat.SupportQueryResult#RESULT_UNDEFINED}, on API level between
     * 29 and 34 inclusive, it's also possible to further check if the session
     * configuration is supported by querying the
     * {@link CameraDevice#isSessionConfigurationSupported}. This approach requires
     * opening the camera first which may introduce latency, so the AndroidX implementation does not
     * include this code path by default. Additionally, this approach does not check the values
     * set in {@link SessionConfiguration#setSessionParameters}. For example, the FPS range is
     * ignored. Sample code:
     *
     * <pre><code>
     * if (SDK_INT <= 34 && SDK_INT >= 29) {
     *   // Check if the session configuration is supported with an opened CameraDevice.
     *   try {
     *     supported = supported || (result.getValue() == RESULT_UNDEFINED &&
     *         cameraDevice.isSessionConfigurationSupported(sessionConfiguration));
     *   } catch (UnsupportedOperationException unsupportedException) {
     *     // CameraDevice may throw UnsupportedOperationException when the config is not supported.
     *   }
     * }
     * </code></pre>
     *
     * @throws IllegalStateException    If the Play Services implementation exists but the library
     *                                  fails to instantiate it. For example, if there are multiple
     *                                  Play Services implementations in the manifest.
     * @throws IllegalArgumentException If {@code cameraId} is null, or if {@code cameraId} does not
     *                                  match any device in {@link CameraManager#getCameraIdList()}.
     * @throws CameraAccessException    If the camera has encountered a fatal error.
     * @see CameraDevice#isSessionConfigurationSupported
     * @see CameraDevice.CameraDeviceSetup#isSessionConfigurationSupported
     */
    @NonNull
    public CameraDeviceSetupCompat getCameraDeviceSetupCompat(@NonNull String cameraId)
            throws CameraAccessException {
        List<CameraDeviceSetupCompat> impls = new ArrayList<>();
        if (mPlayServicesProvider == null) {
            // Create Play Services implementation if there isn't a cached one.
            mPlayServicesProvider = getPlayServicesCameraDeviceSetupCompatProvider();
        }
        if (mPlayServicesProvider != null) {
            // Add the Play Services implementation if the app contains that dependency.
            impls.add(mPlayServicesProvider.getCameraDeviceSetupCompat(cameraId));
        }
        if (SDK_INT >= 35) {
            try {
                if (mCamera2Provider == null) {
                    // Create the camera2 implementation if there isn't a cached one.
                    mCamera2Provider = new Camera2CameraDeviceSetupCompatProvider(mContext);
                }
                impls.add(mCamera2Provider.getCameraDeviceSetupCompat(cameraId));
            } catch (UnsupportedOperationException e) {
                // This can throw UnsupportedOperationException for Android V upgrade devices. In
                // that case, we treat it as SDK_INT < 35 and ignore.
            }
        }
        return new AggregatedCameraDeviceSetupCompat(impls);
    }

    /**
     * Returns a new instance of {@link CameraDeviceSetupCompatProvider}.
     *
     * @return The Play Services CameraDeviceSetupCompat implementation, or null if not found.
     * @throws IllegalStateException if multiple Play Services CameraDeviceSetupCompat
     *                               implementations are found, or failed to instantiate the
     *                               implementation.
     */
    @Nullable
    private CameraDeviceSetupCompatProvider getPlayServicesCameraDeviceSetupCompatProvider()
            throws IllegalStateException {
        PackageInfo packageInfo;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(),
                    PackageManager.GET_META_DATA | PackageManager.GET_SERVICES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }

        String playServiceImplClassName = null;
        if (packageInfo.services == null) {
            return null;
        }
        for (ServiceInfo serviceInfo : packageInfo.services) {
            if (serviceInfo.metaData == null) {
                continue;
            }
            // Try to load the play services impl class name from the Service metadata
            // in camera-feature-combination-query-play-services manifest.
            String className = serviceInfo.metaData.getString(PLAY_SERVICES_IMPL_KEY);
            if (className != null) {
                if (playServiceImplClassName != null) {
                    throw new IllegalStateException(
                            "Multiple Play Services CameraDeviceSetupCompat implementations"
                                    + " found in the manifest.");
                }
                playServiceImplClassName = className;
            }
        }
        if (playServiceImplClassName == null) {
            // The app does not depend on a Play Services implementation.
            return null;
        }
        return instantiatePlayServicesImplProvider(playServiceImplClassName);
    }

    /**
     * Instantiates a Play Services CameraDeviceSetupCompat provider implementation.
     *
     * @param className The class name of the implementation.
     * @return The instantiated implementation.
     */
    private CameraDeviceSetupCompatProvider instantiatePlayServicesImplProvider(
            @NonNull String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (CameraDeviceSetupCompatProvider) clazz.getConstructor(Context.class)
                    .newInstance(mContext);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to instantiate Play Services CameraDeviceSetupCompat implementation",
                    e);
        }
    }
}
