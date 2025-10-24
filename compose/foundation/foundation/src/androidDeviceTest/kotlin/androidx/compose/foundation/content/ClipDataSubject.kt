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

import android.content.ClipData
import android.os.Build
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth

/** Truth extension for ClipData. */
internal class ClipDataSubject
private constructor(failureMetadata: FailureMetadata?, private val subject: ClipData?) :
    Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<ClipDataSubject?, ClipData?> =
            Factory { failureMetadata, subject ->
                ClipDataSubject(failureMetadata, subject)
            }
    }

    /**
     * Checks the equality of two [ClipData]s.
     *
     * @param clipData the [ClipData] to be matched.
     */
    fun isEqualToClipData(clipData: ClipData, ignoreClipDescription: Boolean = false) {
        if (subject === clipData) return
        check("isNotNull()").that(subject).isNotNull()
        check("getItemCount()").that(subject!!.itemCount).isEqualTo(clipData.itemCount)
        for (i in 0 until subject.itemCount) {
            check("getItemAt($i).getUri()")
                .that(subject.getItemAt(i).uri)
                .isEqualTo(clipData.getItemAt(i).uri)
            check("getItemAt($i).getText()")
                .that(subject.getItemAt(i).text)
                .isEqualTo(clipData.getItemAt(i).text)
            check("getItemAt($i).getHtmlText()")
                .that(subject.getItemAt(i).htmlText)
                .isEqualTo(clipData.getItemAt(i).htmlText)
            check("getItemAt($i).getIntent()")
                .that(subject.getItemAt(i).intent)
                .isEqualTo(clipData.getItemAt(i).intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                check("getItemAt($i).getTextLinks()")
                    .that(subject.getItemAt(i).textLinks)
                    .isEqualTo(clipData.getItemAt(i).textLinks)
            }
        }
        if (!ignoreClipDescription) {
            assertClipDescription(subject.description)
                .isEqualToClipDescription(clipData.description)
        }
    }
}

internal fun assertClipData(clipData: ClipData): ClipDataSubject {
    return Truth.assertAbout(ClipDataSubject.SUBJECT_FACTORY).that(clipData)!!
}
