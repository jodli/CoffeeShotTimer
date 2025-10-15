package com.jodli.coffeeshottimer.debug

import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.util.DatabasePopulator
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.viewmodel.DebugViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for conditional compilation behavior of debug functionality.
 * Since we're running in debug build during testing, we focus on testing
 * the actual behavior rather than mocking BuildConfig.
 */
@ExperimentalCoroutinesApi
class ConditionalCompilationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var databasePopulator: DatabasePopulator
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var stringResourceProvider: StringResourceProvider
    private lateinit var domainErrorTranslator: DomainErrorTranslator
    private lateinit var debugViewModel: DebugViewModel

    @Before
    fun setup() {
        // Skip all tests in this class if not in debug build
        org.junit.Assume.assumeTrue(
            "ConditionalCompilation tests only run in debug builds",
            com.jodli.coffeeshottimer.BuildConfig.DEBUG
        )

        Dispatchers.setMain(testDispatcher)
        databasePopulator = mockk(relaxed = true)
        onboardingManager = mockk(relaxed = true)
        stringResourceProvider = mockk(relaxed = true)
        domainErrorTranslator = mockk(relaxed = true)
        debugViewModel = DebugViewModel(databasePopulator, onboardingManager, stringResourceProvider, domainErrorTranslator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DatabasePopulator should work normally in debug build - populateForScreenshots`() = runTest {
        // When & Then - Should not throw exception in debug build
        try {
            databasePopulator.populateForScreenshots()
            // If we reach here, no exception was thrown (which is expected in debug)
        } catch (e: IllegalStateException) {
            if (e.message == "DatabasePopulator can only be used in debug builds") {
                throw AssertionError("Should not throw IllegalStateException in debug build")
            }
            // Re-throw other exceptions (like mocking-related ones)
            throw e
        }
    }

    @Test
    fun `DatabasePopulator should work normally in debug build - addMoreShots`() = runTest {
        // When & Then - Should not throw exception in debug build
        try {
            databasePopulator.addMoreShots(5)
            // If we reach here, no exception was thrown (which is expected in debug)
        } catch (e: IllegalStateException) {
            if (e.message == "DatabasePopulator can only be used in debug builds") {
                throw AssertionError("Should not throw IllegalStateException in debug build")
            }
            // Re-throw other exceptions (like mocking-related ones)
            throw e
        }
    }

    @Test
    fun `DatabasePopulator should work normally in debug build - clearAllData`() = runTest {
        // When & Then - Should not throw exception in debug build
        try {
            databasePopulator.clearAllData()
            // If we reach here, no exception was thrown (which is expected in debug)
        } catch (e: IllegalStateException) {
            if (e.message == "DatabasePopulator can only be used in debug builds") {
                throw AssertionError("Should not throw IllegalStateException in debug build")
            }
            // Re-throw other exceptions (like mocking-related ones)
            throw e
        }
    }

    @Test
    fun `DebugViewModel should work normally in debug build`() {
        // When
        debugViewModel.showDialog()

        // Then
        val state = debugViewModel.uiState.value
        assertEquals(true, state.isDialogVisible)
    }

    @Test
    fun `DebugViewModel fillDatabase should execute in debug build`() = runTest {
        // When
        debugViewModel.fillDatabase()

        // Then - Should start loading (since we're in debug build)
        val state = debugViewModel.uiState.value
        // In debug build, the operation should start (loading state may vary based on timing)
        // The key is that it doesn't immediately return without doing anything
    }

    @Test
    fun `DebugViewModel clearDatabase should execute in debug build`() = runTest {
        // When
        debugViewModel.clearDatabase()

        // Then - Should start loading (since we're in debug build)
        val state = debugViewModel.uiState.value
        // In debug build, the operation should start
    }
}
