@echo off
setlocal
cd /d "%~dp0"
title MusicLoud Launcher

echo ========================================
echo          MUSICLOUD LAUNCHER
echo ========================================
echo.

if not exist "src\gui\Main.java" (
  echo ERROR: Cannot find src\gui\Main.java
  echo Please keep this file in the project root folder.
  pause
  exit /b 1
)

if not exist "build\classes" mkdir "build\classes"

echo [1/2] Compiling MusicLoud...
dir /s /b "src\*.java" > sources.txt
javac -encoding UTF-8 -cp "*;src" -d "build\classes" @sources.txt
if errorlevel 1 (
  echo.
  echo Compilation failed. Check the errors above.
  del sources.txt 2>nul
  pause
  exit /b 1
)
del sources.txt 2>nul

echo.
echo [2/2] Starting MusicLoud...
java -cp "build\classes;src;*" gui.Main
if errorlevel 1 (
  echo.
  echo MusicLoud closed with an error.
  pause
)
endlocal
