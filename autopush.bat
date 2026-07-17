@echo off
set MESSAGE=%~1
if "%MESSAGE%"=="" (
    set MESSAGE=Pembaruan sistem dan perbaikan rutin
)

echo Bumping version...
python bump_version.py

git add .
git commit -m "[vUpdate] %MESSAGE%"
git push origin main

echo.
echo Selesai! Cek GitHub Actions untuk melihat progres rilis.
