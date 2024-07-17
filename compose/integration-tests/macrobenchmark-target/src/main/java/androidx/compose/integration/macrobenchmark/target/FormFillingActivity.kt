/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class FormFillingActivity : ComponentActivity() {
    private lateinit var lazyListState: LazyListState
    private lateinit var formView: FormView
    private lateinit var type: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rowHeightDp: Dp
        val fontSize: TextUnit
        when (intent.getIntExtra(MODE, 0)) {
            FRAME_MEASUREMENT_MODE -> {
                // Larger number of rows to stress the system while measuring frame info.
                rowHeightDp = 30.dp
                fontSize = 5.sp
            }
            CREATE_ANI_MODE -> {
                // Smaller number of rows so that we have no dropped frames.
                rowHeightDp = 100.dp
                fontSize = 10.sp
            }
            else -> error("Invalid Mode")
        }

        type = checkNotNull(intent.getStringExtra(TYPE)) { "No type specified." }
        when (type) {
            COMPOSE ->
                setContent {
                    lazyListState = rememberLazyListState()
                    FormComposable(lazyListState, rowHeightDp, fontSize)
                }
            VIEW -> {
                val rowHeightPx = rowHeightDp.value * resources.displayMetrics.densityDpi / 160f
                formView = FormView(this, rowHeightPx, fontSize)
                setContentView(formView)
            }
            else -> error("Unknown Type")
        }
    }

    override fun onNewIntent(intent: Intent) {
        when (type) {
            COMPOSE -> lazyListState.requestScrollToItem(lazyListState.firstVisibleItemIndex + 100)
            VIEW -> formView.scrollToPosition(formView.lastVisibleItemIndex + 100)
            else -> error("Unknown Type")
        }
        super.onNewIntent(intent)
    }

    @Composable
    private fun FormComposable(lazyListState: LazyListState, rowHeight: Dp, fontSize: TextUnit) {
        val textStyle = LocalTextStyle.current.copy(fontSize = fontSize)
        LazyColumn(state = lazyListState) {
            items(data.size) { index ->
                val person = data[index]
                Row(
                    modifier =
                        Modifier.height(rowHeight).semantics {
                            customActions =
                                listOf(CustomAccessibilityAction("customAction") { false })
                        }
                ) {
                    BasicTextField(
                        value = person.title,
                        onValueChange = { person.title = it },
                        textStyle = textStyle
                    )
                    BasicTextField(
                        value = person.firstName,
                        onValueChange = { person.firstName = it },
                        textStyle = textStyle
                    )
                    BasicTextField(
                        value = person.middleName,
                        onValueChange = { person.middleName = it },
                        textStyle = textStyle
                    )
                    BasicTextField(
                        value = person.lastName,
                        onValueChange = { person.lastName = it },
                        textStyle = textStyle
                    )
                    BasicTextField(
                        value = person.age.toString(),
                        onValueChange = { person.age = it.toInt() },
                        textStyle = textStyle
                    )
                }
            }
        }
    }

    private class FormView(context: Context, rowHeight: Float, fontSize: TextUnit) :
        RecyclerView(context) {
        private val linearLayoutManager: LinearLayoutManager

        init {
            setHasFixedSize(true)
            linearLayoutManager = LinearLayoutManager(context, VERTICAL, false)
            layoutManager = linearLayoutManager
            adapter = DemoAdapter(data, rowHeight, fontSize)
        }

        val lastVisibleItemIndex: Int
            get() = linearLayoutManager.findLastVisibleItemPosition()

        override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo {
            return trace(CREATE_ANI_TRACE) { super.createAccessibilityNodeInfo() }
        }

        override fun sendAccessibilityEvent(eventType: Int) {
            return trace(ACCESSIBILITY_EVENT_TRACE) { super.sendAccessibilityEvent(eventType) }
        }
    }

    private class RowView(context: Context, content: (RowView) -> Unit) : LinearLayout(context) {
        init {
            gravity = Gravity.CENTER_VERTICAL
            content(this)
        }

        override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo {
            return trace(CREATE_ANI_TRACE) { super.createAccessibilityNodeInfo() }
        }

        override fun sendAccessibilityEvent(eventType: Int) {
            return trace(ACCESSIBILITY_EVENT_TRACE) { super.sendAccessibilityEvent(eventType) }
        }
    }

    @SuppressLint("AppCompatCustomView")
    private class EditTextView(context: Context, fontSize: Float) : EditText(context) {
        init {
            textSize = fontSize
            gravity = Gravity.CENTER_VERTICAL
        }

        fun replaceText(newText: String) {
            text.replace(0, length(), newText, 0, newText.length)
        }

        override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo {
            return trace(CREATE_ANI_TRACE) { super.createAccessibilityNodeInfo() }
        }

        override fun sendAccessibilityEvent(eventType: Int) {
            return trace(ACCESSIBILITY_EVENT_TRACE) { super.sendAccessibilityEvent(eventType) }
        }
    }

    private class DemoAdapter(
        val data: List<FormData>,
        val rowHeightPx: Float,
        textSize: TextUnit
    ) : Adapter<DemoAdapter.DemoViewHolder>() {

        private class DemoViewHolder(
            val title: EditTextView,
            val firstName: EditTextView,
            val middleName: EditTextView,
            val lastName: EditTextView,
            val age: EditTextView,
            itemRoot: View
        ) : ViewHolder(itemRoot)

        val textSize = textSize.value

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
            val title = EditTextView(parent.context, textSize)
            val firstName = EditTextView(parent.context, textSize)
            val middleName = EditTextView(parent.context, textSize)
            val lastName = EditTextView(parent.context, textSize)
            val age = EditTextView(parent.context, textSize)

            return DemoViewHolder(
                title,
                firstName,
                middleName,
                lastName,
                age,
                RowView(parent.context) {
                    it.minimumHeight = rowHeightPx.fastRoundToInt()
                    it.addView(title)
                    it.addView(firstName)
                    it.addView(middleName)
                    it.addView(lastName)
                    it.addView(age)
                }
            )
        }

        override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
            val formData = data.elementAt(position)
            holder.title.replaceText(formData.title)
            holder.firstName.replaceText(formData.firstName)
            holder.middleName.replaceText(formData.middleName)
            holder.lastName.replaceText(formData.lastName)
            holder.age.replaceText(formData.age.toString())
        }

        override fun getItemCount(): Int = data.size
    }

    private data class FormData(
        var title: String = "",
        var firstName: String = "",
        var middleName: String = "",
        var lastName: String = "",
        var age: Int = 0,
    )

    private companion object {
        private const val TYPE = "TYPE"
        private const val COMPOSE = "Compose"
        private const val VIEW = "View"
        private const val MODE = "MODE"
        private const val CREATE_ANI_MODE = 1
        private const val FRAME_MEASUREMENT_MODE = 2
        private const val CREATE_ANI_TRACE = "createAccessibilityNodeInfo"
        private const val ACCESSIBILITY_EVENT_TRACE = "sendAccessibilityEvent"
        private val data by lazy {
            List(200000) {
                FormData(
                    title = "Mr",
                    firstName = "John $it",
                    middleName = "Ace $it",
                    lastName = "Doe $it",
                    age = it
                )
            }
        }
    }
}
