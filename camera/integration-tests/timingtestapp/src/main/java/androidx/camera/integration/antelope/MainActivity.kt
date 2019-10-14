/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.integration.antelope.cameracontrollers.camera2Abort
import androidx.camera.integration.antelope.cameracontrollers.cameraXAbort
import androidx.camera.integration.antelope.cameracontrollers.closeAllCameras
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.android.synthetic.main.activity_main.button_abort
import kotlinx.android.synthetic.main.activity_main.button_multi
import kotlinx.android.synthetic.main.activity_main.button_single
import kotlinx.android.synthetic.main.activity_main.progress_test
import kotlinx.android.synthetic.main.activity_main.scroll_log
import kotlinx.android.synthetic.main.activity_main.surface_preview
import kotlinx.android.synthetic.main.activity_main.text_log
import kotlinx.android.synthetic.main.activity_main.texture_preview

private const val REQUEST_CAMERA_PERMISSION = 1
private const val REQUEST_FILE_WRITE_PERMISSION = 2

/**
 * Main Antelope Activity
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Directory to save image files under sdcard/DCIM */
        const val PHOTOS_DIR: String = "Antelope"
        /** Directory to save .csv log files to under sdcard/Documents */
        const val LOG_DIR: String = "Antelope"
        /** Tag to include when using the logd function */
        val LOG_TAG = "Antelope"

        /** Define "normal" focal length as 50.0mm */
        const val NORMAL_FOCAL_LENGTH: Float = 50f
        /** No aperture reference */
        const val NO_APERTURE: Float = 0f
        /** Fixed-focus lenses have a value of 0 */
        const val FIXED_FOCUS_DISTANCE: Float = 0f
        /** Constant for invalid focal length */
        val INVALID_FOCAL_LENGTH: Float = Float.MAX_VALUE
        /** For single tests, percentage completion to show in progress bar when test is running  */
        const val PROGRESS_SINGLE_PERCENTAGE = 25

        /** List of test results for current test run */
        internal val testRun: ArrayList<TestResults> = ArrayList<TestResults>()
        /** List of test configurations for a multiple test run */
        internal val autoTestConfigs: ArrayList<TestConfig> = ArrayList()

        /** Flag if a single test is running */
        var isSingleTestRunning = false
        /** Number of test remaining in a multiple test run */
        var testsRemaining = 0

        /** View model that contains state data for the application */
        lateinit var camViewModel: CamViewModel

        /** Hashmap of CameraParams for all cameras on the device */
        lateinit var cameraParams: HashMap<String, CameraParams>
        /** Convenience access to device information, OS build, etc. */
        lateinit var deviceInfo: DeviceInfo

        /** Array of human-readable information for each camera on this device */
        val cameras: ArrayList<String> = ArrayList<String>()
        /** Array of camera ids for this device */
        val cameraIds: ArrayList<String> = ArrayList<String>()

        /** Idling Resource used for Espresso tests */
        public val antelopeIdlingResource = CountingIdlingResource("AntelopeIdlingResource")

        /** Convenience wrapper for Log.d that can be toggled on/off */
        fun logd(message: String) {
            if (camViewModel.getShouldOutputLog().value ?: false)
                Log.d(LOG_TAG, message)
        }
    }

    /**
     * Check camera permissions and set up UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        camViewModel = ViewModelProvider(this)
            .get(CamViewModel::class.java)
        cameraParams = camViewModel.getCameraParams()
        deviceInfo = DeviceInfo()

        if (checkCameraPermissions()) {
            initializeCameras(this)
            setupCameraNames()
        }

        button_single.setOnClickListener {
            val testDiag = SettingsDialog.newInstance(SettingsDialog.DIALOG_TYPE_SINGLE,
                getString(R.string.settings_single_test_dialog_title),
                cameras.toTypedArray(), cameraIds.toTypedArray())
            testDiag.show(supportFragmentManager, SettingsDialog.DIALOG_TYPE_SINGLE)
        }

        button_multi.setOnClickListener {
            val testDiag = SettingsDialog.newInstance(SettingsDialog.DIALOG_TYPE_MULTI,
                getString(R.string.settings_multi_test_dialog_title),
                cameras.toTypedArray(), cameraIds.toTypedArray())
            testDiag.show(supportFragmentManager, SettingsDialog.DIALOG_TYPE_MULTI)
        }

        button_abort.setOnClickListener {
            abortTests()
        }

        // Human readable report
        val humanReadableReportObserver = object : Observer<String> {
            override fun onChanged(newReport: String?) {
                text_log.text = newReport ?: ""
            }
        }
        camViewModel.getHumanReadableReport().observe(this, humanReadableReportObserver)
    }

    /**
     * Set up options menu to allow debug logging and clearing cache'd data
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        if (camViewModel.getShouldOutputLog().value != null)
            menu.getItem(0).isChecked = camViewModel.getShouldOutputLog().value!!
        return true
    }

    /**
     * Handle menu presses
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logcat -> {
                item.isChecked = !item.isChecked
                camViewModel.getShouldOutputLog().value = item.isChecked
                true
            }
            R.id.menu_delete_photos -> {
                deleteTestPhotos(this)
                true
            }
            R.id.menu_delete_logs -> {
                deleteCSVFiles(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Update the main scrollview text
     *
     * @param log The new text
     * @param append Whether to append the new text or to replace the old
     * @param copyToClipboard Whether or not to copy the text to the system clipboard
     */
    fun updateLog(log: String, append: Boolean = false, copyToClipboard: Boolean = true) {
        runOnUiThread {
            if (append)
                camViewModel.getHumanReadableReport().value =
                    camViewModel.getHumanReadableReport().value + log
            else
                camViewModel.getHumanReadableReport().value = log
        }

        if (copyToClipboard) {
            runOnUiThread {
                // Copy to clipboard
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val clip = ClipData.newPlainText("Log", log)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.log_copied),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Create human readable names for the camera devices
     */
    private fun setupCameraNames() {
        cameras.clear()
        cameraIds.clear()
        for (param in cameraParams) {
            var camera = ""

            camera += param.value.id
            cameraIds += param.value.id

            if (param.value.isFront)
                camera += " (Front)"
            else if (param.value.isExternal)
                camera += " (External)"
            else
                camera += " (Back)"

            camera += " " + param.value.megapixels + "MP"

            if (!param.value.hasAF)
                camera += " fixed-focus"

            camera += " (min FL: " + param.value.smallestFocalLength + "mm)"
            cameras.add(camera)
        }
    }

    /**
     * Act on the result of a permissions request. If permission granted simply restart the activity
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We now have permission, restart the app
                    val intent = this.intent
                    finish()
                    startActivity(intent)
                } else {
                }
                return
            }
            REQUEST_FILE_WRITE_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We now have permission, restart the app
                    val intent = this.intent
                    finish()
                    startActivity(intent)
                } else {
                }
                return
            }
        }
    }

    /**
     * Check if we have been granted the need camera and file-system permissions
     */
    fun checkCameraPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
            return false
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_FILE_WRITE_PERMISSION)
            return false
        }

        return true
    }

    /** Start the background threads associated with the given camera device/params */
    fun startBackgroundThread(params: CameraParams) {
        if (params.backgroundThread == null) {
            params.backgroundThread = HandlerThread(LOG_TAG).apply {
                this.start()
                params.backgroundHandler = Handler(this.looper)
            }
        }
    }

    /** Stop the background threads associated with the given camera device/params */
    fun stopBackgroundThread(params: CameraParams) {
        params.backgroundThread?.quitSafely()
        try {
            params.backgroundThread?.join()
            params.backgroundThread = null
            params.backgroundHandler = null
        } catch (e: InterruptedException) {
            logd("Interrupted while shutting background thread down: " + e)
        }
    }

    /** Resume all background threads associated with any given camera devices/params */
    override fun onResume() {
        super.onResume()
        for (tempCameraParams in cameraParams) {
            startBackgroundThread(tempCameraParams.value)
        }
    }

    /** Pause all background threads associated with any camera devices/params */
    override fun onPause() {
        for (tempCameraParams in cameraParams) {
            stopBackgroundThread(tempCameraParams.value)
        }
        super.onPause()
    }

    /** Show/hide the progress bar during a test */
    fun showProgressBar(visible: Boolean = true, percentage: Int = PROGRESS_SINGLE_PERCENTAGE) {
        runOnUiThread {
            if (visible) {
                progress_test.progress = percentage
                progress_test.visibility = View.VISIBLE
            } else {
                progress_test.progress = 0
                progress_test.visibility = View.INVISIBLE
            }
        }
    }

    /** Enable/disable controls during a test run */
    fun toggleControls(enabled: Boolean = true) {
        runOnUiThread {
            button_multi.isEnabled = enabled
            button_single.isEnabled = enabled
            button_single.isEnabled = enabled
            button_abort.isEnabled = !enabled // note: inverse of others
        }
    }

    /** Lock orientation during a test so the camera doesn't get re-initialized mid-capture */
    fun toggleRotationLock(lockRotation: Boolean = true) {
        if (lockRotation) {
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    /** Launch a single test based on the current configuration */
    fun startSingleTest() {
        testRun.clear()
        testsRemaining = 1
        isSingleTestRunning = true

        val config = createSingleTestConfig(this)
        setupUIForTest(config, false)

        // Tell Espresso to wait until test run is complete
        logd("Incrementing AntelopeIdlingResource")
        antelopeIdlingResource.increment()

        initializeTest(this, cameraParams.get(config.camera), config)
    }

    /** Launch a series of tests based on the current configuration */
    fun startMultiTest() {
        isSingleTestRunning = false
        setupAutoTestRunner(this)

        // Tell Espresso to wait until test run is complete
        logd("Incrementing AntelopeIdlingResource")
        antelopeIdlingResource.increment()

        autoTestRunner(this)
    }

    /**
     * User has requested to abort the test run. Close cameras and reset the UI.
     */
    fun abortTests() {
        val currentConfig: TestConfig = createTestConfig("ABORT")

        val currentCamera = camViewModel.getCurrentCamera().value ?: 0
        val currentParams = cameraParams.get(currentCamera.toString())

        when (currentConfig.api) {
            CameraAPI.CAMERA1 -> closeAllCameras(this, currentConfig)
            CameraAPI.CAMERAX -> {
                if (null != currentParams)
                    cameraXAbort(this, currentParams, currentConfig)
            }
            CameraAPI.CAMERA2 -> {
                if (null != currentParams)
                    camera2Abort(this, currentParams)
            }
        }

        testsRemaining = 0
        multiCounter = 0

        runOnUiThread {
            toggleControls(true)
            toggleRotationLock(false)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            progress_test.progress = 0
            showProgressBar(false)
            updateLog("\nABORTED", true)
        }

        // Indicate to Espresso that a test run has ended
        try {
            logd("Decrementing AntelopeIdlingResource")
            antelopeIdlingResource.decrement()
        } catch (ex: IllegalStateException) {
            logd("Antelope idling resource decremented below 0. This should never happen.")
        }
    }

    /** After tests are completed, reset the UI to the initial state */
    fun resetUIAfterTest() {
        runOnUiThread {
            toggleControls(true)
            toggleRotationLock(false)
            showProgressBar(false)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Prepare the main UI for a test run. This includes showing/hiding the appropriate preview
     * surface depending on if the test is Camera 1/2/X
     */
    internal fun setupUIForTest(testConfig: TestConfig, append: Boolean = true) {
        with(testConfig) {
            MainActivity.camViewModel.getCurrentAPI().postValue(this.api)
            MainActivity.camViewModel.getCurrentImageCaptureSize().postValue(imageCaptureSize)
            MainActivity.camViewModel.getCurrentCamera().postValue(camera.toInt())

            if (FocusMode.FIXED == focusMode)
                MainActivity.camViewModel.getCurrentFocusMode().postValue(FocusMode.AUTO)
            else
                MainActivity.camViewModel.getCurrentFocusMode().postValue(focusMode)

            if (CameraAPI.CAMERAX == api) {
                surface_preview.visibility = View.INVISIBLE
                texture_preview.visibility = View.VISIBLE
            } else {
                surface_preview.visibility = View.VISIBLE
                texture_preview.visibility = View.INVISIBLE
            }

            toggleControls(false)
            toggleRotationLock(true)
            updateLog("Running: " + testName + "\n", append, false)
            scroll_log.fullScroll(View.FOCUS_DOWN)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
