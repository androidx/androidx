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
import android.os.LocaleList
import android.view.Display
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
    private var activeClient: LocationButtonClient? = null
    private var sessionDisplayId = Display.INVALID_DISPLAY
    private var compositionOrder = LocationButton.DEFAULT_COMPOSITION_ORDER

    init {
        provider = LocationButtonProviderFactory.create(context)
    }

    fun setProvider(provider: LocationButtonProvider?) {
        this.provider = provider
    }

    override fun isSessionActive(): Boolean = session != null

    override fun getSessionDisplayId(): Int = sessionDisplayId

    override fun closeSession(surfaceView: SurfaceView?) {
        sessionDisplayId = Display.INVALID_DISPLAY
        releaseSession(surfaceView)
    }

    /**
     * Releases the session without forgetting its display, so that a move to another display still
     * opens a new session.
     */
    private fun releaseSession(surfaceView: SurfaceView?) {
        activeClient = null
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
        val builder =
            LocationButtonRequest.Builder(view.width, view.height, view.effectiveConfiguration)
                .setPaddingLeft(view.safePaddingLeft)
                .setPaddingTop(view.safePaddingTop)
                .setPaddingRight(view.safePaddingRight)
                .setPaddingBottom(view.safePaddingBottom)
                .setBackgroundColor(view.backgroundColor)
                .setStrokeColor(view.strokeColor)
                .setStrokeWidth(view.strokeWidth)
                .setIconTint(view.iconTint)
                .setTextType(view.textType)
                .setTextColor(view.textColor)
        if (view.cornerRadius >= 0f) {
            builder.setCornerRadius(view.cornerRadius)
        }
        if (view.pressedCornerRadius >= 0f) {
            builder.setPressedCornerRadius(view.pressedCornerRadius)
        }
        return builder.build()
    }

    override fun openSession(activity: Activity, displayId: Int, surfaceView: SurfaceView) {
        @Suppress("DEPRECATION") val hostToken = surfaceView.hostToken ?: return
        val activeProvider = provider ?: return

        val request = createButtonRequest()
        // SystemUI only uses the locales from the configuration.
        val requestedLocales = request.configuration.locales
        // The request can't tell an unset corner radius (-1) from 0, so the view's values are kept.
        val requestedCornerRadius = view.cornerRadius
        val requestedPressedCornerRadius = view.pressedCornerRadius

        val clientCallback =
            object : LocationButtonClient {
                override fun onPermissionResult(granted: Boolean) {
                    view.onPermissionResultListener?.onPermissionResult(granted)
                }

                override fun onSessionError(t: Throwable) {
                    if (activeClient !== this) {
                        return
                    }
                    releaseSession(surfaceView)
                    view.onErrorListener?.onError(t)
                }

                override fun onSessionOpened(openedSession: LocationButtonSession) {
                    if (activeClient !== this || !view.isAttachedToWindow) {
                        openedSession.close()
                        return
                    }

                    session = openedSession
                    // Before the surface is shown, so that it doesn't show the old state first.
                    applyChangesSinceRequest(
                        openedSession,
                        request,
                        requestedLocales,
                        requestedCornerRadius,
                        requestedPressedCornerRadius,
                    )
                    surfaceView.apply {
                        visibility = View.VISIBLE
                        setChildSurfacePackage(openedSession.surfacePackage)
                        setCompositionOrder(
                            this@RemoteLocationButtonDelegateApi37.getCompositionOrder()
                        )
                        invalidate()
                    }
                    view.localButtonView.visibility = View.INVISIBLE
                }
            }

        activeClient = clientCallback
        sessionDisplayId = displayId
        activeProvider.openSession(
            activity,
            hostToken,
            displayId,
            request,
            view.handler::post, // Force all client callbacks to the UI thread
            clientCallback,
        )
    }

    /**
     * Sends the changes made to the button while its session was opening, since the setters only
     * reach a session that's already open.
     */
    private fun applyChangesSinceRequest(
        session: LocationButtonSession,
        request: LocationButtonRequest,
        requestedLocales: LocaleList,
        requestedCornerRadius: Float,
        requestedPressedCornerRadius: Float,
    ) {
        val current = createButtonRequest()
        if (current.configuration.locales != requestedLocales) {
            session.changeConfiguration(current.configuration)
        }
        // A view that's gone or not laid out has a size of 0, which onSizeChanged doesn't send.
        if (
            current.width > 0 &&
                current.height > 0 &&
                (current.width != request.width || current.height != request.height)
        ) {
            session.resize(current.width, current.height)
        }
        if (
            current.paddingLeft != request.paddingLeft ||
                current.paddingTop != request.paddingTop ||
                current.paddingRight != request.paddingRight ||
                current.paddingBottom != request.paddingBottom
        ) {
            session.setPadding(
                current.paddingLeft,
                current.paddingTop,
                current.paddingRight,
                current.paddingBottom,
            )
        }
        if (current.backgroundColor != request.backgroundColor) {
            session.setBackgroundColor(current.backgroundColor)
        }
        if (current.strokeColor != request.strokeColor) {
            session.setStrokeColor(current.strokeColor)
        }
        if (current.strokeWidth != request.strokeWidth) {
            session.setStrokeWidth(current.strokeWidth)
        }
        if (current.iconTint != request.iconTint) {
            session.setIconTint(current.iconTint)
        }
        if (current.textType != request.textType) {
            session.setTextType(current.textType)
        }
        if (current.textColor != request.textColor) {
            session.setTextColor(current.textColor)
        }
        // Compared with the view's values, since the request reads an unset radius back as 0.
        if (view.cornerRadius >= 0f && view.cornerRadius != requestedCornerRadius) {
            session.setCornerRadius(view.cornerRadius)
        }
        if (
            view.pressedCornerRadius >= 0f &&
                view.pressedCornerRadius != requestedPressedCornerRadius
        ) {
            session.setPressedCornerRadius(view.pressedCornerRadius)
        }
    }

    override fun setCompositionOrder(order: Int) {
        compositionOrder = order
        view.surfaceView?.setCompositionOrder(order)
    }

    override fun getCompositionOrder(): Int {
        return compositionOrder
    }
}
