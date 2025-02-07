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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/** The helper class to resolve AppFunction related symbols. */
class AppFunctionSymbolResolver(private val resolver: Resolver) {

    /** Resolves valid functions annotated with @AppFunction annotation. */
    fun resolveAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
        return buildMap<KSClassDeclaration, MutableList<KSFunctionDeclaration>>() {
                val annotatedAppFunctions =
                    resolver.getSymbolsWithAnnotation(
                        AppFunctionAnnotation.CLASS_NAME.canonicalName
                    )
                for (symbol in annotatedAppFunctions) {
                    if (symbol !is KSFunctionDeclaration) {
                        throw ProcessingException(
                            "Only functions can be annotated with @AppFunction",
                            symbol
                        )
                    }
                    val functionClass = symbol.parentDeclaration as? KSClassDeclaration
                    if (functionClass == null) {
                        throw ProcessingException(
                            "Top level functions cannot be annotated with @AppFunction ",
                            symbol
                        )
                    }

                    this.getOrPut(functionClass) { mutableListOf<KSFunctionDeclaration>() }
                        .add(symbol)
                }
            }
            .map { (classDeclaration, appFunctionsDeclarations) ->
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations).validate()
            }
    }
}
