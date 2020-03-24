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

package androidx.serialization

import androidx.serialization.compiler.codegen.java.JavaGenerator
import androidx.serialization.compiler.processing.steps.EnumProcessingStep
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion

/**
 * Parses and validates Serialization schema and generates message serializers and service binders.
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class SerializationProcessor : BasicAnnotationProcessor() {
    override fun initSteps(): List<ProcessingStep> {
        val javaGenerator = JavaGenerator(
            processingEnv,
            "androidx.serialization.SerializationProcessor")

        return listOf(
            EnumProcessingStep(processingEnv, javaGenerator)
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()
}
