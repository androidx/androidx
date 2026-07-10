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

package androidx.pdf.util

import android.os.DeadObjectException
import androidx.pdf.view.PdfView

internal object ExceptionUtils {
    /**
     * Determines whether this [Exception] is a known remote-service issue that [PdfView] should
     * handle gracefully (e.g., by displaying an error UI) rather than rethrowing.
     *
     * This includes:
     * - [DeadObjectException]: The remote service process has crashed or been terminated.
     * - Unrecognized IPC calls: Scenarios where the remote binder doesn't recognize an IPC call,
     *   often signaled by an "unimplemented" message.
     *
     * @return `true` if the exception should be captured and handled internally; `false` otherwise.
     */
    val Exception.isHandledRemoteException: Boolean
        get() =
            message?.contains("unimplemented", ignoreCase = true) == true ||
                this is DeadObjectException
}
