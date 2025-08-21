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
import androidx.compose.ui.platform.coreshims.ViewStructureCompat

/** A fake implementation of [ContentCaptureSession] for use in tests. */
class FakeContentCaptureSession : ContentCaptureSessionWrapper {
    var lastAutofillId: AutofillId? = null
    var lastViewStructure: ViewStructure? = null
    var lastDisappearedId: AutofillId? = null
    var lastTextChangeId: AutofillId? = null
    var lastTextChange: CharSequence? = null
    var flushCount = 0
    var lastAppearedNodes: List<ViewStructure>? = null
    var lastDisappearedIds: LongArray? = null

    override fun newAutofillId(virtualChildId: Long): AutofillId? {
        return null
    }

    override fun newVirtualViewStructure(
        parentId: AutofillId,
        virtualId: Long,
    ): ViewStructureCompat? {
        return null
    }

    override fun notifyViewAppeared(node: ViewStructure) {
        lastViewStructure = node
    }

    override fun notifyViewDisappeared(id: AutofillId) {
        lastDisappearedId = id
    }

    override fun flush() {
        flushCount++
    }

    override fun notifyViewsAppeared(appearedNodes: List<ViewStructure>) {
        lastAppearedNodes = appearedNodes
    }

    override fun notifyViewsDisappeared(virtualIds: LongArray) {
        lastDisappearedIds = virtualIds
    }

    override fun notifyViewTextChanged(id: AutofillId, text: CharSequence?) {
        lastTextChangeId = id
        lastTextChange = text
    }
}
