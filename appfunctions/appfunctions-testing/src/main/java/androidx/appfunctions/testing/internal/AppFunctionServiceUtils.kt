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

package androidx.appfunctions.testing.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.internal.AppFunctionInventoryResolver
import androidx.appfunctions.metadata.AppFunctionMetadata
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.robolectric.Robolectric
import org.robolectric.Shadows

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal object AppFunctionServiceUtils {
    private const val TAG = "AppFunctionTestUtils"
    private val EXECUTE_LOOPER_DELAY = 10.milliseconds
    const val APP_FUNCTION_SERVICE_INTENT_ACTION: String =
        android.app.appfunctions.AppFunctionService.SERVICE_INTERFACE

    /**
     * Executes an app function using an instance of [serviceClass].
     *
     * The service instance is created for the execution and destroyed once finished to prevent
     * leaks.
     */
    suspend fun executeFunction(
        serviceClass: Class<AppFunctionService>,
        request: ExecuteAppFunctionRequest,
        functionMetadata: AppFunctionMetadata,
    ): ExecuteAppFunctionResponse {
        val serviceController = Robolectric.buildService(serviceClass)
        val robolectricService = serviceController.get()
        serviceController.create()

        val cancellationSignal = CancellationSignal()
        var executionResponse: ExecuteAppFunctionResponse? = null

        try {
            robolectricService.onExecuteFunction(
                request,
                functionMetadata,
                cancellationSignal,
                Consumer { response -> executionResponse = response },
            )

            while (executionResponse == null) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                delay(EXECUTE_LOOPER_DELAY)
            }
            return executionResponse
        } catch (e: Throwable) {
            if (e is CancellationException) {
                cancellationSignal.cancel()
            }
            throw e
        } finally {
            serviceController.destroy()
        }
    }

    /** Retrieves all enabled AppFunctionService classes within the current test app package. */
    fun getEnabledAppFunctionServiceClasses(context: Context): List<Class<*>> {
        val intent =
            Intent(APP_FUNCTION_SERVICE_INTENT_ACTION).apply {
                setPackage(context.packageName)
            }
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS
        val resolveInfos = context.packageManager.queryIntentServices(intent, flags)
        return resolveInfos
            .filter { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo ?: return@filter false
                isServiceEnabled(context, serviceInfo)
            }
            .mapNotNull { resolveInfo ->
                try {
                    Class.forName(
                        /* name = */ resolveInfo.serviceInfo.name,
                        /* initialize = */ false,
                        /* loader = */ context.classLoader,
                    )
                } catch (e: ClassNotFoundException) {
                    Log.w(TAG, "AppFunctionService class not found", e)
                    null
                }
            }
    }

    /** Checks whether a service component is currently enabled. */
    fun isServiceEnabled(context: Context, serviceInfo: ServiceInfo): Boolean {
        val componentName = ComponentName(serviceInfo.packageName, serviceInfo.name)
        val enabledSetting = context.packageManager.getComponentEnabledSetting(componentName)
        return when (enabledSetting) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> serviceInfo.isEnabled
            else -> false
        }
    }

    /** Resolves the [AppFunctionInventory] implementation corresponding to [serviceClass]. */
    fun resolveInventory(serviceClass: Class<*>): AppFunctionInventory? {
        if (!AppFunctionService::class.java.isAssignableFrom(serviceClass)) {
            Log.w(TAG, "Failed to resolve inventory for service ${serviceClass.name}")
            return null
        }
        return try {
            val serviceInstance =
                serviceClass.getDeclaredConstructor().newInstance() as AppFunctionService
            AppFunctionInventoryResolver.resolveInventory(serviceInstance)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve inventory for service ${serviceClass.name}", e)
            null
        }
    }
}
