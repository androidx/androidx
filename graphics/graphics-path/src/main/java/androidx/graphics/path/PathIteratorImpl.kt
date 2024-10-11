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

package androidx.graphics.path

import android.graphics.Path
import android.graphics.PathIterator as PlatformPathIterator
import android.graphics.PointF
import androidx.annotation.RequiresApi
import androidx.graphics.path.PathIterator.ConicEvaluation
import dalvik.annotation.optimization.FastNative

/**
 * Base class for API-version-specific PathIterator implementation classes. All functionality is
 * implemented in the subclasses except for [next], which relies on shared native code to perform
 * conic conversion.
 */
internal abstract class PathIteratorImpl(
    val path: Path,
    val conicEvaluation: ConicEvaluation = ConicEvaluation.AsQuadratics,
    val tolerance: Float = 0.25f
) {
    /**
     * pointsData is used internally when the no-arg variant of next() is called, to avoid
     * allocating a new array every time.
     */
    private val pointsData = FloatArray(8)

    private companion object {
        init {
            /**
             * The native library is used mainly for pre-API34, but we also rely on the conic
             * conversion code in API34+, thus it is initialized here.
             *
             * Currently this native library is only available for Android, it should not be loaded
             * on host platforms, e.g. LayoutLib and Robolectric.
             */
            val isDalvik = "dalvik".equals(System.getProperty("java.vm.name"), ignoreCase = true)
            if (isDalvik) {
                System.loadLibrary("androidx.graphics.path")
            }
        }
    }

    abstract fun calculateSize(includeConvertedConics: Boolean): Int

    abstract fun hasNext(): Boolean

    abstract fun peek(): PathSegment.Type

    abstract fun next(points: FloatArray, offset: Int = 0): PathSegment.Type

    fun next(): PathSegment {
        val type = next(pointsData, 0)
        if (type == PathSegment.Type.Done) return DoneSegment
        if (type == PathSegment.Type.Close) return CloseSegment
        val weight = if (type == PathSegment.Type.Conic) pointsData[6] else 0.0f
        return PathSegment(type, floatsToPoints(pointsData, type), weight)
    }

    /**
     * Utility function to convert a FloatArray to an array of PointF objects, where every two
     * Floats in the FloatArray correspond to a single PointF in the resulting point array. The
     * FloatArray is used internally to process a next() call, the array of points is used to create
     * a PathSegment from the operation.
     */
    private fun floatsToPoints(pointsData: FloatArray, type: PathSegment.Type): Array<PointF> {
        val points =
            when (type) {
                PathSegment.Type.Move -> {
                    arrayOf(PointF(pointsData[0], pointsData[1]))
                }
                PathSegment.Type.Line -> {
                    arrayOf(
                        PointF(pointsData[0], pointsData[1]),
                        PointF(pointsData[2], pointsData[3])
                    )
                }
                PathSegment.Type.Quadratic,
                PathSegment.Type.Conic -> {
                    arrayOf(
                        PointF(pointsData[0], pointsData[1]),
                        PointF(pointsData[2], pointsData[3]),
                        PointF(pointsData[4], pointsData[5])
                    )
                }
                PathSegment.Type.Cubic -> {
                    arrayOf(
                        PointF(pointsData[0], pointsData[1]),
                        PointF(pointsData[2], pointsData[3]),
                        PointF(pointsData[4], pointsData[5]),
                        PointF(pointsData[6], pointsData[7])
                    )
                }
                // This should not happen because of the early returns above
                else -> emptyArray()
            }
        return points
    }
}

/**
 * In API level 34, we can use new platform functionality for most of what PathIterator does. The
 * exceptions are conic conversion (which is handled in the base impl class) and [calculateSize],
 * which is implemented here.
 */
@RequiresApi(34)
internal class PathIteratorApi34Impl(
    path: Path,
    conicEvaluation: ConicEvaluation = ConicEvaluation.AsQuadratics,
    tolerance: Float = 0.25f
) : PathIteratorImpl(path, conicEvaluation, tolerance) {
    /**
     * The platform iterator handles most of what we need for iterating. We hold an instance of that
     * object in this class.
     */
    private val platformIterator = path.pathIterator

    /**
     * An iterator's ConicConverter converts from a conic to a series of quadratics. It keeps track
     * of the resulting quadratics and iterates through them on ensuing calls to next(). The
     * converter is only ever called if [conicEvaluation] is set to [ConicEvaluation.AsQuadratics].
     */
    private val conicConverter = ConicConverter()

    /**
     * The platform does not expose a calculateSize() method, so we implement our own. In the
     * simplest case, this is done by simply iterating through all segments until done. However, if
     * the caller requested the true size (including any conic conversion) and if there are any
     * conics in the path segments, then there is more work to do since we have to convert and count
     * those segments as well.
     */
    override fun calculateSize(includeConvertedConics: Boolean): Int {
        val convertConics =
            includeConvertedConics && conicEvaluation == ConicEvaluation.AsQuadratics
        var numVerbs = 0
        val tempIterator = path.pathIterator
        val tempFloats = FloatArray(8)
        while (tempIterator.hasNext()) {
            val type = tempIterator.next(tempFloats, 0)
            if (type == PlatformPathIterator.VERB_CONIC && convertConics) {
                with(conicConverter) {
                    convert(tempFloats, tempFloats[6], tolerance)
                    numVerbs += quadraticCount
                }
            } else {
                numVerbs++
            }
        }
        return numVerbs
    }

    override fun next(points: FloatArray, offset: Int): PathSegment.Type {
        // First check to see if we are currently iterating through converted conics
        if (conicConverter.currentQuadratic < conicConverter.quadraticCount) {
            conicConverter.nextQuadratic(points, offset)
            return PathSegment.Type.Quadratic
        } else {
            val typeValue = platformToAndroidXSegmentType(platformIterator.next(points, offset))
            if (
                typeValue == PathSegment.Type.Conic &&
                    conicEvaluation == ConicEvaluation.AsQuadratics
            ) {
                with(conicConverter) {
                    convert(points, points[6 + offset], tolerance, offset)
                    if (quadraticCount > 0) {
                        nextQuadratic(points, offset)
                    }
                }
                return PathSegment.Type.Quadratic
            }
            return typeValue
        }
    }

    override fun hasNext(): Boolean = platformIterator.hasNext()

    override fun peek() = platformToAndroidXSegmentType(platformIterator.peek())
}

/** Callers need the AndroidX segment types, so we must convert from the platform types. */
private fun platformToAndroidXSegmentType(platformType: Int): PathSegment.Type {
    return when (platformType) {
        PlatformPathIterator.VERB_CLOSE -> PathSegment.Type.Close
        PlatformPathIterator.VERB_CONIC -> PathSegment.Type.Conic
        PlatformPathIterator.VERB_CUBIC -> PathSegment.Type.Cubic
        PlatformPathIterator.VERB_DONE -> PathSegment.Type.Done
        PlatformPathIterator.VERB_LINE -> PathSegment.Type.Line
        PlatformPathIterator.VERB_MOVE -> PathSegment.Type.Move
        PlatformPathIterator.VERB_QUAD -> PathSegment.Type.Quadratic
        else -> {
            throw IllegalArgumentException("Unknown path segment type $platformType")
        }
    }
}

/**
 * Most of the functionality for pre-34 iteration is handled in the native code. The only exception,
 * similar to the API34 implementation, is the calculateSize(). There is a size() function in native
 * code which is very quick (it simply tracks the number of verbs in the native structure). But if
 * the caller wants conic conversion, then we need to iterate through and convert appropriately,
 * counting as we iterate.
 */
internal class PathIteratorPreApi34Impl(
    path: Path,
    conicEvaluation: ConicEvaluation = ConicEvaluation.AsQuadratics,
    tolerance: Float = 0.25f
) : PathIteratorImpl(path, conicEvaluation, tolerance) {

    @Suppress("KotlinJniMissingFunction")
    private external fun createInternalPathIterator(
        path: Path,
        conicEvaluation: Int,
        tolerance: Float
    ): Long

    @Suppress("KotlinJniMissingFunction")
    private external fun destroyInternalPathIterator(internalPathIterator: Long)

    @Suppress("KotlinJniMissingFunction")
    @FastNative
    private external fun internalPathIteratorHasNext(internalPathIterator: Long): Boolean

    @Suppress("KotlinJniMissingFunction")
    @FastNative
    private external fun internalPathIteratorNext(
        internalPathIterator: Long,
        points: FloatArray,
        offset: Int
    ): Int

    @Suppress("KotlinJniMissingFunction")
    @FastNative
    private external fun internalPathIteratorPeek(internalPathIterator: Long): Int

    @Suppress("KotlinJniMissingFunction")
    @FastNative
    private external fun internalPathIteratorRawSize(internalPathIterator: Long): Int

    @Suppress("KotlinJniMissingFunction")
    @FastNative
    private external fun internalPathIteratorSize(internalPathIterator: Long): Int

    /** Defines the type of evaluation to apply to conic segments during iteration. */
    private val internalPathIterator =
        createInternalPathIterator(path, conicEvaluation.ordinal, tolerance)

    /**
     * Returns the number of verbs present in this iterator's path. If [includeConvertedConics]
     * property is false and the path has any conic elements, the returned size might be smaller
     * than the number of calls to [next] required to fully iterate over the path. An accurate size
     * can be computed by setting the parameter to true instead, at a performance cost. Including
     * converted conics requires iterating through the entire path, including converting any conics
     * along the way, to calculate the true size.
     */
    override fun calculateSize(includeConvertedConics: Boolean): Int =
        if (!includeConvertedConics || conicEvaluation == ConicEvaluation.AsConic) {
            internalPathIteratorRawSize(internalPathIterator)
        } else {
            internalPathIteratorSize(internalPathIterator)
        }

    /** Returns `true` if the iteration has more elements. */
    override fun hasNext(): Boolean = internalPathIteratorHasNext(internalPathIterator)

    /**
     * Returns the type of the current segment in the iteration, or [Done][PathSegment.Type.Done] if
     * the iteration is finished.
     */
    override fun peek() = PathSegmentTypes[internalPathIteratorPeek(internalPathIterator)]

    /**
     * This is where the actual work happens to get the next segment in the path, which happens in
     * native code. This function is called by [next] in the base class, which then converts the
     * resulting segment from conics to quadratics as necessary.
     */
    override fun next(points: FloatArray, offset: Int) =
        PathSegmentTypes[internalPathIteratorNext(internalPathIterator, points, offset)]

    protected fun finalize() {
        destroyInternalPathIterator(internalPathIterator)
    }
}

/** Cache of [PathSegment.Type] values to avoid internal allocation on each use. */
private val PathSegmentTypes = PathSegment.Type.values()
