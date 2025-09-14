package com.hamzatariq.i210396

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(HomePage::class.java)

    @Before
    fun setUp() {
        Intents.init()
        // Prevent actually launching target activities; we just assert the intent.
        val stubResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(anyIntent()).respondWith(stubResult)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun clickingExplore_opensExplore() {
        // NOTE: ID is 'exlplore' in your Activity; keep as-is unless you rename in XML.
        onView(withId(R.id.exlplore)).perform(click())
        intended(hasComponent(Explore::class.java.name))
    }

    @Test
    fun clickingShare_opensMessages() {
        onView(withId(R.id.share)).perform(click())
        intended(hasComponent(Messages::class.java.name))
    }

    @Test
    fun clickingLike_opensLikeFollowing() {
        onView(withId(R.id.like)).perform(click())
        intended(hasComponent(likeFollowing::class.java.name))
    }

    @Test
    fun clickingProfile_opensProfileScreen() {
        onView(withId(R.id.profile)).perform(click())
        intended(hasComponent(profileScreen::class.java.name))
    }

    @Test
    fun clickingPost_opensAddPostScreen() {
        onView(withId(R.id.post)).perform(click())
        intended(hasComponent(AddPostScreen::class.java.name))
    }

    @Test
    fun clickingCamera_opensStory() {
        onView(withId(R.id.camera)).perform(click())
        intended(hasComponent(Story::class.java.name))
    }

    @Test
    fun clickingStoryImage_opensStory() {
        onView(withId(R.id.story_image)).perform(click())
        intended(hasComponent(Story::class.java.name))
    }
}
