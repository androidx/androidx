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

package androidx.appfunctions.integration.tests

import android.net.Uri
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializable
import java.util.Objects

const val APP_FUNCTION_SCHEMA_CATEGORY_NOTES = "notes"

const val APP_FUNCTION_SCHEMA_VERSION_NOTES: Int = 1

/** Creates a [Note]. */
@AppFunctionSchemaDefinition(
    name = "createNote",
    version = APP_FUNCTION_SCHEMA_VERSION_NOTES,
    category = APP_FUNCTION_SCHEMA_CATEGORY_NOTES,
)
interface CreateNote {
    /**
     * Creates a [Note].
     *
     * The implementing app should throw an appropriate subclass of
     * [androidx.appfunctions.AppFunctionException] in exceptional cases.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param createNoteParams The parameters for creating a note.
     * @return The created note.
     */
    suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        createNoteParams: LegacyCreateNoteParams,
    ): LegacyNote
}

/** A note entity. */
@AppFunctionSerializable(isDescribedByKdoc = true)
class LegacyNote(
    /** The ID of the note. */
    val id: String,
    /** The title of the note. */
    val title: String,
    /** The content of the note. */
    val content: String? = null,
    /** The attachments of the note. */
    val attachments: List<LegacyAttachment> = emptyList(),
    /** The ID of the folder the note is in, if any. */
    val folderId: String? = null,
) {
    override fun equals(other: Any?) =
        other is LegacyNote &&
            id == other.id &&
            title == other.title &&
            content == other.content &&
            attachments == other.attachments &&
            folderId == other.folderId

    override fun hashCode() = Objects.hash(id, title, content, attachments, folderId)
}

/** A file attached to the note. */
@AppFunctionSerializable(isDescribedByKdoc = true)
class LegacyAttachment(
    /** The display name of the attachment. */
    val displayName: String,
    /** The MIME type of the attachment. Format defined in RFC 6838. */
    val mimeType: String? = null,
    /** The URI of the attachment. */
    val uri: Uri,
) {
    override fun equals(other: Any?) =
        other is LegacyAttachment &&
            displayName == other.displayName &&
            mimeType == other.mimeType &&
            uri == other.uri

    override fun hashCode() = Objects.hash(displayName, mimeType, uri)
}

/** The parameters for creating a note. */
@AppFunctionSerializable(isDescribedByKdoc = true)
class LegacyCreateNoteParams(
    /** The title of the note. */
    val title: String,
    /** The content of the note. */
    val content: String? = null,

    /** The attachments of the note. */
    val attachments: List<LegacyAttachment> = emptyList(),

    /**
     * The ID of the folder the note is in, if any.
     *
     * [androidx.appfunctions.AppFunctionElementNotFoundException] should be thrown when a folder
     * with the specified folderId doesn't exist.
     */
    val folderId: String? = null,

    /**
     * An optional UUID for this note provided by the caller. If provided, the caller can use this
     * UUID as well as the returned [LegacyNote.id] to reference this specific note in subsequent
     * requests, such as a request to update the note that was just created.
     *
     * To support [externalId], the application should maintain a mapping between the [externalId]
     * and the internal id of this note. This allows the application to retrieve the correct note
     * when the caller references it using the provided `externalId` in subsequent requests.
     *
     * If the `externalId` is not provided by the caller in the creation request, the application
     * should expect subsequent requests from the caller to reference the note using the application
     * generated [LegacyNote.id].
     */
    val externalId: String? = null,
) {
    override fun equals(other: Any?) =
        other is LegacyCreateNoteParams &&
            title == other.title &&
            content == other.content &&
            attachments == other.attachments &&
            folderId == other.folderId &&
            externalId == other.externalId

    override fun hashCode() = Objects.hash(title, content, attachments, folderId, externalId)
}
