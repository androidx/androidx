/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.os.Bundle
import androidx.annotation.IdRes

/**
 * Navigation actions provide a level of indirection between your navigation code and the
 * underlying destinations. This allows you to define common actions that change their destination
 * or [NavOptions] based on the current [NavDestination].
 *
 * The [NavOptions] associated with a NavAction are used by default when navigating
 * to this action via [NavController.navigate].
 *
 * Actions should be added via [NavDestination.putAction].
 *
 * @param destinationId the ID of the destination that should be navigated to when this
 * action is used.
 * @param navOptions special options for this action that should be used by default
 * @param defaultArguments argument bundle to be used by default
 */
public class NavAction @JvmOverloads constructor(
    /**
     * The ID of the destination that should be navigated to when this action is used
     */
    @field:IdRes
    @param:IdRes
    public val destinationId: Int,
    /**
     * The NavOptions to be used by default when navigating to this action.
     */
    public var navOptions: NavOptions? = null,
    /**
     * The argument bundle to be used by default when navigating to this action.
     *
     * @return bundle of default argument values
     */
    public var defaultArguments: Bundle? = null
)