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

package androidx.navigation3.dynamicfeatures

import androidx.kruth.assertThat
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DynamicFeatureModuleTest {

    @Test
    fun dynamicFeatureModule_registersInitialKeyWithMetadata() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val entry = provider.invoke(FeatureAInitialKey)
        val metadata = checkNotNull(entry.metadata[LoadingDynamicFeatureModuleMetadataKey])
        assertThat(metadata.moduleName).isEqualTo("featureA")
        assertThat(metadata.entryProviderClassName)
            .isEqualTo(FeatureAEntryProvider::class.java.name)
    }

    @Test
    fun dynamicFeatureModule_resolvesInternalSubScreenKeysLazily() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val initialEntry = provider.invoke(FeatureAInitialKey)
        dynamicEntryProvider.initialize(
            checkNotNull(initialEntry.metadata[LoadingDynamicFeatureModuleMetadataKey])
        )

        val resolvedInitialEntry = provider.invoke(FeatureAInitialKey)
        assertThat(resolvedInitialEntry.contentKey).isEqualTo("featureA_initial")
        assertThat(resolvedInitialEntry.metadata[LoadingDynamicFeatureModuleMetadataKey]).isNull()

        val subEntry = provider.invoke(FeatureASubScreenKey)
        assertThat(subEntry.contentKey).isEqualTo("featureA_subScreen")
    }

    @Test
    fun dynamicFeatureModule_whenModuleAlreadyInstalled_resolvesSubScreenDirectlyWithoutInitialKeyLookup() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = { setOf("featureA") },
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val subEntry = provider.invoke(FeatureASubScreenKey)
        assertThat(subEntry.contentKey).isEqualTo("featureA_subScreen")
    }

    @Test
    fun dynamicFeatureModule_whenModuleAlreadyInstalled_preservesEntryMetadata() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = { setOf("featureA", "featureB") },
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                    dynamicFeatureModule(
                        initialKey = FeatureBInitialKey,
                        moduleName = "featureB",
                        entryProviderClassName = FeatureBEntryProvider::class.java.name,
                    )
                }
                .build()

        val initialEntry = provider.invoke(FeatureAInitialKey)
        assertThat(initialEntry.metadata["screenType"]).isEqualTo("initial")

        val subEntry = provider.invoke(FeatureBSubScreenKey)
        assertThat(subEntry.metadata["screenType"]).isEqualTo("subScreen")
    }

    @Test
    fun dynamicFeatureModule_whenMultipleModulesInstalled_resolvingInitialKeyOnlyInitializesTargetModule() {
        CountingEntryProvider.instantiationCount = 0
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = { setOf("featureA", "countingFeature") },
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                    dynamicFeatureModule(
                        initialKey = CountingInitialKey,
                        moduleName = "countingFeature",
                        entryProviderClassName = CountingEntryProvider::class.java.name,
                    )
                }
                .build()

        val entryA = provider.invoke(FeatureAInitialKey)
        assertThat(entryA.contentKey).isEqualTo("featureA_initial")
        assertThat(CountingEntryProvider.instantiationCount).isEqualTo(0)
    }

    @Test
    fun dynamicFeatureModule_multipleModules_resolvesEntriesAcrossModules() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                    dynamicFeatureModule(
                        initialKey = FeatureBInitialKey,
                        moduleName = "featureB",
                        entryProviderClassName = FeatureBEntryProvider::class.java.name,
                    )
                }
                .build()

        dynamicEntryProvider.initialize(
            checkNotNull(
                provider.invoke(FeatureAInitialKey).metadata[LoadingDynamicFeatureModuleMetadataKey]
            )
        )
        dynamicEntryProvider.initialize(
            checkNotNull(
                provider.invoke(FeatureBInitialKey).metadata[LoadingDynamicFeatureModuleMetadataKey]
            )
        )

        val entryA = provider.invoke(FeatureASubScreenKey)
        assertThat(entryA.contentKey).isEqualTo("featureA_subScreen")

        val entryB = provider.invoke(FeatureBSubScreenKey)
        assertThat(entryB.contentKey).isEqualTo("featureB_subScreen")
    }

    @Test
    fun dynamicFeatureModule_missingBuilderClass_throwsIllegalStateException() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "invalidFeature",
                        entryProviderClassName =
                            "androidx.navigation3.dynamicfeatures.NonExistentEntryBuilder",
                    )
                }
                .build()
        val invalidModule =
            checkNotNull(
                provider.invoke(FeatureAInitialKey).metadata[LoadingDynamicFeatureModuleMetadataKey]
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                dynamicEntryProvider.initialize(invalidModule)
            }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot instantiate the DynamicFeatureModuleEntryProvider with class name " +
                    "androidx.navigation3.dynamicfeatures.NonExistentEntryBuilder"
            )
    }

    @Test
    fun dynamicFeatureModule_initializeTwice_buildsEntriesOnce() {
        CountingEntryProvider.instantiationCount = 0
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = CountingInitialKey,
                        moduleName = "countingFeature",
                        entryProviderClassName = CountingEntryProvider::class.java.name,
                    )
                }
                .build()
        val module =
            checkNotNull(
                provider.invoke(CountingInitialKey).metadata[LoadingDynamicFeatureModuleMetadataKey]
            )

        dynamicEntryProvider.initialize(module)
        dynamicEntryProvider.initialize(module)
        val firstEntry = provider.invoke(CountingInitialKey)
        val secondEntry = provider.invoke(CountingInitialKey)

        assertThat(firstEntry).isNotNull()
        assertThat(secondEntry).isNotNull()
        assertThat(CountingEntryProvider.instantiationCount).isEqualTo(1)
    }

    @Test
    fun dynamicFeatureModule_withCustomNonNavKeyType_registersAndResolvesSuccessfully() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<CustomKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = CustomKey("initial"),
                        moduleName = "customFeature",
                        entryProviderClassName = CustomKeyEntryProvider::class.java.name,
                    )
                }
                .build()

        val entry = provider.invoke(CustomKey("initial"))
        val metadata = checkNotNull(entry.metadata[LoadingDynamicFeatureModuleMetadataKey])
        assertThat(metadata.moduleName).isEqualTo("customFeature")

        dynamicEntryProvider.initialize(metadata)

        val subEntry = provider.invoke(CustomKey("sub"))
        assertThat(subEntry.contentKey).isEqualTo("custom_sub")
    }

    @Test
    fun entryProvider_unknownKey_delegatesToCustomFallback() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = { setOf("featureA") },
                defaultLoadingContent = {},
            )
        dynamicEntryProvider.fallback = { key ->
            NavEntry(key = key, contentKey = "custom_fallback_$key") {}
        }
        val provider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val unknownEntry = provider.invoke(FeatureBInitialKey)
        assertThat(unknownEntry.contentKey).isEqualTo("custom_fallback_$FeatureBInitialKey")
    }

    @Test
    fun dynamicFeatureModule_rebuildingEntryProviderAfterInitialization_preservesInitializedEntries() {
        val dynamicEntryProvider =
            DynamicFeatureEntryProvider<NavKey>(
                installedModules = ::emptySet,
                defaultLoadingContent = {},
            )
        val firstProvider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val initialEntry = firstProvider.invoke(FeatureAInitialKey)
        dynamicEntryProvider.initialize(
            checkNotNull(initialEntry.metadata[LoadingDynamicFeatureModuleMetadataKey])
        )

        val rebuiltProvider =
            DynamicFeatureEntryProviderScope(dynamicEntryProvider)
                .apply {
                    dynamicFeatureModule(
                        initialKey = FeatureAInitialKey,
                        moduleName = "featureA",
                        entryProviderClassName = FeatureAEntryProvider::class.java.name,
                    )
                }
                .build()

        val resolvedAfterRebuild = rebuiltProvider.invoke(FeatureAInitialKey)
        assertThat(resolvedAfterRebuild.contentKey).isEqualTo("featureA_initial")
        assertThat(resolvedAfterRebuild.metadata[LoadingDynamicFeatureModuleMetadataKey]).isNull()
    }
}

private data object FeatureAInitialKey : NavKey

private data object FeatureASubScreenKey : NavKey

private data object FeatureBInitialKey : NavKey

private data object FeatureBSubScreenKey : NavKey

private data object CountingInitialKey : NavKey

private data class CustomKey(val id: String)

private class FeatureAEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(
            FeatureAInitialKey,
            contentKey = "featureA_initial",
            metadata = mapOf("screenType" to "initial"),
        ) {}
        entry(
            FeatureASubScreenKey,
            contentKey = "featureA_subScreen",
            metadata = mapOf("screenType" to "subScreen"),
        ) {}
    }
}

private class FeatureBEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(FeatureBInitialKey, contentKey = "featureB_initial") {}
        entry(
            FeatureBSubScreenKey,
            contentKey = "featureB_subScreen",
            metadata = mapOf("screenType" to "subScreen"),
        ) {}
    }
}

private class CountingEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    init {
        instantiationCount++
    }

    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(CountingInitialKey, contentKey = "counting_initial") {}
    }

    companion object {
        var instantiationCount = 0
    }
}

private class CustomKeyEntryProvider : DynamicFeatureModuleEntryProvider<CustomKey> {
    override fun EntryProviderScope<CustomKey>.buildEntries() {
        entry(CustomKey("initial"), contentKey = "custom_initial") {}
        entry(CustomKey("sub"), contentKey = "custom_sub") {}
    }
}
