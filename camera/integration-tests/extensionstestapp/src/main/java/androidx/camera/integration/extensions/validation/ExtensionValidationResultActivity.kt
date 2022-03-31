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
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_RESULT_MAP
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_TEST_RESULT
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INVALID_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.getLensFacingStringFromInt
import androidx.camera.integration.extensions.validation.TestResults.Companion.INVALID_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_NOT_SUPPORTED
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_NOT_TESTED
import androidx.core.app.ActivityCompat

class ExtensionValidationResultActivity : AppCompatActivity() {
    private val extensionTestResultMap = linkedMapOf<Int, Int>()
    private val result = Intent()
    private var lensFacing = INVALID_LENS_FACING
    private lateinit var cameraId: String
    private lateinit var adapter: BaseAdapter
    private val imageValidationActivityRequestCode =
        ImageValidationActivity::class.java.hashCode() % 1000

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_listview)

        cameraId = intent.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        lensFacing = intent.getIntExtra(INTENT_EXTRA_KEY_LENS_FACING, INVALID_LENS_FACING)

        val resultMap =
            intent.getSerializableExtra(INTENT_EXTRA_KEY_RESULT_MAP) as HashMap<Int, Int>
        resultMap.forEach {
            extensionTestResultMap[it.key] = it.value
        }

        result.putExtra(INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
        result.putExtra(INTENT_EXTRA_KEY_RESULT_MAP, extensionTestResultMap)
        val requestCode = intent.getIntExtra(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
        setResult(requestCode, result)

        supportActionBar?.title = "${resources.getString(R.string.extensions_validator)}"
        supportActionBar!!.subtitle = "Camera $cameraId [${getLensFacingStringFromInt(lensFacing)}]"

        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        adapter = ExtensionValidationResultAdapter(layoutInflater, extensionTestResultMap)

        val listView = findViewById<ListView>(R.id.listView)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                if (extensionTestResultMap.values.elementAt(position) == TEST_RESULT_NOT_SUPPORTED
                ) {
                    Toast.makeText(this, "Not supported!", Toast.LENGTH_SHORT).show()
                    return@OnItemClickListener
                }

                startCaptureValidationActivity(
                    cameraId,
                    extensionTestResultMap.keys.elementAt(position)
                )
            }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != imageValidationActivityRequestCode) {
            return
        }

        val extensionMode =
            data?.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, INVALID_EXTENSION_MODE)!!
        val testResult =
            data.getIntExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_NOT_TESTED)
        if (testResult != TEST_RESULT_NOT_TESTED) {
            extensionTestResultMap[extensionMode] = testResult
            adapter.notifyDataSetChanged()
        }
    }

    private fun startCaptureValidationActivity(cameraId: String, mode: Int) {
        val intent = Intent(this, ImageValidationActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
        intent.putExtra(INTENT_EXTRA_KEY_LENS_FACING, lensFacing)
        intent.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, mode)
        intent.putExtra(INTENT_EXTRA_KEY_REQUEST_CODE, imageValidationActivityRequestCode)

        ActivityCompat.startActivityForResult(
            this,
            intent,
            imageValidationActivityRequestCode,
            null
        )
    }
}
