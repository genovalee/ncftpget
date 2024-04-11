@echo off
SET command_jar=S:\FSG001\P20G01\jar\FtpDownLoad.jar
SET command_prop=S:\fsg001\p20g01\config\prop.properties
cmd /C java -jar "%command_jar%" "%command_prop%"  
echo %ERRORLEVEL%