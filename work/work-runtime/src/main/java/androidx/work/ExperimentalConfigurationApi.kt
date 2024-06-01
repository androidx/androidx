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

package androidx.work

/**
 * Annotation indicating experimental API for new WorkManager's Configuration APIs.
 *
 * These APIs allow fine grained tuning WorkManager's behavior. However, full effects of these flags
 * on OS health and WorkManager's throughput aren't fully known and currently are being explored.
 * After the research either the best default value for a flag will be chosen and then associated
 * API will be removed or the guidance on how to choose a value depending on app's specifics will
 * developed and then associated API will be promoted to stable.
 *
 * As a result these APIs annotated with `ExperimentalConfigurationApi` requires opt-in
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.CLASS
)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalConfigurationApi
