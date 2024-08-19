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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import java.time.Instant

/**
 * Deletion Request.
 *
 * @param deletionMode Set the deletion mode for the supplied params. [DELETION_MODE_ALL]: All data
 *   associated with the selected records will be deleted. [DELETION_MODE_EXCLUDE_INTERNAL_DATA]:
 *   All data except the internal system data (e.g. rate limits) associated with the selected
 *   records will be deleted.
 * @param matchBehavior Set the match behavior for the supplied params. [MATCH_BEHAVIOR_DELETE]:
 *   This option will use the supplied params (Origin URIs & Domain URIs) for selecting records for
 *   deletion. [MATCH_BEHAVIOR_PRESERVE]: This option will preserve the data associated with the
 *   supplied params (Origin URIs & Domain URIs) and select remaining records for deletion.
 * @param start [Instant] Set the start of the deletion range. Not setting this or passing in
 *   [java.time.Instant#MIN] will cause everything from the oldest record to the specified end be
 *   deleted.
 * @param end [Instant] Set the end of the deletion range. Not setting this or passing in
 *   [java.time.Instant#MAX] will cause everything from the specified start until the newest record
 *   to be deleted.
 * @param domainUris the list of domain URI which will be used for matching. These will be matched
 *   with records using the same domain or any subdomains. E.g. If domainUri is {@code
 *   https://example.com}, then {@code https://a.example.com}, {@code https://example.com} and
 *   {@code https://b.example.com} will match; {@code https://abcexample.com} will NOT match. A null
 *   or empty list will match everything.
 * @param originUris the list of origin URI which will be used for matching. These will be matched
 *   with records using the same origin only, i.e. subdomains won't match. E.g. If originUri is
 *   {@code https://a.example.com}, then {@code https://a.example.com} will match; {@code
 *   https://example.com}, {@code https://b.example.com} and {@code https://abcexample.com} will NOT
 *   match. A null or empty list will match everything.
 */
@RequiresApi(android.os.Build.VERSION_CODES.O)
class DeletionRequest(
    @DeletionMode val deletionMode: Int,
    @MatchBehavior val matchBehavior: Int,
    val start: Instant = Instant.MIN,
    val end: Instant = Instant.MAX,
    val domainUris: List<Uri> = emptyList(),
    val originUris: List<Uri> = emptyList(),
) {

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
        val deletionModeStr =
            if (deletionMode == DELETION_MODE_ALL) "DELETION_MODE_ALL"
            else "DELETION_MODE_EXCLUDE_INTERNAL_DATA"
        val matchBehaviorStr =
            if (matchBehavior == MATCH_BEHAVIOR_DELETE) "MATCH_BEHAVIOR_DELETE"
            else "MATCH_BEHAVIOR_PRESERVE"
        return "DeletionRequest { DeletionMode=$deletionModeStr, " +
            "MatchBehavior=$matchBehaviorStr, " +
            "Start=$start, End=$end, DomainUris=$domainUris, OriginUris=$originUris }"
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.measurement.DeletionRequest {
        return android.adservices.measurement.DeletionRequest.Builder()
            .setDeletionMode(deletionMode)
            .setMatchBehavior(matchBehavior)
            .setStart(start)
            .setEnd(end)
            .setDomainUris(domainUris)
            .setOriginUris(originUris)
            .build()
    }

    companion object {
        /** Deletion mode to delete all data associated with the selected records. */
        public const val DELETION_MODE_ALL = 0

        /**
         * Deletion mode to delete all data except the internal data (e.g. rate limits) for the
         * selected records.
         */
        public const val DELETION_MODE_EXCLUDE_INTERNAL_DATA = 1

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(DELETION_MODE_ALL, DELETION_MODE_EXCLUDE_INTERNAL_DATA)
        annotation class DeletionMode

        /** Match behavior option to delete the supplied params (Origin/Domains). */
        public const val MATCH_BEHAVIOR_DELETE = 0

        /**
         * Match behavior option to preserve the supplied params (Origin/Domains) and delete
         * everything else.
         */
        public const val MATCH_BEHAVIOR_PRESERVE = 1

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(MATCH_BEHAVIOR_DELETE, MATCH_BEHAVIOR_PRESERVE)
        annotation class MatchBehavior
    }

    /**
     * Builder for {@link DeletionRequest} objects.
     *
     * @param deletionMode {@link DeletionMode} Set the match behavior for the supplied params.
     *   {@link #DELETION_MODE_ALL}: All data associated with the selected records will be deleted.
     *   {@link #DELETION_MODE_EXCLUDE_INTERNAL_DATA}: All data except the internal system data
     *   (e.g. rate limits) associated with the selected records will be deleted.
     * @param matchBehavior {@link MatchBehavior} Set the match behavior for the supplied params.
     *   {@link #MATCH_BEHAVIOR_DELETE}: This option will use the supplied params (Origin URIs &
     *   Domain URIs) for selecting records for deletion. {@link #MATCH_BEHAVIOR_PRESERVE}: This
     *   option will preserve the data associated with the supplied params (Origin URIs & Domain
     *   URIs) and select remaining records for deletion.
     */
    @RequiresApi(android.os.Build.VERSION_CODES.O)
    public class Builder
    constructor(
        @DeletionMode private val deletionMode: Int,
        @MatchBehavior private val matchBehavior: Int
    ) {
        private var start: Instant = Instant.MIN
        private var end: Instant = Instant.MAX
        private var domainUris: List<Uri> = emptyList()
        private var originUris: List<Uri> = emptyList()

        /**
         * Sets the start of the deletion range. Not setting this or passing in {@link
         * java.time.Instant#MIN} will cause everything from the oldest record to the specified end
         * be deleted.
         */
        fun setStart(start: Instant): Builder = apply { this.start = start }

        /**
         * Sets the end of the deletion range. Not setting this or passing in {@link
         * java.time.Instant#MAX} will cause everything from the specified start until the newest
         * record to be deleted.
         */
        fun setEnd(end: Instant): Builder = apply { this.end = end }

        /**
         * Set the list of domain URI which will be used for matching. These will be matched with
         * records using the same domain or any subdomains. E.g. If domainUri is {@code
         * https://example.com}, then {@code https://a.example.com}, {@code https://example.com} and
         * {@code https://b.example.com} will match; {@code https://abcexample.com} will NOT match.
         * A null or empty list will match everything.
         */
        fun setDomainUris(domainUris: List<Uri>): Builder = apply { this.domainUris = domainUris }

        /**
         * Set the list of origin URI which will be used for matching. These will be matched with
         * records using the same origin only, i.e. subdomains won't match. E.g. If originUri is
         * {@code https://a.example.com}, then {@code https://a.example.com} will match; {@code
         * https://example.com}, {@code https://b.example.com} and {@code https://abcexample.com}
         * will NOT match. A null or empty list will match everything.
         */
        fun setOriginUris(originUris: List<Uri>): Builder = apply { this.originUris = originUris }

        /** Builds a {@link DeletionRequest} instance. */
        fun build(): DeletionRequest {
            return DeletionRequest(deletionMode, matchBehavior, start, end, domainUris, originUris)
        }
    }
}
