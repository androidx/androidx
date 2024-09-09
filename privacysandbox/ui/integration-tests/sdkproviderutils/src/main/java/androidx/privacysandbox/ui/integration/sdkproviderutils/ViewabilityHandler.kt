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

package androidx.privacysandbox.ui.integration.sdkproviderutils

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory

class ViewabilityHandler {
    companion object {

        private const val TAG = "ViewabilityHandler"

        fun addObserverFactoryToAdapter(adapter: SandboxedUiAdapter, drawViewability: Boolean) {
            adapter.addObserverFactory(SessionObserverFactoryImpl(drawViewability))
        }

        private class SessionObserverFactoryImpl(val drawViewability: Boolean) :
            SessionObserverFactory {

            override fun create(): SessionObserver {
                return SessionObserverImpl()
            }

            private inner class SessionObserverImpl : SessionObserver {
                lateinit var view: View
                lateinit var sandboxedSdkViewUiInfo: SandboxedSdkViewUiInfo

                override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
                    Log.i(TAG, "onSessionOpened $sessionObserverContext")
                    view = checkNotNull(sessionObserverContext.view)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && drawViewability) {
                        view.setOnScrollChangeListener {
                            _: View,
                            scrollX: Int,
                            scrollY: Int,
                            _: Int,
                            _: Int ->
                            if (::sandboxedSdkViewUiInfo.isInitialized) {
                                val rect =
                                    Rect(
                                        sandboxedSdkViewUiInfo.onScreenGeometry.left + scrollX,
                                        sandboxedSdkViewUiInfo.onScreenGeometry.top + scrollY,
                                        sandboxedSdkViewUiInfo.onScreenGeometry.right + scrollX,
                                        sandboxedSdkViewUiInfo.onScreenGeometry.bottom + scrollY
                                    )
                                drawRedRectangle(rect)
                            }
                        }
                    }
                }

                override fun onUiContainerChanged(uiContainerInfo: Bundle) {
                    sandboxedSdkViewUiInfo = SandboxedSdkViewUiInfo.fromBundle(uiContainerInfo)
                    if (drawViewability) {
                        // draw a red rectangle over the received onScreenGeometry of the view
                        drawRedRectangle(sandboxedSdkViewUiInfo.onScreenGeometry)
                    }
                    Log.i(TAG, "onUiContainerChanged $sandboxedSdkViewUiInfo")
                }

                override fun onSessionClosed() {
                    Log.i(TAG, "session closed")
                }

                private fun drawRedRectangle(bounds: Rect) {
                    view.overlay.clear()
                    val viewabilityRect =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            setStroke(10, Color.RED)
                        }
                    viewabilityRect.bounds = bounds
                    view.overlay.add(viewabilityRect)
                }
            }
        }
    }
}
