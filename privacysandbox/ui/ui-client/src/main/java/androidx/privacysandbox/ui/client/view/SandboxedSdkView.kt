/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.client.view

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.util.function.Consumer
import kotlin.math.min

// TODO(b/268014171): Remove API requirements once S- support is added
// TODO(b/266728841): Add listener that reports the state of SandboxedSdkView
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SandboxedSdkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private var adapter: SandboxedUiAdapter? = null
    private var client: Client? = null
    private var isZOrderOnTop = true
    private var errorConsumer: Consumer<Throwable>? = null
    private var contentView: View? = null
    private var requestedWidth = -1
    private var requestedHeight = -1
    private var isTransitionGroupSet = false

    fun setAdapter(sandboxedUiAdapter: SandboxedUiAdapter) {
        if (this.adapter === sandboxedUiAdapter) return
        client?.close()
        client = null
        this.adapter = sandboxedUiAdapter
        checkClientOpenSession()
    }

    // TODO(b/269590488): Remove Error Consumer once StateChangeListener is added
    fun setSdkErrorConsumer(errorConsumer: Consumer<Throwable>?) {
        this.errorConsumer = errorConsumer
    }

    fun setZOrderOnTopAndEnableUserInteraction(setOnTop: Boolean) {
        if (setOnTop == isZOrderOnTop) return
        this.isZOrderOnTop = setOnTop
        checkClientOpenSession()
        client?.notifyZOrderChanged(setOnTop)
    }

    private fun checkClientOpenSession() {
        val adapter = adapter
        if (client == null && adapter != null && isAttachedToWindow && width > 0 && height > 0) {
            client = Client(this)
            adapter.openSession(
                context,
                width,
                height,
                isZOrderOnTop,
                handler::post,
                client!!
            )
        }
    }

    internal fun requestSize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        requestedWidth = width
        requestedHeight = height
        requestLayout()
    }

    internal fun removeContentView() {
        if (childCount == 1) {
            super.removeViewAt(0)
        }
    }

    internal fun setContentView(contentView: View) {
        if (childCount > 1) {
            throw IllegalStateException("Number of children views must not exceed 1")
        }

        this.contentView = contentView
        removeContentView()

        if (contentView.layoutParams == null) {
            super.addView(contentView, 0, generateDefaultLayoutParams())
        } else {
            super.addView(contentView, 0, contentView.layoutParams)
        }
    }

    private fun calculateMeasuredDimension(requestedSize: Int, measureSpec: Int): Int {
        val measureSpecSize = MeasureSpec.getSize(measureSpec)

        when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> {
                return measureSpecSize
            }

            MeasureSpec.UNSPECIFIED -> {
                return if (requestedSize < 0) {
                    measureSpecSize
                } else {
                    requestedSize
                }
            }

            MeasureSpec.AT_MOST -> {
                return if (requestedSize >= 0) {
                    min(requestedSize, measureSpecSize)
                } else {
                    measureSpecSize
                }
            }

            else -> {
                return measureSpecSize
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newWidth = calculateMeasuredDimension(requestedWidth, widthMeasureSpec)
        val newHeight = calculateMeasuredDimension(requestedHeight, heightMeasureSpec)
        setMeasuredDimension(newWidth, newHeight)
    }

    override fun isTransitionGroup(): Boolean = !isTransitionGroupSet || super.isTransitionGroup()

    override fun setTransitionGroup(isTransitionGroup: Boolean) {
        super.setTransitionGroup(isTransitionGroup)
        isTransitionGroupSet = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        getChildAt(0)?.layout(left, top, right, bottom)
        checkClientOpenSession()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkClientOpenSession()
    }

    override fun onDetachedFromWindow() {
        client?.close()
        client = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        checkClientOpenSession()
        client?.notifyResized(width, height)
    }

    // TODO(b/270971893) Compare to old configuration before notifying of configuration change.
    override fun onConfigurationChanged(config: Configuration?) {
        requireNotNull(config) { "Config cannot be null" }
        super.onConfigurationChanged(config)
        checkClientOpenSession()
        client?.notifyConfigurationChanged(config)
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun addView(
        view: View?,
        index: Int,
        params: LayoutParams?
    ) {
        throw UnsupportedOperationException("Cannot add a view to SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeView(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewInLayout(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewsInLayout(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViews(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeAllViews() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeAllViewsInLayout() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    internal class Client(private var sandboxedSdkView: SandboxedSdkView?) :
        SandboxedUiAdapter.SessionClient {

        private var session: SandboxedUiAdapter.Session? = null
        private var pendingWidth: Int? = null
        private var pendingHeight: Int? = null

        // pendingZOrderOnTop ensures visible and interactive provider UI as long as the UI is
        // unobstructed to the user.
        private var pendingZOrderOnTop: Boolean? = true
        private var pendingConfiguration: Configuration? = null

        fun notifyConfigurationChanged(configuration: Configuration) {
            val session = session
            if (session != null) {
                session.notifyConfigurationChanged(configuration)
            } else {
                pendingConfiguration = configuration
            }
        }

        fun notifyResized(width: Int, height: Int) {
            val session = session
            if (session != null) {
                session.notifyResized(width, height)
            } else {
                pendingWidth = width
                pendingHeight = height
            }
        }

        fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
            if (sandboxedSdkView?.isZOrderOnTop == isZOrderOnTop) return
            val session = session
            if (session != null) {
                session.notifyZOrderChanged(isZOrderOnTop)
            } else {
                pendingZOrderOnTop = isZOrderOnTop
            }
        }

        fun close() {
            sandboxedSdkView?.removeContentView()
            sandboxedSdkView = null
            session?.close()
            session = null
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            if (sandboxedSdkView == null) {
                session.close()
                return
            }
            sandboxedSdkView?.setContentView(session.view)
            this.session = session
            val width = pendingWidth
            val height = pendingHeight
            if ((width != null) && (height != null) && (width >= 0) && (height >= 0)) {
                session.notifyResized(width, height)
            }
            pendingConfiguration?.let {
                session.notifyConfigurationChanged(it)
            }
            pendingConfiguration = null
            pendingZOrderOnTop?.let {
                session.notifyZOrderChanged(it)
            }
        }

        override fun onSessionError(throwable: Throwable) {
            if (sandboxedSdkView == null) return
            sandboxedSdkView?.removeContentView()
            sandboxedSdkView?.errorConsumer?.accept(throwable) ?: throw throwable
            sandboxedSdkView = null
        }

        override fun onResizeRequested(width: Int, height: Int) {
            if (sandboxedSdkView == null) return
            sandboxedSdkView?.requestSize(width, height)
        }
    }
}
