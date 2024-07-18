package app.cash.paparazzi.internal

// Sampled from https://cs.android.com/android/platform/superproject/+/master:external/robolectric-shadows/shadows/framework/src/main/java/org/robolectric/shadows/ShadowOpenGLMatrix.java;l=69-121
object MatrixVectorMultiplicationInterceptor {
  @Suppress("unused")
  @JvmStatic
  fun intercept(
    resultVec: FloatArray,
    resultVecOffset: Int,
    lhsMat: FloatArray,
    lhsMatOffset: Int,
    rhsVec: FloatArray,
    rhsVecOffset: Int
  ) {
    require(resultVecOffset + 4 <= resultVec.size) { "resultOffset + 4 > result.length" }
    require(lhsMatOffset + 16 <= lhsMat.size) { "lhsOffset + 16 > lhs.length" }
    require(rhsVecOffset + 4 <= rhsVec.size) { "rhsOffset + 4 > rhs.length" }
    val x = rhsVec[rhsVecOffset + 0]
    val y = rhsVec[rhsVecOffset + 1]
    val z = rhsVec[rhsVecOffset + 2]
    val w = rhsVec[rhsVecOffset + 3]
    resultVec[resultVecOffset + 0] =
      lhsMat[I(0, 0, lhsMatOffset)] * x + lhsMat[I(1, 0, lhsMatOffset)] * y +
          lhsMat[I(2, 0, lhsMatOffset)] * z + lhsMat[I(3, 0, lhsMatOffset)] * w
    resultVec[resultVecOffset + 1] =
      lhsMat[I(0, 1, lhsMatOffset)] * x + lhsMat[I(1, 1, lhsMatOffset)] * y +
          lhsMat[I(2, 1, lhsMatOffset)] * z + lhsMat[I(3, 1, lhsMatOffset)] * w
    resultVec[resultVecOffset + 2] =
      lhsMat[I(0, 2, lhsMatOffset)] * x + lhsMat[I(1, 2, lhsMatOffset)] * y +
          lhsMat[I(2, 2, lhsMatOffset)] * z + lhsMat[I(3, 2, lhsMatOffset)] * w
    resultVec[resultVecOffset + 3] =
      lhsMat[I(0, 3, lhsMatOffset)] * x + lhsMat[I(1, 3, lhsMatOffset)] * y +
          lhsMat[I(2, 3, lhsMatOffset)] * z + lhsMat[I(3, 3, lhsMatOffset)] * w
  }

  @Suppress("FunctionName")
  private fun I(i: Int, j: Int, offset: Int): Int {
    // #define I(_i, _j) ((_j)+ 4*(_i))
    return offset + j + 4 * i
  }
}
