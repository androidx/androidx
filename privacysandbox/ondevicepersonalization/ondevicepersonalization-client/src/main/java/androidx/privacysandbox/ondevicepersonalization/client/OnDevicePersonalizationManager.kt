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

package androidx.privacysandbox.ondevicepersonalization.client

import android.adservices.ondevicepersonalization.SurfacePackageToken
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.ext.SdkExtensions
import android.view.SurfaceControlViewHost

/**
 * OnDevicePersonalizationManager provides APIs for apps to load an
 * [IsolatedService][android.adservices.ondevicepersonalization.IsolatedService] in an isolated
 * process and interact with it.
 *
 * An app can request an
 * [IsolatedService][android.adservices.ondevicepersonalization.IsolatedService] to generate content
 * for display within an [SurfaceView][android.view.SurfaceView] within the app's view hierarchy,
 * and also write persistent results to on-device storage which can be consumed by Federated
 * Analytics for cross-device statistical analysis or by Federated Learning for model training. The
 * displayed content and the persistent output are both not directly accessible by the calling app.
 */
public abstract class OnDevicePersonalizationManager internal constructor() {
    /**
     * Executes an [IsolatedService][android.adservices.ondevicepersonalization.IsolatedService] in
     * the OnDevicePersonalization sandbox bound to an isolated process. Upon execution completion,
     * the returned response object contains tokens that can be subsequently used to display results
     * in a [SurfaceView][android.view.SurfaceView] within the calling app. If the service returned
     * a [RenderingConfig][android.adservices.ondevicepersonalization.RenderingConfig] to be
     * displayed, [ExecuteInIsolatedServiceResponse.surfacePackageToken] can be used in a subsequent
     * [requestSurfacePackage] call to display the result in a view. The SurfacePackageToken may be
     * null to indicate that no output is expected to be displayed for this request.
     *
     * @param executeInIsolatedServiceRequest
     * @return [ExecuteInIsolatedServiceResponse]
     * @throws android.adservices.ondevicepersonalization.OnDevicePersonalizationException If
     *   execution of the handler fails with an error code.
     * @throws android.content.pm.PackageManager.NameNotFoundException On older versions if the
     *   handler package is not installed or does not have a valid ODP manifest
     * @throws ClassNotFoundException On older versions if the handler class is not found.
     */
    public abstract suspend fun executeInIsolatedService(
        executeInIsolatedServiceRequest: ExecuteInIsolatedServiceRequest
    ): ExecuteInIsolatedServiceResponse

    /**
     * Requests a [SurfacePackage][android.view.SurfaceControlViewHost.SurfacePackage] to be
     * inserted into a [SurfaceView][android.view.SurfaceView] inside the calling app.
     *
     * @param surfacePackageToken a reference to a [SurfacePackageToken] returned by a prior call to
     *   [executeInIsolatedService].
     * @param surfaceViewHostToken the hostToken of the [SurfaceView][android.view.SurfaceView],
     *   which is returned by [android.view.SurfaceView.getHostToken] after the
     *   [SurfaceView][android.view.SurfaceView] has been added to the view hierarchy.
     * @param displayId the integer ID of the logical display on which to display the
     *   [SurfaceControlViewHost.SurfacePackage], returned by Context.getDisplay().getDisplayId().
     * @param width the width of the [SurfaceControlViewHost.SurfacePackage] in pixels.
     * @param height the height of the [SurfaceControlViewHost.SurfacePackage] in pixels.
     * @return A surface package containing a [View][android.view.View] with the content from a
     *   result of a prior call to [executeInIsolatedService] running in the OnDevicePersonalization
     *   sandbox.
     * @throws android.adservices.ondevicepersonalization.OnDevicePersonalizationException if
     *   execution of the handler fails with an error code.
     */
    public abstract suspend fun requestSurfacePackage(
        surfacePackageToken: SurfacePackageToken,
        surfaceViewHostToken: IBinder,
        displayId: Int,
        width: Int,
        height: Int,
    ): SurfaceControlViewHost.SurfacePackage

    public companion object {
        /**
         * Creates [OnDevicePersonalizationManager].
         *
         * @return OnDevicePersonalizationManager object. If the device is running an incompatible
         *   build, the value returned is `null`.
         */
        @SuppressLint("NewApi")
        @JvmStatic
        public fun obtain(context: Context): OnDevicePersonalizationManager? {
            return if (adServicesVersion() >= 12) {
                OnDevicePersonalizationManagerAPI33Ext12Impl(context)
            } else {
                null
            }
        }

        private fun adServicesVersion(): Int {
            return if (Build.VERSION.SDK_INT >= 33) {
                SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
            } else {
                0
            }
        }
    }
}
