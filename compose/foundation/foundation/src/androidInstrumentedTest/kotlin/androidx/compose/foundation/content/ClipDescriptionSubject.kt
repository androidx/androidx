/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.content

import android.content.ClipDescription
import android.os.Build
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth

/**
 * Truth extension for ClipDescription.
 */
internal class ClipDescriptionSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: ClipDescription?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<ClipDescriptionSubject?, ClipDescription?> =
            Factory { failureMetadata, subject -> ClipDescriptionSubject(failureMetadata, subject) }
    }

    /**
     * Checks the equality of two [ClipDescription]s.
     *
     * @param clipDescription the [ClipDescription] to be matched.
     */
    fun isEqualToClipDescription(clipDescription: ClipDescription) {
        if (subject === clipDescription) return
        check("isNotNull()").that(subject).isNotNull()
        check("getMimeTypeCount()").that(subject!!.mimeTypeCount)
            .isEqualTo(clipDescription.mimeTypeCount)
        for (i in 0 until subject.mimeTypeCount) {
            check("getMimeType($i)").that(subject.getMimeType(i))
                .isEqualTo(clipDescription.getMimeType(i))
        }
        if (Build.VERSION.SDK_INT >= 24) {
            check("getExtras()").that(subject.extras).isEqualTo(clipDescription.extras)
        }
        check("getLabel()").that(subject.label).isEqualTo(clipDescription.label)
        if (Build.VERSION.SDK_INT >= 31) {
            check("isStyledText()").that(subject.isStyledText)
                .isEqualTo(clipDescription.isStyledText)
            check("getClassificationStatus()").that(subject.classificationStatus)
                .isEqualTo(clipDescription.classificationStatus)
        }
    }
}

internal fun assertClipDescription(clipDescription: ClipDescription?): ClipDescriptionSubject {
    return Truth.assertAbout(ClipDescriptionSubject.SUBJECT_FACTORY).that(clipDescription)!!
}
