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

package androidx.camera.integration.extensions.validation

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_LENS_FACING
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_RESULT_MAP
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_TEST_TYPE
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_SUPPORTED
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Activity to list all supported CameraX/Camera2 Extensions and camera ids combination items.
 *
 * Clicking a list item will launch the CameraValidationResultActivity to list the supported
 * extension modes of the selected item.
 */
class CameraValidationResultActivity : AppCompatActivity() {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var adapter: BaseAdapter
    private lateinit var testResults: TestResults
    private lateinit var cameraLensFacingMap: LinkedHashMap<String, Int>
    private lateinit var cameraExtensionResultMap: LinkedHashMap<Pair<String, String>,
        LinkedHashMap<Int, Int>>
    private val extensionValidationActivityRequestCode =
        ExtensionValidationResultActivity::class.java.hashCode() % 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_listview)

        supportActionBar?.title = resources.getString(R.string.extensions_validator)
        initialize()
    }

    private fun initialize() {
        lifecycleScope.launch {
            cameraProvider =
                ProcessCameraProvider.getInstance(this@CameraValidationResultActivity).await()
            extensionsManager = ExtensionsManager.getInstanceAsync(
                this@CameraValidationResultActivity,
                cameraProvider
            ).await()

            testResults = TestResults(this@CameraValidationResultActivity)
            testResults.loadTestResults(cameraProvider, extensionsManager)

            cameraLensFacingMap = testResults.getCameraLensFacingMap()
            cameraExtensionResultMap = testResults.getCameraExtensionResultMap()

            if (cameraExtensionResultMap.isEmpty()) {
                showLoadErrorMessage()
                return@launch
            }

            val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            adapter = CameraValidationResultAdapter(
                layoutInflater,
                cameraLensFacingMap,
                cameraExtensionResultMap
            )

            val listView = findViewById<ListView>(R.id.listView)
            listView.adapter = adapter
            listView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    val (testType, cameraId) = cameraExtensionResultMap.keys.elementAt(position)
                    if (!isAnyExtensionModeSupported(testType, cameraId)) {
                        Toast.makeText(
                            this@CameraValidationResultActivity,
                            "No extension mode is supported by the camera!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnItemClickListener
                    }

                    val intent = Intent(
                        this@CameraValidationResultActivity,
                        ExtensionValidationResultActivity::class.java
                    )
                    intent.putExtra(
                        INTENT_EXTRA_KEY_TEST_TYPE,
                        testType
                    )
                    intent.putExtra(
                        INTENT_EXTRA_KEY_CAMERA_ID,
                        cameraId
                    )
                    intent.putExtra(
                        INTENT_EXTRA_KEY_LENS_FACING,
                        cameraLensFacingMap[cameraId]
                    )
                    intent.putExtra(
                        INTENT_EXTRA_KEY_RESULT_MAP,
                        cameraExtensionResultMap.values.elementAt(position)
                    )
                    intent.putExtra(
                        INTENT_EXTRA_KEY_REQUEST_CODE,
                        extensionValidationActivityRequestCode
                    )

                    ActivityCompat.startActivityForResult(
                        this@CameraValidationResultActivity,
                        intent,
                        extensionValidationActivityRequestCode,
                        null
                    )
                }
        }
    }

    private fun showLoadErrorMessage() {
        val listView = findViewById<ListView>(R.id.listView)
        val textView = findViewById<TextView>(R.id.textView)

        listView.visibility = View.GONE
        textView.visibility = View.VISIBLE
    }

    private fun isAnyExtensionModeSupported(testType: String, cameraId: String): Boolean {
        cameraExtensionResultMap[Pair(testType, cameraId)]?.forEach {
            if (it.value != TEST_RESULT_NOT_SUPPORTED) {
                return true
            }
        }
        return false
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != extensionValidationActivityRequestCode) {
            return
        }

        val testType = data?.getStringExtra(INTENT_EXTRA_KEY_TEST_TYPE)!!
        val cameraId = data.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        val extensionTestResultMap = cameraExtensionResultMap[Pair(testType, cameraId)]

        @Suppress("UNCHECKED_CAST")
        val map = data.getSerializableExtra(INTENT_EXTRA_KEY_RESULT_MAP) as HashMap<Int, Int>
        map.forEach {
            extensionTestResultMap?.put(it.key, it.value)
        }

        adapter.notifyDataSetChanged()
        testResults.saveTestResults(cameraExtensionResultMap)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_validator_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export -> {
                val outputFilePath = testResults.exportTestResults(contentResolver)
                if (outputFilePath != null) {
                    Toast.makeText(
                        this,
                        "Test results have been saved in $outputFilePath!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "Failed to export the test results!", Toast.LENGTH_LONG)
                        .show()
                }
                true
            }
            R.id.menu_reset -> {
                testResults.resetTestResults(cameraProvider, extensionsManager)
                adapter.notifyDataSetChanged()
                true
            }
            R.id.menu_extensions_app -> {
                val intent = Intent(this, CameraExtensionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        fun getLensFacingStringFromInt(lensFacing: Int): String = when (lensFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> throw IllegalArgumentException("Invalid lens facing!!")
        }
    }
}
