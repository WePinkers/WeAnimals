package com.example.weanimals

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.Press
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import com.example.weanimals.user.lostandfound.presentation.LostPetFormActivity
import com.example.weanimals.user.lostandfound.presentation.FoundPetFormActivity
import com.example.weanimals.user.lostandfound.presentation.AdjustLocationActivity
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.user.map.overview.domain.PublicOccurrence
import com.example.weanimals.user.map.overview.interactor.NearbyOccurrences
import androidx.test.espresso.action.ViewActions.swipeDown
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.weanimals.user.map.overview.presentation.MapActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LostAndFoundMapRepositoryTest {
    private fun publishUsingExistingForms(base: Context): List<FoundPetAd> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val before = LostAndFoundRepository.getAllAds(base).map { it.id }.toSet()
        val address = android.content.Intent()
            .putExtra(AdjustLocationActivity.EXTRA_STREET, "Praça XV")
            .putExtra(AdjustLocationActivity.EXTRA_NEIGHBORHOOD, "Centro")
            .putExtra(AdjustLocationActivity.EXTRA_CITY, "Rio de Janeiro")
            .putExtra(AdjustLocationActivity.EXTRA_STATE, "RJ")
            .putExtra(AdjustLocationActivity.EXTRA_LATITUDE, -22.9035)
            .putExtra(AdjustLocationActivity.EXTRA_LONGITUDE, -43.1746)
        val monitor = object : android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: android.content.Intent): android.app.Instrumentation.ActivityResult? {
                if (intent.component?.className == AdjustLocationActivity::class.java.name) {
                    // Fixed result contract: tests publication, not the external Geocoder provider.
                    return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK, address)
                }
                return null
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch<LostPetFormActivity>(
                android.content.Intent(base, LostPetFormActivity::class.java)
            ).use {
                onView(withId(R.id.et_pet_name)).perform(scrollTo(), replaceText("Publicação de teste"), closeSoftKeyboard())
                onView(withId(R.id.chip_species_cat)).perform(scrollTo(), click())
                onView(withId(R.id.chip_size_medium)).perform(scrollTo(), click())
                onView(withId(R.id.chip_age_adult)).perform(scrollTo(), click())
                onView(withId(R.id.btn_continue_step1)).perform(click())
                onView(withId(R.id.btn_continue_step2)).perform(click())
                onView(withId(R.id.btn_adjust_location)).perform(scrollTo(), click())
                onView(withId(R.id.btn_publish_lost)).perform(click())
            }
            ActivityScenario.launch<FoundPetFormActivity>(
                android.content.Intent(base, FoundPetFormActivity::class.java)
            ).use {
                onView(withId(R.id.chip_species_cat)).perform(scrollTo(), click())
                onView(withId(R.id.chip_size_medium)).perform(scrollTo(), click())
                onView(withId(R.id.chip_age_adult)).perform(scrollTo(), click())
                onView(withId(R.id.btn_continue_step1)).perform(click())
                onView(withId(R.id.btn_adjust_location)).perform(scrollTo(), click())
                onView(withId(R.id.btn_publish)).perform(click())
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        return LostAndFoundRepository.getAllAds(base).filter { it.id !in before }
            .sortedBy { if (it.category == "PERDIDO") 0 else 1 }
    }

    @Test fun publishesBothCategoriesUpdatesStatusAndPersistsWithoutDuplicatingIds() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            instrumentation.uiAutomation.grantRuntimePermission(base.packageName, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            instrumentation.uiAutomation.grantRuntimePermission(base.packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        // Exercise the production repository without writing to the user's announcements.
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("map_repository_test_" + name, mode)
        }
        val prefs = context.getSharedPreferences("lost_and_found_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val events = Channel<List<FoundPetAd>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            LostAndFoundRepository.observeAds(context).collect { events.send(it) }
        }
        try {
            withTimeout(5000) { events.receive() }
            val lost = FoundPetAd(id = "lost-map-test", ownerUserId = "test", species = "Gato",
                category = "PERDIDO", latitude = -23.5508, longitude = -46.6333,
                street = "PRIVATE_STREET", number = "123", complement = "PRIVATE_COMPLEMENT",
                marks = "Raça: SRD · Mancha branca · SEGREDO: PRIVATE_SECRET")
            val found = lost.copy(id = "found-map-test", category = "ENCONTRADO", latitude = 0.0)
            val bulk = (1..40).map { number ->
                lost.copy(id = "bulk-map-$number", latitude = 10.0, longitude = 10.0,
                    calledNames = listOf("Registro $number: reagiu"))
            }
            instrumentation.runOnMainSync {
                bulk.forEach { LostAndFoundRepository.saveAd(context, it) }
                LostAndFoundRepository.saveAd(context, lost)
                LostAndFoundRepository.saveAd(context, found)
                LostAndFoundRepository.saveAd(context, lost.copy(species = "Cão"))
            }
            withTimeout(5000) {
                while (events.receive().size != 42) { }
            }
            assertEquals(42, LostAndFoundRepository.getAllAds(context).size)
            ActivityScenario.launch<MapActivity>(MapActivity.newIntent(base)).use { scenario ->
                val styleReady = CountDownLatch(1)
                lateinit var testMap: MapLibreMap
                scenario.onActivity { activity ->
                    activity.findViewById<MapView>(R.id.map_view).getMapAsync { map ->
                        testMap = map
                        // Deterministic basemap: viewport/card tests must not depend on a tile server.
                        map.setStyle(Style.Builder().fromJson(
                            """{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#fafafa"}}]}"""
                        )) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.551, -46.633), 12.0))
                            styleReady.countDown()
                        }
                    }
                }
                assertTrue(styleReady.await(15, TimeUnit.SECONDS))
                scenario.onActivity { activity ->
                    val root = activity.window.decorView
                    val insets = androidx.core.view.ViewCompat.getRootWindowInsets(root)!!
                    val safeTop = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()).top
                    val bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
                    val filter = activity.findViewById<android.view.View>(R.id.filter_scroll_view)
                    val location = IntArray(2)
                    filter.getLocationOnScreen(location)
                    assertTrue(location[1] >= safeTop + (8 * activity.resources.displayMetrics.density).toInt())
                    val navigationItem = activity.findViewById<android.view.View>(R.id.nav_map)
                    val rect = android.graphics.Rect()
                    navigationItem.getGlobalVisibleRect(rect)
                    assertTrue(rect.bottom <= root.height - bottom)
                }
                // A real pan must prevent a late GPS result from moving the camera away.
                onView(withId(R.id.map_view)).perform(androidx.test.espresso.action.GeneralSwipeAction(
                    androidx.test.espresso.action.Swipe.SLOW,
                    { view ->
                        val origin = IntArray(2); view.getLocationOnScreen(origin)
                        floatArrayOf(origin[0] + view.width * 0.65f, origin[1] + view.height * 0.4f)
                    },
                    { view ->
                        val origin = IntArray(2); view.getLocationOnScreen(origin)
                        floatArrayOf(origin[0] + view.width * 0.35f, origin[1] + view.height * 0.4f)
                    }, Press.FINGER))
                scenario.onActivity { activity ->
                    // A controlled authorized location keeps the test independent of late emulator GPS responses.
                    activity.showOccurrences(NearbyOccurrences(Coordinates(-23.551, -46.633), emptyList()))
                    testMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.551, -46.633), 12.0))
                }
                onView(withId(R.id.chip_lost_found)).perform(scrollTo(), click())
                onView(withId(R.id.chip_secondary_all)).check(matches(isChecked()))
                onView(withId(R.id.chip_lost)).perform(scrollTo(), click())
                onView(withText("Cão perdido")).check(matches(isDisplayed()))
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("1 nesta região")))
                onView(withId(R.id.chip_found)).perform(scrollTo(), click())
                onView(withText("Nenhum registro encontrado nesta região.")).check(matches(isDisplayed()))
                scenario.onActivity {
                    testMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, -46.633), 12.0))
                }
                instrumentation.waitForIdleSync()
                onView(withText("Gato encontrado")).check(matches(isDisplayed()))
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("1 nesta região")))
                // MapLibre renders markers on a separate GL thread, outside Espresso's idling resources.
                kotlinx.coroutines.delay(350)
                onView(withId(R.id.map_view)).perform(GeneralClickAction(Tap.SINGLE, { view ->
                    val marker = testMap.markers.first { it.position.latitude == 0.0 && it.position.longitude < -46.0 }
                    val point = testMap.projection.toScreenLocation(marker.position)
                    val origin = IntArray(2)
                    view.getLocationOnScreen(origin)
                    floatArrayOf(origin[0] + point.x, origin[1] + point.y - 12 * view.resources.displayMetrics.density)
                }, Press.FINGER))
                withTimeout(5000) {
                    var selected = false
                    while (!selected) {
                        scenario.onActivity { activity ->
                            selected = activity.findViewById<android.widget.TextView>(R.id.animal_title)
                                .text.toString() == "Gato encontrado"
                        }
                        if (!selected) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withId(R.id.animal_title)).check(matches(withText("Gato encontrado")))
                onView(withId(R.id.animal_category)).check(matches(withText("Encontrado")))
                onView(withId(R.id.animal_main_action)).check(matches(withText("Acho que é meu")))
                onView(withId(R.id.animal_physical_details)).check(matches(
                    org.hamcrest.Matchers.not(withText(org.hamcrest.Matchers.containsString("PRIVATE_SECRET")))))
                onView(withId(R.id.close_animal_details)).perform(click())
                onView(withId(R.id.drag_handle)).perform(swipeUp())
                onView(withId(R.id.nav_map)).check(matches(isDisplayed()))
                onView(withId(R.id.drag_handle)).perform(swipeDown())
                onView(withId(R.id.nav_map)).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    LostAndFoundRepository.saveAd(context,
                        LostAndFoundRepository.getAdById(context, lost.id)!!.copy(latitude = 0.0, longitude = -46.64))
                    activity.showOccurrences(NearbyOccurrences(Coordinates(0.0, -46.633), listOf(
                        PublicOccurrence("report-high", "dog", "high", 0, -4664, System.currentTimeMillis()),
                        PublicOccurrence("report-medium", "cat", "medium", 1, -4664, System.currentTimeMillis()),
                        PublicOccurrence("report-low", "dog", "low", -1, -4664, System.currentTimeMillis())
                    )))
                }
                onView(withId(R.id.chip_all)).perform(scrollTo(), click())
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("5 nesta região")))
                onView(withId(R.id.chip_reports)).perform(scrollTo(), click())
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("3 nesta região")))
                listOf(R.id.chip_high, R.id.chip_medium, R.id.chip_low).forEach { chipId ->
                    onView(withId(chipId)).perform(scrollTo(), click())
                    onView(withId(R.id.occurrence_count_text)).check(matches(withText("1 nesta região")))
                }
                onView(withId(R.id.chip_high)).perform(scrollTo(), click())
                onView(withId(R.id.card_root)).perform(click())
                withTimeout(5000) {
                    var selected = false
                    while (!selected) {
                        scenario.onActivity { activity ->
                            selected = activity.findViewById<android.widget.TextView>(R.id.animal_category)
                                .text.toString() == "Denúncia"
                        }
                        if (!selected) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withId(R.id.view_animal_ad)).check(matches(withText("Ver denúncia")))
                onView(withId(R.id.animal_main_action)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                // Verify the existing report destination and ID without querying a fictitious server record.
                val reportIntent = java.util.concurrent.atomic.AtomicReference<android.content.Intent>()
                val monitor = object : android.app.Instrumentation.ActivityMonitor() {
                    override fun onStartActivity(intent: android.content.Intent): android.app.Instrumentation.ActivityResult? {
                        if (intent.component?.className == "com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity") {
                            reportIntent.set(intent)
                            return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
                        }
                        return null
                    }
                }
                instrumentation.addMonitor(monitor)
                try {
                    onView(withId(R.id.view_animal_ad)).perform(click())
                    assertEquals("report-high", reportIntent.get()?.getStringExtra("extra_report_id"))
                } finally {
                    instrumentation.removeMonitor(monitor)
                }
                onView(withId(R.id.close_animal_details)).perform(click())
                scenario.onActivity { activity ->
                    activity.showOccurrences(NearbyOccurrences(Coordinates(0.0, -46.633), emptyList()))
                    testMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(1.0, -46.633)))
                }
                onView(withId(R.id.btn_my_location)).perform(click())
                withTimeout(5000) {
                    var centered = false
                    while (!centered) {
                        scenario.onActivity {
                            centered = kotlin.math.abs(testMap.cameraPosition.target!!.latitude) < 0.01
                        }
                        if (!centered) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withId(R.id.chip_all)).perform(scrollTo(), click())
                onView(withId(R.id.secondary_filter_scroll)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                scenario.onActivity {
                    testMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(10.0, 10.0), 12.0))
                }
                instrumentation.waitForIdleSync()
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("40 nesta região")))
                onView(withId(R.id.chip_lost_found)).perform(scrollTo(), click())
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("40 nesta região")))
                scenario.onActivity { activity ->
                    BottomSheetBehavior.from(activity.findViewById<LinearLayout>(R.id.bottom_sheet)).state =
                        BottomSheetBehavior.STATE_EXPANDED
                }
                withTimeout(5000) {
                    var expanded = false
                    while (!expanded) {
                        scenario.onActivity { activity ->
                            expanded = BottomSheetBehavior.from(activity.findViewById<LinearLayout>(R.id.bottom_sheet)).state ==
                                BottomSheetBehavior.STATE_EXPANDED
                        }
                        if (!expanded) kotlinx.coroutines.delay(50)
                    }
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val recycler = activity.findViewById<RecyclerView>(R.id.recycler_occurrences)
                    assertTrue(recycler.childCount < 40)
                    recycler.scrollToPosition(39)
                }
                instrumentation.waitForIdleSync()
                onView(withText("Registro 1")).check(matches(isDisplayed()))
                onView(withText("Registro 1")).perform(click())
                withTimeout(5000) {
                    var visible = false
                    while (!visible) {
                        scenario.onActivity { activity ->
                            visible = activity.findViewById<android.widget.TextView>(R.id.animal_title)
                                .text.toString() == "Registro 1" &&
                                activity.findViewById<android.view.View>(R.id.animal_details).visibility == android.view.View.VISIBLE
                        }
                        if (!visible) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withId(R.id.animal_category)).check(matches(withText("Perdido")))
                onView(withId(R.id.animal_main_action)).check(matches(withText("Acho que encontrei")))
                onView(withId(R.id.view_animal_ad)).perform(click())
                // Reuse the public announcement page by ID.
                val opened = withTimeout(5000) {
                    var value = false
                    while (!value) {
                        instrumentation.runOnMainSync {
                            value = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                                .any { it is com.example.weanimals.user.lostandfound.presentation.PublicAdDetailActivity }
                        }
                        if (!value) kotlinx.coroutines.delay(100)
                    }
                    value
                }
                assertTrue(opened)
                onView(withId(R.id.tv_location_text_public)).check(matches(
                    org.hamcrest.Matchers.not(withText(org.hamcrest.Matchers.containsString("PRIVATE_STREET")))))
                pressBack()
                onView(withId(R.id.animal_title)).check(matches(withText("Registro 1")))
                onView(withId(R.id.close_animal_details)).perform(click())
                scenario.onActivity { LostAndFoundRepository.endAd(context, "bulk-map-1") }
                withTimeout(5000) {
                    var removed = false
                    while (!removed) {
                        scenario.onActivity { activity ->
                            removed = activity.findViewById<android.widget.TextView>(R.id.occurrence_count_text)
                                .text.toString() == "39 nesta região"
                        }
                        if (!removed) kotlinx.coroutines.delay(50)
                    }
                }
                assertTrue(LostAndFoundRepository.getAdById(context, "bulk-map-1")!!.isEnded)
                scenario.onActivity {
                    assertEquals(10.0, testMap.cameraPosition.target!!.latitude, 0.01)
                    BottomSheetBehavior.from(it.findViewById<LinearLayout>(R.id.bottom_sheet)).state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
                onView(withId(R.id.nav_map)).check(matches(isDisplayed()))
                onView(withId(R.id.chip_lost)).perform(scrollTo(), click())
                scenario.recreate()
                onView(withId(R.id.chip_lost_found)).check(matches(isChecked()))
                onView(withId(R.id.chip_lost)).check(matches(isChecked()))
                val recreatedReady = CountDownLatch(1)
                scenario.onActivity { activity ->
                    activity.findViewById<MapView>(R.id.map_view).getMapAsync { map ->
                        assertEquals(10.0, map.cameraPosition.target!!.latitude, 0.01)
                        recreatedReady.countDown()
                    }
                }
                assertTrue(recreatedReady.await(10, TimeUnit.SECONDS))
                val original = java.util.concurrent.atomic.AtomicReference<MapActivity>()
                scenario.onActivity { original.set(it) }
                onView(withId(R.id.nav_profile)).perform(click())
                onView(withId(R.id.item_lost_found)).perform(scrollTo(), click())
                // The NavigationBar entry chooses all categories.
                onView(withId(R.id.nav_map)).perform(click())
                onView(withId(R.id.chip_all)).check(matches(isChecked()))
                scenario.onActivity { activity -> assertSame(original.get(), activity) }
                // The dedicated button chooses animals on that same MapActivity.
                onView(withId(R.id.nav_profile)).perform(click())
                onView(withId(R.id.item_lost_found)).perform(scrollTo(), click())
                onView(withId(R.id.btn_view_map)).perform(scrollTo(), click())
                onView(withId(R.id.chip_lost_found)).check(matches(isChecked()))
                onView(withId(R.id.chip_secondary_all)).check(matches(isChecked()))
                // Reorder-to-front keeps the origin immediately below the map.
                pressBack()
                onView(withId(R.id.btn_view_map)).check(matches(isDisplayed()))
            }
            val stored = JSONArray(prefs.getString("saved_ads", "[]"))
            assertEquals(42, stored.length())
            val foundJson = (0 until stored.length()).map { stored.getJSONObject(it) }
                .first { it.getString("id") == found.id }
            assertEquals(0.0, foundJson.getDouble("latitude"), 0.0)
            assertEquals(-46.6333, foundJson.getDouble("longitude"), 0.0)
            val published = publishUsingExistingForms(base)
            assertEquals(setOf("PERDIDO", "ENCONTRADO"), published.map { it.category }.toSet())
            assertTrue(published.all { it.latitude == -22.9035 && it.longitude == -43.1746 })
            ActivityScenario.launch<MapActivity>(MapActivity.newIntent(base, MapActivity.FILTER_LOST_FOUND, published[0].id)).use { scenario ->
                val ready = CountDownLatch(1)
                scenario.onActivity { activity ->
                    activity.findViewById<MapView>(R.id.map_view).getMapAsync { map ->
                        map.setStyle(Style.Builder().fromJson(
                            """{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#fafafa"}}]}"""
                        )) { ready.countDown() }
                    }
                }
                assertTrue(ready.await(15, TimeUnit.SECONDS))
                withTimeout(5000) {
                    var shown = false
                    while (!shown) {
                        scenario.onActivity { activity ->
                            activity.showOccurrences(NearbyOccurrences(null, emptyList()))
                            shown = activity.findViewById<android.widget.TextView>(R.id.occurrence_count_text)
                                .text.toString() == "2 nesta região"
                        }
                        if (!shown) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withId(R.id.chip_lost)).perform(scrollTo(), click())
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("1 nesta região")))
                onView(withId(R.id.chip_found)).perform(scrollTo(), click())
                onView(withId(R.id.occurrence_count_text)).check(matches(withText("1 nesta região")))
                scenario.onActivity { LostAndFoundRepository.endAd(context, published.first { it.category == "ENCONTRADO" }.id) }
                withTimeout(5000) {
                    var removed = false
                    while (!removed) {
                        scenario.onActivity { activity ->
                            removed = activity.findViewById<android.widget.TextView>(R.id.occurrence_count_text)
                                .text.toString() == "0 nesta região"
                        }
                        if (!removed) kotlinx.coroutines.delay(50)
                    }
                }
                onView(withText("Nenhum registro encontrado nesta região.")).check(matches(isDisplayed()))
                assertTrue(LostAndFoundRepository.getAdById(context, published[1].id)!!.isEnded)
            }
            assertEquals(44, JSONArray(prefs.getString("saved_ads", "[]")).length())
            val selfRequest = TutorVerificationRequest(adId = found.id, requesterUserId = "test",
                ownerUserId = "test", petName = "private", specialMark = "private")
            assertFalse(LostAndFoundRepository.saveVerificationRequest(context, selfRequest))
            val guestRequest = selfRequest.copy(id = "existing-real-id", requesterUserId = "another-user")
            assertTrue(LostAndFoundRepository.saveVerificationRequest(context, guestRequest))
            assertFalse(LostAndFoundRepository.saveVerificationRequest(context, guestRequest.copy(id = "duplicate")))
            LostAndFoundRepository.updateRequestStatus(context, guestRequest.id, "APROVADO")
            assertEquals(guestRequest.id, LostAndFoundRepository.getVerificationRequestsForAd(context, found.id).single().id)
            instrumentation.runOnMainSync {
                LostAndFoundRepository.endAd(context, lost.id)
                LostAndFoundRepository.updateAdSituation(context, found.id, "Reencontrado")
            }
            withTimeout(5000) {
                while (events.receive().any { (it.id == lost.id || it.id == found.id) && !it.isEnded }) { }
            }
            assertTrue(LostAndFoundRepository.getAllAds(context).first { it.id == found.id }.isReunited)
            assertEquals(44, JSONArray(prefs.getString("saved_ads", "[]")).length())
        } finally {
            observer.cancel()
            events.close()
            prefs.edit().clear().commit()
        }
    }
}
