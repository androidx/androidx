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

package androidx.appfunctions.service

import androidx.annotation.RestrictTo

/**
 * Annotation for marking a class as an AppFunction entry point.
 *
 * This annotation is used to designate a class as the central registry for AppFunctions within a
 * module.
 *
 * ## Generated content
 *
 * The AppFunction compiler processes classes marked with this annotation to generate two key
 * artifacts:
 * 1. **A concrete service implementation:** The compiler generates a subclass of
 *    `AppFunctionService` with the name specified in the [serviceName] parameter. This generated
 *    service is responsible for routing incoming `ExecuteAppFunctionRequest`s to the appropriate
 *    `@AppFunction`-annotated methods.
 * 2. **An AppFunction XML file:** The compiler generates an XML file named after the
 *    [appFunctionXmlFileName] parameter. This file is placed in the application's `assets`
 *    directory and describes the AppFunctions exposed by this entry point.
 *
 * ## How to use the generated content
 *
 * To expose the AppFunctions, you must declare the generated service in your application's
 * `AndroidManifest.xml` and associate it with the generated XML file using `<property>` tags.
 *
 * ### Example
 *
 * First, define your entry point:
 * ```
 * @AppFunctionEntryPoint(
 *   serviceName = "MySimpleService",
 *   appFunctionXmlFileName = "my_simple_service.xml"
 * )
 * abstract class MySimpleEntryPoint : AppFunctionService() {
 *   @AppFunction suspend fun doSomething() { ... }
 * }
 * ```
 *
 * Then, declare the generated service and XML file in your `AndroidManifest.xml`:
 * ```xml
 * <service
 *     android:name=".MySimpleService"
 *     android:permission="android.permission.BIND_APP_FUNCTION_SERVICE"
 *     android:exported="true">
 *     <property
 *         android:name="android.app.appfunctions.schema"
 *         android:value="app_functions_schema.xsd" />
 *     <property
 *         android:name="android.app.appfunctions.v2"
 *         android:value="my_simple_service.xml" />
 *     <intent-filter>
 *         <action android:name="android.app.appfunctions.AppFunctionService" />
 *     </intent-filter>
 * </service>
 * ```
 *
 * @param serviceName The name of the generated service class.
 * @param appFunctionXmlFileName The name of the generated app function XML file.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class AppFunctionEntryPoint(
    val serviceName: String,
    val appFunctionXmlFileName: String,
)
