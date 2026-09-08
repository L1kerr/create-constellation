@echo off
setlocal enabledelayedexpansion

:: Check if JAVA_HOME is already set and valid for Java 21
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        goto :run_build
    )
)

:: Auto-detect JDK 21 in standard directories
for %%d in (
    "%USERPROFILE%\.jdks\ms-21.0.9"
    "%USERPROFILE%\.jdks\ms-21.0.11"
    "%USERPROFILE%\.jdks\openjdk-21*"
    "%USERPROFILE%\.jdks\corretto-21*"
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Microsoft\jdk-21*"
    "C:\Program Files\BellSoft\LibericaJDK-21*"
) do (
    if exist "%%~d\bin\javac.exe" (
        set "JAVA_HOME=%%~d"
        goto :run_build
    )
)

:run_build
if defined JAVA_HOME (
    echo [INFO] Using JDK at: %JAVA_HOME%
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo [WARN] No JDK 21 found automatically. Using system default Java.
)

:: Ensure reliable networking on systems with complex virtual/VPN interfaces
set "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true %JAVA_TOOL_OPTIONS%"
set "GRADLE_OPTS=-Djava.net.preferIPv4Stack=true %GRADLE_OPTS%"

echo [INFO] Building CreateTree mod with Gradle...
call "%~dp0gradlew.bat" build %*

exit /b %ERRORLEVEL%
