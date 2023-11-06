/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolder
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderStateImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderValueImpl
import androidx.camera.integration.camera2.pipe.dataholders.KeyValueDataHolder
import androidx.camera.integration.camera2.pipe.transformations.DataTransformations1D
import androidx.camera.integration.camera2.pipe.transformations.DataTransformationsKeyValue
import androidx.camera.integration.camera2.pipe.visualizations.KeyValueView
import androidx.camera.integration.camera2.pipe.visualizations.Paints
import androidx.camera.integration.camera2.pipe.visualizations.StateGraphView
import androidx.camera.integration.camera2.pipe.visualizations.ValueGraphView

class DataVisualizations(activity: Activity) {
    private var dataManager: DataManager
    private var dataListener: DataListener
    private var dataGenerator: DataGenerator
    private var paints: Paints

    private val context: Context

    init {
        context = activity
        paints = Paints(activity)

        val beginTimeNanos = System.nanoTime()

        // Initialize manager of data holders for visualizations, and data holders themselves
        dataManager = DataManager(VisualizationDefaults)
        dataManager.initializeDataHolders()

        // Initialize the listener of new data
        dataListener = DataListener(
            dataManager,
            DataTransformationsKeyValue,
            DataTransformations1D,
            beginTimeNanos
        )

        // Initialize the data generator and start generating data, noting the begin timestamp
        dataGenerator = DataGenerator(
            dataListener,
            VisualizationDefaults
        )
        dataGenerator.beginTimeNanos = beginTimeNanos

        // Set up the views powered by data holders, and plug in the data holders
        setUpVisualizations()
    }

    fun start() {
        // TODO(codelogic): Attach and start the data visualizer.
//        dataGenerator.runDataGenerators()
    }

    fun stop() {
        // TODO(codelogic): Unregister and clear visualizations
    }

    fun close() {
    }

    /** Sets up all the different types of visualizations */
    private fun setUpVisualizations() {
        setUpKeyValueVisualizations(dataManager.keyValueDataHolders, dataManager.graphDataHolders)
        setUpGraphVisualizations(dataManager.keyValueDataHolders, dataManager.graphDataHolders)
    }

    private fun setUpKeyValueVisualizations(
        keyValeDataHolders: Map<CameraMetadataKey, KeyValueDataHolder>,
        graphDataHolders: Map<CameraMetadataKey, GraphDataHolder>
    ) {
        val heightPixels = 30

        keyValeDataHolders.forEach loop@{
            if (graphDataHolders.containsKey(it.key)) return@loop
            val dataHolder = it.value
            val keyValueView =
                KeyValueView(
                    context,
                    dataHolder,
                    paints
                )

            val keyValueLayout = View.inflate(context, R.layout.key_value_layout, null)
            keyValueLayout.findViewById<TextView>(R.id.key_name).text =
                context.getString(R.string.data_key_name, it.key.name)

            val keyValueViewParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams
                    .MATCH_PARENT,
                heightPixels
            )
            keyValueView.layoutParams = keyValueViewParams

            keyValueLayout.findViewById<RelativeLayout>(R.id.value_layout).addView(keyValueView)
//            kotlinx.android.synthetic.main.activity_main. .addView(keyValueLayout)
        }
    }

    /** Sets up the graph visualizations specifically */
    private fun setUpGraphVisualizations(
        keyValeDataHolders: Map<CameraMetadataKey, KeyValueDataHolder>,
        graphDataHolders: Map<CameraMetadataKey, GraphDataHolder>
    ) {
        val heightPixels = 110

        graphDataHolders.forEach {
            val key = it.key
            val dataHolder = it.value
            val graphView = when (dataHolder) {
                is GraphDataHolderValueImpl -> ValueGraphView(
                    context,
                    dataGenerator.beginTimeNanos,
                    dataHolder,
                    paints
                )
                is GraphDataHolderStateImpl -> StateGraphView(
                    context,
                    dataGenerator.beginTimeNanos,
                    dataHolder,
                    paints = paints
                )
                else -> throw Exception(
                    "Visualization is not supported for this graphDataHolder " +
                        "implementation"
                )
            }

            val graphLayout = View.inflate(context, R.layout.graph_layout, null)

            val graphViewParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPixels
            )
            graphViewParams.addRule(RelativeLayout.BELOW, R.id.top_row_layout)
            graphView.layoutParams = graphViewParams

            graphLayout.findViewById<RelativeLayout>(R.id.graph_view_layout).addView(graphView)
            graphLayout.findViewById<RelativeLayout>(R.id.graph_layout)
                .setBackgroundResource(R.drawable.graph_background)

            val graphLayoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams
                    .WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            graphLayoutParams.setMargins(0, 0, 0, 20)
            graphLayout.layoutParams = graphLayoutParams

            val keyValueDataHolder = keyValeDataHolders[key]
            if (keyValueDataHolder != null) {
                val keyValueView =
                    KeyValueView(
                        context,
                        keyValueDataHolder,
                        paints
                    )
                val keyValueLayout = View.inflate(context, R.layout.key_value_layout, null)
                keyValueLayout.findViewById<TextView>(R.id.key_name).text =
                    context.getString(R.string.data_key_name, it.key.name)

                val keyValueViewParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams
                        .MATCH_PARENT,
                    30
                )
                keyValueView.layoutParams = keyValueViewParams

                keyValueLayout.findViewById<RelativeLayout>(R.id.value_layout).addView(keyValueView)
                graphLayout.findViewById<RelativeLayout>(R.id.top_row_layout)
                    .addView(keyValueLayout)
            }

//            graphs.addView(graphLayout)
        }
    }
}
