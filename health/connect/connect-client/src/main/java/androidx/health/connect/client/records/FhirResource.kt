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
import androidx.health.connect.client.impl.platform.records.PlatformFhirResource
import androidx.health.connect.client.impl.platform.records.PlatformFhirResourceBuilder
import androidx.health.connect.client.impl.platform.records.toPlatformFhirResourceType

/**
 * A class to capture the FHIR resource data. This is the class used for all supported FHIR resource
 * types, which is a subset of the resource list on
 * [the official FHIR website](https://build.fhir.org/resourcelist.html) and might expand in the
 * future.
 *
 * FHIR stands for the [Fast Healthcare Interoperability Resources](https://hl7.org/fhir/).
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An
 * [UnsupportedOperationException] would be thrown if the feature is not available.
 *
 * @property type The type of this FHIR resource. This is extracted from the `resourceType` field in
 *   [data]. The list of supported types is a subset of the resource list on
 *   [the official FHIR website](https://build.fhir.org/resourcelist.html) and might expand in the
 *   future.
 * @property id The FHIR resource ID. This is extracted from the `id` field in [data]. More about
 *   FHIR resource ID in
 *   [https://www.hl7.org/fhir/resource.html#id](https://www.hl7.org/fhir/resource.html#id).
 * @property data The FHIR resource data in JSON representation.
 */
@ExperimentalPersonalHealthRecordApi
class FhirResource(@FhirResourceType val type: Int, val id: String, val data: String) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformFhirResource: PlatformFhirResource =
        withPhrFeatureCheck(this::class) {
            PlatformFhirResourceBuilder(type.toPlatformFhirResourceType(), id, data).build()
        }

    override fun toString() = toString(this, mapOf("type" to type, "id" to id, "data" to data))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FhirResource) return false

        if (type != other.type) return false
        if (id != other.id) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + id.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    companion object {
        /** FHIR resource type for [Immunization](https://www.hl7.org/fhir/immunization.html). */
        const val FHIR_RESOURCE_TYPE_IMMUNIZATION = 1

        /**
         * FHIR resource type for
         * [AllergyIntolerance](https://www.hl7.org/fhir/allergyintolerance.html).
         */
        const val FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE = 2

        /**
         * FHIR resource type for a [FHIR Observation](https://www.hl7.org/fhir/observation.html).
         */
        const val FHIR_RESOURCE_TYPE_OBSERVATION = 3

        /** FHIR resource type for a [FHIR Condition](https://www.hl7.org/fhir/condition.html). */
        const val FHIR_RESOURCE_TYPE_CONDITION = 4

        /** FHIR resource type for a [FHIR Procedure](https://www.hl7.org/fhir/procedure.html). */
        const val FHIR_RESOURCE_TYPE_PROCEDURE = 5

        /** FHIR resource type for a [FHIR Medication](https://www.hl7.org/fhir/medication.html). */
        const val FHIR_RESOURCE_TYPE_MEDICATION = 6

        /**
         * FHIR resource type for a
         * [FHIR MedicationRequest](https://www.hl7.org/fhir/medicationrequest.html).
         */
        const val FHIR_RESOURCE_TYPE_MEDICATION_REQUEST = 7

        /**
         * FHIR resource type for a
         * [FHIR MedicationStatement](https://www.hl7.org/fhir/medicationstatement.html).
         */
        const val FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT = 8

        /** FHIR resource type for a [FHIR Patient](https://www.hl7.org/fhir/patient.html). */
        const val FHIR_RESOURCE_TYPE_PATIENT = 9

        /**
         * FHIR resource type for a [FHIR Practitioner](https://www.hl7.org/fhir/practitioner.html).
         */
        const val FHIR_RESOURCE_TYPE_PRACTITIONER = 10

        /**
         * FHIR resource type for a
         * [FHIR PractitionerRole](https://www.hl7.org/fhir/practitionerrole.html).
         */
        const val FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE = 11

        /** FHIR resource type for a [FHIR Encounter](https://www.hl7.org/fhir/encounter.html). */
        const val FHIR_RESOURCE_TYPE_ENCOUNTER = 12

        /** FHIR resource type for a [FHIR Location](https://www.hl7.org/fhir/location.html). */
        const val FHIR_RESOURCE_TYPE_LOCATION = 13

        /**
         * FHIR resource type for a [FHIR Organization](https://www.hl7.org/fhir/organization.html).
         */
        const val FHIR_RESOURCE_TYPE_ORGANIZATION = 14

        /** List of possible FHIR resource types. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
            FHIR_RESOURCE_TYPE_OBSERVATION,
            FHIR_RESOURCE_TYPE_CONDITION,
            FHIR_RESOURCE_TYPE_PROCEDURE,
            FHIR_RESOURCE_TYPE_MEDICATION,
            FHIR_RESOURCE_TYPE_MEDICATION_REQUEST,
            FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT,
            FHIR_RESOURCE_TYPE_PATIENT,
            FHIR_RESOURCE_TYPE_PRACTITIONER,
            FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE,
            FHIR_RESOURCE_TYPE_ENCOUNTER,
            FHIR_RESOURCE_TYPE_LOCATION,
            FHIR_RESOURCE_TYPE_ORGANIZATION,
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class FhirResourceType
    }
}
