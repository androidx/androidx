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

package androidx.window.demo.embedding

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Activity to show a dialog. */
class ExpandedDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    private fun showDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Dialog in expanded activity")
            .setMessage("To demo showing dialog that can expand over a split")
            .setNeutralButton("Close") { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }

        dialog.show()
    }
}
