@echo off
echo Creating file list in tasks directory...

REM Change directory to the root of your project (just in case)
cd /d %~dp0

REM Check if tasks\fb directory exists
if not exist fb (
    echo Error: fb directory not found. Make sure this script is in your project root.
    pause
    exit /b 1
)

REM List JSON files in tasks\fb, get only filenames (/b), and redirect to file_list.txt in tasks dir
dir /b /a-d fb\*.json > fb_task_list.txt

echo File list created in tasks\file_list.txt
pause
exit /b 0
