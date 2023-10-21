package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java )
            .allowMainThreadQueries()
            .build()

        dataSource = RemindersLocalRepository(database.reminderDao(),
        Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderGetReminderById_successWhenUniqueReminder() = runBlocking{
        // Given a reminder with unique id to save
        val reminder = ReminderDTO("title", "description", "location", -10.0, 10.00)

        // When saving the reminder
        dataSource.saveReminder(reminder)
        val result = dataSource.getReminder(reminder.id)

        // Then reminder is saved successfully
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminder_returnsNotFoundError() = runBlocking{
        val reminder = ReminderDTO("title", "description", "location", -10.0, 10.00)

        // When saving the reminder
        dataSource.saveReminder(reminder)
        val result = dataSource.getReminder("12345")

        // Then reminder is saved successfully
        assertThat(result.succeeded, `is`(false))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun getReminders_returnsReminders() = runBlocking{
        // Given a db which contains some reminders
        val reminders = listOf<ReminderDTO>(ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00),
            ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00),
            ReminderDTO("Title3", "Description3", "Location4", -13.00, 13.00))
        for (reminder in reminders) {
            dataSource.saveReminder(reminder)
        }

        // When retrieving all reminders
        val result = dataSource.getReminders()

        // Then reminder is saved successfully
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.size, `is`(3))
    }

    @Test
    fun deleteAllReminders_clearsDatabase() = runBlocking{
        // Given a db which contains some reminders
        val reminders = listOf<ReminderDTO>(
            ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00),
            ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00),
            ReminderDTO("Title3", "Description3", "Location4", -13.00, 13.00))
        for (reminder in reminders) {
            dataSource.saveReminder(reminder)
        }

        // When deleting all results
        dataSource.deleteAllReminders()

        // Then has 0 records when trying to fetch them
        val result = dataSource.getReminders()
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.size, `is`(0))
    }

}