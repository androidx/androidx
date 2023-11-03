/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.annotation

/**
 * Denotes that the annotated element requires one or more features. This is used to auto-generate
 * documentation, and more importantly: to ensure correct usage in application code, where lint and
 * Android Studio can check that calls marked with this annotation is surrounded by has-feature
 * calls, referenced via the [RequiresFeature.enforcement] attribute.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
)
public annotation class RequiresFeature(
    /** The name of the feature that is required. */
    val name: String,
    /**
     * Defines the name of the method that should be called to check whether the feature is
     * available, using the same signature format as javadoc. The feature checking method can have
     * multiple parameters, but the feature name parameter must be of type String and must also be
     * the first String-type parameter.
     */
    val enforcement: String
)
