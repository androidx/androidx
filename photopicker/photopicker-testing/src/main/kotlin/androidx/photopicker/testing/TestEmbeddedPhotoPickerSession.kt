/* Copyright (C) 2025 The Android Open Source Project
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

package androidx.photopicker.testing

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.view.SurfaceControlViewHost
import android.view.View
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.MainThread
import androidx.annotation.RequiresExtension

/**
 * A test implementation of [EmbeddedPhotoPickerSession] that sets up & behaves similarly to the
 * [EmbeddedPhotoPickerSession] except it attaches an empty [View] rather than the regular
 * PhotoPicker embedded view.
 *
 * Callbacks to the client can be initiated with the [TestEmbeddedPhotoPickerProvider] to allow
 * faking user interactions.
 *
 * @property context
 * @property hostToken
 * @property displayId
 * @property width
 * @property height
 * @property featureInfo
 * @property clientCallback
 */
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
internal class TestEmbeddedPhotoPickerSession(
    context: Context,
    private val hostToken: IBinder,
    private val displayId: Int,
    private val width: Int,
    private val height: Int,
    private val featureInfo: EmbeddedPhotoPickerFeatureInfo,
    private val clientCallback: EmbeddedPhotoPickerClient,
) : EmbeddedPhotoPickerSession {

    private val _view: View
    private val _host: SurfaceControlViewHost

    init {
        _view = View(context)
        _host = createSurfaceControlViewHost(context, displayId, hostToken)
        _host.setView(_view, width, height)
    }

    override fun getSurfacePackage(): SurfaceControlViewHost.SurfacePackage {
        return checkNotNull(_host.surfacePackage) { "SurfacePackage was null." }
    }

    /*
     * [SurfaceControlViewHost] has issues if not closed on the MainThread, so this will
     * throw an error if not called from the main thread.
     */
    @MainThread
    override fun close() {

        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw IllegalStateException("Cannot invoke close on a background thread")
        }

        _host.release()
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        _view.dispatchConfigurationChanged(configuration)
    }

    /* NoOp for test implementation. */
    override fun notifyPhotoPickerExpanded(isExpanded: Boolean) {}

    override fun notifyResized(width: Int, height: Int) {
        _host.relayout(width, height)
    }

    /* NoOp for test implementation. */
    override fun notifyVisibilityChanged(isVisible: Boolean) {}

    /*
     * NoOp for test implementation.
     *
     * Note: We could call the client here via [EmbeddedPhotoPickerClient#onUriPermissionRevoked],
     * however the actual PhotoPicker session implementation does not make this call, so this is
     * intentionally excluded to mirror the same behavior.
     */
    override fun requestRevokeUriPermission(uris: List<Uri>) {}

    /**
     * Creates the [SurfaceControlViewHost] which owns the [SurfacePackage] that will be used for
     * remote rendering the Photopicker's [ComposeView] inside the client app's [SurfaceView].
     *
     * SurfaceControlViewHost needs to be created on the Main thread, so this method will spawn a
     * coroutine on the @Main dispatcher and block until that coroutine has completed.
     *
     * @param context The service context
     * @param displayId the displayId to locate the display for the [SurfaceControlViewHost]. This
     *   must resolve to a corresponding display in [DisplayManager] or the Session will crash.
     * @param hostToken A [Binder] token from the client to pass to the [SurfaceControlViewHost]
     */
    private fun createSurfaceControlViewHost(
        context: Context,
        displayId: Int,
        hostToken: IBinder,
    ): SurfaceControlViewHost {
        val displayManager: DisplayManager = context.getSystemService(DisplayManager::class.java)
        val display =
            checkNotNull(displayManager.getDisplay(displayId)) {
                "The displayId provided to openSession did not result in a valid display."
            }
        return SurfaceControlViewHost(context, display, hostToken)
    }
}
