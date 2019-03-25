/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app.test

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.test.R
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import androidx.testutils.RecreatedActivity

class LoaderActivity : RecreatedActivity(R.layout.activity_loader),
    LoaderManager.LoaderCallbacks<String> {

    lateinit var textView: TextView
    lateinit var textViewB: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textView = findViewById(R.id.textA)
        textViewB = findViewById(R.id.textB)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, TextLoaderFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this).initLoader(TEXT_LOADER_ID, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<String> {
        return TextLoader(this)
    }

    override fun onLoadFinished(loader: Loader<String>, data: String) {
        textView.text = data
    }

    override fun onLoaderReset(loader: Loader<String>) {
    }

    internal class TextLoader(context: Context) : AsyncTaskLoader<String>(context) {

        override fun onStartLoading() {
            forceLoad()
        }

        override fun loadInBackground(): String? {
            return "Loaded!"
        }
    }

    class TextLoaderFragment : Fragment(R.layout.fragment_c),
        LoaderManager.LoaderCallbacks<String> {
        lateinit var textView: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            LoaderManager.getInstance(this).initLoader(TEXT_LOADER_ID, null, this)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            textView = view.findViewById(R.id.textC)
        }

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<String> {
            return TextLoader(requireContext())
        }

        override fun onLoadFinished(loader: Loader<String>, data: String) {
            textView.text = data
        }

        override fun onLoaderReset(loader: Loader<String>) {}
    }

    companion object {
        private const val TEXT_LOADER_ID = 14

        val activity get() = RecreatedActivity.activity
    }
}
