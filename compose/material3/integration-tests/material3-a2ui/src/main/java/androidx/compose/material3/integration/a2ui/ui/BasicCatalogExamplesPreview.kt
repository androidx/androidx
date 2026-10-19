/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.integration.a2ui.ui

import androidx.a2ui.compose.ui.tooling.preview.A2uiPreviewParameterProvider
import androidx.a2ui.compose.ui.tooling.preview.A2uiSurfaceModelFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp

@Preview(name = "A2UI Basic Catalog Examples", showBackground = true, widthDp = 380)
@Composable
fun BasicCatalogExamplesPreview(
    @PreviewParameter(BasicCatalogExampleProvider::class) surfaceFactory: A2uiSurfaceModelFactory
) {
    MaterialTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                A2uiSurface(surfaceModel = surfaceFactory.create())
            }
        }
    }
}

class BasicCatalogExampleProvider :
    A2uiPreviewParameterProvider(
        catalogProvider = { rememberBasicCatalog() },
        namedJsonMessages =
            mapOf(
                "Order Confirmation" to JsonPreviewPresets.OrderConfirmation,
                "Flight Status" to JsonPreviewPresets.FlightStatus,
                "Feedback Survey" to JsonPreviewPresets.FeedbackSurvey,
                "Restaurant Reservation" to JsonPreviewPresets.RestaurantReservation,
                "Product Overview Tabs" to JsonPreviewPresets.ProductOverviewTabs,
                "Notification Settings" to JsonPreviewPresets.NotificationSettings,
            ),
    )

object JsonPreviewPresets {
    val OrderConfirmation =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "main_col"
                  },
                  {
                    "id": "main_col",
                    "component": "Column",
                    "children": [
                      "title_text",
                      "order_info",
                      "divider_1",
                      "action_row"
                    ]
                  },
                  {
                    "id": "title_text",
                    "component": "Text",
                    "text": "🎉 Order Confirmed!"
                  },
                  {
                    "id": "order_info",
                    "component": "Text",
                    "text": "Your order #849204 has been received. Estimated delivery: Tomorrow by 2:00 PM."
                  },
                  {
                    "id": "divider_1",
                    "component": "Divider"
                  },
                  {
                    "id": "action_row",
                    "component": "Row",
                    "children": [
                      "track_btn",
                      "receipt_btn"
                    ]
                  },
                  {
                    "id": "track_btn",
                    "component": "Button",
                    "child": "track_label",
                    "variant": "primary",
                    "weight": 2.0,
                    "action": {
                      "event": {
                        "name": "track_delivery"
                      }
                    }
                  },
                  {
                    "id": "track_label",
                    "component": "Text",
                    "text": "Track Delivery"
                  },
                  {
                    "id": "receipt_btn",
                    "component": "Button",
                    "child": "receipt_label",
                    "variant": "borderless",
                    "weight": 1.0,
                    "action": {
                      "event": {
                        "name": "view_receipt"
                      }
                    }
                  },
                  {
                    "id": "receipt_label",
                    "component": "Text",
                    "text": "Receipt"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val FlightStatus =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "flight_col"
                  },
                  {
                    "id": "flight_col",
                    "component": "Column",
                    "children": [
                      "flight_header",
                      "flight_status",
                      "divider_flight",
                      "btn_pass"
                    ]
                  },
                  {
                    "id": "flight_header",
                    "component": "Text",
                    "text": "Flight AA 1024 • SFO ➔ JFK"
                  },
                  {
                    "id": "flight_status",
                    "component": "Text",
                    "text": "Gate B12 • Status: On Time • Boarding in 25 min"
                  },
                  {
                    "id": "divider_flight",
                    "component": "Divider"
                  },
                  {
                    "id": "btn_pass",
                    "component": "Button",
                    "child": "btn_pass_label",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "open_boarding_pass"
                      }
                    }
                  },
                  {
                    "id": "btn_pass_label",
                    "component": "Text",
                    "text": "View Digital Boarding Pass"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val FeedbackSurvey =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateDataModel": {
                "surfaceId": "preview_surface",
                "path": "/",
                "value": {
                  "rating": 7.0,
                  "feedback": ""
                }
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "survey_col"
                  },
                  {
                    "id": "survey_col",
                    "component": "Column",
                    "align": "stretch",
                    "children": [
                      "survey_title",
                      "survey_slider",
                      "survey_input",
                      "survey_submit"
                    ]
                  },
                  {
                    "id": "survey_title",
                    "component": "Text",
                    "text": "How was your agent interaction today?"
                  },
                  {
                    "id": "survey_slider",
                    "component": "Slider",
                    "min": 0.0,
                    "max": 10.0,
                    "value": { "path": "/rating" },
                    "label": "Satisfaction Rating (0-10)"
                  },
                  {
                    "id": "survey_input",
                    "component": "TextField",
                    "value": { "path": "/feedback" },
                    "label": "Optional Feedback",
                    "placeholder": "What did you like or want improved?"
                  },
                  {
                    "id": "survey_submit",
                    "component": "Button",
                    "child": "survey_submit_label",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "submit_survey"
                      }
                    }
                  },
                  {
                    "id": "survey_submit_label",
                    "component": "Text",
                    "text": "Submit Feedback"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val RestaurantReservation =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateDataModel": {
                "surfaceId": "preview_surface",
                "path": "/",
                "value": {
                  "selectedTime": ["19:30"],
                  "outdoorSeating": true
                }
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "res_col"
                  },
                  {
                    "id": "res_col",
                    "component": "Column",
                    "children": [
                      "res_title",
                      "res_subtitle",
                      "res_divider",
                      "res_time_picker",
                      "res_outdoor_checkbox",
                      "res_confirm_btn"
                    ]
                  },
                  {
                    "id": "res_title",
                    "component": "Text",
                    "text": "🍽️ Osteria Mozza",
                    "variant": "h3"
                  },
                  {
                    "id": "res_subtitle",
                    "component": "Text",
                    "text": "Table for 2 • Tonight • Italian"
                  },
                  {
                    "id": "res_divider",
                    "component": "Divider"
                  },
                  {
                    "id": "res_time_picker",
                    "component": "ChoicePicker",
                    "label": "Available Times",
                    "variant": "mutuallyExclusive",
                    "displayStyle": "chips",
                    "options": [
                      { "label": "6:30 PM", "value": "18:30" },
                      { "label": "7:30 PM", "value": "19:30" },
                      { "label": "8:45 PM", "value": "20:45" }
                    ],
                    "value": { "path": "/selectedTime" }
                  },
                  {
                    "id": "res_outdoor_checkbox",
                    "component": "CheckBox",
                    "label": "Request heated patio seating",
                    "value": { "path": "/outdoorSeating" }
                  },
                  {
                    "id": "res_confirm_btn",
                    "component": "Button",
                    "child": "res_confirm_label",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "confirm_reservation"
                      }
                    }
                  },
                  {
                    "id": "res_confirm_label",
                    "component": "Text",
                    "text": "Reserve Table"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val ProductOverviewTabs =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "prod_col"
                  },
                  {
                    "id": "prod_col",
                    "component": "Column",
                    "align": "stretch",
                    "children": [
                      "prod_title",
                      "prod_price",
                      "prod_tabs",
                      "prod_buy_btn"
                    ]
                  },
                  {
                    "id": "prod_title",
                    "component": "Text",
                    "text": "🎧 Pixel Buds Pro 2",
                    "variant": "h3"
                  },
                  {
                    "id": "prod_price",
                    "component": "Text",
                    "text": "${'$'}229.00 • In Stock • Free 2-Day Shipping"
                  },
                  {
                    "id": "prod_tabs",
                    "component": "Tabs",
                    "tabs": [
                      { "title": "Highlights", "child": "tab_highlights" },
                      { "title": "Specs", "child": "tab_specs" },
                      { "title": "Box", "child": "tab_box" }
                    ]
                  },
                  {
                    "id": "tab_highlights",
                    "component": "Text",
                    "text": "Tensor A1 chip for 2x Active Noise Cancellation, Conversation Detection, and up to 30 hours of listening time with case."
                  },
                  {
                    "id": "tab_specs",
                    "component": "Text",
                    "text": "11mm custom dynamic driver • Bluetooth 5.4 • IP54 sweat & water resistant."
                  },
                  {
                    "id": "tab_box",
                    "component": "Text",
                    "text": "Earbuds, wireless charging case, and 4 pairs of eartips (XS, S, M, L)."
                  },
                  {
                    "id": "prod_buy_btn",
                    "component": "Button",
                    "child": "prod_buy_label",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "add_to_cart"
                      }
                    }
                  },
                  {
                    "id": "prod_buy_label",
                    "component": "Text",
                    "text": "Add to Cart"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val NotificationSettings =
        listOf(
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "preview_surface",
                "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateDataModel": {
                "surfaceId": "preview_surface",
                "path": "/",
                "value": {
                  "pushEnabled": true,
                  "emailDigest": false,
                  "securityAlerts": true
                }
              }
            }
            """
                .trimIndent(),
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Card",
                    "child": "notif_col"
                  },
                  {
                    "id": "notif_col",
                    "component": "Column",
                    "align": "end",
                    "children": [
                      "notif_col_top",
                      "notif_save_btn"
                    ]
                  },
                  {
                    "id": "notif_col_top",
                    "component": "Column",
                    "align": "stretch",
                    "children": [
                      "notif_title",
                      "notif_desc",
                      "notif_divider",
                      "cb_push",
                      "cb_email",
                      "cb_security",
                      "notif_divider"
                    ]
                  },
                  {
                    "id": "notif_title",
                    "component": "Text",
                    "text": "🔔 Notification Preferences",
                    "variant": "h3"
                  },
                  {
                    "id": "notif_desc",
                    "component": "Text",
                    "text": "Choose which updates you'd like the assistant to send you."
                  },
                  {
                    "id": "notif_divider",
                    "component": "Divider"
                  },
                  {
                    "id": "cb_push",
                    "component": "CheckBox",
                    "label": "Real-time push notifications",
                    "value": { "path": "/pushEnabled" }
                  },
                  {
                    "id": "cb_email",
                    "component": "CheckBox",
                    "label": "Weekly summary email digest",
                    "value": { "path": "/emailDigest" }
                  },
                  {
                    "id": "cb_security",
                    "component": "CheckBox",
                    "label": "Critical security & sign-in alerts",
                    "value": { "path": "/securityAlerts" }
                  },
                  {
                    "id": "notif_save_btn",
                    "component": "Button",
                    "child": "notif_save_label",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "save_preferences"
                      }
                    }
                  },
                  {
                    "id": "notif_save_label",
                    "component": "Text",
                    "text": "Save Preferences"
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )
}
