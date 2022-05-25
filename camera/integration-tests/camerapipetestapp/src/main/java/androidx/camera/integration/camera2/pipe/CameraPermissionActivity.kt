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

package androidx.camera.integration.camera2.pipe

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Trace
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Suppress(
  "DEPRECATION", // Editor does not detect deprecated API calls that are behind an SDK check
  "ObsoleteSdkInt" // Editor does not realize the VERSION.SDK_INT can change at runtime.
)
fun configureFullScreenCameraWindow(activity: Activity) {
  Trace.beginSection("CXCP-App#windowFlags")

  val window = activity.window
  // Make the navigation bar semi-transparent.
  window.setFlags(
    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
  )

  // Hide navigation to make the app full screen
  // TODO: Alter this to use window insets class when running on Android R
  val uiOptions = (
      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          or View.SYSTEM_UI_FLAG_FULLSCREEN
          or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          or View.SYSTEM_UI_FLAG_LOW_PROFILE
          or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      )
  window.decorView.systemUiVisibility = uiOptions

  // Make portrait / landscape rotation seamless
  val windowParams: WindowManager.LayoutParams = window.attributes
  windowParams.rotationAnimation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
  } else {
    WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
  }

  // Allow the app to draw over screen cutouts (notches, camera bumps, etc)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    windowParams.layoutInDisplayCutoutMode =
      WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
  }
  window.attributes = windowParams

  Trace.endSection()
}

/**
 * Utility class for handling simplified permission request/responses for the CameraPipe Sample Apps.
 */
open class CameraPermissionActivity : Activity() {
  private var isStarted = true
  private var lastPermissionRequestNumber = 0
  private var permissionsGrantedCallback: (() -> Unit)? = null

  override fun onRestart() {
    Log.i("CXCP-App", "Received onRestart")
    super.onRestart()
    isStarted = true
  }

  override fun onStart() {
    Log.i("CXCP-App", "Received onStart")
    super.onStart()
    isStarted = true
  }

  override fun onResume() {
    Log.i("CXCP-App", "Received onResume")
    super.onResume()
  }

  override fun onPause() {
    Log.i("CXCP-App", "Received onPause")
    super.onPause()
  }

  override fun onStop() {
    Log.i("CXCP-App", "Received onStop")
    super.onStop()
    isStarted = false
  }

  override fun onDestroy() {
    Log.i("CXCP-App", "Received onDestroy")
    super.onDestroy()
  }

  /**
   * Handles responses to permission requests. Since this is asynchronous, there are several funny
   * things that could happen between requestPermissions and onRequestPermissionResult:
   * - The app could be stopped.
   * - The permissions may be accepted, denied, canceled, or just ignored until next time.
   * - Some permissions may be granted, while others are denied.
   */
  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray
  ) {
    // Standard edge case: If permissions dialog is canceled (eg user hits back button)
    if (grantResults.isEmpty()) {
      Log.w(
        "CXCP-App",
        "Permission request was canceled, there were no grantResults"
      )
      return
    }

    val callback = permissionsGrantedCallback
    // Smoke check, permission runnable should not be null, but if it is, there's no point in
    // iterating the permissions.
    if (callback == null) {
      Log.w(
        "CXCP-App",
        "Got permissions results for $requestCode but no callback was configured."
      )
      return
    }

    // If there are multiple permission requests, only honor the most recent one.
    if (requestCode != lastPermissionRequestNumber) {
      Log.w(
        "CXCP-App",
        "Got permissions results for " +
          requestCode +
          " but it does not match the last requestCode: " +
          lastPermissionRequestNumber
      )
      return
    }

    // Only fire the callback if the activity is in a "Started" state.
    if (!isStarted) {
      Log.w(
        "CXCP-App",
        "Received permission results, but activity is not started."
      )
      return
    }

    // Validity check: Permissions and results should be the same length.
    if (permissions.size != grantResults.size) {
      Log.w(
        "CXCP-App",
        "Got permissions results for " +
          requestCode +
          " the permissions and grants have different lengths. Permissions: " +
          permissions.contentToString() +
          " Results: " +
          grantResults.contentToString()
      )
      return
    }

    // Check to make sure all requested permissions were granted.
    val success = grantResults.zip(permissions)
      .fold(true) { currentSuccess, (result, permission) ->
        val granted = result == PackageManager.PERMISSION_GRANTED
        if (!granted) {
          Log.w("CXCP-App", "Permission $permission was denied.")
        }
        currentSuccess && granted
      }

    // Invoke the callback if successful.
      if (success) {
        Log.i("CXCP-App", "All permissions granted, invoking callback.")
        callback()
      }
  }

  /**
   * Check to see if the required permissions have been granted, and invoke the result on success.
   */
  fun checkPermissionsAndRun(
      permissions: Set<String>,
      permissionsGrantedListener: () -> Unit
  ) {
    val granted = permissions.filter {
      ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    // Invoke and exit early if we already have permissions.
    if (permissions.size == granted.size) {
      permissionsGrantedListener()
      return
    }

    // Always request missing permissions.
    val permissionsToRequest = permissions.filter { !granted.contains(it) }

    lastPermissionRequestNumber += 1
    permissionsGrantedCallback = permissionsGrantedListener
    ActivityCompat.requestPermissions(
      this,
      permissionsToRequest.toTypedArray(),
      lastPermissionRequestNumber
    )
  }
}