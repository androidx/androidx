package androidx.core.telecom.test;

import androidx.core.telecom.extensions.Capability;
import androidx.core.telecom.extensions.Participant;

// NOTE: only supports one voip call at a time right now + suspend functions are not supported by
// AIDL :(
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface ITestAppControl {
  void setVoipCapabilities(in List<androidx.core.telecom.extensions.Capability> capabilities);
  List<androidx.core.telecom.extensions.Capability> getVoipCapabilities();
  String addCall(boolean isOutgoing);
  // TODO:: add ID argument
  void updateParticipants(in List<Participant> participants);
}