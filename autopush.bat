@echo off
if "%~1"=="" (
    echo Usage: autopush.bat "Pesan pembaruan Anda"
    exit /b 1
)

echo Bumping version...
python bump_version.py

set MESSAGE=%~1

git add .
git commit -m "[vUpdate] %MESSAGE%"
git push

echo.
echo Selesai! Cek GitHub Actions untuk melihat progres rilis.
