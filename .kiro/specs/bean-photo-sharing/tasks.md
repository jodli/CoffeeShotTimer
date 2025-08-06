# Implementation Plan

- [x] 1. Set up database foundation for photo storage
  - Create database migration to add photoPath field to Bean entity
  - Update Bean data class with photoPath field and validation
  - Add photo-related methods to BeanDao interface
  - Write unit tests for Bean entity validation with photo field
  - _Requirements: 1.4, 2.3, 6.3, 7.3_

- [x] 2. Implement photo storage management system
  - Create PhotoStorageManager interface and implementation
  - Implement image compression and file storage operations
  - Add photo file cleanup and storage management utilities
  - Write unit tests for PhotoStorageManager operations
  - _Requirements: 1.4, 6.1, 6.2, 7.3_

- [x] 3. Create photo management use cases
  - Implement AddPhotoToBeanUseCase for adding photos to beans
  - Implement RemovePhotoFromBeanUseCase for deleting bean photos
  - Implement GetBeanPhotoUseCase for retrieving photo files
  - Write unit tests for all photo use cases
  - Return DomainExceptions with DomainErrorCodes for proper translation
  - _Requirements: 2.3, 4.3, 4.5, 6.1_

- [x] 4. Update repository layer for photo operations
  - Add photo-related methods to BeanRepository interface
  - Implement photo operations in BeanRepositoryImpl
  - Update repository to handle photo file cleanup when beans are deleted
  - Write unit tests for repository photo operations
  - _Requirements: 1.4, 2.3, 4.5, 6.3_

- [ ] 5. Implement camera integration and permission handling
  - Create PhotoCaptureManager for camera and gallery operations
  - Implement camera permission request and handling logic
  - Add image capture intent creation and result handling
  - Write unit tests for camera integration components
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 6. Create photo UI components
  - Implement BeanPhotoSection composable for photo display and actions
  - Create PhotoActionSheet for camera/gallery selection
  - Implement PhotoViewer for full-screen photo viewing
  - Add proper loading states and error handling to photo components
  - _Requirements: 3.1, 3.3, 8.2, 8.4_

- [ ] 7. Update AddEditBeanViewModel for photo operations
  - Add photo-related state properties to ViewModel
  - Implement photo capture, replacement, and deletion methods
  - Add proper error handling and loading states for photo operations
  - Write unit tests for ViewModel photo functionality
  - _Requirements: 1.1, 2.1, 4.1, 8.5_

- [ ] 8. Integrate photo section into AddEditBeanScreen
  - Add BeanPhotoSection to the bean editing UI layout
  - Implement photo action handling (capture, replace, delete)
  - Add proper spacing and styling consistent with app design system
  - Ensure photo section works in both create and edit modes
  - _Requirements: 1.1, 2.1, 8.1, 8.3_

- [ ] 9. Implement photo viewing and management
  - Add photo display functionality to bean details view
  - Implement full-screen photo viewer with zoom capability
  - Add photo replacement and deletion confirmation dialogs
  - Ensure proper image loading and error state handling
  - _Requirements: 3.1, 3.3, 4.1, 4.2_

- [ ] 10. Add comprehensive error handling and user feedback
  - Implement graceful handling of camera unavailable scenarios
  - Add proper error messages for storage and permission issues
  - Implement retry mechanisms for failed photo operations
  - Add success feedback for photo operations
  - _Requirements: 5.2, 5.4, 6.5, 8.5_

- [ ] 11. Write integration tests for photo workflow
  - Create end-to-end tests for photo capture and storage workflow
  - Test database migration and photo field integration
  - Add tests for camera permission scenarios and error handling
  - Test photo cleanup when beans are deleted
  - _Requirements: 1.4, 4.5, 5.1, 6.3_

- [ ] 12. Optimize performance and add final polish
  - Implement efficient image loading with proper memory management
  - Add image compression optimization for storage efficiency
  - Ensure photo operations don't block UI thread
  - Add accessibility support for photo components
  - _Requirements: 3.4, 6.1, 6.2, 8.4_