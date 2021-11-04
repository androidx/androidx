package androidx.playground

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class PlaygroundPlugin implements Plugin<Settings> {
    void apply(Settings settings) {
        settings.apply(plugin: 'playground-ge-conventions')

        settings.extensions.create('playground', PlaygroundExtension, settings)

        validateJvm(settings)
    }

    def validateJvm(Settings settings) {
        // validate JVM version to print an understandable error if it is not set to the
        // required value (11)
        def jvmVersion = System.getProperty("java.vm.specification.version")
        if (jvmVersion != "11") {
            def guidance;
            if (settings.gradle.startParameter.projectProperties.containsKey("android.injected.invoked.from.ide")) {
                guidance = "Make sure to set the gradle JDK to JDK 11 in the project settings." +
                        "(File -> Other Settings -> Default Project Structure)"
            } else {
                guidance = "Make sure your JAVA_HOME environment variable points to Java 11 JDK."
            }
            throw new IllegalStateException("""
                    AndroidX build must be invoked with JDK 11.
                    $guidance
                    Current version: $jvmVersion
                    Current JAVA HOME: ${System.getProperty("java.home")}""".stripIndent());
        }

    }
}
