/*
 * Copyright 2019 The Android Open Source Project
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

package sample.optin

import androidx.annotation.OptIn

@Suppress("unused", "MemberVisibilityCanBePrivate")
class UseJavaExperimentalFromKt {

    /**
     * Unsafe call into a field on an experimental class.
     */
    fun unsafeExperimentalClassField(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.field
    }

    /**
     * Unsafe call into a method on an experimental class.
     */
    fun unsafeExperimentalClassMethod(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method()
    }

    /**
     * Unsafe call into a static field on an experimental class.
     */
    fun unsafeExperimentalClassStaticField(): Int {
        return AnnotatedJavaClass.FIELD_STATIC
    }

    /**
     * Unsafe call into a static method on an experimental class.
     */
    fun unsafeExperimentalClassStaticMethod(): Int {
        return AnnotatedJavaClass.methodStatic()
    }

    /**
     * Safe call due to propagation of experimental annotation.
     */
    @ExperimentalJavaAnnotation
    fun safePropagateMarker(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method()
    }

    /**
     * Safe call due to opting in to experimental annotation.
     */
    @OptIn(ExperimentalJavaAnnotation::class)
    fun safeOptInMarker(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method()
    }

    /**
     * Unsafe call into an experimental field on a stable class.
     */
    fun unsafeExperimentalField(): Int {
        val stableObject = AnnotatedJavaMembers()
        return stableObject.field
    }

    /**
     * Unsafe call into an experimental method on a stable class.
     */
    fun unsafeExperimentalMethod(): Int {
        val stableObject = AnnotatedJavaMembers()
        return stableObject.method()
    }

    /**
     * Unsafe call into an experimental static field on a stable class.
     */
    fun unsafeExperimentalStaticField(): Int {
        return AnnotatedJavaMembers.FIELD_STATIC
    }

    /**
     * Unsafe call into an experimental static method on a stable class.
     */
    fun unsafeExperimentalStaticMethod(): Int {
        return AnnotatedJavaMembers.methodStatic()
    }

    /**
     * Unsafe call into multiple experimental classes.
     */
    @ExperimentalJavaAnnotation
    fun unsafeMultipleExperimentalClasses(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method() + AnnotatedJavaClass2.FIELD_STATIC
    }

    /**
     * Safe call due to propagation of both annotations.
     */
    @ExperimentalJavaAnnotation
    @ExperimentalJavaAnnotation2
    fun safePropagateMultipleMarkers(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method() + AnnotatedJavaClass2.FIELD_STATIC
    }

    /**
     * Safe call due to opt-in of one annotation and propagation of another.
     */
    @OptIn(ExperimentalJavaAnnotation::class)
    @ExperimentalJavaAnnotation2
    fun safePropagateAndOptInMarkers(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method() + AnnotatedJavaClass2.FIELD_STATIC
    }

    /**
     * Safe call due to opt-in of both annotations.
     */
    @OptIn(ExperimentalJavaAnnotation::class, ExperimentalJavaAnnotation2::class)
    fun safeOptInMultipleMarkers(): Int {
        val experimentalObject = AnnotatedJavaClass()
        return experimentalObject.method() + AnnotatedJavaClass2.FIELD_STATIC
    }
}
