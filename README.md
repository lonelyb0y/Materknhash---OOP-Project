#  Materknhash ERP (متكرنهاش)
### *Next-Generation Automotive Marketplace & Enterprise Resource Planning*

Welcome to **Materknhash ERP** (متكرنهاش), a comprehensive multi-tenant automotive parts marketplace and enterprise resource planning desktop platform. Built with **JavaFX**, **JDBC**, **HikariCP**, and **MySQL/TiDB**, this application bridges the gap between suppliers, sellers, service centers, and customers under secure administrative supervision.

---

##  The Startup Story
**Materknhash** was born out of a real-world tech startup initiative. As founders of this venture, we recognized a major bottleneck in the local automotive parts supply chain: the lack of robust, localized digital inventory management and marketplace coordination. 

While bootstrapping this startup, we needed a powerful desktop application project for our academic curriculum. We decided to bridge both worlds by developing this production-grade desktop ERP platform. 

To prove its viability, **we have successfully contracted with an automotive parts merchant** who agreed to run this system as a pilot in their actual retail operations. If the system succeeds, we plan to scale it across additional dealers; if not, it remains a robust, real-world educational prototype of our startup vision!

---

##  Project Team & Contributors
This project is owned and executed by our startup founders:
* **Founder & Owner**: Mohamed Ahmed Ismael
* **Co-Founder & Lead Engineer**: Mohamed Hamdy
* **Co-Founder & Database Specialist**: Mohamed Suliman
* **Developer**: Khaled Ahmed
* **Developer**: Ahmed Sayed

---

##  Key Features
- **Multi-Tenant Roles**: Distinct secure interfaces for **Admin**, **Employee**, **Seller**, **Customer**, and **Service Center**.
- **Asynchronous Execution (Multithreading)**: High-latency network calls and heavy database calculations run on background threads, keeping the JavaFX GUI fully responsive and buttery smooth.
- **Dynamic Shipping & Order Tracking **:
  - Customers input their **Shipping Address** at checkout.
  - Sellers process orders by assigning a **Courier Name** and **Tracking Number**, changing status to `SHIPPED`.
  - Customers receive **live delivery updates** with courier and tracking details directly on their tracking panel.
  - Admins inspect shipment metadata before final delivery approval and automated stock updates.
- **Out of Stock Catalog Visibility **: Out-of-stock items (quantity = 0) are kept visible in the catalog with a distinct red "Out of Stock" badge and disabled buy button, helping buyers see what parts are carried.
- **Bilingual Core Support**: Advanced database character configurations fully support Arabic metadata, product descriptions, and filenames.
- **Robust Foreign Key Constraint Safety**: Gracefully handles deletion failures for parts with active sales history. Instead of silent failures, it prompts managers with helpful instructions to set stock to 0 or hide the listing.
- **Socket-Based Invoicing Server**: Features a multi-client TCP network socket server (`net` module) to process real-time sales transactions securely.

---

##  Project Architecture & Directory Structure
The codebase follows a modular MVC and Service-oriented architecture:

```
Materknhash/
├── database/
│   ├── schema.sql           # Database schema & idempotent alter statements
│   └── seed.sql             # Default demo mock data (users, parts, history)
├── src/
│   ├── main/
│   │   ├── java/com/matraknhash/
│   │   │   ├── app/         # App context manager, Hikari connection factory, and user session
│   │   │   ├── chart/       # SalesChartFXGL chart modules for dashboard graphics
│   │   │   ├── dao/         # Data Access Objects (CRUD, Custom SQL queries)
│   │   │   ├── db/          # Database Bootstrap migration system
│   │   │   ├── model/       # Core domain entities (User, Part, Supplier, Sale, etc.)
│   │   │   ├── net/         # Socket server/client invoice network implementation
│   │   │   ├── service/     # Core business logic layer (Auth, Part, Listing, Purchase, Sale)
│   │   │   ├── thread/      # Thread pool executors for asynchronous tasks
│   │   │   ├── tools/       # Diagnostic tools and database helpers
│   │   │   ├── ui/          # JavaFX View FXML controllers
│   │   │   └── util/        # Utility helpers (ImageUploader, BCrypt hashing)
│   │   └── resources/
│   │       ├── fxml/        # FXML layouts (Catalog, Cart, SellerOrders, MyListings, etc.)
│   │       ├── css/         # Global stylesheets and theme styling
│   │       └── application.properties # Database connection settings
│   └── test/
│       └── java/com/matraknhash/
│           └── IntegrationSmokeTest.java # Comprehensive E2E socket & db tests
└── pom.xml                  # Maven dependencies, plugins, and shades package config
```

---

##  Default Accounts (For Testing & Review)
To help the professor/doctor test all workflows, the database bootstraps with these default accounts:

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | Finalizes orders, reviews financial dashboards, adds suppliers. |
| **Employee** | `employee` | `emp123` | Approves new merchant listings to go live instantly. |
| **Seller (Merchant)** | `seller` | `sell123` | Publishes listings, views incoming orders, inputs courier tracking codes. |
| **Customer** | `taha` | `1234` | Browses catalog, places orders, enters delivery addresses, tracks orders. |

*Note: New customers can also register dynamically directly from the sign-up view.*

---

##  Configuration & Databases

Database configurations are loaded from `src/main/resources/application.properties`.

```properties
db.host=yamabiko.proxy.rlwy.net   # Cloud Host
db.port=49863                     # Cloud Port
db.name=railway                   # Cloud Database
db.user=root                      # Username
db.password=agWQNrqZixken...      # Password
db.tls=false
```

### 1. Remote Mode (Railway Cloud Database - Default)
Out of the box, the app connects to our shared **Railway.app** MySQL cloud database.
- **Advantage**: Zero database setup is required. The Doctor/Professor can run the app immediately and see live, synchronized records from our tests.

### 2. Local Mode (Local MySQL - Recommended for Development)
To run the app with 0ms network latency:
1. Start your local MySQL server (via XAMPP, WAMP, or MySQL Workbench).
2. Create a schema: `CREATE DATABASE matraknhash;`.
3. Configure `application.properties` with your credentials (`db.host=localhost`, etc.).
4. Run the app with the seeding flag enabled once to populate default suppliers/history:
   ```bash
   mvn clean javafx:run -Dmatraknhash.seed=on
   ```

---

##  How to Run & Package

### Compile and Launch in Development Mode
```bash
mvn clean javafx:run
```

### Package into a Shaded Standalone JAR (For Submission)
To compile and package the entire desktop application into a single executable Fat JAR containing all compiled classes, FXML, assets, and dependency libraries:

1. Execute the packaging command:
   ```bash
   mvn clean package
   ```
2. Retrieve the standalone jar:
   - Location: `target/matraknhash-erp-1.0.0.jar`
3. **Running the Packaged JAR**:
   ```bash
   java -jar target/matraknhash-erp-1.0.0.jar
   ```

---

##  Future Roadmap & Features
- **Integrated Courier APIs**: Automated shipping label generation and real-time transit tracking APIs (e.g., Aramex, DHL).
- **Merchant Wallet & Payout Gateway**: Multi-currency escrow system and automated payment splits between marketplace operator and merchants.
- **Mobile Companion Apps**: Lightweight iOS & Android apps for customers to browse catalog and for couriers to mark packages as delivered.
- **Smart Predictive Inventory**: Machine learning algorithms predicting auto parts seasonal demands and low-stock alerts.
