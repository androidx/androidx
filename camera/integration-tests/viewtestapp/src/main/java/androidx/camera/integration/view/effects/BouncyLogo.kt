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

package androidx.camera.integration.view.effects

import android.graphics.Point
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A logo that bounces around the screen.
 *
 * <p> Each time the logo reaches the edge of the screen, it will bounce off and move in a new
 * direction. The new destination is a random point on the next edge.
 *
 * @param width The width of the screen.
 * @param height The height of the screen.
 */
class BouncyLogo(private val width: Int, private val height: Int) {

    companion object {
        private const val STEP_PERCENTAGE = 0.01F
    }

    private var currentPos: Point = Point(width / 2, height / 2)
    private var currentEdge: Edge = Edge.TOP
    private var destination: Point = getRandomPointOnEdge(currentEdge)

    // Each step is 0.5% of the screen diagonal.
    private val stepSize: Double = calculateStepSize()

    /** Gets the next position of the logo. */
    fun getNextPosition(): Point {
        val deltaX = destination.x - currentPos.x
        val deltaY = destination.y - currentPos.y
        val distance = hypot(deltaX.toDouble(), deltaY.toDouble())

        if (distance <= stepSize) {
            // The logo has reached the destination. Now randomly choose a new destination on the
            // next edge.
            currentPos = destination
            currentEdge = getNextEdge(currentEdge)
            destination = getRandomPointOnEdge(currentEdge)
        } else {
            // Move the logo towards the destination by one step.
            val stepX = (stepSize * deltaX / distance).toInt()
            val stepY = (stepSize * deltaY / distance).toInt()
            currentPos = Point(currentPos.x + stepX, currentPos.y + stepY)
        }
        currentPos = Point(currentPos.x, currentPos.y)
        return currentPos
    }

    private fun calculateStepSize(): Double {
        return sqrt((width * width + height * height).toDouble()) * STEP_PERCENTAGE
    }

    private fun getNextEdge(currentEdge: Edge): Edge {
        return when (currentEdge) {
            Edge.TOP -> Edge.RIGHT
            Edge.RIGHT -> Edge.BOTTOM
            Edge.BOTTOM -> Edge.LEFT
            Edge.LEFT -> Edge.TOP
        }
    }

    private fun getRandomPointOnEdge(edge: Edge): Point {
        return when (edge) {
            Edge.TOP -> Point(Random.nextInt(width), 0)
            Edge.RIGHT -> Point(width - 1, Random.nextInt(height))
            Edge.BOTTOM -> Point(Random.nextInt(width), height - 1)
            Edge.LEFT -> Point(0, Random.nextInt(height))
        }
    }

    enum class Edge {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT
    }
}
