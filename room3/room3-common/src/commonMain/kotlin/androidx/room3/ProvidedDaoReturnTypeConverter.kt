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

package androidx.room3

/**
 * Marks a class containing [DaoReturnTypeConverter] functions that will be provided to Room at
 * runtime.
 *
 * An instance of a class annotated with this annotation has to be provided to the Room database
 * using [androidx.room3.RoomDatabase.Builder.addDaoReturnTypeConverter]. If Room uses the annotated
 * DAO return type converter class, it will verify that it is provided in the builder and if not, it
 * will throw an exception.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ProvidedDaoReturnTypeConverter
