@echo off
setlocal EnableDelayedExpansion

set SRC_DIR=src\main\java
set OUT_DIR=out
set JAR_NAME=party-marker-sync.jar
set SOURCES=

if exist "%OUT_DIR%" rd /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

for /r "%SRC_DIR%" %%f in (*.java) do (
  set SOURCES=!SOURCES! "%%f"
)

javac -d "%OUT_DIR%" %SOURCES%
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)

copy /Y mod.json "%OUT_DIR%" >nul
if errorlevel 1 (
  echo Failed to copy mod.json.
  exit /b 1
)

jar --create --file "%JAR_NAME%" -C "%OUT_DIR%" .
if errorlevel 1 (
  echo Jar packaging failed.
  exit /b 1
)

echo Built %JAR_NAME%
