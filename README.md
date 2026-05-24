# 🚗 Materknhash ERP (Automotive Marketplace & Management System)

[**العربية 👇 (اضغط هنا للانتقال إلى القسم العربي)**](#توثيق-مشروع-materknhash-erp-بالعربية)

**Materknhash ERP** is a comprehensive automotive parts marketplace and enterprise resource planning desktop application built using **JavaFX**, **JDBC**, **HikariCP**, and **MySQL/TiDB**. 

The system serves as a digital marketplace connecting suppliers, sellers, service centers, and customers, moderated by a management/admin role to ensure high data quality and transaction security.

---

## 🌟 Key Features & Architecture
- **Multi-Tenant Roles**: Secure, distinct interfaces for **Admin**, **Employee**, **Seller**, **Customer**, and **Service Center**.
- **Asynchronous Execution (Multithreading)**: Heavy database operations and statistics run on background threads to ensure the JavaFX user interface remains smooth and lag-free.
- **Connection Pooling**: Backed by **HikariCP** to reuse JDBC connections and optimize network latency for remote databases.
- **Automated Bootstrap**: Automatically creates tables, default indexes, and seeds administrative accounts on first launch.
- **Bilingual Support (Arabic & English)**: Custom Unicode and character encoding configurations to natively support Arabic text.

---

## 🛠️ Requirements & Tech Stack
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Build Tool**: Maven 3.x.
- **Database**: MySQL 8.x, TiDB, or any MySQL-compatible server.

---

## ⚙️ Configuration (`application.properties`)

The application configures its database connection through `src/main/resources/application.properties`.

```properties
db.host=yamabiko.proxy.rlwy.net   # Server IP or Host name
db.port=49863                     # Port (default is 3306)
db.name=railway                   # Database schema name
db.user=root                      # Username
db.password=agWQNrqZixkenzDF...   # Password
db.tls=false                      # Set to true if TLS is mandated (e.g. TiDB Cloud)
```

### 1. Remote Mode (Railway Cloud Database)
By default, the application connects to a shared database hosted on **Railway.app**. 
- **Advantage**: Zero database setup is required on your machine. Anyone running the app links to the same database.
- **Disadvantage**: Since the cloud server is hosted overseas, actions might have slight latency depending on network conditions.

### 2. Local Mode (Local MySQL Server - Recommended for Dev)
To experience instant loads (0ms latency), run a local database:
1. Start your local MySQL database (e.g., via XAMPP, WampServer, or MySQL Installer).
2. Create a database schema (e.g., `CREATE DATABASE matraknhash;`).
3. Update `application.properties` with:
   ```properties
   db.host=localhost
   db.port=3306
   db.name=matraknhash
   db.user=your_mysql_username
   db.password=your_mysql_password
   db.tls=false
   ```
4. Run the app with the seeding flag enabled once to populate mock data:
   `mvn clean javafx:run -Dmatraknhash.seed=on`

---

## 🚀 How to Run and Package

### Run in Development Mode
Use Maven to compile and launch the JavaFX UI:
```bash
mvn clean javafx:run
```

### Package into a Standalone Executable (For Google Drive Submission)
To share the project with the professor or colleagues without sending raw code files, package it into a single shaded **"Fat JAR"** file containing all dependencies and JavaFX modules:

1. Run the packaging command:
   ```bash
   mvn clean package
   ```
2. Retrieve the executable JAR from the `target/` directory:
   - File: `target/matraknhash-erp-1.0.0.jar`
3. Upload this single `.jar` file to **Google Drive**.
4. **To run the packaged JAR**: The professor only needs to download the JAR and run:
   ```bash
   java -jar matraknhash-erp-1.0.0.jar
   ```

---

## 🔑 Default Accounts (For Testing)
On a fresh database, the system initializes the following default credentials:
- **Admin Account**: Username: `admin` | Password: `admin123`
- **Employee Account**: Username: `employee` | Password: `emp123`
- **Seller Account**: Username: `seller` | Password: `sell123`
- *Customers can sign up dynamically through the register page on startup.*

---

# توثيق مشروع Materknhash ERP (بالعربية)

مشروع **Materknhash** هو نظام ERP متكامل وسوق رقمي (Marketplace) مخصص لقطاع قطع غيار السيارات وخدمات الصيانة. النظام مبني باستخدام **JavaFX** للواجهات الرسومية، و**JDBC** للاتصال بقواعد البيانات مع تجميع اتصالات **HikariCP** لضمان الكفاءة والسرعة.

---

## 👥 الأدوار والصلاحيات
1. **المدير (ADMIN)**: رقابة الحسابات، مراجعة واعتماد البائعين الجدد، الموافقة النهائية على قطع الغيار، ومتابعة التقارير المالية.
2. **الموظف (EMPLOYEE)**: المراجعة الفنية لقطع الغيار المرفوعة من البائعين، عمليات الشراء والمشتريات الروتينية، وإدارة المخزون.
3. **البائع (SELLER)**: إضافة المنتجات وتعديل أسعارها وكمياتها، ومتابعة حالة الطلبات وتأكيد استلامها.
4. **العميل (CUSTOMER)**: تصفح الكتالوج، الشراء عبر سلة التسوق، حجز مواعيد الصيانة، وطلب إرجاع القطع.
5. **مركز الخدمة (SERVICE CENTER)**: إضافة العروض والخدمات الفنية واستقبال وتأكيد حجوزات العملاء.

---

## ⚙️ التهيأة والتشغيل المحلي

### 1. استخدام قاعدة البيانات السحابية (Railway)
افتراضياً، يتصل التطبيق بقاعدة بيانات مشتركة مرفوعة سلفاً على منصة **Railway**:
- يمكنك تشغيل التطبيق مباشرة دون الحاجة لتهيئة أي قاعدة بيانات على جهازك.
- أي بيانات تقوم بإدخالها ستظهر للدكتور أو لأي زميل يقوم بتشغيل البرنامج من جهازه.

### 2. استخدام قاعدة بيانات محلية (للحصول على سرعة فائقة)
إذا كنت ترغب في تشغيل التطبيق بأعلى سرعة وبدون أي تأخير في الاستجابة:
1. قم بتشغيل خادم MySQL المحلي لديك (مثل XAMPP).
2. قم بإنشاء قاعدة بيانات فارغة باسم `matraknhash`.
3. قم بتعديل ملف الإعدادات `src/main/resources/application.properties` كالتالي:
   ```properties
   db.host=localhost
   db.port=3306
   db.name=matraknhash
   db.user=اسم_المستخدم
   db.password=كلمة_المرور
   db.tls=false
   ```
4. قم بتشغيل الأمر التالي لمرة واحدة لبناء الجداول وملء قاعدة البيانات بالبيانات الافتراضية:
   `mvn clean javafx:run -Dmatraknhash.seed=on`

---

## 📦 كيفية تصدير البرنامج ومشاركته (Google Drive)
لتسليم المشروع للدكتور كملف واحد قابل للتشغيل مباشرة بدلاً من إرسال الكود البرمجي بالكامل:

1. قم بفتح سطر الأوامر في مجلد المشروع واكتب الأمر التالي:
   ```bash
   mvn clean package
   ```
2. بعد انتهاء العملية بنجاح، ستجد ملفاً جديداً باسم `matraknhash-erp-1.0.0.jar` داخل مجلد `target`.
3. قم برفع هذا الملف (`matraknhash-erp-1.0.0.jar`) فقط إلى **Google Drive** وشاركه مع الدكتور.
4. يستطيع الدكتور تشغيل البرنامج على جهازه فوراً عن طريق فتح سطر الأوامر وكتابة:
   ```bash
   java -jar matraknhash-erp-1.0.0.jar
   ```

---

## 🔐 الحسابات الافتراضية للتجربة
عند تهيئة التطبيق لأول مرة، يتم إنشاء الحسابات التالية تلقائياً:
- **حساب المدير**: اسم المستخدم: `admin` | كلمة المرور: `admin123`
- **حساب الموظف**: اسم المستخدم: `employee` | كلمة المرور: `emp123`
- **حساب البائع**: اسم المستخدم: `seller` | كلمة المرور: `sell123`
- *يمكن للعميل تسجيل حساب جديد مباشرة من شاشة تسجيل الدخول.*