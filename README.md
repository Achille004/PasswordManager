# Password Manager <img src="https://wakatime.com/badge/github/Achille004/PasswordManager.svg?style=flat" alt="wakatime"> <img src="https://img.shields.io/badge/version-3.1.0-green" alt="version"> <img alt="license" src="https://img.shields.io/github/license/Achille004/PasswordManager">

Password Manager is a secure and efficient application designed to manage locally stored accounts.  
Each account entry includes software details, username, and a password encrypted using [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard) ([GCM](https://en.wikipedia.org/wiki/Galois/Counter_Mode) mode). The application itself is protected by a master password that the user must remember.

## Features

- **Add, Modify, Delete Accounts**: Manage your account information seamlessly.
- **AES-GCM Encryption**: Ensure your passwords are securely encrypted.
- **Multi-Language Support**: Currently available in English and Italian, other languages will be added shortly.
- **Simple and light UI**: A dark-themed UI designed for as little clicks as possible, with the help of shortcuts.

## Installation

[Available Releases](https://github.com/Achille004/PasswordManager/releases)

Installer versions are completely standalone, as they come with a bundled runtime environment.
On the other hand, portable versions require a Java Runtime Environment (JRE).

## Data Storage

- **Windows**: `$USER_HOME/AppData/Local/Password Manager`
- **Other Operating Systems**: `$USER_HOME/.password-manager`

## Exporting Data

The software previously supported exporting data to plain files, but this feature has been removed due to security concerns.
A Google Drive integration is planned to replace this functionality, enabling users to securely transfer and manage their data. All synchronization operations will be performed on encrypted data, ensuring that plain data remains exclusively within the application.

## Contribution

All contributions to the project are welcome: if you have suggestions, bug reports, or want to contribute code, feel free to open an issue or submit a pull request on this repository.

## Contact

For any inquiries or support, contact me at 2004marras@gmail.com.