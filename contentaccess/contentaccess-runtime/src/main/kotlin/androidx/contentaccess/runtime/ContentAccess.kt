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

package androidx.contentaccess

import android.content.ContentResolver
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import androidx.arch.core.executor.ArchTaskExecutor
import java.util.concurrent.Executor

public class ContentAccess {
    public companion object {
        @Suppress("UNCHECKED_CAST")
        public fun <T : Any> getAccessor(
            contentAccessObject: KClass<T>,
            contentResolver: ContentResolver,
            queryExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()
        ): T {
            val packageName = contentAccessObject.java.`package`!!.name
            // We do this instead of getting the simple name of the class in case of nested classes.
            val generatedClassName = "${contentAccessObject.qualifiedName!!
                .removePrefix(packageName).replace(".", "_")}Impl"
            try {
                val cl = Class.forName("$packageName.$generatedClassName")
                val constructor = cl.getConstructor(
                    ContentResolver::class.java,
                    CoroutineDispatcher::class.java
                )
                return constructor.newInstance(
                    contentResolver,
                    queryExecutor
                        .asCoroutineDispatcher()
                ) as T
            } catch (e: ClassNotFoundException) {
                error(
                    "Cannot find generated class for content accessor ${contentAccessObject
                        .qualifiedName}, this is most likely because the class is not annotated " +
                        "with @ContentAccessObject or because the contentaccess-compiler " +
                        "annotation processor was not ran properly."
                )
            } catch (e: InstantiationException) {
                error(
                    "Unable to instantiate implementation $packageName.$generatedClassName of " +
                        "${contentAccessObject.qualifiedName}."
                )
            }
        }
    }
}
