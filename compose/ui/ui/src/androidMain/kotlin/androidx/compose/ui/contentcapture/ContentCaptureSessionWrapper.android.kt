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

package androidx.compose.ui.contentcapture

import android.view.ViewStructure
import android.view.autofill.AutofillId
import androidx.annotation.RestrictTo
import androidx.compose.ui.platform.coreshims.ViewStructureCompat

/**
 * Interface for content capture sessions.
 *
 * This interface is a wrapper around the platform's
 * [android.view.contentcapture.ContentCaptureSession] and is used to abstract away the platform's
 * implementation details. This allows for the use of fakes in tests.
 *
 * The methods in this interface are expected to delegate to the underlying platform's
 * [android.view.contentcapture.ContentCaptureSession].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface ContentCaptureSessionWrapper {
    /**
     * Creates a new [AutofillId] for a virtual child.
     *
     * @see android.view.contentcapture.ContentCaptureSession.newAutofillId
     */
    fun newAutofillId(virtualChildId: Long): AutofillId?

    /**
     * Creates a [ViewStructure] for a "virtual" view.
     *
     * @see android.view.contentcapture.ContentCaptureSession.newVirtualViewStructure
     */
    fun newVirtualViewStructure(parentId: AutofillId, virtualId: Long): ViewStructureCompat?

    /**
     * Notifies the Content Capture Service that a node has been added to the view structure.
     *
     * @see android.view.contentcapture.ContentCaptureSession.notifyViewAppeared
     */
    fun notifyViewAppeared(node: ViewStructure)

    /**
     * Flushes an internal buffer of UI events.
     *
     * @see android.view.contentcapture.ContentCaptureSession.flush
     */
    fun flush()

    /**
     * Notifies the Content Capture Service that a list of nodes has appeared in the view structure.
     *
     * This method is only available on API level 34 and above.
     *
     * @see android.view.contentcapture.ContentCaptureSession.notifyViewsAppeared
     */
    fun notifyViewsAppeared(appearedNodes: @JvmSuppressWildcards List<ViewStructure>)

    /**
     * Notifies the Content Capture Service that many nodes has been removed from a virtual view
     * structure.
     *
     * This method is only available on API level 34 and above.
     *
     * @see android.view.contentcapture.ContentCaptureSession.notifyViewsDisappeared
     */
    fun notifyViewsDisappeared(virtualIds: LongArray)

    /**
     * Notifies the Content Capture Service that a node has been removed from the view structure.
     *
     * @see android.view.contentcapture.ContentCaptureSession.notifyViewDisappeared
     */
    fun notifyViewDisappeared(id: AutofillId)

    /**
     * Notifies the Content Capture Service that the value of a text node has been changed.
     *
     * @see android.view.contentcapture.ContentCaptureSession.notifyViewTextChanged
     */
    fun notifyViewTextChanged(id: AutofillId, text: CharSequence?)
}
