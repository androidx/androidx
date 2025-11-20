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

package androidx.appfunctions

import androidx.appsearch.annotation.Document

@Document(name = "com.google.android.appfunctions.schema.common.v1.notes.CreateNoteParams")
data class LegacyCreateNoteParams(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The title of the note. */
    @Document.StringProperty(required = true) val title: String,
    /** The content of the note. */
    @Document.StringProperty val content: String? = null,
    /** The attachments of the note. */
    @Document.DocumentProperty val attachments: List<LegacyAttachment> = emptyList(),
    /** The ID of the folder the note is in, if any. */
    @Document.StringProperty val folderId: String? = null,
    /**
     * The ID the agent will use to refer to this note in all subsequent requests. If provided, the
     * app must use this ID (eg. by maintaining a mapping between its internal ID and this external
     * ID).
     */
    @Document.StringProperty val externalId: String? = null,
)

@Document(name = "com.google.android.appfunctions.schema.common.v1.notes.Note")
data class LegacyNote(
    @Document.Namespace val namespace: String = "", // unused
    /** The ID of the note. */
    @Document.Id val id: String,
    /** The title of the note. */
    @Document.StringProperty(required = true) val title: String,
    /** The content of the note. */
    @Document.StringProperty val content: String? = null,
    /** The attachments of the note. */
    @Document.DocumentProperty val attachments: List<LegacyAttachment> = emptyList(),
    /** The ID of the folder the note is in, if any. */
    @Document.StringProperty val folderId: String? = null,
)

/**
 * The parameters for finding notes. A null value means that there is no restriction on the given
 * field. If all fields are null, it returns all the notes (up to [maxCount]).
 */
@Document(name = "com.google.android.appfunctions.schema.common.v1.notes.FindNotesParams")
data class LegacyFindNotesParams(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The query to search for notes. */
    @Document.StringProperty val query: String? = null,
    /** The start date (inclusive) of the notes to find. */
    @Document.DocumentProperty val startDate: LegacyDateTime? = null,
    /** The end date (exclusive) of the notes to find. */
    @Document.DocumentProperty val endDate: LegacyDateTime? = null,
    /** The maximum number of notes to return. */
    @Document.LongProperty(required = true) val maxCount: Int,
)

/** The parameters for updating a note. */
@Document(name = "com.google.android.appfunctions.schema.common.v1.notes.UpdateNoteParams")
data class LegacyUpdateNoteParams(
    @Document.Namespace val namespace: String = "", // unused
    @Document.Id val id: String = "", // unused
    /** The ID of the note to update. */
    @Document.StringProperty(required = true) val noteId: String,
    /** The title of the note. */
    @Document.DocumentProperty val title: LegacySetStringField? = null,
    /** The content of the note. */
    @Document.DocumentProperty val content: LegacySetStringNullableField? = null,
    /** The attachments of the note. */
    @Document.DocumentProperty val attachments: LegacySetAttachmentListField? = null,
    /** The ID of the folder the note is in, if any. */
    @Document.DocumentProperty val folderId: LegacySetStringNullableField? = null,
)
