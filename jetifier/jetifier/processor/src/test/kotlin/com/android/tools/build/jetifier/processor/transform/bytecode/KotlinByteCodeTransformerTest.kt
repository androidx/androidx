/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.KotlinTestConfig
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class KotlinByteCodeTransformerTest {
    @Test
    fun propertyTest() {
        val testClass = transformAndLoadClass("TestProperty")
        val testInstance = testClass.primaryConstructor!!.call()

        assertThat(testClass.memberProperties).hasSize(1)
        val property = testClass.memberProperties.first()
        // this call will fail if metadata for this property is incorrect
        property.call(testInstance)
    }

    @Test
    fun dataClassTest() {
        val testClass = transformAndLoadClass("TestDataClass")
        // this call will fail if metadata for this data class is incorrect
        testClass.primaryConstructor!!.call("a", null)
    }
}

private fun transformAndLoadClass(className: String): KClass<*> {
    val inputClassPath = "/androidx/fake/lib/$className.class"
    val inputFile = File(KotlinByteCodeTransformerTest::class.java.getResource(inputClassPath).file)
    val archiveFile = ArchiveFile(
        Paths.get("androidx/fake/lib", "$className.class"),
        inputFile.readBytes()
    )

    val context = TransformationContext(config = KotlinTestConfig)
    val transformer = ByteCodeTransformer(context)
    transformer.runTransform(archiveFile)
    val bytes = archiveFile.data

    val classLoader = object : URLClassLoader(emptyArray()) {
        override fun findClass(name: String): Class<*> =
            if (name == "android.old.fake.$className") defineClass(name, bytes, 0, bytes.size)
            else super.findClass(name)
    }
    return classLoader.loadClass("android.old.fake.$className").kotlin
}
