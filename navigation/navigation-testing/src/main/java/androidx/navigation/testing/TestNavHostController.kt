/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.testing

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import java.lang.IllegalArgumentException

/**
 * Subclass of [NavHostController] that offers additional APIs for testing Navigation.
 */
public class TestNavHostController(context: Context) : NavHostController(context) {

    /**
     * Gets an immutable copy of the [elements][NavBackStackEntry] currently on the back stack.
     */
    public val backStack: List<NavBackStackEntry> get() = backQueue.toList()

    init {
        navigatorProvider = TestNavigatorProvider()
    }

    /**
     * Navigate directly to any destination on the current [androidx.navigation.NavGraph] via an
     * explicit deep link. If an implicit deep link exists for this destination use
     * [#navigate(Uri)] instead.
     *
     * @param destId The destination id to navigate to.
     * @param args The arguments to pass to the destination.
     * @throws IllegalArgumentException If the [destination][destId] does not exist on the NavGraph.
     */
    @JvmOverloads
    public fun setCurrentDestination(@IdRes destId: Int, args: Bundle = Bundle()) {
        val taskStackBuilder = createDeepLink()
            .setDestination(destId)
            .setArguments(args)
            .createTaskStackBuilder()
        val intent = taskStackBuilder.editIntentAt(0)
        require(handleDeepLink(intent)) { "Destination does not exist on the NavGraph." }
    }

    /**
     * Navigate directly to any destination on the current [androidx.navigation.NavGraph] via an
     * explicit deep link. If an implicit deep link exists for this destination use
     * [#navigate(Uri)] instead.
     *
     * @param destRoute The destination route to navigate to.
     * @param args The arguments to pass to the destination.
     * @throws IllegalArgumentException If the [destination][destRoute] does not exist on the
     * NavGraph.
     */
    @JvmOverloads
    public fun setCurrentDestination(destRoute: String, args: Bundle = Bundle()) {
        val taskStackBuilder = createDeepLink()
            .setDestination(destRoute)
            .setArguments(args)
            .createTaskStackBuilder()
        val intent = taskStackBuilder.editIntentAt(0)
        require(handleDeepLink(intent)) { "Destination does not exist on the NavGraph." }
    }
}
