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

package androidx.credentials.registry.digitalcredentials.openid4vp

import android.graphics.Bitmap
import androidx.credentials.registry.digitalcredentials.mdoc.MdocEntry
import androidx.credentials.registry.digitalcredentials.mdoc.MdocInlineIssuanceEntry
import androidx.credentials.registry.digitalcredentials.openid4vp.OpenId4VpDefaults.DEFAULT_MATCHER
import androidx.credentials.registry.digitalcredentials.sdjwt.SdJwtEntry
import androidx.credentials.registry.digitalcredentials.sdjwt.SdJwtInlineIssuanceEntry
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialEntry
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry
import androidx.credentials.registry.provider.digitalcredentials.DisplayType
import androidx.credentials.registry.provider.digitalcredentials.EntryDisplayProperties
import androidx.credentials.registry.provider.digitalcredentials.FieldDisplayProperties
import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry
import androidx.credentials.registry.provider.digitalcredentials.VerificationEntryDisplayProperties
import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayProperties
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONArray
import org.json.JSONObject

/**
 * A registration request containing data that can serve the
 * [OpenID for Verifiable Presentations protocol](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)
 * based request.
 *
 * The ([type], [id]) properties together act as a primary key for this registry record stored with
 * the Registry Manager. You later can use the same values of [type] + [id] to overwrite or delete a
 * previously registered registry. Therefore, you should track the ids you use for various registry
 * use cases (most often you only need one) so that you can later use them to update the same
 * registry record.
 *
 * If both [credentialEntries] and [inlineIssuanceEntries], the registry will never be used to
 * answer a request.
 *
 * @param credentialEntries the list of entries to register
 * @param id the unique id for this registry
 * @param intentAction the intent action that will be used to launch your fulfillment activity when
 *   one of your credentials was chosen by the user, default to
 *   [RegistryManager.ACTION_GET_CREDENTIAL] when unspecified; when Credential Manager launches your
 *   fulfillment activity, it will build an intent with the given `intentAction` targeting your
 *   package, so this is useful when you need to define different fulfillment activities for
 *   different registries
 * @param inlineIssuanceEntries the list of inline issuance entries to add the user credentials on
 *   the fly, if applicable
 * @throws IllegalArgumentException if [id] or [intentAction] length is greater than 64 characters
 */
public class OpenId4VpRegistry
@JvmOverloads
public constructor(
    credentialEntries: List<DigitalCredentialEntry>,
    id: String,
    intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL,
    inlineIssuanceEntries: List<InlineIssuanceEntry> = emptyList(),
) :
    DigitalCredentialRegistry(
        id = id,
        credentials = toCredentialBytes(credentialEntries, inlineIssuanceEntries),
        matcher = DEFAULT_MATCHER,
        intentAction = intentAction,
    ) {

    private companion object {
        private const val CREDENTIALS = "credentials"
        private const val ID = "id"
        private const val TITLE = "title"
        private const val SUBTITLE = "subtitle"
        private const val ICON = "icon"
        private const val START = "start"
        private const val LENGTH = "length"
        private const val PATHS = "paths"
        private const val VALUE = "value"
        private const val DISPLAY = "display"
        private const val DISPLAY_VALUE = "display_value"
        private const val MAX_HEIGHT = 40
        private const val MAX_WIDTH = 52
        private const val VERIFICATION = "verification"
        private const val MSO_MDOC = "mso_mdoc"
        private const val DC_SD_JWT = "dc+sd-jwt"
        private const val ISSUANCE = "issuance"
        private const val SUPPORTED = "supported"

        private fun getIconBytes(icon: Bitmap): ByteArray {
            val currWidth = icon.width
            val currHeight = icon.height
            val scaledIcon =
                if (currHeight <= MAX_HEIGHT && currWidth <= MAX_WIDTH) {
                    icon
                } else {
                    val scaleRatio =
                        minOf(1.0 * MAX_WIDTH / currWidth, 1.0 * MAX_HEIGHT / currHeight)
                    val newHeight = (currHeight * scaleRatio).toInt()
                    val newWidth = (currWidth * scaleRatio).toInt()
                    Bitmap.createScaledBitmap(icon, newWidth, newHeight, true)
                }

            val stream = ByteArrayOutputStream()
            scaledIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }

        private class RegistryIcon(val iconValue: ByteArray, var iconOffset: Int = 0)

        private fun JSONObject.putDisplayAndIdForEntry(
            itemId: String,
            displays: Set<EntryDisplayProperties>,
            iconMap: Map<String, Map<@DisplayType Int, RegistryIcon>>,
        ) {
            put(ID, itemId)

            val displayJson = JSONObject()
            for (display in displays) {
                when (display) {
                    is VerificationEntryDisplayProperties -> {
                        val verificationDisplay = JSONObject()
                        verificationDisplay.put(TITLE, display.title)
                        verificationDisplay.putOpt(SUBTITLE, display.subtitle)
                        val icon =
                            iconMap.get(itemId)?.get(display.displayType)
                                ?: throw IllegalArgumentException(
                                    "Unexpected error during icon conversion"
                                )
                        val iconJson = JSONObject()
                        iconJson.put(LENGTH, icon.iconValue.size)
                        iconJson.put(START, icon.iconOffset)
                        verificationDisplay.put(ICON, iconJson)

                        displayJson.put(VERIFICATION, verificationDisplay)
                    }

                    else -> {} // Not supported
                }
            }

            put(DISPLAY, displayJson)
        }

        private fun JSONObject.putDisplayForField(displays: Set<FieldDisplayProperties>) {
            val displayJson = JSONObject()
            for (display in displays) {
                when (display) {
                    is VerificationFieldDisplayProperties -> {
                        val verificationDisplay = JSONObject()
                        verificationDisplay.put(DISPLAY, display.displayName)
                        verificationDisplay.putOpt(DISPLAY_VALUE, display.displayValue)
                        displayJson.put(VERIFICATION, verificationDisplay)
                    }

                    else -> {} // Not supported
                }
            }
            put(DISPLAY, displayJson)
        }

        private fun addClaimToPathJson(
            pathJson: JSONObject,
            targetPath: List<String>,
            claim: JSONObject,
        ) {
            var currPathJson = pathJson
            targetPath.forEachIndexed { idx, currPath ->
                if (idx == targetPath.size - 1) {
                    currPathJson.put(currPath, claim)
                } else {
                    val nextPathJson = currPathJson.optJSONObject(currPath)
                    if (nextPathJson == null) {
                        val newPathJson = JSONObject()
                        currPathJson.put(currPath, newPathJson)
                        currPathJson = newPathJson
                    } else {
                        currPathJson = nextPathJson
                    }
                }
            }
        }

        /**
         * Turn the credential entries into a structure understood by the default matcher.
         *
         * The OpenID4VP credential registry has the following format:
         *
         * |---------------------------------------| |--- (Int) offset of credential json ---|
         * |--------- (Byte Array) Icon 1 ---------| |--------- (Byte Array) Icon 2 ---------|
         * |------------- More Icons... -----------| |----------- Credential Json -----------|
         * |---------------------------------------|
         */
        private fun toCredentialBytes(
            credentialEntries: List<DigitalCredentialEntry>,
            inlineIssuanceEntries: List<InlineIssuanceEntry>,
        ): ByteArray {
            val out = ByteArrayOutputStream()

            // TODO: optimize for duplicates
            val iconMap: Map<String, Map<@DisplayType Int, RegistryIcon>> =
                credentialEntries.associate { entry ->
                    Pair(
                        entry.id,
                        entry.entryDisplayPropertySet
                            .mapNotNull { display ->
                                Pair(
                                    display.displayType,
                                    when (display) {
                                        is VerificationEntryDisplayProperties ->
                                            RegistryIcon(getIconBytes(display.icon))
                                        else -> {
                                            // Not supported
                                            return@mapNotNull null
                                        }
                                    },
                                )
                            }
                            .associate { it },
                    )
                }
            val inlineIssuanceIconMap: Map<String, RegistryIcon> =
                inlineIssuanceEntries
                    .mapNotNull { entry ->
                        entry.display.iconHint?.let {
                            Pair(entry.id, RegistryIcon(getIconBytes(it)))
                        }
                    }
                    .associate { it }

            // Write the offset to the json
            val jsonOffset =
                4 +
                    iconMap.values.sumOf {
                        it.values.sumOf { registryIcon -> registryIcon.iconValue.size }
                    } +
                    inlineIssuanceIconMap.values.sumOf { registryIcon ->
                        registryIcon.iconValue.size
                    }
            val buffer = ByteBuffer.allocate(4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(jsonOffset)
            out.write(buffer.array())

            // Write the icons
            var currIconOffset = 4
            iconMap.values.forEach {
                it.values.forEach { registryIcon ->
                    registryIcon.iconOffset = currIconOffset
                    out.write(registryIcon.iconValue)
                    currIconOffset += registryIcon.iconValue.size
                }
            }
            inlineIssuanceIconMap.values.forEach { registryIcon ->
                registryIcon.iconOffset = currIconOffset
                out.write(registryIcon.iconValue)
                currIconOffset += registryIcon.iconValue.size
            }

            val mdocCredentials = JSONObject()
            val sdJwtCredentials = JSONObject()
            credentialEntries.forEach { item ->
                when (item) {
                    is SdJwtEntry -> {
                        val credJson = JSONObject()
                        credJson.putDisplayAndIdForEntry(
                            item.id,
                            item.entryDisplayPropertySet,
                            iconMap,
                        )
                        val pathJson = JSONObject()
                        for (claim in item.claims) {
                            val claimJson = JSONObject()
                            claimJson.putDisplayForField(claim.fieldDisplayPropertySet)
                            claimJson.putOpt(VALUE, claim.value)
                            addClaimToPathJson(pathJson, claim.path, claimJson)
                        }
                        credJson.put(PATHS, pathJson)
                        addCredentialJson(sdJwtCredentials, credJson, item.verifiableCredentialType)
                    }
                    is MdocEntry -> {
                        val credJson = JSONObject()
                        credJson.putDisplayAndIdForEntry(
                            item.id,
                            item.entryDisplayPropertySet,
                            iconMap,
                        )
                        val pathJson = JSONObject()
                        for (field in item.fields) {
                            val namespaceJson =
                                pathJson.optJSONObject(field.namespace) ?: JSONObject()
                            val fieldJson = JSONObject()

                            fieldJson.putDisplayForField(field.fieldDisplayPropertySet)
                            fieldJson.putOpt(VALUE, field.fieldValue)
                            namespaceJson.put(field.identifier, fieldJson)
                            pathJson.put(field.namespace, namespaceJson)
                        }
                        credJson.put(PATHS, pathJson)
                        addCredentialJson(mdocCredentials, credJson, item.docType)
                    }

                    else -> {} // Not supported
                }
            }
            val inlineIssuanceCredentials = JSONObject()
            for (inlineIssuanceEntry in inlineIssuanceEntries) {
                when (inlineIssuanceEntry) {
                    is MdocInlineIssuanceEntry -> {
                        val entryJson = JSONObject()
                        entryJson.put(ID, inlineIssuanceEntry.id)
                        entryJson.putOpt(SUBTITLE, inlineIssuanceEntry.display.subtitle)
                        entryJson.putOpt(TITLE, inlineIssuanceEntry.display.titleHint)
                        val icon = inlineIssuanceIconMap[inlineIssuanceEntry.id]
                        icon?.let {
                            val iconJson = JSONObject()
                            iconJson.put(LENGTH, it.iconValue.size)
                            iconJson.put(START, it.iconOffset)
                            entryJson.put(ICON, iconJson)
                        }
                        val supportedDocTypes =
                            JSONArray(inlineIssuanceEntry.supportedMdocs.map { it.docType })
                        entryJson.putOpt(SUPPORTED, supportedDocTypes)
                        addCredentialJson(inlineIssuanceCredentials, entryJson, MSO_MDOC)
                    }
                    is SdJwtInlineIssuanceEntry -> {
                        val entryJson = JSONObject()
                        entryJson.put(ID, inlineIssuanceEntry.id)
                        entryJson.putOpt(SUBTITLE, inlineIssuanceEntry.display.subtitle)
                        entryJson.putOpt(TITLE, inlineIssuanceEntry.display.titleHint)
                        val icon = inlineIssuanceIconMap[inlineIssuanceEntry.id]
                        icon?.let {
                            val iconJson = JSONObject()
                            iconJson.put(LENGTH, it.iconValue.size)
                            iconJson.put(START, it.iconOffset)
                            entryJson.put(ICON, iconJson)
                        }
                        val supportedDocTypes =
                            JSONArray(
                                inlineIssuanceEntry.supportedSdJwts.map {
                                    it.verifiableCredentialType
                                }
                            )
                        entryJson.putOpt(SUPPORTED, supportedDocTypes)
                        addCredentialJson(inlineIssuanceCredentials, entryJson, DC_SD_JWT)
                    }
                    else -> {} // Not supported
                }
            }
            val registryCredentials = JSONObject()
            registryCredentials.put(MSO_MDOC, mdocCredentials)
            registryCredentials.put(DC_SD_JWT, sdJwtCredentials)
            registryCredentials.put(ISSUANCE, inlineIssuanceCredentials)
            val registryJson = JSONObject()
            registryJson.put(CREDENTIALS, registryCredentials)
            out.write(registryJson.toString().toByteArray())
            return out.toByteArray()
        }

        private fun addCredentialJson(
            collection: JSONObject,
            newCredJson: JSONObject,
            key: String,
        ) {
            when (val current = collection.opt(key) ?: JSONArray()) {
                is JSONArray -> {
                    collection.put(key, current.put(newCredJson))
                }
                else -> {} // Not expected
            }
        }
    }
}
