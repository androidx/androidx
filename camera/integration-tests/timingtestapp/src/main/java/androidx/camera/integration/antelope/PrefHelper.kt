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

@file:Suppress("DEPRECATION")

package androidx.camera.integration.antelope

import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import java.util.Collections
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.asList
import kotlin.collections.iterator

/**
 * Convenience class to simplify getting values for Shared Preferences
 */
class PrefHelper {
    companion object {
        internal fun getAutoDelete(activity: MainActivity): Boolean {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref.getBoolean(activity.getString(R.string.settings_autodelete_key), true)
        }

        internal fun getNumTests(activity: MainActivity): Int {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return (sharedPref.getString(activity
                .getString(R.string.settings_numtests_key), "30") ?: "30").toInt()
        }

        internal fun getPreviewBuffer(activity: MainActivity): Long {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return (sharedPref.getString(activity.getString(R.string.settings_previewbuffer_key),
                "1500") ?: "1500").toLong()
        }

        internal fun getAPIs(activity: MainActivity): ArrayList<CameraAPI> {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            val defApis: HashSet<String> =
                HashSet(activity.resources.getStringArray(R.array.array_settings_api).asList())

            val apiStrings: HashSet<String> =
                sharedPref.getStringSet(activity.getString(R.string.settings_autotest_api_key),
                    defApis) as HashSet<String>

            val apis: ArrayList<CameraAPI> = ArrayList()

            for (apiString in apiStrings) {
                when (apiString) {
                    "Camera1" -> apis.add(CameraAPI.CAMERA1)
                    "Camera2" -> apis.add(CameraAPI.CAMERA2)
                    "CameraX" -> apis.add(CameraAPI.CAMERAX)
                }
            }

            Collections.sort(apis, ApiComparator())
            return apis
        }

        internal fun getImageSizes(activity: MainActivity): ArrayList<ImageCaptureSize> {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            val defSizes: HashSet<String> = HashSet(activity.resources
                .getStringArray(R.array.array_settings_imagesize).asList())

            val sizeStrings: HashSet<String> =
                sharedPref.getStringSet(activity
                    .getString(R.string.settings_autotest_imagesize_key), defSizes)
                    as HashSet<String>

            val sizes: ArrayList<ImageCaptureSize> = ArrayList()

            for (sizeString in sizeStrings) {
                when (sizeString) {
                    "Min" -> sizes.add(ImageCaptureSize.MIN)
                    "Max" -> sizes.add(ImageCaptureSize.MAX)
                }
            }

            Collections.sort(sizes, ImageSizeComparator())
            return sizes
        }

        internal fun getFocusModes(activity: MainActivity): ArrayList<FocusMode> {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            val defModes: HashSet<String> =
                HashSet(activity.resources.getStringArray(R.array.array_settings_focus).asList())

            val modeStrings: HashSet<String> =
                sharedPref.getStringSet(activity.getString(R.string.settings_autotest_focus_key),
                    defModes) as HashSet<String>

            val modes: ArrayList<FocusMode> = ArrayList()

            for (modeString in modeStrings) {
                when (modeString) {
                    "Auto" -> modes.add(FocusMode.AUTO)
                    "Continuous" -> modes.add(FocusMode.CONTINUOUS)
                    "Fixed" -> modes.add(FocusMode.FIXED)
                }
            }

            Collections.sort(modes, FocusModeComparator())
            return modes
        }

        internal fun getOnlyLogical(activity: MainActivity): Boolean {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref.getBoolean(activity
                .getString(R.string.settings_autotest_cameras_key), true)
        }

        internal fun getSwitchTest(activity: MainActivity): Boolean {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getBoolean(activity.getString(R.string.settings_autotest_switchtest_key), true)
        }

        internal fun getSingleTestType(activity: MainActivity): String {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getString(activity.getString(R.string.settings_single_test_type_key), "PHOTO")
                ?: "PHOTO"
        }

        internal fun getSingleTestFocus(activity: MainActivity): String {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getString(activity.getString(R.string.settings_single_test_focus_key), "Auto")
                ?: "Auto"
        }

        internal fun getSingleTestImageSize(activity: MainActivity): String {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getString(activity.getString(R.string.settings_single_test_imagesize_key), "Max")
                ?: "Max"
        }

        internal fun getSingleTestCamera(activity: MainActivity): String {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getString(activity.getString(R.string.settings_single_test_camera_key), "0") ?: "0"
        }

        internal fun getSingleTestApi(activity: MainActivity): String {
            val sharedPref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
            return sharedPref
                .getString(activity.getString(R.string.settings_single_test_api_key), "Camera2")
                ?: "Camera2"
        }

        internal fun getCameraIds(
            activity: MainActivity,
            cameraParams: HashMap<String, CameraParams>
        ): ArrayList<String> {

            val cameraIds: ArrayList<String> = ArrayList()
            val onlyLogical: Boolean = getOnlyLogical(activity)

            // First we add all the cameras, then we either remove the logical ones or physical ones
            for (params in cameraParams)
                cameraIds.add(params.value.id)

            // Before 28, no physical camera access
            if (Build.VERSION.SDK_INT < 28)
                return cameraIds

            // For logical cameras, we remove all physical camera ids
            if (onlyLogical) {
                for (params in cameraParams) {
                    for (physicalId in params.value.physicalCameras)
                        cameraIds.remove(physicalId)
                }

                // For only physical, we check if it is backed by physical cameras, if so, remove it
                // as the physical cameras should already be in the list
            } else {
                for (params in cameraParams) {
                    if (params.value.hasMulti)
                        cameraIds.remove(params.value.id)
                }
            }

            Collections.sort(cameraIds)
            return cameraIds
        }

        internal fun getLogicalCameraIds(
            cameraParams: HashMap<String, CameraParams>
        ): ArrayList<String> {

            val cameraIds: ArrayList<String> = ArrayList()

            // First we add all the cameras, then we either remove the logical ones or physical ones
            for (params in cameraParams) {
                cameraIds.add(params.value.id)
            }
            // Before 28, no physical camera access
            if (Build.VERSION.SDK_INT < 28)
                return cameraIds

            // For logical cameras, we remove all physical camera ids
            for (params in cameraParams) {
                for (physicalId in params.value.physicalCameras) {
                    cameraIds.remove(physicalId)
                }
            }

            Collections.sort(cameraIds)
            return cameraIds
        }
    }

    // Order: Camera2, CameraX, Camera1
    internal class ApiComparator : Comparator<CameraAPI> {
        override fun compare(api1: CameraAPI, api2: CameraAPI): Int {
            if (api1 == api2)
                return 0
            if (api1 == CameraAPI.CAMERA2)
                return -1
            if (api2 == CameraAPI.CAMERA2)
                return 1
            if (api1 == CameraAPI.CAMERAX)
                return -1
            if (api2 == CameraAPI.CAMERAX)
                return 1

            // This should never happen
            return -1
        }
    }

    // Order: Max, Min
    internal class ImageSizeComparator : Comparator<ImageCaptureSize> {
        override fun compare(size1: ImageCaptureSize, size2: ImageCaptureSize): Int {
            if (size1 == size2)
                return 0
            if (size1 == ImageCaptureSize.MAX)
                return -1
            if (size2 == ImageCaptureSize.MAX)
                return 1

            // This should never happen
            return -1
        }
    }

    // Order: Auto, Continuous
    internal class FocusModeComparator : Comparator<FocusMode> {
        override fun compare(mode1: FocusMode, mode2: FocusMode): Int {
            if (mode1 == mode2)
                return 0
            if (mode1 == FocusMode.AUTO)
                return -1
            if (mode2 == FocusMode.AUTO)
                return 1

            // This should never happen
            return -1
        }
    }
}