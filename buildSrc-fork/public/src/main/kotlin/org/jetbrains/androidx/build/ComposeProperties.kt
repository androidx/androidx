/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.androidx.build

import org.gradle.api.Project

data class ComposeProperties(val targetPlatforms: Set<ComposePlatforms>) {
    constructor(project: Project) : this(
        targetPlatforms =
            ComposePlatforms.parse(project.findProperty("compose.platforms")?.toString() ?: "jvm, android")
    )
}
