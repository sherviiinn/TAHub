<div dir="rtl">

# فاز ۲: نمودارهای طراحی

**زمان در برنامه‌ی ترم:** ۱۶ تا ۲۰ آبان ۱۴۰۵
**خروجی این فاز:** نمودار مورد کاربرد (Use Case)، نمودار فعالیت (Activity)، نمودار معماری سیستم و نمودار داده‌ها (ERD). نمودارها با Mermaid نوشته شدن و GitHub خودش نشونشون می‌ده؛ نسخه‌ی عکس هر کدوم هم کنارش توی همین پوشه هست.

[بازگشت به فهرست مستندات](../README.md)

---

## نمودارها

نمودار مورد کاربرد فقط قابلیت‌های باید و بهتره‌ی MVP رو نشون می‌ده.

</div>

### Use Case
```mermaid
flowchart LR
    S((Student))
    T((TA))
    P((Professor))
    A((Admin))

    subgraph TAHub ["TA Hub (MVP)"]
        UC1([Register / Log in / Log out])
        UC2([Join a course with code])
        UC3([Read announcements, group link, resources])
        UC4([Apply to be a TA])
        UC5([Accept or decline TA invitation])
        UC6([Submit / replace own PDF])
        UC7([View own published grades])
        UC8([Object to a question grade])
        UC9([Rate and review professor / TA])
        UC10([Read reviews and AI summary])
        UC11([Ask the AI course assistant])
        UC12([Create offering and class list])
        UC13([Accept or reject TA requests])
        UC14([Invite TAs and set permissions])
        UC15([Post announcement, group link, resources])
        UC16([Create assignment with questions])
        UC17([Grade in the grade sheet])
        UC18([Publish grades])
        UC19([Answer objection])
        UC20([Evaluate TAs])
        UC21([Manage semesters, majors, accounts, invites])
        UC22([Review hidden comments])
    end

    S --- UC1
    S --- UC2
    S --- UC3
    S --- UC4
    S --- UC5
    S --- UC6
    S --- UC7
    S --- UC8
    S --- UC9
    S --- UC10
    S --- UC11

    T --- UC15
    T --- UC17
    T --- UC19

    P --- UC1
    P --- UC12
    P --- UC13
    P --- UC14
    P --- UC15
    P --- UC16
    P --- UC17
    P --- UC18
    P --- UC19
    P --- UC20

    A --- UC1
    A --- UC21
    A --- UC22
```

Image version: [use-case.png](use-case.png)

<div dir="rtl">

TA هم یه دانشجوئه، پس همه‌ی کارهای دانشجو رو هم می‌تونه بکنه. کارهای TA به دسترسی‌هایی بستگی داره که استاد بهش داده.

</div>

### Activity: submitting a PDF (with error paths)
```mermaid
flowchart TD
    Start([Student opens the assignment and chooses a PDF]) --> Login{Signed in?}
    Login -- no --> ToLogin[Redirect to login page]
    Login -- yes --> Enrolled{Active student of this course?}
    Enrolled -- no --> E403[Show 403 Forbidden]
    Enrolled -- yes --> Deadline{Before the deadline?}
    Deadline -- no --> Locked[Show error: submission is closed]
    Deadline -- yes --> Valid{Real PDF and under the size limit?}
    Valid -- no --> BadFile[Show error: only PDF files up to the limit]
    Valid -- yes --> Save[Save file with a server-made name; create or replace the student's single submission]
    Save --> Done[Show confirmation with time and file name]
    ToLogin --> End([End])
    E403 --> End
    Locked --> End
    BadFile --> End
    Done --> End
```

Image version: [activity-submit-pdf.png](activity-submit-pdf.png)

### System architecture
```mermaid
flowchart LR
    Browser["Browser<br/>HTML pages from Thymeleaf<br/>+ small JS (live updates, grade sheet)"]

    subgraph Server ["Spring Boot application (one process)"]
        Sec["Spring Security<br/>login, session, role rules, CSRF"]
        Ctl["Controllers<br/>request + input checks"]
        Svc["Services<br/>business and ownership rules"]
        Repo["Repositories<br/>Spring Data JPA / Hibernate"]
        Sec --> Ctl --> Svc --> Repo
    end

    DB[("PostgreSQL<br/>users, courses, enrollments, TAs,<br/>assignments, grades, reviews")]
    Files[("Disk storage<br/>submitted PDF files")]
    AI["External AI model API<br/>(profanity check, summaries,<br/>course assistant)"]

    Browser -- "HTTPS: GET pages / POST forms" --> Sec
    Server -- "Server-Sent Events: live updates" --> Browser
    Repo -- "JDBC (transactions)" --> DB
    Svc -- "read / write files" --> Files
    Svc -. "HTTPS + API key from env var (server only)" .-> AI
```

Image version: [architecture.png](architecture.png)

### ERD (main MVP tables)
```mermaid
erDiagram
    USERS ||--o{ COURSE_OFFERINGS : "teaches (professor)"
    COURSES ||--o{ COURSE_OFFERINGS : "offered as"
    SEMESTERS ||--o{ COURSE_OFFERINGS : "in"
    USERS ||--o{ ENROLLMENTS : "student in"
    COURSE_OFFERINGS ||--o{ ENROLLMENTS : "has"
    USERS ||--o{ TA_APPLICATIONS : "applies"
    COURSE_OFFERINGS ||--o{ TA_APPLICATIONS : "receives"
    USERS ||--o{ TA_ASSIGNMENTS : "TA in"
    COURSE_OFFERINGS ||--o{ TA_ASSIGNMENTS : "has TAs"
    COURSE_OFFERINGS ||--o{ ANNOUNCEMENTS : "has"
    COURSE_OFFERINGS ||--o{ COURSE_RESOURCES : "has"
    COURSE_OFFERINGS ||--o{ ASSIGNMENTS : "has (planned)"
    ASSIGNMENTS ||--|{ QUESTIONS : "has (planned)"
    ASSIGNMENTS ||--o{ SUBMISSIONS : "receives (planned)"
    USERS ||--o{ SUBMISSIONS : "submits (planned)"
    SUBMISSIONS ||--o{ QUESTION_GRADES : "graded per question (planned)"
    QUESTIONS ||--o{ QUESTION_GRADES : "for (planned)"
    QUESTION_GRADES ||--o| OBJECTIONS : "may have one (planned)"
    USERS ||--o{ REVIEWS : "writes / is reviewed (planned)"
    COURSE_OFFERINGS ||--o{ REVIEWS : "about (planned)"
    USERS ||--o| AI_SUMMARIES : "summary of reviews (planned)"

    USERS {
        bigint id PK
        string student_number UK "login name"
        string full_name
        string password_hash "BCrypt"
        string role "STUDENT, PROFESSOR, ADMIN"
        timestamp disabled_at "null = active"
    }
    SEMESTERS {
        bigint id PK
        string code UK "e.g. 4052"
        boolean active "only one active"
    }
    COURSES {
        bigint id PK
        string code UK
        string name
    }
    COURSE_OFFERINGS {
        bigint id PK
        bigint course_id FK
        bigint semester_id FK
        bigint professor_id FK
        string join_code "null = closed"
        string telegram_link
        boolean ta_applications_open
        int ta_team_limit
        boolean cancelled
    }
    ENROLLMENTS {
        bigint id PK
        bigint student_id FK
        bigint offering_id FK
        string status "ACTIVE, DROPPED, REMOVED"
        string source "ROSTER, JOIN_CODE"
    }
    TA_APPLICATIONS {
        bigint id PK
        bigint student_id FK
        bigint offering_id FK
        string message
        string status "PENDING, ACCEPTED, REJECTED, WITHDRAWN"
    }
    TA_ASSIGNMENTS {
        bigint id PK
        bigint user_id FK
        bigint offering_id FK
        boolean active
    }
    ANNOUNCEMENTS {
        bigint id PK
        bigint offering_id FK
        bigint author_id FK
        string title
        boolean deleted
    }
    COURSE_RESOURCES {
        bigint id PK
        bigint offering_id FK
        string title
        string url "https only"
        string category
    }
    ASSIGNMENTS {
        bigint id PK
        bigint offering_id FK
        string title
        timestamp deadline
        boolean published
        boolean grades_published
    }
    QUESTIONS {
        bigint id PK
        bigint assignment_id FK
        int number
        decimal max_points
    }
    SUBMISSIONS {
        bigint id PK
        bigint assignment_id FK
        bigint student_id FK
        string stored_file_name "made by server"
        string original_file_name
        timestamp submitted_at
    }
    QUESTION_GRADES {
        bigint id PK
        bigint submission_id FK
        bigint question_id FK
        decimal points "0..max_points"
        string comment
        bigint graded_by_id FK
    }
    OBJECTIONS {
        bigint id PK
        bigint question_grade_id FK
        string text
        string reply
        string status "OPEN, ANSWERED, ACCEPTED, REJECTED, RESOLVED"
    }
    REVIEWS {
        bigint id PK
        bigint author_id FK "hidden from readers"
        bigint target_user_id FK "professor or TA"
        bigint offering_id FK
        int rating
        string comment
        string moderation "VISIBLE, HIDDEN, WAITING_ADMIN"
    }
    AI_SUMMARIES {
        bigint id PK
        bigint target_user_id FK
        string text
        int review_count
        timestamp created_at
    }
```

Image version: [erd.png](erd.png)

<div dir="rtl">

جدول‌هایی که کنارشون «planned» نوشته شده مال مرحله‌های بعدی‌ان. بقیه الان توی دیتابیس هستن.

**قیدهای مهم:**
- **ثبت‌نام‌ها:** برای هر دانشجو و ارائه فقط یه ردیف. حذف یعنی عوض شدن وضعیت، نه پاک شدن.
- **درخواست‌های TA:** برای هر دانشجو و ارائه فقط یه درخواست.
- **TAها:** برای هر کاربر و ارائه یه ردیف. دانشجوی یه درس نمی‌تونه TA همون درس باشه (توی کد چک می‌شه).
- **ارائه‌ها:** یکتا روی درس، ترم و استاد.
- **تحویل‌ها:** هر دانشجو برای هر تمرین فقط یه تحویل.
- **نمره‌ها:** برای هر تحویل و سؤال یه نمره. هر نمره حداکثر یه اعتراض داره.
- **نظرها:** هر دانشجو برای هر استاد یا TA توی هر ارائه یه نظر.
- هیچ سابقه‌ای واقعاً پاک نمی‌شه؛ رکوردها وضعیت یا پرچم پنهان دارن.

جدول‌های دیگه‌ای هم هستن که بیرون از مسیرهای اصلی‌ان (نوتیفیکیشن، دعوت TA، سلف و ...). فهرست کاملشون توی `project-contex.md` (بخش ۵) هست.

</div>
