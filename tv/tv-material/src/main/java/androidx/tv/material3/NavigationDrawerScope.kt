/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

/**
 * [NavigationDrawerScope] is used to provide the doesNavigationDrawerHaveFocus state to the
 * [NavigationDrawerItem] composable
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
interface NavigationDrawerScope {
    /**
     * Whether any item within the [NavigationDrawer] or [ModalNavigationDrawer] is focused
     */
    @get:Suppress("GetterSetterNames")
    val hasFocus: Boolean
}

@OptIn(ExperimentalTvMaterial3Api::class)
internal class NavigationDrawerScopeImpl(
    override val hasFocus: Boolean
) : NavigationDrawerScope
