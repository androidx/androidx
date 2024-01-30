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

package androidx.hilt

import androidx.hilt.work.WorkerStep
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import com.google.auto.service.AutoService
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING

/**
 * Annotation processor for the various AndroidX Hilt extensions.
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class AndroidXHiltProcessor : JavacBasicAnnotationProcessor(
    config = WorkerStep.ENV_CONFIG
) {

    override fun processingSteps() = listOf(WorkerStep())

    override fun getSupportedSourceVersion() = SourceVersion.latest()
}
