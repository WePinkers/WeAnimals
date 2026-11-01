package com.example.weanimals

import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weanimals.user.map.overview.presentation.MapActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapSearchUiTest {
    @Test fun manualSearchClearAndRealDeviceGeocoder(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityScenario.launch<MapActivity>(MapActivity.newIntent(context, MapActivity.FILTER_LOST_FOUND)).use { scenario ->
            val ready = CountDownLatch(1)
            lateinit var map: MapLibreMap
            scenario.onActivity { activity ->
                activity.findViewById<MapView>(R.id.map_view).getMapAsync { value ->
                    map = value
                    value.setStyle(Style.Builder().fromJson(
                        """{"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#fafafa"}}]}"""
                    )) { ready.countDown() }
                }
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS))
            onView(withId(R.id.map_search_input)).check(matches(withHint("Buscar bairro ou endereço")))
            onView(withId(R.id.map_search_submit)).perform(click())
            onView(withId(R.id.map_search_message)).check(matches(withText(R.string.map_search_empty)))
            onView(withId(R.id.map_search_input)).perform(replaceText("Texto ainda não enviado"))
            delay(300)
            onView(withId(R.id.map_search_progress)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.map_search_message)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            scenario.onActivity { activity ->
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)!!
                val top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout()).top
                val position = IntArray(2)
                activity.findViewById<View>(R.id.map_search_controls).getLocationOnScreen(position)
                assertTrue(position[1] >= top + (8 * activity.resources.displayMetrics.density).toInt())
                assertEquals(EditorInfo.IME_ACTION_SEARCH,
                    activity.findViewById<EditText>(R.id.map_search_input).imeOptions and EditorInfo.IME_MASK_ACTION)
            }
            val queries = listOf("Vila Maria, São Paulo", "Mooca, São Paulo", "Avenida Paulista, São Paulo")
            queries.forEachIndexed { index, query ->
                onView(withId(R.id.map_search_input)).perform(replaceText(query))
                if (index == 0) onView(withId(R.id.map_search_input)).perform(pressImeActionButton())
                else onView(withId(R.id.map_search_submit)).perform(click())
                withTimeout(20_000) {
                    var finished = false
                    while (!finished) {
                        scenario.onActivity { activity ->
                            finished = activity.findViewById<View>(R.id.map_search_progress).visibility == View.GONE
                        }
                        if (!finished) delay(50)
                    }
                }
                delay(500) // MapLibre's camera animation is outside Espresso's idling resources.
                scenario.onActivity { activity ->
                    val message = activity.findViewById<TextView>(R.id.map_search_message)
                    if (message.visibility == View.VISIBLE) {
                        val allowed = listOf(R.string.map_search_not_found, R.string.map_search_offline,
                            R.string.map_search_unavailable, R.string.map_search_failed).map { activity.getString(it) }
                        assertTrue(allowed.contains(message.text.toString()))
                        Log.i("MapSearchValidation", "$query -> ${message.text}")
                    } else {
                        val target = map.cameraPosition.target!!
                        assertTrue(target.latitude in -24.0..-23.0 && target.longitude in -47.0..-46.0)
                        assertEquals(14.0, map.cameraPosition.zoom, 0.1)
                        Log.i("MapSearchValidation", "$query -> FOUND")
                    }
                }
                onView(withId(R.id.chip_lost_found)).check(matches(isChecked()))
                onView(withId(R.id.chip_secondary_all)).check(matches(isChecked()))
            }
            lateinit var beforeClear: org.maplibre.android.camera.CameraPosition
            scenario.onActivity { beforeClear = map.cameraPosition }
            onView(withId(R.id.map_search_clear)).perform(click())
            onView(withId(R.id.map_search_input)).check(matches(withText("")))
            scenario.onActivity {
                assertEquals(beforeClear.target!!.latitude, map.cameraPosition.target!!.latitude, 0.0001)
                assertEquals(beforeClear.target!!.longitude, map.cameraPosition.target!!.longitude, 0.0001)
            }
        }
    }
}
