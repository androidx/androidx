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

package androidx.privacysandbox.ui.client.view

import android.annotation.SuppressLint
import android.view.View
import androidx.core.util.Preconditions
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Represents an asset comprising a native ad. Each asset is associated with a [View] and has an
 * assigned [assetId]. For [SandboxedSdkView]s, allows to provide a [SandboxedUiAdapter] for
 * [SharedUiContainer] to manage the opening of a [SandboxedSdkView] session. However, the session
 * will be closed by the container before the shared UI session in any case.
 *
 * No user-sensitive information should be added to the asset registered on [SharedUiContainer] as
 * it will be sent to the UI provider.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
public class SharedUiAsset
@JvmOverloads
constructor(
    public val view: View,
    public val assetId: String,
    public val sandboxedUiAdapter: SandboxedUiAdapter? = null,
) {
    init {
        if (sandboxedUiAdapter != null)
            Preconditions.checkArgument(
                view is SandboxedSdkView,
                "${SandboxedUiAdapter::class.qualifiedName} can only be set for ${SandboxedSdkView::class.qualifiedName} assets",
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharedUiAsset) return false

        if (view !== other.view) return false
        if (assetId != other.assetId) return false
        if (sandboxedUiAdapter !== other.sandboxedUiAdapter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = view.hashCode()
        result = 31 * result + assetId.hashCode()
        result = 31 * result + (sandboxedUiAdapter?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SharedUiAsset(view=$view, assetId='$assetId', sandboxedUiAdapter=$sandboxedUiAdapter)"
    }
}
