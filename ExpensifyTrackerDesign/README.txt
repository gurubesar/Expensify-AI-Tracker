GEMINI AI API KEY SETUP GUIDE
==============================

This application uses Google Gemini AI for features such as AI Chat, Expense Analysis, and Receipt Scanning. To enable these AI functionalities, you need to provide your own API Key in the 'local.properties' file.

STEPS TO SETUP:
---------------

1. Get a Free API Key:
   Visit Google AI Studio (https://aistudio.google.com/) and create a new API Key.

2. Update 'local.properties' File:
   - Open the file named 'local.properties' located in the project's root folder.
   - Add the following line at the very bottom:

     GEMINI_API_KEY=YOUR_API_KEY_HERE

3. Run the Application:
   - Android Studio will automatically read this key through the 'build.gradle.kts' file.
   - You may need to click "Sync Project with Gradle Files" if prompted.

WHY IS THIS NECESSARY?
----------------------
This is a Best Practice in Android development to prevent sensitive information like API Keys from being exposed in the source code, especially when sharing projects or using version control systems like Git.

Thank you!