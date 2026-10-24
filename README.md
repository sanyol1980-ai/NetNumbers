# NetNumbers — передача чисел по TCP/UDP (Android)

## Получить APK через GitHub (без Android Studio)
1. Создайте новый репозиторий на github.com.
2. Загрузите в него все файлы проекта (включая скрытую папку `.github`).
   Проще через git:
       git init && git add . && git commit -m "init"
       git branch -M main
       git remote add origin <URL вашего репозитория>
       git push -u origin main
3. Откройте вкладку **Actions** → «Build APK» (запускается сам после push, либо кнопкой Run workflow).
4. Когда сборка станет зелёной, откройте её и скачайте **NetNumbers-debug-apk** из раздела Artifacts (внутри zip лежит `app-debug.apk`).
5. Перенесите APK на телефон и установите (разрешите установку из неизвестных источников).

## Локальная сборка
Нужны JDK 17 и Android SDK (или просто откройте папку в Android Studio):
    gradle assembleDebug
APK: `app/build/outputs/apk/debug/app-debug.apk`
