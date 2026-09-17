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

package androidx.appfunctions

import androidx.appfunctions.metadata.AppFunctionMetadata

// TODO(b/561508342): Link SdkVersionFull instead of using code reference.

/**
 * Defines the caller access level of an app function.
 *
 * If an app function is not annotated with [AppFunctionAccessLevel], it defaults to
 * [AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED], meaning it is accessible to any caller
 * holding the `android.permission.EXECUTE_APP_FUNCTIONS` permission.
 *
 * The Android framework natively enforces access restrictions starting in Android 17.2
 * (`android.os.Build.SdkVersionFull.CINNAMON_BUN_2`). On earlier platform versions, setting
 * [isCompatEnforcementEnabled] to `true` enables Jetpack to enforce access restrictions instead of
 * the platform.
 *
 * Backwards compatibility enforcement on platform versions prior to Android 17.2 is a best-effort
 * verification mechanism using caller tokens and incurs an IPC verification overhead for foreign
 * callers. Additionally, access-level-based state filtering on pre-17.2 platforms is only applied
 * when callers query via Jetpack's `AppFunctionManager.getAppFunctionStates`; direct queries using
 * platform framework APIs on pre-17.2 platforms will see all indexed functions when the caller has
 * the `android.permission.EXECUTE_APP_FUNCTIONS` permission.
 *
 * When [isCompatEnforcementEnabled] is set to `false`, access level checks are skipped on pre-17.2
 * platforms, allowing any caller certified for AppFunctions to execute the function, while devices
 * running Android 17.2+ continue to enforce the declared [level]. To prevent a function from being
 * exposed or executable on platforms prior to Android 17.2 when backwards compatibility enforcement
 * is disabled, the function should only be enabled or registered at runtime on devices running
 * Android 17.2+ (e.g., using [androidx.appfunctions.AppFunctionManager.setAppFunctionEnabled] or
 * [androidx.appfunctions.AppFunctionManager.registerAppFunction]).
 *
 * Example usage:
 * ```kotlin
 * @AppFunctionDeclaration
 * @AppFunctionAccessLevel(
 *     level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
 *     isCompatEnforcementEnabled = true,
 * )
 * fun executeTask(params: TaskParams): TaskResult { ... }
 * ```
 *
 * @param level minimum access level required to invoke the function. Possible values are
 *   [AppFunctionMetadata.ACCESS_LEVEL_SELF] (strictly restricted to callers with the same UID as
 *   the hosting application), [AppFunctionMetadata.ACCESS_LEVEL_SYSTEM], and
 *   [AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED].
 * @param isCompatEnforcementEnabled whether Jetpack should enforce access restrictions on platform
 *   versions prior to Android 17.2.
 */
@ExperimentalAppFunctionsApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class AppFunctionAccessLevel(
    @AppFunctionMetadata.AccessLevel public val level: Int,
    public val isCompatEnforcementEnabled: Boolean,
)
