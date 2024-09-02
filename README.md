# Password Manager <img src="https://wakatime.com/badge/github/Achille004/PasswordManager.svg?style=flat" alt="wakatime"> <img src="https://img.shields.io/badge/version-3.0.2-green" alt="version"> <img alt="license" src="https://img.shields.io/github/license/Achille004/PasswordManager">

Password Manager is a secure and efficient application designed to manage your locally stored accounts. Each account entry includes software details, username, and a password encrypted using [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard) ([GCM](https://en.wikipedia.org/wiki/Galois/Counter_Mode) mode). The application itself is protected by a master password that the user must remember.

## Features

- **Add, Modify, Delete Accounts**: Manage your account information seamlessly.
- **AES-GCM Encryption**: Ensure your passwords are securely encrypted.
- **Multi-Language Support**: Currently available in English and Italian, other languages will be added shortly.

## Installation

### Windows

**Installers (exe and msi) are available:**

- [Releases Page](https://github.com/Achille004/PasswordManager/releases)
- [Installers Folder](https://github.com/Achille004/PasswordManager/tree/main/compiled/installer) (These might be newer but unstable or bugged).

### Portable Versions

The portable versions are available [here](https://github.com/Achille004/PasswordManager/tree/main/compiled/portable) and require a Java Runtime Environment (JRE) to run as they do not come bundled with one.

## Data Storage

- **Windows**: `$USER_HOME/AppData/Local/Password Manager`
- **Other Operating Systems**: `$USER_HOME/.password-manager`

## Exporting Data

The software previously supported exporting to `passwords.html` and `passwords.csv` files, which were saved on the desktop. However, this feature is currently being rewritten and is not available in version 3.0.2. Please note that these files are not encrypted and are thus extremely vulnerable.

## Contribution

All contributions to the project are welcome: if you have suggestions, bug reports, or want to contribute code, feel free to open an issue or submit a pull request on this repository.

## Contact

For any inquiries or support, contact me at 2004marras@gmail.com.