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

package androidx.compose.foundation.text.test

import androidx.compose.ui.unit.IntRect
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

internal fun assertThatIntRect(rect: IntRect?): IntRectSubject {
    return assertAbout(IntRectSubject.SUBJECT_FACTORY).that(rect)!!
}

internal class IntRectSubject
private constructor(failureMetadata: FailureMetadata?, private val subject: IntRect?) :
    Subject(failureMetadata, subject) {
    companion object {
        internal val SUBJECT_FACTORY: Factory<IntRectSubject?, IntRect?> =
            Factory { failureMetadata, subject ->
                IntRectSubject(failureMetadata, subject)
            }
    }

    fun isEqualTo(left: Int, top: Int, right: Int, bottom: Int) {
        if (subject == null) failWithoutActual(simpleFact("is null"))
        check("instanceOf()").that(subject!!).isInstanceOf(IntRect::class.java)
        check("left").that(subject.left).isEqualTo(left)
        check("top").that(subject.top).isEqualTo(top)
        check("right").that(subject.right).isEqualTo(right)
        check("bottom").that(subject.bottom).isEqualTo(bottom)
    }
}
