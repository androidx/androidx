/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.mediarouting

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.SuggestedDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceSuggestionsViewModel(application: Application) : AndroidViewModel(application) {
    private val mediaRouter: MediaRouter = MediaRouter.getInstance(application)
    private var suggestedRouteId: String? = null

    private val _suggestedDevice = MutableStateFlow<SuggestedDeviceInfo?>(null)
    val suggestedDevice: StateFlow<SuggestedDeviceInfo?> = _suggestedDevice.asStateFlow()

    private val deviceSuggestionsCallback =
        object : MediaRouter.DeviceSuggestionsUpdatesCallback {
            override fun onSuggestionsUpdated(
                suggestingPackageName: String,
                suggestedDeviceInfo: List<SuggestedDeviceInfo>,
            ) {
                _suggestedDevice.value =
                    if (suggestedDeviceInfo.isEmpty()) null else suggestedDeviceInfo.first()
                suggestedRouteId = _suggestedDevice.value?.routeId
            }

            override fun onSuggestionsCleared(suggestingPackageName: String) {
                _suggestedDevice.value = null
                suggestedRouteId = null
            }

            override fun onSuggestionsRequested() {
                // no-op. Adding a log for validation.
                Log.i(TAG, "onSuggestionsRequested called")
            }
        }

    init {
        mediaRouter.registerDeviceSuggestionsUpdatesCallback(
            deviceSuggestionsCallback,
            /* executor= */ null,
        )
    }

    fun onChangeSuggestedDevice() {
        val routes = mediaRouter.routes
        // The selector for getting routes is already set in MainActivity.
        if (routes.isEmpty()) {
            Log.e(TAG, "Routes should not be empty!")
            return
        }
        var suggestedRouteIndex = routes.indexOfFirst { suggestedRouteId == it.id }
        suggestedRouteIndex = (suggestedRouteIndex + 1) % routes.size
        val nextSuggestedRoute = routes[suggestedRouteIndex]
        val deviceInfo =
            SuggestedDeviceInfo.Builder(
                    nextSuggestedRoute.name,
                    nextSuggestedRoute.id,
                    nextSuggestedRoute.deviceType,
                )
                .build()
        mediaRouter.setDeviceSuggestions(listOf(deviceInfo))
    }

    fun onClearSuggestedDevice() {
        mediaRouter.clearDeviceSuggestions()
    }

    override fun onCleared() {
        mediaRouter.unregisterDeviceSuggestionsUpdatesCallback(deviceSuggestionsCallback)
        mediaRouter.clearDeviceSuggestions()
        super.onCleared()
    }

    companion object {
        const val TAG: String = "DeviceSuggestionsViewModel"
    }
}
