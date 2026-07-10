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

package androidx.appfunctions

/**
 * Annotation to mark [AppFunctionService] as an entry point.
 *
 * This annotation helps generate a service, bridging [AppFunctionService.onExecuteFunction] and
 * [AppFunction]-annotated methods defined in the same class.
 *
 * ### Example
 *
 * First, define your service:
 * ```
 * import androidx.appfunctions.AppFunctionServiceEntryPoint
 * import androidx.appfunctions.AppFunction
 * import androidx.appfunctions.AppFunctionService
 *
 * @AppFunctionServiceEntryPoint(
 *   serviceName = "MyAppFunctionService",
 *   appFunctionXmlFileName = "my_service"
 * )
 * abstract class BaseMyAppFunctionService : AppFunctionService() {
 *   @AppFunction fun add(a: Int, b: Int): Int = a + b
 * }
 * ```
 *
 * Then, declare the generated service and XML file in your `AndroidManifest.xml`:
 * ```xml
 * <service
 *     android:name=".MyAppFunctionService"
 *     android:permission="android.permission.BIND_APP_FUNCTION_SERVICE"
 *     android:exported="true">
 *     <property
 *         android:name="android.app.appfunctions.schema"
 *         android:value="app_functions_schema.xsd" />
 *     <property
 *         android:name="android.app.appfunctions.v2"
 *         android:value="my_service.xml" />
 *     <intent-filter>
 *         <action android:name="android.app.appfunctions.AppFunctionService" />
 *     </intent-filter>
 * </service>
 * ```
 *
 * ## Generated content
 *
 * The AppFunction compiler processes classes marked with this annotation to generate two key
 * artifacts:
 * - **A concrete service implementation:** The compiler generates a subclass of your abstract class
 *   (`BaseMyAppFunctionService` in the example above) with the name specified in the [serviceName]
 *   parameter. In addition to the generated `onExecuteFunction` implementation, the function IDs
 *   that can be used with [AppFunctionManager.setAppFunctionEnabled] are also available in the
 *   companion object. The generated service has the following structure:
 * ```
 * public class MyAppFunctionService : BaseMyAppFunctionService() {
 *   override suspend fun onExecuteFunction(
 *     request: ExecuteAppFunctionRequest
 *   ): ExecuteAppFunctionResponse {
 *     return when (request.functionIdentifier) {
 *       FUNCTION_ID_ADD -> {
 *         add(request.parameters.getInt("a"), request.getParameters.getInt("b"))
 *       }
 *       ...
 *     }.toAppFunctionResponse()
 *   }
 *
 *   public companion object {
 *     public const val FUNCTION_ID_ADD: String = <id>
 *   }
 * }
 * ```
 * - **An AppFunction XML file:** The compiler generates an XML file named after the
 *   [appFunctionXmlFileName] parameter. This file is placed in the application's `assets` directory
 *   and describes the AppFunctions exposed by this entry point.
 *
 * ```
 * <appfunctions>
 *      <appfunction>
 *          <id>add</id>
 *          <enabledByDefault>true</enabledByDefault>
 *          <parameters>...</parameters>
 *          <response>...</response>
 *      </appfunction>
 *  </appfunctions>
 * ```
 *
 * @param serviceName The name of the generated service class.
 * @param appFunctionXmlFileName The name of the generated app function XML file.
 * @see androidx.appfunctions.AppFunctionService
 * @see androidx.appfunctions.AppFunction
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionServiceEntryPoint(
    val serviceName: String,
    val appFunctionXmlFileName: String,
)
