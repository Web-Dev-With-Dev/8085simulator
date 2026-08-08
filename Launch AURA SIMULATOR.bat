@echo off
setlocal EnableDelayedExpansion

set CP=dist\AuraSimulator.jar
for %%f in (lib\*.jar) do set CP=!CP!;%%f
for %%f in (dist\lib\*.jar) do set CP=!CP!;%%f

java -cp "!CP!" --module-path "C:\Program Files\Java\javafx-sdk-17.0.18\lib" --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics SplashScreen

endlocal
