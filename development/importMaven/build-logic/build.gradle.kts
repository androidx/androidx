import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlinJvm)
}

// create a config file that ships in resources so that we can detect the repository layout at
// runtime.
val writeConfigPropsTask = tasks.register("prepareEnvironmentProps", WriteProperties::class) {
    description =  "Generates a properties file with the current environment"
    destinationFile.set(project.layout.buildDirectory.map {
        it.file("importMavenConfig.properties")
    })
    property("supportRoot", project.projectDir.resolve("../../../").canonicalPath)
}

val createPropertiesResourceDirectoryTask = tasks.register("createPropertiesResourceDirectory", Copy::class) {
    description = "Creates a directory with the importMaven properties which can be set" +
            " as an input directory to the java resources"
    from(writeConfigPropsTask.map { it.destinationFile })
    into(project.layout.buildDirectory.dir("environmentConfig"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    sourceSets {
        main {
            resources.srcDir(createPropertiesResourceDirectoryTask.map { it.destinationDir })
        }
    }
}
tasks.withType(KotlinCompile::class.java).configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(gradleApi())
    implementation(gradleTestKit())
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinCoroutinesCore)
    implementation(importMavenLibs.okio)
    implementation(importMavenLibs.bundles.ktorServer)
    implementation(importMavenLibs.ktorClientOkHttp)
    implementation(importMavenLibs.bundles.log4j)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(importMavenLibs.okioFakeFilesystem)
}

gradlePlugin {
    plugins {
        create("ImportMavenPlugin") {
            id = "ImportMavenPlugin"
            implementationClass = "androidx.build.importmaven.ImportMavenPlugin"
        }
    }
}
