package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import androidx.test.espresso.assertion.ViewAssertions.matches

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: KoinTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    val repository: ReminderDataSource by inject()
    @Before
    fun initRepository() {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { FakeDataSource() as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }

        }
        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }
    }

    @After
    fun stopKoinAfterTest() {
        (repository as FakeDataSource).setReturnError(true)
        stopKoin()
    }

    @Test
    fun clickAdd_navigateToSaveReminderFragment() = runBlockingTest {
        // Given on the reminder list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // When click on the add reminder fab
        onView(withId(R.id.addReminderFAB))
            .perform(click())

        // Then verify that we navigate to SaveReminderFragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun displaysListOfReminders() = runBlockingTest {
        // Given database contains some reminders
        repository.saveReminder(ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00))
        repository.saveReminder(ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00))

        // When on the home screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // Then Displays reminders in recycler view
        onView(withText("Title1")).check(matches(isDisplayed()))
        onView(withText("Description1")).check(matches(isDisplayed()))
        onView(withText("Location1")).check(matches(isDisplayed()))


        onView(withText("Title2")).check(matches(isDisplayed()))
        onView(withText("Description3")).check(matches(isDisplayed()))
        onView(withText("Location4")).check(matches(isDisplayed()))
    }

    @Test
    fun displaysNoData() {
        // Given empty database

        // When on home screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // Then no data icon is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun showErrorSnackBarWhenDbAccessFails() {
        // Given database that returns error
        (repository as FakeDataSource).setReturnError(true)

        // When on home screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // Then error snack bar is displayed
        onView(withId(R.id.snackbar_text)).check(matches(withText("Unknown error occurred")))
    }
}