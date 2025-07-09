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

package androidx.pdf.service.connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.pdf.PdfSandboxHandle
import androidx.pdf.service.PdfDocumentServiceImpl

/**
 * A [PdfSandboxHandle] implementation that establishes a connection with the
 * [PdfDocumentServiceImpl] bound service.
 *
 * This implementation is intended to reduce cold-start latency by ensuring that service
 * initialization occurs ahead of time, typically during application startup or activity entry
 * points.
 *
 * @param context A [Context] of component to be associated with pdf session.
 */
internal class PdfSandboxHandleImpl(
    private val context: Context,
    private val onConnected: (() -> Unit)? = null,
    private val onDisconnected: (() -> Unit)? = null,
) : PdfSandboxHandle, ServiceConnection {
    // Flag to ensure unbindService is called only when service was bound before.
    private var isServiceBound: Boolean = false

    internal fun connect() {
        val intent = Intent(context, PdfDocumentServiceImpl::class.java)
        context.bindService(intent, /* conn= */ this, /* flags= */ Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        onConnected?.invoke()
        // We do not interact with the binder directly.
        // This binding is only used to keep the service process alive.
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected?.invoke()
    }

    override fun close() {
        if (isServiceBound) {
            context.unbindService(this)
            isServiceBound = false
            onDisconnected?.invoke()
        }
    }
}
