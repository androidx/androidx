package androidx.playground

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.initialization.Settings

class PlaygroundExtension {
    private final ObjectFactory objectFactory
    private final Settings settings

    private File supportRootDir

    @Inject
    PlaygroundExtension(Settings settings, ObjectFactory objectFactory) {
        this.settings = settings
        this.objectFactory = objectFactory
    }

    def includeProject(String name, String path) {
        settings.include(name)
        settings.project(name).projectDir = new File(supportRootDir, path)
    }

    /**
    * Initializes the playground project to use public repositories as well as other internal projects
    * that cannot be found in public repositories.
    *
    * @param settings The reference to the settings script
    * @param relativePathToRoot The relative path of the project to the root AndroidX project
    */
    def setupPlayground(String relativePathToRoot) {
        def projectDir = settings.rootProject.getProjectDir()
        def supportRoot = new File(projectDir, relativePathToRoot).getCanonicalFile()
        this.supportRootDir = supportRoot
        def buildFile = new File(supportRoot, "playground-common/playground-build.gradle")
        def relativePathToBuild = projectDir.toPath().relativize(buildFile.toPath()).toString()

        Properties playgroundProperties = new Properties()
        File propertiesFile = new File(supportRoot, "playground-common/playground.properties")
        propertiesFile.withInputStream {
            playgroundProperties.load(it)
        }
        settings.gradle.beforeProject { project ->
            // load playground properties. These are not kept in the playground projects to prevent
            // AndroidX build from reading them.
            playgroundProperties.each {
                project.ext[it.key] = it.value
            }
        }

        settings.rootProject.buildFileName = relativePathToBuild
        settings.enableFeaturePreview("VERSION_CATALOGS")

        def catalogFiles = objectFactory.fileCollection().from("$supportRoot/gradle/libs.versions.toml")
        settings.dependencyResolutionManagement {
            versionCatalogs {
                libs {
                    from(catalogFiles)
                }
            }
        }

        includeProject(":lint-checks", "lint-checks")
        includeProject(":lint-checks:integration-tests", "lint-checks/integration-tests")
        includeProject(":fakeannotations", "fakeannotations")
        includeProject(":internal-testutils-common", "testutils/testutils-common")
        includeProject(":internal-testutils-gradle-plugin", "testutils/testutils-gradle-plugin")

        // allow public repositories
        System.setProperty("ALLOW_PUBLIC_REPOS", "true")

        // specify out dir location
        System.setProperty("CHECKOUT_ROOT", supportRoot.path)
    }


    /**
    * A convenience method to include projects from the main AndroidX build using a filter.
    *
    * @param filter This filter will be called with the project name (project path in gradle).
    *               If filter returns true, it will be included in the build.
    */
    def selectProjectsFromAndroidX(filter) {
        if (supportRootDir == null) {
            throw new RuntimeException("Must call setupPlayground() first.")
        }

        // Multiline matcher for anything of the form:
        //  includeProject(name, path, ...)
        // where '...' is anything except the ')' character.
        def includeProjectPattern = ~/(?m)^[\n\r\s]*includeProject\("(?<name>[a-z0-9-:]*)",[\n\r\s]*"(?<path>[a-z0-9-\/]+)[^)]+\)$/
        def supportSettingsFile = new File(supportRootDir, "settings.gradle")
        def matcher = includeProjectPattern.matcher(supportSettingsFile.text)

        while (matcher.find()) {
            // check if is an include project line, if so, extract project gradle path and
            // file system path and call the filter
            def projectGradlePath = matcher.group("name")
            def projectFilePath = matcher.group("path")
            if (filter(projectGradlePath)) {
                includeProject(projectGradlePath, projectFilePath)
            }
        }
    }

    /**
    * Checks if a project is necessary for playground projects that involve compose.
    */
    def isNeededForComposePlayground(name) {
        if (name == ":compose:lint:common") return true
        if (name == ":compose:lint:internal-lint-checks") return true
        if (name == ":compose:test-utils") return true
        if (name == ":compose:lint:common-test") return true
        if (name == ":test:screenshot:screenshot") return true
        return false
    }
}