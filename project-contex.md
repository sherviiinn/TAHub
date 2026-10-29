# TA Hub - زمینه‌ی پروژه

آخرین به‌روزرسانی: ۳ آبان ۱۴۰۵ (2026-10-25)

## این فایل رو چطور استفاده کنیم

- این فایل توی ریشه‌ی ریپو می‌مونه. آخر هر فاز، توی همون commit کد، به‌روزش کنید.
- اول هر چت جدید با Claude این فایل رو بچسبونید (یا توی یه Project توی حساب خودتون بذارید). بعد بگید کی هستید (بک‌اند، فرانت، دیتابیس و تست) و روی چی کار می‌کنید.
- هیچ رمز، کلید یا اطلاعات شخصی توی این فایل ننویسید. چند نفر اون رو توی چت هوش مصنوعی می‌ذارن.
- حافظه‌ی Claude مال حساب هر نفره و جای این فایل رو نمی‌گیره. منبع اصلی، Git و همین فایله.

## ۱. پروژه چیه

TA Hub یه پلتفرم دانشگاهی برای دانشجو، استاد، TA و ادمینه: درس‌ها، ثبت‌نام، تمرین، نمره‌دهی آنلاین روی PDF، جدول نمره (Grade Sheet)، اعتراض، بانک سؤال با راه‌حل‌های بچه‌ها، نظرسنجی استاد و TA، استخدام TA و برنامه‌ریز درسی. سند اصلی همون PDF به اسم «UniHub - Final Project Specification & 2-Month Roadmap» ـه. اسم TA Hub هنوز قطعی نیست.

- مهلت: دو ماه از شروع (اوایل مهر ۱۴۰۵).
- دانشگاه: نوشیروانی. به هیچ API دانشگاه وابسته نیستیم (توی spec هم ادغام با سیستم دانشگاه جزو «آینده» ـه).
- تقسیم کار: بک‌اند و دیتابیس (شروین)، فرانت (یکی از دوستان که با کمک Claude می‌سازه)، و بخش هوش مصنوعی (نفر سوم، بخش ۹.۱).
- MVP یعنی فاز ۰ تا ۶ (هسته‌ی دمو). بقیه «اگه وقت شد».

## ۲. ابزارها و اجرا

- Java 21، Spring Boot 4.1.1، Maven، Thymeleaf (صفحه‌ها سمت سرور ساخته می‌شن)، Spring Security، Spring Data JPA (Hibernate)، PostgreSQL 16 توی Docker.
- IntelliJ IDEA، ویندوز، Git و ریپوی Private روی GitHub.
- اجرای لوکال:
  1. نصب JDK 21، IntelliJ، Docker Desktop و Git.
  2. `git clone` و باز کردن پروژه توی IntelliJ.
  3. توی پوشه‌ی پروژه `docker compose up -d` (دیتابیس روی پورت 5433 سیستم، پورت 5432 داخل کانتینر).
  4. Run کردن `TaHubApplication`.
  5. باز کردن `http://localhost:8080` (با `http://`، نه `https://`).
- اگه پورتی روی سیستم شما مشغوله، فقط روی سیستم خودتون عوضش کنید و commit نکنید.
- مشکل دیتابیس یا تغییر ساختار: `docker compose down -v` و بعد `docker compose up -d` (همه‌ی داده‌ی لوکال پاک می‌شه).
- داده‌ی نمونه: با `tahub.demo-data=true` توی `application.properties`، دیتابیس **خالی** موقع اجرا پر می‌شه: دو ترم (4051 آرشیو، 4052 فعال)، دو استاد (شماره‌های 9001 و 9002)، شش دانشجو (40000001 تا 40000006)، سه رشته، سه درس، ارائه‌ها و ثبت‌نام‌ها با همه‌ی وضعیت‌ها. رمز حساب‌های نمونه اولین بار توی کنسول چاپ می‌شه. ادمین پیش‌فرض (شماره `admin`) رو `DataInitializer` می‌سازه. همه‌ی این‌ها فقط برای توسعه‌ان.

## ۳. ساختار کد

پکیج اصلی `ir.TAHub.TAHub`.

| پکیج | محتوا |
|---|---|
| ریشه | `TaHubApplication` و کنترلرها: `HomeController`، `LoginController`، `RegistrationController`، `UserController`، `CourseController`، `SemesterController`، `EnrollmentController`، `MyCoursesController`، `MajorController`، `ProfileController`، `RosterController` (لیست کلاس)، `TaController` (اضافه، حذف و برگردوندن TA)، `TaApplicationController` (درخواست TA شدن)، `ProfessorController` (بخش اساتید)، `TelegramLinkController` (لینک تلگرام درس)، `AnnouncementController` (اطلاعیه‌ها)، `NotificationController` (نوتیفیکیشن‌ها)، `CafeteriaController` (منوی سلف)، `FoodController` (فهرست غذاها)، `SiteNoticeController` (اطلاعیه‌ی ادمین)، `GlobalModelAttributes` (داده‌ی منو برای همه‌ی صفحه‌ها) |
| `model` | `User`، `Role` (STUDENT, PROFESSOR, ADMIN)، `Semester`، `Course`، `CourseOffering`، `Enrollment`، `EnrollmentStatus`، `EnrollmentSource` (ROSTER، JOIN_CODE)، `Major`، `RosterEntry`، `TaAssignment`، `TaPermission`، `TaPermissionConverter`، `TaApplication`، `TaApplicationStatus`، `Announcement`، `Notification`، `NotificationType` (ANNOUNCEMENT، GENERAL)، `Food`، `CafeteriaMeal`، `MealType` (BREAKFAST، LUNCH، DINNER)، `CafeteriaSettings`، `SiteNotice`، `SiteNoticeDismissal` |
| `repository` | برای هر جدول یه Repository (متدهایی که Spring از روی اسمشون کوئری می‌سازه) |
| `dto` | `RegisterForm`، `CourseForm` (فرم‌ها، تا کاربر نتونه فیلدهایی مثل نقش رو بفرسته) |
| `config` | `SecurityConfig`، `DataInitializer`، `DemoDataSeeder`، `SchedulingConfig` (روشن کردن کارهای زمان‌بندی‌شده با `@Scheduled`) |
| `security` | `CustomUserDetailsService` (لاگین با شماره‌ی دانشجویی) |
| `service` | `SemesterService` (شروع ترم جدید به‌صورت یکجا)، `RosterService` (خوندن لیست چسبونده‌شده و ثبت‌نام خودکار)، `NotificationService` (ساختن نوتیفیکیشن‌ها)، `TaService` (قانون‌های TA شدن، هم برای افزودن دستی و هم قبول درخواست)، `OfferingAccess` (کی توی یه ارائه چه کاری می‌تونه بکنه: استاد همه‌چیز، TA طبق دسترسی‌هاش)، `CafeteriaService` (هفته‌ی منو، ذخیره‌ی یکجای هفته، پاک‌سازی شبانه) |
| `util` | `JoinCodes` (ساخت کد ورود با SecureRandom)، `Digits` (تبدیل ارقام فارسی و عربی به انگلیسی) |

منابع: `templates/` (صفحه‌های Thymeleaf)، `static/` (CSS و JS، بعداً)، `messages.properties` (همه‌ی متن‌های صفحه)، `application.properties`.

## ۴. قراردادها (لطفاً رعایت کنید)

- کد، اسم‌ها و کامنت‌ها انگلیسی. هر متنی که کاربر می‌بینه توی `messages.properties` و با `#{key}` استفاده می‌شه. فارسی و راست‌به‌چپ آخر کار با `messages_fa.properties` اضافه می‌شه.
- چک فرم‌ها فقط **اولین خطا** رو به ترتیب فیلدها نشون می‌ده: متد `findFirstError(...)` کلید پیام رو برمی‌گردونه.
- هر کاری که داده رو عوض می‌کنه POST ـه. فرم‌ها همیشه `th:action` دارن (توکن امنیتی CSRF رو اضافه می‌کنه).
- مجوز دو لایه داره: قانون‌های آدرس توی `SecurityConfig`، و چک «مالکیت» توی کد (مثل `CourseOffering.isManagedBy(user)`)، چون قانون آدرس نمی‌دونه کی صاحب چیه.
- به id یا نقشی که از فرم میاد اعتماد نکنید: شیء واقعی رو لود کنید و چک کنید. همیشه روی کاربر واردشده کار کنید، نه کاربری که id ش توی درخواست اومده.
- سابقه هیچ‌وقت پاک نمی‌شه: ثبت‌نام‌ها وضعیت دارن، ارائه‌ها پرچم `cancelled` دارن، ترم‌های قدیمی فقط از لیست‌ها پنهان می‌شن.
- تغییر دیتابیس: فعلاً `ddl-auto=update`. اضافه کردن ستون اجباری (NOT NULL) به جدولی که ردیف داره موقع اجرا بی‌صدا شکست می‌خوره. یا ستون رو اختیاری بذارید، یا قبل از اجرا `ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...` بزنید، یا دیتابیس لوکال رو ریست کنید. commitهایی که ساختار رو عوض می‌کنن توی پیامشون `SCHEMA CHANGE` بنویسن.
- Git: برای هر قدم یه شاخه‌ی کوتاه با اسم `step-<فاز>-<اسم>`، merge با `git merge --no-ff` (`git config merge.ff false`)، پاک کردن شاخه بعد از merge، برچسب برای هر فاز تموم‌شده (`phase-2-done`)، فقط کد تست‌شده commit بشه، مرتب push. میان‌بر لاگ: `git config --global alias.lg "log --graph --oneline --decorate --all"`.

## ۵. ساختار دیتابیس (فعلی)

- `users`: id، full_name، student_number (یکتا، همون نام کاربری)، password_hash (BCrypt)، role، major_id (اختیاری، فقط دانشجوها).
- `majors`: id، name (یکتا). ادمین از داخل برنامه مدیریتش می‌کنه.
- `semesters`: id، code (یکتا، ۴ یا ۵ رقم مثل 4052)، active (هر لحظه فقط یکی فعاله).
- `courses`: id، code (یکتا، با حروف بزرگ)، name. دائمی و مستقل از ترم. ویدیوها و منابع به این وصل می‌شن.
- `course_offerings`: id، course_id، semester_id، professor_id، join_code (اختیاری؛ خالی یعنی ثبت‌نام بسته)، cancelled، ta_applications_open (پیش‌فرض false)، telegram_link (اختیاری). یکتا روی (درس، ترم، استاد)، پس دو استاد می‌تونن یه درس رو توی یه ترم ارائه بدن (دو گروه).
- `roster_entries`: id، offering_id، student_number، added_at. شماره‌های لیست کلاس که استاد چسبونده (حتی اگه هنوز حساب ندارن). یکتا روی (ارائه، شماره).
- `ta_assignments`: id، user_id، offering_id، active، assigned_at، permissions_set (اختیاری؛ خالی یعنی TA قدیمی که هنوز پیش‌فرض‌ها رو داره). یکتا روی (کاربر، ارائه).
- `ta_assignment_permissions`: ta_assignment_id، permission (متن ساده، بدون چک enum، تا اضافه کردن دسترسی جدید ALTER نخواد).
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
- TA: فقط کاربر با نقش دانشجو می‌تونه TA باشه. نمی‌شه توی ارائه‌ای که دانشجوی فعالشه TA باشه (و برعکس). حذف TA فقط `active` رو false می‌کنه (سابقه می‌مونه)؛ برگردوندن یعنی دوباره اضافه کردن، با دسترسی‌های تازه. کارهای هر TA با دسترسی‌هاش تعیین می‌شه (۳-چ). اسم TA ها رو همه می‌بینن، شماره‌ی دانشجویی و دکمه‌ها فقط مدیرهای ارائه.
- درخواست TA: پیش‌فرض بسته‌ست و استاد برای هر ارائه بازش می‌کنه. هر دانشجو برای هر ارائه یه درخواست داره (وضعیت‌ها PENDING، ACCEPTED، REJECTED، WITHDRAWN). رد شده با Reconsider برمی‌گرده. پس گرفته‌شده رو دانشجو می‌تونه دوباره بده. قبول کردن همون قانون‌های TA شدن رو دوباره چک می‌کنه.
- اطلاعیه: استاد (و ادمین) و TA های ارائه می‌نویسن. می‌خونن: استاد، TA ها و دانشجوهای فعال همون ارائه (بقیه ۴۰۳). استاد هر اطلاعیه‌ای رو پاک می‌کنه، TA فقط مال خودش رو. پاک‌کردن فقط پنهان می‌کنه و راه برگشت نداره. ارائه‌ی قدیمی یا لغوشده فقط خواندنیه. اطلاعیه‌های ۳ روز اول برچسب «New» دارن (`recent` توی `AnnouncementView`، کلاس CSS به اسم `tag-new`).
- نوتیفیکیشن: با هر اطلاعیه‌ی جدید، هر دانشجوی فعال ارائه (به جز نویسنده) یه نوتیفیکیشن می‌گیره. برای اینکه صفحه شلوغ نشه، هر دانشجو برای هر درس فقط **یه نوتیفیکیشن خونده‌نشده** داره و اطلاعیه‌ی بعدی شمارنده‌ش (`item_count`) رو زیاد می‌کنه و بالا میاردش. متن «New announcement in CS201 - Operating Systems» یا «2 new announcements in ...» و با کلیک می‌ره صفحه‌ی اطلاعیه‌های همون درس. هر کس فقط نوتیفیکیشن‌های خودش رو می‌بینه. ۵۰ تای آخر نشون داده می‌شن؛ پاکسازی نوتیفیکیشن‌های خیلی قدیمی بعداً.
- بخش اساتید: همه‌ی کاربرها لیست استادها و درس‌های ترم فعال هر استاد (بدون ارائه‌های لغوشده) و TA های هر درس رو می‌بینن.

## ۷. آدرس‌ها و قرارداد صفحه‌ها (برای فرانت)

این آدرس‌ها، اسم فیلدهای فرم و ویژگی‌های `th:` باید بمونن. اگه صفحه‌ای داده‌ی جدید لازم داره، به برنامه‌نویس بک‌اند بگید.

| قالب | آدرس | داده‌هایی که به صفحه می‌رسه | فرم‌ها (POST) |
|---|---|---|---|
| `home` | `GET /` | `appName`، `fullName`، `role`، `isAdmin` | `/logout` |
| `login` | `GET /login` (پارامترهای `error`، `logout`، `registered`) | ندارد | `/login`: `username` (شماره‌ی دانشجویی)، `password` |
| `register` | `GET /register` | `form` (`fullName`، `studentNumber`، `majorId`، `password`، `confirmPassword`)، `majors`، `errorKey` | `/register` |
| `users` (ادمین) | `GET /users` | `users` (id، fullName، studentNumber، role) | `/users`: `fullName`، `studentNumber`، `password`، `role`؛ `/users/{id}/delete` |
| `courses` | `GET /courses` | `semester`، `offerings`، `canCreate` | ندارد |
| `course-form` | `GET /courses/new` | `form` (`code`، `name`، `professorId`)، `isAdmin`، `professors`، `semester`، `errorKey` | `/courses` |
| `course-detail` | `GET /courses/{id}` (پارامترهای `joinError`، `cancelError`) | `offering`، `isCurrentSemester`، `isStudent`، `isEnrolled`، `wasRemoved`، `canManage`، `isCancelled`، `enrollmentOpen`، `joinCode` (فقط مدیرها)، `enrollments`، `removedEnrollments`، `pendingRoster`، `rosterResult` و `rosterErrorKey` (فقط بعد از چسبوندن لیست)، `teachingAssistants`، `removedTeachingAssistants`، `isTa`، `taErrorKey`، `taApplicationsOpen`، `myTaApplication`، `taApplyErrorKey`، `pendingTaRequests`، `canManageStudents`، `canSeeStudents`، `canManageTeam`، `canEditTelegram`، `myTaPermissions` (فقط برای TA)، `taPermissionsSavedFor`؛ `teachingAssistants` حالا ردیف‌هایی با `assignment`، `canRemove`، `canEditPermissions` ـه (و `removedTeachingAssistants` حذف شد)، `canSeeAnnouncements` (لینک اطلاعیه‌ها و لینک تلگرام فقط برای این‌ها)، `announcementCount`، `telegramSaved`، `telegramErrorKey`؛ لینک تلگرام: `offering.telegramLink` | `/courses/{id}/enroll` (`code`)، `/drop`، `/cancel`، `/uncancel`، `/join-code/regenerate`، `/join-code/close`، `/students/{sid}/remove`، `/students/{sid}/restore`، `/roster` (`numbers`)، `/tas` (`studentNumber`)، `/tas/{uid}/remove`، `/ta-applications` (`message`)، `/ta-applications/open`، `/ta-applications/close`، `/telegram` (`link`) |
| `my-courses` | `GET /my-courses?semester={id}` | `isProfessor`، `semesters`، `selectedSemesterId`، `selectedIsActive`، `offerings`، `activeSemester`، `reofferable` | `/courses/{id}/reoffer` |
| `semesters` (ادمین) | `GET /admin/semesters` | `semesters`، `errorKey` | `/admin/semesters`: `code` |
| `majors` (ادمین) | `GET /admin/majors` | `majors`، `usage` (تعداد دانشجوی هر رشته)، `errorKey` | `/admin/majors`: `name`؛ `/admin/majors/{id}/delete` |
| `ta-requests` (استاد، ادمین) | `GET /ta-requests` | `pending`، `rejected`، `requestErrorKey` | `/ta-applications/{id}/accept`، `/reject`، `/reconsider` |
| `my-ta-requests` (دانشجو) | `GET /my-ta-requests` | `applications` | `/ta-applications/{id}/withdraw` |
| `professors` | `GET /professors` | `professors` | ندارد |
| `professor-detail` | `GET /professors/{id}` | `professor`، `courses` (هر ردیف: `offering` و `teachingAssistants`) | ندارد |
| `announcements` | `GET /courses/{id}/announcements` | `offering`، `announcements` (هر ردیف: `announcement`، `when`، `editedWhen`، `recent`، `canDelete`، `canEdit`)، `isCurrentSemester`، `isCancelled`، `canPost`، `canManage`، `announcementErrorKey` | `/courses/{id}/announcements` (`title`، `body`)، `/announcements/{id}/delete` |
| `announcement-edit` | `GET /announcements/{id}/edit` | `announcement`، `offering`، `title`، `body`، `announcementErrorKey` | `/announcements/{id}/edit` (`title`، `body`) |
| `notifications` | `GET /notifications` | `notifications` (هر ردیف: `notification`، `when`)، `unreadCount` | `/notifications/{id}/open`، `/notifications/read-all` |
| `cafeteria` | `GET /cafeteria` (یا `?next=true` برای هفته‌ی بعد) | `days` (هر ردیف: `index`، `date`، `dayKey`، `today`، `meals` که با `meals.get(m)` خونده می‌شه و `option1` و `option2` داره)، `mealTypes`، `weekStart`، `weekEnd`، `isNextWeek`، `hasAnyMeal`، `reservationUrl`، `serverNowMillis`؛ هر روز یه `reservation` هم داره (`state`: BEFORE/OPEN/CLOSED، `opensMillis`، `closesMillis`، `percentLeft`، `color`: bar-green/bar-yellow/bar-red/bar-grey، `remaining`، `opensText`، `closesText`). اسکریپت `static/js/cafeteria.js` از `data-opens`، `data-closes` و `#reservation-labels` استفاده می‌کنه و این‌ها نباید حذف بشن | ندارد |
| `cafeteria-edit` (ادمین) | `GET /admin/cafeteria` (یا `?next=true`) | همون داده‌های `cafeteria` (به جز `reservationUrl`)، به‌علاوه‌ی `activeFoods`، `slots` (۱ و ۲)، `cafeteriaSaved`، `cafeteriaErrorKey` | `/admin/cafeteria?next=...`: لیست‌ها با اسم `meal_<روز از ۰ برای شنبه>_<BREAKFAST/LUNCH/DINNER>_<1 یا 2>` و مقدار id غذا (خالی یعنی بدون غذا) |
| `cafeteria-settings` (ادمین) | `GET /admin/cafeteria/settings` | `openDaysBefore`، `openTime`، `closeDaysBefore`، `closeTime`، `settingsSaved`، `settingsErrorKey`، `reminderSentTo` | `/admin/cafeteria/settings` (همون چهار فیلد)، `/admin/cafeteria/reminder` (فرستادن یادآوری همین الان) |
| `foods` (ادمین) | `GET /admin/foods` | `foods`، `foodErrorKey` | `/admin/foods` (`name`)، `/admin/foods/{id}/rename` (`name`)، `/admin/foods/{id}/deactivate`، `/admin/foods/{id}/activate` |
| `ta-permissions` (استاد یا TA با GRANT_PERMISSIONS) | `GET /courses/{id}/tas/{userId}/permissions` | `offering`، `target`، `rows` (هر ردیف: `permission`، `checked`، `editable`)، `isManager` | `/courses/{id}/tas/{userId}/permissions`: چک‌باکس‌های `permission` (مقدار: اسم دسترسی) |
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
- فایل `static/css/app.css` مال فرانت‌کاره. از ویژگی‌های منطقی (`margin-inline-start` و ...) استفاده کنید تا فارسی و راست‌به‌چپ بعداً خراب نشه.

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

## ۹. نقشه‌ی راه

**فاز ۲ (درس‌ها): تموم شد** (برچسب `phase-2-done`). کارهای کوچیکی که از این فاز عقب افتاده و موقع مناسب انجام می‌شن: وضعیت حساب به‌جای حذف، کد دعوت استاد، جلوگیری از ثبت‌نام با شماره‌ی دانشجویی دیگران. جزئیات قدم‌های انجام‌شده:
- ۲-ز: استاد لیست شماره‌های دانشجویی کلاس رو می‌چسبونه. کسایی که حساب دارن خودکار وارد درس می‌شن، بقیه یادداشت می‌شن و موقع ثبت‌نام خودکار وارد می‌شن. هر ثبت‌نام یه «منبع» (لیست یا کد) داره. دانشجوی حذف‌شده توسط استاد برنمی‌گرده. **(انجام و تست شد)**
- ۲-ح: TA بودن توی ارائه (جدول `ta_assignments`، یکتا روی کاربر و ارائه؛ حذف TA فقط `active` رو false می‌کنه)، نمایش TA ها توی صفحه‌ی ارائه. **(انجام و تست شد؛ حذف TA برگشت‌پذیره)**
- ۲-ح۲: درخواست TA شدن: استاد برای ارائه‌اش «پذیرش درخواست TA» رو باز یا بسته می‌کنه. دانشجو درخواست (با پیام کوتاه) می‌ده، استاد توی یه پنل «درخواست‌های TA» قبول یا رد می‌کنه، و قبول کردن خودکار اون دانشجو رو TA می‌کنه (جدول `ta_applications`، وضعیت‌ها PENDING، ACCEPTED، REJECTED، WITHDRAWN). بعداً توی فاز ۱۰ رزومه و نمره‌ی درس به همین درخواست وصل می‌شه. ردکردن برگشت‌پذیره (دکمه‌ی Reconsider)؛ درخواستی که دانشجو خودش پس گرفته (WITHDRAWN) رو می‌تونه دوباره بده. **(انجام و تست شد)**
- ۲-ط: بخش اساتید: لیست استادها، با کلیک روی استاد درس‌های ترم فعالش و TA هاش، با کلیک روی درس صفحه‌ی ارائه. بعداً اطلاعات دفتر و نظرسنجی هم اینجا میاد. **(انجام و تست شد)**
- قالب مشترک، منوی بالا و CSS پایه (بخش ۷.۱). **(انجام و تست شد؛ بقیه‌ی صفحه‌ها رو دوست فرانت‌کار به قالب می‌بره)**
- بعد از فاز ۲: وضعیت حساب (غیرفعال کردن به‌جای حذف)، کد دعوت استاد (قابل عقب انداختن).

**فاز ۳ (در حال انجام):**
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
- ۳-چ: دسترسی‌های جدا برای هر TA. استاد (و ادمین) همه‌کاره‌ست؛ هر TA فقط کارهایی رو می‌کنه که توی دسترسی‌هاشه (enum `TaPermission`): POST_ANNOUNCEMENTS (نوشتن اطلاعیه و ویرایش و حذف مال خودش)، MANAGE_ANNOUNCEMENTS (ویرایش و حذف اطلاعیه‌های همه)، EDIT_TELEGRAM، VIEW_STUDENTS (دیدن لیست دانشجوها)، MANAGE_STUDENTS (لیست کلاس، حذف و برگردوندن دانشجو، کد ورود)، MANAGE_TEAM (اضافه و حذف TA)، GRANT_PERMISSIONS (دادن دسترسی به بقیه‌ی TAها). پیش‌فرض: POST_ANNOUNCEMENTS و EDIT_TELEGRAM. همه‌ی چک‌ها توی سرویس `OfferingAccess` ـه. بعد از قبول درخواست یا اضافه کردن دستی، استاد مستقیم به صفحه‌ی دسترسی‌های اون TA می‌ره (`/courses/{id}/tas/{userId}/permissions`) و بعداً هم از لینک «Permissions» کنار هر TA عوضش می‌کنه. TA با GRANT_PERMISSIONS فقط دسترسی‌هایی که خودش داره رو به TAهای دیگه می‌ده یا می‌گیره؛ دسترسی خودش و TAای که خودش GRANT_PERMISSIONS داره رو نمی‌تونه عوض کنه. TA با MANAGE_TEAM نمی‌تونه خودش یا TAای که MANAGE_TEAM داره رو حذف کنه. TAای که یه TA دیگه اضافه می‌کنه اول هیچ دسترسی‌ای نداره. Restore برای TA حذف شد (فقط دوباره اضافه می‌شه، با دسترسی‌های تازه) و «Leave the TA team» نداریم. TA اضافه‌شده و TAای که دسترسیش عوض شده نوتیفیکیشن می‌گیرن. TA توی صفحه‌ی درس می‌بینه چه دسترسی‌هایی داره. کسی که درخواستش قبول شده و بعد حذف شده، متن «accepted before, but you are no longer a TA» می‌بینه. **(کد داده شد، تست در انتظار)**
- ۳-ح (بعدی): Head TA روی همین سیستم. دانشجوها فقط برای Head TA درخواست می‌دن. استاد موقع قبول یه سقف تیم تعیین می‌کنه (شامل خود Head؛ بعداً قابل تغییر ولی نه کمتر از تعداد فعلی). بعد از قبول، درخواست‌ها بسته و درخواست‌های در انتظار بقیه خودکار رد می‌شن (با Reconsider برمی‌گردن). Head TA یعنی TA با MANAGE_TEAM و GRANT_PERMISSIONS (پیش‌فرض موقع قبول). Head TA تیمش رو مستقیم با شماره‌ی دانشجویی تا سقف اضافه می‌کنه (اضافه‌ی دستی استاد به سقف حساب نمی‌شه) و فقط Head TA یا استاد حذف می‌کنن. استاد با «Make head TA» یکی رو Head می‌کنه. استاد با اضافه و حذف تیم توسط Head TA خبردار می‌شه. برچسب «Head TA» توی صفحه‌ی درس.
- ۳-ت (انجام‌شده داخل ۳-پ۲، این خط فقط برای سابقه‌ست): یادآوری هفتگی رزرو سلف به‌صورت نوتیفیکیشن برای همه‌ی دانشجوها (کار زمان‌بندی‌شده `@Scheduled`؛ گوشی‌ها فقط وقتی زنگ می‌خورن که بعداً ربات تلگرام، ایمیل یا PWA اضافه بشه).
- بعد: لینک تلگرام (برای هر ارائه)، ویدیوها و منابع آموزشی از یوتیوب و آپارات (به درس دائمی وصل: عنوان، فصل، لینک، توضیح، آپلودکننده)، اطلاعات دفتر استاد (ساختمان، اتاق، ساعت حضور، ایمیل دانشگاهی، راه‌های تماس).

**فاز ۴:** تمرین با سؤال‌ها و بارم، آپلود PDF توسط دانشجو (فقط تحویل خودش، حذف و تعویض قبل از مهلت)، ذخیره‌ی فایل روی دیسک با اسمی که سرور می‌سازه.

**فاز ۵ (هسته):** نمایش PDF داخل سایت، نمره و کامنت برای هر سؤال، جمع خودکار، Grade Sheet (ردیف‌ها از ثبت‌نام‌های فعال؛ ستون‌ها: نام، شماره‌ی دانشجویی، Q1 و ...، جمع، وضعیت اعتراض)، انتشار نمره (هر دانشجو فقط نمره‌ی خودش رو می‌بینه)، خروجی CSV (توی Google Sheets باز می‌شه). بعضی قسمت‌ها جاوااسکریپت لازم دارن.

**فاز ۶:** اعتراض، حداکثر یکی برای هر سؤال، پاسخ TA و تغییر نمره، وضعیت‌ها: باز، پاسخ داده‌شده، پذیرفته، رد، حل‌شده.

**فاز ۷:** بانک سؤال بر اساس درس، استاد، ترم و نوع آزمون، چند جواب برای هر سؤال، راه‌حل‌های بچه‌ها: اول جواب استاد («Official»)، بعد جواب‌هایی که استاد تأیید کرده، بعد بقیه بر اساس رأی (هر نفر برای هر جواب یه رأی مثبت یا منفی).

**فاز ۸:** نظرسنجی استاد و TA (امتیاز ساختاریافته و نظر اختیاری، نمایش ناشناس ولی هویت نویسنده داخلی نگه داشته می‌شه)، ارزیابی پایان ترم TA توسط استاد (شایسته / ناشایسته)، فیلتر کلمات بد، دکمه‌ی گزارش، بررسی ادمین.

**فاز ۹:** برنامه‌ریز درسی با برنامه‌ی A و B و C و تشخیص تداخل زمانی. یه جدول جدید برای جلسه‌های کلاس (روز، ساعت شروع و پایان، کلاس، شاید تاریخ امتحان) برای هر ارائه لازمه. اگه وقت شد: تخمین تقاضا و شباهت برنامه‌ها.

**هماهنگی تاریخ میان‌ترم (ایده‌ی صاحب پروژه، برای بعد از ثبت‌نام‌ها و برنامه‌ی کلاس‌ها، نزدیک فاز ۹):** وقتی چند استاد می‌خوان برای دانشجوهاشون میان‌ترم بذارن، سیستم اول حساب می‌کنه کدوم ارائه‌ها بیشترین دانشجوی مشترک رو دارن (از جدول ثبت‌نام‌های فعال). بعد استادهایی که بیشترین اشتراک رو دارن اول تاریخ میان‌ترم‌شون رو توی یه جدول مشترک ثبت می‌کنن، تا بقیه‌ی استادها اون جدول رو ببینن، تداخل نداشته باشن و بهترین زمان رو انتخاب کنن. به یه جدول تاریخ امتحان برای هر ارائه نیاز داره (همون جدولی که فاز ۹ برای جلسه‌های کلاس و تاریخ امتحان می‌خواد) و یه صفحه‌ی «تداخل» برای استاد: برای هر زمان پیشنهادی نشون می‌ده چند دانشجو دو امتحان توی همون روز یا همون ساعت پیدا می‌کنن.

**فاز ۱۰:** اگه وقت شد: استخدام TA (استاد درخواست می‌ده، دانشجو با رزومه اقدام می‌کنه؛ بخش کوچیک عمومی و بخش کامل که فقط خودش و استاد استخدام‌کننده می‌بینن) و پیشنهاد TA. بعد تست، بررسی امنیت، رفع باگ، دیپلوی (سرور، پورت‌های 80 و 443، رمزها توی متغیر محیطی، Flyway)، فایل exe ویندوز (پوسته‌ای که سرور رو باز می‌کنه، یا jpackage برای دمو)، بعداً PWA یا اندروید، فارسی و راست‌به‌چپ، مستندات و دموی نهایی.

**برنامه‌ی فرانت:** از همین الان قالب مشترک (منو، دکمه، جدول، فرم، پیام‌ها) و CSS روی صفحه‌های فعلی ساخته می‌شه و صفحه‌های هر فاز جدید هم قشنگ می‌شن. طراحی کامل بعد از فاز ۶. فارسی و راست‌به‌چپ آخر کار.

## ۹.۱. بخش هوش مصنوعی (نفر سوم تیم)

سه کار، همه با API یه مدل ارزون (مثل Gemini) و نه ساخت مدل از پایه:
1. خلاصه کردن نظرها و یه توضیح کلی درباره‌ی استادها و TA ها (به فاز ۸، نظرسنجی، وصله).
2. تشخیص و پنهان کردن فحش بین کامنت‌ها (به فاز ۷ و ۸ وصله).
3. دستیار کمکی برای جواب دادن به سؤال‌های درسی دانشجوها.

قانون‌ها:
- کلید API فقط توی متغیر محیطی سرور، نه توی کد و نه توی Git. فراخوانی API فقط از بک‌اند، هیچ‌وقت از مرورگر.
- یه رابط ساده (مثلاً `AiClient`) با یه پیاده‌سازی تقلبی برای تست، تا بقیه‌ی برنامه به یه سرویس خاص وابسته نباشه.
- فحش: اول فیلتر سریع با لیست کلمه‌ها (فارسی و انگلیسی، با یکسان‌سازی حرف‌ها و نیم‌فاصله)، بعد هوش مصنوعی فقط برای موارد مشکوک. کامنت نشونه‌دار پنهان می‌شه و به ادمین می‌ره، پاک نمی‌شه (متن اصلی می‌مونه).
- خلاصه: فقط وقتی نظر کافی هست، بدون اسم و هویت، ذخیره توی دیتابیس با زمان ساخت، با برچسب «ساخته‌شده با هوش مصنوعی»، و دوباره‌سازی زمان‌بندی‌شده (نه برای هر بازدید).
- دستیار درسی: با برچسب «ممکنه اشتباه کنه»، محدودیت تعداد پرسش برای هر کاربر، و بدون فرستادن اطلاعات شخصی به API.
- هزینه و محدودیت تعداد درخواست رو کنترل کنید. شرایط استفاده و حریم خصوصی سرویس (به‌خصوص نسخه‌ی رایگان) قبل از فرستادن نظر دانشجوها بررسی بشه.

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

روی یه شاخه مثل `frontend/design` کار کن، Pull Request باز کن و مستقیم روی `main` push نکن. برنامه‌نویس بک‌اند هر PR رو بررسی می‌کنه: فقط فایل‌های `templates` و `static` (و کلیدهای جدید ته `messages.properties`) باید عوض شده باشن.

## ۱۱. کارهای همیشگی و یادآوری‌ها

- فقط کد تست‌شده commit بشه. برای قدم‌های ریسک‌دار یا تغییر دیتابیس شاخه‌ی جدا. برچسب برای هر فاز تموم‌شده. مرتب push. ریپوی GitHub همیشه Private.
- قبل از دیپلوی: رمز دیتابیس و رمز ادمین پیش‌فرض به متغیر محیطی؛ خاموش کردن `tahub.demo-data` (حساب‌های نمونه رمز ضعیف و معروف دارن)؛ Flyway به‌جای `ddl-auto=update`؛ محدودیت تعداد حدس زدن کد ورود؛ حذف لاگ debug؛ صفحه‌های خطای مرتب (404، 403، 500).
- یه README برای هم‌تیمی‌های جدید (بخش «ابزارها و اجرا» شروع خوبیه).

## ۱۲. مشکلات شناخته‌شده و کارهای باز

- هر کسی می‌تونه با هر شماره‌ی دانشجویی ثبت‌نام کنه، پس ممکنه شماره‌ی کسی رو کس دیگه بگیره. بعداً باید درست حل بشه (تطبیق با لیست رسمی کلاس، تأیید ایمیل یا کد فعال‌سازی).
- تا ادمین حداقل یه رشته اضافه نکنه، ثبت‌نام بسته‌ست.
- کاربرهایی که ادمین از `/users` می‌سازه رشته ندارن (از پروفایل انتخاب می‌کنن). حذف کاربری که سابقه داره به‌خاطر کلیدهای خارجی خطا می‌ده (راه‌حل: وضعیت حساب).
- صفحه‌های خطا همون Whitelabel پیش‌فرض‌ان. هشدار `spring.jpa.open-in-view` هنوز رسیدگی نشده.
- آخرین چیزی که صاحب پروژه تست کرده و تأیید شده: همه‌ی فاز ۲ و قدم‌های ۳-الف تا ۳-ب۴ (اطلاعیه‌ها، ویرایش و حذف، و نوتیفیکیشن‌ها). همه‌ی ۳-پ۱ (فهرست غذاها و منوی دو هفته) هم تست شده. ۳-پ۲ (زمان رزرو، نوار رنگی و یادآوری چهارشنبه) هم تست شده. ۳-ث (اطلاعیه‌ی ادمین و نوار بالا) هم تست شده. ۳-ث۲ (نوتیفیکیشن دوباره) و ۳-ج (لینک تلگرام) هم تست شدن. قدم ۳-چ (دسترسی‌های TA) تحویل داده شده و تستش مونده. صفحه‌های قدیمی (`home`، `courses`، `course-detail` و ...) هنوز به قالب مشترک و منو منتقل نشدن؛ این کار با دوست فرانت‌کاره.
- بخش هوش مصنوعی (نفر سوم) باید با API ارزون یه مدل (مثل Gemini) کار کنه؛ قبل از فرستادن نظر دانشجوها به یه سرویس بیرونی شرایط استفاده و حریم خصوصی اون سرویس بررسی بشه.