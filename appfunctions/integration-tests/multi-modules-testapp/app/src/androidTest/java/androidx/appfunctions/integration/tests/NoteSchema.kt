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

/** Creates an [AppFunctionNote] with the given parameters. */
@AppFunctionSchemaDefinition(name = "createNote", version = 2, category = "myNotes")
public interface CreateNoteAppFunction {
    /**
     * Creates an [AppFunctionNote] with the given parameters.
     *
     * The implementing app should throw an appropriate subclass of
     * [androidx.appfunctions.AppFunctionException] in exceptional cases.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param parameters The parameters for creating a note.
     * @param tag Optional tag.
     * @return The response including the created note.
     */
    public suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
        tag: String? = null,
    ): Response

    /** The parameters for creating a note. */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    public data class Parameters(
        /** The title of the note. */
        val title: String,

        /** The text content of the note. */
        val content: String? = null,

        /** The attachments of the note. */
        val attachments: List<AppFunctionNote.Attachment> = emptyList(),

        /**
         * The ID of the group the note is in, if any else `null`.
         *
         * [androidx.appfunctions.AppFunctionElementNotFoundException] should be thrown when a group
         * with the specified groupId doesn't exist.
         */
        val groupId: String? = null,

        /**
         * An optional UUID for this note provided by the caller. If provided, the caller can use
         * this UUID as well as the returned [AppFunctionNote.id] to reference this specific note in
         * subsequent requests, such as a request to update the note that was just created.
         *
         * To support [externalUuid], the application should maintain a mapping between the
         * [externalUuid] and the internal id of this note. This allows the application to retrieve
         * the correct note when the caller references it using the provided `externalUuid` in
         * subsequent requests.
         *
         * If the `externalUuid` is not provided by the caller in the creation request, the
         * application should expect subsequent requests from the caller to reference the note using
         * the application generated [AppFunctionNote.id].
         */
        val externalUuid: String? = null,
    )

    /** The response including the created note. */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    public data class Response(
        /** The created note. */
        public val createdNote: AppFunctionNote,
        /** Optional tag. */
        public val tag: String? = null,
    )
}

/** A note entity. */
@AppFunctionSerializable(isDescribedByKdoc = true)
public data class AppFunctionNote(
    /** The ID of the note. */
    val id: String,

    /** The title of the note. */
    val title: String,

    /** The content of the note. */
    val content: String? = null,

    /** The attachments of the note. */
    val attachments: List<Attachment> = emptyList(),
) {
    /** An attached file. */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class Attachment(
        /**
         * The URI of the attached file.
         *
         * When providing an [Uri] to another app, that app must be granted URI permission using
         * [android.content.Context.grantUriPermission] to the receiving app.
         *
         * The providing app should also consider revoking the URI permission by using
         * [android.content.Context.revokeUriPermission] after a certain time period.
         */
        val uri: Uri,

        /** The display name of the attached file. */
        val displayName: String,

        /** The MIME type of the attached file. Format defined in RFC 6838. */
        val mimeType: String? = null,
    )
}
