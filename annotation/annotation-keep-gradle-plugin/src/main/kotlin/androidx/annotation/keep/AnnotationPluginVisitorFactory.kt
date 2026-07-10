/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.annotation.keep

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

abstract class AnnotationPluginVisitorFactory : AsmClassVisitorFactory<AnnotationPluginParameters> {
    private val annotationPluginParameters: AnnotationPluginParameters
        get() = this.parameters.get()

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor {
        // At this point we should have already created a file that can hold the keep rules.
        val keepRulesPath = annotationPluginParameters.keepRules
        // R8 will provide a ClassVisitor that we can eventually use.
        /*
        return KeepAnno.createClassVisitorForKeepRulesExtraction { rule ->
            keepRulesPath.appendText(rule)
        }
        */
        return nextClassVisitor
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true // Scan all classes for annotations given class fields can be annotated.
    }
}
