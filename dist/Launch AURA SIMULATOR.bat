@echo off
title AURA SIMULATOR
java -cp "%~dp0AuraSimulator.jar;%~dp0..\lib\*" ^
     --module-path "C:\Program Files\Java\javafx-sdk-17.0.18\lib" ^
     --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics ^
     SplashScreen
pause
