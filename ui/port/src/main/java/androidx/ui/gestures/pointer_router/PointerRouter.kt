/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.pointer_router

import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.gestures.events.PointerEvent

/** A routing table for [PointerEvent] events. */
class PointerRouter {
    private val _routeMap: MutableMap<Int, LinkedHashSet<PointerRoute>> = mutableMapOf()
    private val _globalRoutes: LinkedHashSet<PointerRoute> = LinkedHashSet()

    /**
     * Adds a route to the routing table.
     *
     * Whenever this object routes a [PointerEvent] corresponding to
     * pointer, call route.
     *
     * Routes added reentrantly within [PointerRouter.route] will take effect when
     * routing the next event.
     */
    fun addRoute(pointer: Int, route: PointerRoute) {
        val routes: LinkedHashSet<PointerRoute> = _routeMap.getOrPut(pointer) { LinkedHashSet() }
        assert(!routes.contains(route))
        routes.add(route)
    }

    /**
     * Removes a route from the routing table.
     *
     * No longer call route when routing a [PointerEvent] corresponding to
     * pointer. Requires that this route was previously added to the router.
     *
     * Routes removed reentrantly within [PointerRouter.route] will take effect
     * immediately.
     */
    fun removeRoute(pointer: Int, route: PointerRoute) {
        assert(_routeMap.containsKey(pointer))
        val routes: LinkedHashSet<PointerRoute> = _routeMap[pointer]!!
        assert(routes.contains(route))
        routes.remove(route)
        if (routes.isEmpty())
            _routeMap.remove(pointer)
    }

    /**
     * Adds a route to the global entry in the routing table.
     *
     * Whenever this object routes a [PointerEvent], call route.
     *
     * Routes added reentrantly within [PointerRouter.route] will take effect when
     * routing the next event.
     */
    fun addGlobalRoute(route: PointerRoute) {
        assert(!_globalRoutes.contains(route))
        _globalRoutes.add(route)
    }

    /**
     * Removes a route from the global entry in the routing table.
     *
     * No longer call route when routing a [PointerEvent]. Requires that this
     * route was previously added via [addGlobalRoute].
     *
     * Routes removed reentrantly within [PointerRouter.route] will take effect
     * immediately.
     */
    fun removeGlobalRoute(route: PointerRoute) {
        assert(_globalRoutes.contains(route))
        _globalRoutes.remove(route)
    }

    private fun _dispatch(event: PointerEvent, route: PointerRoute) {
        try {
            route(event)
        } catch (exception: Exception) {
            FlutterError.reportError(
                FlutterErrorDetailsForPointerRouter(
                    exception = exception,
                    stack = exception.stackTrace,
                    library = "gesture library",
                    context = "while routing a pointer event",
                    router = this,
                    route = route,
                    event = event,
                    informationCollector = { information: StringBuffer ->
                        information.appendln("Event:")
                        information.append("  $event")
                    }
                )
            )
        }
    }

    /**
     * Calls the routes registered for this pointer event.
     *
     * Routes are called in the order in which they were added to the
     * PointerRouter object.
     */
    fun route(event: PointerEvent) {
        val routes: LinkedHashSet<PointerRoute>? = _routeMap[event.pointer]
        val globalRoutes: List<PointerRoute> = _globalRoutes.toList()
        val copy = routes?.toList()
        copy?.forEach {
            if (routes.contains(it))
                _dispatch(event, it)
        }
        globalRoutes.forEach {
            if (_globalRoutes.contains(it))
                _dispatch(event, it)
        }
    }
}