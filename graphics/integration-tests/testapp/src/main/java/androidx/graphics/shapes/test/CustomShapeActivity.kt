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

package androidx.graphics.shapes.test

import android.graphics.Matrix
import android.graphics.PointF
import androidx.core.graphics.rotationMatrix
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.transformed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * This subclass activity shows how to create many different kinds of shapes, some very custom, the
 * rest just adjusting the normal parameters in the standard polygon and star shapes.
 */
class CustomShapeActivity : ShapeActivity() {

    override fun setupShapes() {

        val tofu = RoundedPolygon.rectangle(width = 2f, height = 2f)

        // Cache various roundings for use below
        val cornerRound15 = CornerRounding(.15f)
        val cornerRound20 = CornerRounding(.2f)
        val cornerRound30 = CornerRounding(.3f)
        val cornerRound40 = CornerRounding(.4f)
        val cornerRound50 = CornerRounding(.5f)
        val cornerRound100 = CornerRounding(1f)

        val mRotateNeg45 = rotationMatrix(-45f)
        val mRotate45 = rotationMatrix(45f)
        val mRotateNeg90 = rotationMatrix(-90f)
        val mRotate90 = rotationMatrix(90f)
        val unrounded = CornerRounding.Unrounded

        // Circle
        shapes.add(RoundedPolygon.circle())

        // Oval, - just an extruded circle, rotated
        val m = Matrix()
        m.setScale(1.8f, 1f)
        m.postRotate(-45f)
        shapes.add(RoundedPolygon.circle().transformed(m))

        // Pill
        val verticalPill = RoundedPolygon.pill(1f, 1.25f)
        shapes.add(verticalPill)

        // CCW Rotated pill
        shapes.add(verticalPill.transformed(mRotateNeg45))

        // CW Rotated pill
        shapes.add(verticalPill.transformed(mRotate45))

        // rounded triangle
        var shape = RoundedPolygon(3, rounding = cornerRound20)
        shapes.add(shape.transformed(mRotateNeg90))

        // rotated triangle
        shapes.add(shape.transformed(mRotate90))

        // pie wedge
        shape =
            RoundedPolygon(
                4,
                perVertexRounding =
                    listOf(cornerRound100, cornerRound20, cornerRound20, cornerRound20)
            )
        shapes.add(shape.transformed(mRotateNeg45))

        // Alice
        shapes.add(MaterialShapes.alice())

        // Rotated alice
        shapes.add(MaterialShapes.alice().transformed(rotationMatrix(180f)))

        // Rounded square
        shapes.add(RoundedPolygon.rectangle(1f, 1f, rounding = cornerRound30))

        // Skewed roundRect
        shape =
            RoundedPolygon.rectangle(width = 1.2f, height = 1f, rounding = CornerRounding(.2f, .5f))
        m.setSkew(-.15f, 0f)
        shapes.add(shape.transformed(m))

        // Dome
        shape =
            RoundedPolygon(
                4,
                perVertexRounding =
                    listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20)
            )
        shapes.add(shape.transformed(mRotateNeg45))

        // Half circle
        shape =
            RoundedPolygon.rectangle(
                1.8f,
                1f,
                perVertexRounding =
                    listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100)
            )
        shapes.add(shape)

        // Sandwich cookie - basically, two pills stacked on each other
        var inset = .4f
        val sandwichPoints =
            floatArrayOf(
                1f,
                1f,
                inset,
                1f,
                -inset,
                1f,
                -1f,
                1f,
                -1f,
                0f,
                -inset,
                0f,
                -1f,
                0f,
                -1f,
                -1f,
                -inset,
                -1f,
                inset,
                -1f,
                1f,
                -1f,
                1f,
                0f,
                inset,
                0f,
                1f,
                0f
            )
        var pvRounding =
            listOf(
                cornerRound100,
                unrounded,
                unrounded,
                cornerRound100,
                cornerRound100,
                unrounded,
                cornerRound100,
                cornerRound100,
                unrounded,
                unrounded,
                cornerRound100,
                cornerRound100,
                unrounded,
                cornerRound100
            )
        shapes.add(RoundedPolygon(sandwichPoints, perVertexRounding = pvRounding))

        // placeholder
        shapes.add(tofu)

        // pentagon
        shape = RoundedPolygon(5, rounding = cornerRound50)
        shapes.add(shape.transformed(rotationMatrix(-360f / 20)))

        // extruded hexagon
        val cornerInset = .6f
        val edgeInset = .4f
        val height = .65f
        val hexPoints =
            floatArrayOf(
                1f,
                0f,
                cornerInset,
                height,
                edgeInset,
                height,
                -edgeInset,
                height,
                -cornerInset,
                height,
                -1f,
                0f,
                -cornerInset,
                -height,
                -edgeInset,
                -height,
                edgeInset,
                -height,
                cornerInset,
                -height,
            )
        pvRounding =
            listOf(
                cornerRound50,
                cornerRound50,
                unrounded,
                unrounded,
                cornerRound50,
                cornerRound50,
                cornerRound50,
                unrounded,
                unrounded,
                cornerRound50,
            )
        shapes.add(RoundedPolygon(hexPoints, perVertexRounding = pvRounding))

        // irregular hexagon (right narrower than left, then rotated)
        // First, generate a standard hexagon
        val numVertices = 6
        val radius = 1f
        var points = FloatArray(numVertices * 2)
        var index = 0
        for (i in 0 until numVertices) {
            val vertex = radialToCartesian(radius, (PI.toFloat() / numVertices * 2 * i))
            points[index++] = vertex.x
            points[index++] = vertex.y
        }
        // Now adjust-in the points at the top (next-to-last and second vertices, post rotation)
        points[2] -= .1f
        points[3] -= .1f
        points[10] -= .1f
        points[11] += .1f
        shapes.add(RoundedPolygon(points, cornerRound50).transformed(mRotateNeg90))

        // Star 1
        shapes.add(RoundedPolygon.star(8, innerRadius = .83f, rounding = cornerRound15))

        // Bugdroid
        inset = .5f
        val w = .88f
        points = floatArrayOf(1f, w, -1f, w, -inset, 0f, -1f, -w, 1f, -w)
        pvRounding =
            listOf(cornerRound100, cornerRound50, cornerRound100, cornerRound50, cornerRound100)
        shapes.add(RoundedPolygon(points, perVertexRounding = pvRounding).transformed(mRotateNeg90))

        // Star4
        shapes.add(RoundedPolygon.star(4, innerRadius = .42f, rounding = cornerRound20))

        // Clover
        shapes.add(
            RoundedPolygon.star(4, innerRadius = .5f, rounding = cornerRound30)
                .transformed(mRotateNeg45)
        )

        // Star6
        shapes.add(
            RoundedPolygon.star(6, innerRadius = .75f, rounding = cornerRound50)
                .transformed(mRotateNeg90)
        )

        // Star7
        shapes.add(
            RoundedPolygon.star(7, innerRadius = .75f, rounding = cornerRound50)
                .transformed(mRotateNeg90)
        )

        // Star9
        shapes.add(
            RoundedPolygon.star(9, innerRadius = .75f, rounding = cornerRound50)
                .transformed(mRotateNeg90)
        )

        // Star12
        shapes.add(
            RoundedPolygon.star(12, innerRadius = .8f, rounding = cornerRound50)
                .transformed(mRotateNeg90)
        )

        // Star8
        shapes.add(RoundedPolygon.star(8, innerRadius = .65f, rounding = cornerRound15))

        // Star10
        shapes.add(RoundedPolygon.star(10, innerRadius = .65f, rounding = cornerRound15))

        // Star8Bulbous (no inner rounding)
        val smoothRound = CornerRounding(.26f, .95f)
        shapes.add(
            RoundedPolygon.star(
                8,
                radius = 2f,
                innerRadius = 1.15f,
                rounding = smoothRound,
                innerRounding = unrounded
            )
        )

        // Star4Bulbous (no inner rounding)
        shapes.add(
            RoundedPolygon.star(
                    4,
                    innerRadius = .2f,
                    rounding = cornerRound40,
                    innerRounding = unrounded
                )
                .transformed(mRotate45)
        )

        // Star8BulbousB (no inner rounding)
        shapes.add(
            RoundedPolygon.star(
                    8,
                    innerRadius = .65f,
                    rounding = cornerRound30,
                    innerRounding = unrounded
                )
                .transformed(rotationMatrix(360f / 16))
        )

        // Pointy12
        shapes.add(RoundedPolygon.star(12, innerRadius = .7f))

        // Pointy15
        shapes.add(RoundedPolygon.star(15, innerRadius = .42f))

        // Heart
        points =
            floatArrayOf(
                .2f,
                0f,
                -.4f,
                .5f,
                -1f,
                1f,
                -1.5f,
                .5f,
                -1f,
                0f,
                -1.5f,
                -.5f,
                -1f,
                -1f,
                -.4f,
                -.5f
            )
        pvRounding =
            listOf(
                unrounded,
                unrounded,
                cornerRound100,
                cornerRound100,
                unrounded,
                cornerRound100,
                cornerRound100,
                unrounded
            )
        shape = RoundedPolygon(points, perVertexRounding = pvRounding)
        m.setRotate(-90f)
        shapes.add(shape.transformed(rotationMatrix(90f)))

        // Jaggycircle
        val pixelSize = .1f
        points =
            floatArrayOf(
                // BR quadrant
                6 * pixelSize,
                0 * pixelSize,
                6 * pixelSize,
                2 * pixelSize,
                5 * pixelSize,
                2 * pixelSize,
                5 * pixelSize,
                4 * pixelSize,
                4 * pixelSize,
                4 * pixelSize,
                4 * pixelSize,
                5 * pixelSize,
                2 * pixelSize,
                5 * pixelSize,
                2 * pixelSize,
                6 * pixelSize,

                // BL quadrant
                -2 * pixelSize,
                6 * pixelSize,
                -2 * pixelSize,
                5 * pixelSize,
                -4 * pixelSize,
                5 * pixelSize,
                -4 * pixelSize,
                4 * pixelSize,
                -5 * pixelSize,
                4 * pixelSize,
                -5 * pixelSize,
                2 * pixelSize,
                -6 * pixelSize,
                2 * pixelSize,
                -6 * pixelSize,
                0 * pixelSize,

                // TL quadrant
                -6 * pixelSize,
                -2 * pixelSize,
                -5 * pixelSize,
                -2 * pixelSize,
                -5 * pixelSize,
                -4 * pixelSize,
                -4 * pixelSize,
                -4 * pixelSize,
                -4 * pixelSize,
                -5 * pixelSize,
                -2 * pixelSize,
                -5 * pixelSize,
                -2 * pixelSize,
                -6 * pixelSize,

                // TR quadrant
                2 * pixelSize,
                -6 * pixelSize,
                2 * pixelSize,
                -5 * pixelSize,
                4 * pixelSize,
                -5 * pixelSize,
                4 * pixelSize,
                -4 * pixelSize,
                5 * pixelSize,
                -4 * pixelSize,
                5 * pixelSize,
                -2 * pixelSize,
                6 * pixelSize,
                -2 * pixelSize
            )
        shapes.add(RoundedPolygon(points, unrounded))

        prevShape = shapes[0]
        currShape = shapes[0]
    }
}

private val Zero = PointF(0f, 0f)

private fun radialToCartesian(radius: Float, angleRadians: Float, center: PointF = Zero) =
    directionVector(angleRadians) * radius + center

private fun directionVector(angleRadians: Float) = PointF(cos(angleRadians), sin(angleRadians))

internal operator fun PointF.times(operand: Float): PointF = PointF(x * operand, y * operand)

internal operator fun PointF.plus(operand: PointF): PointF = PointF(x + operand.x, y + operand.y)
