# PasswordManager
Password Manager is an application able to add, modify and delete locally saved accounts, characterized by a software, a username and an [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard "AES explanation.") ([GCM](https://en.wikipedia.org/wiki/Galois/Counter_Mode "GCM explanation.") mode) encrypted password.  
The program will be protected with a password that the user must remember.  
Two languages are available: English and Italian. If you want to help translating, contact me at my email.


**The JAR and Windows Installers (exe and msi) are available both [here](https://github.com/Achille004/PasswordManager/releases "Releases page.") and [here](https://github.com/Achille004/PasswordManager/tree/main/compiled "Compiled files folder.") (these might be newer but unstable or bugged).** 
The Windows Installers do not need any JRE as they come with a bundled one.


The data files will be saved under `$USER_HOME/AppData/Local/Password Manager` if running on Windows or under `$USER_HOME/.passwordmanager` if running on any other operating system.  
The software is also capable of exporting to a `passwords.html` or `passwords.csv` file, saved on the desktop.  
**NOTE**: html and csv files, which are not encrypted, are extremely vulnerable.
