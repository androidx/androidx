package a.b.reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage

import a.b.secondreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage.R
import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class LongPackageFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
  }
}
