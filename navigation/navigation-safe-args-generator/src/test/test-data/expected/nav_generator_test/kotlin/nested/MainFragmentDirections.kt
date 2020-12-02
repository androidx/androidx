package foo.flavor

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.R

public class MainFragmentDirections private constructor() {
  public companion object {
    public fun startLogin(): NavDirections = ActionOnlyNavDirections(R.id.start_login)
  }
}
