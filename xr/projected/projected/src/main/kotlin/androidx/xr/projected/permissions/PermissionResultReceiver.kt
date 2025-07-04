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

package androidx.xr.projected.permissions

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

/**
 * A Parcelable callback for receiving permission results from another activity.
 *
 * This is used instead of [android.app.Activity#startActivityForResult] because the launched
 * activity is expected to be on a different display from the launching activity.
 */
internal class PermissionResultReceiver(handler: Handler) : ResultReceiver(handler) {
    fun interface PermissionResultCallback {
        fun onPermissionResult(permissionResults: Bundle)
    }

    internal var localCallback: PermissionResultCallback? = null

    protected override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        localCallback?.onPermissionResult(resultData)
    }
}
