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

package androidx.credentials.playservices.createkeycredential

import androidx.credentials.playservices.TestUtils
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CreatePublicKeyCredentialControllerTestUtils {
    companion object {

         // optional and not required key 'transports' is missing in the JSONObject that composes
         // up the JSONArray found at key 'excludeCredentials'
         const val OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD = ("{\"rp\": {\"name\": " +
             "\"Address " + "Book\", " + "\"id\": " +
             "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"name\": \"lee@gmail.com\", " +
             "\"id\": " + "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\"," +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
             "\"excludeCredentials\": [{\"id\":\"AA\",\"type\":\"public-key\"}]," +
             "\"attestation\": \"none\"}")

        // optional, but if it exists, required key 'type' exists but is empty in the JSONObject
        // that composes up the JSONArray found at key 'excludeCredentials'
         const val OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD = ("{\"rp\": {\"name\": " +
            "\"Address " + "Book\", " + "\"id\": " +
             "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"name\": \"lee@gmail.com\", " +
             "\"id\": " + "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\"," +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
             "\"excludeCredentials\": [{\"type\":\"\",\"id\":\"public-key\"," +
             "\"transports\"=[\"usb\"]}]," +
             "\"attestation\": \"none\"}")

        // optional, but if it exists, required key 'type' is missing in the JSONObject that
        // composes up the JSONArray found at key 'excludeCredentials'
         const val OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD = ("{\"rp\": {\"name\": " +
             "\"Address " + "Book\", " + "\"id\": " + "\"addressbook-c7876.uc.r.appspot.com\"}, " +
             "\"user\": {\"name\": \"lee@gmail.com\", " + "\"id\": " +
             "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\"," +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
             "\"excludeCredentials\": [{\"id\":\"AA\",\"transports\"=[\"usb\"]}]," +
             "\"attestation\": \"none\"}")

         // user id is non existent
         const val MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD = ("{\"rp\": {\"name\": " +
             "\"Address " + "Book\", " + "\"id\": " +
             "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"name\": \"lee@gmail.com\", " +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
             "\"excludeCredentials\": []," + "\"attestation\": \"none\"}")

        // user id is empty ("")
        const val MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY = ("{\"rp\": {\"name\": " +
            "\"Address " + "Book\", " + "\"id\": " +
            "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"id\": " +
            "\"\", \"name\": \"lee@gmail.com\", " +
            "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
            "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
            "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
            "\"public-key\", \"alg\": -7}, {\"type\": " +
            "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
            "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
            "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
            "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
            "\"excludeCredentials\": []," + "\"attestation\": \"none\"}")

        // all required and optional types are here
        const val MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT = ("{\"rp\": " +
            "{\"name\": " + "\"Address Book\", " + "\"id\": " +
            "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"id\": " +
            "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\", \"name\": \"lee@gmail.com\", " +
            "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
            "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
            "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
            "\"public-key\", \"alg\": -7}, {\"type\": " +
            "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
            "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
            "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
            "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}], \"timeout\": 60000, " +
            "\"excludeCredentials\": [{\"id\":\"AA\",\"type\":\"A\",\"transports\"=[\"A\"]}], " +
            "\"authenticatorSelection\": " +
            "{\"authenticatorAttachment\": \"platform\", \"residentKey\": \"required\", " +
            "\"requireResidentKey\": true, \"userVerification\": \"preferred\"}, " +
            "\"attestation\": \"none\"}")

        const val MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT = ("{\"rp\": {\"name\": " +
            "\"Address " + "Book\", " + "\"id\": " + "\"addressbook-c7876.uc.r.appspot.com\"}, " +
            "\"user\": {\"id\": " +
            "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\", \"name\": \"lee@gmail.com\", " +
            "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
            "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
            "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
            "\"public-key\", \"alg\": -7}, {\"type\": " +
            "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
            "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
            "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
            "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
            "\"excludeCredentials\": []," + "\"attestation\": \"none\"}")

        /**
         * Generates a JSON for the **create request** flow that is maximally filled given the inputs,
         * so it can always be a representative json to any input to compare against for this
         * create request flow, acting as a 'subset' as during parsing certain values may have
         * been removed if not required from the input based on the fido impl flow. I.e. the input
         * that generates the [PublicKeyCredentialCreationOptions] we utilize here must be a
         * superset based on the FIDO Implementation, *not* the spec! Then during parsing, further
         * values may have been removed, meaning the JSON formed from
         * [PublicKeyCredentialCreationOptions] is a guaranteed subset, never greater than the
         * input json superset.
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun createJsonObjectFromPublicKeyCredentialCreationOptions(
            options: PublicKeyCredentialCreationOptions
        ): JSONObject {
            val json = JSONObject()
            configureRpAndUser(options, json)
            configureChallengeParamsAndTimeout(options, json)
            configureDescriptors(options, json)
            configureSelectionCriteriaAndAttestation(options, json)

            // TODO("Handle extensions in this testing parsing")
            return json
        }

        private fun configureSelectionCriteriaAndAttestation(
            options: PublicKeyCredentialCreationOptions,
            json: JSONObject
        ) {
            val selectionCriteria = options.authenticatorSelection
            selectionCriteria?.let {
                val authSelect = JSONObject()
                val authAttachment = selectionCriteria.attachmentAsString
                if (authAttachment != null) authSelect.put(
                    "authenticatorAttachment",
                    authAttachment
                )
                val residentKey = selectionCriteria.residentKeyRequirementAsString
                if (residentKey != null) authSelect.put("residentKey", residentKey)
                if (selectionCriteria.requireResidentKey != null) {
                    val requireResidentKey = selectionCriteria.requireResidentKey!!
                    authSelect.put("requireResidentKey", requireResidentKey)
                }
                json.put("authenticatorSelection", authSelect)
                // TODO("Missing userVerification in fido impl")
            }
            val attestation = options.attestationConveyancePreferenceAsString
            if (attestation != null) {
                json.put("attestation", attestation)
            }
        }

        private fun configureDescriptors(
            options: PublicKeyCredentialCreationOptions,
            json: JSONObject
        ) {
            val descriptors = options.excludeList
            val descriptor = JSONArray()
            if (descriptors != null) {
                for (descriptorJSON in descriptors) {
                    val descriptorI = JSONObject()
                    descriptorI.put("id", TestUtils.b64Encode(descriptorJSON.id))
                    descriptorI.put("type", descriptorJSON.type)
                    descriptorI.put("transports", descriptorJSON.transports)
                    descriptor.put(descriptorI)
                }
            }
            json.put("excludeCredentials", descriptor)
        }

        private fun configureChallengeParamsAndTimeout(
            options: PublicKeyCredentialCreationOptions,
            json: JSONObject
        ) {
            val requiredChallenge = options.challenge
            json.put("challenge", TestUtils.b64Encode(requiredChallenge))
            val publicKeyCredentialParameters = options.parameters
            val parameters = JSONArray()
            for (params in publicKeyCredentialParameters) {
                val paramI = JSONObject()
                paramI.put("type", params.typeAsString)
                paramI.put("alg", params.algorithmIdAsInteger)
                parameters.put(paramI)
            }
            json.put("pubKeyCredParams", parameters)
            if (options.timeoutSeconds != null) {
                val optionalTimeout = options.timeoutSeconds!!
                json.put("timeout", optionalTimeout)
            }
        }

        private fun configureRpAndUser(
            options: PublicKeyCredentialCreationOptions,
            json: JSONObject
        ) {
            val rpJson = JSONObject()
            val rpRequired = options.rp
            val rpName = rpRequired.name
            rpJson.put("name", rpName)
            val rpId = rpRequired.id
            rpJson.put("id", rpId)
            val optionalRpIcon = rpRequired.icon
            if (optionalRpIcon != null) {
                rpJson.put("icon", optionalRpIcon)
            }
            json.put("rp", rpJson)
            val userJson = JSONObject()
            val userRequired = options.user
            val userId = userRequired.id
            userJson.put("id", TestUtils.b64Encode(userId))
            val userName = userRequired.name
            userJson.put("name", userName)
            val userDisplayName = userRequired.displayName
            userJson.put("displayName", userDisplayName)
            val optionalUserIcon = userRequired.icon
            if (optionalUserIcon != null) {
                userJson.put("icon", optionalUserIcon)
            }
            json.put("user", userJson)
        }
    }
}