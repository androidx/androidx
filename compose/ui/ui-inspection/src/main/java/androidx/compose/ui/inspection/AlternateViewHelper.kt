/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.annotation.RequiresApi
import androidx.inspection.InspectorEnvironment
import java.lang.reflect.Field
import java.lang.reflect.Method

private const val PANEL_ENTITY_CLASS = "com.google.vr.androidx.xr.core.PanelEntity"
private const val PANEL_ENTITY_IMPL_CLASS =
    "com.google.vr.realitycore.runtime.androidxr.PanelEntityImpl"
private const val JXR_CORE_SESSION_CLASS = "com.google.vr.androidx.xr.core.Session"

private const val GET_ENTITIES_OF_TYPE_METHOD = "getEntitiesOfType"
private const val IS_HIDDEN_METHOD = "isHidden"

private const val SURFACE_CONTROL_VIEW_HOST_FIELD = "surfaceControlViewHost"
private const val RT_PANEL_ENTITY_FIELD = "rtPanelEntity"

class AlternateViewHelper(private val environment: InspectorEnvironment) {
    private val activity = environment.artTooling().findInstances(Activity::class.java).first()

    fun getAlternateViews(): List<View> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getExtraViewsImpl() else emptyList()
        } catch (ex: Exception) {
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getExtraViewsImpl(): List<View> {
        val sessionClass = loadClass(JXR_CORE_SESSION_CLASS)
        val xrSessions = environment.artTooling().findInstances(sessionClass)
        return xrSessions.flatMap { getSessionViews(it) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("BanUncheckedReflection")
    private fun getSessionViews(session: Any): List<View> {
        val entitiesFun =
            loadMethod(session.javaClass, GET_ENTITIES_OF_TYPE_METHOD, Class::class.java)
        val entityClass = loadClass(PANEL_ENTITY_CLASS)
        val entities = entitiesFun.invoke(session, entityClass) as List<*>
        return entities.mapNotNull { entity -> entity?.let { getView(it) } }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getView(entity: Any): View? {
        if (isHidden(entity)) {
            return null
        }
        return entity
            .mapAllFields { field ->
                if (field.name == RT_PANEL_ENTITY_FIELD) {
                    getRuntimeEntityView(field.get(entity)!!)
                } else {
                    null
                }
            }
            .filterNotNull()
            .firstOrNull()
    }

    @SuppressLint("BanUncheckedReflection")
    private fun isHidden(instance: Any): Boolean {
        var isHidden = false

        runCatching {
            instance.mapAllMethods { method ->
                if (method.name == IS_HIDDEN_METHOD) {
                    isHidden = method.invoke(instance, true) as Boolean
                    return@mapAllMethods
                }
            }
        }

        return isHidden
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getRuntimeEntityView(instance: Any): View? {
        val clazz = instance.javaClass
        if (clazz.name != PANEL_ENTITY_IMPL_CLASS) {
            return null
        }
        val surfaceControlViewHostField = loadField(clazz, SURFACE_CONTROL_VIEW_HOST_FIELD)
        if (surfaceControlViewHostField != null) {
            surfaceControlViewHostField.isAccessible = true
            val surfaceControlViewHost =
                surfaceControlViewHostField.get(instance) as SurfaceControlViewHost
            return surfaceControlViewHost.view
        } else {
            return null
        }
    }

    @Suppress("UnnecessaryLambdaCreation")
    private fun <T> Any.mapAllFields(block: (filed: Field) -> T): List<T> {
        var clazz: Class<*>? = javaClass
        val results = mutableListOf<T>()

        while (clazz != Any::class.java && clazz != null) {
            results.addAll(loadFields(clazz).map { block(it) })

            // Move to the superclass
            clazz = clazz.superclass
        }

        return results
    }

    @Suppress("UnnecessaryLambdaCreation")
    private fun <T> Any.mapAllMethods(block: (method: Method) -> T): List<T> {
        var clazz: Class<*>? = javaClass
        val results = mutableListOf<T>()

        while (clazz != Any::class.java && clazz != null) {
            results.addAll(loadMethods(clazz).map { block(it) })

            // Move to the superclass
            clazz = clazz.superclass
        }

        return results
    }

    private fun loadClass(name: String): Class<*> = activity.classLoader.loadClass(name)

    private fun loadMethod(cls: Class<*>, name: String, vararg args: Class<*>): Method =
        cls.getDeclaredMethod(name, *args).apply { isAccessible = true }

    private fun loadMethods(cls: Class<*>): List<Method> =
        cls.declaredMethods.map { it.apply { isAccessible = true } }

    private fun loadField(cls: Class<*>, name: String): Field? =
        runCatching { cls.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()

    private fun loadFields(cls: Class<*>): List<Field> =
        cls.declaredFields.map { it.apply { it.isAccessible = true } }
}
