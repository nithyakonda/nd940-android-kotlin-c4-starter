package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database:RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderGetReminderById_successWhenUniqueReminder() = runBlockingTest{
        // Given a reminder with unique id to save
        val reminder = ReminderDTO("title", "description", "location", -10.0, 10.00)

        // When saving the reminder
        database.reminderDao().saveReminder(reminder)
        val result = database.reminderDao().getReminderById(reminder.id)

        // Then reminder is saved successfully
        assertThat(result as ReminderDTO, notNullValue())
        assertThat(result.id, `is`(reminder.id))
        assertThat(result.title, `is`(reminder.title))
        assertThat(result.description, `is`(reminder.description))
        assertThat(result.location, `is`(reminder.location))
        assertThat(result.latitude, `is`(reminder.latitude))
        assertThat(result.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminders_returnsReminders() = runBlockingTest{
        // Given a db which contains some reminders
        val reminders = listOf<ReminderDTO>(ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00),
            ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00),
            ReminderDTO("Title3", "Description3", "Location4", -13.00, 13.00))
        for (reminder in reminders) {
            database.reminderDao().saveReminder(reminder)
        }

        // When retrieving all reminders
        val result = database.reminderDao().getReminders()

        // Then reminder is saved successfully
        assertThat(result, notNullValue())
        assertThat(result.size, `is`(3))
    }

    @Test
    fun deleteAllReminders_clearsDatabase() = runBlockingTest(){
        // Given a db which contains some reminders
        val reminders = listOf<ReminderDTO>(ReminderDTO("Title1", "Description1", "Location1", -11.00, 11.00),
            ReminderDTO("Title2", "Description3", "Location4", -12.00, 12.00),
            ReminderDTO("Title3", "Description3", "Location4", -13.00, 13.00))
        for (reminder in reminders) {
            database.reminderDao().saveReminder(reminder)
        }

        // When deleting all results
        database.reminderDao().deleteAllReminders()

        // Then has 0 records when trying to fetch them
        val result = database.reminderDao().getReminders()
        assertThat(result, notNullValue())
        assertThat(result.size, `is`(0))
    }
}