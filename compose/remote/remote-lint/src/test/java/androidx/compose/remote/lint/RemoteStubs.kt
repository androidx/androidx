/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin

object RemoteStubs {
    val RemoteComposable =
        kotlin(
            """
    package androidx.compose.remote.creation.compose.layout

    @Retention(AnnotationRetention.BINARY)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.TYPE,
        AnnotationTarget.TYPE_PARAMETER,
    )
    annotation class RemoteComposable
    """
        )

    val RemoteModifier =
        kotlin(
            """
    package androidx.compose.remote.creation.compose.modifier

    interface RemoteModifier {
        infix fun then(other: RemoteModifier): RemoteModifier =
            if (other === RemoteModifier) this else CombinedRemoteModifier(this, other)

        interface Element : RemoteModifier

        companion object : RemoteModifier {
            override infix fun then(other: RemoteModifier): RemoteModifier = other
        }
    }

    class CombinedRemoteModifier(
        private val outer: RemoteModifier,
        private val inner: RemoteModifier
    ) : RemoteModifier
    """
        )
}
