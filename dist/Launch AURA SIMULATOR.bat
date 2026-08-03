@echo off
title AURA SIMULATOR
java --module-path "C:\Program Files\Java\javafx-sdk-17.0.19\lib" ^
     --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics ^
     -jar "%~dp0AuraSimulator.jar"
