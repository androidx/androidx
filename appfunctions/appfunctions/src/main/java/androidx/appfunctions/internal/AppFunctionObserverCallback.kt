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

package androidx.appfunctions.internal

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.ObserveAppFunctionsEvent
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_NAMESPACE
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_NAMESPACE
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_STATIC_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.OBSERVER_DEBOUNCE_MILLIS
import androidx.appfunctions.internal.ChangeEventsFlowUtils.debounceAndMerge
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appsearch.observer.DocumentChangeInfo
import androidx.appsearch.observer.ObserverCallback
import androidx.appsearch.observer.SchemaChangeInfo
import java.io.Closeable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@RequiresApi(Build.VERSION_CODES.S)
internal class AppFunctionObserverCallback : ObserverCallback, Closeable {
    private val changeEvents = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)

    override fun onSchemaChanged(schemaChangeInfo: SchemaChangeInfo) {
        if (!isAppFunctionDb(schemaChangeInfo.getDatabaseName())) {
            return
        }
        val changedPackages =
            schemaChangeInfo.changedSchemaNames
                .mapNotNull { schemaName ->
                    AppFunctionMetadataDocument.getPackageFromSchemaName(schemaName)
                }
                .distinct()
                .toSet()
        if (changedPackages.isNotEmpty()) {
            changeEvents.trySend(ObserveAppFunctionsEvent.MetadataChanged(changedPackages))
        }
    }

    override fun onDocumentChanged(docChangeInfo: DocumentChangeInfo) {
        val isStatic = isStaticDbChange(docChangeInfo)
        val isRuntime = isRuntimeDbChange(docChangeInfo)
        if (!isStatic && !isRuntime) {
            return
        }
        val changedFunctions =
            docChangeInfo.changedDocumentIds
                .mapNotNull { docId ->
                    try {
                        AppFunctionName.fromQualifiedId(docId)
                    } catch (e: IllegalArgumentException) {
                        Log.w(APP_FUNCTIONS_TAG, "Failed to parse changed function ID $docId", e)
                        null
                    }
                }
                .distinct()
                .toSet()
        if (changedFunctions.isNotEmpty()) {
            if (isStatic) {
                val changedPackages = changedFunctions.map { it.packageName }.toSet()
                changeEvents.trySend(ObserveAppFunctionsEvent.MetadataChanged(changedPackages))
            }
            if (isRuntime) {
                changeEvents.trySend(ObserveAppFunctionsEvent.StatesChanged(changedFunctions))
            }
        }
    }

    public fun observe(): Flow<ObserveAppFunctionsEvent> =
        changeEvents.receiveAsFlow().debounceAndMerge(OBSERVER_DEBOUNCE_MILLIS)

    override fun close() {
        changeEvents.close()
    }

    private companion object {
        private fun isStaticDbChange(changeInfo: DocumentChangeInfo): Boolean =
            changeInfo.databaseName == APP_FUNCTIONS_STATIC_DATABASE_NAME &&
                changeInfo.namespace == APP_FUNCTIONS_NAMESPACE

        private fun isRuntimeDbChange(changeInfo: DocumentChangeInfo): Boolean =
            changeInfo.databaseName == APP_FUNCTIONS_RUNTIME_DATABASE_NAME &&
                changeInfo.namespace == APP_FUNCTIONS_RUNTIME_NAMESPACE

        private fun isAppFunctionDb(databaseName: String): Boolean =
            databaseName == APP_FUNCTIONS_STATIC_DATABASE_NAME ||
                databaseName == APP_FUNCTIONS_RUNTIME_DATABASE_NAME
    }
}
