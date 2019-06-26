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

package androidx.annotation

/**
 * Denotes that the annotated function is considered a sample function, and is linked to from the
 * KDoc of a source module.
 *
 * There are corresponding lint checks ensuring that functions referred to from KDoc with a @sample
 * tag are annotated with this annotation, and also to ensure that any functions annotated with this
 * annotation are linked to from a @sample tag.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Sampled
