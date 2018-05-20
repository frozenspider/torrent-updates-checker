@ECHO OFF
for /f "delims=" %%i in ('dir torrent-updates-checker-*.jar /b/s') do @set jarfile=%%i
START /B javaw -jar "%jarfile%" start
