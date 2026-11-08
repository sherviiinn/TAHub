# TA Hub - زمینه‌ی پروژه

آخرین به‌روزرسانی: ۱۸ آبان ۱۴۰۵ (2026-11-08) — بخش هوش مصنوعی: پوشه‌ی `ai-service` توی ریپو، تصمیم Docker، روش Git (بخش‌های ۲، ۳، ۴.۱، ۸، ۹.۱.۱، ۱۱، ۱۲)

## این فایل رو چطور استفاده کنیم

- این فایل توی ریشه‌ی ریپو می‌مونه. آخر هر فاز، توی همون commit کد، به‌روزش کنید.
- اول هر چت جدید با Claude این فایل رو بچسبونید (یا توی یه Project توی حساب خودتون بذارید). بعد بگید کی هستید (بک‌اند، فرانت، دیتابیس و تست) و روی چی کار می‌کنید.
- هیچ رمز، کلید یا اطلاعات شخصی توی این فایل ننویسید. چند نفر اون رو توی چت هوش مصنوعی می‌ذارن.
- حافظه‌ی Claude مال حساب هر نفره و جای این فایل رو نمی‌گیره. منبع اصلی، Git و همین فایله.

## ۱. پروژه چیه

TA Hub یه پلتفرم دانشگاهی برای دانشجو، استاد، TA و ادمینه: درس‌ها، ثبت‌نام، تمرین، نمره‌دهی آنلاین روی PDF، جدول نمره (Grade Sheet)، اعتراض، بانک سؤال با راه‌حل‌های بچه‌ها، نظرسنجی استاد و TA، استخدام TA و برنامه‌ریز درسی. سند اصلی همون PDF به اسم «UniHub - Final Project Specification & 2-Month Roadmap» ـه. اسم TA Hub هنوز قطعی نیست.

- مهلت: سه ماه از شروع (اوایل مهر ۱۴۰۵)، یعنی حدود اوایل دی ۱۴۰۵ (یه ماه اضافه‌وقت داده شد).
- دانشگاه: نوشیروانی. به هیچ API دانشگاه وابسته نیستیم (توی spec هم ادغام با سیستم دانشگاه جزو «آینده» ـه).
- تقسیم کار: بک‌اند و دیتابیس (شروین)، فرانت (مهرسا)، و بخش هوش مصنوعی (ماهان، بخش ۹.۱).
- هدف اصلی (همین‌جوری معرفی می‌شه): یه برنامه به‌جای چند برنامه. الان تمرین توی کوئرا، نمره توی گوگل شیت و اطلاعیه و لینک گروه‌ها توی کانال‌های تلگرامه، TA شدن حضوریه (برای دانشجویی که از شهر دیگه میاد سخته)، و برای انتخاب واحد جایی برای نظر دانشجوها درباره‌ی استادها نیست. چهار هدف: (۱) TA شدن از راه دور، (۲) تمرین، تحویل PDF و نمره‌دهی با جدول شبیه گوگل شیت و اعتراض، (۳) صفحه‌ی هر درس با اطلاعیه، لینک گروه و منابع، (۴) نظر دانشجوها درباره‌ی استاد و TA با هوش مصنوعی.
- MVP (تصمیم ۱۸ آبان ۱۴۰۵، جای «فاز ۰ تا ۶» قبلی): فاز ۰ تا ۶ **به‌علاوه‌ی فاز ۸ (نظرسنجی)** و **کل بخش هوش مصنوعی** (فیلتر فحش، خلاصه‌ی نظرها، دستیار درسی) همه Must Have ان و باید تا ۱۷ دی تموم بشن. ترتیب کار: فاز ۴ (همراه تست‌های خودکار برای ارائه‌ی ۲)، فاز ۵، فاز ۶، فاز ۸؛ ماهان هوش مصنوعی رو هم‌زمان جلو می‌بره. فاز ۷، ۹ و ۱۰ و سلف Could Have ان. جدول کامل اولویت‌ها و داستان‌های کاربر توی `docs/phase-01-mvp-user-stories/README.md` ـه.

## ۲. ابزارها و اجرا

- Java 21، Spring Boot 4.1.1، Maven، Thymeleaf (صفحه‌ها سمت سرور ساخته می‌شن)، Spring Security، Spring Data JPA (Hibernate)، PostgreSQL 16 توی Docker.
- IntelliJ IDEA، ویندوز، Git و ریپوی عمومی (Public) روی GitHub (از ۱۶ آبان ۱۴۰۵، برای تحویل درس).
- اجرای لوکال:
  1. نصب JDK 21، IntelliJ، Docker Desktop و Git.
  2. `git clone` و باز کردن پروژه توی IntelliJ.
  3. توی پوشه‌ی پروژه `docker compose up -d` (دیتابیس روی پورت 5433 سیستم، پورت 5432 داخل کانتینر).
  4. Run کردن `TaHubApplication`.
  5. باز کردن `http://localhost:8080` (با `http://`، نه `https://`).
- سرویس هوش مصنوعی (پوشه‌ی `ai-service`، پایتون): **برای کار روزانه‌ی بک‌اند و فرانت لازم نیست روشن باشه** (Spring با پیاده‌سازی تقلبی `AiClient` کار می‌کنه، وقتی ساخته بشه). تصمیم: با Docker و همون `docker compose up -d` بالا بیاد تا کسی پایتون نصب نکنه (`Dockerfile` و سرویس `ai` توی `docker-compose.yml` هنوز اضافه نشدن، بخش ۹.۱.۱). تا اون موقع اجرای دستی (فقط برای کسی که روی هوش مصنوعی کار می‌کنه)، توی پوشه‌ی `ai-service`: یه بار `py -m venv venv`، `venv\Scripts\activate`، `pip install -r requirements.txt`، `copy .env.example .env` و گذاشتن کلید Gemini خودتون توی `.env` (کلید رایگان از AI Studio؛ **هیچ‌وقت commit نشه**)؛ بعد هر بار `venv\Scripts\activate` و `uvicorn app.main:app --port 8000` و باز کردن `http://127.0.0.1:8000/docs`.
- اگه پورتی روی سیستم شما مشغوله، فقط روی سیستم خودتون عوضش کنید و commit نکنید.
- مشکل دیتابیس یا تغییر ساختار: `docker compose down -v` و بعد `docker compose up -d` (همه‌ی داده‌ی لوکال پاک می‌شه).
- داده‌ی نمونه: با `tahub.demo-data=true` توی `application.properties`، دیتابیس **خالی** موقع اجرا پر می‌شه: دو ترم (4051 آرشیو، 4052 فعال)، دو استاد (نام کاربری `karimi` و `hosseini`)، شش دانشجو با شماره‌ی ۱۱ رقمی (40312100001 تا 40312100003، 40212100004، 40212100005، 40412100006)، سه رشته، سه درس، ارائه‌ها و ثبت‌نام‌ها با همه‌ی وضعیت‌ها. رمز حساب‌های نمونه اولین بار توی کنسول چاپ می‌شه. ادمین پیش‌فرض (شماره `admin`) رو `DataInitializer` می‌سازه. همه‌ی این‌ها فقط برای توسعه‌ان.

## ۳. ساختار کد

پکیج اصلی `ir.TAHub.TAHub`.

| پکیج | محتوا |
|---|---|
| ریشه | `TaHubApplication` و کنترلرها: `HomeController`، `LoginController`، `RegistrationController`، `UserController`، `CourseController`، `SemesterController`، `EnrollmentController`، `MyCoursesController`، `MajorController`، `ProfileController`، `RosterController` (لیست کلاس)، `TaController` (اضافه، حذف و برگردوندن TA)، `TaApplicationController` (درخواست TA شدن)، `ProfessorController` (بخش اساتید)، `TelegramLinkController` (لینک تلگرام درس)، `TaInvitationController` (قبول، رد و کنسل دعوت TA)، `TaLimitRequestController` (درخواست افزایش سقف تیم)، `ResourceController` (منابع آموزشی)، `ProfessorController` (بخش اساتید و اطلاعات دفتر)، `AnnouncementController` (اطلاعیه‌ها)، `NotificationController` (نوتیفیکیشن‌ها)، `CafeteriaController` (منوی سلف)، `FoodController` (فهرست غذاها)، `SiteNoticeController` (اطلاعیه‌ی ادمین)، `GlobalModelAttributes` (داده‌ی منو برای همه‌ی صفحه‌ها) |
| `model` | `User`، `Role` (STUDENT, PROFESSOR, ADMIN)، `Semester`، `Course`، `CourseOffering`، `Enrollment`، `EnrollmentStatus`، `EnrollmentSource` (ROSTER، JOIN_CODE)، `Major`، `RosterEntry`، `TaAssignment`، `TaPermission`، `TaPermissionConverter`، `TaApplication`، `TaApplicationStatus`، `TaInvitation`، `TaInvitationStatus`، `TaLimitRequest`، `TaLimitRequestStatus`، `CourseResource`، `ResourceKind` (نوع لینک برای آیکون)، `ProfessorProfile`، `Announcement`، `Notification`، `NotificationType` (ANNOUNCEMENT، GENERAL)، `Food`، `CafeteriaMeal`، `MealType` (BREAKFAST، LUNCH، DINNER)، `CafeteriaSettings`، `SiteNotice`، `SiteNoticeDismissal` |
| `repository` | برای هر جدول یه Repository (متدهایی که Spring از روی اسمشون کوئری می‌سازه) |
| `dto` | `RegisterForm`، `CourseForm` (فرم‌ها، تا کاربر نتونه فیلدهایی مثل نقش رو بفرسته) |
| `config` | `SecurityConfig`، `DataInitializer`، `DemoDataSeeder`، `SchedulingConfig` (روشن کردن کارهای زمان‌بندی‌شده با `@Scheduled`) |
| `security` | `CustomUserDetailsService` (لاگین با شماره‌ی دانشجویی یا نام کاربری استاد؛ رقم فارسی قبوله)، `DisabledAccountFilter` (کاربری که حسابش غیرفعال شده با کلیک بعدی بیرون انداخته می‌شه) |
| `service` | `SemesterService` (شروع ترم جدید به‌صورت یکجا، و بستن درخواست‌ها و دعوت‌های TA ترم قبل)، `TaHousekeeping` (انقضای ۷روزه‌ی دعوت‌ها و درخواست‌های سقف، و بستن کارهای TA ترم تموم‌شده)، `RosterService` (خوندن لیست چسبونده‌شده و ثبت‌نام خودکار)، `NotificationService` (ساختن نوتیفیکیشن‌ها)، `TaService` (قانون‌های TA شدن، هم برای افزودن دستی و هم قبول درخواست)، `OfferingAccess` (کی توی یه ارائه چه کاری می‌تونه بکنه: استاد همه‌چیز، TA طبق دسترسی‌هاش)، `CafeteriaService` (هفته‌ی منو، ذخیره‌ی یکجای هفته، پاک‌سازی شبانه) |
| `util` | `JoinCodes` (ساخت کد ورود با SecureRandom)، `Digits` (تبدیل ارقام فارسی و عربی به انگلیسی) |

منابع: `templates/` (صفحه‌های Thymeleaf)، `static/` (CSS و JS، بعداً)، `messages.properties` (همه‌ی متن‌های صفحه)، `application.properties`.

**پوشه‌ی `ai-service/` (سرویس هوش مصنوعی، پایتون، جدا از کد جاوا؛ روی build جاوا اثری نداره):** `app/config.py` (تنظیمات از `.env`)، `app/llm.py` (تنها فایلی که با Gemini حرف می‌زنه)، `app/security.py` (فقط درخواست با هدر `X-Internal-Key` درست قبوله)، `app/main.py` (endpointها)، `app/rag/` (`reader.py` خوندن PDF، `ingest.py` تکه کردن و ذخیره، `store.py` SQLite و جستجو، `qa.py` جواب دادن)، `tools/` (ابزارهای تست از CMD)، `requirements.txt` (کتابخونه‌ها؛ خود کتابخونه‌ها commit نمی‌شن)، `.env.example`. فایل `.env` و پوشه‌های `data/` و `venv/` با `ai-service/.gitignore` بیرون از Git می‌مونن.

## ۴. قراردادها (لطفاً رعایت کنید)

- کد، اسم‌ها و کامنت‌ها انگلیسی. هر متنی که کاربر می‌بینه توی `messages.properties` و با `#{key}` استفاده می‌شه. فارسی و راست‌به‌چپ آخر کار با `messages_fa.properties` اضافه می‌شه.
- چک فرم‌ها فقط **اولین خطا** رو به ترتیب فیلدها نشون می‌ده: متد `findFirstError(...)` کلید پیام رو برمی‌گردونه.
- هر کاری که داده رو عوض می‌کنه POST ـه. فرم‌ها همیشه `th:action` دارن (توکن امنیتی CSRF رو اضافه می‌کنه).
- مجوز دو لایه داره: قانون‌های آدرس توی `SecurityConfig`، و چک «مالکیت» توی کد (مثل `CourseOffering.isManagedBy(user)`)، چون قانون آدرس نمی‌دونه کی صاحب چیه.
- به id یا نقشی که از فرم میاد اعتماد نکنید: شیء واقعی رو لود کنید و چک کنید. همیشه روی کاربر واردشده کار کنید، نه کاربری که id ش توی درخواست اومده.
- سابقه هیچ‌وقت پاک نمی‌شه: ثبت‌نام‌ها وضعیت دارن، ارائه‌ها پرچم `cancelled` دارن، ترم‌های قدیمی فقط از لیست‌ها پنهان می‌شن.
- enum توی دیتابیس: Hibernate برای هر ستون enum (`@Enumerated(STRING)`، و حتی با AttributeConverter) یه CHECK با اسم‌های فعلی می‌سازه. اضافه کردن مقدار جدید به enumی که توی دیتابیس هست، یه ALTER دستی (حذف اون CHECK) لازم داره. برای چیزهایی که مقدار جدید می‌گیرن (مثل دسترسی‌ها) متن ساده نگه دارید.
- تغییر دیتابیس: فعلاً `ddl-auto=update`. اضافه کردن ستون اجباری (NOT NULL) به جدولی که ردیف داره موقع اجرا بی‌صدا شکست می‌خوره. یا ستون رو اختیاری بذارید، یا قبل از اجرا `ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...` بزنید، یا دیتابیس لوکال رو ریست کنید. commitهایی که ساختار رو عوض می‌کنن توی پیامشون `SCHEMA CHANGE` بنویسن.
- روش کار با Claude (صاحب پروژه): Claude به پوشه‌ی پروژه روی کامپیوتر صاحب پروژه دسترسی داره و فایل‌ها رو مستقیم می‌نویسه؛ commit، merge و push رو خود صاحب پروژه انجام می‌ده.
- Git: روش کامل توی بخش ۴.۱.

## ۴.۲. پوشه‌ی `docs` (خروجی هر فاز برای درس)

استاد درس خواسته خروجی هر فاز از برنامه‌ی کار ترم توی ریپو، داخل پوشه‌ی `docs` و هر فاز توی پوشه‌ی جدای خودش باشه (ترجیحاً فایل `.md`). زمان بررسی‌شون تصادفیه و اگه خروجی نباشه نمره کم می‌شه، پس **خروجی هر فاز باید تا آخر بازه‌اش توی ریپو push شده باشه.** منتورها (`seydanloo`، `aminupy`، `AmirHosseinZg`) به ریپو دسترسی دارن.

- `docs/README.md`: فهرست همه‌ی فازها با تاریخ و وضعیت.
- `docs/phase-01-mvp-user-stories` تا `docs/phase-13-presentation-3`: هر فاز یه `README.md` داره؛ فایل‌های دیگه‌ی همون فاز (عکس نمودارها، پروتوتایپ، اسلاید، گزارش تست) کنارش.
- `README.md` اصلی ریپو فقط معرفی، لینک به فازها، روش اجرا و تست، و اعضای تیمه. محتوای اصلی تکراری نوشته نمی‌شه.
- پروتوتایپ: `docs/phase-03-prototype-api-tech-hosting/prototype/index.html` (GitHub Pages از پوشه‌ی `/docs` شاخه‌ی `main`).

## ۴.۱. روش کار با Git

قانون اصلی: **`main` همیشه سالم و تست‌شده‌ست.** هیچ کاری مستقیم روی `main` انجام نمی‌شه.

**هر قدم = یه شاخه.**
- برای هر قدم (مثل ۴-الف یا ۴-ب) یه شاخه‌ی جدا از `main` به‌روز ساخته می‌شه. اسمش `step-<فاز><حرف>-<اسم کوتاه>` ـه، مثل `step-4a-accounts` یا `step-4b-assignments`.
- قبل از شروع:
  ```
  git switch main
  git pull
  git switch -c step-4b-assignments
  ```
- اگه وسط کار تغییرهای commit‌نشده هست و فقط شاخه عوض می‌شه، `git switch -c` اون تغییرها رو با خودش می‌بره.

**اصلاح‌ها روی همون شاخه.**
- اگه تست مشکل داشت یا چیزی باید عوض بشه، روی **همون شاخه** یه commit جدید زده می‌شه. برای هر اصلاح شاخه‌ی جدا ساخته نمی‌شه.
- فقط وقتی همه‌ی تست‌ها اوکی شد، شاخه یه بار merge می‌شه.

**merge، push و پاک کردن شاخه.**
```
git add .
git commit -m "Short English message about what changed"
git switch main
git pull
git merge --no-ff step-4b-assignments
git push
git branch -d step-4b-assignments
```
- `--no-ff` باعث می‌شه توی تاریخچه معلوم باشه هر قدم کجا شروع و کجا تموم شده. برای اینکه همیشه همین‌طور باشه: `git config merge.ff false`.
- اگه شاخه قبلاً push شده بود، نسخه‌ی روی GitHub هم پاک بشه: `git push origin --delete step-4b-assignments`.

**بخش هوش مصنوعی هم دقیقاً همین روش رو داره.** اسم شاخه: `step-ai-<اسم کوتاه>` (مثل `step-ai-docker`، `step-ai-endpoints`)، چون قدم‌های هوش مصنوعی شماره‌ی فاز ندارن. فقط فایل‌های داخل `ai-service/` (و بخش ۹.۱ همین فایل) عوض می‌شن؛ اگه کاری به فایل‌های جاوا یا `docker-compose.yml` دست می‌زنه، قبلش با برنامه‌نویس بک‌اند هماهنگ بشه. قبل از هر commit با `git status` چک بشه که `ai-service/.env` و `ai-service/data/` توی لیست نیستن (ریپو عمومیه).

**پیام commit.**
- انگلیسی، کوتاه، و بگه چی عوض شد (نه «fix» خالی).
- اگه ساختار دیتابیس عوض شده (جدول یا ستون جدید)، توی پیام `SCHEMA CHANGE` بیاد.
- اگه یه ALTER دستی لازمه، دستور کاملش هم توی همین فایل (بخش ۵ یا خط همون قدم توی نقشه‌ی راه) نوشته بشه تا هم‌تیمی‌ها هم بزنن.

**برچسب آخر هر فاز.**
```
git tag phase-3-done
git push origin phase-3-done
```

**پاک کردن فایل.** یا `git rm <فایل>`، یا فایل دستی پاک بشه و بعد `git add .` (پاک شدن رو هم ثبت می‌کنه).

**کار با Claude.**
- Claude فایل‌ها رو مستقیم توی پوشه‌ی پروژه می‌نویسه، ولی هیچ دستور git‌ای که چیزی رو عوض کنه (commit، merge، switch و ...) اجرا نمی‌کنه. ساختن شاخه، commit، merge و push همیشه با خود صاحب پروژه‌ست. Claude آخر هر قدم دستورهای لازم رو می‌نویسه.
- Claude فقط برای دیدن وضعیت، `git -c core.autocrlf=true status` یا `diff` می‌زنه (بدون اون گزینه، روی ویندوز همه‌ی فایل‌ها به‌خاطر فرق پایان خط «عوض‌شده» دیده می‌شن).

**چیزهای دیگه.**
- فقط کد تست‌شده commit بشه. مرتب push بشه. ریپو عمومیه، پس هیچ رمز یا کلید واقعی توش نره.
- رمز، کلید API و اطلاعات شخصی هیچ‌وقت commit نمی‌شن.
- میان‌بر دیدن تاریخچه: `git config --global alias.lg "log --graph --oneline --decorate --all"` و بعد `git lg`.

## ۵. ساختار دیتابیس (فعلی)

- `users`: id، full_name، student_number (یکتا، همون نام کاربری: برای دانشجو شماره، برای استاد و ادمین نام کاربری با حروف کوچک)، password_hash (BCrypt)، role، major_id (اختیاری، فقط دانشجوها)، disabled_at (اختیاری؛ خالی یعنی فعال)، released_number (اختیاری؛ وقتی شماره آزاد شده، شماره‌ی قبلی اینجاست و student_number می‌شه `released-<id>`).
- `student_number_settings`: id (همیشه ۱)، digits (پیش‌فرض ۱۱)، years_back (پیش‌فرض ۱۰). اگه ردیف نباشه پیش‌فرض‌ها استفاده می‌شن.
- `professor_invites`: id، code (یکتا، ۱۰ حرفی)، for_name (اختیاری)، created_by_id، created_at، expires_at (۱۴ روز بعد)، used_at، used_by_id، cancelled. ردیف‌ها پاک نمی‌شن (سابقه).
- `majors`: id، name (یکتا). ادمین از داخل برنامه مدیریتش می‌کنه.
- `semesters`: id، code (یکتا، ۴ یا ۵ رقم مثل 4052)، active (هر لحظه فقط یکی فعاله).
- `courses`: id، code (یکتا، با حروف بزرگ)، name. دائمی و مستقل از ترم. ویدیوها و منابع به این وصل می‌شن.
- `course_offerings`: id، course_id، semester_id، professor_id، join_code (اختیاری؛ خالی یعنی ثبت‌نام بسته)، cancelled، ta_applications_open (پیش‌فرض false)، telegram_link (اختیاری)، resource_suggestions_hidden (اختیاری)، ta_team_limit (سقف تیم TA؛ ارائه‌ی جدید با ۱ شروع می‌شه، خالی یعنی بدون سقف، ارائه‌های قدیمی‌تر خالی‌ان). یکتا روی (درس، ترم، استاد)، پس دو استاد می‌تونن یه درس رو توی یه ترم ارائه بدن (دو گروه).
- `roster_entries`: id، offering_id، student_number، added_at. شماره‌های لیست کلاس که استاد چسبونده (حتی اگه هنوز حساب ندارن). یکتا روی (ارائه، شماره).
- `ta_assignments`: id، user_id، offering_id، active، assigned_at، permissions_set (اختیاری؛ خالی یعنی TA قدیمی که هنوز پیش‌فرض‌ها رو داره). یکتا روی (کاربر، ارائه).
- `professor_profiles`: id، professor_id (یکتا)، building، room، availability، email، phone، telegram (بدون @)، website، note (همه اختیاری)، updated_at، updated_semester_id.
- `course_resources`: id، offering_id، title (حداکثر ۱۵۰)، url (حداکثر ۵۰۰، فقط https)، description (اختیاری، ۳۰۰)، category (اختیاری، ۱۰۰)، created_at، deleted.
- `ta_limit_requests`: id، offering_id، requested_by_id، requested_limit، message (اختیاری، حداکثر ۳۰۰)، status (PENDING، APPROVED، REJECTED، EXPIRED، CANCELLED)، created_at، decided_at.
- `ta_invitations`: id، offering_id، invitee_id، invited_by_id، by_manager، status، created_at، decided_at. حداکثر یه دعوت PENDING برای هر دانشجو و ارائه (توی کد چک می‌شه).
- `ta_assignment_permissions`: ta_assignment_id، permission (متن ساده؛ توی جاوا `Set<String>` ـه، نه `Set<TaPermission>`، چون Hibernate برای ستون enum یه CHECK با اسم‌های فعلی می‌سازه و هر دسترسی جدید ALTER دستی لازم داشت). **یه بار برای همه لازمه** (چون نسخه‌ی قبلی این CHECK رو ساخته): `docker exec -it tahub-db psql -U tahub -d tahub -c "ALTER TABLE ta_assignment_permissions DROP CONSTRAINT IF EXISTS ta_assignment_permissions_permission_check;"`
- `announcements`: id، offering_id، author_id، title (حداکثر ۱۰۰)، body (حداکثر ۲۰۰۰)، created_at، edited_at (اختیاری، زمان آخرین ویرایش)، deleted (پنهان‌شدن، برگشت‌ناپذیر از داخل برنامه).
- `foods`: id، name (یکتا، حداکثر ۱۰۰؛ «ي» و «ك» عربی به فارسی تبدیل می‌شن)، active، created_at.
- `cafeteria_meals`: id، meal_date، meal_type (BREAKFAST، LUNCH، DINNER)، option1_id (اجباری)، option2_id (اختیاری)، updated_at، updated_by_id. یکتا روی (روز، وعده). فقط دو هفته نگه داشته می‌شه.
- `site_notices`: id، author_id، title (حداکثر ۱۰۰)، body (حداکثر ۲۰۰۰)، created_at، edited_at (اختیاری)، show_in_banner، deleted.
- `site_notice_dismissals`: id، user_id، notice_id، dismissed_at. یکتا روی (کاربر، اطلاعیه).
- `cafeteria_settings`: id (همیشه ۱)، open_days_before، open_time، close_days_before، close_time، updated_at.
- `notifications`: id، user_id، type، title (برای اطلاعیه: «کد درس - اسم درس»)، link (مسیر داخل برنامه که سرور می‌سازه)، item_count (تعداد رویدادی که این ردیف نشون می‌ده)، announcement_id (اختیاری، فقط برای نوتیفیکیشن «اطلاعیه ویرایش شد»)، first_event_at (اختیاری)، message_key و message_arg (اختیاری؛ برای نوتیفیکیشن‌هایی که متنشون از `messages.properties` میاد، مثل یادآوری سلف)، seen، created_at.
- `ta_applications`: id، student_id، offering_id، message، status (PENDING، ACCEPTED، REJECTED، WITHDRAWN)، created_at، decided_at. یکتا روی (دانشجو، ارائه).
- `enrollments`: id، student_id، offering_id، enrolled_at، status، source (ROSTER یا JOIN_CODE). برای هر دانشجو و ارائه فقط یه ردیف. یکتا روی (دانشجو، ارائه).

## ۶. چیزهایی که الان کار می‌کنن

نقش‌ها:
- **دانشجو:** ثبت‌نام (به شرطی که ادمین حداقل یه رشته تعریف کرده باشه)، لاگین با شماره‌ی دانشجویی، دیدن درس‌های ترم فعال، ورود به درس با کد، انصراف، «درس‌های من» برای هر ترمی که توش بوده، تغییر رشته از پروفایل، دیدن TA های هر درس و بخش اساتید، درخواست TA شدن (وقتی استاد باز کرده باشه) و «درخواست‌های TA من».
- **استاد:** ساخت ارائه برای خودش، دیدن لیست دانشجوهای درس‌هاش (با رشته‌شون، چه فعال چه حذف‌شده)، حذف و برگردوندن دانشجو، ساخت کد جدید یا بستن ثبت‌نام، لغو ارائه (فقط وقتی دانشجوی فعال نداره) و برگردوندنش، ارائه‌ی دوباره‌ی درس‌های ترم‌های قبل توی ترم فعال، چسبوندن لیست شماره‌های دانشجویی کلاس، اضافه و حذف و برگردوندن TA، باز و بسته کردن درخواست TA، و قبول و رد و بازبینی درخواست‌ها توی «TA requests».
- **ادمین:** هر کاری که استاد روی ارائه‌ها می‌کنه، روی همه‌ی ارائه‌ها (و موقع ساخت، استاد رو انتخاب می‌کنه)، به‌علاوه‌ی مدیریت کاربرها، ترم‌ها (شروع ترم جدید) و رشته‌ها. ادمین «درس‌های من» نداره.

قوانین:
- لیست درس‌ها فقط ارائه‌های ترم فعال و لغونشده رو نشون می‌ده. ارائه‌های قدیمی با آدرس مستقیم باز می‌شن و فقط‌خواندنی‌ان (متن‌ها گذشته، بدون دکمه).
- وضعیت ثبت‌نام: ACTIVE (فعال)، DROPPED (خود دانشجو انصراف داده، با کد می‌تونه برگرده)، REMOVED (استاد حذفش کرده، خودش نمی‌تونه برگرده، فقط استاد برش می‌گردونه).
- کد ورود: ۶ کاراکتر از حروفی که با هم قاطی نمی‌شن، ساخته‌شده با SecureRandom، فقط به مدیرهای ارائه نشون داده می‌شه و دانشجو هیچ‌وقت نمی‌بینه. کد جدید کد قبلی رو باطل می‌کنه. بستن ثبت‌نام کد رو خالی می‌کنه.
- شروع ترم جدید، ترم فعلی رو یکجا آرشیو می‌کنه. چیزی پاک نمی‌شه.
- حذف دانشجو از یه درس به حساب و درس‌های دیگه‌اش هیچ ربطی نداره.
- لیست کلاس: ارقام فارسی قبوله، حداکثر ۵۰۰ شماره در هر بار. دانشجوی دارای حساب خودکار وارد می‌شه، بقیه یادداشت می‌شن و موقع ثبت‌نام وارد می‌شن. کسی که انصراف داده یا حذف شده برنمی‌گرده. TA همون ارائه هم با لیست دانشجو نمی‌شه.
- TA: فقط کاربر با نقش دانشجو می‌تونه TA باشه. نمی‌شه توی ارائه‌ای که دانشجوی فعالشه TA باشه (و برعکس)، و این برای همه‌ی گروه‌های همون درس توی همون ترم هم صدق می‌کنه. حذف TA فقط `active` رو false می‌کنه (سابقه می‌مونه)؛ برگردوندن یعنی دوباره اضافه کردن، با دسترسی‌های تازه. کارهای هر TA با دسترسی‌هاش تعیین می‌شه (۳-چ). اسم TA ها رو همه می‌بینن، شماره‌ی دانشجویی و دکمه‌ها فقط مدیرهای ارائه.
- درخواست TA: پیش‌فرض بسته‌ست و استاد برای هر ارائه بازش می‌کنه. هر دانشجو برای هر ارائه یه درخواست داره (وضعیت‌ها PENDING، ACCEPTED، REJECTED، WITHDRAWN). رد شده با Reconsider برمی‌گرده. پس گرفته‌شده رو دانشجو می‌تونه دوباره بده. قبول کردن همون قانون‌های TA شدن رو دوباره چک می‌کنه.
- اطلاعیه: استاد (و ادمین) و TA های ارائه می‌نویسن. می‌خونن: استاد، TA ها و دانشجوهای فعال همون ارائه (بقیه ۴۰۳). استاد هر اطلاعیه‌ای رو پاک می‌کنه، TA فقط مال خودش رو. پاک‌کردن فقط پنهان می‌کنه و راه برگشت نداره. ارائه‌ی قدیمی یا لغوشده فقط خواندنیه. اطلاعیه‌های ۳ روز اول برچسب «New» دارن (`recent` توی `AnnouncementView`، کلاس CSS به اسم `tag-new`).
- نوتیفیکیشن: با هر اطلاعیه‌ی جدید، هر دانشجوی فعال ارائه (به جز نویسنده) یه نوتیفیکیشن می‌گیره. برای اینکه صفحه شلوغ نشه، هر دانشجو برای هر درس فقط **یه نوتیفیکیشن خونده‌نشده** داره و اطلاعیه‌ی بعدی شمارنده‌ش (`item_count`) رو زیاد می‌کنه و بالا میاردش. متن «New announcement in CS201 - Operating Systems» یا «2 new announcements in ...» و با کلیک می‌ره صفحه‌ی اطلاعیه‌های همون درس. هر کس فقط نوتیفیکیشن‌های خودش رو می‌بینه. ۵۰ تای آخر نشون داده می‌شن؛ پاکسازی نوتیفیکیشن‌های خیلی قدیمی بعداً.
- بخش اساتید: همه‌ی کاربرها لیست استادها و درس‌های ترم فعال هر استاد (بدون ارائه‌های لغوشده) و TA های هر درس رو می‌بینن.

## ۶.۱. فهرست کامل قابلیت‌ها

همه‌ی چیزهایی که برنامه قراره داشته باشه (تصمیم‌های تیم تا ۱۷ آبان ۱۴۰۵). علامت‌ها: **[انجام]** ساخته و تست شده، **[تست نشده]** نوشته شده و تستش مونده (قدم ۴-الف)، **[برنامه]** هنوز ساخته نشده. اولویت هر کدوم برای درس (Must / Should / Could) توی `docs/phase-01-mvp-user-stories/README.md` ـه.

**۱. حساب و ورود**
- [انجام] ثبت‌نام دانشجو با شماره‌ی دانشجویی و رشته (بدون رشته‌ی تعریف‌شده، ثبت‌نام بسته‌ست).
- [انجام] ورود و خروج؛ شماره‌ی دانشجویی همون نام کاربریه؛ رمز با BCrypt.
- [انجام] نقش‌ها: دانشجو، استاد، ادمین. TA نقش جدا نیست، دانشجوییه که توی یه درس TA شده.
- [انجام] پروفایل و تغییر رشته.
- [تست نشده] فرمت شماره‌ی دانشجویی (۱۱ رقم، سه رقم اول سال ورودی، از ۱۰ سال قبل تا سال ترم فعال؛ قابل تنظیم توسط ادمین؛ رقم فارسی قبوله).
- [تست نشده] ثبت‌نام استاد با کد دعوت یه‌بارمصرف ادمین (۱۴ روز) و نام کاربری انگلیسی.
- [تست نشده] پنل حساب‌های ادمین: جستجو، غیرفعال و فعال کردن، آزاد کردن شماره‌ای که کس دیگه گرفته (با تایپ دوباره‌ی شماره)، فیلتر شماره‌ی نامعتبر. هیچ حسابی پاک نمی‌شه.
- [تست نشده] بیرون انداختن کاربر واردشده‌ای که حسابش غیرفعال شده؛ صفحه‌های `/admin` فقط برای ادمین.
- [برنامه] تأیید دانشجو با عکس کارت دانشجویی (بعد از فاز ۴؛ فقط ادمین می‌بینه و بعد از تصمیم پاک می‌شه).

**۲. ترم، رشته و درس**
- [انجام] ترم با کد مثل 4052؛ ادمین ترم جدید رو دستی شروع می‌کنه و ترم قبل آرشیو می‌شه (چیزی پاک نمی‌شه).
- [انجام] رشته‌ها رو ادمین تعریف می‌کنه؛ رشته‌ای که استفاده شده پاک نمی‌شه.
- [انجام] درس دائمی جدا از «ارائه» (همون درس توی یه ترم با یه استاد).
- [انجام] استاد برای خودش ارائه می‌سازه، ادمین برای هر استادی؛ «ارائه‌ی دوباره» درس‌های ترم قبل.
- [انجام] لغو ارائه (فقط بدون دانشجوی فعال) و برگردوندنش.
- [انجام] لیست درس‌ها فقط ترم فعال؛ ترم‌های قبل فقط‌خواندنی با متن زمان گذشته.
- [انجام] «درس‌های من» با جابه‌جایی بین ترم‌ها.

**۳. ورود دانشجو به درس**
- [انجام] کد ورود ۶ حرفی (فقط مدیرهای درس می‌بینن؛ کد جدید، بستن ثبت‌نام).
- [انجام] لیست کلاس: استاد شماره‌ها رو می‌چسبونه (تا ۵۰۰)؛ صاحبان حساب همون موقع وارد می‌شن و بقیه موقع ثبت‌نام.
- [انجام] منبع هر ثبت‌نام (لیست یا کد) برای استاد دیده می‌شه.
- [انجام] وضعیت‌ها: فعال، انصراف (با کد برمی‌گرده)، حذف‌شده توسط استاد (فقط استاد برش می‌گردونه).
- [انجام] استاد رشته‌ی دانشجوهاش رو می‌بینه (فعال و حذف‌شده).

**۴. TA و تیم TA**
- [انجام] هر کس می‌تونه TA چند درس باشه؛ هم‌زمان دانشجو و TA یه درس نمی‌شه (برای همه‌ی گروه‌های همون درس توی همون ترم).
- [انجام] درخواست TA شدن (استاد باز یا بسته می‌کنه، قبول، رد، Reconsider؛ دانشجو پس می‌گیره و دوباره می‌ده).
- [انجام] دعوت به TA شدن؛ دعوت‌شده قبول یا رد می‌کنه.
- [انجام] دسترسی‌های جدا برای هر TA (اطلاعیه، مدیریت اطلاعیه‌ها، تلگرام، منابع، دیدن و مدیریت دانشجوها، مدیریت تیم، دادن دسترسی).
- [انجام] Head TA (چند تا، فقط استاد تعیین می‌کنه؛ Headها همدیگه رو تغییر نمی‌دن).
- [انجام] سقف تیم (پیش‌فرض ۱)؛ Head درخواست افزایش سقف می‌ده؛ تیم پر از هیچ راهی عضو نمی‌گیره.
- [انجام] انقضای ۷ روزه‌ی دعوت‌ها و درخواست‌ها و بسته شدنشون با تموم شدن ترم.
- [انجام] حذف TA سابقه رو نگه می‌داره؛ TA خودش از تیم بیرون نمی‌ره (فقط استاد یا Head حذف می‌کنن).
- [برنامه] پنل «My TA courses» برای TAها.
- [برنامه] درخواست بازبینی (یه بار) برای TAای که حذف شده.

**۵. اطلاعیه و نوتیفیکیشن**
- [انجام] اطلاعیه‌ی هر درس (فقط اعضای درس؛ ویرایش با «edited»؛ حذف فقط پنهان می‌کنه؛ برچسب «New»).
- [انجام] نوتیفیکیشن یکی‌شده (برای هر موضوع یه خونده‌نشده که با اتفاق جدید شمارنده‌اش زیاد می‌شه و میاد بالا)؛ با ویرایش و حذف اطلاعیه درست می‌شه.
- [انجام] نوتیفیکیشن دعوت و درخواست TA، قبول و رد، Head شدن، لینک تلگرام، درخواست افزایش سقف.
- [انجام] عدد نوتیفیکیشن زنده بدون رفرش (SSE).
- [انجام] صفحه‌های زنده که نوشته‌های فرم، مکان‌نما و اسکرول رو نگه می‌دارن.
- [برنامه] پنجره‌ی کوچیک (toast) برای نوتیفیکیشن تازه.

**۶. اطلاعیه‌ی ادمین برای کل سایت**
- [انجام] صفحه‌ی اطلاعیه‌ها و نوار بالای همه‌ی صفحه‌ها (هر کاربر برای خودش می‌بنده).
- [انجام] نوتیفیکیشن بعد از ویرایش و دکمه‌ی «Notify everyone again».

**۷. سلف**
- [انجام] فهرست غذاها (اضافه، تغییر اسم، غیرفعال).
- [انجام] منوی دو هفته (جاری و بعدی)؛ هر روز صبحانه، ناهار، شام؛ هر وعده دو گزینه با «/»؛ هفته‌های تموم‌شده خودکار پاک می‌شن.
- [انجام] لینک سایت رزرو دانشگاه (هیچ رمز شخصی ذخیره نمی‌شه).
- [انجام] نوار سبز، زرد و قرمز تا مهلت رزرو هر روز؛ تنظیم زمان باز و بسته شدن رزرو.
- [انجام] یادآوری چهارشنبه ساعت ۸ (و دکمه‌ی «الان بفرست»)؛ نوتیفیکیشن «منو عوض شد».

**۸. صفحه‌ی درس، منابع و اساتید**
- [انجام] لینک گروه تلگرام برای هر ارائه.
- [انجام] منابع آموزشی (یوتیوب، آپارات، درایو، تلگرام، گیت‌هاب، PDF، لینک)، دسته‌بندی اختیاری، هر ترم خالی با پیشنهاد منابع ترم قبل.
- [انجام] بخش اساتید: استادها ← درس‌های ترم فعال ← TAهای هر درس.
- [انجام] اطلاعات دفتر استاد (همه اختیاری؛ برچسب «may be out of date» برای اطلاعات ترم قبل).

**۹. تمرین و تحویل (فاز ۴)**
- [برنامه] تمرین با سؤال، بارم و مهلت (استاد یا TA با دسترسی)؛ PDF صورت سؤال اختیاری؛ پیش‌نویس؛ نوتیفیکیشن تمرین جدید.
- [برنامه] تحویل PDF: یکی برای هر تمرین، عوض یا حذف تا قبل از مهلت، بعدش قفل؛ چک واقعی PDF بودن.
- [برنامه] ذخیره روی دیسک با اسمی که سرور می‌سازه؛ دانلود فقط برای صاحب فایل، استاد و TAها.
- [برنامه] لیست «تحویل داده / نداده».
- تصمیم‌نگرفته: تحویل دیرتر از مهلت، سقف حجم فایل، یادآوری ۲۴ ساعت قبل، دانلود ZIP.

**۱۰. نمره (فاز ۵)**
- [برنامه] نمایش PDF داخل سایت؛ نمره و توضیح برای هر سؤال؛ جمع خودکار.
- [برنامه] Grade Sheet (نام، شماره، Q1 و ...، جمع، وضعیت اعتراض)؛ انتشار نمره (هر کس فقط نمره‌ی خودش)؛ خروجی CSV.

**۱۱. اعتراض (فاز ۶)**
- [برنامه] حداکثر یه اعتراض برای هر سؤال؛ جواب TA و تغییر نمره؛ وضعیت‌ها: باز، جواب‌داده‌شده، پذیرفته، رد، حل‌شده.

**۱۲. بانک سؤال (فاز ۷)**
- [برنامه] سؤال‌ها بر اساس درس، استاد، ترم و نوع آزمون (وصل به درس دائمی)؛ چند جواب برای هر سؤال: اول جواب استاد (Official)، بعد جواب‌های تأییدشده، بعد بقیه بر اساس رأی مثبت و منفی.

**۱۳. نظرسنجی (فاز ۸)**
- [برنامه] امتیاز و نظر برای استاد و TA (بدون نمایش اسم، ولی سرور نویسنده رو می‌دونه)؛ ارزیابی پایان ترم TA توسط استاد (شایسته / ناشایسته)؛ دکمه‌ی گزارش و بررسی ادمین.

**۱۴. هوش مصنوعی (ماهان، Must Have)**
- [برنامه] فیلتر فحش: لیست کلمه‌ها، بعد هوش مصنوعی برای موارد مشکوک؛ نظر نشونه‌دار پنهان می‌شه و به ادمین می‌ره (پاک نمی‌شه).
- [برنامه] خلاصه‌ی نظرهای هر استاد و TA (بدون اسم، با برچسب «ساخته‌شده با هوش مصنوعی»، دوباره‌سازی زمان‌بندی‌شده).
- [در حال انجام] دستیار درسی با RAG («ممکنه اشتباه باشه»، سقف روزانه، بدون فرستادن اطلاعات شخصی). هسته‌ی سرویس پایتون (خوندن جزوه، ذخیره، جستجو و جواب) ساخته و روی جزوه‌ی نمونه تست شده؛ endpointها و اتصال به Spring مونده (بخش ۹.۱.۱).
- [برنامه، پیشنهاد ماهان، اولویتش با تیم] کوییز و فلش‌کارت از روی جزوه‌ها (Must Have نیست).
- قانون: API یه مدل آماده (مثل Gemini)، فقط از سمت سرور، کلید توی متغیر محیطی (جزئیات بخش ۹.۱).

**۱۵. برنامه‌ریزی درسی (فاز ۹)**
- [برنامه] برنامه‌ریز درسی با برنامه‌ی A، B، C و تشخیص تداخل (جدول جلسه‌های کلاس لازمه).
- [برنامه] هماهنگی تاریخ میان‌ترم بر اساس بیشترین دانشجوی مشترک.
- [برنامه] اگه وقت شد: تخمین تقاضای درس‌ها.

**۱۶. استخدام TA (فاز ۱۰، اگه وقت شد)**
- [برنامه] آگهی TA و درخواست با رزومه (رزومه‌ی کامل فقط برای خودش و استاد).
- [برنامه] پیشنهاد TA؛ نمره‌ی درس توی جدول جدا با «منبع».
- [برنامه] اسم و شماره‌ی دانشجوها برای TA و استاد توی Google Sheet.

**۱۷. ایده‌های دیگه**
- [برنامه] «درخواست به ادمین» (گزارش باگ، درخواست ویرایش اطلاعات استاد، ...) با وضعیت، پاسخ ادمین و نوتیفیکیشن.

**۱۸. ظاهر و دستگاه‌ها**
- [برنامه] ظاهر مینیمال شبیه ChatGPT با منوی کناری، انیمیشن نرم، پالت Navy و Teal و Sky Blue و Beige، فونت Vazirmatn (مرحله‌ی اول: شاخه‌ی `front-1-base`).
- [برنامه] فارسی و راست‌به‌چپ آخر کار با فایل ترجمه.
- [برنامه] فایل exe ویندوز؛ بعداً PWA یا اندروید (iOS کنار گذاشته شد).

**۱۹. کارهای فنی که درس هم خواسته**
- [برنامه] تست خودکار برای ورود، دسترسی‌ها و مسیر اصلی (برای ارائه‌ی ۲).
- [برنامه] آنلاین کردن روی HTTPS (برای ارائه‌ی ۳)، با کارهای بخش ۱۱ (رمزها توی متغیر محیطی، خاموش کردن داده‌ی نمونه، Flyway، محدودیت حدس کد ورود، صفحه‌های خطا، حذف لاگ debug، buffering برای نوتیفیکیشن زنده).

## ۷. آدرس‌ها و قرارداد صفحه‌ها (برای فرانت)

این آدرس‌ها، اسم فیلدهای فرم و ویژگی‌های `th:` باید بمونن. اگه صفحه‌ای داده‌ی جدید لازم داره، به برنامه‌نویس بک‌اند بگید.

| قالب | آدرس | داده‌هایی که به صفحه می‌رسه | فرم‌ها (POST) |
|---|---|---|---|
| `home` | `GET /` | `appName`، `fullName`، `role`، `isAdmin` | `/logout` |
| `login` | `GET /login` (پارامترهای `error`، `disabled`، `logout`، `registered`) | ندارد | `/login`: `username` (شماره‌ی دانشجویی یا نام کاربری استاد)، `password` |
| `register` | `GET /register` | `form` (`fullName`، `studentNumber`، `majorId`، `password`، `confirmPassword`)، `majors`، `errorKey`، `hint` (`digits()`، `fromYear()`، `toYear()`؛ دو تای آخر بدون ترم فعال null‌ان) | `/register` |
| `professor-register` | `GET /register/professor` (پارامتر اختیاری `code`) | `code`، `fullName`، `username`، `errorKey` | `/register/professor`: `code`، `fullName`، `username`، `password`، `confirmPassword` |
| `users` (ادمین) | `GET /users` | `users` (id، fullName، studentNumber، role)، `hint`، `errorKey` | `/users`: `fullName`، `studentNumber` (برای استاد و ادمین نام کاربری)، `password`، `role` (حذف دیگه نداره) |
| `accounts` (ادمین) | `GET /admin/accounts` (پارامترهای `q` برای جستجو، `invalid=true` برای فیلتر شماره‌ی نامعتبر) | `q`، `invalid`، `searched`، `users`، `courses`، `taRoles`، `disabledAt`، `validNumber` (همه map با id)، `settings`، `hint`، `accountErrorKey`، `accountDoneKey` | `/admin/accounts/{id}/disable`، `/enable`، `/release` (`confirmNumber`)، همه با `q`؛ `/admin/accounts/number-format` (`digits`، `yearsBack`) |
| `professor-invites` (ادمین) | `GET /admin/professor-invites` | `invites` (هر کدوم `state`: ACTIVE، USED، CANCELLED، EXPIRED)، `created`، `expires`، `used` (map با id)، `newCode`، `inviteErrorKey` | `/admin/professor-invites` (`forName`)، `/admin/professor-invites/{id}/cancel` |
| `courses` | `GET /courses` | `semester`، `offerings`، `canCreate` | ندارد |
| `course-form` | `GET /courses/new` | `form` (`code`، `name`، `professorId`)، `isAdmin`، `professors`، `semester`، `errorKey` | `/courses` |
| `course-detail` | `GET /courses/{id}` (پارامترهای `joinError`، `cancelError`) | `offering`، `isCurrentSemester`، `isStudent`، `isEnrolled`، `wasRemoved`، `canManage`، `isCancelled`، `enrollmentOpen`، `joinCode` (فقط مدیرها)، `enrollments`، `removedEnrollments`، `pendingRoster`، `rosterResult` و `rosterErrorKey` (فقط بعد از چسبوندن لیست)، `teachingAssistants`، `removedTeachingAssistants`، `isTa`، `taErrorKey`، `taApplicationsOpen`، `myTaApplication`، `taApplyErrorKey`، `pendingTaRequests`، `canManageStudents`، `canSeeStudents`، `canManageTeam`، `canEditTelegram`، `taTeamFull`، `taTeamSize` و `taTeamLimit` و `pendingLimitRequest` و `canRequestLimit` (برای استاد و TA با MANAGE_TEAM)، `limitRequestSent`، `limitRequestErrorKey`، `teamLimitSaved`، `teamLimitErrorKey`، `myTaPermissions` (فقط برای TA)، `taPermissionsSavedFor`؛ `teachingAssistants` حالا ردیف‌هایی با `assignment`، `head`، `canRemove`، `canEditPermissions`، `canChangeHead` ـه؛ `pendingInvitations` (ردیف‌ها: `invitation`، `canCancel`)، `taInvitedName` (و `removedTeachingAssistants` حذف شد)، `canSeeAnnouncements` (لینک اطلاعیه‌ها و لینک تلگرام فقط برای این‌ها)، `announcementCount`، `resourceCount`، `telegramSaved`، `telegramErrorKey`؛ لینک تلگرام: `offering.telegramLink` | `/courses/{id}/enroll` (`code`)، `/drop`، `/cancel`، `/uncancel`، `/join-code/regenerate`، `/join-code/close`، `/students/{sid}/remove`، `/students/{sid}/restore`، `/roster` (`numbers`)، `/tas` (`studentNumber`)، `/tas/{uid}/remove`، `/tas/{uid}/head`، `/tas/{uid}/unhead`، `/ta-team-limit` (`limit`)، `/ta-limit-requests` (`requestedLimit`، `message`)، `/ta-limit-requests/{rid}/approve`، `/ta-limit-requests/{rid}/reject`، `/ta-invitations/{iid}/cancel`، (`/tas` با `studentNumber` حالا دعوت می‌فرسته)، `/ta-applications` (`message`)، `/ta-applications/open`، `/ta-applications/close`، `/telegram` (`link`) |
| `my-courses` | `GET /my-courses?semester={id}` | `isProfessor`، `semesters`، `selectedSemesterId`، `selectedIsActive`، `offerings`، `activeSemester`، `reofferable` | `/courses/{id}/reoffer` |
| `semesters` (ادمین) | `GET /admin/semesters` | `semesters`، `errorKey` | `/admin/semesters`: `code` |
| `majors` (ادمین) | `GET /admin/majors` | `majors`، `usage` (تعداد دانشجوی هر رشته)، `errorKey` | `/admin/majors`: `name`؛ `/admin/majors/{id}/delete` |
| `ta-requests` (استاد، ادمین) | `GET /ta-requests` | `pending`، `rejected`، `requestErrorKey` | `/ta-applications/{id}/accept`، `/reject`، `/reconsider` |
| `my-ta-requests` (دانشجو) | `GET /my-ta-requests` | `applications`، `invitations` (دعوت‌های در انتظار)، `invitationErrorKey` | `/ta-applications/{id}/withdraw`، `/ta-invitations/{id}/accept`، `/ta-invitations/{id}/decline` |
| `professors` | `GET /professors` | `professors` | ندارد |
| `professor-detail` | `GET /professors/{id}` | `professor`، `courses` (هر ردیف: `offering` و `teachingAssistants`)، `profile` (یا خالی)، `profileUpdated`، `profileOld`، `canEditProfile`، `officeSaved` | ندارد |
| `professor-office-edit` (خود استاد یا ادمین) | `GET /professors/{id}/office` | `professor`، `building`، `room`، `availability`، `email`، `phone`، `telegram`، `website`، `note`، `officeErrorKey` | `/professors/{id}/office` (همون فیلدها) |
| `announcements` | `GET /courses/{id}/announcements` | `offering`، `announcements` (هر ردیف: `announcement`، `when`، `editedWhen`، `recent`، `canDelete`، `canEdit`)، `isCurrentSemester`، `isCancelled`، `canPost`، `canManage`، `announcementErrorKey` | `/courses/{id}/announcements` (`title`، `body`)، `/announcements/{id}/delete` |
| `announcement-edit` | `GET /announcements/{id}/edit` | `announcement`، `offering`، `title`، `body`، `announcementErrorKey` | `/announcements/{id}/edit` (`title`، `body`) |
| `notifications` | `GET /notifications` | `notifications` (هر ردیف: `notification`، `when`)، `unreadCount` | `/notifications/{id}/open`، `/notifications/read-all` |
| `cafeteria` | `GET /cafeteria` (یا `?next=true` برای هفته‌ی بعد) | `days` (هر ردیف: `index`، `date`، `dayKey`، `today`، `meals` که با `meals.get(m)` خونده می‌شه و `option1` و `option2` داره)، `mealTypes`، `weekStart`، `weekEnd`، `isNextWeek`، `hasAnyMeal`، `reservationUrl`، `serverNowMillis`؛ هر روز یه `reservation` هم داره (`state`: BEFORE/OPEN/CLOSED، `opensMillis`، `closesMillis`، `percentLeft`، `color`: bar-green/bar-yellow/bar-red/bar-grey، `remaining`، `opensText`، `closesText`). اسکریپت `static/js/cafeteria.js` از `data-opens`، `data-closes` و `#reservation-labels` استفاده می‌کنه و این‌ها نباید حذف بشن | ندارد |
| `cafeteria-edit` (ادمین) | `GET /admin/cafeteria` (یا `?next=true`) | همون داده‌های `cafeteria` (به جز `reservationUrl`)، به‌علاوه‌ی `activeFoods`، `slots` (۱ و ۲)، `cafeteriaSaved`، `cafeteriaErrorKey` | `/admin/cafeteria?next=...`: لیست‌ها با اسم `meal_<روز از ۰ برای شنبه>_<BREAKFAST/LUNCH/DINNER>_<1 یا 2>` و مقدار id غذا (خالی یعنی بدون غذا) |
| `cafeteria-settings` (ادمین) | `GET /admin/cafeteria/settings` | `openDaysBefore`، `openTime`، `closeDaysBefore`، `closeTime`، `settingsSaved`، `settingsErrorKey`، `reminderSentTo` | `/admin/cafeteria/settings` (همون چهار فیلد)، `/admin/cafeteria/reminder` (فرستادن یادآوری همین الان) |
| `foods` (ادمین) | `GET /admin/foods` | `foods`، `foodErrorKey` | `/admin/foods` (`name`)، `/admin/foods/{id}/rename` (`name`)، `/admin/foods/{id}/deactivate`، `/admin/foods/{id}/activate` |
| `ta-permissions` (استاد یا TA با GRANT_PERMISSIONS) | `GET /courses/{id}/tas/{userId}/permissions` | `offering`، `target`، `rows` (هر ردیف: `permission`، `checked`، `editable`)، `isManager` | `/courses/{id}/tas/{userId}/permissions`: چک‌باکس‌های `permission` (مقدار: اسم دسترسی) |
| `resources` | `GET /courses/{id}/resources` | `offering`، `groups` (هر گروه: `category` که برای بدون‌دسته خالیه، و `resources`؛ هر منبع `kind` داره)، `isEmpty`، `isCurrentSemester`، `canManage`؛ برای مدیرها `suggestions` و `categories`؛ `resourceErrorKey` و `draft*` بعد از خطا | `/courses/{id}/resources` (`title`، `url`، `description`، `category`)، `/courses/{id}/resources/copy` (چک‌باکس‌های `resourceId`)، `/courses/{id}/resources/hide-suggestions`، `/resources/{rid}/delete` |
| `resource-edit` | `GET /resources/{rid}/edit` | `resource`، `offering`، `title`، `url`، `description`، `category`، `resourceErrorKey` | `/resources/{rid}/edit` |
| `notices` | `GET /notices` | `notices` (هر ردیف: `notice`، `when`، `editedWhen`)؛ برای ادمین `noticeErrorKey`، `draftTitle`، `draftBody` | `/notices/{id}/dismiss` (`back`، برای همه)؛ ادمین: `/admin/notices` (`title`، `body`، `showInBanner`)، `/admin/notices/{id}/banner-off`، `/banner-on`، `/renotify`، `/delete`؛ بعد از renotify، `noticeRenotifiedId` |
| `notice-edit` (ادمین) | `GET /admin/notices/{id}/edit` | `notice`، `title`، `body`، `noticeErrorKey` | `/admin/notices/{id}/edit` (`title`، `body`) |
| `profile` | `GET /profile` (پارامتر `saved`) | `profileUser`، `isStudent`، `majors` | `/profile/major`: `majorId` |

## ۷.۱. قالب مشترک (layout)

- `templates/fragments/layout.html` دو تکه داره: `head(عنوان)` (متا، عنوان و `static/css/app.css`) و `nav` (منوی بالا، بر اساس نقش).
- هر صفحه این‌طور شروع می‌شه:
  ```html
  <head th:replace="~{fragments/layout :: head(#{some.title.key})}"></head>
  <body>
      <nav th:replace="~{fragments/layout :: nav}"></nav>
      <main> ... </main>
  </body>
  ```
- داده‌ی منو رو `GlobalModelAttributes` (بک‌اند) به همه‌ی صفحه‌ها می‌ده: `navLoggedIn`، `navFullName`، `navIsStudent`، `navIsProfessor`، `navIsAdmin`، `navUnreadCount` (تعداد نوتیفیکیشن خونده‌نشده، برای لینک «Notifications (n)» توی منو)، `navBannerNotice` (اطلاعیه‌ی ادمین برای نوار بالا، یا خالی)، `navCurrentPath` (صفحه‌ی فعلی، برای برگشتن بعد از بستن نوار). نوار بالا توی تکه‌ی `nav` ـه (کلاس‌های `site-banner` و `site-banner-close`). این اسم‌ها نباید عوض بشن.
- صفحه‌های زنده: ویژگی `data-live-topics` روی `<main>`، تکه‌ی `banner` (`site-banner-slot`) و id و `data-*` لینک نوتیفیکیشن‌ها برای `static/js/live.js` لازمن و نباید حذف بشن. محتوای هر صفحه باید داخل `<main>` باشه.
- فایل `static/css/app.css` مال فرانت‌کاره. از ویژگی‌های منطقی (`margin-inline-start` و ...) استفاده کنید تا فارسی و راست‌به‌چپ بعداً خراب نشه.
- طراحی فعلی (فرانت، قدم ۱، شاخه‌ی `front-1-base`): منوی کناری (`app-nav`، `sidebar`، `nav-link`، `nav-section`) که روی گوشی پشت دکمه‌ی منو می‌ره (`static/js/app.js`)، عدد نوتیفیکیشن توی دایره‌ی `nav-badge` (که `live.js` زنده عوضش می‌کنه)، فونت Vazirmatn از `static/fonts/`، و کلاس‌های آماده برای صفحه‌ها (توی `app.css` شاخه‌ی `front-1-base` تعریف شدن و بعد از merge در دسترسن؛ صفحه‌ها هنوز ازشون استفاده نمی‌کنن): `btn-primary`، `btn-danger`، `btn-ghost`، `btn-small`، `alert`، `alert-error`، `alert-success`، `card`، `card-soft`، `empty`، `page-head`، `row`، `stack`، `field`، `tag`، `tag-muted`، `main.wide` (برای جدول‌های پهن). کد جدید بک‌اند برای صفحه‌ها از همین کلاس‌ها استفاده می‌کنه.

### ۷.۲. بازبینی شاخه‌ی `front-1-base` (۱۲ آبان ۱۴۰۵)

نتیجه: **خوب و قابل merge.** فقط فایل‌های مجاز عوض شدن (`layout.html`، `app.css`، `app.js`، `live.js`، ته `messages.properties`). همه‌ی تکه‌های لازم سر جاشونن (`nav-notifications` با `data-*`، تکه‌ی `banner` و `site-banner-slot`، `live.js` توی head، همه‌ی لینک‌های منو). تغییر `live.js` (دایره‌ی عدد و جایگزین نکردن نوار زرد وقتی عوض نشده) درسته.

کارهایی که دوست فرانت‌کار باید انجام بده:
1. **فونت‌ها رو commit کنه:** `app.css` چهار فایل می‌خواد: `static/fonts/Vazirmatn-Regular.woff2`، `Vazirmatn-Medium.woff2`، `Vazirmatn-Bold.woff2`، `Vazirmatn-Black.woff2` (از ریپوی رسمی github.com/rastikerdar/vazirmatn، مجوز آزاد OFL). تا اضافه نشن، توی کنسول خطای 404 میاد و فونت پیش‌فرض نشون داده می‌شه. اگه `git add` گفت ignore شده، خط فونت‌ها رو از `.gitignore` برداره.
2. **غلط تایپی:** توی `messages.properties` خط `ta.restore=Restore TA]` یه `]` اضافه داره. این کلید دیگه استفاده نمی‌شه (Restore TA حذف شده)، پس کل خط رو پاک کنه. (`ta.removed.title` هم دیگه استفاده نمی‌شه.)
3. **اختیاری:** وقتی ادمین توی `/admin/cafeteria` (ویرایش منو و تنظیمات رزرو) هست، هیچ لینکی توی منو فعال نیست. شرط لینک Cafeteria می‌تونه `/admin/cafeteria` رو هم بگیره.
4. **روال:** قبل از merge، `git pull origin main` توی شاخه‌ی خودش بزنه تا تغییرات بک‌اند بهش برسه، و کار رو تیکه‌تیکه merge کنه تا کانفلیکت کم بشه. هماهنگی حضوریه.
5. **صفحه‌ی منابع (۳-د):** کلاس‌های `resource-list`، `resource-url` و `resource-youtube` / `resource-aparat` / `resource-drive` / `resource-telegram` / `resource-github` / `resource-pdf` / `resource-link` (روی هر `li`) برای طراحی آماده‌ان. آیکون‌های فعلی ساده و عمومی‌ان و توی خود `resources.html` تعریف شدن (`#r-<نوع>`)؛ اگه خواستی لوگوی سایت‌ها رو بذاری، همون‌جا یا با CSS عوضشون کن. برای جلوگیری از کانفلیکت با شاخه‌ات، بک‌اند به `app.css` دست نزد: استایل لیست منابع (بدون bullet، آیکون و لینک توی یه خط) رو خودت اضافه کن.
6. **قدم‌های بعدی فرانت:** صفحه‌هایی که هنوز از قالب مشترک استفاده نمی‌کنن (`home`، `courses`، `course-form`، `my-courses`، `users`، `semesters`، `majors`، `profile`، `login`، `register`) باید به قالب منتقل بشن. محتوای هر صفحه داخل `<main>` باشه و `data-live-topics` هر جا هست حفظ بشه.

## ۸. تصمیم‌هایی که گرفته شده

- ترم جدید رو ادمین دستی شروع می‌کنه (تاریخ خودکار نداریم، چون تقویم دانشگاه جابه‌جا می‌شه).
- درس دائمی از ارائه (هر ترم و هر استاد) جداست. ویدیوها و بانک سؤال به درس دائمی وصلن تا هر ترم بمونن. اطلاعیه‌ها، لینک تلگرام، ثبت‌نام، TA ها و تمرین‌ها به ارائه وصلن.
- ارائه‌های قدیمی پنهان می‌شن، هیچ‌وقت پاک نمی‌شن (نمره، اعتراض و سابقه‌ی TA بهشون وابسته‌ان).
- هر دانشجو تاریخچه‌ی هر ترم رو داره و می‌تونه بین ترم‌های قبلش جابه‌جا بشه.
- کنترل ورود به درس: **هم کد ورود، هم لیستی که استاد می‌ده**، چون بعضی وقت‌ها توی انتخاب واحد دستی اسم بچه‌ها ثبت نمی‌شه. توی لیست استاد منبع هر دانشجو (لیست یا کد) دیده می‌شه تا کسایی که رسمی نیستن معلوم باشن.
- رشته از لیستی که ادمین تعریف می‌کنه انتخاب می‌شه. استاد رشته‌ی دانشجوهای درس خودش رو می‌بینه، چه فعال (ACTIVE) و چه حذف‌شده (REMOVED).
- یه نفر می‌تونه TA چند ارائه باشه، ولی نه توی ارائه‌ای که خودش دانشجوشه.
- دانشجو تا قبل از مهلت می‌تونه فایل تحویلش رو حذف یا عوض کنه (برای هر تمرین یه تحویل؛ بعد از مهلت یا بعد از نمره‌دهی قفل می‌شه).
- کاربری که سابقه داره هیچ‌وقت واقعاً پاک نمی‌شه: حساب‌ها وضعیت می‌گیرن (ACTIVE، PENDING، DISABLED).
- بعداً استادها با یه کد دعوت یه‌بارمصرف که ادمین حضوری می‌ده خودشون ثبت‌نام می‌کنن (بدون صف تأیید و بدون ذخیره‌ی عکس مدارک). تا اون موقع ادمین استادها رو می‌سازه.
- پیشنهاد TA نباید به API دانشگاه یا همکاری استادها برای ثبت نمره‌ی نهایی وابسته باشه. نمره‌ی درس توی یه جدول جدا با کلید (دانشجو، درس دائمی) و یه ستون «منبع» نگه داشته می‌شه (استاد وارد کرده، خود دانشجو اعلام کرده، استاد استخدام‌کننده تأیید کرده، بعداً از دانشگاه). فرمول اول به داده‌های خود برنامه (امتیاز TA، ارزیابی استاد، سابقه‌ی TA) تکیه می‌کنه و نمره فقط کمکیه.
- همه‌ی متن‌های صفحه فعلاً انگلیسی، فارسی و راست‌به‌چپ آخر کار.
- هوش مصنوعی یه سرویس جدای پایتون (FastAPI) توی همین ریپوئه (پوشه‌ی `ai-service`)، نه داخل خود Spring. دلیل: کار انجام‌شده (خوندن PDF با Gemini، کش، جستجو) دوباره نوشته نشه و کسی که روی هوش مصنوعی کار می‌کنه درگیر کد Spring نشه. مرورگر هیچ‌وقت مستقیم صداش نمی‌زنه؛ فقط Spring با هدر `X-Internal-Key`. اجرا با Docker کنار دیتابیس (۱۸ آبان ۱۴۰۵).

## ۹. نقشه‌ی راه

**فاز ۲ (درس‌ها): تموم شد** (برچسب `phase-2-done`). کارهای عقب‌افتاده‌ی این فاز (وضعیت حساب به‌جای حذف، کد دعوت استاد، جلوگیری از ثبت‌نام با شماره‌ی دیگران) توی قدم ۴-الف انجام شدن. جزئیات قدم‌های انجام‌شده:
- ۲-ز: استاد لیست شماره‌های دانشجویی کلاس رو می‌چسبونه. کسایی که حساب دارن خودکار وارد درس می‌شن، بقیه یادداشت می‌شن و موقع ثبت‌نام خودکار وارد می‌شن. هر ثبت‌نام یه «منبع» (لیست یا کد) داره. دانشجوی حذف‌شده توسط استاد برنمی‌گرده. **(انجام و تست شد)**
- ۲-ح: TA بودن توی ارائه (جدول `ta_assignments`، یکتا روی کاربر و ارائه؛ حذف TA فقط `active` رو false می‌کنه)، نمایش TA ها توی صفحه‌ی ارائه. **(انجام و تست شد؛ حذف TA برگشت‌پذیره)**
- ۲-ح۲: درخواست TA شدن: استاد برای ارائه‌اش «پذیرش درخواست TA» رو باز یا بسته می‌کنه. دانشجو درخواست (با پیام کوتاه) می‌ده، استاد توی یه پنل «درخواست‌های TA» قبول یا رد می‌کنه، و قبول کردن خودکار اون دانشجو رو TA می‌کنه (جدول `ta_applications`، وضعیت‌ها PENDING، ACCEPTED، REJECTED، WITHDRAWN). بعداً توی فاز ۱۰ رزومه و نمره‌ی درس به همین درخواست وصل می‌شه. ردکردن برگشت‌پذیره (دکمه‌ی Reconsider)؛ درخواستی که دانشجو خودش پس گرفته (WITHDRAWN) رو می‌تونه دوباره بده. **(انجام و تست شد)**
- ۲-ط: بخش اساتید: لیست استادها، با کلیک روی استاد درس‌های ترم فعالش و TA هاش، با کلیک روی درس صفحه‌ی ارائه. بعداً اطلاعات دفتر و نظرسنجی هم اینجا میاد. **(انجام و تست شد)**
- قالب مشترک، منوی بالا و CSS پایه (بخش ۷.۱). **(انجام و تست شد؛ بقیه‌ی صفحه‌ها رو دوست فرانت‌کار به قالب می‌بره)**
- بعد از فاز ۲: وضعیت حساب (غیرفعال کردن به‌جای حذف)، کد دعوت استاد. **(توی ۴-الف انجام شد)**

**فاز ۳: تموم شد** (برچسب `phase-3-done` باید زده بشه):
- ۳-الف: اطلاعیه‌ها برای هر ارائه. **(انجام و تست شد)**
- ۳-ب: نوتیفیکیشن برای دانشجوها (زنگ و تعداد نخوانده توی منو، اطلاعیه‌ها اونجا هم میان). **(انجام و تست شد)**
- ۳-ب۳: وقتی استاد (یا TA) اطلاعیه‌ای رو پاک می‌کنه، نوتیفیکیشن دانشجوها (چه خونده‌شده چه خونده‌نشده) درست می‌شه: شمارنده کم می‌شه و اگه چیزی نمونده نوتیفیکیشن حذف می‌شه (ستون اختیاری `first_event_at`، بدون نیاز به ALTER). با حذف، شمارنده و زمان نوتیفیکیشن به آخرین اطلاعیه‌ی باقی‌مونده برمی‌گرده تا ترتیب لیست مثل قبل بشه. **(انجام و تست شد)**
- ۳-ب۴: ویرایش اطلاعیه. استاد/ادمین هر اطلاعیه‌ای و TA فقط مال خودش رو ویرایش می‌کنه (همون قانون حذف)، فقط توی ترم فعال و ارائه‌ی لغو‌نشده. بعد از ویرایش کنار اطلاعیه «(edited زمان)» میاد. بعد از ویرایش، به دانشجوهای فعال ارائه نوتیفیکیشن «Announcement edited in ...» می‌ره (برای هر اطلاعیه فقط یکی؛ اگه خونده‌نشده باشه فقط میاد بالا؛ اگه دانشجو نوتیفیکیشن خونده‌نشده‌ی «اطلاعیه‌ی جدید» که همین اطلاعیه رو شامل می‌شه داشته باشه، دوباره خبر داده نمی‌شه). با حذف اطلاعیه، نوتیفیکیشن‌های ویرایش اون هم حذف می‌شن. ستون‌های اختیاری `announcements.edited_at` و `notifications.announcement_id`، بدون نیاز به ALTER (نوع نوتیفیکیشن ویرایش همون `GENERAL` موجوده، چون عوض کردن enum توی دیتابیس ALTER دستی می‌خواست). **(انجام و تست شد)**
- ۳-ب۲: تمیزکاری: یه نوتیفیکیشن برای هر درس به‌جای یکی برای هر اطلاعیه، برچسب «New» کنار اطلاعیه‌های تازه، حذف دکمه‌ی Restore اطلاعیه. **(انجام و تست شد؛ تغییر دیتابیس: `ALTER TABLE notifications ADD COLUMN item_count integer NOT NULL DEFAULT 1` و پاک کردن نوتیفیکیشن‌های تستی قدیمی با `DELETE FROM notifications`)**
- ۳-پ۱: منوی سلف. فهرست غذاها (`/admin/foods`، جدول `foods`): ادمین اضافه، ویرایش اسم و غیرفعال می‌کنه (حذف نداره). برنامه‌ی هفتگی (`/admin/cafeteria`): هر روز صبحانه، ناهار و شام، هر وعده حداکثر دو گزینه که فقط از غذاهای فعال انتخاب می‌شن و برای دانشجو به شکل «غذای ۱ / غذای ۲» نشون داده می‌شن. فقط هفته‌ی جاری و هفته‌ی بعد وجود دارن و هر شب ساعت ۰۰:۱۰ (تهران) منوی هفته‌های تموم‌شده پاک می‌شه. صفحه‌ی `/cafeteria` برای همه، با لینک سایت رزرو دانشگاه (`food.nit.ac.ir`). سایت سلف لاگین می‌خواد، پس منو دستی وارد می‌شه و رمز شخصی هیچ‌کس توی برنامه ذخیره نمی‌شه. **(انجام و تست شد)**
- ۳-پ۲: زمان رزرو و یادآوری سلف. تنظیمات ادمین (`/admin/cafeteria/settings`، جدول یه‌ردیفه‌ی `cafeteria_settings`): رزرو هر روز چند روز قبل و چه ساعتی باز و بسته می‌شه (پیش‌فرض ۶ روز قبل ۰۰:۰۰ و ۲ روز قبل ۲۳:۵۹، وقت تهران). کنار هر روز توی صفحه‌ی سلف یه نوار هست که با کم شدن وقت کوتاه می‌شه و رنگش سبز (بیشتر از دو سوم مونده)، زرد و قرمز (کمتر از یک سوم) می‌شه؛ قبل از باز شدن و بعد از بسته شدن خاکستری. شمارش زنده با `static/js/cafeteria.js` (ساعت سرور ملاکه). هر چهارشنبه ساعت ۸ صبح نوتیفیکیشن «یادت نره رزرو کنی» برای همه‌ی دانشجوها می‌ره که تا خونده نشه می‌مونه؛ اگه یادآوری قبلی هنوز خونده نشده، نوتیفیکیشن دوم ساخته نمی‌شه و همون میاد بالا. ادمین دکمه‌ی «الان بفرست» هم داره. اگه ادمین منوی روزی رو عوض کنه که رزروش باز شده و هنوز نگذشته، نوتیفیکیشن «منو عوض شد» (با تاریخ روزها، یکی‌شده تا وقتی خونده نشده) می‌ره. نوتیفیکیشن روزانه‌ی «رزرو باز شد» و برچسب کنار منو عمداً گذاشته نشدن (روزانه شلوغ می‌شدن). **(انجام و تست شد)**
- ۳-ث: اطلاعیه‌ی ادمین برای کل سایت (جدول‌های `site_notices` و `site_notice_dismissals`). صفحه‌ی `/notices` برای همه؛ ادمین فرم و دکمه‌ها رو توی همون صفحه می‌بینه (نوشتن، ویرایش، برداشتن از نوار و برگردوندن، حذف که فقط پنهان می‌کنه). جدیدترین اطلاعیه‌ای که «نمایش در نوار» داره، توی یه نوار زیر منو و توی همه‌ی صفحه‌هایی که از قالب مشترک استفاده می‌کنن نشون داده می‌شه. هر کاربر می‌تونه با × برای خودش ببندتش (توی دیتابیس ذخیره می‌شه، پس روی همه‌ی دستگاه‌ها بسته می‌مونه). نوار فقط با دست ادمین برداشته می‌شه، خودکار نه. نوتیفیکیشن برای همه‌ی کاربرها به جز نویسنده: «Site notice: عنوان»؛ ویرایش: «Site notice updated: عنوان» (یکی برای هر اطلاعیه)؛ حذف، نوتیفیکیشن‌های اون اطلاعیه رو پاک می‌کنه. **(انجام و تست شد)**
- ۳-ث۲: بعد از ویرایش اطلاعیه‌ی ادمین، برای همه (به جز ویرایش‌کننده) نوتیفیکیشن می‌ره، حتی کسایی که «جدید» رو هنوز نخوندن. دکمه‌ی «Notify everyone again» برای ادمین: برای همه یه نوتیفیکیشن خونده‌نشده بالای لیست می‌ذاره (اگه نخونده داشتن فقط میاد بالا) و اگه اطلاعیه توی نوار بالاست، نوار برای کسایی که بسته بودنش دوباره باز می‌شه (`POST /admin/notices/{id}/renotify`). **(انجام و تست شد)**
- ۳-ج: لینک گروه تلگرام برای هر ارائه (ستون اختیاری `course_offerings.telegram_link`، بدون ALTER). فقط استاد و ادمین توی صفحه‌ی درس می‌ذارن، عوض می‌کنن یا با خالی کردن پاک می‌کنن (فقط ترم فعال و ارائه‌ی لغونشده). فقط لینک‌هایی که با `https://t.me/` یا `https://telegram.me/` شروع می‌شن قبوله، حداکثر ۲۰۰. فقط کسایی که اطلاعیه‌ها رو می‌بینن (استاد، TAها، دانشجوهای فعال) لینک رو توی صفحه‌ی درس و صفحه‌ی اطلاعیه‌ها می‌بینن. لینک جدید یا عوض‌شده نوتیفیکیشن «Telegram group link updated in ...» برای دانشجوهای فعال می‌فرسته (یکی برای هر درس تا وقتی خونده نشده). **(انجام و تست شد)**
- ۳-چ: دسترسی‌های جدا برای هر TA. استاد (و ادمین) همه‌کاره‌ست؛ هر TA فقط کارهایی رو می‌کنه که توی دسترسی‌هاشه (enum `TaPermission`): POST_ANNOUNCEMENTS (نوشتن اطلاعیه و ویرایش و حذف مال خودش)، MANAGE_ANNOUNCEMENTS (ویرایش و حذف اطلاعیه‌های همه)، EDIT_TELEGRAM، VIEW_STUDENTS (دیدن لیست دانشجوها)، MANAGE_STUDENTS (لیست کلاس، حذف و برگردوندن دانشجو، کد ورود)، MANAGE_TEAM (اضافه و حذف TA)، GRANT_PERMISSIONS (دادن دسترسی به بقیه‌ی TAها). پیش‌فرض: POST_ANNOUNCEMENTS و EDIT_TELEGRAM. همه‌ی چک‌ها توی سرویس `OfferingAccess` ـه. بعد از قبول درخواست یا اضافه کردن دستی، استاد مستقیم به صفحه‌ی دسترسی‌های اون TA می‌ره (`/courses/{id}/tas/{userId}/permissions`) و بعداً هم از لینک «Permissions» کنار هر TA عوضش می‌کنه. TA با GRANT_PERMISSIONS فقط دسترسی‌هایی که خودش داره رو به TAهای دیگه می‌ده یا می‌گیره؛ دسترسی خودش و TAای که خودش GRANT_PERMISSIONS داره رو نمی‌تونه عوض کنه. TA با MANAGE_TEAM نمی‌تونه خودش یا TAای که MANAGE_TEAM داره رو حذف کنه. TAای که یه TA دیگه اضافه می‌کنه اول هیچ دسترسی‌ای نداره. Restore برای TA حذف شد (فقط دوباره اضافه می‌شه، با دسترسی‌های تازه) و «Leave the TA team» نداریم. TA اضافه‌شده و TAای که دسترسیش عوض شده نوتیفیکیشن می‌گیرن. TA توی صفحه‌ی درس می‌بینه چه دسترسی‌هایی داره. کسی که درخواستش قبول شده و بعد حذف شده، متن «accepted before, but you are no longer a TA» می‌بینه. **(انجام و تست شد)**
- ۳-خ: نوتیفیکیشن زنده با SSE. هر تب باز یه اتصال به `/notifications/stream` داره (`NotificationStream`، `NotificationStreamController`). هر وقت نوتیفیکیشنی ساخته، عوض یا پاک بشه (`NotificationLiveListener` روی `Notification`)، بعد از commit عدد خونده‌نشده‌ها به همه‌ی تب‌های اون کاربر فرستاده می‌شه و `static/js/live.js` لینک «Notifications (n)» رو بدون رفرش عوض می‌کنه (id `nav-notifications` و `data-label`، `data-stream` و `data-form-gone` توی `layout.html` نباید حذف بشن). هر ۲۵ ثانیه یه پیام خالی برای باز موندن اتصال. **(انجام و تست شد)**
- ۳-خ۲: صفحه‌های زنده. `LiveTopicListener` (توی `config`) بعد از هر commit روی جدول‌های اصلی به همه‌ی تب‌ها می‌گه کدوم «موضوع» عوض شد: `offering:<id>` (ارائه، ثبت‌نام، لیست کلاس، TA، درخواست TA، اطلاعیه)، `ta-requests`، `notices`، `banner`، `cafeteria`؛ و `notifications` فقط برای صاحبش. هر صفحه‌ی زنده روی `<main>` یه `data-live-topics` داره؛ `live.js` اگه موضوعش عوض شد `<main>` رو بی‌صدا دوباره می‌گیره و عوض می‌کنه، و چیزی که کاربر توی فرم‌ها نوشته یا انتخاب کرده، جای مکان‌نما و اسکرول رو نگه می‌داره (فقط اگه اون فرم دیگه نباشه یه پیام کوچیک میاد). نوار زرد بالا از `/live/banner` (تکه‌ی `banner` توی `layout.html`، با id `site-banner-slot`) دوباره گرفته می‌شه. `course-detail` به قالب مشترک منتقل شد. فقط اسم موضوع فرستاده می‌شه، هیچ داده‌ای؛ هر صفحه با دسترسی‌های خود کاربر دوباره ساخته می‌شه. **(انجام و تست شد)**
- ۳-خ۳ (روی همون شاخه‌ی ۳-خ۲): درخواست TA به استاد همون درس نوتیفیکیشن «New TA request in ...» با لینک `/ta-requests` می‌فرسته (یکی برای هر درس تا وقتی خونده نشده). چند گروه از یه درس توی یه ترم: دانشجوی هر گروه نمی‌تونه TA هیچ گروهی از همون درس بشه (درخواست، قبول، اضافه‌ی دستی، دعوت)، و TA هر گروه نمی‌تونه با کد یا لیست کلاس دانشجوی گروه دیگه بشه. TA بودن توی دو گروه مجازه. موارد قبلی خودکار حذف نمی‌شن. **(انجام و تست شد)**
- ۳-چ۲: دعوت به TA شدن (جدول `ta_invitations`، وضعیت‌ها PENDING، ACCEPTED، DECLINED، CANCELLED). «Invite as TA» توی صفحه‌ی درس (استاد یا TA با MANAGE_TEAM) فقط دعوت می‌سازه و همون لحظه قانون‌های TA شدن رو چک می‌کنه (و اینکه دعوت در انتظار نداشته باشه). دعوت‌شده نوتیفیکیشن «You were invited to be a TA of ...» می‌گیره و توی «My TA requests» قبول یا رد می‌کنه؛ موقع قبول قانون‌ها دوباره چک می‌شن. دعوت استاد با دسترسی‌های پیش‌فرض، دعوت TA بدون دسترسی. دعوت‌های در انتظار توی صفحه‌ی درس (برای استاد و TA با MANAGE_TEAM)؛ دعوت‌کننده یا استاد کنسل می‌کنن (نوتیفیکیشن خونده‌نشده‌ی دعوت پاک می‌شه). با قبول یا رد، دعوت‌کننده و اگه TA بوده استاد هم خبردار می‌شن. قبول درخواست خود دانشجو دعوت نمی‌خواد. وقتی کسی دانشجوی فعال هر گروهی از درس می‌شه (کد ورود، لیست کلاس، ثبت‌نام با شماره‌ی توی لیست، برگردوندن توسط استاد)، `TaService.onBecameStudent` دعوت‌های در انتظارش برای همه‌ی گروه‌های همون درس رو می‌بنده (CANCELLED، با نوتیفیکیشن «TA invitation closed» برای دعوت‌کننده و استاد) و درخواست‌های TA در انتظارش رو WITHDRAWN می‌کنه. اگه Accept به هر دلیلی نشه، دعوت بسته می‌شه و در انتظار نمی‌مونه. دعوتی که ۷ روز جواب نگیره منقضی می‌شه (CANCELLED، با نوتیفیکیشن «expired» برای دعوت‌کننده). **(انجام و تست شد)**
- بعداً: «درخواست به ادمین»: هر کاربر بتونه برای ادمین درخواست یا گزارش بفرسته (گزارش باگ، درخواست ویرایش اطلاعات یه استاد، هر درخواست دیگه)، با وضعیت (باز، در حال بررسی، انجام شد، رد شد) و پاسخ ادمین، و نوتیفیکیشن برای هر دو طرف.
- بعداً: یه پنل «My TA courses» برای TAها: لیست درس‌هایی که توشون TA هستن (ترم فعال، با لینک درس و برچسب Head TA و دسترسی‌هاشون)، توی منو برای کسایی که حداقل توی یه درس TA هستن.
- بعداً: TAای که استاد (یا Head TA) حذفش کرده، بتونه یه بار درخواست بازبینی (review) بده؛ استاد قبول کنه برمی‌گرده (با دسترسی‌های قبلی یا پیش‌فرض)، رد کنه دیگه نمی‌تونه برای همون درس درخواست بده.
- بعداً: پنجره‌ی کوچیک (toast) بالای صفحه برای نوتیفیکیشن تازه، همون لحظه که کاربر آنلاینه. سرور متن و لینک نوتیفیکیشن رو هم زنده می‌فرسته و یه اسکریپت چند ثانیه نشونش می‌ده؛ شکل و جاش کار فرانت‌کاره (کلاس CSS `toast`).
- ۳-ح: Head TA (می‌تونه چند تا باشه). Head TA یعنی TA با MANAGE_TEAM و GRANT_PERMISSIONS (`TaAssignment.isHead()`)، و این دو دسترسی رو فقط استاد می‌ده یا می‌گیره (`OfferingAccess.grantable`). دانشجوها برای Head TA درخواست می‌دن؛ قبول درخواست پیش‌فرض‌ها به‌علاوه‌ی این دو رو می‌ده؛ استاد هر چند نفر بخواد قبول می‌کنه و هر وقت کافی بود درخواست‌ها رو خودش می‌بنده (رد خودکار نداریم). استاد با «Make head TA» و «Remove head TA» کنار هر TA، Head رو تعیین می‌کنه. سقف تیم (`course_offerings.ta_team_limit`، اختیاری، ۱ تا ۵۰، نه کمتر از تیم فعلی): Headها وقتی «TAهای فعال + دعوت‌های در انتظار» به سقف رسید نمی‌تونن دعوت کنن؛ استاد محدود نیست. Headها همدیگه رو نمی‌تونن حذف یا تغییر بدن. برچسب «Head TA» توی صفحه‌ی درس و بخش اساتید. نوتیفیکیشن برای Head شدن و برداشته شدن. **(تست شد)** اصلاحات روی همون شاخه (همه تست و merge شدن): (۱) وقتی درخواست در انتظاری برای یه درس نمونده، نوتیفیکیشن «New TA request» استاد برای اون درس پاک می‌شه. (۲) تیم پر (سقف تعیین‌شده و TAهای فعال + دعوت‌های در انتظار بهش رسیده) یعنی **هیچ راهی** نیست: دانشجو درخواست نمی‌ده، استاد هم نه قبول می‌کنه نه دعوت، تا استاد سقف رو بالا ببره. (۳) درخواست افزایش سقف (جدول `ta_limit_requests`): Head TA عدد جدید و توضیح اختیاری می‌فرسته، فقط یه درخواست در انتظار برای هر درس، استاد Approve (سقف همون عدد می‌شه) یا Reject می‌کنه، و اگه استاد خودش سقف رو به اون عدد یا بیشتر برسونه خودکار تأیید می‌شه. (۴) انقضا (`TaHousekeeping`، هر شب ۰۰:۲۰): دعوت‌ها و درخواست‌های افزایش سقف بعد از ۷ روز بسته می‌شن؛ درخواست‌های Head TA دانشجوها منقضی نمی‌شن. (۶) سقف پیش‌فرض تیم برای هر ارائه‌ی جدید ۱ هست؛ استاد کم و زیادش می‌کنه یا Head TA درخواست افزایش می‌ده. (۵) شروع ترم جدید (`SemesterService`): درخواست‌های TA در انتظار ترم قبل WITHDRAWN، دعوت‌ها و درخواست‌های سقف بسته، و نوتیفیکیشن‌های خونده‌نشده‌ی مربوط پاک می‌شن؛ بقیه‌ی نوتیفیکیشن‌ها سابقه‌ان و می‌مونن. **(انجام و تست شد)**
- ۳-د: منابع آموزشی (جدول `course_resources`، صفحه‌ی `/courses/{id}/resources`، لینک «Resources (n)» توی صفحه‌ی درس برای همه). منابع به **ارائه** وصلن، پس هر ترم خالی شروع می‌شه. هر منبع: عنوان، لینک (فقط `https://`)، توضیح اختیاری، دسته‌ی اختیاری (اگه کسی دسته بنویسه، منابع گروه‌بندی می‌شن؛ وگرنه لیست ساده). «کی اضافه کرده» ذخیره و نشون داده نمی‌شه. نوع لینک از روی دامنه تشخیص داده می‌شه (`ResourceKind`: youtube، aparat، drive، telegram، github، pdf، link) و کنارش یه آیکون ساده با کلاس `resource-<نوع>` میاد. کی مدیریت می‌کنه: استاد و TA با دسترسی جدید MANAGE_RESOURCES (جزو پیش‌فرض‌ها؛ TAهایی که دسترسی‌شون قبلاً دستی ذخیره شده، باید استاد براشون تیک بزنه)، فقط ترم فعال؛ حذف فقط پنهان می‌کنه. پیشنهاد ترم قبل: مدیرها منابع **آخرین ترمی** که همین درس ارائه شده (همه‌ی گروه‌ها، بدون تکرار و بدون لینک‌هایی که الان هست) رو می‌بینن، انتخاب و اضافه می‌کنن یا «Hide suggestions» می‌زنن (`course_offerings.resource_suggestions_hidden`)؛ چیزی که انتخاب نشه، ترم بعد دیگه پیشنهاد نمی‌شه. نوتیفیکیشن «New resources in ...» برای دانشجوهای فعال (یکی برای هر درس تا وقتی خونده نشده). صفحه زنده‌ست. **(انجام و تست شد)**
- ۳-ذ: اطلاعات دفتر استاد (جدول `professor_profiles`، یه ردیف برای هر استاد). **همه‌ی فیلدها اختیاری‌ان**: ساختمان، اتاق، «Usually in the office» (متن آزاد؛ عمداً «ساعت حضور» قطعی نیست و زیرش نوشته می‌شه که ممکنه عوض بشه)، ایمیل، تلفن، آیدی تلگرام (با یا بدون @ یا لینک t.me)، صفحه‌ی وب (فقط https)، یادداشت. خود استاد یا ادمین از صفحه‌ی استاد توی بخش اساتید ویرایش می‌کنن (`/professors/{id}/office`). همه‌ی کاربرهای واردشده می‌بینن. تاریخ آخرین به‌روزرسانی نشون داده می‌شه و اگه توی ترم قبل ذخیره شده باشه برچسب «may be out of date» می‌گیره. **(انجام و تست شد)**
- ۳-ت (انجام‌شده داخل ۳-پ۲، این خط فقط برای سابقه‌ست): یادآوری هفتگی رزرو سلف به‌صورت نوتیفیکیشن برای همه‌ی دانشجوها (کار زمان‌بندی‌شده `@Scheduled`؛ گوشی‌ها فقط وقتی زنگ می‌خورن که بعداً ربات تلگرام، ایمیل یا PWA اضافه بشه).
- بعد: لینک تلگرام (برای هر ارائه)، ویدیوها و منابع آموزشی از یوتیوب و آپارات (به درس دائمی وصل: عنوان، فصل، لینک، توضیح، آپلودکننده)، اطلاعات دفتر استاد (ساختمان، اتاق، ساعت حضور، ایمیل دانشگاهی، راه‌های تماس).

**قدم ۴-الف (قبل از فاز ۴): حساب‌ها و شماره‌ی دانشجویی** (شاخه‌ی `step-4a-accounts`، **کد مستقیم توی پروژه نوشته شد، تست در انتظار**):
- فرمت شماره‌ی دانشجویی (`StudentNumberRules`، جدول `student_number_settings`): ۱۱ رقم که ۳ رقم اولش سال ورودیه (403 یعنی ۱۴۰۳). سال فعلی از ۳ رقم اول کد ترم فعال میاد (4052 → 405) و سال ورودی از ۱۰ سال قبل تا سال فعلی قبوله. ادمین تعداد رقم و تعداد سال‌های قبل رو توی پنل حساب‌ها عوض می‌کنه. اگه ترم فعالی نباشه فقط تعداد رقم چک می‌شه. رقم فارسی همه‌جا قبوله. این قانون توی ثبت‌نام، لیست کلاس (شماره‌ی نامعتبر توی «Not valid» شمرده می‌شه) و ساختن دانشجو توسط ادمین چک می‌شه. دعوت TA چک نمی‌کنه (اونجا فقط حساب‌های موجود پیدا می‌شن). حساب‌های قدیمی دست نمی‌خورن؛ پنل حساب‌ها فیلتر «شماره‌ی نامعتبر» داره.
- پنل حساب‌ها `/admin/accounts` (فقط ادمین): جستجو با شماره یا اسم. هیچ چیزی پاک نمی‌شه. مرحله‌ی ۱ **Disable**: برگشت‌پذیر با Enable؛ حساب نمی‌تونه وارد بشه و اگه الان واردشده باشه با کلیک بعدی بیرون می‌ره (`DisabledAccountFilter`)، صفحه‌ی ورود پیام «این حساب غیرفعال شده» نشون می‌ده. ادمین نمی‌تونه حساب خودش رو غیرفعال کنه. مرحله‌ی ۲ **Release number**: فقط برای حساب دانشجوی غیرفعال، با تایپ دوباره‌ی شماره؛ برگشت‌ناپذیر. شماره آزاد می‌شه (حساب می‌شه `released-<id>` و شماره‌ی قبلی توی `released_number` می‌مونه)، ثبت‌نام‌های فعال REMOVED، نقش‌های TA غیرفعال، و دعوت‌ها، درخواست‌های TA و درخواست‌های افزایش سقف در انتظارش بسته می‌شن. صاحب واقعی شماره بعدش ثبت‌نام می‌کنه و لیست‌های کلاس خودکار واردش می‌کنن.
- دکمه‌ی Delete از `/users` برداشته شد؛ به‌جاش لینک پنل حساب‌ها و کدهای دعوت استاد. ساختن کاربر توسط ادمین حالا چک می‌کنه (اسم، فرمت شماره برای دانشجو، نام کاربری برای استاد و ادمین، تکراری نبودن، رمز ۸ تا ۶۴).
- ثبت‌نام استاد با کد دعوت: ادمین توی `/admin/professor-invites` کد ۱۰ حرفی یه‌بارمصرف می‌سازه (اسم اختیاری، ۱۴ روز اعتبار)، لیست کدها رو با وضعیتشون می‌بینه و کد استفاده‌نشده رو لغو می‌کنه. استاد توی `/register/professor` کد، اسم کامل، نام کاربری و رمز می‌ده. نام کاربری ۴ تا ۳۰ حرف و رقم انگلیسی با حداقل یه حرف (پس هیچ‌وقت شبیه شماره‌ی دانشجویی نمی‌شه)، با حروف کوچک ذخیره می‌شه. لاگین همون `/login` ـه با لینک «Professor? Register with an invite code». ادمین هنوز می‌تونه از `/users` هم استاد بسازه.
- **امنیت:** قبلاً هیچ قانونی توی `SecurityConfig` برای `/admin/**` نبود و هر کاربر واردشده‌ای می‌تونست صفحه‌های ادمین (ترم، رشته، منوی سلف، اطلاعیه‌ی ادمین) رو باز کنه و فرمشون رو بفرسته. حالا `/admin/**` فقط برای ADMIN ـه.
- بعداً (بعد از فاز ۴): تأیید دانشجو با عکس کارت دانشجویی. فقط ادمین می‌بینه، توی جای عمومی ذخیره نمی‌شه و بعد از تصمیم پاک می‌شه؛ محدودیت‌های حساب تأییدنشده همون موقع تصمیم گرفته می‌شه. (تأیید با ایمیل دانشجویی کنار گذاشته شد چون بیشتر دانشجوها ندارن.)

**فاز ۴:** تمرین با سؤال‌ها و بارم، آپلود PDF توسط دانشجو (فقط تحویل خودش، حذف و تعویض قبل از مهلت)، ذخیره‌ی فایل روی دیسک با اسمی که سرور می‌سازه.

**فاز ۵ (هسته):** نمایش PDF داخل سایت، نمره و کامنت برای هر سؤال، جمع خودکار، Grade Sheet (ردیف‌ها از ثبت‌نام‌های فعال؛ ستون‌ها: نام، شماره‌ی دانشجویی، Q1 و ...، جمع، وضعیت اعتراض)، انتشار نمره (هر دانشجو فقط نمره‌ی خودش رو می‌بینه)، خروجی CSV (توی Google Sheets باز می‌شه). بعضی قسمت‌ها جاوااسکریپت لازم دارن.

**فاز ۶:** اعتراض، حداکثر یکی برای هر سؤال، پاسخ TA و تغییر نمره، وضعیت‌ها: باز، پاسخ داده‌شده، پذیرفته، رد، حل‌شده.

**فاز ۷:** بانک سؤال بر اساس درس، استاد، ترم و نوع آزمون، چند جواب برای هر سؤال، راه‌حل‌های بچه‌ها: اول جواب استاد («Official»)، بعد جواب‌هایی که استاد تأیید کرده، بعد بقیه بر اساس رأی (هر نفر برای هر جواب یه رأی مثبت یا منفی).

**فاز ۸:** نظرسنجی استاد و TA (امتیاز ساختاریافته و نظر اختیاری، نمایش ناشناس ولی هویت نویسنده داخلی نگه داشته می‌شه)، ارزیابی پایان ترم TA توسط استاد (شایسته / ناشایسته)، فیلتر کلمات بد، دکمه‌ی گزارش، بررسی ادمین.

**فاز ۹:** برنامه‌ریز درسی با برنامه‌ی A و B و C و تشخیص تداخل زمانی. یه جدول جدید برای جلسه‌های کلاس (روز، ساعت شروع و پایان، کلاس، شاید تاریخ امتحان) برای هر ارائه لازمه. اگه وقت شد: تخمین تقاضا و شباهت برنامه‌ها.

**هماهنگی تاریخ میان‌ترم (ایده‌ی صاحب پروژه، برای بعد از ثبت‌نام‌ها و برنامه‌ی کلاس‌ها، نزدیک فاز ۹):** وقتی چند استاد می‌خوان برای دانشجوهاشون میان‌ترم بذارن، سیستم اول حساب می‌کنه کدوم ارائه‌ها بیشترین دانشجوی مشترک رو دارن (از جدول ثبت‌نام‌های فعال). بعد استادهایی که بیشترین اشتراک رو دارن اول تاریخ میان‌ترم‌شون رو توی یه جدول مشترک ثبت می‌کنن، تا بقیه‌ی استادها اون جدول رو ببینن، تداخل نداشته باشن و بهترین زمان رو انتخاب کنن. به یه جدول تاریخ امتحان برای هر ارائه نیاز داره (همون جدولی که فاز ۹ برای جلسه‌های کلاس و تاریخ امتحان می‌خواد) و یه صفحه‌ی «تداخل» برای استاد: برای هر زمان پیشنهادی نشون می‌ده چند دانشجو دو امتحان توی همون روز یا همون ساعت پیدا می‌کنن.

**فاز ۱۰:** اگه وقت شد: استخدام TA (استاد درخواست می‌ده، دانشجو با رزومه اقدام می‌کنه؛ بخش کوچیک عمومی و بخش کامل که فقط خودش و استاد استخدام‌کننده می‌بینن) و پیشنهاد TA. بعد تست، بررسی امنیت، رفع باگ، دیپلوی (سرور، پورت‌های 80 و 443، رمزها توی متغیر محیطی، Flyway)، فایل exe ویندوز (پوسته‌ای که سرور رو باز می‌کنه، یا jpackage برای دمو)، بعداً PWA یا اندروید، فارسی و راست‌به‌چپ، مستندات و دموی نهایی.

**برنامه‌ی فرانت:** از همین الان قالب مشترک (منو، دکمه، جدول، فرم، پیام‌ها) و CSS روی صفحه‌های فعلی ساخته می‌شه و صفحه‌های هر فاز جدید هم قشنگ می‌شن. طراحی کامل بعد از فاز ۶. فارسی و راست‌به‌چپ آخر کار.

## ۹.۱. بخش هوش مصنوعی (ماهان)

کارها، همه با API یه مدل ارزون (Gemini) و نه ساخت مدل از پایه:
1. خلاصه کردن نظرها و یه توضیح کلی درباره‌ی استادها و TA ها (به فاز ۸، نظرسنجی، وصله).
2. تشخیص و پنهان کردن فحش بین کامنت‌ها (به فاز ۷ و ۸ وصله).
3. پرسش‌وپاسخ درسی (دستیار درسی با RAG): جواب سؤال‌های درسی دانشجوها از روی جزوه‌هایی که **از قبل توی دیتابیس هستن** (به‌صورت لینک). دانشجو جزوه آپلود نمی‌کنه. جستجو روی جزوه‌های **همون درس و همون استاد** (همه‌ی ترم‌ها) انجام می‌شه.
4. کوییز و فلش‌کارت از جزوه (پیشنهاد ماهان؛ جزو Must Haveهای تیم نیست، اولویتش با تیم): ساختن سؤال چندگزینه‌ای و فلش‌کارت (سؤال جلو، جواب پشت) از روی همون جزوه‌های دیتابیس.

پیاده‌سازی: یه سرویس جدای پایتون (FastAPI + uvicorn، `google-genai` برای Gemini، `pymupdf` برای خوندن متن PDF، `python-dotenv` برای کلید). مرورگر هیچ‌وقت مستقیم به این سرویس وصل نمی‌شه؛ فقط بک‌اند جاوا صداش می‌زنه. کلید Gemini فقط توی `.env` همین سرویسه (و `.env` توی Git نمی‌ره).

قانون‌ها:
- کلید API فقط توی متغیر محیطی سرور، نه توی کد و نه توی Git. فراخوانی API فقط از بک‌اند، هیچ‌وقت از مرورگر.
- یه رابط ساده (مثلاً `AiClient`) با یه پیاده‌سازی تقلبی برای تست، تا بقیه‌ی برنامه به یه سرویس خاص وابسته نباشه.
- فحش: اول فیلتر سریع با لیست کلمه‌ها (فارسی و انگلیسی، با یکسان‌سازی حرف‌ها و نیم‌فاصله)، بعد هوش مصنوعی فقط برای موارد مشکوک. کامنت نشونه‌دار پنهان می‌شه و به ادمین می‌ره، پاک نمی‌شه (متن اصلی می‌مونه).
- خلاصه: فقط وقتی نظر کافی هست، بدون اسم و هویت، ذخیره توی دیتابیس با زمان ساخت، با برچسب «ساخته‌شده با هوش مصنوعی»، و دوباره‌سازی زمان‌بندی‌شده (نه برای هر بازدید).
- دستیار درسی: با برچسب «ممکنه اشتباه کنه»، محدودیت تعداد پرسش برای هر کاربر، و بدون فرستادن اطلاعات شخصی به API. جواب فقط از روی جزوه‌های همون درس ساخته می‌شه و اگه جواب توی جزوه نبود، همینو می‌گه (نه حدس).
- کوییز و فلش‌کارت: با برچسب «ساخته‌شده با هوش مصنوعی»، محدودیت تعداد ساخت برای هر کاربر؛ کوییز و فلش‌کارتی که برای یه جزوه ساخته شد ذخیره بشه تا برای هر بار باز کردن دوباره ساخته نشه.
- هزینه و محدودیت تعداد درخواست رو کنترل کنید. شرایط استفاده و حریم خصوصی سرویس (به‌خصوص نسخه‌ی رایگان) قبل از فرستادن نظر دانشجوها بررسی بشه.

### ۹.۱.۱. وضعیت سرویس هوش مصنوعی (آخرین کار: ۱۳ آبان ۱۴۰۵)

**جزوه‌ها از کجا میان (تصمیم):** خود فایل PDF روی سرور نگه داشته نمی‌شه (هزینه). استاد یا TA لینک جزوه رو توی «منابع آموزشی» (۳-د، `course_resources`) می‌ذاره. سرویس AI فایل رو **یه بار** دانلود می‌کنه، می‌خونه و فقط متن و بردارهاش (embedding) رو نگه می‌داره؛ لینک اصلی برای دانشجو می‌مونه. فقط `https` و PDF، با سقف حجم. لینک گوگل درایو به لینک دانلود مستقیم تبدیل می‌شه؛ لینک تلگرام پشتیبانی نمی‌شه. اگه لینکی دانلود نشد، راه جایگزین: آپلود یه‌باره‌ی فایل که بعد از خوندن پاک می‌شه. **سمت جاوا (دوست بک‌اند) باید این رو تأیید کنه.**

**تصمیم‌های فنی:**
- مسیر: مرورگر ← Spring ← سرویس AI (پایتون). Spring هدر `X-Internal-Key` می‌فرسته.
- مدل‌ها توی `.env`: `GEMINI_MODEL=gemini-3.6-flash` (اصلی)، `GEMINI_FALLBACK_MODEL=gemini-3.8-flash` (وقتی اصلی شلوغه)، `EMBEDDING_MODEL=gemini-embedding-001` (بردار ۷۶۸تایی). `gemini-3.8-flash` توی تست‌ها مدام خطای ۵۰۳ (شلوغی سرور گوگل) می‌داد.
- خوندن PDF: خود Gemini صفحه‌ها رو می‌خونه (۵ صفحه در هر درخواست)، چون استخراج متن معمولی روی جزوه‌های فارسی خرابه («کاملاً» ← «کامالً») و صفحه‌های عکسی رو نمی‌بینه. نتیجه روی دیسک کش می‌شه (با هش فایل)، پس هر فایل فقط یه بار خونده می‌شه، و بعد از هر ۵ صفحه ذخیره می‌شه تا اگه وسط کار قطع شد از همون‌جا ادامه بده.
- تکه‌ها: هر صفحه یه تکه؛ صفحه‌ی بلندتر از ۱۲۰۰ کاراکتر به تکه‌های ۱۰۰۰ کاراکتری با ۱۵۰ هم‌پوشانی.
- ذخیره و جستجو: یه فایل SQLite (`data/rag.db`) و جستجوی شباهت با numpy (بدون ChromaDB و LangChain). ۵ تکه‌ی نزدیک‌تر با فیلتر درس و استاد.
- «پیدا نشد»: تکه‌های با شباهت کمتر از `0.55` کنار گذاشته می‌شن (سؤال بی‌ربط اصلاً به Gemini نمی‌ره)؛ بقیه رو Gemini بررسی می‌کنه و اگه جواب توی جزوه نبود `found=false` می‌ده.
- خطاها: ۳ بار تلاش (۵ و ۱۵ ثانیه فاصله)، سقف ۲ دقیقه برای هر درخواست، مدلی که شلوغ بوده تا ۱۰ دقیقه دوم امتحان می‌شه.

**تست‌شده روی جزوه‌ی نمونه (۳۳ صفحه، توابع بازگشتی، ساختمان، اشاره‌گرها):** ایندکس حدود ۵ دقیقه، ۳۶ تکه. متن فارسی درست، صفحه‌های فقط‌عکس (رابطه‌ی هانوی، نمودارهای حافظه‌ی اشاره‌گر) هم خونده شدن. سؤال «شرط پایه چیست» درست و با منبع (صفحه ۳ و ۴) جواب داده شد (شباهت ۰.۷۴). سؤال «مرتب‌سازی سریع» (توی جزوه نیست، ۰.۶۲) و «پایتخت فرانسه» (۰.۴۹) هر دو درست `found=false` گرفتن.

**ساختار فایل‌ها:** `app/config.py` (تنظیمات)، `app/llm.py` (تنها فایلی که با Gemini حرف می‌زنه)، `app/security.py` (چک کلید داخلی)، `app/main.py` (`/health`، `/docs`، `/debug/chat` موقت)، `app/rag/reader.py` (خوندن PDF)، `app/rag/ingest.py` (تکه کردن و ذخیره)، `app/rag/store.py` (SQLite و جستجو)، `app/rag/qa.py` (جواب دادن)، `tools/ingest_file.py` و `tools/ask.py` (تست از CMD بدون Spring).

**انتقال به ریپو (۱۸ آبان ۱۴۰۵):** کد از یه پوشه‌ی جدا به پوشه‌ی `ai-service/` توی ریپوی اصلی منتقل شد؛ از این به بعد فقط همون‌جا کار می‌شه و طبق روش Git بخش ۴.۱.

**قدم بعدی، به ترتیب:**
1. `step-ai-docker`: `Dockerfile` توی `ai-service` و سرویس `ai` توی `docker-compose.yml` (پورت 8000، خوندن `.env`، volume برای `data`)، و قفل کردن نسخه‌ی کتابخونه‌ها توی `requirements.txt`.
2. `step-ai-endpoints` (قدم ۴، طرحش آماده‌ست):
- `POST /materials/ingest` (لینک + `materialId`، `courseId`، `professorId`، `title`، `chapter`): جواب فوری `queued`، دانلود و ایندکس پشت صحنه (چون حدود ۵ دقیقه طول می‌کشه).
- `GET /materials/{id}`: وضعیت (توی صف، در حال خوندن، آماده، خطا با دلیل).
- `POST /materials/upload`: راه جایگزین با خود فایل PDF.
- `DELETE /materials/{id}`.
- `POST /ask` (`courseId`، `professorId`، `question`) ← `found`، `answer`، `sources` (عنوان، فصل، صفحه). اگه Gemini شلوغ بود، صفحه‌های مربوط به سؤال بدون جواب کامل برمی‌گرده تا دانشجو معطل نمونه.

**بعد از اون** (هر سه کار Must Have تا ۱۷ دی): سند قرارداد API برای سمت جاوا (`AiClient` با پیاده‌سازی تقلبی، همگام‌سازی لینک‌های منابع)، کوییز و فلش‌کارت، تشخیص فحش، خلاصه‌ی نظرها (منتظر جدول نظرها، فاز ۸)، اضافه کردن سرویس به `docker-compose.yml`.

## ۱۰. قوانین دوست فرانت‌کار (اول چت هوش مصنوعی بچسبونه)

این متن عمداً انگلیسی مونده، چون مستقیم به هوش مصنوعی داده می‌شه:

```
You are helping me style the frontend of a Spring Boot + Thymeleaf web app called TA Hub.
Rules you must follow:
1. I only edit files in src/main/resources/templates and src/main/resources/static.
   Never change Java files, pom.xml, application.properties or docker-compose.yml.
2. Keep every Thymeleaf attribute (th:text, th:each, th:if, th:href, th:action, th:field, th:object, th:value, th:selected, th:block). You may change tags, CSS classes, layout and structure around them.
3. Forms: always keep th:action (it adds the CSRF security token; a plain action="" breaks every form with a 403 error). Never rename the name="" of inputs or the form URLs.
4. Never rename or remove variables like ${offering}, ${isEnrolled}. If a page needs new data, tell me and I will ask the backend developer.
5. All visible text must come from messages.properties using #{key}. Add new keys at the END of that file. The text is English for now; Persian (right-to-left) will be added later with a second file, so do not hard-code text or left/right in CSS: use logical properties like margin-inline-start.
6. Do not use external CDNs. Put CSS/JS files in src/main/resources/static/ and link them with th:href="@{/css/app.css}".
7. The page must work on a phone screen too (responsive).
8. Give me the complete file each time, not fragments.
```

روی شاخه‌ی خودت کار کن و merge رو خودت انجام بده؛ هماهنگی کارهای فرانت حضوری با برنامه‌نویس بک‌انده. قبل از merge، `main` رو توی شاخه‌ات pull کن تا کانفلیکت کم بشه، و فقط فایل‌های `templates` و `static` (و کلیدهای جدید ته `messages.properties`) رو عوض کن.

## ۱۱. کارهای همیشگی و یادآوری‌ها

- روال Git (شاخه برای هر قدم، اصلاح روی همون شاخه، merge با `--no-ff`، برچسب آخر فاز) توی بخش ۴.۱ ـه.
- قبل از دیپلوی (سرویس هوش مصنوعی): `INTERNAL_API_KEY` یه مقدار طولانی و تصادفی باشه و نبودنش خطا بده (الان پیش‌فرض `change-me-dev-key` داره)؛ endpoint موقت `/debug/chat` حذف بشه؛ پورت 8000 فقط از داخل Docker در دسترس باشه، نه از بیرون.
- قبل از دیپلوی: اگه جلوی برنامه Nginx یا سرور مشابه هست، برای `/notifications/stream` گزینه‌ی buffering خاموش بشه (`proxy_buffering off`) تا نوتیفیکیشن زنده کار کنه؛ رمز دیتابیس و رمز ادمین پیش‌فرض به متغیر محیطی؛ خاموش کردن `tahub.demo-data` (حساب‌های نمونه رمز ضعیف و معروف دارن)؛ Flyway به‌جای `ddl-auto=update`؛ محدودیت تعداد حدس زدن کد ورود؛ حذف لاگ debug؛ صفحه‌های خطای مرتب (404، 403، 500).
- یه README برای هم‌تیمی‌های جدید (بخش «ابزارها و اجرا» شروع خوبیه).

## ۱۲. مشکلات شناخته‌شده و کارهای باز

- **پروتوتایپ رو باید از نو ساخت** (`docs/phase-03-prototype-api-tech-hosting/prototype/index.html`)، بعد از اینکه مهرسا فرانت مرحله‌ی اول رو زد و merge کرد: با همون منوی کناری، رنگ‌ها و فونتش، و با هر چهار مسیر اصلی (TA شدن، صفحه‌ی درس، تمرین با جدول نمره‌ی شبیه گوگل شیت و اعتراض، نظرسنجی و دستیار درسی)، هر کدوم با حالت خطا. نسخه‌ی فعلی فقط ورود، ورود به درس و مسیر تمرین رو داره.
- هنوز هر کسی می‌تونه با شماره‌ی دانشجویی کس دیگه (با فرمت درست) ثبت‌نام کنه. فعلاً راه‌حل اینه که صاحب واقعی به ادمین بگه و ادمین از پنل حساب‌ها اون حساب رو Disable و بعد Release number کنه. راه‌حل کامل بعداً: تأیید با عکس کارت دانشجویی (قدم ۴-الف).
- تا ادمین حداقل یه رشته اضافه نکنه، ثبت‌نام بسته‌ست.
- کاربرهایی که ادمین از `/users` می‌سازه رشته ندارن (از پروفایل انتخاب می‌کنن). حذف کاربر دیگه وجود نداره؛ به‌جاش Disable توی پنل حساب‌ها.
- صفحه‌های خطا همون Whitelabel پیش‌فرض‌ان. هشدار `spring.jpa.open-in-view` هنوز رسیدگی نشده.
- آخرین چیزی که صاحب پروژه تست کرده و تأیید شده: همه‌ی فاز ۲ و قدم‌های ۳-الف تا ۳-ب۴ (اطلاعیه‌ها، ویرایش و حذف، و نوتیفیکیشن‌ها). همه‌ی ۳-پ۱ (فهرست غذاها و منوی دو هفته) هم تست شده. ۳-پ۲ (زمان رزرو، نوار رنگی و یادآوری چهارشنبه) هم تست شده. ۳-ث (اطلاعیه‌ی ادمین و نوار بالا) هم تست شده. ۳-ث۲ (نوتیفیکیشن دوباره) و ۳-ج (لینک تلگرام) هم تست شدن. ۳-چ (دسترسی‌های TA) هم تست شده. ۳-خ (نوتیفیکیشن زنده) هم تست شده. ۳-خ۲ (صفحه‌های زنده) هم تست شده. ۳-خ۳ (نوتیفیکیشن درخواست TA و قانون گروه‌ها)، ۳-چ۲ (دعوت TA) و خود ۳-ح (Head TA) هم تست شدن. اصلاحات ۳-ح (تیم پر، درخواست افزایش سقف، انقضای ۷روزه، بستن کارهای ترم قبل، سقف پیش‌فرض ۱) هم تست شدن. ۳-د (منابع آموزشی) هم تست شده. ۳-ذ (اطلاعات دفتر استاد) هم تست شده و فاز ۳ تمومه. قدم ۴-الف (حساب‌ها، فرمت شماره و کد دعوت استاد) نوشته شده و تستش مونده. `course-detail` حالا از قالب مشترک استفاده می‌کنه. صفحه‌های قدیمی (`home`، `courses` و ...) هنوز به قالب مشترک و منو منتقل نشدن؛ این کار با دوست فرانت‌کاره.
- بخش هوش مصنوعی (ماهان): کد توی `ai-service/` ریپوئه. RAG (خوندن جزوه، ذخیره، جستجو و جواب) کار می‌کنه و تست شده؛ Docker، endpointها و اتصال به Spring مونده (بخش ۹.۱.۱). فیلتر فحش و خلاصه‌ی نظرها شروع نشدن (خلاصه منتظر جدول نظرهای فاز ۸ ـه). قبل از فرستادن نظر دانشجوها به یه سرویس بیرونی، شرایط استفاده و حریم خصوصی اون سرویس بررسی بشه.