package com.jodli.coffeeshottimer.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class to verify Hilt modules are configured correctly.
 */
@RunWith(RobolectricTestRunner::class)
class HiltModuleTest {

    @Test
    fun `database module should provide database instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseModule = DatabaseModule

        // Test that the database module can provide a database instance
        val database = databaseModule.provideAppDatabase(context)

        assertNotNull(database)
        assertNotNull(database.beanDao())
        assertNotNull(database.shotDao())

        database.close()
    }
}
