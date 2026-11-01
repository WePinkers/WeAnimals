package com.example.weanimals

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.user.lostandfound.presentation.*
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PublicAnnouncementFlowTest {
    @Test fun authorCanTestBothPublicFormsApprovalAndReuseTheSameConversation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        // Keep all test announcements and requests out of the user's storage.
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("public_flow_test_" + name, mode)
        }
        context.getSharedPreferences("lost_and_found_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        LostAndFoundRepository.init(context)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
        assertTrue(LostAndFoundRepository.canTestOwnAnnouncement(context))

        for (category in listOf("ENCONTRADO", "PERDIDO")) {
            val ad = FoundPetAd(ownerUserId = uid, category = category, species = "Gato",
                neighborhood = "Vila Maria", city = "São Paulo")
            LostAndFoundRepository.saveAd(context, ad)
            val forbiddenByDefault = TutorVerificationRequest(adId = ad.id, requesterUserId = uid,
                ownerUserId = uid, petName = "Teste", specialMark = "Mancha de teste")
            assertFalse(LostAndFoundRepository.saveVerificationRequest(context, forbiddenByDefault))

            ActivityScenario.launch<PublicAdDetailActivity>(
                Intent(base, PublicAdDetailActivity::class.java).putExtra(PublicAdDetailActivity.EXTRA_AD_ID, ad.id)
            ).use { scenario ->
                val action = if (category == "PERDIDO") "Acho que encontrei" else "Acho que é meu"
                onView(withId(R.id.btn_main_action_public)).check(matches(withText(action)))
                    .check(matches(isEnabled())).perform(click())
                if (category == "ENCONTRADO") {
                    onView(withId(R.id.et_pet_name)).perform(scrollTo(), replaceText("Teste"), closeSoftKeyboard())
                    onView(withId(R.id.et_special_mark)).perform(scrollTo(), replaceText("Mancha de teste"), closeSoftKeyboard())
                    onView(withId(R.id.btn_submit_verification)).perform(click())
                } else {
                    onView(withId(R.id.chip_situation_sighted)).perform(scrollTo(), click())
                    onView(withId(R.id.chip_sighting_now)).perform(scrollTo(), click())
                    onView(withId(R.id.btn_sighting_continue_step1)).perform(click())
                    onView(withId(R.id.et_sighting_characteristics)).perform(scrollTo(), replaceText("Mancha de teste"), closeSoftKeyboard())
                    onView(withId(R.id.btn_sighting_submit)).perform(click())
                }
                onView(withId(R.id.tv_submitted_status_badge)).check(matches(withText("Em análise")))
                val request = LostAndFoundRepository.getVerificationRequestsForAd(context, ad.id).single()
                assertEquals(uid, request.requesterUserId)
                assertEquals("PENDENTE", request.status)
                assertFalse(LostAndFoundRepository.saveVerificationRequest(context,
                    request.copy(id = "duplicate"), allowOwnAdForTesting = true))
                onView(withId(R.id.btn_review_test_request)).perform(click())
                if (category == "ENCONTRADO") {
                    onView(withId(R.id.btn_approve_request)).perform(click())
                } else {
                    onView(withId(R.id.btn_approve_sighting)).perform(click())
                    onView(withText("Pode ser, abrir chat")).perform(click())
                }
                onView(withId(R.id.message_input)).check(matches(isDisplayed()))
                onView(withId(R.id.message_input)).perform(replaceText("Mensagem de teste"), closeSoftKeyboard())
                onView(withId(R.id.send_button)).perform(click())
                assertEquals("APROVADO", LostAndFoundRepository.getVerificationRequestById(context, request.id)!!.status)
                val messages = LostAndFoundRepository.getMessagesForRequest(context, request.id)
                assertEquals(1, messages.size)
                pressBack()
                onView(withId(R.id.btn_main_action_public)).check(matches(withText("Abrir conversa"))).perform(click())
                onView(withId(R.id.message_input)).check(matches(isDisplayed()))
                assertEquals(listOf(request.id), LostAndFoundRepository.getVerificationRequestsForAd(context, ad.id).map { it.id })
                assertEquals(messages.map { it.id }, LostAndFoundRepository.getMessagesForRequest(context, request.id).map { it.id })
                pressBack()
                scenario.onActivity {
                    assertEquals(AdActionNavigator.Action.CHAT, AdActionNavigator.action(it, ad))
                }
            }
        }
    }
}
