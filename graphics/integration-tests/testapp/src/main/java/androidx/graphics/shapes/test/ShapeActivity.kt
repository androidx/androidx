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

package androidx.graphics.shapes.test

import android.app.Activity
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.graphics.shapes.Circle
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.CornerRounding.Companion.Unrounded
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Star

class ShapeActivity : Activity() {

    val shapes = mutableListOf<RoundedPolygon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.BLACK)
        setContentView(container)

        setupShapes()

        addShapeViews(container)
    }

    private fun getShapeView(shape: RoundedPolygon, width: Int, height: Int): View {
        val view = ShapeView(this, shape)
        val layoutParams = LinearLayout.LayoutParams(width, height)
        layoutParams.setMargins(5, 5, 5, 5)
        view.layoutParams = layoutParams
        // TODO: add click listener to show expanded version of shape at bottom of container
        return view
    }

    private fun setupShapes() {
        // Note: all RoundedPolygon(4) shapes are placeholders for shapes not yet handled
        val matrix1 = Matrix().apply { setRotate(-45f) }
        val matrix2 = Matrix().apply { setRotate(45f) }
        val blobR1 = MaterialShapes.blobR(.19f, .86f)
        blobR1.transform(matrix1)
        val blobR2 = MaterialShapes.blobR(.19f, .86f)
        blobR2.transform(matrix2)

        //        "Circle" to DefaultShapes.star(4, 1f, 1f),
        shapes.add(Circle())
        //        "Squirrel" to DefaultShapes.fgSquircle(0.9f),
        shapes.add(RoundedPolygon(4))
        //        "Squircle" to DefaultShapes.fgSquircle(0.7f),
        shapes.add(RoundedPolygon(4))
        //        "Scallop" to DefaultShapes.Scallop,
        shapes.add(MaterialShapes.scallop())
        //        "Clover" to DefaultShapes.Clover,
        shapes.add(MaterialShapes.clover())

        //        "Alice" to DefaultShapes.Alice,
        shapes.add(MaterialShapes.alice())
        //        "Veronica" to DefaultShapes.Alice.rotate(TwoPI / 2),
        shapes.add(RoundedPolygon(4))
        //        "Wiggle-Star" to DefaultShapes.WiggleStar,
        shapes.add(MaterialShapes.wiggleStar())
        //        "Wovel" to DefaultShapes.Wovel,
        shapes.add(MaterialShapes.wovel())
        //        "Blob Left" to DefaultShapes.BlobR.rotate(-TwoPI / 4),
        shapes.add(blobR1)

        //        "Blob Right" to DefaultShapes.BlobR,
        shapes.add(blobR2)
        //        "More" to DefaultShapes.More,
        shapes.add(MaterialShapes.more())
        //        "Less" to DefaultShapes.More.rotate(TwoPI / 2),
        shapes.add(RoundedPolygon(4))
        //        "CornerNE" to DefaultShapes.CornerSE.rotate(-TwoPI / 4),
        shapes.add(RoundedPolygon(4))
        //        "CornerNW" to DefaultShapes.CornerSE.rotate(TwoPI / 2),
        shapes.add(RoundedPolygon(4))

        //        "CornerSW" to DefaultShapes.CornerSE.rotate(TwoPI / 4),
        shapes.add(RoundedPolygon(4))
        //        "CornerSE" to DefaultShapes.CornerSE,
        shapes.add(MaterialShapes.cornerSouthEast(.4f))
        //        "Quarty" to DefaultShapes.Quarty,
        shapes.add(MaterialShapes.quarty(.3f, smooth = .5f))
        //        "5D Cube" to DefaultShapes.Cube5D,
        shapes.add(MaterialShapes.cube5D())
        //        "Pentagon" to DefaultShapes.Pentagon
        shapes.add(MaterialShapes.pentagon())

        // Some non-Material shapes

        val rounding = CornerRounding(.1f, .5f)
        val starRounding = CornerRounding(.05f, .25f)
        shapes.add(RoundedPolygon(numVertices = 4, rounding = rounding))
        shapes.add(Star(8, radius = 1f, innerRadius = .4f, rounding = starRounding))
        shapes.add(Star(8, radius = 1f, innerRadius = .4f, rounding = starRounding,
            innerRounding = CornerRounding.Unrounded))
        shapes.add(
            MaterialShapes.clover(rounding = .352f, innerRadius = .1f,
            innerRounding = Unrounded))
        shapes.add(RoundedPolygon(3))
    }

    private fun addShapeViews(container: ViewGroup) {
        val WIDTH = 200
        val HEIGHT = 200

        var shapeIndex = 0
        var row: LinearLayout? = null
        while (shapeIndex < shapes.size) {
            if (shapeIndex % 5 == 0) {
                row = LinearLayout(this)
                val layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                row.layoutParams = layoutParams
                row.orientation = LinearLayout.HORIZONTAL
                container.addView(row)
            }
            row!!.addView(getShapeView(shapes[shapeIndex], WIDTH, HEIGHT))
            ++shapeIndex
        }
    }
}
