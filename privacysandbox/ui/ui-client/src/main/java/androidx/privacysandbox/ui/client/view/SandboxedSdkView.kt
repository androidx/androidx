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
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.lang.IllegalStateException
import java.util.function.Consumer

// TODO(b/268014171): Remove API requirements once S- support is added
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
    private var isTransitionGroupSet = false

    fun setAdapter(sandboxedUiAdapter: SandboxedUiAdapter) {
        if (this.adapter === sandboxedUiAdapter) return
        client?.close()
        client = null
        this.adapter = sandboxedUiAdapter
        checkClientOpenSession()
    }

    fun setSdkErrorConsumer(errorConsumer: Consumer<Throwable>?) {
        this.errorConsumer = errorConsumer
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

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            if (sandboxedSdkView == null) {
                session.close()
                return
            }
            sandboxedSdkView?.setContentView(session.view)
        }

        override fun onSessionError(throwable: Throwable) {
            if (sandboxedSdkView == null) return
            sandboxedSdkView?.removeContentView()
            // TODO(b/266728841): Add listener that reports the state of SandboxedSdkView
            sandboxedSdkView?.errorConsumer?.accept(throwable) ?: throw throwable
            sandboxedSdkView = null
        }

        fun close() {
            session?.close()
            session = null
            sandboxedSdkView?.removeContentView()
            sandboxedSdkView = null
            // TODO: Add listener that reports the state of SandboxedSdkView
        }
    }
}
