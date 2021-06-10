/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.testutils.runOnUiThreadRethrow
import java.io.FileDescriptor
import java.io.PrintWriter

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.startupFragmentController(
    viewModelStore: ViewModelStore,
    savedState: Parcelable? = null
): FragmentController {
    lateinit var fc: FragmentController
    runOnUiThreadRethrow {
        fc = FragmentController.createController(
            ControllerHostCallbacks(activity, viewModelStore)
        )
        fc.attachHost(null)
        fc.restoreSaveState(savedState)
        fc.dispatchCreate()
        fc.dispatchActivityCreated()
        fc.noteStateNotSaved()
        fc.execPendingActions()
        fc.dispatchStart()
        fc.dispatchResume()
        fc.execPendingActions()
    }
    return fc
}

fun FragmentController.restart(
    @Suppress("DEPRECATION")
    rule: androidx.test.rule.ActivityTestRule<out FragmentActivity>,
    viewModelStore: ViewModelStore,
    destroyNonConfig: Boolean = true
): FragmentController {
    var savedState: Parcelable? = null
    rule.runOnUiThreadRethrow {
        savedState = shutdown(viewModelStore, destroyNonConfig)
    }
    return rule.startupFragmentController(viewModelStore, savedState)
}

fun FragmentController.shutdown(
    viewModelStore: ViewModelStore,
    destroyNonConfig: Boolean = true
): Parcelable? {
    dispatchPause()
    @Suppress("DEPRECATION")
    val savedState = saveAllState()
    dispatchStop()
    if (destroyNonConfig) {
        viewModelStore.clear()
    }
    dispatchDestroy()
    return savedState
}

class ControllerHostCallbacks(
    private val activity: FragmentActivity,
    private val viewModelStore: ViewModelStore
) : FragmentHostCallback<FragmentActivity>(activity), ViewModelStoreOwner {

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    override fun onDump(
        prefix: String,
        fd: FileDescriptor?,
        writer: PrintWriter,
        args: Array<String>?
    ) {
    }

    override fun onShouldSaveFragmentState(fragment: Fragment): Boolean {
        return !activity.isFinishing
    }

    override fun onGetLayoutInflater(): LayoutInflater {
        return activity.layoutInflater.cloneInContext(activity)
    }

    override fun onGetHost(): FragmentActivity? {
        return activity
    }

    override fun onSupportInvalidateOptionsMenu() {
        activity.invalidateOptionsMenu()
    }

    override fun onStartActivityFromFragment(
        fragment: Fragment,
        intent: Intent,
        requestCode: Int
    ) {
        activity.startActivityFromFragment(fragment, intent, requestCode)
    }

    override fun onStartActivityFromFragment(
        fragment: Fragment,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        activity.startActivityFromFragment(fragment, intent, requestCode, options)
    }

    override fun onRequestPermissionsFromFragment(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: Int
    ) {
        throw UnsupportedOperationException()
    }

    override fun onShouldShowRequestPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, permission
        )
    }

    override fun onHasWindowAnimations() = activity.window != null

    override fun onGetWindowAnimations() =
        activity.window?.attributes?.windowAnimations ?: 0

    override fun onFindViewById(id: Int): View? {
        return activity.findViewById(id)
    }

    override fun onHasView(): Boolean {
        val w = activity.window
        return w?.peekDecorView() != null
    }
}