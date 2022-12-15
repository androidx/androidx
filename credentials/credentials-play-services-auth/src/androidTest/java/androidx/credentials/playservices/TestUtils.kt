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

package androidx.credentials.playservices

import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.google.android.gms.common.ConnectionResult

class TestUtils {
    companion object {

        fun b64Encode(byteArray: ByteArray): String {
            return PublicKeyCredentialControllerUtility.b64Encode(byteArray)
        }

        fun b64Decode(b64String: String): ByteArray {
            return PublicKeyCredentialControllerUtility.b64Decode(b64String)
        }

        fun isAlgSupported(alg: Int): Boolean {
            return PublicKeyCredentialControllerUtility.checkAlgSupported(alg)
        }

        /**
         * Given a superset and a subset json, this figures out if the subset can be found
         * within the superset by recursively checking for values that exist in the subset
         * also existing in the superset in the same format. Note this means that the superset
         * is always equal to or larger than the subset json but should contain every key and
         * **sub-value** pair found in the subset. I.e. for example:
         * ```
         * storeA = {a : {b : 2,  c : 2, d : 2, e : {x : 3, y : 3, z : 3} }, q: 5}
         * storeB = {a : {b : 2, d : 2, e : {x : 3} }, q : 5}
         * isSubsetJson(storeA, storeB) //-> true
         * ```
         *
         * Note this is lax on json arrays and their inner objects, that can be extended if
         * required.
         *
         * @param superset the superset json that should have all the keys and values and subvalues
         * that the subset contains as well as new keys and values/subvalues the subset does not
         * contain
         * @param subset the subset json that should have equal to or less keys and subvalues than
         * the superset
         * @return a boolean indicating if the subset was truly a subset of the superset it was
         * tested with
         */
        fun isSubsetJson(superset: JSONObject, subset: JSONObject): Boolean {
            val keys = subset.keys()
            for (key in keys) {
                if (!superset.has(key)) {
                    return false
                }
                val values = subset.get(key)
                val superValues = superset.get(key)

                if (values::class.java != superValues::class.java) {
                    return false
                }
                if (values is JSONObject) {
                    if (!isSubsetJson(superValues as JSONObject, values)) {
                        return false
                    }
                } else if (values is JSONArray) {
                    val subSet = jsonArrayToStringSet(values)
                    val superSet = jsonArrayToStringSet(superValues as JSONArray)
                    if (!superSet.containsAll(subSet)) {
                        return false
                    }
                } else {
                    if (!values.equals(superValues)) {
                        return false
                    }
                }
            }
            return true
        }

        private fun jsonArrayToStringSet(values: JSONArray): Set<String> {
            val setValue = LinkedHashSet<String>()
            val len = values.length()
            for (i in 0 until len) {
                setValue.add(values[i].toString())
            }
            return setValue
        }

        /**
         * Generates a JSON for the create request flow that is maximally filled given the inputs,
         * so it can always be a valid 'subset' to the input (the 'superset'), which will contain
         * all expected outputs and more that may be parsed way by
         * [PublicKeyCredentialCreationOptions].
         */
        @Throws(JSONException::class)
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
                    descriptorI.put("id", b64Encode(descriptorJSON.id))
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
            json.put("challenge", b64Encode(requiredChallenge))
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
            userJson.put("id", b64Encode(userId))
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

        @JvmStatic
        val ConnectionResultFailureCases = arrayListOf(
            ConnectionResult.UNKNOWN, ConnectionResult.API_DISABLED, ConnectionResult.CANCELED,
            ConnectionResult.API_DISABLED_FOR_CONNECTION, ConnectionResult.API_UNAVAILABLE,
            ConnectionResult.DEVELOPER_ERROR, ConnectionResult.INTERNAL_ERROR,
            ConnectionResult.INTERRUPTED, ConnectionResult.INVALID_ACCOUNT,
            ConnectionResult.LICENSE_CHECK_FAILED, ConnectionResult.NETWORK_ERROR,
            ConnectionResult.RESOLUTION_ACTIVITY_NOT_FOUND, ConnectionResult.RESOLUTION_REQUIRED,
            ConnectionResult.RESTRICTED_PROFILE, ConnectionResult.SERVICE_DISABLED,
            ConnectionResult.SERVICE_INVALID, ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_MISSING_PERMISSION, ConnectionResult.SERVICE_UPDATING,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SIGN_IN_FAILED,
            ConnectionResult.SIGN_IN_REQUIRED, ConnectionResult.TIMEOUT)
    }
}