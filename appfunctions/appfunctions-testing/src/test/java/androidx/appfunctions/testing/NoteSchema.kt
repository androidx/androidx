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

package androidx.appfunctions.testing

import android.net.Uri
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializableInterface

/** Creates an [AppFunctionNote] with the given parameters. */
@AppFunctionSchemaDefinition(name = "createNote", version = 2, category = "myNotes")
public interface CreateNoteAppFunction<
    Parameters : CreateNoteAppFunction.Parameters,
    Response : CreateNoteAppFunction.Response,
> {
    /**
     * Creates an [AppFunctionNote] with the given parameters.
     *
     * The implementing app should throw an appropriate subclass of
     * [androidx.appfunctions.AppFunctionException] in exceptional cases.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param parameters The parameters for creating a note.
     * @return The response including the created note.
     */
    public suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
    ): Response

    /** The parameters for creating a note. */
    @AppFunctionSerializableInterface
    public interface Parameters {
        /** The title of the note. */
        public val title: String

        /** The text content of the note. */
        public val content: String?
            get() = null

        /** The attachments of the note. */
        public val attachments: List<AppFunctionNote.Attachment>
            get() = emptyList()

        /**
         * The ID of the group the note is in, if any else `null`.
         *
         * [androidx.appfunctions.AppFunctionElementNotFoundException] should be thrown when a group
         * with the specified groupId doesn't exist.
         */
        public val groupId: String?
            get() = null

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
        public val externalUuid: String?
            get() = null
    }

    /** The response including the created note. */
    @AppFunctionSerializableInterface
    public interface Response {
        /** The created note. */
        public val createdNote: AppFunctionNote
    }
}

/** A note entity. */
@AppFunctionSerializableInterface
public interface AppFunctionNote {
    /** The ID of the note. */
    public val id: String

    /** The title of the note. */
    public val title: String

    /** The content of the note. */
    public val content: String?
        get() = null

    /** The attachments of the note. */
    public val attachments: List<Attachment>
        get() = emptyList()

    /** An attached file. */
    @AppFunctionSerializableInterface
    public interface Attachment {
        /**
         * The URI of the attached file.
         *
         * When providing an [Uri] to another app, that app must be granted URI permission using
         * [android.content.Context.grantUriPermission] to the receiving app.
         *
         * The providing app should also consider revoking the URI permission by using
         * [android.content.Context.revokeUriPermission] after a certain time period.
         */
        public val uri: Uri

        /** The display name of the attached file. */
        public val displayName: String

        /** The MIME type of the attached file. Format defined in RFC 6838. */
        public val mimeType: String?
            get() = null
    }
}
