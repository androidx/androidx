/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

data class ComposeComponent(
    val path: String,
    val supportedPlatforms: Set<ComposePlatforms> = ComposePlatforms.SKIKO_SUPPORT,

    // TODO: Remove once android redirect is configured
    //  - `artifactRedirecting.publication.targetNames` shouldn't add Android implicitly
    //  - Setup Compose Android redirects explicitly
    val neverRedirect: Boolean = false
)
