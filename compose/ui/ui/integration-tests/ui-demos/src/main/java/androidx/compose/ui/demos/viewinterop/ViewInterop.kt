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

package androidx.compose.ui.demos.viewinterop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.demos.databinding.TestLayoutBinding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.Ref
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding

@SuppressLint("ResourceType")
@Composable
fun ViewInteropDemo() {
    Column {
        // This is a collection of multiple ways to include Android Views in Compose UI hierarchies
        // and Compose in Android ViewGroups. Note that these APIs are subject to change.

        // Compose and inflate a layout with ViewBinding.
        AndroidViewBinding(TestLayoutBinding::inflate) {
            text1.text = "Text updated"
        }

        // Compose Android View.
        AndroidView({ context -> TextView(context).apply { text = "This is a TextView" } })

        // Compose Android View and update its size based on state. The AndroidView takes modifiers.
        var size by remember { mutableIntStateOf(20) }
        AndroidView(::View, Modifier.clickable { size += 20 }.background(Color.Blue)) { view ->
            view.layoutParams = ViewGroup.LayoutParams(size, size)
        }

        AndroidView(::TextView) {
            it.text = "This is a text in a TextView"
        }

        // Compose custom Android View and do remeasurements and invalidates.
        val squareRef = Ref<ColoredSquareView>()
        AndroidView({ ColoredSquareView(it).also { squareRef.value = it } }) {
            it.size = 200
            it.color = Color.Cyan
        }
        Button(onClick = { squareRef.value!!.size += 50 }) {
            Text("Increase size of Android view")
        }
        val colorIndex = remember { mutableIntStateOf(0) }
        Button(
            onClick = {
                colorIndex.intValue = (colorIndex.intValue + 1) % 4
                squareRef.value!!.color = arrayOf(
                    Color.Blue, Color.LightGray, Color.Yellow, Color.Cyan
                )[colorIndex.intValue]
            }
        ) {
            Text("Change color of Android view")
        }

        Column(modifier =
            Modifier.fillMaxWidth().height(100.dp).verticalScroll(rememberScrollState())
        ) {
            AndroidView({ c ->
                LinearLayout(c).apply {
                    val text1 = TextView(c).apply { text = "LinearLayout child 1"; id = 11 }
                    val text2 = TextView(c).apply { text = "LinearLayout child 2"; id = 22 }
                    val text3 = TextView(c).apply { text = "LinearLayout child 3"; id = 33 }
                    if (Build.VERSION.SDK_INT >= 26) {
                        Api26Impl.setAccessibilityTraversalAfter(text3, text2.getId());
                        Api26Impl.setAccessibilityTraversalAfter(text2, text1.getId());
                    }
                    addView(text3)
                    addView(text2)
                    addView(text1)
                }
            })
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            item {
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 1A" }
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 1B" }
            }
            item {
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 2A" }
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 2B" }
            }
            item {
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 3A" }
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 3B" }
            }
            item {
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 4A" }
                AndroidView(::TextView) { it.text = "TextView in LazyColumn 4B" }
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth().height(50.dp).verticalScroll(rememberScrollState())
        ) {
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 1" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 2" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 3" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 4" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 5" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 6" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 7" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 8" }
            AndroidView(::TextView) { it.text = "TextView in verticalScroll 9" }
        }
    }
}

private class ColoredSquareView(context: Context) : View(context) {
    var size: Int = 100
        set(value) {
            if (value != field) {
                field = value
                rect.set(0, 0, value, value)
                requestLayout()
            }
        }

    var color: Color = Color.Blue
        set(value) {
            if (value != field) {
                field = value
                paint.color = value.toArgb()
                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(size, size)
    }

    private var rect = Rect(0, 0, 0, 0)
    private val paint = Paint()

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawRect(rect, paint)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object Api26Impl {
    @DoNotInline
    @JvmStatic
    fun setAccessibilityTraversalAfter(
        view: View,
        id: Int
    ) {
        view.setAccessibilityTraversalAfter(id);
    }
}
