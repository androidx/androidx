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

package androidx.navigation.runtime.lint

import androidx.navigation.lint.common.BaseTypeSafeDestinationMissingAnnotationDetector
import androidx.navigation.lint.common.createMissingKeepAnnotationIssue
import androidx.navigation.lint.common.createMissingSerializableAnnotationIssue
import com.android.tools.lint.detector.api.Issue

/**
 * Checks for missing annotations on type-safe route declarations
 *
 * Retrieves route classes/objects by tracing KClasses passed as route during NavDestination
 * creation
 */
class TypeSafeDestinationMissingAnnotationDetector :
    BaseTypeSafeDestinationMissingAnnotationDetector(
        methodNames = listOf("activity"),
        constructorNames = listOf("androidx.navigation.ActivityNavigatorDestinationBuilder")
    ) {

    companion object {
        val MissingSerializableAnnotationIssue =
            createMissingSerializableAnnotationIssue(
                TypeSafeDestinationMissingAnnotationDetector::class.java
            )
        val MissingKeepAnnotationIssue =
            createMissingKeepAnnotationIssue(
                TypeSafeDestinationMissingAnnotationDetector::class.java
            )
    }

    override fun getMissingSerializableIssue(): Issue = MissingSerializableAnnotationIssue

    override fun getMissingKeepIssue(): Issue = MissingKeepAnnotationIssue
}
