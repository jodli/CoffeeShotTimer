package com.jodli.coffeeshottimer.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertNotNull

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
    
    @Test
    fun `repository module should provide repository instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseModule = DatabaseModule
        val repositoryModule = RepositoryModule
        
        // Create database and DAOs
        val database = databaseModule.provideAppDatabase(context)
        val beanDao = databaseModule.provideBeanDao(database)
        val shotDao = databaseModule.provideShotDao(database)
        
        // Test that the repository module can provide repository instances
        val beanRepository = repositoryModule.provideBeanRepository(beanDao)
        val shotRepository = repositoryModule.provideShotRepository(shotDao, beanDao)
        
        assertNotNull(beanRepository)
        assertNotNull(shotRepository)
        
        database.close()
    }
}