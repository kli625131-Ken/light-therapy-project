@echo off
setlocal enabledelayedexpansion
echo Starting Light Therapy Backend...
echo.

REM Check if JAVA_HOME is set
if not defined JAVA_HOME (
    echo Error: JAVA_HOME is not set.
    echo Please set JAVA_HOME to your JDK installation directory.
    pause
    exit /b 1
)

REM Set classpath including all dependencies in .m2 directory
set CLASSPATH=target/classes
for /r "%USERPROFILE%\.m2" %%f in (*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%f
)

REM Start the application
echo Using CLASSPATH: %CLASSPATH%
echo.
java -cp "%CLASSPATH%" com.lontri.lighttherapy.LightTherapyApplication

pause