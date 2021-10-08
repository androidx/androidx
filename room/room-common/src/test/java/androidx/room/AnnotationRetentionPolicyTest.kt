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

package androidx.room

import com.google.common.reflect.ClassPath
import org.junit.Test
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** Test to make sure annotations have the correct retention policy. */
class AnnotationRetentionPolicyTest {

    @Test
    fun ensureAnnotationsHaveClassRetentionPolicy() {
        val annotations: List<Class<*>> =
            ClassPath.from(Database::class.java.classLoader)
                .allClasses
                .filter { it.name.startsWith("androidx.room") }
                .map { it.load() }
                .filter { it.isAnnotation }

        // For Room to be incremental, all annotations need to have CLASS retention policy.
        annotations.forEach {
            val retentionPolicy = it.getAnnotation(Retention::class.java)?.value
            assert(retentionPolicy == RetentionPolicy.CLASS) {
                "Expected ${it.name} annotation to have retention policy CLASS" +
                    " but it is $retentionPolicy"
            }
        }
    }
}