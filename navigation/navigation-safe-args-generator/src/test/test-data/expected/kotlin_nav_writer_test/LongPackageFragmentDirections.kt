package a.b.reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage

import a.b.secondreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage.R
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class LongPackageFragmentDirections private constructor() {
  public companion object {
    public fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
  }
}
