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

package androidx.health.connect.client.matchmaking

import android.annotation.SuppressLint
import android.os.Build
import androidx.health.connect.client.ExperimentalMatchmakingApi
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.withMatchmakingFeatureCheck
import androidx.health.connect.client.impl.platform.records.toPlatformDataOrigin
import androidx.health.connect.client.impl.platform.records.toPlatformRecordClass
import androidx.health.connect.client.impl.platform.request.PlatformMatchmakingRequest
import androidx.health.connect.client.impl.platform.request.PlatformMatchmakingRequestBuilder
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.isAtLeastSdkExtension23
import androidx.health.connect.client.records.metadata.DataOrigin
import kotlin.reflect.KClass

/**
 * Request class for matchmaking flow.
 *
 * Use this class to launch a matchmaking flow to discover apps and devices that can write health
 * data that the calling app has permission to read.
 *
 * To check if matchmaking is available, call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.Companion.FEATURE_MATCHMAKING] as an argument.
 *
 * @property recordTypes Set of [androidx.health.connect.client.records.Record] classes to find
 *   matching apps and devices for. If empty, the flow focuses on all record types for which the
 *   calling package has permission to read.
 * @property includedDataSources Set of [DataOrigin] to include in the matchmaking results. If
 *   non-empty, only data sources whose package names are present in this set are considered.
 * @property excludedDataSources Set of [DataOrigin] to exclude from the matchmaking results. Data
 *   sources whose package names are present in this set are excluded.
 *
 * Note: If a data source is an app, the calling app must have visibility of the package name (e.g.
 * declared in the manifest inside `<queries>`). If a data source is a device, the calling app does
 * not need to declare it in the manifest.
 *
 * [includedDataSources] and [excludedDataSources] cannot both be set at the same time.
 */
@ExperimentalMatchmakingApi
class MatchmakingRequest(
    val recordTypes: Set<KClass<out Record>> = emptySet(),
    val includedDataSources: Set<DataOrigin> = emptySet(),
    val excludedDataSources: Set<DataOrigin> = emptySet(),
) {
    /*
     * Android U devices with SDK extension 23 and later use the platform's validation
     * instead of Jetpack validation.
     */
    init {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                isAtLeastSdkExtension23()
        ) {
            this.platformMatchmakingRequest
        } else {
            require(includedDataSources.isEmpty() || excludedDataSources.isEmpty()) {
                "Cannot set both includeDataSources and excludeDataSources"
            }
        }
    }

    @get:SuppressLint("NewApi") // checked by withMatchmakingFeatureCheck
    internal val platformMatchmakingRequest: PlatformMatchmakingRequest
        get() =
            withMatchmakingFeatureCheck(this::class, "platformMatchmakingRequest") {
                PlatformMatchmakingRequestBuilder()
                    .addRecordTypes(recordTypes.map { it.toPlatformRecordClass() }.toSet())
                    .apply {
                        if (includedDataSources.isNotEmpty()) {
                            setIncludedDataSources(
                                includedDataSources.map { it.toPlatformDataOrigin() }.toSet()
                            )
                        }
                        if (excludedDataSources.isNotEmpty()) {
                            setExcludedDataSources(
                                excludedDataSources.map { it.toPlatformDataOrigin() }.toSet()
                            )
                        }
                    }
                    .build()
            }

    override fun toString(): String {
        return "MatchmakingRequest(recordTypes=$recordTypes, " +
            "includedDataSources=$includedDataSources, " +
            "excludedDataSources=$excludedDataSources)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MatchmakingRequest) return false

        if (recordTypes != other.recordTypes) return false
        if (includedDataSources != other.includedDataSources) return false
        if (excludedDataSources != other.excludedDataSources) return false
        return true
    }

    override fun hashCode(): Int {
        var result = recordTypes.hashCode()
        result = 31 * result + includedDataSources.hashCode()
        result = 31 * result + excludedDataSources.hashCode()
        return result
    }
}
