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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolder
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderStateImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderValueImpl
import androidx.camera.integration.camera2.pipe.transformations.DataTransformations1D
import androidx.camera.integration.camera2.pipe.dataholders.KeyValueDataHolder
import androidx.camera.integration.camera2.pipe.transformations.DataTransformationsKeyValue
import androidx.camera.integration.camera2.pipe.visualizations.KeyValueView
import androidx.camera.integration.camera2.pipe.visualizations.Paints
import androidx.camera.integration.camera2.pipe.visualizations.StateGraphView
import androidx.camera.integration.camera2.pipe.visualizations.ValueGraphView
import kotlinx.android.synthetic.main.activity_main.graphs
import kotlinx.android.synthetic.main.activity_main.key_values
import kotlinx.android.synthetic.main.graph_layout.view.graph_layout
import kotlinx.android.synthetic.main.graph_layout.view.graph_view_layout
import kotlinx.android.synthetic.main.graph_layout.view.top_row_layout
import kotlinx.android.synthetic.main.key_value_layout.view.key_name
import kotlinx.android.synthetic.main.key_value_layout.view.key_value_layout
import kotlinx.android.synthetic.main.key_value_layout.view.value_layout

class MainActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var dataListener: DataListener
    private lateinit var dataGenerator: DataGenerator
    private lateinit var paints: Paints

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        paints = Paints(this)

        val beginTimeNanos = System.nanoTime()

        /** Initialize manager of data holders for visualizations, and data holders themselves */
        dataManager = DataManager(VisualizationDefaults)
        dataManager.initializeDataHolders()

        /** Initialize the listener of new data */
        dataListener = DataListener(
            dataManager,
            DataTransformationsKeyValue,
            DataTransformations1D,
            beginTimeNanos
        )

        /** Initialize the data generator and start generating data, noting the begin timestamp */
        dataGenerator = DataGenerator(
            dataListener,
            VisualizationDefaults
        )
        dataGenerator.beginTimeNanos = beginTimeNanos
        dataGenerator.runDataGenerators()

        /** Set up the views powered by data holders, and plug in the data holders */
        setUpVisualizations()
    }

    override fun onPause() {
        super.onPause()
        dataGenerator.quitDataGenerators()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataGenerator.quitDataGenerators()
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
                    this,
                    dataHolder,
                    paints
                )

            val keyValueLayout = View.inflate(this, R.layout.key_value_layout, null)
            keyValueLayout.key_name.text = "${it.key.name}    "

            val keyValueViewParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, heightPixels)
            keyValueView.layoutParams = keyValueViewParams

            keyValueLayout.key_value_layout.value_layout.addView(keyValueView)
            key_values.addView(keyValueLayout)
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
                    this,
                    dataGenerator.beginTimeNanos,
                    dataHolder,
                    paints
                )
                is GraphDataHolderStateImpl -> StateGraphView(
                    this,
                    dataGenerator.beginTimeNanos,
                    dataHolder,
                    paints = paints
                )
                else -> throw Exception("Visualization is not supported for this graphDataHolder " +
                        "implementation")
            }

            val graphLayout = View.inflate(this, R.layout.graph_layout, null)

            val graphViewParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPixels
            )
            graphViewParams.addRule(RelativeLayout.BELOW, graphLayout.top_row_layout.id)
            graphView.layoutParams = graphViewParams

            graphLayout.graph_view_layout.addView(graphView)
            graphLayout.graph_layout.setBackgroundResource(R.drawable.graph_background)

            val graphLayoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams
                .WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            graphLayoutParams.setMargins(0, 0, 0, 20)
            graphLayout.layoutParams = graphLayoutParams

            val keyValueDataHolder = keyValeDataHolders[key]
            if (keyValueDataHolder != null) {
                val keyValueView =
                    KeyValueView(
                        this,
                        keyValueDataHolder,
                        paints
                    )
                val keyValueLayout = View.inflate(this, R.layout.key_value_layout, null)
                keyValueLayout.key_name.text = "${it.key.name} "

                val keyValueViewParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams
                    .MATCH_PARENT, 30)
                keyValueView.layoutParams = keyValueViewParams

                keyValueLayout.key_value_layout.value_layout.addView(keyValueView)
                graphLayout.top_row_layout.addView(keyValueLayout)
            }

            graphs.addView(graphLayout)
        }
    }
}