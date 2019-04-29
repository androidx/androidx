/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.testapp

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController

class LearnMoreDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val myarg = arguments?.getString("myarg")
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.learn_more)
            .setMessage(myarg)
            .setPositiveButton(R.string.learn_more_positive) { _, _ -> }
            .setNeutralButton(R.string.learn_more_neutral) { _, _ ->
                findNavController().navigate(R.id.learn_more_about_android)
            }
            .create()
    }
}
