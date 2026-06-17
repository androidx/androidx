/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.locationbutton

import android.app.Activity
import android.app.permissionui.LocationButtonClient
import android.app.permissionui.LocationButtonProvider
import android.app.permissionui.LocationButtonProviderFactory
import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Trace
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
internal class RemoteLocationButtonDelegateApi37(
    private val view: LocationButton,
    private val context: Context,
) : RemoteLocationButtonDelegate {
    private var provider: LocationButtonProvider? = null
    private var session: LocationButtonSession? = null
    private var compositionOrder = LocationButton.DEFAULT_COMPOSITION_ORDER

    init {
        provider = LocationButtonProviderFactory.create(context)
    }

    fun setProvider(provider: LocationButtonProvider?) {
        this.provider = provider
    }

    override fun isSessionActive(): Boolean = session != null

    override fun closeSession(surfaceView: SurfaceView?) {
        session?.close()
        session = null
        surfaceView?.clearChildSurfacePackage()
        surfaceView?.visibility = View.INVISIBLE
    }

    override fun onSizeChanged(w: Int, h: Int) {
        session?.resize(w, h)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        session?.setPadding(left, top, right, bottom)
    }

    override fun changeConfiguration(newConfig: Configuration) {
        session?.changeConfiguration(newConfig)
    }

    override fun setCornerRadius(radius: Float) {
        session?.setCornerRadius(radius)
    }

    override fun setPressedCornerRadius(radius: Float) {
        session?.setPressedCornerRadius(radius)
    }

    override fun setTextColor(color: Int) {
        session?.setTextColor(color)
    }

    override fun setBackgroundColor(color: Int) {
        session?.setBackgroundColor(color)
    }

    override fun setIconTint(color: Int) {
        session?.setIconTint(color)
    }

    override fun setStrokeColor(color: Int) {
        session?.setStrokeColor(color)
    }

    override fun setStrokeWidth(strokeWidth: Int) {
        session?.setStrokeWidth(strokeWidth)
    }

    override fun setTextType(textType: Int) {
        session?.setTextType(textType)
    }

    fun createButtonRequest(): LocationButtonRequest {
        return LocationButtonRequest.Builder(view.width, view.height, view.resources.configuration)
            .setPaddingLeft(view.safePaddingLeft)
            .setPaddingTop(view.safePaddingTop)
            .setPaddingRight(view.safePaddingRight)
            .setPaddingBottom(view.safePaddingBottom)
            .setBackgroundColor(view.backgroundColor)
            .setStrokeColor(view.strokeColor)
            .setStrokeWidth(view.strokeWidth)
            .setCornerRadius(view.cornerRadius)
            .setIconTint(view.iconTint)
            .setTextType(view.textType)
            .setTextColor(view.textColor)
            .setPressedCornerRadius(view.pressedCornerRadius)
            .build()
    }

    override fun openSession(activity: Activity, displayId: Int, surfaceView: SurfaceView) {
        Trace.beginAsyncSection("LocationButton.openSession", 0)

        @Suppress("DEPRECATION")
        val hostToken =
            surfaceView.hostToken
                ?: run {
                    Trace.endAsyncSection("LocationButton.openSession", 0)
                    return
                }

        val request = createButtonRequest()

        val clientCallback =
            object : LocationButtonClient {
                override fun onPermissionResult(granted: Boolean) {
                    view.onPermissionResultListener?.onPermissionResult(granted)
                }

                override fun onSessionError(t: Throwable) {
                    closeSession(surfaceView)
                    view.onErrorListener?.onError(t)
                }

                override fun onSessionOpened(openedSession: LocationButtonSession) {
                    try {
                        if (!view.isAttachedToWindow) {
                            openedSession.close()
                            return
                        }

                        session = openedSession
                        surfaceView.apply {
                            visibility = View.VISIBLE
                            setChildSurfacePackage(openedSession.surfacePackage)
                            setCompositionOrder(
                                this@RemoteLocationButtonDelegateApi37.getCompositionOrder()
                            )
                            invalidate()
                        }
                        view.localButtonView.visibility = View.INVISIBLE
                    } finally {
                        Trace.endAsyncSection("LocationButton.openSession", 0)
                    }
                }
            }

        provider?.openSession(
            activity,
            hostToken,
            displayId,
            request,
            view.handler::post, // Force all client callbacks to the UI thread
            clientCallback,
        )
    }

    override fun setCompositionOrder(order: Int) {
        compositionOrder = order
        view.surfaceView?.setCompositionOrder(order)
    }

    override fun getCompositionOrder(): Int {
        return compositionOrder
    }
}
