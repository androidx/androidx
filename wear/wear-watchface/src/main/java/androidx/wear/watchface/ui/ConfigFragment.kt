/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.watchface.Constants
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.complications.ProviderInfoRetriever
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.R
import androidx.wear.watchface.style.UserStyle
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

/**
 * Top level configuration fragment. Lets the user select whether they want to select a complication
 * to configure, configure a background complication or select an option from a user style setting.
 * Should only be used if theres's at least two items from that list.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ConfigFragment : Fragment() {

    private lateinit var providerInfoRetriever: ProviderInfoRetriever
    private lateinit var watchFaceConfigActivity: WatchFaceConfigActivity
    private lateinit var view: SwipeDismissFrameLayout
    private lateinit var configViewAdapter: ConfigViewAdapter

    @SuppressWarnings("deprecation")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        watchFaceConfigActivity = activity as WatchFaceConfigActivity
        providerInfoRetriever = ProviderInfoRetriever(activity as WatchFaceConfigActivity)

        initConfigOptions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.config_layout, container, false) as
            SwipeDismissFrameLayout

        view.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentFragmentManager.popBackStackImmediate()
            }
        })

        return view
    }

    private fun initConfigOptions() {
        val hasBackgroundComplication =
            watchFaceConfigActivity.watchFaceConfigDelegate.getBackgroundComplicationId() != null
        val numComplications =
            watchFaceConfigActivity.watchFaceConfigDelegate.getComplicationsMap().size
        val hasNonBackgroundComplication =
            numComplications > if (hasBackgroundComplication) 1 else 0
        val configOptions = ArrayList<ConfigOption>()

        if (hasNonBackgroundComplication) {
            configOptions.add(
                ConfigOption(
                    id = Constants.KEY_COMPLICATIONS_SETTINGS,
                    icon =
                        Icon.createWithResource(
                            context,
                            R.drawable.ic_elements_settings_complications
                        ),
                    title = resources.getString(R.string.settings_complications),
                    summary = ""
                )
            )
        }

        if (hasBackgroundComplication) {
            configOptions.add(createBackgroundConfigOption())
        }

        for (styleCategory in watchFaceConfigActivity.styleSchema.userStyleSettings) {
            configOptions.add(
                ConfigOption(
                    id = styleCategory.id,
                    icon = styleCategory.icon,
                    title = styleCategory.displayName.toString(),
                    summary = styleCategory.description.toString()
                )
            )
        }

        configViewAdapter = ConfigViewAdapter(
            requireContext(),
            configOptions,
            this::onItemClick
        )
        view.findViewById<WearableRecyclerView>(R.id.configOptionsList).apply {
            adapter = configViewAdapter
            layoutManager = WearableLinearLayoutManager(context)
        }
    }

    private fun createBackgroundConfigOption(): ConfigOption {
        // Initially assume there is no background image provider.
        val backgroundConfigOption = ConfigOption(
            id = Constants.KEY_BACKGROUND_IMAGE_SETTINGS,
            icon = Icon.createWithResource(
                context,
                R.drawable.ic_elements_comps_bg
            ),
            title = getResources().getString(R.string.settings_background_image),
            summary = resources.getString(R.string.none_background_image_provider)
        )

        // Update the summary with the actual background complication provider name, if there is
        // one.
        val future = providerInfoRetriever.retrieveProviderInfo(
            watchFaceConfigActivity.watchFaceComponentName,
            intArrayOf(watchFaceConfigActivity.backgroundComplicationId!!)
        )
        future.addListener(
            {
                val provideInfo = future.get()
                provideInfo.info?.apply {
                    backgroundConfigOption.summary = providerName!!
                    configViewAdapter.notifyDataSetChanged()
                }
            },
            { runnable -> runnable.run() }
        )
        return backgroundConfigOption
    }

    /** Called with the result from the call to watchFaceImpl.onComplicationConfigTap() above. */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.PROVIDER_CHOOSER_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK
        ) {
            activity?.finish()
        }
    }

    override fun onDestroy() {
        providerInfoRetriever.close()
        super.onDestroy()
    }

    private fun onItemClick(configKey: String) {
        when (configKey) {
            Constants.KEY_COMPLICATIONS_SETTINGS ->
                watchFaceConfigActivity.fragmentController.showComplicationConfigSelectionFragment()

            Constants.KEY_BACKGROUND_IMAGE_SETTINGS -> {
                val backgroundComplication =
                    watchFaceConfigActivity.watchFaceConfigDelegate.getComplicationsMap()[
                        watchFaceConfigActivity.backgroundComplicationId!!
                    ]!!
                watchFaceConfigActivity.fragmentController.showComplicationConfig(
                    backgroundComplication.id,
                    *ComplicationType.toWireTypes(backgroundComplication.supportedTypes)
                )
            }

            else -> {
                watchFaceConfigActivity.fragmentController.showStyleConfigFragment(
                    configKey,
                    watchFaceConfigActivity.styleSchema,
                    UserStyle(
                        watchFaceConfigActivity.watchFaceConfigDelegate.getUserStyle(),
                        watchFaceConfigActivity.styleSchema
                    )
                )
            }
        }
    }
}

internal data class ConfigOption(
    val id: String,
    val icon: Icon?,
    val title: String,
    var summary: String
)

internal class ConfigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var configOption: ConfigOption? = null
}

internal class Helper {
    companion object {
        /**
         * Wraps a given [Drawable] with a standard background to match the normal preference icon
         * styling. The wrapping is idempotent, calling it multiple times will only wrap the icon
         * once.
         *
         * @param context the current [Context], used for resolving resources.
         * @param icon the icon to wrap.
         * @return the wrapped icon.
         */
        fun wrapIcon(
            context: Context,
            icon: Drawable
        ): Drawable {
            if (icon is LayerDrawable && icon.findDrawableByLayerId(R.id.nested_icon) != null) {
                return icon // icon was already wrapped, return the icon without modifying it
            }
            val wrappedDrawable =
                (context.getDrawable(R.drawable.preference_wrapped_icon) as LayerDrawable)
            wrappedDrawable.setDrawableByLayerId(R.id.nested_icon, icon)
            return wrappedDrawable
        }
    }
}

/** Adapter for top level config options. */
internal class ConfigViewAdapter(
    private val context: Context,
    private val configOptions: List<ConfigOption>,
    val clickListener: (String) -> Unit
) :
    RecyclerView.Adapter<ConfigViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfigViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.configlist_item_layout,
            parent,
            false
        )
    ).apply {
        itemView.setOnClickListener { clickListener(configOption!!.id) }
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val configOption = configOptions[position]
        holder.configOption = configOption
        val textView = holder.itemView as TextView

        val builder = SpannableStringBuilder()
        builder.append(configOption.title)
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, configOption.title.length, 0)
        builder.append("\n")
        builder.append(configOption.summary)
        textView.text = builder

        configOption.icon?.loadDrawableAsync(
            context,
            { drawable ->
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    Helper.wrapIcon(context, drawable),
                    /* top = */ null,
                    /* end = */ null,
                    /* bottom = */ null
                )
            },
            handler
        )
    }

    override fun getItemCount() = configOptions.size
}
