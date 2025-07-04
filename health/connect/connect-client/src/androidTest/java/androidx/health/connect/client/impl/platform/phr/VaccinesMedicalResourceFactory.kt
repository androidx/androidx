/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform.phr

import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.impl.platform.phr.PhrConstants.FHIR_VERSION_4_0_1
import androidx.health.connect.client.impl.platform.phr.VaccinesMedicalResourceFactory.CompleteStatus.COMPLETE
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
import org.json.JSONObject

internal object VaccinesMedicalResourceFactory {
    private val FHIR_RESOURCE_JSON_DATA_IMMUNIZATIONS =
        JSONObject(
            """
            {
              "resourceType": "Immunization",
              "id": "immunization-1",
              "status": "completed",
              "vaccineCode": {
                "coding": [
                  {
                    "system": "http://hl7.org/fhir/sid/cvx",
                    "code": "115"
                  },
                  {
                    "system": "http://hl7.org/fhir/sid/ndc",
                    "code": "58160-842-11"
                  }
                ],
                "text": "Tdap"
              },
              "patient": {
                "reference": "Patient/patient_1",
                "display": "Example, Anne"
              },
              "encounter": {
                "reference": "Encounter/encounter_unk",
                "display": "GP Visit"
              },
              "occurrenceDateTime": "2018-05-21",
              "primarySource": true,
              "manufacturer": {
                "display": "Sanofi Pasteur"
              },
              "lotNumber": "1",
              "site": {
                "coding": [
                  {
                    "system": "http://terminology.hl7.org/CodeSystem/v3-ActSite",
                    "code": "LA",
                    "display": "Left Arm"
                  }
                ],
                "text": "Left Arm"
              },
              "route": {
                "coding": [
                  {
                    "system": "http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration",
                    "code": "IM",
                    "display": "Injection, intramuscular"
                  }
                ],
                "text": "Injection, intramuscular"
              },
              "doseQuantity": {
                "value": 0.5,
                "unit": "mL"
              },
              "performer": [
                {
                  "function": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/v2-0443",
                        "code": "AP",
                        "display": "Administering Provider"
                      }
                    ],
                    "text": "Administering Provider"
                  },
                  "actor": {
                    "reference": "Practitioner/practitioner_1",
                    "type": "Practitioner",
                    "display": "Dr Maria Hernandez"
                  }
                }
              ]
            }"""
                .trimIndent()
        )

    @OptIn(ExperimentalPersonalHealthRecordApi::class)
    fun createVaccinesUpsertMedicalResourceRequest(
        dataSourceId: String,
        fhirResourceId: String? = null,
        completeStatus: CompleteStatus = COMPLETE,
    ): UpsertMedicalResourceRequest {
        val data =
            FHIR_RESOURCE_JSON_DATA_IMMUNIZATIONS.apply {
                    putOpt("id", fhirResourceId)
                    put("status", completeStatus.stringValue)
                }
                .toString()
        return UpsertMedicalResourceRequest(dataSourceId, FHIR_VERSION_4_0_1, data)
    }

    enum class CompleteStatus(internal val stringValue: String) {
        COMPLETE("complete"),
        INCOMPLETE("incomplete"),
    }
}
