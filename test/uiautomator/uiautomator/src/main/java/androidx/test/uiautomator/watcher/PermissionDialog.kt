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

package androidx.test.uiautomator.watcher

import androidx.test.uiautomator.internal.uiDevice
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.onElementOrNull

/** Allows easy interaction with the permission dialog. */
public object PermissionDialog : ScopedUiWatcher<PermissionDialog.Scope> {

    /** The package name of the permission controller. */
    private const val PACKAGE_NAME: String = "com.google.android.permissioncontroller"

    /** The id of the deny button in the permission controller dialog. */
    private const val VIEW_ID_RESOURCE_NAME_BUTTON_DENY: String =
        "com.android.permissioncontroller:id/permission_deny_button"

    /** The id of the allow button in the permission controller dialog. */
    private const val VIEW_ID_RESOURCE_NAME_BUTTON_ALLOW: String =
        "com.android.permissioncontroller:id/permission_allow_button"

    public override fun isVisible(): Boolean =
        uiDevice.onElementOrNull(0) { packageName == PACKAGE_NAME } != null

    override fun scope(): Scope = Scope

    public object Scope {

        public fun clickAllow(): Unit =
            uiDevice.onElement { viewIdResourceName == VIEW_ID_RESOURCE_NAME_BUTTON_ALLOW }.click()

        public fun clickDeny(): Unit =
            uiDevice.onElement { viewIdResourceName == VIEW_ID_RESOURCE_NAME_BUTTON_DENY }.click()
    }
}
