package com.example.coffeeshottimer.domain.usecase

import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.ValidationResult
import com.example.coffeeshottimer.data.repository.BeanRepository
import com.example.coffeeshottimer.data.repository.RepositoryException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for AddBeanUseCase.
 * Tests bean creation, validation, and error handling scenarios.
 */
class AddBeanUseCaseTest {
    
    private lateinit var beanRepository: BeanRepository
    private lateinit var addBeanUseCase: AddBeanUseCase
    
    @Before
    fun setup() {
        beanRepository = mockk()
        addBeanUseCase = AddBeanUseCase(beanRepository)
    }
    
    @Test
    fun `execute should create bean successfully with valid parameters`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        val notes = "Floral and citrusy"
        val isActive = true
        val grinderSetting = "15"
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)
        
        // When
        val result = addBeanUseCase.execute(name, roastDate, notes, isActive, grinderSetting)
        
        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertNotNull(createdBean)
        assertEquals(name, createdBean?.name)
        assertEquals(roastDate, createdBean?.roastDate)
        assertEquals(notes, createdBean?.notes)
        assertEquals(isActive, createdBean?.isActive)
        assertEquals(grinderSetting, createdBean?.lastGrinderSetting)
        
        coVerify { beanRepository.validateBean(any()) }
        coVerify { beanRepository.addBean(any()) }
    }
    
    @Test
    fun `execute should trim whitespace from inputs`() = runTest {
        // Given
        val name = "  Ethiopian Yirgacheffe  "
        val notes = "  Floral and citrusy  "
        val grinderSetting = "  15  "
        val roastDate = LocalDate.now().minusDays(7)
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)
        
        // When
        val result = addBeanUseCase.execute(name, roastDate, notes, true, grinderSetting)
        
        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertEquals("Ethiopian Yirgacheffe", createdBean?.name)
        assertEquals("Floral and citrusy", createdBean?.notes)
        assertEquals("15", createdBean?.lastGrinderSetting)
    }
    
    @Test
    fun `execute should handle empty grinder setting`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        val grinderSetting = ""
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)
        
        // When
        val result = addBeanUseCase.execute(name, roastDate, lastGrinderSetting = grinderSetting)
        
        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertNull(createdBean?.lastGrinderSetting)
    }
    
    @Test
    fun `execute should fail when validation fails`() = runTest {
        // Given
        val name = ""
        val roastDate = LocalDate.now().minusDays(7)
        
        val validationResult = ValidationResult(
            isValid = false,
            errors = listOf("Bean name cannot be empty")
        )
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        
        // When
        val result = addBeanUseCase.execute(name, roastDate)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BeanUseCaseException.ValidationError)
        assertTrue(exception?.message?.contains("Bean name cannot be empty") == true)
        
        coVerify { beanRepository.validateBean(any()) }
        coVerify(exactly = 0) { beanRepository.addBean(any()) }
    }
    
    @Test
    fun `execute should fail when repository add fails`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.failure(
            RepositoryException.DatabaseError("Database error")
        )
        
        // When
        val result = addBeanUseCase.execute(name, roastDate)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RepositoryException.DatabaseError)
    }
    
    @Test
    fun `execute should handle repository exception`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        
        coEvery { beanRepository.validateBean(any()) } throws RuntimeException("Unexpected error")
        
        // When
        val result = addBeanUseCase.execute(name, roastDate)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BeanUseCaseException.UnknownError)
        assertTrue(exception?.message?.contains("Unexpected error adding bean") == true)
    }
    
    @Test
    fun `validateBeanParameters should return validation result`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        val notes = "Floral and citrusy"
        
        val expectedResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns expectedResult
        
        // When
        val result = addBeanUseCase.validateBeanParameters(name, roastDate, notes)
        
        // Then
        assertEquals(expectedResult, result)
        coVerify { beanRepository.validateBean(any()) }
    }
    
    @Test
    fun `validateBeanParameters should handle validation exception`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        
        coEvery { beanRepository.validateBean(any()) } throws RuntimeException("Validation error")
        
        // When
        val result = addBeanUseCase.validateBeanParameters(name, roastDate)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Failed to validate bean parameters") })
    }
    
    @Test
    fun `isBeanNameAvailable should return true when name is available`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        coEvery { beanRepository.getBeanByName(name) } returns Result.success(null)
        
        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `isBeanNameAvailable should return false when name is taken`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val existingBean = Bean(
            name = name,
            roastDate = LocalDate.now().minusDays(7)
        )
        coEvery { beanRepository.getBeanByName(name) } returns Result.success(existingBean)
        
        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }
    
    @Test
    fun `isBeanNameAvailable should fail with empty name`() = runTest {
        // Given
        val name = ""
        
        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BeanUseCaseException.ValidationError)
        assertTrue(exception?.message?.contains("Bean name cannot be empty") == true)
    }
    
    @Test
    fun `isBeanNameAvailable should handle repository error`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        coEvery { beanRepository.getBeanByName(name) } returns Result.failure(
            RepositoryException.DatabaseError("Database error")
        )
        
        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RepositoryException.DatabaseError)
    }
    
    @Test
    fun `createQuickBean should create bean with default values`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now()
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)
        
        // When
        val result = addBeanUseCase.createQuickBean(name, roastDate)
        
        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertNotNull(createdBean)
        assertEquals(name, createdBean?.name)
        assertEquals(roastDate, createdBean?.roastDate)
        assertEquals("", createdBean?.notes)
        assertTrue(createdBean?.isActive == true)
        assertNull(createdBean?.lastGrinderSetting)
    }
    
    @Test
    fun `createQuickBean should use today as default roast date`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val today = LocalDate.now()
        
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)
        
        // When
        val result = addBeanUseCase.createQuickBean(name)
        
        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertEquals(today, createdBean?.roastDate)
    }
    
    @Test
    fun `getValidationRules should return expected rules`() {
        // When
        val rules = addBeanUseCase.getValidationRules()
        
        // Then
        assertEquals(4, rules.size)
        assertTrue(rules.containsKey("name"))
        assertTrue(rules.containsKey("roastDate"))
        assertTrue(rules.containsKey("notes"))
        assertTrue(rules.containsKey("grinderSetting"))
        
        assertEquals("Required, unique, max 100 characters", rules["name"])
        assertEquals("Cannot be future date, max 365 days ago", rules["roastDate"])
        assertEquals("Optional, max 500 characters", rules["notes"])
        assertEquals("Optional, max 50 characters", rules["grinderSetting"])
    }
}