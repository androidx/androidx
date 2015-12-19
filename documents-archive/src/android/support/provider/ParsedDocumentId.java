/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.provider;

/**
 * @hide
 */
class ParsedDocumentId {
    public final String mArchiveId;
    public final String mPath;

    public ParsedDocumentId(String archiveId, String path) {
        mArchiveId = archiveId;
        mPath = path;
    }

    static public ParsedDocumentId fromDocumentId(String documentId, char idDelimiter) {
        final int delimiterPosition = documentId.indexOf(idDelimiter);
        if (delimiterPosition == -1) {
            return new ParsedDocumentId(documentId, null);
        } else {
            return new ParsedDocumentId(documentId.substring(0, delimiterPosition),
                    documentId.substring((delimiterPosition + 1)));
        }
    }

    static public boolean hasPath(String documentId, char idDelimiter) {
        return documentId.indexOf(idDelimiter) != -1;
    }

    public String toDocumentId(char idDelimiter) {
        if (mPath == null) {
            return mArchiveId;
        } else {
            return mArchiveId + idDelimiter + mPath;
        }
    }
};
