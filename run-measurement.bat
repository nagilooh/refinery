@echo off
setlocal enabledelayedexpansion
REM Usage: run-cli.bat file1.problem file2.problem ...

REM ==== Generate timestamp (locale-independent) ====
for /f "skip=1" %%x in ('wmic os get LocalDateTime') do (
    if not defined ts set ts=%%x
)

set yyyy=%ts:~0,4%
set mm=%ts:~4,2%
set dd=%ts:~6,2%
set hh=%ts:~8,2%
set min=%ts:~10,2%
set ss=%ts:~12,2%

set timestamp=%yyyy%%mm%%dd%-%hh%%min%%ss%

REM CSV filename (one per run)
set csv=measurement-%timestamp%.csv

echo CSV Output: %csv%

REM Make sure output directory exists
if not exist output mkdir output

REM ==== Loop over input files ====
for %%f in (%*) do (
    echo Running: %%f

    REM Extract name and extension
    for %%A in ("%%f") do (
        set name=%%~nA
        set ext=%%~xA
    )

    REM Build timestamped output filename
    set outFile=!name!-!timestamp!!ext!
    set outPath=output/!outFile!

    echo Output file: !outPath!

    REM Call Gradle
    gradlew cli --args="measure %%f -n 10 -t 1 -u -c !csv! -o \"!outPath!\""
)

endlocal
