pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        val allowJetbrains = "androidx.allowJetbrainsDev"
        if (settings.extra.has(allowJetbrains) && settings.extra.get(allowJetbrains) == "true") {
            maven(url = "https://packages.jetbrains.team/maven/p/kt/dev")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        val allowJetbrains = "androidx.allowJetbrainsDev"
        if (settings.extra.has(allowJetbrains) && settings.extra.get(allowJetbrains) == "true") {
            maven(url = "https://packages.jetbrains.team/maven/p/kt/dev")
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("importMavenLibs") {
            from(files("../importMaven.versions.toml"))
        }
    }
}
