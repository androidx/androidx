/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room3

/**
 * Marks a [androidx.room3.migration.AutoMigrationSpec] class that will be provided to Room at
 * runtime.
 *
 * An instance of a class annotated with this annotation has to be provided to the Room database
 * using [androidx.room3.RoomDatabase.Builder.addAutoMigrationSpec]. Room will verify that the spec
 * is provided in the builder and if not, it will throw an exception.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ProvidedAutoMigrationSpec
