package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import com.udacity.project4.locationreminders.util.getOrAwaitValue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var repository: FakeDataSource
    private lateinit var context: Application

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        repository = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00)
        val reminder2 = ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location4", -13.00, 13.00)

        repository.addReminders(reminder1, reminder2, reminder3)

        // Given a fresh SaveReminderViewModel
        context = ApplicationProvider.getApplicationContext()
        viewModel = SaveReminderViewModel(
            context,
            repository)
    }

    @After
    fun teardown() {
        repository.setReturnError(false)
        stopKoin()
    }

    @Test
    fun validateAndSaveReminder_loading() {
        mainCoroutineRule.pauseDispatcher()

        // When reminder data has no validation errors
        val reminder = ReminderDataItem("Title4", "Description4", "Location4", -14.00, 14.00)
        viewModel.validateAndSaveReminder(reminder)

        // Then show loading live data is set to true before db access
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun validateAndSaveReminder_errorSavingWhenInvalidTitle() {
        // When reminder data has null title
        var reminder = ReminderDataItem(null, "Description4", "Location4", -14.00, 14.00)
        viewModel.validateAndSaveReminder(reminder)

        // Then snack bar with error message is triggered
        var result = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(result, `is`(R.string.err_enter_title))

        // When reminder data has empty title
        reminder = ReminderDataItem("null", "Description4", "Location4", -14.00, 14.00)
        viewModel.validateAndSaveReminder(reminder)

        // Then snack bar with error message is triggered
        result = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(result, `is`(R.string.err_enter_title))
    }

    @Test
    fun validateAndSaveReminder_errorSavingWhenInvalidLocation() {
        // When reminder data has null location
        var reminder = ReminderDataItem("Title", "Description4", null, -14.00, 14.00)
        viewModel.validateAndSaveReminder(reminder)

        // Then snack bar with error message is triggered
        var result = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(result, `is`(R.string.err_select_location))

        // When reminder data has empty title
        reminder = ReminderDataItem("Title", "Description4", "", -14.00, 14.00)
        viewModel.validateAndSaveReminder(reminder)

        // Then snack bar with error message is triggered
        result = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(result, `is`(R.string.err_select_location))
    }
}