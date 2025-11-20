/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.health.connect.client.records

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.records.PlatformMedicalResource
import androidx.health.connect.client.impl.platform.records.PlatformMedicalResourceBuilder
import androidx.health.connect.client.impl.platform.records.toPlatformMedicalResourceType
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_MEDICAL_DATA_VACCINES
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_VACCINES

/**
 * A class to hold medical resource data.
 *
 * Unlike FHIR resource which is represented in Health Connect with [FhirResource],
 * `MedicalResource` is a Health Connect specific concept. A `MedicalResource` contains more than
 * just a [FhirResource], notably, it also contains:
 * - A [dataSourceId] representing ID of a [MedicalDataSource] where data of this `MedicalResource`
 *   come from.
 * - A [MedicalResourceType] indicating how HealthConnect categorizes the [FhirResource]. Each
 *   [MedicalResourceType] is tied to a read permission. For example, a client can only read
 *   `MedicalResource`s with [MEDICAL_RESOURCE_TYPE_VACCINES] if it holds
 *   [PERMISSION_READ_MEDICAL_DATA_VACCINES].
 *
 * The data representation follows the
 * [Fast Healthcare Interoperability Resources (FHIR)](https://hl7.org/fhir/) standard.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An
 * [UnsupportedOperationException] would be thrown if the feature is not available.
 *
 * @property type The [MedicalResourceType] of this `MedicalResource`, this is assigned by Health
 *   Connect at insertion time. Clients should be aware that this list is non exhaustive and may
 *   increase in future releases when additional types will need to be handled.
 * @property id The unique ID of this `MedicalResource` represented by a [MedicalResourceId].
 * @property dataSourceId The ID of the [MedicalDataSource] where this `MedicalResource` comes from.
 * @property fhirVersion The FHIR version of [fhirResource].
 * @property fhirResource The [FhirResource] that this `MedicalResource` represents.
 */
@ExperimentalPersonalHealthRecordApi
class MedicalResource(
    @MedicalResourceType val type: Int,
    val id: MedicalResourceId,
    val dataSourceId: String,
    val fhirVersion: FhirVersion,
    val fhirResource: FhirResource,
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformMedicalResource: PlatformMedicalResource =
        withPhrFeatureCheck(this::class) {
            PlatformMedicalResourceBuilder(
                    type.toPlatformMedicalResourceType(),
                    dataSourceId,
                    fhirVersion.platformFhirVersion,
                    fhirResource.platformFhirResource,
                )
                .build()
        }

    override fun toString(): String =
        toString(
            this,
            mapOf(
                "type" to type,
                "dataSourceId" to dataSourceId,
                "fhirVersion" to fhirVersion,
                "fhirResource" to fhirResource,
            ),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MedicalResource) return false

        if (type != other.type) return false
        if (id != other.id) return false
        if (dataSourceId != other.dataSourceId) return false
        if (fhirVersion != other.fhirVersion) return false
        if (fhirResource != other.fhirResource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + id.hashCode()
        result = 31 * result + dataSourceId.hashCode()
        result = 31 * result + fhirVersion.hashCode()
        result = 31 * result + fhirResource.hashCode()
        return result
    }

    companion object {
        /** Medical resource type that labels data as vaccines. */
        const val MEDICAL_RESOURCE_TYPE_VACCINES = 1

        /** Medical resource type that labels data as allergies or intolerances. */
        const val MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES = 2

        /** Medical resource type that labels data as to do with pregnancy. */
        const val MEDICAL_RESOURCE_TYPE_PREGNANCY = 3

        /** Medical resource type that labels data as social history. */
        const val MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY = 4

        /** Medical resource type that labels data as vital signs. */
        const val MEDICAL_RESOURCE_TYPE_VITAL_SIGNS = 5

        /** Medical resource type that labels data as results (Laboratory or pathology). */
        const val MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS = 6

        /**
         * Medical resource type that labels data as medical conditions (clinical condition,
         * problem, diagnosis etc).
         */
        const val MEDICAL_RESOURCE_TYPE_CONDITIONS = 7

        /**
         * Medical resource type that labels data as procedures (actions taken on or for a patient).
         */
        const val MEDICAL_RESOURCE_TYPE_PROCEDURES = 8

        /** Medical resource type that labels data as medication related. */
        const val MEDICAL_RESOURCE_TYPE_MEDICATIONS = 9

        /**
         * Medical resource type that labels data as related to personal details, including
         * demographic information such as name, date of birth, and contact details such as address
         * or telephone numbers.
         */
        const val MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS = 10

        /**
         * Medical resource type that labels data as related to practitioners. This is information
         * about the doctors, nurses, masseurs, physios, etc who have been involved with the user.
         */
        const val MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS = 11

        /**
         * Medical resource type that labels data as related to an encounter with a practitioner.
         * This includes visits to healthcare providers and remote encounters such as telephone and
         * videoconference appointments, and information about the time, location and organization
         * who is being met.
         */
        const val MEDICAL_RESOURCE_TYPE_VISITS = 12

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
            MEDICAL_RESOURCE_TYPE_CONDITIONS,
            MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS,
            MEDICAL_RESOURCE_TYPE_MEDICATIONS,
            MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS,
            MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS,
            MEDICAL_RESOURCE_TYPE_PREGNANCY,
            MEDICAL_RESOURCE_TYPE_PROCEDURES,
            MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY,
            MEDICAL_RESOURCE_TYPE_VACCINES,
            MEDICAL_RESOURCE_TYPE_VISITS,
            MEDICAL_RESOURCE_TYPE_VITAL_SIGNS,
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class MedicalResourceType
    }
}
