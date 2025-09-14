package com.hamzatariq.i210396

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.widget.DatePicker
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterUserTest {

    @Before
    fun setup() {
        Intents.init()
        // Stub all external launches so tests don't actually navigate away
        val ok = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(anyIntent()).respondWith(ok)
    }

    @After
    fun teardown() {
        Intents.release()
    }

    @Test
    fun clickingBack_finishesActivity() {
        val scenario = ActivityScenario.launch(RegisterUser::class.java)

        onView(withId(R.id.ivBack)).perform(click())

        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun clickingDob_opensDatePicker_andSetsText() {
        ActivityScenario.launch(RegisterUser::class.java)

        // Open the picker
        onView(withId(R.id.etDob)).perform(click())

        // Set date = 15/1/2000 (day/month/year)
        onView(withClassName(equalTo(DatePicker::class.java.name)))
            .perform(PickerActions.setDate(2000, 1, 15))

        // Confirm (use platform button id to avoid locale issues)
        onView(withId(android.R.id.button1)).perform(click())

        // Verify EditText updated
        onView(withId(R.id.etDob)).check(matches(withText("15/1/2000")))
    }

    @Test
    fun clickingRegister_launchesHome_andFinishes() {
        val scenario = ActivityScenario.launch(RegisterUser::class.java)

        onView(withId(R.id.btnRegister)).perform(click())

        intended(hasComponent(HomePage::class.java.name))
        // Activity calls finish() after startActivity
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }
}
