# MusicLoud - Music Library Management System

## Complete Documentaion [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/muhammadraza30/Music-Library-Management-System) 

## Project Overview
MusicLoud is a Java-based Music Library Management System designed to allow users to browse, search, and play songs. The system features two types of user authentication: **Admin** and **Simple User**, each with distinct functionalities.

## Features
### **Admin Features**
- Add, edit, or delete songs from the library.
- View all registered users.
- Delete user accounts.

### **Simple User Features**
- View the list of available songs, including details such as artist name, release year, and genre.
- Search for songs by title, artist, or genre.
- Receive song recommendations based on preferences.
- Play songs (with limited functionality).
- Create playlists (feature under development).

## Getting Started
### **Login System**
1. **Admin Login:** Use `admin` as both the username and password to access administrative functionalities.
2. **Simple User Login:** Enter your registered username and password. If you do not have an account, create one first.

### **Playing Songs**
- Songs must be added to the project folder in `.wav` format.
- Copy the file location of the song and paste it into the code at the **Play** button.

## Demonstration Video
If you encounter any issues or need guidance, watch the demonstration video:  
[Watch Here](https://youtu.be/NATTkMK6n5A?si=vSf-j6y8fso2fhQF)

## Future Enhancements
- **Full Playlist Management**: Implementing a fully functional playlist feature.
- **Improved Song Playback**: Enhancing the song-playing experience.
- **User Profile Management**: Allowing users to update their details.

## Acknowledgments
Thank you for visiting my project! If you find it useful, leave a like and follow my profile. 😊


## MusicLoud Purple UI (VS Code)

The application entry point is `gui.Main`. It opens the refreshed purple-themed Swing interface (`gui.MusicLoudRedesign`). Existing song and user file handling classes and data files are retained.

### Run on Windows
1. Install a JDK (17 or newer) and make sure `java` and `javac` work in PowerShell.
2. Extract the complete project folder.
3. Keep `RUN_MUSICLOUD.bat`, `src`, `build.xml`, and all `.jar` files together in the project root.
4. Double-click `RUN_MUSICLOUD.bat`, or run `./RUN_MUSICLOUD.bat` from PowerShell.

### Run manually in VS Code PowerShell
From the project root:

```powershell
$jars = (Get-ChildItem -Path . -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ';'
$files = Get-ChildItem -Path ".\src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force ".\build\classes" | Out-Null
javac -encoding UTF-8 -cp $jars -d ".\build\classes" $files
$cp = "build\classes;src;" + ((Get-ChildItem -Filter "*.jar" | ForEach-Object { $_.Name }) -join ';')
java -cp $cp gui.Main
```

### Connect to the Spring Boot backend

The Swing app uses `http://localhost:8080` by default. Start PostgreSQL with a `musicloud` database, then from the repository's `musicloud-backend\musicloud-backend` folder run:

```powershell
.\mvnw.cmd spring-boot:run
```

Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` if your local database uses different connection details. Then launch the Swing app as above. The app checks `/api/health`, uses the backend for account sign-in, songs, and playlists when available, and retains local song-file access while the backend is offline.

To use a non-default backend URL, set `MUSICLOUD_API_URL` before starting the app, for example:

```powershell
$env:MUSICLOUD_API_URL = "https://your-musicloud-backend.example"
.\RUN_MUSICLOUD.bat
```

The `musicloud.api.url` Java system property can also override the URL. Admin demo login is `admin` / `admin` after the backend has initialized its database. Audio playback currently supports WAV files, as in the original project.
