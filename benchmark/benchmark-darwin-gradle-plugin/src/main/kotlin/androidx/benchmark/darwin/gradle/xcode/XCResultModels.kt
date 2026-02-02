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

package androidx.benchmark.darwin.gradle.xcode

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// Rather unfortunate that all values types are wrapped in a property bag containing a single
// key called "_value". This is as per the JSON schema used by `xcresulttool`.

data class StringTypedValue(@SerializedName("_value") val value: String)

data class IntTypedValue(@SerializedName("_value") val value: Int)

data class DoubleTypedValue(@SerializedName("_value") val value: Double)

data class BooleanTypedValue(@SerializedName("_value") val value: Boolean)

data class Metrics(private val testsCount: IntTypedValue) {
    fun size(): Int {
        return testsCount.value
    }
}

data class TestReference(
    val id: StringTypedValue,
)

data class ActionResult(
    private val status: StringTypedValue,
    @SerializedName("testsRef") private val testsReference: TestReference
) {
    fun isSuccessful(): Boolean {
        return status.value == "succeeded"
    }

    fun testsReferenceId(): String {
        return testsReference.id.value
    }
}

data class ActionPlatformRecord(
    val identifier: StringTypedValue,
    val userDescription: StringTypedValue,
)

data class ActionSDKRecord(
    val name: StringTypedValue,
    val identifier: StringTypedValue,
    val operatingSystemVersion: StringTypedValue,
    val isInternal: BooleanTypedValue,
)

data class ActionDeviceRecord(
    val name: StringTypedValue,
    val isConcreteDevice: BooleanTypedValue,
    val operatingSystemVersion: StringTypedValue,
    val operatingSystemVersionWithBuildNumber: StringTypedValue,
    val nativeArchitecture: StringTypedValue,
    val modelName: StringTypedValue,
    val modelCode: StringTypedValue,
    val identifier: StringTypedValue,
    val isWireless: BooleanTypedValue,
    val cpuKind: StringTypedValue,
    val cpuCount: IntTypedValue?,
    val cpuSpeedInMHz: IntTypedValue?,
    val busSpeedInMHz: IntTypedValue?,
    val ramSizeInMegabytes: IntTypedValue?,
    val physicalCPUCoresPerPackage: IntTypedValue?,
    val logicalCPUCoresPerPackage: IntTypedValue?,
    val platformRecord: ActionPlatformRecord,
)

data class ActionRunDestinationRecord(
    val displayName: StringTypedValue,
    val targetArchitecture: StringTypedValue,
    val targetDeviceRecord: ActionDeviceRecord,
    val localComputerRecord: ActionDeviceRecord,
    val targetSDKRecord: ActionSDKRecord,
)

data class ActionRecord(
    val actionResult: ActionResult,
    val runDestination: ActionRunDestinationRecord
)

data class Actions(@SerializedName("_values") val actionRecords: List<ActionRecord>) {
    fun testReferences(): List<String> {
        return actionRecords.asSequence().map { it.actionResult.testsReferenceId() }.toList()
    }

    fun isSuccessful(): Boolean {
        return actionRecords.all { it.actionResult.isSuccessful() }
    }
}

data class ActionsInvocationRecord(val metrics: Metrics, val actions: Actions)

// Test Plan Summaries

data class ActionTestMetadataSummary(val id: StringTypedValue)

data class TypeDefinition(@SerializedName("_name") val name: String)

// Marker interface
sealed interface ActionsTestSummaryGroupOrMeta

data class ActionsTestSummaryGroupOrMetaArray(
    @SerializedName("_values") val values: List<ActionsTestSummaryGroupOrMeta>
)

data class ActionTestSummaryGroup(
    val duration: DoubleTypedValue,
    val identifier: StringTypedValue,
    val name: StringTypedValue,
    @SerializedName("subtests") val subTests: ActionsTestSummaryGroupOrMetaArray
) : ActionsTestSummaryGroupOrMeta {
    fun summaries(): List<ActionTestSummaryMeta> {
        return buildSummaries(mutableListOf(), this)
    }

    companion object {
        internal fun buildSummaries(
            summaries: MutableList<ActionTestSummaryMeta>,
            group: ActionTestSummaryGroup
        ): MutableList<ActionTestSummaryMeta> {
            for (subTest in group.subTests.values) {
                when (subTest) {
                    is ActionTestSummaryGroup -> buildSummaries(summaries, subTest)
                    is ActionTestSummaryMeta -> summaries += subTest
                }
            }
            return summaries
        }
    }
}

data class ActionTestSummaryMeta(
    val duration: DoubleTypedValue,
    val identifier: StringTypedValue,
    val name: StringTypedValue,
    val summaryRef: ActionTestMetadataSummary,
    val testStatus: StringTypedValue
) : ActionsTestSummaryGroupOrMeta {
    fun isSuccessful(): Boolean {
        return testStatus.value == "Success"
    }

    fun summaryRefId(): String {
        return summaryRef.id.value
    }
}

class ActionTestSummaryDeserializer : JsonDeserializer<ActionsTestSummaryGroupOrMeta> {
    override fun deserialize(
        jsonElement: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ActionsTestSummaryGroupOrMeta {
        return if (checkType(jsonElement, ACTION_TEST_SUMMARY_GROUP)) {
            val adapter = GsonHelpers.gson().getAdapter(ActionTestSummaryGroup::class.java)
            adapter.fromJson(jsonElement.toString())
        } else if (checkType(jsonElement, ACTION_TEST_SUMMARY_META)) {
            val adapter = GsonHelpers.gson().getAdapter(ActionTestSummaryMeta::class.java)
            adapter.fromJson(jsonElement.toString())
        } else {
            reportException(jsonElement)
        }
    }

    private fun reportException(jsonElement: JsonElement): Nothing {
        throw IllegalStateException("Unable to deserialize to ActionTestSummary ($jsonElement)")
    }

    companion object {
        private const val TYPE = "_type"
        private const val ACTION_TEST_SUMMARY_GROUP = "ActionTestSummaryGroup"
        private const val ACTION_TEST_SUMMARY_META = "ActionTestMetadata"

        internal fun checkType(jsonElement: JsonElement, name: String): Boolean {
            if (!jsonElement.isJsonObject) return false
            val json = jsonElement.asJsonObject
            val jsonType: JsonElement? = json.get(TYPE)
            if (jsonType != null && jsonType.isJsonObject) {
                val adapter = GsonHelpers.gson().getAdapter(TypeDefinition::class.java)
                val type = adapter.fromJson(jsonType.toString())
                return type.name == name
            }
            return false
        }
    }
}

data class ActionTestSummaryGroupArray(
    @SerializedName("_values") val values: List<ActionTestSummaryGroup>
)

data class ActionTestableSummary(
    val diagnosticsDirectoryName: StringTypedValue,
    val name: StringTypedValue,
    val projectRelativePath: StringTypedValue,
    val targetName: StringTypedValue,
    val tests: ActionTestSummaryGroupArray
)

data class ActionTestableSummaryArray(
    @SerializedName("_values") val values: List<ActionTestableSummary>
)

data class ActionTestPlanRunSummary(val testableSummaries: ActionTestableSummaryArray)

data class ActionTestPlanSummaryArray(
    @SerializedName("_values") val values: List<ActionTestPlanRunSummary>
)

data class ActionTestPlanRunSummaries(val summaries: ActionTestPlanSummaryArray) {
    fun testSummaries(): List<ActionTestSummaryMeta> {
        return summaries.values.flatMap { testPlanSummary ->
            testPlanSummary.testableSummaries.values.flatMap { testableSummary ->
                testableSummary.tests.values.flatMap { summaryGroup -> summaryGroup.summaries() }
            }
        }
    }
}

// Test Metrics

data class ActionTestActivitySummary(val title: StringTypedValue)

data class ActionTestActivitySummaryArray(
    @SerializedName("_values") val values: List<ActionTestActivitySummary>
) {
    fun title(): String? {
        return values.firstOrNull()?.title?.value
    }
}

data class MeasurementArray(@SerializedName("_values") val values: List<DoubleTypedValue>)

data class ActionTestPerformanceMetricSummary(
    val displayName: StringTypedValue,
    val identifier: StringTypedValue,
    val measurements: MeasurementArray,
    val polarity: StringTypedValue,
    val unitOfMeasurement: StringTypedValue,
)

data class ActionTestPerformanceMetricSummaryArray(
    @SerializedName("_values") val values: List<ActionTestPerformanceMetricSummary>
)

data class ActionTestSummary(
    val activitySummaries: ActionTestActivitySummaryArray,
    val name: StringTypedValue,
    val testStatus: StringTypedValue,
    val performanceMetrics: ActionTestPerformanceMetricSummaryArray
) {
    fun isSuccessful(): Boolean {
        return testStatus.value == "Success"
    }

    fun title(): String? {
        return activitySummaries.title()
    }
}
