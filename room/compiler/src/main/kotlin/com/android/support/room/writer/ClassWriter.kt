/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.writer

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment

/**
 * Base class for all writers that can produce a class.
 */
abstract class ClassWriter(val className: ClassName) {

    abstract fun createTypeSpec(): TypeSpec

    fun write(processingEnv: ProcessingEnvironment) {
        JavaFile.builder(className.packageName(), createTypeSpec())
                .build()
                .writeTo(processingEnv.filer)
    }
}
