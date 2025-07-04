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

package androidx.appfunctions.compiler

import androidx.appfunctions.compiler.core.ProcessingException

/** Represents the compiler option for [AppFunctionCompiler] */
class AppFunctionCompilerOptions
private constructor(
    /** Indicates whether aggregation should run or not. */
    val aggregateAppFunctions: Boolean,
    /**
     * Indicates whether the compiler should generate metadata from schema to support legacy indexer
     * or not.
     */
    val generateMetadataFromSchema: Boolean,
) {
    companion object {
        private const val AGGREGATE_APP_FUNCTIONS_OPTION_KEY = "appfunctions:aggregateAppFunctions"

        private const val SUPPORT_LEGACY_INDEXER_OPTION_KEY =
            "appfunctions:generateMetadataFromSchema"

        fun from(options: Map<String, String>): AppFunctionCompilerOptions {
            return AppFunctionCompilerOptions(
                aggregateAppFunctions = getAggregateAppFunctionsOption(options),
                generateMetadataFromSchema = getGenerateMetadataFromSchemaOption(options),
            )
        }

        private fun getAggregateAppFunctionsOption(options: Map<String, String>): Boolean {
            return try {
                options[AGGREGATE_APP_FUNCTIONS_OPTION_KEY]?.toBooleanStrict() ?: false
            } catch (e: Exception) {
                throw ProcessingException(
                    message =
                        "Compiler option appfunctions:aggregateAppFunctions should be either " +
                            "`true` or `false`",
                    symbol = null,
                    throwable = e,
                )
            }
        }

        private fun getGenerateMetadataFromSchemaOption(options: Map<String, String>): Boolean {
            return try {
                options[SUPPORT_LEGACY_INDEXER_OPTION_KEY]?.toBooleanStrict() ?: false
            } catch (e: Exception) {
                throw ProcessingException(
                    message =
                        "Compiler option appfunctions:generateMetadataFromSchema should be either " +
                            "`true` or `false`",
                    symbol = null,
                    throwable = e,
                )
            }
        }
    }
}
