# IMPORTANT NOTES

### Windows users
The Windows installers are signed with an open source self-certificate, so Windows might do... you know, _Windows shenanigans_ and complain about the app's trustworthiness. Of course, it's up to you whether to trust them or build them yourself. (Can't really afford a code signing certificate, sorry!)

### Other operating systems
Since the app works with any OS, but the installers are only for Windows and Linux, I included the portable versions (which do not come with a bundled java runtime), with their respective signature.

### PGP signatures and checksums
The `.asc` files contain [OpenPGP](https://en.wikipedia.org/wiki/Pretty_Good_Privacy#OpenPGP) signatures. If desired, these can be verified with any tool that can read them, I suggest [Kleopatra](https://www.openpgp.org/software/kleopatra).
You can easily find my public key (using the mail address listed below) on [keys.openpgp.org](https://keys.openpgp.org/), by running `gpg --auto-key-locate keyserver --locate-keys user@example.com` or simply uploaded as a release file.

### Project contribution
If you find any bugs, want to suggest some features or just help out, feel free to open an issue or email me at 2004marras@gmail.com.

# 3.1 PATCH HISTORY

## Patch 3.1.2 (March 16, 2026)

This update focuses on security and reliability first, while also polishing a few UX details that were still rough around the edges. The encryption flow has been redesigned to support safer key handling and future-proof account management, and several UI areas were improved to behave more consistently across different screen sizes.
Please note that the new internal encryption technique could be described as *"foreshadowing"*.

### Features

- Improved overall account protection with a new internal encryption workflow.
- Enhanced general handling of user actions and all under-the-hood stuff.
- Made all the application resizable, beside EULA.
- Improved save popup behavior and visibility.
- Added country flags to the language selector for clearer localization settings.
- Included various stability and compatibility improvements through dependency and configuration updates.

### Code

- Migrated account encryption to a DEK-based model, with KEK support and HKDF/Argon2-driven key derivation flow.
- Implemented full-account encryption refactoring across `Account`, `AES`, `UserPreferences`, and related security classes.
- Refactored `AccountRepository` synchronization logic to avoid inconsistent internal states.
- Improved locale persistence by adding a serialization-transparent `SupportedLocale` enum.
- Refactored singleton infrastructure again, consolidating behavior into an abstract singleton base and improving lifecycle handling.
- Introduced and integrated the new `CustomPopup` component in `lib`, replacing previous popup content handling.
- Refined controller and FXML structure (including `AbstractController`, `TabManager`, and package/layout moves) for cleaner separation.
- Improved `Logger` formatting support and updated `Transaction` naming/identifier handling.
- Enhanced memory reservation logic to account for internal overhead.
- Expanded and revised tests (including new `TestUserPreferences`) to cover security, singleton lifecycle, repository behavior, and transactions more thoroughly.
- Updated dependencies/modules and related Gradle configuration for better cross-module compatibility.

## Patch 3.1.1 (December 12, 2025)

A lot of under-the-hood changes have been made to the application for better account management while enhancing code organization, maintainability and allowing for unit testing. This also brings smaller UI additions and fixes.

### Features

- Added playful prompts for empty fields
- Fixed password suggestions in packaged version
- Improved loading animations and save popup visibility
- Various bug fixes and stability improvements

### Code

- Merged AbstractViewController into AbstractController
- Introduced animation-dedicated classes while refactoring their handling
- Implemented a unified singleton pattern
- Refactored IOManager to use AccountRepository, with a CRUD-like implementation
- Added TransactionManager for transactional operations
- Enhanced SecurityVersion with memory reservation
- Improved error handling and logging throughout
- Added unit tests for AES, Account, AccountRepository, transactions, and singleton lifecycle
- Bumped Gradle and dependencies to latest versions

## Patch 3.1.0 (October 17, 2025)
The entire UI has been redesigned and rewritten to provide a more comfortable and intuitive user experience while maintaining a modern look and feel.
The encrypter and decrypter have been unified into a single page - the Manager - which now features a list-based layout instead of the bulky combo box previously used for account selection. This new design introduces a tab-oriented interface, allowing users to have multiple editors open simultaneously.

Also, the following shortcuts have been added:
- Ctrl + Q/E: Scroll tabs left/right
- Ctrl + W: Close current tab
- Ctrl + T: Go to add tab

This adds up to the pre-existing Return shortcut (helps to navigate trough text fields).

### Features
- Implemented search functionality in ManagerController.
- Added auto-completion feature and multiple keyboard shortcuts.
- Implemented data backup functionality with a streamlined process.
- Implemented autosave popup for better user feedback.
- Added effects and improved UI/UX consistency
- Added localization updates to improve clarity and consistency.

### Code
- Bumped Java and JavaFX version to 25 and Gradle to version 9.1, improving Gradle configuration and JVM arguments.
- Added PasswordInputControl interface for consistent password field behavior.
- Refactored application structure:
  - Consolidated IOManager and ObservableResourceFactory into singletons.
  - Later improved singleton logic using a class-mapped approach.
- Merged AppManager into App for a cleaner architecture.
- Refactored AppManager, ManagerController, UserPreferences, and IOManager for readability, better property handling, and maintainability.
- Enhanced logging across FXML loading and controllers for consistency and clarity.
- Introduced TabManager delegate for encapsulation and better controller separation.
- Refactored security handling:
  - Moved key derivation functions into SecurityVersion, to emphasize their strict bound
  - Updated password encryption/decryption logic.
- Refactored code to use final modifiers for local variables to improve clarity and performance.
- Enhanced asynchronous account list handling using CompletableFuture.
- Cleaned up whitespace across files and streamlined password handling (e.g., stripping input).
