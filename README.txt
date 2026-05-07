==============================================================
  Metrkansh ERP  -  Auto Parts Management System
  Advanced Programming Applications  -  Week 12 Project
==============================================================

GROUP MEMBERS (sorted alphabetically)
----------------------------------------
  1. Ahmed Sayed            -  241009160
  2. Khaled Ahmed           -  241008653
  3. Mohamed Ahmed Ismael   -  241008562
  4. Mohamed Hamdey Besher  -  241008660
  5. Mohamed Soliman        -  241009247
  6. Taha Yasser            -  241009362   (6oy.taha@gmail.com)

BUSINESS DOMAIN
----------------------------------------
Auto-parts retail / warehouse management ("Metrkansh").
A multi-role desktop ERP that lets a shop manage car spare-parts
inventory, suppliers, point-of-sale, and reporting.

ROLES
----------------------------------------
  - Admin     : full access (users, parts, suppliers, reports)
  - Employee  : parts + suppliers CRUD, reports view
  - Seller    : POS / cashier screen only

TECH STACK
----------------------------------------
  - Java 17
  - JavaFX 21 (Presentation layer)
  - SQLite + JDBC (Data access + DB layer)
  - FXGL 17 (live sales chart)
  - Maven build
  - BCrypt for password hashing

ARCHITECTURE (4 layers)
----------------------------------------
  Presentation (ui/)
        |
  Business Logic (service/)
        |
  Data Access (dao/, JDBC)
        |
  Database (SQLite, database/matraknhash.db)

HOW TO RUN
----------------------------------------
Requirements:
  - JDK 17 on PATH
  - Maven 3.9+

1) From the project root:
       mvn clean javafx:run

   The first run will auto-create database/matraknhash.db and
   seed it from database/schema.sql + database/seed.sql.

2) Default accounts (seeded):
       admin    / admin123
       employee / emp123
       seller   / sell123

DATABASE SETUP
----------------------------------------
No external DB server is required. SQLite file is created
automatically on first launch under: database/matraknhash.db
To reset: delete that file and re-run the app.

SOCKET PORT
----------------------------------------
  Server listens on TCP port 5555 (localhost).
  Cashier client connects to localhost:5555 to push invoices.
  Port can be overridden with -Dmatraknhash.port=XXXX.

PROJECT STRUCTURE
----------------------------------------
  src/main/java/com/matraknhash/
        app/         entry point
        ui/          JavaFX controllers
        service/     business logic
        dao/         JDBC DAOs (generic BaseDao<T>)
        model/       domain models
        net/         socket server & client
        thread/      background tasks (stock monitor)
        chart/       FXGL live chart
        db/          ConnectionFactory + bootstrap
        util/        generic utilities (Result<T>, EventBus<T>)
  src/main/resources/fxml      FXML views
  src/main/resources/css       stylesheets
  database/                    schema.sql, seed.sql, *.db
  uml/                         class diagram (PNG + source)
  report/                      development report (PDF)
  video/                       YouTube link / mp4
  lib/                         (optional external jars)

NOTES
----------------------------------------
  - Threads, sockets, generics, FXGL chart, OOP and JDBC are
    all real, integrated, and use live database data.
  - No web, no REST APIs - desktop only as required.
