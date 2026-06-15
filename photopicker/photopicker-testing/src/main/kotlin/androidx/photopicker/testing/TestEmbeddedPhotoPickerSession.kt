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
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import java.util.Collections.synchronizedList

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
public class TestEmbeddedPhotoPickerSession(
    context: Context,
    private val hostToken: IBinder,
    private val displayId: Int,
    private val width: Int,
    private val height: Int,
    public val featureInfo: EmbeddedPhotoPickerFeatureInfo,
    private val clientCallback: EmbeddedPhotoPickerClient,
) : EmbeddedPhotoPickerSession {

    private val _selectedUris: MutableList<Uri> =
        synchronizedList(featureInfo.preSelectedUris.toMutableList())

    /** The list of URIs that are currently selected in this session. */
    public val selectedUris: List<Uri>
        get() = _selectedUris

    /** Boolean flag indicating whether this session has been closed or not. */
    @Volatile
    public var isClosed: Boolean = false
        private set

    /** The view that represents the embedded photo picker. */
    public val view: View
        get() = _view

    /** The last configuration received by this session via [notifyConfigurationChanged]. */
    @Volatile
    public var lastConfiguration: Configuration? = null
        private set

    /** The last expanded state received by this session via [notifyPhotoPickerExpanded]. */
    @Volatile
    @Suppress("AutoBoxing")
    public var lastExpandedState: Boolean? = null
        private set

    /** The last visibility state received by this session via [notifyVisibilityChanged]. */
    @Volatile
    @Suppress("AutoBoxing")
    public var lastNotifiedVisibility: Boolean? = null
        private set

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
     *
     * NotCloseable is suppressed because this class implements the close() method from the
     * EmbeddedPhotoPickerSession interface, which itself does not implement AutoCloseable.
     */
    @MainThread
    @Suppress("NotCloseable")
    override fun close() {

        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw IllegalStateException("Cannot invoke close on a background thread")
        }

        isClosed = true
        _host.release()
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        lastConfiguration = configuration
        _view.dispatchConfigurationChanged(configuration)
    }

    override fun notifyPhotoPickerExpanded(isExpanded: Boolean) {
        lastExpandedState = isExpanded
    }

    override fun notifyResized(width: Int, height: Int) {
        _host.relayout(width, height)
    }

    override fun notifyVisibilityChanged(isVisible: Boolean) {
        lastNotifiedVisibility = isVisible
    }

    override fun requestRevokeUriPermission(uris: List<Uri>) {
        _selectedUris.removeAll(uris)
        clientCallback.onUriPermissionRevoked(uris)
    }

    /**
     * Creates the [SurfaceControlViewHost] which owns the
     * [android.view.SurfaceControlViewHost.SurfacePackage] that will be used for remote rendering
     * the Photopicker's [ComposeView] inside the client app's [android.view.SurfaceView].
     *
     * SurfaceControlViewHost needs to be created on the Main thread, so this method will spawn a
     * coroutine on the @Main dispatcher and block until that coroutine has completed.
     *
     * @param context The service context
     * @param displayId the displayId to locate the display for the [SurfaceControlViewHost]. This
     *   must resolve to a corresponding display in [DisplayManager] or the Session will crash.
     * @param hostToken A [IBinder] token from the client to pass to the [SurfaceControlViewHost]
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

    /** Test only API to direct the [EmbeddedPhotoPickerSession] to throw an error to the client. */
    public fun notifySessionError(throwable: Throwable) {
        clientCallback.onSessionError(throwable)
    }

    /**
     * Test only API to direct the [EmbeddedPhotoPickerSession] to emit the provided list of [Uri]
     * as selected by the user.
     */
    public fun selectUris(uris: List<Uri>) {
        _selectedUris.addAll(uris)
        clientCallback.onUriPermissionGranted(uris)
    }

    /**
     * Test only API to direct the [EmbeddedPhotoPickerSession] to emit the provided list of [Uri]
     * as deselected by the user.
     */
    public fun deselectUris(uris: List<Uri>) {
        _selectedUris.removeAll(uris)
        clientCallback.onUriPermissionRevoked(uris)
    }

    /**
     * Test only API to direct the [EmbeddedPhotoPickerSession] to emit the user has completed
     * selection media.
     */
    public fun notifySelectionComplete() {
        clientCallback.onSelectionComplete()
    }
}
