/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.measurement

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant

/** Deletion Request. */
class DeletionRequest public constructor(
    val deletionMode: DeletionMode,
    val domainUris: List<Uri>,
    val originUris: List<Uri>,
    val start: Instant,
    val end: Instant,
    val matchBehavior: MatchBehavior
    ) {

    /**
     * Deletion modes for matched records.
     */
    enum class DeletionMode {
        /** Deletion mode to delete all data associated with the selected records.  */
        DELETION_MODE_ALL,

        /**
         * Deletion mode to delete all data except the internal data (e.g. rate limits) for the
         * selected records.
         */
        DELETION_MODE_EXCLUDE_INTERNAL_DATA;
    }

    /**
     * Matching Behaviors for params.
     */
    enum class MatchBehavior {
        /** Match behavior option to delete the supplied params (Origin/Domains).  */
        MATCH_BEHAVIOR_DELETE,

        /**
         * Match behavior option to preserve the supplied params (Origin/Domains) and delete
         * everything else.
         */
        MATCH_BEHAVIOR_PRESERVE;
    }

    override fun hashCode(): Int {
        var hash = deletionMode.hashCode()
        hash = 31 * hash + domainUris.hashCode()
        hash = 31 * hash + originUris.hashCode()
        hash = 31 * hash + start.hashCode()
        hash = 31 * hash + end.hashCode()
        hash = 31 * hash + matchBehavior.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeletionRequest) return false
        return this.deletionMode == other.deletionMode &&
            HashSet(this.domainUris) == HashSet(other.domainUris) &&
            HashSet(this.originUris) == HashSet(other.originUris) &&
            this.start == other.start &&
            this.end == other.end &&
            this.matchBehavior == other.matchBehavior
    }

    override fun toString(): String {
        return "DeletionRequest { DeletionMode=$deletionMode, DomainUris=$domainUris, " +
            "OriginUris=$originUris, Start=$start, End=$end, MatchBehavior=$matchBehavior }"
    }

    /**
     * Builder for {@link DeletionRequest} objects.
     *
     * @param deletionMode {@link DeletionMode} Set the match behavior for the supplied params.
     *     {@link #DELETION_MODE_ALL}: All data associated with the selected records will be
     *     deleted.
     *     {@link #DELETION_MODE_EXCLUDE_INTERNAL_DATA}: All data except the internal system
     *     data (e.g. rate limits) associated with the selected records will be deleted.
     *
     * @param start {@link Instant} Set the start of the deletion range. Not setting this or
     *     passing in {@link java.time.Instant#MIN} will cause everything from the oldest record to
     *     the specified end be deleted.
     *
     * @param end {@link Instant} Set the end of the deletion range. Not setting this or passing in
     *     {@link java.time.Instant#MAX} will cause everything from the specified start until the
     *     newest record to be deleted.
     *
     * @param matchBehavior {@link MatchBehavior} Set the match behavior for the supplied params.
     *     {@link #MATCH_BEHAVIOR_DELETE}: This option will use the supplied params
     *     (Origin URIs & Domain URIs) for selecting records for deletion.
     *     {@link #MATCH_BEHAVIOR_PRESERVE}: This option will preserve the data associated with the
     *     supplied params (Origin URIs & Domain URIs) and select remaining records for deletion.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public class Builder constructor(
        private val deletionMode: DeletionMode = DeletionMode.DELETION_MODE_ALL,
        private val start: Instant = Instant.MIN,
        private val end: Instant = Instant.MAX,
        private val matchBehavior: MatchBehavior = MatchBehavior.MATCH_BEHAVIOR_DELETE
        ) {
        private var domainUris: List<Uri> = emptyList()
        private var originUris: List<Uri> = emptyList()

        /**
         * Set the list of domain URI which will be used for matching. These will be matched with
         * records using the same domain or any subdomains. E.g. If domainUri is {@code
         * https://example.com}, then {@code https://a.example.com}, {@code https://example.com} and
         * {@code https://b.example.com} will match; {@code https://abcexample.com} will NOT match.
         * A null or empty list will match everything.
         */
        fun setDomainUris(domainUris: List<Uri>): Builder = apply {
            this.domainUris = domainUris
        }

        /**
         * Set the list of origin URI which will be used for matching. These will be matched with
         * records using the same origin only, i.e. subdomains won't match. E.g. If originUri is
         * {@code https://a.example.com}, then {@code https://a.example.com} will match; {@code
         * https://example.com}, {@code https://b.example.com} and {@code https://abcexample.com}
         * will NOT match. A null or empty list will match everything.
         */
        fun setOriginUris(originUris: List<Uri>): Builder = apply {
            this.originUris = originUris
        }

        /** Builds a {@link DeletionRequest} instance. */
        fun build(): DeletionRequest {
            return DeletionRequest(
                deletionMode,
                domainUris,
                originUris,
                start,
                end,
                matchBehavior)
        }
    }
}