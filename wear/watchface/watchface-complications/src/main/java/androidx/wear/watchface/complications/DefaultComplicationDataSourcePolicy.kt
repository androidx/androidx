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

    /** Second of two non-system data sources to be tried in turn. Set to null if not required. */
    public val secondaryDataSource: ComponentName?

    /** Fallback in case none of the non-system data sources could be used. */
    @DataSourceId
    public val systemDataSourceFallback: Int

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        dataSources: List<ComponentName>,
        @DataSourceId systemProviderFallback: Int
    ) {
        this.primaryDataSource = if (dataSources.isNotEmpty()) dataSources[0] else null
        this.secondaryDataSource = if (dataSources.size >= 2) dataSources[1] else null
        this.systemDataSourceFallback = systemProviderFallback
    }

    /** No default complication data source. */
    public constructor() {
        primaryDataSource = null
        secondaryDataSource = null
        systemDataSourceFallback = NO_DEFAULT_PROVIDER
    }

    /**
     * Uses systemProvider as the default complication data source.
     */
    public constructor(@DataSourceId systemProvider: Int) {
        primaryDataSource = null
        secondaryDataSource = null
        systemDataSourceFallback = systemProvider
    }

    /**
     * Attempts to use [dataSource] as the default complication complication data source, if not
     * present then [systemDataSourceFallback] will be used instead.
     */
    public constructor(
        dataSource: ComponentName,
        @DataSourceId systemDataSourceFallback: Int
    ) {
        primaryDataSource = dataSource
        secondaryDataSource = null
        this.systemDataSourceFallback = systemDataSourceFallback
    }

    /**
     * Attempts to use [primaryDataSource] as the default complication data source, if not present
     * then [secondaryDataSource] will be tried and if that's not present then
     * [systemDataSourceFallback] will be used instead.
     */
    public constructor(
        primaryDataSource: ComponentName,
        secondaryDataSource: ComponentName,
        @DataSourceId systemDataSourceFallback: Int
    ) {
        this.primaryDataSource = primaryDataSource
        this.secondaryDataSource = secondaryDataSource
        this.systemDataSourceFallback = systemDataSourceFallback
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