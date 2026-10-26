@rem Standard Gradle wrapper launcher for Windows (Gradle 8.x).
@rem NB: requires gradle\wrapper\gradle-wrapper.jar. Android Studio generates it
@rem automatically on first open, or run:  gradle wrapper --gradle-version 8.9
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
if defined JAVA_HOME (set JAVACMD=%JAVA_HOME%\bin\java.exe) else (set JAVACMD=java.exe)
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
