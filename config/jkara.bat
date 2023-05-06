@echo off
set PATH=%~dp0ffmpeg;%PATH%
"%JAVA_HOME%\bin\java" -jar %~dp0jkara-1.0.jar %*
