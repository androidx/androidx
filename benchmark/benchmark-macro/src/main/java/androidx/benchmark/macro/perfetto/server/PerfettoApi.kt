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

package androidx.benchmark.macro.perfetto.server

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import perfetto.protos.AppendTraceDataResult
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.DisableAndReadMetatraceResult
import perfetto.protos.QueryArgs
import perfetto.protos.QueryResult
import perfetto.protos.StatusResult
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Perfetto trace_shell_processor api exposed in the http server implementation
 */
internal interface PerfettoApi {

    @POST("/query")
    fun query(@Body queryArgs: QueryArgs): Call<QueryResult>

    @POST("/compute_metric")
    fun computeMetric(@Body computeMetricArgs: ComputeMetricArgs): Call<ComputeMetricResult>

    @POST("/parse")
    fun parse(@Body bytes: RequestBody): Call<AppendTraceDataResult>

    @GET("/notify_eof")
    fun notifyEof(): Call<Unit>

    @GET("/status")
    fun status(): Call<StatusResult>

    @GET("/enable_metatrace")
    fun enableMetatrace(): Call<String>

    @GET("/disable_and_read_metatrace")
    fun disableAndReadMetatrace(): Call<DisableAndReadMetatraceResult>

    @GET("/restore_initial_tables")
    fun restoreInitialTables(): Call<Unit>

    companion object {

        private const val READ_TIMEOUT_SECONDS = 300L

        fun create(address: String): PerfettoApi {
            return Retrofit.Builder()
                .baseUrl(address)
                .addConverterFactory(WireConverterFactory.create())
                .client(OkHttpClient.Builder()
                    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build())
                .build()
                .create(PerfettoApi::class.java)
        }
    }
}
