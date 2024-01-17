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
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CreatePublicKeyCredentialControllerTestUtils {
    companion object {

        const val TAG = "PasskeyTestUtils"

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

        // This signature indicates what the json above, after parsing, must contain
        const val OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE = "{\"rp\":{\"name\":true," +
            "\"id\":true},\"user\":{\"name\":true,\"id\":true,\"displayName\":true," +
            "\"icon\":true}, \"challenge\":true,\"pubKeyCredParams\":true," +
            "\"excludeCredentials\":true," + "\"attestation\":true}"

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
             "\"transports\"=[\"ble\"]}]," +
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
             "\"excludeCredentials\": [{\"id\":\"AA\",\"transports\"=[\"ble\"]}]," +
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
            "\"excludeCredentials\": [{\"id\":\"AA\",\"type\":\"public-key\"," +
            "\"transports\"=[\"ble\"]}], " + "\"authenticatorSelection\": " +
            "{\"authenticatorAttachment\": \"platform\", \"residentKey\": \"required\", " +
            "\"requireResidentKey\": true, \"userVerification\": \"preferred\"}, " +
            "\"attestation\": \"none\"}")

        // This signature indicates what [MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL], after
        // parsing, must contain. It is a 'brace' to ensure required values are tested.
        const val ALL_REQUIRED_AND_OPTIONAL_SIGNATURE = "{\"rp\":{\"name\":true,\"id\":true}," +
            "\"user\":{\"id\":true,\"name\":true,\"displayName\":true,\"icon\":true}," +
            "\"challenge\":true,\"pubKeyCredParams\":true,\"timeout\":true," +
            "\"excludeCredentials\":true,\"authenticatorSelection\":{" +
            "\"authenticatorAttachment\":true,\"residentKey\":true,\"requireResidentKey\":true," +
            "\"userVerification\":true},\"attestation\":true}"

        // Contains all required keys for the JSON, but not any of the other cases
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

        // This signature indicates what [MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT], after
        // parsing, must contain. It is a 'brace' to ensure required values are tested.
        const val ALL_REQUIRED_FIELDS_SIGNATURE = "{\"rp\":{\"name\":true,\"id\":true}," +
            "\"user\":{\"id\":true,\"name\":true,\"displayName\":true,\"icon\":true}," +
            "\"challenge\":true,\"pubKeyCredParams\":true,\"excludeCredentials\":true," +
            "\"attestation\":true}"

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
                authSelect.put("userVerification", "preferred")
                json.put("authenticatorSelection", authSelect)
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
                    descriptorJSON.transports?.let {
                        descriptorI.put("transports",
                            createJSONArrayFromTransports(descriptorJSON.transports!!))
                    }
                    descriptor.put(descriptorI)
                }
            }
            json.put("excludeCredentials", descriptor)
        }

        private fun createJSONArrayFromTransports(transports: List<Transport>): JSONArray {
            val jsonArr = JSONArray()
            for (transport in transports) {
                jsonArr.put(transport.toString())
            }
            return jsonArr
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
                val optionalTimeout: Int = options.timeoutSeconds!!.toInt()
                json.put("timeout", optionalTimeout * 1000)
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

        /**
         * This converts all JSON Leaves to a 'true' boolean value. Note this is lax on
         * lists/JSONArrays. In short, it creates a 'signature' for a JSONObject. It can be used
         * to generate constants which can be used to test with.
         *
         * For example, given this json object
         * ```
         * {"rp":{"name":true,"id":true},"user":{
         * "id":true,"name":true,"displayName":true,"icon":true
         * },"challenge":true,"pubKeyCredParams":true,"excludeCredentials":true,"attestation":true}
         * ```
         * notice that all the 'leaves' have become true outside of the array exception. This can
         * be used to make fixed required keys.
         *
         * @param json the json object with which to modify in place
         */
        @JvmStatic
        fun convertJsonLeavesToBooleanSignature(json: JSONObject) {
            val keys = json.keys()
            for (key in keys) {
                val value = json.get(key)
                if (value is JSONObject) {
                    convertJsonLeavesToBooleanSignature(value)
                } else {
                    json.put(key, true)
                }
            }
        }

        /**
         * Helps generate a PublicKeyCredential response json format to start tests with, locally
         * for example.
         *
         * Usage details as follows:
         *
         *     val byteArrayClientDataJson = byteArrayOf(0x48, 101, 108, 108, 111)
         *     val byteArrayAuthenticatorData = byteArrayOf(0x48, 101, 108, 108, 112)
         *     val byteArraySignature = byteArrayOf(0x48, 101, 108, 108, 113)
         *     val byteArrayUserHandle = byteArrayOf(0x48, 101, 108, 108, 114)
         *     val publicKeyCredId = "id"
         *     val publicKeyCredRawId = byteArrayOf(0x48, 101, 108, 108, 115)
         *     val publicKeyCredType = "type"
         *     val authenticatorAttachment = "platform"
         *     val hasClientExtensionOutputs = true
         *     val isDiscoverableCredential = true
         *     val expectedClientExtensions = "{\"credProps\":{\"rk\":true}}"
         *
         *     val json = PublicKeyCredentialControllerUtility.beginSignInAssertionResponse(
         *       byteArrayClientDataJson,
         *       byteArrayAuthenticatorData,
         *       byteArraySignature,
         *       byteArrayUserHandle,
         *       publicKeyCredId,
         *       publicKeyCredRawId,
         *       publicKeyCredType,
         *       authenticatorAttachment,
         *       hasClientExtensionOutputs,
         *       isDiscoverableCredential
         *     )
         *
         * The json can be used as necessary, even if only to generate a log with which to pull
         * the string from (to then further use that string in other test cases).
         */
        fun getPublicKeyCredentialResponseGenerator(
            clientDataJSON: ByteArray,
            authenticatorData: ByteArray,
            signature: ByteArray,
            userHandle: ByteArray?,
            publicKeyCredId: String,
            publicKeyCredRawId: ByteArray,
            publicKeyCredType: String,
            authenticatorAttachment: String?,
            hasClientExtensionResults: Boolean,
            isDiscoverableCredential: Boolean?
        ): JSONObject {
            val json = JSONObject()
            val responseJson = JSONObject()
            responseJson.put(
                PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_DATA,
                PublicKeyCredentialControllerUtility.b64Encode(clientDataJSON)
            )
            responseJson.put(
                PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_DATA,
                PublicKeyCredentialControllerUtility.b64Encode(authenticatorData)
            )
            responseJson.put(
                PublicKeyCredentialControllerUtility.JSON_KEY_SIGNATURE,
                PublicKeyCredentialControllerUtility.b64Encode(signature)
            )
            userHandle?.let {
                responseJson.put(
                    PublicKeyCredentialControllerUtility.JSON_KEY_USER_HANDLE,
                    PublicKeyCredentialControllerUtility.b64Encode(userHandle)
                )
            }
            json.put(PublicKeyCredentialControllerUtility.JSON_KEY_RESPONSE, responseJson)
            json.put(PublicKeyCredentialControllerUtility.JSON_KEY_ID, publicKeyCredId)
            json.put(
                PublicKeyCredentialControllerUtility.JSON_KEY_RAW_ID,
                PublicKeyCredentialControllerUtility.b64Encode(publicKeyCredRawId)
            )
            json.put(PublicKeyCredentialControllerUtility.JSON_KEY_TYPE, publicKeyCredType)
            addOptionalAuthenticatorAttachmentAndRequiredExtensions(
                authenticatorAttachment,
                hasClientExtensionResults,
                isDiscoverableCredential,
                json
            )
            return json;
        }

        // This can be shared by both get and create flow response parsers, fills 'json'.
        private fun addOptionalAuthenticatorAttachmentAndRequiredExtensions(
            authenticatorAttachment: String?,
            hasClientExtensionResults: Boolean,
            isDiscoverableCredential: Boolean?,
            json: JSONObject
        ) {

            json.putOpt(
                PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_ATTACHMENT,
                authenticatorAttachment
            )

            val clientExtensionsJson = JSONObject()

            if (hasClientExtensionResults) {
                if (isDiscoverableCredential != null) {
                    val credPropsObject = JSONObject()
                    credPropsObject.put(
                        PublicKeyCredentialControllerUtility.JSON_KEY_RK,
                        isDiscoverableCredential
                    )
                    clientExtensionsJson.put(
                        PublicKeyCredentialControllerUtility.JSON_KEY_CRED_PROPS,
                        credPropsObject
                    )
                }
            }

            json.put(
                PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_EXTENSION_RESULTS,
                clientExtensionsJson
            )
        }
    }
}
