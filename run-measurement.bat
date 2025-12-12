@echo off
REM Usage: run-cli.bat file1.problem file2.problem ...

REM Get a locale-independent timestamp: YYYYMMDDhhmmss
for /f "skip=1" %%x in ('wmic os get LocalDateTime') do (
    if not defined ts set ts=%%x
)

REM Extract parts
set yyyy=%ts:~0,4%
set mm=%ts:~4,2%
set dd=%ts:~6,2%
set hh=%ts:~8,2%
set min=%ts:~10,2%
set ss=%ts:~12,2%

REM Build friendly timestamp
set timestamp=%yyyy%%mm%%dd%-%hh%%min%%ss%

REM Final CSV filename
set csv=measurement-%timestamp%.csv

echo Output CSV: %csv%
echo.

for %%f in (%*) do (
    echo Running: %%f
    gradlew cli --args="measure %%f -n 10 -t 2 -u -c %csv%"
)
