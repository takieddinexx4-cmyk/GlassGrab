@echo off
setlocal
set DIRNAME=%~dp0
set WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    where gradle >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        gradle wrapper --gradle-version 9.6.0 --no-daemon
    ) else (
        echo ERROR: gradle-wrapper.jar not found and system gradle is unavailable.
        exit /b 1
    )
)
if defined JAVA_HOME (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVACMD=java.exe"
)
"%JAVACMD%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
