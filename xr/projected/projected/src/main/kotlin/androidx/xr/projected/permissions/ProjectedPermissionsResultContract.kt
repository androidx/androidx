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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract

/**
 * An [ActivityResultContract] to request permissions from a Projected [Activity].
 *
 * This contract is intended to be used with
 * [androidx.activity.result.ActivityResultCaller#registerForActivityResult], but can also be used
 * by directly calling [createIntent] and [parseResult].
 *
 * When this [ActivityResultContract] is launched from a Projected activity, a new activity will be
 * launched on the Projected device and another activity will be launched on the host device (e.g.
 * phone). The Projected activity will request the user to go to the host activity to act on the
 * permission request. The host activity will present the user with the rationale for the permission
 * request (if provided) along with buttons to accept or reject. If accepted, the system dialog for
 * permission requests will appear for the user to grant/deny the permission.
 *
 * If a rationale is not provided, the host activity will immediately trigger the system dialog for
 * permission requests.
 *
 * After the user has acted on the permission request, both the Projected activity and host activity
 * will finish. The callback provided to
 * [androidx.activity.result.ActivityResultCaller#registerForActivityResult] will be invoked with a
 * [Map] of permission results. The keys are the permission names and the values are booleans
 * indicating whether the permission was granted.
 *
 * If multiple [ProjectedPermissionsRequestParams] are provided, they will be presented to the user
 * in order. If the user rejects a request, the host activity will automatically advance to the next
 * [ProjectedPermissionsRequestParams] with a rationale, implicitly rejecting all
 * [ProjectedPermissionsRequestParams] in between (if any). This prevents a confusing user
 * experience where the user is immediately prompted for a permission after hitting cancel in the
 * rationale UI.
 */
public class ProjectedPermissionsResultContract :
    ActivityResultContract<List<ProjectedPermissionsRequestParams>, Map<String, Boolean>>() {

    override fun createIntent(
        context: Context,
        input: List<ProjectedPermissionsRequestParams>,
    ): Intent {
        val requestDataBundles =
            input
                .map { requestData ->
                    Bundle().apply {
                        putStringArrayList(
                            BUNDLE_KEY_PERMISSIONS,
                            requestData.permissions.toCollection(ArrayList()),
                        )
                        putString(BUNDLE_KEY_RATIONALE, requestData.rationale)
                    }
                }
                .toTypedArray()

        return Intent()
            .setClass(context, GoToHostProjectedActivity::class.java)
            .putExtra(EXTRA_PERMISSION_REQUEST_DATA, requestDataBundles)
            .putExtra(EXTRA_DEVICE_ID, context.deviceId)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Map<String, Boolean> {
        if (resultCode != Activity.RESULT_OK) return emptyMap()
        if (intent == null) return emptyMap()

        return intent.getBundleExtra(EXTRA_PERMISSION_RESULTS)?.let { bundle ->
            bundle.keySet().associateWith { key -> bundle.getBoolean(key, false) }
        } ?: emptyMap()
    }

    internal companion object {
        internal const val EXTRA_DEVICE_ID = "androidx.xr.projected.permissions.extra.DEVICE_ID"
        internal const val EXTRA_PERMISSION_REQUEST_DATA =
            "androidx.xr.projected.permissions.extra.PERMISSION_REQUEST_DATA"

        internal const val EXTRA_PERMISSION_RESULTS =
            "androidx.xr.projected.permissions.extra.PERMISSION_RESULTS"

        internal const val BUNDLE_KEY_PERMISSIONS =
            "androidx.xr.projected.permissions.BUNDLE_KEY_PERMISSIONS"
        internal const val BUNDLE_KEY_RATIONALE =
            "androidx.xr.projected.permissions.BUNDLE_KEY_RATIONALE"
    }
}
