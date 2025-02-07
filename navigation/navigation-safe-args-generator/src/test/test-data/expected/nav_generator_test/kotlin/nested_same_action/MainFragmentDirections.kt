package foo.flavor

import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.R

public class MainFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun startLogin(): NavDirections = ActionOnlyNavDirections(R.id.start_login)
  }
}
