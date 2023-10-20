package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import com.udacity.project4.locationreminders.util.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var repository: FakeDataSource

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

        // Given a fresh RemindersListViewModel
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            repository)
    }

    @After
    fun teardown() {
        repository.setReturnError(false)
        stopKoin()
    }

    @Test
    fun loadReminders_returnsRemindersList() {
        // When there are existing reminders
        viewModel.loadReminders()

        // Then remindersList live data is updated successfully
        val result = viewModel.remindersList.getOrAwaitValue()
        assertThat(result, not(nullValue()))
        assertThat(result.size, `is`(3))
    }

    @Test
    fun loadReminders_triggersErrorSnackBar() {
        // When there is an error fetching data
        repository.setReturnError(true)
        viewModel.loadReminders()

        // Then the showSnackBar live data is set to the error message
        val result = viewModel.showSnackBar.getOrAwaitValue()
        assertThat(result, `is`("Unknown error occurred"))
    }

    @Test
    fun loadReminders_returnsNoData() = runBlockingTest{
        // When the database is empty
        repository.deleteAllReminders()
        viewModel.loadReminders()

        // Then showNoData live data value is set to true
        val result = viewModel.showNoData.getOrAwaitValue()
        assertThat(result, `is`(true))
    }
}