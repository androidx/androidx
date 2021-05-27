/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation.dynamicfeatures

import androidx.annotation.IdRes
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.dynamicfeatures.DynamicIncludeGraphNavigator.DynamicIncludeNavGraph
import androidx.navigation.get

/**
 * Construct a new [DynamicIncludeGraphNavigator.DynamicIncludeNavGraph].
 *
 * @param id NavGraph id.
 * @param moduleName Dynamic feature module name as defined in the module's `AndroidManifest`.
 * This must not be an empty string.
 * @param graphResourceName Graph's resource name without the `navigation` qualifier. This
 * must not be an empty string.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to include your DynamicNavGraph instead",
    ReplaceWith("includeDynamic(route = id.toString(), moduleName, graphResourceName)")
)
public inline fun DynamicNavGraphBuilder.includeDynamic(
    @IdRes id: Int,
    moduleName: String,
    graphResourceName: String
): Unit = includeDynamic(id, moduleName, graphResourceName) {}

/**
 * Construct a new [DynamicIncludeGraphNavigator.DynamicIncludeNavGraph].
 *
 * @param id NavGraph id.
 * @param moduleName Dynamic feature module name as defined in the module's `AndroidManifest`.
 * This must not be an empty string.
 * @param graphResourceName Graph's resource name without the `navigation` qualifier. This
 * must not be an empty string.
 * @param builder Another builder for chaining.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to include your DynamicNavGraph instead",
    ReplaceWith(
        "includeDynamic(route = id.toString(), moduleName, graphResourceName) { builder.invoke() }"
    )
)
public inline fun DynamicNavGraphBuilder.includeDynamic(
    @IdRes id: Int,
    moduleName: String,
    graphResourceName: String,
    builder: DynamicIncludeNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicIncludeNavGraphBuilder(
        provider[DynamicIncludeGraphNavigator::class],
        id,
        moduleName,
        graphResourceName
    ).apply(builder)
)

/**
 * Construct a new [DynamicIncludeGraphNavigator.DynamicIncludeNavGraph].
 *
 * @param route NavGraph route.
 * @param moduleName Dynamic feature module name as defined in the module's `AndroidManifest`.
 * This must not be an empty string.
 * @param graphResourceName Graph's resource name without the `navigation` qualifier. This
 * must not be an empty string.
 */
public inline fun DynamicNavGraphBuilder.includeDynamic(
    route: String,
    moduleName: String,
    graphResourceName: String
): Unit = includeDynamic(route, moduleName, graphResourceName) {}

/**
 * Construct a new [DynamicIncludeGraphNavigator.DynamicIncludeNavGraph].
 *
 * @param route NavGraph route.
 * @param moduleName Dynamic feature module name as defined in the module's `AndroidManifest`.
 * This must not be an empty string.
 * @param graphResourceName Graph's resource name without the `navigation` qualifier. This
 * must not be an empty string.
 * @param builder Another builder for chaining.
 */
public inline fun DynamicNavGraphBuilder.includeDynamic(
    route: String,
    moduleName: String,
    graphResourceName: String,
    builder: DynamicIncludeNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicIncludeNavGraphBuilder(
        provider[DynamicIncludeGraphNavigator::class],
        route,
        moduleName,
        graphResourceName
    ).apply(builder)
)

/**
 * DSL for constructing a new [DynamicIncludeGraphNavigator.DynamicIncludeNavGraph]
 */
@NavDestinationDsl
public class DynamicIncludeNavGraphBuilder : NavDestinationBuilder<DynamicIncludeNavGraph> {

    private var dynamicIncludeGraphNavigator: DynamicIncludeGraphNavigator
    private var moduleName: String
    private var graphResourceName: String

    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to create your DynamicIncludeNavGraphBuilder instead",
        ReplaceWith(
            "DynamicIncludeNavGraphBuilder(dynamicIncludeGraphNavigator, route = id.toString(), " +
                "moduleName, graphResourceName)"
        )
    )
    public constructor(
        dynamicIncludeGraphNavigator: DynamicIncludeGraphNavigator,
        @IdRes id: Int,
        moduleName: String,
        graphResourceName: String
    ) : super(dynamicIncludeGraphNavigator, id) {
        this.dynamicIncludeGraphNavigator = dynamicIncludeGraphNavigator
        this.moduleName = moduleName
        this.graphResourceName = graphResourceName
    }

    public constructor(
        dynamicIncludeGraphNavigator: DynamicIncludeGraphNavigator,
        route: String,
        moduleName: String,
        graphResourceName: String
    ) : super(dynamicIncludeGraphNavigator, route) {
        this.dynamicIncludeGraphNavigator = dynamicIncludeGraphNavigator
        this.moduleName = moduleName
        this.graphResourceName = graphResourceName
    }

    /**
     * Destination NavGraph's resource package as defined in the module's
     * `AndroidManifest`. This generally does not need to be manually set and will
     * be set `applicationId.moduleName` if left null.
     *
     * This cannot be an empty string.
     */
    public var graphPackage: String? = null

    /**
     * @return The [DynamicGraphNavigator.DynamicNavGraph]
     */
    override fun build(): DynamicIncludeNavGraph =
        super.build().also { navGraph ->
            check(moduleName.isNotEmpty()) {
                "Module name cannot be empty"
            }
            navGraph.moduleName = moduleName
            if (graphPackage == null) {
                navGraph.graphPackage = "${dynamicIncludeGraphNavigator.packageName}.$moduleName"
            } else {
                check(!graphPackage.isNullOrEmpty()) {
                    "Graph package name cannot be empty"
                }
                navGraph.graphPackage = graphPackage
            }
            check(graphResourceName.isNotEmpty()) {
                "Graph resource name cannot be empty"
            }
            navGraph.graphResourceName = graphResourceName
        }
}
