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
    val main: String,
    val mainInt: Int,
    val optional: String = "bla",
    val optionalInt: Int = 239,
    val optionalParcelable: ActivityInfo? = null,
    val parcelable: ActivityInfo,
    val innerData: ActivityInfo.WindowLayout
) : NavDirections {
    override fun getActionId(): Int = R.id.next

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun getArguments(): Bundle {
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
        } else if (Serializable::class.java.isAssignableFrom(ActivityInfo.WindowLayout::class.java))
                {
            result.putSerializable("innerData", this.innerData as Serializable)
        } else {
            throw UnsupportedOperationException(ActivityInfo.WindowLayout::class.java.name +
                    " must implement Parcelable or Serializable or must be an Enum.")
        }
        return result
    }
}
