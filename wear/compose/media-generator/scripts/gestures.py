# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
ADB gesture choreography for Wear OS Compose Material 3 video animation samples.

Provides timing, touch gestures, and intent broadcasts to capture interactive component states.
"""

from dataclasses import dataclass
import time
from typing import Callable, Dict, Optional
import utils

LEFT_TOGGLE_X = 0.40
RIGHT_TOGGLE_X = 0.60
CENTER_X = 0.5
CENTER_Y = 0.5


# ==============================================================================
# SECTION 1: Gesture Handler Data Model
# ==============================================================================

@dataclass(frozen=True)
class GestureHandler:
    name: str
    action: Optional[Callable[[str], None]]
    triggers_on_load: bool
    description: str

    def trigger_animation(self, sample_name: str = "") -> None:
        """Executes the gesture choreography or autoplay broadcast for a given sample."""
        if self.action is not None:
            self.action(sample_name)


# ==============================================================================
# SECTION 2: Gesture Choreography Actions & Handlers
# ==============================================================================

# ------------------------------------------------------------------------------
# Autoplay Animations
# ------------------------------------------------------------------------------

def execute_autoplay_broadcast(sample_name: str = "") -> None:
    """Sends an ADB broadcast to force VideoActivity to restart the animation from frame 0."""
    broadcast_cmd = f"am broadcast -a {utils.INTENT_RESTART_ANIMATION}"
    utils.run_adb_shell_clock_synced(broadcast_cmd)
    time.sleep(3.0)


AUTOPLAY_HANDLER = GestureHandler(
    name="autoplay",
    action=execute_autoplay_broadcast,
    triggers_on_load=True,
    description="Executing autoplay broadcast...",
)


# ------------------------------------------------------------------------------
# One-Handed Gestures (OHG)
# ------------------------------------------------------------------------------

def execute_ohg_single_flick(sample_name: str = "") -> None:
    """Restarts wrist cue animation, pauses for indicator settle, and performs 1 forward flick."""
    utils.run_adb_shell_clock_synced(f"am broadcast -a {utils.INTENT_RESTART_ANIMATION}")
    time.sleep(2.5)
    utils.run_adb_shell_clock_synced(f"am broadcast -a {utils.INTENT_PERFORM_FORWARD_FLICK}")
    time.sleep(2.0)


OHG_SINGLE_FLICK_HANDLER = GestureHandler(
    name="ohg_single_flick",
    action=execute_ohg_single_flick,
    triggers_on_load=True,
    description="Executing one-handed single wrist flick...",
)


def execute_ohg_double_flick(sample_name: str = "") -> None:
    """Restarts wrist cue animation, pauses for indicator settle, and performs 2 forward flicks."""
    utils.run_adb_shell_clock_synced(f"am broadcast -a {utils.INTENT_RESTART_ANIMATION}")
    time.sleep(2.5)
    for _ in range(2):
        utils.run_adb_shell_clock_synced(f"am broadcast -a {utils.INTENT_PERFORM_FORWARD_FLICK}")
        time.sleep(2.0)


OHG_DOUBLE_FLICK_HANDLER = GestureHandler(
    name="ohg_double_flick",
    action=execute_ohg_double_flick,
    triggers_on_load=True,
    description="Executing one-handed double wrist flick...",
)


# ------------------------------------------------------------------------------
# Buttons, Toggles & Groups
# ------------------------------------------------------------------------------

def execute_double_tap_center(sample_name: str = "") -> None:
    """Performs two consecutive center taps separated by 1.5s."""
    utils.perform_synced_tap(CENTER_X, CENTER_Y)
    time.sleep(1.5)
    utils.perform_synced_tap(CENTER_X, CENTER_Y)
    time.sleep(1.5)


DOUBLE_TAP_CENTER_HANDLER = GestureHandler(
    name="double_tap_center",
    action=execute_double_tap_center,
    triggers_on_load=False,
    description="Executing double tap gesture...",
)


def execute_triple_tap_center(sample_name: str = "") -> None:
    """Performs three consecutive center taps separated by 1.5s."""
    for _ in range(3):
        utils.perform_synced_tap(CENTER_X, CENTER_Y)
        time.sleep(1.5)


TRIPLE_TAP_CENTER_HANDLER = GestureHandler(
    name="triple_tap_center",
    action=execute_triple_tap_center,
    triggers_on_load=False,
    description="Executing triple tap gesture...",
)


def execute_dual_icon_toggle(sample_name: str = "") -> None:
    """Toggles left and right icon toggle buttons sequentially."""
    for _ in range(2):
        utils.perform_synced_tap(LEFT_TOGGLE_X, CENTER_Y)
        time.sleep(1.2)
        utils.perform_synced_tap(RIGHT_TOGGLE_X, CENTER_Y)
        time.sleep(1.2)


DUAL_ICON_TOGGLE_HANDLER = GestureHandler(
    name="dual_icon_toggle",
    action=execute_dual_icon_toggle,
    triggers_on_load=False,
    description="Executing dual icon toggle choreography...",
)


def execute_button_group_two(sample_name: str = "") -> None:
    """Taps left and right buttons in a two-button group."""
    utils.perform_synced_tap(0.3, CENTER_Y, hold_ms=200)
    time.sleep(1.5)
    utils.perform_synced_tap(0.7, CENTER_Y, hold_ms=200)
    time.sleep(2.5)


BUTTON_GROUP_TWO_HANDLER = GestureHandler(
    name="button_group_two",
    action=execute_button_group_two,
    triggers_on_load=False,
    description="Executing two-button group choreography...",
)


def execute_button_group_three(sample_name: str = "") -> None:
    """Taps left and center buttons in a three-button group."""
    utils.perform_synced_tap(0.2, CENTER_Y, hold_ms=200)
    time.sleep(1.2)
    utils.perform_synced_tap(CENTER_X, CENTER_Y, hold_ms=200)
    time.sleep(2.0)


BUTTON_GROUP_THREE_HANDLER = GestureHandler(
    name="button_group_three",
    action=execute_button_group_three,
    triggers_on_load=False,
    description="Executing three-button group choreography...",
)


def execute_animated_text_button_response(sample_name: str = "") -> None:
    """Taps '+' button twice and '-' button twice to animate counter text."""
    utils.perform_synced_tap(RIGHT_TOGGLE_X, CENTER_Y)
    time.sleep(1.2)
    utils.perform_synced_tap(RIGHT_TOGGLE_X, CENTER_Y)
    time.sleep(1.2)
    utils.perform_synced_tap(LEFT_TOGGLE_X, CENTER_Y)
    time.sleep(1.2)
    utils.perform_synced_tap(LEFT_TOGGLE_X, CENTER_Y)
    time.sleep(1.5)


ANIMATED_TEXT_BUTTON_RESPONSE_HANDLER = GestureHandler(
    name="animated_text_button_response",
    action=execute_animated_text_button_response,
    triggers_on_load=False,
    description="Executing animated text button response choreography...",
)


# ------------------------------------------------------------------------------
# Dialogs
# ------------------------------------------------------------------------------

def execute_open_on_phone_dialog(sample_name: str = "") -> None:
    """Taps center button to trigger OpenOnPhone dialog and waits for progress arc to self-dismiss."""
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(4.5)
    time.sleep(1.5)


OPEN_ON_PHONE_DIALOG_HANDLER = GestureHandler(
    name="open_on_phone_dialog",
    action=execute_open_on_phone_dialog,
    triggers_on_load=False,
    description="Executing OpenOnPhone dialog choreography...",
)


def execute_alert_dialog_confirm_dismiss(sample_name: str = "") -> None:
    """Opens AlertDialog, scrolls down 2 items, and confirms action."""
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(2.0)
    utils.perform_synced_scroll_down(2, sy_pct=0.6, pause=0.5)
    time.sleep(0.5)
    utils.perform_synced_tap(0.70, 0.80)


ALERT_DIALOG_CONFIRM_DISMISS_HANDLER = GestureHandler(
    name="alert_dialog_confirm_dismiss",
    action=execute_alert_dialog_confirm_dismiss,
    triggers_on_load=False,
    description="Executing AlertDialog confirm/dismiss choreography...",
)


def execute_alert_dialog_content_groups(sample_name: str = "") -> None:
    """Opens AlertDialog with content groups, scrolls down 3 items, and dismisses action."""
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(2.0)
    utils.perform_synced_scroll_down(3, sy_pct=0.6, pause=0.5)
    time.sleep(0.5)
    utils.perform_synced_tap(0.5, 0.90)


ALERT_DIALOG_CONTENT_GROUPS_HANDLER = GestureHandler(
    name="alert_dialog_content_groups",
    action=execute_alert_dialog_content_groups,
    triggers_on_load=False,
    description="Executing AlertDialog content groups choreography...",
)


def execute_alert_dialog_edge_button(sample_name: str = "") -> None:
    """Opens AlertDialog with EdgeButton and taps bottom EdgeButton action."""
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(2.0)
    utils.perform_synced_tap(0.5, 0.90)


ALERT_DIALOG_EDGE_BUTTON_HANDLER = GestureHandler(
    name="alert_dialog_edge_button",
    action=execute_alert_dialog_edge_button,
    triggers_on_load=False,
    description="Executing AlertDialog EdgeButton choreography...",
)


# ------------------------------------------------------------------------------
# Pagers & Scaffolds
# ------------------------------------------------------------------------------

def _run_pager_sequence(start_x: float, start_y: float, end_x: float, end_y: float) -> None:
    """Executes a 4-page forward and backward swipe sequence preserving settling pauses."""
    for pause in (1.0, 1.0, 1.5):
        utils.perform_synced_swipe(start_x, start_y, end_x, end_y, 800)
        time.sleep(pause)
    for pause in (1.0, 1.0, 1.5):
        utils.perform_synced_swipe(end_x, end_y, start_x, start_y, 800)
        time.sleep(pause)


def execute_horizontal_pager(sample_name: str = "") -> None:
    """Executes horizontal page swipe sequence."""
    _run_pager_sequence(start_x=0.9, start_y=0.5, end_x=0.1, end_y=0.5)


HORIZONTAL_PAGER_HANDLER = GestureHandler(
    name="horizontal_pager",
    action=execute_horizontal_pager,
    triggers_on_load=False,
    description="Executing horizontal pager swipe choreography...",
)


def execute_vertical_pager(sample_name: str = "") -> None:
    """Executes vertical page swipe sequence."""
    _run_pager_sequence(start_x=0.5, start_y=0.9, end_x=0.5, end_y=0.1)


VERTICAL_PAGER_HANDLER = GestureHandler(
    name="vertical_pager",
    action=execute_vertical_pager,
    triggers_on_load=False,
    description="Executing vertical pager swipe choreography...",
)


def execute_scaffold_scroll_gestures(sample_name: str = "") -> None:
    """Scrolls down list and swipes back up to original position."""
    utils.perform_synced_scroll_down(1, sy_pct=0.8, pause=1.0)
    utils.perform_synced_swipe(0.5, 0.2, 0.5, 0.8, 800)
    time.sleep(2.0)


SCAFFOLD_SCROLL_HANDLER = GestureHandler(
    name="scaffold_scroll",
    action=execute_scaffold_scroll_gestures,
    triggers_on_load=False,
    description="Executing scroll away / scaffold choreography...",
)


# ------------------------------------------------------------------------------
# Swipe-To-Reveal
# ------------------------------------------------------------------------------

def execute_swipe_to_reveal_default(sample_name: str = "") -> None:
    """Performs partial reveal, complete reveal, and undo action."""
    utils.perform_synced_swipe(0.9, 0.5, 0.45, 0.5, 500)
    time.sleep(1.0)
    utils.perform_synced_swipe(0.45, 0.5, 0.05, 0.5, 300)
    time.sleep(1.0)
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(2.5)


SWIPE_TO_REVEAL_DEFAULT_HANDLER = GestureHandler(
    name="swipe_to_reveal_default",
    action=execute_swipe_to_reveal_default,
    triggers_on_load=False,
    description="Executing SwipeToReveal choreography...",
)


def execute_swipe_to_reveal_single_action_card(sample_name: str = "") -> None:
    """Performs clear partial reveal, complete reveal, and undo action."""
    utils.perform_synced_swipe(0.9, 0.5, 0.45, 0.5, 800)
    time.sleep(1.5)
    utils.perform_synced_swipe(0.45, 0.5, 0.05, 0.5, 300)
    time.sleep(2.0)
    utils.perform_synced_tap(0.5, 0.5)
    time.sleep(2.5)


SWIPE_TO_REVEAL_SINGLE_ACTION_CARD_HANDLER = GestureHandler(
    name="swipe_to_reveal_single_action_card",
    action=execute_swipe_to_reveal_single_action_card,
    triggers_on_load=False,
    description="Executing SwipeToReveal single action card choreography...",
)


def execute_swipe_to_reveal_tlc(sample_name: str = "") -> None:
    """Performs small swipe to stop mid-stage and complete swipe to reveal."""
    utils.perform_synced_swipe(0.9, 0.5, 0.45, 0.5, 600)
    time.sleep(1.0)
    utils.perform_synced_swipe(0.45, 0.5, 0.05, 0.5, 300)
    time.sleep(2.5)


SWIPE_TO_REVEAL_TLC_HANDLER = GestureHandler(
    name="swipe_to_reveal_tlc",
    action=execute_swipe_to_reveal_tlc,
    triggers_on_load=False,
    description="Executing SwipeToReveal TransformingLazyColumn choreography...",
)


def execute_swipe_to_reveal_no_partial_reveal(sample_name: str = "") -> None:
    """Focuses on list item two and deletes item 3 times with full swipes."""
    utils.perform_synced_swipe(0.5, 0.7, 0.5, 0.4, 500)
    time.sleep(1.0)
    for _ in range(3):
        utils.perform_synced_swipe(0.9, 0.5, 0.1, 0.5, 800)
        time.sleep(1.5)
    time.sleep(1.5)


SWIPE_TO_REVEAL_NO_PARTIAL_REVEAL_HANDLER = GestureHandler(
    name="swipe_to_reveal_no_partial_reveal",
    action=execute_swipe_to_reveal_no_partial_reveal,
    triggers_on_load=False,
    description="Executing SwipeToReveal no partial reveal choreography...",
)


# ==============================================================================
# SECTION 3: Direct Sample-to-Gesture Dictionary Map (Single Source of Truth)
# ==============================================================================

SAMPLE_GESTURES_MAP: Dict[str, GestureHandler] = {
    # --- Confirmation Dialogs (Autoplay) ---
    "ConfirmationDialogSample": AUTOPLAY_HANDLER,
    "LongTextConfirmationDialogSample": AUTOPLAY_HANDLER,
    "SuccessConfirmationDialogSample": AUTOPLAY_HANDLER,
    "FailureConfirmationDialogSample": AUTOPLAY_HANDLER,
    "FailureConfirmationDialogWithGenericFailureIconSample": AUTOPLAY_HANDLER,

    # --- Progress Indicators (Autoplay) ---
    "IndeterminateProgressArcSample": AUTOPLAY_HANDLER,
    "IndeterminateProgressIndicatorSample": AUTOPLAY_HANDLER,
    "CircularProgressIndicatorCustomAnimationSample": AUTOPLAY_HANDLER,

    # --- Animated Text & Placeholders (Autoplay) ---
    "AnimatedTextSample": AUTOPLAY_HANDLER,
    "AnimatedTextSampleSharedFontRegistry": AUTOPLAY_HANDLER,
    "TextPlaceholder": AUTOPLAY_HANDLER,

    # --- Cached & Placeholder Buttons (Autoplay) ---
    "ButtonWithIconAndLabelAndPlaceholders": AUTOPLAY_HANDLER,
    "ButtonWithIconAndLabelCachedData": AUTOPLAY_HANDLER,

    # --- Phone Dialogs ---
    "OpenOnPhoneDialogSample": OPEN_ON_PHONE_DIALOG_HANDLER,

    # --- Alert Dialogs ---
    "AlertDialogWithConfirmAndDismissSample": ALERT_DIALOG_CONFIRM_DISMISS_HANDLER,
    "AlertDialogWithConfirmAndDismissTransformingContentSample": ALERT_DIALOG_CONFIRM_DISMISS_HANDLER,
    "AlertDialogWithEdgeButtonSample": ALERT_DIALOG_EDGE_BUTTON_HANDLER,
    "AlertDialogWithContentGroupsSample": ALERT_DIALOG_CONTENT_GROUPS_HANDLER,
    "AlertDialogWithEdgeButtonTransformingContentSample": ALERT_DIALOG_EDGE_BUTTON_HANDLER,
    "AlertDialogWithContentGroupsTransformingContentSample": ALERT_DIALOG_CONTENT_GROUPS_HANDLER,

    # --- Buttons & Button Groups ---
    "ButtonGroupSample": BUTTON_GROUP_TWO_HANDLER,
    "ButtonGroupThreeButtonsSample": BUTTON_GROUP_THREE_HANDLER,
    "IconButtonWithCornerAnimationSample": DOUBLE_TAP_CENTER_HANDLER,
    "TextButtonWithCornerAnimationSample": DOUBLE_TAP_CENTER_HANDLER,
    "FadingExpandingLabelButtonSample": TRIPLE_TAP_CENTER_HANDLER,

    # --- Toggle Buttons ---
    "IconToggleButtonSample": DUAL_ICON_TOGGLE_HANDLER,
    "IconToggleButtonVariantSample": DUAL_ICON_TOGGLE_HANDLER,
    "LargeTextToggleButtonSample": DOUBLE_TAP_CENTER_HANDLER,
    "TextToggleButtonSample": DOUBLE_TAP_CENTER_HANDLER,
    "TextToggleButtonVariantSample": DOUBLE_TAP_CENTER_HANDLER,

    # --- Interactive Animated Text ---
    "AnimatedTextSampleButtonResponse": ANIMATED_TEXT_BUTTON_RESPONSE_HANDLER,

    # --- Swipe to Reveal ---
    "SwipeToRevealSample": SWIPE_TO_REVEAL_DEFAULT_HANDLER,
    "SwipeToRevealSingleActionCardSample": SWIPE_TO_REVEAL_SINGLE_ACTION_CARD_HANDLER,
    "SwipeToRevealWithTransformingLazyColumnSample": SWIPE_TO_REVEAL_TLC_HANDLER,
    "SwipeToRevealNoPartialRevealWithScalingLazyColumnSample": SWIPE_TO_REVEAL_NO_PARTIAL_REVEAL_HANDLER,

    # --- Pagers & Scaffolds ---
    "HorizontalPageIndicatorWithPagerSample": HORIZONTAL_PAGER_HANDLER,
    "HorizontalPagerScaffoldSample": HORIZONTAL_PAGER_HANDLER,
    "HorizontalPagerScaffoldWithLowSensitivitySample": HORIZONTAL_PAGER_HANDLER,
    "VerticalPageIndicatorWithPagerSample": VERTICAL_PAGER_HANDLER,
    "VerticalPagerScaffoldSample": VERTICAL_PAGER_HANDLER,
    "VerticalPagerScaffoldWithLowSensitivitySample": VERTICAL_PAGER_HANDLER,
    "ScaffoldWithTLCEdgeButtonSample": SCAFFOLD_SCROLL_HANDLER,
    "ScrollAwaySample": SCAFFOLD_SCROLL_HANDLER,

    # --- One-Handed Gestures (Single Flick) ---
    "OneHandedGestureButtonSample": OHG_SINGLE_FLICK_HANDLER,
    "ButtonContentWithOneHandedGestureSample": OHG_SINGLE_FLICK_HANDLER,
    "CompactButtonContentWithOneHandedGestureSample": OHG_SINGLE_FLICK_HANDLER,
    "AppCardContentWithOneHandedGestureSample": OHG_SINGLE_FLICK_HANDLER,
    "TitleCardContentWithOneHandedGestureSample": OHG_SINGLE_FLICK_HANDLER,

    # --- One-Handed Gestures (Double Flick) ---
    "OneHandedGestureDisableButtonSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureHorizontalPagerSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureScalingLazyColumnSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureScalingLazyColumnScrollToNextItemSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureTransformingLazyColumnSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureTransformingLazyColumnScrollToNextItemSample": OHG_DOUBLE_FLICK_HANDLER,
    "OneHandedGestureVerticalPagerSample": OHG_DOUBLE_FLICK_HANDLER,
}


# ==============================================================================
# SECTION 4: Dispatch & Discovery APIs
# ==============================================================================

def get_media_capture_gesture_handler(
    sample_name: str,
) -> GestureHandler:
    """
    Returns the gesture handler registered for the given sample_name.
    Raises an explicit KeyError with troubleshooting context if sample is unmapped.
    """
    if sample_name not in SAMPLE_GESTURES_MAP:
        raise KeyError(
            f"Sample '{sample_name}' is not registered in gestures.SAMPLE_GESTURES_MAP. "
            f"Please add a mapping for '{sample_name}' in scripts/gestures.py."
        )
    return SAMPLE_GESTURES_MAP[sample_name]
