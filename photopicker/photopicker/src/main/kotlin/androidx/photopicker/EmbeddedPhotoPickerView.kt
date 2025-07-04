/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.photopicker

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerProviderFactory
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.RequiresExtension

/**
 * A custom [ViewGroup] which manages an underlying [SurfaceView] for connecting to and hosting the
 * Embedded PhotoPicker.
 */
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@ExperimentalPhotoPickerApi
public class EmbeddedPhotoPickerView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    internal companion object {
        internal const val TAG: String = "EmbeddedPhotoPickerView"
    }

    private val listeners: MutableList<EmbeddedPhotoPickerStateChangeListener> = mutableListOf()

    private val surfaceView: SurfaceView =
        SurfaceView(context).apply {
            // Initially make the SurfaceView invisible so that the empty surface isn't visible to
            // the user. When the session is opened, the SurfaceView needs to be marked as VISIBLE.
            setVisibility(INVISIBLE)
        }

    private var embeddedPhotoPickerProvider: EmbeddedPhotoPickerProvider? = null
    private var embeddedFeatureInfo: EmbeddedPhotoPickerFeatureInfo? = null
    private var session: EmbeddedPhotoPickerSession? = null
    private val displayId = context.display.displayId
    private var initialized: Boolean = false

    init {
        addView(surfaceView)

        // The EmbeddedPhotoPicker wants to draw above the window to prevent other UI elements from
        // drawing on top of it.
        surfaceView.setZOrderOnTop(true)

        initialized = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int): Unit =
        internalOnLayout(left, top, right, bottom)

    internal fun internalOnLayout(left: Int, top: Int, right: Int, bottom: Int) {
        getChildAt(0).also { child -> child?.layout(left, top, right, bottom) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // subtract paddings for calculating available width for child views
        val width = maxOf(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight)
        val height = maxOf(0, MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom)
        // Create measure spec
        child.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)),
        )
        // Set measurements
        setMeasuredDimension(
            child.measuredWidth + paddingLeft + paddingRight,
            child.measuredHeight + paddingTop + paddingBottom,
        )
    }

    /**
     * Configuration changes need to be passed to both the underlying SurfaceView and any open
     * session that is currently running.
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        newConfig?.let {
            // Notify the remote session if it exists so the remote view receives the configuration
            // change.
            session?.notifyConfigurationChanged(it)
        }
    }

    /**
     * Layout size changes are passed to any open sessions so the view can be resized on the
     * Session's side.
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        session?.notifyResized(width, height)
    }

    /**
     * Hook into the View's setVisibility calls to ensure that changes to this view's visibility is
     * passed to any open session.
     */
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

        when (visibility) {
            INVISIBLE,
            GONE -> session?.let { it.notifyVisibilityChanged(false) }
            VISIBLE -> session?.let { it.notifyVisibilityChanged(true) }
            else -> {}
        }
    }

    /** When this ViewGroup is removed from a window cleanup any attached session. */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closeSession()
    }

    /**
     * Hook into ViewGroup's visibility changes to ensure that changes to this view's visibility is
     * passed to any open session.
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)

        when (visibility) {
            INVISIBLE,
            GONE -> session?.let { it.notifyVisibilityChanged(false) }
            VISIBLE -> session?.let { it.notifyVisibilityChanged(true) }
            else -> {}
        }
    }

    // Below: enforce restrictions on adding child views to this ViewGroup

    override fun addView(child: View?) {
        checkAddView()
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        checkAddView()
        super.addView(child, index)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        checkAddView()
        super.addView(child, width, height)
    }

    override fun addView(child: View?, params: LayoutParams?) {
        checkAddView()
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        checkAddView()
        super.addView(child, index, params)
    }

    override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params)
    }

    private fun checkAddView() {
        if (initialized) {
            throw UnsupportedOperationException(
                "Cannot add views to " +
                    "${javaClass.simpleName}; This View does not support additional children."
            )
        }
    }

    /**
     * Set the [EmbeddedPhotoPickerProvider] that this view should use to initialize the
     * EmbeddedPhotoPicker.
     *
     * This allows for a test implementation to be set for testing purposes, but can be ignored for
     * regular use. By default, the [EmbeddedPhotoPickerView] will use
     * [EmbeddedPhotoPickerProviderFactory] with the current applicationContext so that the service
     * connection survives any Activity recreation.
     */
    public fun setProvider(provider: EmbeddedPhotoPickerProvider) {
        // If a session exists, close it.
        session?.let {
            Log.w(TAG, "Provider was changed while a session was still open.")
            it.close()
            session = null
        }

        embeddedPhotoPickerProvider = provider
        maybeOpenSession()
    }

    /**
     * Sets the [EmbeddedPhotoPickerFeatureInfo] object that will be passed to the
     * [EmbeddedPhotoPickerProvider] when the session is started.
     */
    public fun setEmbeddedPhotoPickerFeatureInfo(featureInfo: EmbeddedPhotoPickerFeatureInfo) {
        embeddedFeatureInfo = featureInfo
        maybeOpenSession()
    }

    /**
     * Generate an internal client that will proxy operations to the host app's provided client (if
     * it exists.)
     *
     * This allows the view to hook into session callbacks and act as accordingly.
     *
     * These callbacks all run on the View's UI thread via `getHandler()::post`
     */
    private fun createClient(): EmbeddedPhotoPickerClient {
        return object : EmbeddedPhotoPickerClient {
            override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {

                // When the session has been received, the SurfaceView can be setup
                // with the incoming surface package, and then the SurfaceView can be shown
                // as the Embedded Photopicker's UI is already running.
                session = newSession
                surfaceView.setChildSurfacePackage(newSession.surfacePackage)
                surfaceView.setVisibility(VISIBLE)

                listeners.forEach { it.onSessionOpened(newSession) }
            }

            override fun onSessionError(throwable: Throwable) {
                listeners.forEach { it.onSessionError(throwable) }
            }

            override fun onUriPermissionGranted(uris: List<Uri>) {
                listeners.forEach { it.onUriPermissionGranted(uris) }
            }

            override fun onUriPermissionRevoked(uris: List<Uri>) {
                listeners.forEach { it.onUriPermissionRevoked(uris) }
            }

            override fun onSelectionComplete() {
                listeners.forEach { it.onSelectionComplete() }
            }
        }
    }

    internal fun maybeOpenSession() {
        if (session == null && embeddedFeatureInfo != null) {
            openSession(checkNotNull(embeddedFeatureInfo))
        }
    }

    /** Open a new [EmbeddedPhotoPickerSession] with the provided list of features. */
    internal fun openSession(embeddedPhotoPickerFeatureInfo: EmbeddedPhotoPickerFeatureInfo) {
        val provider =
            checkNotNull(embeddedPhotoPickerProvider) {
                "Session cannot be opened without a provider."
            }
        // Use post here so that early calls to openSession don't happen until
        // layout/measurement of the surface view is complete.
        post({
            provider.openSession(
                @Suppress("DEPRECATION")
                /* hostToken =       */ checkNotNull(surfaceView.hostToken) {
                    "Cannot open session with a null surfaceHostToken"
                },
                /* displayId =       */ displayId,
                /* width =           */ width,
                /* height =          */ height,
                /* featureInfo =     */ embeddedPhotoPickerFeatureInfo,

                // Force all client callbacks to the UI thread
                /* clientExecutor =  */ getHandler()::post,
                /* callback =        */ createClient(),
            )
        })
    }

    /**
     * Mark the current [EmbeddedPhotoPickerSession] as closed.
     *
     * This should be called when the Session is no longer needed, before a new session can be
     * started.
     *
     * This call will be ignored if there is no current session active.
     */
    internal fun closeSession() {
        session?.let { it.close() }
    }

    /** Adds an [EmbeddedPhotoPickerStateChangeListener] to this view. */
    public fun addEmbeddedPhotoPickerStateChangeListener(
        listener: EmbeddedPhotoPickerStateChangeListener
    ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /** Removes an [EmbeddedPhotoPickerStateChangeListener] from this view. */
    public fun removeEmbeddedPhotoPickerStateChangeListener(
        listener: EmbeddedPhotoPickerStateChangeListener
    ) {
        if (listeners.contains(listener)) {
            listeners.remove(listener)
        }
    }

    /**
     * A state change listener which supplies callbacks for state changes related to any currently
     * running Embedded PhotoPicker session.
     */
    public interface EmbeddedPhotoPickerStateChangeListener {

        /**
         * Reports that session of app with photopicker was established successfully. Also shares
         * [EmbeddedPhotoPickerSession] handle containing the view with the caller that should be
         * used to notify the session of UI events.
         */
        public fun onSessionOpened(newSession: EmbeddedPhotoPickerSession)

        /**
         * Reports that terminal error has occurred in the session. Any further events notified on
         * this session will be ignored. The embedded photopicker view will be torn down along with
         * session upon error.
         */
        public fun onSessionError(throwable: Throwable)

        /**
         * Reports that URI permission has been granted to the item selected by the user.
         *
         * It is possible that the permission to the URI was revoked if the item was unselected by
         * user before the URI is actually accessed by the caller. Hence callers must handle
         * [SecurityException] when attempting to read or use the URI in response to this callback.
         */
        public fun onUriPermissionGranted(uris: List<Uri>)

        /** Reports that URI permission has been revoked of the item deselected by the user. */
        public fun onUriPermissionRevoked(uris: List<Uri>)

        /**
         * Reports that the user is done with their selection and should collapse the picker.
         *
         * This doesn't necessarily mean that the session should be closed, but rather the user has
         * indicated that they are done selecting images and should go back to the app.
         */
        public fun onSelectionComplete()
    }
}
