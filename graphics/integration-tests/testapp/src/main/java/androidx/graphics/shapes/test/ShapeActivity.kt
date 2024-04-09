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
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.CornerRounding.Companion.Unrounded
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.transformed

open class ShapeActivity : Activity() {

    val shapes = mutableListOf<RoundedPolygon>()

    lateinit var morphView: MorphView

    lateinit var prevShape: RoundedPolygon
    lateinit var currShape: RoundedPolygon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.BLACK)
        setContentView(container)

        setupShapes()

        addShapeViews(container)

        setupMorphView()
        container.addView(morphView)
    }

    private fun setupMorphView() {
        val morph = Morph(prevShape, currShape)
        if (this::morphView.isInitialized) {
            morphView.morph = morph
        } else {
            morphView = MorphView(this, morph)
        }
    }

    private fun getShapeView(shape: RoundedPolygon, width: Int, height: Int): View {
        val view = ShapeView(this, shape)
        val layoutParams = LinearLayout.LayoutParams(width, height)
        layoutParams.setMargins(5, 5, 5, 5)
        view.layoutParams = layoutParams
        // TODO: add click listener to show expanded version of shape at bottom of container
        return view
    }

    internal open fun setupShapes() {
        // Note: all RoundedPolygon(4) shapes are placeholders for shapes not yet handled
        val matrix1 = Matrix().apply { setRotate(-45f) }
        val matrix2 = Matrix().apply { setRotate(45f) }
        val blobR1 = MaterialShapes.blobR(.19f, .86f).transformed(matrix1)
        val blobR2 = MaterialShapes.blobR(.19f, .86f).transformed(matrix2)

        //        "Circle" to DefaultShapes.star(4, 1f, 1f),
        shapes.add(RoundedPolygon.circle())
        //        "Squirrel" to DefaultShapes.fgSquircle(0.9f),
        shapes.add(RoundedPolygon(4))
        //        Square, using rectangle function
        shapes.add(RoundedPolygon.rectangle(width = 2f, height = 2f))
        //        "Scallop" to DefaultShapes.Scallop,
        shapes.add(MaterialShapes.scallop())
        //        "Clover" to DefaultShapes.Clover,
        shapes.add(MaterialShapes.clover())

        //        "Alice" to DefaultShapes.Alice,
        shapes.add(MaterialShapes.alice())
        //        Rectangle
        shapes.add(RoundedPolygon.rectangle(width = 4f, height = 2f))
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
        //        Round Rect
        shapes.add(RoundedPolygon.rectangle(width = 4f, height = 2f,
            rounding = CornerRounding(1f)
        ))
        //        Round Rect (smoothed)
        shapes.add(RoundedPolygon.rectangle(width = 4f, height = 2f,
            rounding = CornerRounding(1f, .5f)))
        //        Round Rect (smoothed more)
        shapes.add(RoundedPolygon.rectangle(width = 4f, height = 2f,
            rounding = CornerRounding(1f, 1f)))

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
        shapes.add(RoundedPolygon.star(8, radius = 1f, innerRadius = .4f, rounding = starRounding))
        shapes.add(RoundedPolygon.star(8, radius = 1f, innerRadius = .4f, rounding = starRounding,
            innerRounding = CornerRounding.Unrounded))
        shapes.add(
            MaterialShapes.clover(rounding = .352f, innerRadius = .1f,
            innerRounding = Unrounded))
        shapes.add(RoundedPolygon(3))

        // Pills
        shapes.add(RoundedPolygon.pill())
        shapes.add(RoundedPolygon.pill(15f, 1f))
        shapes.add(RoundedPolygon.pillStar())
        shapes.add(RoundedPolygon.pillStar(numVerticesPerRadius = 10,
            rounding = CornerRounding(.5f)
        ))
        shapes.add(RoundedPolygon.pillStar(numVerticesPerRadius = 10,
            rounding = CornerRounding(.5f), innerRadiusRatio = .5f,
            innerRounding = CornerRounding(.2f)))

        prevShape = shapes[0]
        currShape = shapes[0]
    }

    private fun addShapeViews(container: ViewGroup) {
        val WIDTH = 170
        val HEIGHT = 170

        var shapeIndex = 0
        var row: LinearLayout? = null
        while (shapeIndex < shapes.size) {
            if (shapeIndex % 6 == 0) {
                row = LinearLayout(this)
                val layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                row.layoutParams = layoutParams
                row.orientation = LinearLayout.HORIZONTAL
                container.addView(row)
            }
            val shape = shapes[shapeIndex]
            val shapeView = getShapeView(shape, WIDTH, HEIGHT)
            row!!.addView(shapeView)
            shapeView.setOnClickListener {
                prevShape = currShape
                currShape = shape
                setupMorphView()
            }
            ++shapeIndex
        }
    }
}
