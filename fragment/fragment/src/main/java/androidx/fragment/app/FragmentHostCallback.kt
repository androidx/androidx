/*
 * Copyright 2018 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RestrictTo
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * Integration points with the Fragment host.
 *
 * Fragments may be hosted by any object; such as an [Activity]. In order to
 * host fragments, implement [FragmentHostCallback], overriding the methods
 * applicable to the host.
 *
 * FragmentManager changes its behavior based on what optional interfaces your
 * FragmentHostCallback implements. This includes the following:
 *
 * - **[androidx.activity.result.ActivityResultRegistryOwner]**: Removes the need to
 * override [.onStartIntentSenderFromFragment] or
 * [.onRequestPermissionsFromFragment].
 * - **[FragmentOnAttachListener]**: Removes the need to
 * manually call [FragmentManager.addFragmentOnAttachListener] from your
 * host in order to receive [FragmentOnAttachListener.onAttachFragment] callbacks
 * for the [FragmentController.getSupportFragmentManager].
 * - **[androidx.activity.OnBackPressedDispatcherOwner]**: Removes
 * the need to manually call
 * [FragmentManager.popBackStackImmediate] when handling the system
 * back button.
 * - **[androidx.lifecycle.ViewModelStoreOwner]**: Removes the need
 * for your [FragmentController] to call
 * [FragmentController.retainNestedNonConfig] or
 * [FragmentController.restoreAllState].
 *
 * @param H the type of object that's currently hosting the fragments. An instance of this
 * class must be returned by [onGetHost].
 */
@Suppress("deprecation")
abstract class FragmentHostCallback<H> internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val activity: Activity?,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val context: Context,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val handler: Handler,
    private val windowAnimations: Int
) : FragmentContainer() {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val fragmentManager: FragmentManager = FragmentManagerImpl()

    @Suppress("unused")
    constructor(
        context: Context,
        handler: Handler,
        windowAnimations: Int
    ) : this(
        if (context is Activity) context else null,
        context,
        handler,
        windowAnimations
    )

    @Suppress("deprecation")
    internal constructor(activity: FragmentActivity) : this(
        activity,
        context = activity,
        Handler(),
        windowAnimations = 0
    )

    /**
     * Print internal state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state. This will be closed
     * for you after you return.
     * @param args additional arguments to the dump request.
     */
    open fun onDump(
        prefix: String,
        fd: FileDescriptor?,
        writer: PrintWriter,
        args: Array<String>?
    ) {
    }

    /**
     * Return `true` if the fragment's state needs to be saved.
     */
    open fun onShouldSaveFragmentState(fragment: Fragment): Boolean {
        return true
    }

    /**
     * Return a [LayoutInflater].
     * See [Activity.getLayoutInflater].
     */
    open fun onGetLayoutInflater(): LayoutInflater {
        return LayoutInflater.from(context)
    }

    /**
     * Return the object that's currently hosting the fragment. If a [Fragment]
     * is hosted by a [FragmentActivity], the object returned here should be
     * the same object returned from [Fragment.getActivity].
     */
    abstract fun onGetHost(): H

    /**
     * Invalidates the activity's options menu.
     * See [FragmentActivity.supportInvalidateOptionsMenu]
     */
    open fun onSupportInvalidateOptionsMenu() {}

    /**
     * Starts a new [Activity] from the given fragment.
     * See [FragmentActivity.startActivityForResult].
     */
    open fun onStartActivityFromFragment(
        fragment: Fragment,
        intent: Intent,
        requestCode: Int
    ) {
        onStartActivityFromFragment(fragment, intent, requestCode, null)
    }

    /**
     * Starts a new [Activity] from the given fragment.
     * See [FragmentActivity.startActivityForResult].
     */
    open fun onStartActivityFromFragment(
        fragment: Fragment,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        check(requestCode == -1) {
            "Starting activity with a requestCode requires a FragmentActivity host"
        }
        ContextCompat.startActivity(context, intent, options)
    }

    /**
     * Starts a new [IntentSender] from the given fragment.
     * See [Activity.startIntentSender].
     */
    @Deprecated(
        """Have your FragmentHostCallback implement {@link ActivityResultRegistryOwner}
      to allow Fragments to use
      {@link Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      with {@link StartIntentSenderForResult}. This method will still be called when Fragments
      call the deprecated <code>startIntentSenderForResult()</code> method."""
    )
    @Throws(SendIntentException::class)
    open fun onStartIntentSenderFromFragment(
        fragment: Fragment,
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
        check(requestCode == -1) {
            "Starting intent sender with a requestCode requires a FragmentActivity host"
        }
        val activity = checkNotNull(activity) {
            "Starting intent sender with a requestCode requires a FragmentActivity host"
        }
        ActivityCompat.startIntentSenderForResult(
            activity, intent, requestCode, fillInIntent,
            flagsMask, flagsValues, extraFlags, options
        )
    }

    /**
     * Requests permissions from the given fragment.
     * See [FragmentActivity.requestPermissions]
     */
    @Deprecated(
        """Have your FragmentHostCallback implement {@link ActivityResultRegistryOwner}
      to allow Fragments to use
      {@link Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      with {@link RequestMultiplePermissions}. This method will still be called when Fragments
      call the deprecated <code>requestPermissions()</code> method."""
    )
    open fun onRequestPermissionsFromFragment(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: Int
    ) {
    }

    /**
     * Checks whether to show permission rationale UI from a fragment.
     * See [FragmentActivity.shouldShowRequestPermissionRationale]
     */
    open fun onShouldShowRequestPermissionRationale(permission: String): Boolean {
        return false
    }

    /**
     * Return `true` if there are window animations.
     */
    open fun onHasWindowAnimations(): Boolean {
        return true
    }

    /**
     * Return the window animations.
     */
    open fun onGetWindowAnimations(): Int {
        return windowAnimations
    }

    override fun onFindViewById(id: Int): View? {
        return null
    }

    override fun onHasView(): Boolean {
        return true
    }
}
