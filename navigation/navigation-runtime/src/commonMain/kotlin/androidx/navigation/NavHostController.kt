/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.navigation

/**
 * Subclass of [NavController] that offers additional APIs for use by a
 * [NavHost] to connect the NavController to external dependencies.
 *
 * Apps should generally not construct controllers, instead obtain a relevant controller
 * directly from a navigation host via [NavHost.navController] or by using one of
 * the utility methods on the [Navigation] class.
 */
public expect open class NavHostController : NavController
