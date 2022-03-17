/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications

import android.content.ComponentName
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.SystemDataSources.DataSourceId
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.DefaultComplicationDataSourcePolicyWireFormat
import java.util.ArrayList

/**
 * A watch face may wish to try and set one or more non-system data sources as the default
 * data source for a complication. If a complication data source can't be used for some reason (e.g.
 * it isn't installed or it doesn't support the requested type, or the watch face lacks the
 * necessary permission) then the next one will be tried. A system complication data source acts
 * as a final fallback in case no non-system data sources can be used.
 *
 * If the DefaultComplicationDataSourcePolicy is empty then no default is set.
 */
public class DefaultComplicationDataSourcePolicy {
    /** First of two non-system data sources to be tried in turn. Set to null if not required. */
    public val primaryDataSource: ComponentName?

    /**
     * The default [ComplicationType] for [primaryDataSource]. Note Pre-R this will be ignored in
     * favour of [systemDataSourceFallbackDefaultType].
     */
    public val primaryDataSourceDefaultType: ComplicationType?

    /** Second of two non-system data sources to be tried in turn. Set to null if not required. */
    public val secondaryDataSource: ComponentName?

    /**
     * The default [ComplicationType] for [secondaryDataSource]. Note Pre-R this will be ignored in
     * favour of [systemDataSourceFallbackDefaultType].
     */
    public val secondaryDataSourceDefaultType: ComplicationType?

    /** Fallback in case none of the non-system data sources could be used. */
    @DataSourceId
    public val systemDataSourceFallback: Int

    /** The default [ComplicationType] for [systemDataSourceFallback]. */
    public val systemDataSourceFallbackDefaultType: ComplicationType

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        dataSources: List<ComponentName>,
        @DataSourceId systemDataSourceFallback: Int,
        primaryDataSourceDefaultType: ComplicationType,
        secondaryDataSourceDefaultType: ComplicationType,
        systemDataSourceFallbackDefaultType: ComplicationType
    ) {
        this.primaryDataSource = if (dataSources.isNotEmpty()) dataSources[0] else null
        this.primaryDataSourceDefaultType = primaryDataSourceDefaultType

        this.secondaryDataSource = if (dataSources.size >= 2) dataSources[1] else null
        this.secondaryDataSourceDefaultType = secondaryDataSourceDefaultType

        this.systemDataSourceFallback = systemDataSourceFallback
        this.systemDataSourceFallbackDefaultType =
            systemDataSourceFallbackDefaultType
    }

    /** No default complication data source. */
    public constructor() {
        primaryDataSource = null
        primaryDataSourceDefaultType = null
        secondaryDataSource = null
        secondaryDataSourceDefaultType = null
        systemDataSourceFallback = NO_DEFAULT_PROVIDER
        systemDataSourceFallbackDefaultType = ComplicationType.NOT_CONFIGURED
    }

    /**
     * Uses [systemProvider] as the default complication data source.
     */
    @Deprecated(
        "Use a constructor that sets the DefaultTypes",
        ReplaceWith("DefaultComplicationDataSourcePolicy(Int, ComplicationType)")
    )
    public constructor(@DataSourceId systemProvider: Int) {
        primaryDataSource = null
        secondaryDataSource = null
        systemDataSourceFallback = systemProvider
        primaryDataSourceDefaultType = null
        secondaryDataSourceDefaultType = null
        systemDataSourceFallbackDefaultType = ComplicationType.NOT_CONFIGURED
    }

    /**
     * Uses a system data source as the default complication data source.
     *
     * @param systemDataSource The system [DataSourceId] data source to use.
     * @param systemDataSourceDefaultType The initial default [ComplicationType].
     */
    public constructor(
        @DataSourceId systemDataSource: Int,
        systemDataSourceDefaultType: ComplicationType
    ) {
        primaryDataSource = null
        primaryDataSourceDefaultType = null
        secondaryDataSource = null
        secondaryDataSourceDefaultType = null
        systemDataSourceFallback = systemDataSource
        systemDataSourceFallbackDefaultType = systemDataSourceDefaultType
    }

    /**
     * Attempts to use [dataSource] as the default complication complication data source, if not
     * present then [systemDataSourceFallback] will be used instead.
     */
    @Deprecated(
        "Use a constructor that sets the DefaultTypes",
        ReplaceWith(
            "DefaultComplicationDataSourcePolicy(ComponentName, ComplicationType, Int," +
                " ComplicationType)"
        )
    )
    public constructor(
        dataSource: ComponentName,
        @DataSourceId systemDataSourceFallback: Int
    ) {
        primaryDataSource = dataSource
        primaryDataSourceDefaultType = null
        secondaryDataSource = null
        secondaryDataSourceDefaultType = null
        this.systemDataSourceFallback = systemDataSourceFallback
        systemDataSourceFallbackDefaultType = ComplicationType.NOT_CONFIGURED
    }

    /**
     * Attempts to use [primaryDataSource] as the default complication complication data source, if
     * not present then [systemDataSourceFallback] will be used instead.
     *
     * @param primaryDataSource The data source to try.
     * @param primaryDataSourceDefaultType The default [ComplicationType] if
     * primaryDataSource is selected. Note Pre-R this will be ignored in favour of
     * [systemDataSourceFallbackDefaultType].
     * @param systemDataSourceFallback The system data source to fall back on if neither provider is
     * available.
     * @param systemDataSourceFallbackDefaultType The default [ComplicationType] if
     * systemDataSourceFallback is selected.
    */
    public constructor(
        primaryDataSource: ComponentName,
        primaryDataSourceDefaultType: ComplicationType,
        @DataSourceId systemDataSourceFallback: Int,
        systemDataSourceFallbackDefaultType: ComplicationType
    ) {
        this.primaryDataSource = primaryDataSource
        this.primaryDataSourceDefaultType = primaryDataSourceDefaultType
        secondaryDataSource = null
        secondaryDataSourceDefaultType = null
        this.systemDataSourceFallback = systemDataSourceFallback
        this.systemDataSourceFallbackDefaultType = systemDataSourceFallbackDefaultType
    }

    /**
     * Attempts to use [primaryDataSource] as the default complication data source, if not present
     * then [secondaryDataSource] will be tried and if that's not present then
     * [systemDataSourceFallback] will be used instead.
     */
    @Deprecated(
        "Use a constructor that sets the DefaultTypes",
        ReplaceWith(
            "DefaultComplicationDataSourcePolicy(ComponentName, ComplicationType, ComponentName, " +
                "ComplicationType, Int, ComplicationType)"
        )
    )
    public constructor(
        primaryDataSource: ComponentName,
        secondaryDataSource: ComponentName,
        @DataSourceId systemDataSourceFallback: Int
    ) {
        this.primaryDataSource = primaryDataSource
        this.secondaryDataSource = secondaryDataSource
        this.systemDataSourceFallback = systemDataSourceFallback
        primaryDataSourceDefaultType = null
        secondaryDataSourceDefaultType = null
        systemDataSourceFallbackDefaultType = ComplicationType.NOT_CONFIGURED
    }

    /**
     * Attempts to use [primaryDataSource] as the default complication data source, if not present
     * then [secondaryDataSource] will be tried and if that's not present then
     * [systemDataSourceFallback] will be used instead.
     *
     * @param primaryDataSource The first data source to try.
     * @param primaryDataSourceDefaultType The default [ComplicationType] if
     * primaryDataSource is selected. Note Pre-R this will be ignored in favour of
     * [systemDataSourceFallbackDefaultType].
     * @param secondaryDataSource The second data source to try.
     * @param secondaryDataSourceDefaultType The default [ComplicationType] if
     * secondaryDataSource is selected. Note Pre-R this will be ignored in favour of
     * [systemDataSourceFallbackDefaultType].
     * @param systemDataSourceFallback The system data source to fall back on if neither provider is
     * available.
     * @param systemDataSourceFallbackDefaultType The default [ComplicationType] if
     * systemDataSourceFallback is selected.
     */
    public constructor(
        primaryDataSource: ComponentName,
        primaryDataSourceDefaultType: ComplicationType,
        secondaryDataSource: ComponentName,
        secondaryDataSourceDefaultType: ComplicationType,
        @DataSourceId systemDataSourceFallback: Int,
        systemDataSourceFallbackDefaultType: ComplicationType
    ) {
        this.primaryDataSource = primaryDataSource
        this.primaryDataSourceDefaultType = primaryDataSourceDefaultType
        this.secondaryDataSource = secondaryDataSource
        this.secondaryDataSourceDefaultType = secondaryDataSourceDefaultType
        this.systemDataSourceFallback = systemDataSourceFallback
        this.systemDataSourceFallbackDefaultType = systemDataSourceFallbackDefaultType
    }

    /** Whether or not this DefaultComplicationDataSourcePolicy contains a default data source. */
    public fun isEmpty(): Boolean =
        primaryDataSource == null && systemDataSourceFallback == NO_DEFAULT_PROVIDER

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun dataSourcesAsList(): ArrayList<ComponentName> = ArrayList<ComponentName>().apply {
        primaryDataSource?.let { add(it) }
        secondaryDataSource?.let { add(it) }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(wireFormat: DefaultComplicationDataSourcePolicyWireFormat) : this(
        wireFormat.mDefaultDataSourcesToTry,
        wireFormat.mFallbackSystemDataSource,
        ComplicationType.fromWireType(wireFormat.mPrimaryDataSourceDefaultType),
        ComplicationType.fromWireType(wireFormat.mSecondaryDataSourceDefaultType),
        ComplicationType.fromWireType(wireFormat.mDefaultType)
    ) {
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): DefaultComplicationDataSourcePolicyWireFormat {
        val systemDataSourceFallbackDefaultType = systemDataSourceFallbackDefaultType
            .toWireComplicationType()

        return DefaultComplicationDataSourcePolicyWireFormat(
            dataSourcesAsList(),
            systemDataSourceFallback,
            systemDataSourceFallbackDefaultType,
            primaryDataSourceDefaultType
                ?.toWireComplicationType() ?: systemDataSourceFallbackDefaultType,
            secondaryDataSourceDefaultType
                ?.toWireComplicationType() ?: systemDataSourceFallbackDefaultType
        )
    }

    override fun toString(): String =
        "DefaultComplicationDataSourcePolicy[" +
            "primary($primaryDataSource, $primaryDataSourceDefaultType), " +
            "secondary($secondaryDataSource, $secondaryDataSourceDefaultType), " +
            "system($systemDataSourceFallback, $systemDataSourceFallbackDefaultType)]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultComplicationDataSourcePolicy

        if (primaryDataSource != other.primaryDataSource) return false
        if (secondaryDataSource != other.secondaryDataSource) return false
        if (systemDataSourceFallback != other.systemDataSourceFallback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryDataSource?.hashCode() ?: 0
        result = 31 * result + (secondaryDataSource?.hashCode() ?: 0)
        result = 31 * result + systemDataSourceFallback
        return result
    }

    internal companion object {
        internal const val NO_DEFAULT_PROVIDER = SystemDataSources.NO_DATA_SOURCE
    }
}