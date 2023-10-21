# PasswordManager
Password Manager is an application able to add, modify and delete locally saved accounts, characterized by a software, a username and an [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard "AES explanation.")-encrypted password.  
The program will be protected with a password that the user must remember.  
Two languages are available: English and Italian. If you want to help translating, contact me at my email.


**The JAR and Windows Installers (exe and msi) are available [here](https://github.com/Achille004/PasswordManager/tree/main/compiled "Compiled stuff folder.").**  
To execute the program is reccomended jdk 17.0.1 or higher.


The data files will be saved under `$USER_HOME/AppData/Local/Password Manager` if running on Windows or under execution directory if running on any other operating system.  
The software is also capable of exporting to a `passwords.html` or `passwords.csv` file, saved on the desktop.  
**NOTE**: html and csv files, which are not encrypted, are extremely vulnerable.
