package foo

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class LoginDirections private constructor() {
  public companion object {
    public fun actionDone(): NavDirections = ActionOnlyNavDirections(R.id.action_done)
  }
}
