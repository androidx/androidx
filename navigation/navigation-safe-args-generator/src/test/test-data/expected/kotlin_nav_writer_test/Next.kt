package a.b

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavDirections
import java.io.Serializable
import java.lang.UnsupportedOperationException
import kotlin.Int
import kotlin.String
import kotlin.Suppress

private data class Next(
  public val main: String,
  public val mainInt: Int,
  public val optional: String = "bla",
  public val optionalInt: Int = 239,
  public val optionalParcelable: ActivityInfo? = null,
  public val parcelable: ActivityInfo,
  public val innerData: ActivityInfo.WindowLayout
) : NavDirections {
  public override val actionId: Int = R.id.next

  public override val arguments: Bundle
    @Suppress("CAST_NEVER_SUCCEEDS")
    get() {
      val result = Bundle()
      result.putString("main", this.main)
      result.putInt("mainInt", this.mainInt)
      result.putString("optional", this.optional)
      result.putInt("optionalInt", this.optionalInt)
      if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
        result.putParcelable("optionalParcelable", this.optionalParcelable as Parcelable?)
      } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
        result.putSerializable("optionalParcelable", this.optionalParcelable as Serializable?)
      }
      if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
        result.putParcelable("parcelable", this.parcelable as Parcelable)
      } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
        result.putSerializable("parcelable", this.parcelable as Serializable)
      } else {
        throw UnsupportedOperationException(ActivityInfo::class.java.name +
            " must implement Parcelable or Serializable or must be an Enum.")
      }
      if (Parcelable::class.java.isAssignableFrom(ActivityInfo.WindowLayout::class.java)) {
        result.putParcelable("innerData", this.innerData as Parcelable)
      } else if (Serializable::class.java.isAssignableFrom(ActivityInfo.WindowLayout::class.java)) {
        result.putSerializable("innerData", this.innerData as Serializable)
      } else {
        throw UnsupportedOperationException(ActivityInfo.WindowLayout::class.java.name +
            " must implement Parcelable or Serializable or must be an Enum.")
      }
      return result
    }
}
