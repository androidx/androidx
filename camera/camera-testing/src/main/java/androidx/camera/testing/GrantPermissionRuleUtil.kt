/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.testing

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

/**
 * Util functions for grant permission
 */
object GrantPermissionRuleUtil {
    /**
     * Returns a GrantPermissionRule after filtering the available permissions according to API
     * level.
     */
    @JvmStatic
    fun grantWithApiLevelFilter(vararg permissions: String): GrantPermissionRule =
        GrantPermissionRule.grant(*(permissions.filter {
            Build.VERSION.SDK_INT <= 32 || it != Manifest.permission.WRITE_EXTERNAL_STORAGE
        }).toTypedArray())
}