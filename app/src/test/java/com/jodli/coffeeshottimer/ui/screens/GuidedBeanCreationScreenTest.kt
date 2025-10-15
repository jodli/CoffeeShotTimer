package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.data.onboarding.GuidedBeanCreationUiState
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.viewmodel.GuidedBeanCreationViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GuidedBeanCreationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun educationPhase_displaysEducationalContent() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify educational content is displayed
        composeTestRule.onNodeWithText("Why Track Coffee Beans?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Freshness Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn From Every Shot").assertIsDisplayed()
        composeTestRule.onNodeWithText("Visual Memory").assertIsDisplayed()

        // Verify action buttons are displayed
        composeTestRule.onNodeWithText("Add My First Bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for Now").assertIsDisplayed()
    }

    @Test
    fun educationPhase_continueButtonCallsViewModel() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Add My First Bean").performClick()

        verify { mockViewModel.proceedToForm() }
    }

    @Test
    fun educationPhase_skipButtonCallsCallback() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        var skipCalled = false
        val onSkip = { skipCalled = true }

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = onSkip,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Skip for Now").performClick()

        assert(skipCalled)
    }

    @Test
    fun educationPhase_displaysCoffeeBeanIcon() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify coffee bean icon is displayed
        composeTestRule.onNodeWithContentDescription("Coffee bean freshness illustration").assertIsDisplayed()
    }

    @Test
    fun educationPhase_displaysEducationCards() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify all education card descriptions are present
        composeTestRule.onNodeWithText(
            "Coffee beans are best 4-21 days after roasting. We'll help you track roast dates for optimal flavor."
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            "Connect your shots to specific beans to understand which coffee works best with your setup."
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            "Add photos to remember bag designs and identify your favorites at a glance."
        ).assertIsDisplayed()
    }

    @Test
    fun successPhase_displaysCreatedBeanInformation() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val testBean = Bean(
            id = "test-id",
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Floral and bright",
            isActive = true
        )
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.SUCCESS,
            createdBean = testBean
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        every {
            mockViewModel.getFreshnessMessage(testBean)
        } returns "Perfect timing – these beans are in their optimal freshness window!"

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify success content is displayed
        composeTestRule.onNodeWithText("Great! Your First Bean is Ready").assertIsDisplayed()
        composeTestRule.onNode(hasText("Ethiopian Yirgacheffe", substring = true)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue to First Shot").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Perfect timing – these beans are in their optimal freshness window!"
        ).assertIsDisplayed()
    }

    @Test
    fun successPhase_continueButtonCallsCallback() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val testBean = Bean(
            id = "test-id",
            name = "Test Bean",
            roastDate = LocalDate.now(),
            isActive = true
        )
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.SUCCESS,
            createdBean = testBean
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        every { mockViewModel.getFreshnessMessage(testBean) } returns "Test message"

        var completeCalled = false
        var completedBean: Bean? = null
        val onComplete: (Bean) -> Unit = { bean ->
            completeCalled = true
            completedBean = bean
        }

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = onComplete,
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Continue to First Shot").performClick()

        assert(completeCalled)
        assert(completedBean == testBean)
    }

    @Test
    fun successPhase_displaysBeanSummaryCard() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val testBean = Bean(
            id = "test-id",
            name = "Colombian Single Origin",
            roastDate = LocalDate.now().minusDays(5),
            notes = "Chocolatey notes",
            isActive = true
        )
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.SUCCESS,
            createdBean = testBean
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        every { mockViewModel.getFreshnessMessage(testBean) } returns "Good freshness"

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify bean summary information is displayed
        composeTestRule.onNodeWithText("Colombian Single Origin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roasted 5 days ago").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chocolatey notes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good freshness").assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysLoadingIndicator() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION,
            isLoading = true
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify loading indicator and message
        composeTestRule.onNodeWithText("Preparing...").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION,
            error = "Something went wrong"
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify error is displayed
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun buttonsDisabledDuringLoading() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.EDUCATION,
            isLoading = true
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Buttons should be present but clicking them shouldn't call the ViewModel
        composeTestRule.onNodeWithText("Add My First Bean").performClick()
        composeTestRule.onNodeWithText("Skip for Now").performClick()

        // Should not call ViewModel methods when loading
        verify(exactly = 0) { mockViewModel.proceedToForm() }
    }

    @Test
    fun formPhase_displaysAddEditBeanScreen() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.FORM
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // The form phase delegates to AddEditBeanScreen
        // We can verify by checking for common form elements
        composeTestRule.onNodeWithText("Bean name or description").assertIsDisplayed()
    }

    @Test
    fun successPhase_showsTodayRoastMessage() {
        val mockViewModel = mockk<GuidedBeanCreationViewModel>(relaxed = true)
        val todayBean = Bean(
            id = "test-id",
            name = "Today Bean",
            roastDate = LocalDate.now(), // Today
            isActive = true
        )
        val uiState = GuidedBeanCreationUiState(
            currentPhase = BeanCreationPhase.SUCCESS,
            createdBean = todayBean
        )
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        every { mockViewModel.getFreshnessMessage(todayBean) } returns "Your bean is ready for brewing!"

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GuidedBeanCreationScreen(
                    onComplete = {},
                    onSkip = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify today roast date is displayed correctly
        composeTestRule.onNodeWithText("Roasted today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your bean is ready for brewing!").assertIsDisplayed()
    }
}
