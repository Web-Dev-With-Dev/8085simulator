@echo off
setlocal EnableDelayedExpansion

set CP=
for %%f in (lib\*.jar) do set CP=!CP!;%%f
for %%f in (dist\lib\*.jar) do set CP=!CP!;%%f

set SOURCES=
for %%f in (src\*.java) do set SOURCES=!SOURCES! "%%f"

javac -cp "!CP!" -d build\classes --module-path "C:\Program Files\Java\javafx-sdk-17.0.18\lib" --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics !SOURCES!

if %ERRORLEVEL% == 0 (
    rem -- Copy resource files into build\classes so they land in the JAR --
    copy /Y src\aura_logo.dat    build\classes\aura_logo.dat    >nul 2>&1
    copy /Y src\create.dat       build\classes\create.dat       >nul 2>&1
    copy /Y "Video Project.mp4"  build\classes\splash_video.mp4 >nul 2>&1
    jar --create --file dist\AuraSimulator.jar --manifest manifest.mf -C build\classes .
    echo BUILD OK
) else (
    echo BUILD FAILED
)
endlocal
