@echo off
REM Usage: run-cli.bat file1.problem file2.problem ...

for %%f in (%*) do (
    echo Running: %%f
    gradlew cli --args="measure %%f -n 10 -t 1 -u -c measurement.csv"
)
