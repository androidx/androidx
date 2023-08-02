package app.cash.paparazzi.internal

// Sampled from https://cs.android.com/android/platform/superproject/+/master:external/robolectric-shadows/shadows/framework/src/main/java/org/robolectric/shadows/ShadowOpenGLMatrix.java;l=10-67
object MatrixMatrixMultiplicationInterceptor {
  @Suppress("unused")
  @JvmStatic
  fun intercept(
    result: FloatArray,
    resultOffset: Int,
    lhs: FloatArray,
    lhsOffset: Int,
    rhs: FloatArray,
    rhsOffset: Int
  ) {
    require(resultOffset + 16 <= result.size) { "resultOffset + 16 > result.length" }
    require(lhsOffset + 16 <= lhs.size) { "lhsOffset + 16 > lhs.length" }
    require(rhsOffset + 16 <= rhs.size) { "rhsOffset + 16 > rhs.length" }
    for (i in 0..3) {
      val rhs_i0 = rhs[I(i, 0, rhsOffset)]
      var ri0 = lhs[I(0, 0, lhsOffset)] * rhs_i0
      var ri1 = lhs[I(0, 1, lhsOffset)] * rhs_i0
      var ri2 = lhs[I(0, 2, lhsOffset)] * rhs_i0
      var ri3 = lhs[I(0, 3, lhsOffset)] * rhs_i0
      for (j in 1..3) {
        val rhs_ij = rhs[I(i, j, rhsOffset)]
        ri0 += lhs[I(j, 0, lhsOffset)] * rhs_ij
        ri1 += lhs[I(j, 1, lhsOffset)] * rhs_ij
        ri2 += lhs[I(j, 2, lhsOffset)] * rhs_ij
        ri3 += lhs[I(j, 3, lhsOffset)] * rhs_ij
      }
      result[I(i, 0, resultOffset)] = ri0
      result[I(i, 1, resultOffset)] = ri1
      result[I(i, 2, resultOffset)] = ri2
      result[I(i, 3, resultOffset)] = ri3
    }
  }

  @Suppress("FunctionName")
  private fun I(i: Int, j: Int, offset: Int): Int {
    // #define I(_i, _j) ((_j)+ 4*(_i))
    return offset + j + 4 * i
  }
}
