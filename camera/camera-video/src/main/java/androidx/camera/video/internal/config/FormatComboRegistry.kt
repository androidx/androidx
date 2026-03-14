/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video.internal.config

import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import androidx.camera.video.MediaSpec
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_UNSPECIFIED

/**
 * A read-only lookup engine for media format combinations.
 *
 * This registry manages a collection of [FormatCombo] objects organized by container and codec. It
 * is designed for high-performance lookups during camera configuration and UI population.
 *
 * ### Inclusion of Video-Only and Audio-Only Variants
 * A key behavior of this registry is that queries will **always include potential `null` formats**.
 * For every registered codec combination, the registry automatically provides:
 * * **Video-only variants**: The video codec paired with a `null` audio track.
 * * **Audio-only variants**: The audio codec paired with a `null` video track.
 *
 * Consequently, callers using query methods like [getCombos] should expect results to contain
 * [FormatCombo] instances where either the video or audio MIME type is `null`, representing valid
 * single-track recording scenarios.
 *
 * @param formatComboMapping A pre-computed mapping of Container -> VideoMime -> Set of valid
 *   [FormatCombo]s.
 */
public class FormatComboRegistry
private constructor(private val formatComboMapping: Map<Int, Map<String?, Set<FormatCombo>>>) {
    /**
     * Retrieves combinations based on container, video, and audio criteria with wildcard support.
     *
     * @param outputFormat The specific container ID or [OUTPUT_FORMAT_UNSPECIFIED] for any.
     * @param videoMime The specific video MIME or [MIME_TYPE_UNSPECIFIED] for any.
     * @param audioMime The specific audio MIME or [MIME_TYPE_UNSPECIFIED] for any.
     * @return A list of matching canonical [FormatCombo] instances.
     */
    public fun getCombos(
        @MediaSpec.OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
    ): List<FormatCombo> {
        val results = mutableListOf<FormatCombo>()

        // Resolve Containers to search
        val containersToSearch =
            if (outputFormat == OUTPUT_FORMAT_UNSPECIFIED) {
                formatComboMapping.keys
            } else {
                listOf(outputFormat)
            }

        for (container in containersToSearch) {
            val videoMap = formatComboMapping[container] ?: continue

            // Resolve Video Mimes to search
            val videoMimesToSearch =
                if (videoMime == MIME_TYPE_UNSPECIFIED) {
                    videoMap.keys
                } else {
                    listOf(videoMime)
                }

            for (videoMime in videoMimesToSearch) {
                val comboSet = videoMap[videoMime] ?: continue

                // Filter by Audio Mime
                if (audioMime == MIME_TYPE_UNSPECIFIED) {
                    results.addAll(comboSet)
                } else {
                    results.addAll(comboSet.filter { it.audioMime == audioMime })
                }
            }
        }
        return results
    }

    /**
     * Retrieves combinations for a specific video MIME type.
     *
     * @param videoMime The video MIME type.
     * @return A list of combos representing every valid container and audio pairing.
     */
    public fun getCombosForVideo(videoMime: String?): List<FormatCombo> {
        val results = mutableListOf<FormatCombo>()
        for (videoMap in formatComboMapping.values) {
            videoMap[videoMime]?.let { results.addAll(it) }
        }
        return results
    }

    /**
     * Retrieves combinations for a specific audio MIME type.
     *
     * @param audioMime The audio MIME type.
     * @return A list of combos representing every valid container and video pairing.
     */
    public fun getCombosForAudio(audioMime: String?): List<FormatCombo> {
        val results = mutableListOf<FormatCombo>()
        for (videoMap in formatComboMapping.values) {
            for (comboSet in videoMap.values) {
                results.addAll(comboSet.filter { it.audioMime == audioMime })
            }
        }
        return results
    }

    /** DSL Builder for constructing a [FormatComboRegistry]. */
    public class Builder {
        private val formatComboMapping =
            mutableMapOf<Int, MutableMap<String?, MutableSet<FormatCombo>>>()

        /**
         * Begins a configuration block for a specific media container.
         *
         * @param format The muxer output format constant.
         * @param block The scoping block for specifying codec support within this container.
         */
        public fun container(
            @MediaSpec.OutputFormat format: Int,
            block: ContainerScope.() -> Unit,
        ) {
            val videoMap = formatComboMapping.getOrPut(format) { mutableMapOf() }
            ContainerScope(format, videoMap).apply(block)
        }

        /** DSL scope for defining codec support within a specific container. */
        public class ContainerScope(
            private val container: Int,
            private val videoMap: MutableMap<String?, MutableSet<FormatCombo>>,
        ) {
            /**
             * Registers support for the intersection of provided video and audio codecs.
             * * This method automatically populates three scenarios:
             * 1. **Video + Audio**: Full multiplexed combinations.
             * 2. **Video-Only**: Every video codec paired with a null audio track.
             * 3. **Audio-Only**: Every audio codec paired with a null video track.
             *
             * @param videoMimes List of supported video codec MIME types.
             * @param audioMimes List of supported audio codec MIME types.
             */
            public fun support(videoMimes: List<String>, audioMimes: List<String>) {
                videoMimes.forEach { vMime ->
                    // Add all video-audio pairings
                    val targets = videoMap.getOrPut(vMime) { mutableSetOf() }
                    audioMimes.forEach { aMime ->
                        targets.add(FormatCombo(container, vMime, aMime))
                    }

                    // Add video-only pairing
                    targets.add(FormatCombo(container, vMime, null))
                }

                // Add audio-only pairing
                val audioOnlyTargets = videoMap.getOrPut(null) { mutableSetOf() }
                audioMimes.forEach { aMime ->
                    audioOnlyTargets.add(FormatCombo(container, null, aMime))
                }
            }
        }

        /** Finalizes the configuration and returns a read-only [FormatComboRegistry]. */
        public fun build(): FormatComboRegistry = FormatComboRegistry(formatComboMapping)
    }
}
