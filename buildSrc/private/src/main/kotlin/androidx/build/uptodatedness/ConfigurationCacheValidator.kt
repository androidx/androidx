/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.uptodatedness

import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

fun Project.setupConfigurationCacheValidator(
    registry: BuildEventsListenerRegistry,
    buildFeatures: BuildFeatures,
    flowScope: FlowScope,
) {
    val validate =
        providers.environmentVariable(DISALLOW_TASK_EXECUTION_VAR_NAME).map { true }.orElse(false)

    val tracker =
        gradle.sharedServices.registerIfAbsent(
            "configurationPhaseTracker",
            ConfigurationPhaseTracker::class.java,
        ) {}
    // Registering as a task completion listener is required to keep the build service
    // alive across the build lifecycle, otherwise it won't work.
    registry.onTaskCompletion(tracker)
    tracker.get().configurationPhaseRan.set(true)

    flowScope.always(VerifyConfigurationCacheAction::class.java) { spec ->
        spec.parameters.configurationPhaseTracker.set(tracker)
        spec.parameters.validate.set(validate)
        spec.parameters.configurationCacheActive.set(buildFeatures.configurationCache.active)
    }
}

/**
 * A build service that is registered during the configuration phase to detect if the configuration
 * phase actually ran.
 *
 * Since build services are reconstructed or restored but their configuration-phase registration
 * logic is skipped when the configuration cache is reused, the [configurationPhaseRan] flag will
 * only be set to `true` if the configuration phase was executed in the current build.
 */
abstract class ConfigurationPhaseTracker :
    BuildService<BuildServiceParameters.None>, OperationCompletionListener {

    val configurationPhaseRan = AtomicBoolean(false)

    override fun onFinish(event: FinishEvent) = Unit
}

/**
 * Validates configuration cache reuse on build completion.
 *
 * Asserts that a verifyUpToDate re-run successfully reuses the configuration cache. Throws a
 * [GradleException] if the configuration cache was invalidated.
 */
abstract class VerifyConfigurationCacheAction :
    FlowAction<VerifyConfigurationCacheAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference val configurationPhaseTracker: Property<ConfigurationPhaseTracker>

        @get:Input val validate: Property<Boolean>
        @get:Input val configurationCacheActive: Property<Boolean>
    }

    override fun execute(parameters: Parameters) {
        val tracker = parameters.configurationPhaseTracker.get()
        val fail =
            tracker.configurationPhaseRan.get() &&
                parameters.validate.get() &&
                parameters.configurationCacheActive.orElse(false).get()
        if (fail) {
            throw GradleException(
                "Configuration cache was invalidated; the verifyUpToDate re-run should have reused it.\n" +
                    "See \$DIST_DIR/configuration-cache-reports for details."
            )
        }
    }
}
