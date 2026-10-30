<details>
<summary>Русская версия</summary>

# Звонки — приложение Android

Показывает постоянное уведомление с текущим уроком или переменой, временем до конца и следующим уроком. Уведомление пересчитывается каждые 20 секунд. После перезагрузки телефона включённый таймер запускается снова.

В приложении есть собственная иконка звонка, монохромная иконка уведомления и карточный экран расписания с выделением текущего дня.

На карточке каникул показывается отсчёт по циклу «5 недель учёбы / 1 неделя отдыха» и «6 недель учёбы после Нового года / 1 неделя отдыха». Расчётный учебный год начинается 1 сентября; точные утверждённые даты конкретной школы могут отличаться.

## Виджеты и расписания

- На главный экран можно добавить компактный виджет с обратным отсчётом или подробный виджет со следующим уроком. Нажатие открывает приложение.
- В приложении нет встроенного расписания конкретной школы. Откройте **Настройки → Мои расписания → Добавить расписание**, задайте имя, затем вставьте содержимое своего текстового файла в поле расписания и сохраните профиль.
- Формат строки: `день время предмет`, например `Пн 08:00-08:40 Предмет`. Дни: `Пн`, `Вт`, `Ср`, `Чт`, `Пт`, `Сб`, `Вс`.
- Код или распространённое название предмета можно вводить в строке расписания. Коды сгруппированы в [SubjectCatalog.java](app/src/main/java/com/homik/bell/SubjectCatalog.java):
  - Языки: `en`, `de`, `fr`, `es`, `zh`, `pl`, `foreign`, `foreign2`, `rus`, `ukr`, `bel`, `nativelang`, `nativelit`, `minoritylang`.
  - Математика и естественные науки: `math`, `alg`, `geom`, `probstat`, `phys`, `chem`, `bio`, `science`, `naturecourse`, `ecology`, `astro`, `geo`, `geoua`.
  - Языки и литературы: `lit`, `worldlit`, `ukrlit`, `bellit`, `ruslit`, `literatures`, `langlit`, `reading`.
  - История и общественные предметы: `hist`, `historyru`, `historyua`, `historyby`, `worldhistory`, `byworldhistory`, `uahistorycivics`, `introhistory`, `historysociety`, `localhistory`, `social`, `civics`, `law`, `economics`, `finance`, `dnkr`, `importanttalks`.
  - Технологии, здоровье, безопасность и искусство: `cs`, `robotics`, `tech`, `labor`, `drawing`, `pe`, `pehealth`, `life_safety`, `safety`, `obzr`, `ua_defense`, `healthsafety`, `health`, `ethics`, `art`, `artculture`, `mhk`, `visualarts`, `music`, `musicart`.
  - Начальная школа и интегрированные курсы: `humanworld`, `exploreworld`, `worldaround`, `orkse`, `classhour`.
- Также распознаются русские, украинские, белорусские и английские варианты названий. По умолчанию приложение показывает название на выбранном языке интерфейса; язык названий можно выбрать отдельно. Неизвестный предмет сохраняется как введено. Списки предметов меняются по классу и школе, поэтому коды покрывают основные и распространённые дополнительные курсы, а не каждый факультатив.
- Интерфейс приложения доступен на русском, английском, українській та беларускай мовах (или на языке системы). Язык названий предметов выбирается отдельно; пользовательские названия остаются без автоперевода.
- Каталог сверялся с типовой программой [МОН Украины](https://mon.gov.ua/osvita-2/zagalna-serednya-osvita/osvitni-programi/modelni-navchalni-programi-dlya-5-9-klasiv-novoi-ukrainskoi-shkoli-zaprovadzhuyutsya-poetapno-z-2022-roku), материалами [Минпросвещения России](https://edu.gov.ru/press/11456/sergey-kravcov-predmet-dnkr-osnovan-na-izuchenii-tradicionnyh-cennostey-s-oporoy-na-utverzhdennyy-na-federalnom-urovne-panteon-geroev/) и типовыми планами на [сайте Министерства образования Беларуси](https://edu.gov.by/urovni-obrazovaniya/srenee-obr/srenee-obr/informatsiya/tipovye-uchebnye-plany/).

## Как собрать APK

1. Откройте папку `SchoolBell` в Android Studio.
2. Дождитесь загрузки Gradle и установите Android SDK 35, если Android Studio предложит.
3. Выберите **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Установите APK на телефон. При первом включении разрешите уведомления.

## Настройки и свои расписания

- Откройте **Настройки** на главном экране, чтобы выбрать тему, добавить текстовые строки в уведомление или импортировать их из `.txt`.
- В разделе **Мои расписания** добавьте профиль и заполните уроки по строкам. Формат: `день время предмет`, например `Пн 08:00-08:40 Предмет`. Дни: `Пн`, `Вт`, `Ср`, `Чт`, `Пт`, `Сб`, `Вс`.
- Сохранённый профиль автоматически становится активным. Переключать профили можно в настройках или нажатием на название текущего расписания на главном экране.
- На Android 12 и новее палитра Material You берётся из системных цветов устройства; на более старых версиях используется резервная палитра.

</details>

<details>
<summary>English version</summary>

# School Bell — Android app

Shows a persistent notification with the current lesson or break, the time remaining, and the next lesson. The notification is recalculated every 20 seconds. After the phone restarts, an enabled timer starts again.

The app has its own bell icon, a monochrome notification icon, and a card-based timetable screen with the current day highlighted.

The holiday card counts down using a “5 weeks of school / 1 week of rest” cycle and a “6 weeks of school after New Year / 1 week of rest” cycle. The estimated school year starts on September 1; the exact approved dates may differ for each school.

## Widgets and timetables

- You can add a compact countdown widget or a detailed widget with the next lesson to the home screen. Tapping it opens the app.
- The app does not include a timetable for a specific school. Open **Settings → My schedules → Add schedule**, enter a name, then paste the contents of your text file into the timetable field and save the profile.
- Line format: `day time subject`, for example `Mon 08:00-08:40 Subject`. Days: `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`.
- You can enter a subject code or a common subject name in a timetable line. The codes are grouped in [SubjectCatalog.java](app/src/main/java/com/homik/bell/SubjectCatalog.java):
  - Languages: `en`, `de`, `fr`, `es`, `zh`, `pl`, `foreign`, `foreign2`, `rus`, `ukr`, `bel`, `nativelang`, `nativelit`, `minoritylang`.
  - Mathematics and natural sciences: `math`, `alg`, `geom`, `probstat`, `phys`, `chem`, `bio`, `science`, `naturecourse`, `ecology`, `astro`, `geo`, `geoua`.
  - Languages and literature: `lit`, `worldlit`, `ukrlit`, `bellit`, `ruslit`, `literatures`, `langlit`, `reading`.
  - History and social subjects: `hist`, `historyru`, `historyua`, `historyby`, `worldhistory`, `byworldhistory`, `uahistorycivics`, `introhistory`, `historysociety`, `localhistory`, `social`, `civics`, `law`, `economics`, `finance`, `dnkr`, `importanttalks`.
  - Technology, health, safety, and arts: `cs`, `robotics`, `tech`, `labor`, `drawing`, `pe`, `pehealth`, `life_safety`, `safety`, `obzr`, `ua_defense`, `healthsafety`, `health`, `ethics`, `art`, `artculture`, `mhk`, `visualarts`, `music`, `musicart`.
  - Primary school and integrated courses: `humanworld`, `exploreworld`, `worldaround`, `orkse`, `classhour`.
- Russian, Ukrainian, Belarusian, and English subject names are also recognized. By default, the app shows names in the selected interface language; subject-name language can be selected separately. Unknown subjects are kept exactly as entered. Subject lists vary by grade and school, so the codes cover common core and additional courses rather than every elective.
- The app interface is available in Russian, English, Ukrainian, and Belarusian (or the system language). Subject-name language can be selected separately; custom subject names are not translated automatically.
- The catalog was checked against the standard curriculum from the [Ministry of Education and Science of Ukraine](https://mon.gov.ua/osvita-2/zagalna-serednya-osvita/osvitni-programi/modelni-navchalni-programi-dlya-5-9-klasiv-novoi-ukrainskoi-shkoli-zaprovadzhuyutsya-poetapno-z-2022-roku), materials from the [Russian Ministry of Education](https://edu.gov.ru/press/11456/sergey-kravcov-predmet-dnkr-osnovan-na-izuchenii-tradicionnyh-cennostey-s-oporoy-na-utverzhdennyy-na-federalnom-urovne-panteon-geroev/) and standard plans on the [Belarusian Ministry of Education website](https://edu.gov.by/urovni-obrazovaniya/srenee-obr/srenee-obr/informatsiya/tipovye-uchebnye-plany/).

## Building the APK

1. Open the `SchoolBell` folder in Android Studio.
2. Wait for Gradle to load and install Android SDK 35 if Android Studio asks you to.
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Install the APK on your phone. Allow notifications when you enable them for the first time.

## Settings and custom timetables

- Open **Settings** on the home screen to choose a theme, add text lines to the notification, or import them from a `.txt` file.
- In **My schedules**, add a profile and enter lessons line by line. Format: `day time subject`, for example `Mon 08:00-08:40 Subject`. Days: `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`.
- A saved profile becomes active automatically. You can switch profiles in Settings or by tapping the current timetable name on the home screen.
- On Android 12 and newer, the Material You palette uses the device's system colors; older versions use a fallback palette.

</details>
