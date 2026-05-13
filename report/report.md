# Metrkansh ERP — Development Report

**Course:** Advanced Programming Applications — Week 12 Project
**Submission date:** May 2026

| # | Name | Student ID |
|---|------|-----------|
| 1 | Ahmed Sayed | 241009160 |
| 2 | Khaled Ahmed | 241008653 |
| 3 | Mohamed Ahmed Ismael | 241008562 |
| 4 | Mohamed Hamdey Besher | 241008660 |
| 5 | Mohamed Soliman | 241009247 |
| 6 | Taha Yasser | 241009362 |

> **Group ID:** *(insert before submission)*

---

## 1. Problem statement

Local auto-parts shops in Egypt are still run from paper notebooks or Excel files. There is no shared, role-aware tooling that lets a small team of (a) an admin, (b) a back-office employee, and (c) a cashier work on the same inventory in real time. We were asked to design a desktop ERP that demonstrates **OOP, JDBC, multithreading, sockets, generics, and JavaFX** working together on a realistic business case.

**Metrkansh** ("شركة مطركنش") is the fictional shop. The application:

- Stores spare parts (SKU, car make/model, prices, stock).
- Manages suppliers and tracks purchase orders.
- Provides a cashier ("seller") POS that creates sales atomically and decrements stock.
- Runs a background stock-monitor thread that pushes low-stock alerts to the UI through an event bus.
- Exposes a TCP socket so any cashier station on the LAN — including a future mobile or kiosk — can submit invoices to the same backend.
- Hosts the database remotely so every team member, on any PC, sees the same data immediately.

## 2. Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     Presentation (JavaFX / FXML)                 │
│   LoginController · MainShellController · DashboardController    │
│   PartsController · SuppliersController · PurchasesController    │
│   SalesController · ReportsController · UsersController          │
│   SettingsController                                             │
└────────────────────────────┬─────────────────────────────────────┘
                             │ depends on
┌────────────────────────────▼─────────────────────────────────────┐
│                        Service layer                             │
│   AuthService · PartService · SupplierService · UserService      │
│   SaleService · PurchaseService                                  │
└────────────────────────────┬─────────────────────────────────────┘
                             │ uses
┌────────────────────────────▼─────────────────────────────────────┐
│                    DAO layer (generic JDBC)                      │
│        BaseDao<T> ── UserDao, PartDao, SupplierDao               │
│        SaleDao, PurchaseDao  (own multi-table transactions)      │
└────────────────────────────┬─────────────────────────────────────┘
                             │ JDBC
┌────────────────────────────▼─────────────────────────────────────┐
│       MySQL 8.0 on Railway.app  (utf8mb4, InnoDB, FK on)         │
└──────────────────────────────────────────────────────────────────┘
```

Cross-cutting components:

- **`db.ConnectionFactory`** — singleton, reads `application.properties`, exposes a shared `Connection` plus a `serverOnly()` connection used once at bootstrap to `CREATE DATABASE IF NOT EXISTS`.
- **`db.DatabaseBootstrap`** — applies `schema.sql`, seeds default users with bcrypt hashes, applies `seed.sql`. Idempotent (re-runs do nothing).
- **`net.InvoiceServer` / `InvoiceClient`** — a Java-Object TCP protocol; the POS sends an `InvoiceMessage(INVOICE_REQUEST, sale, …)`, the server persists it through `SaleService` and replies with `INVOICE_ACK` or `INVOICE_ERROR`.
- **`thread.StockMonitorTask`** — daemon thread, polls `findLowStock()` every 30 s and publishes a `LowStockEvent` on the typed `EventBus<E>`. The dashboard subscribes.
- **`util.Result<T>` / `util.EventBus<E>`** — small generics utilities used by services and the threading layer.

## 3. OOP principles applied

### 3.1 Inheritance — `User` hierarchy
`User` is `abstract` with three concrete subclasses (`Admin`, `Employee`, `Seller`). Each overrides `permissions()` to return a comma-separated list of permitted screens.

```java
public abstract class User {
    public abstract String permissions();
    public static User from(int id, String u, String h, String n, Role r, boolean a) {
        return switch (r) {
            case ADMIN    -> new Admin(...);
            case EMPLOYEE -> new Employee(...);
            case SELLER   -> new Seller(...);
        };
    }
}
```
The static factory `User.from(...)` is the **single point of polymorphism**: the `UserDao.extract` method only knows about `User`, never about the concrete subclasses.

### 3.2 Encapsulation
Domain objects (`Part`, `Sale`, `Purchase`, …) keep all fields `private` and expose getters/setters. Mutating operations that must remain consistent are methods, not raw setters — e.g. `Sale.addItem(SaleItem)` updates `total` automatically:

```java
public void addItem(SaleItem it) { items.add(it); total += it.getSubtotal(); }
```

### 3.3 Polymorphism
- `User.permissions()` — runtime dispatch by role.
- `BaseDao<T>` — every concrete DAO supplies `table()`, `columns()`, `extract()`, `bindInsert()`, `bindUpdate()`. The generic CRUD methods (`findAll`, `insert`, `update`, `delete`) call those abstract methods polymorphically.
- `Runnable` is implemented by `StockMonitorTask` so it can be handed to a `Thread`.

### 3.4 Abstraction
`BaseDao<T>` is the textbook abstraction example: callers see `findAll() : List<T>` and don't care that internally it builds an SQL string from `columns()` and dispatches `ResultSet -> T` via `extract`.

### 3.5 Composition over inheritance
Services do not extend their DAOs; they own them as `final` fields. Same for `AppContext`, `InvoiceServer`, `StockMonitorTask`. This keeps each unit testable in isolation (the `IntegrationSmokeTest` constructs a fresh `InvoiceServer` on a different port without touching the desktop one).

### 3.6 Generics
- `BaseDao<T>` — generic CRUD across all entity types.
- `util.Result<T>` — a minimal "either" type used by `AuthService.login(...)`. Replaces throwing exceptions for predictable failures (wrong password, locked account).
- `util.EventBus<E>` — typed pub/sub used by the stock monitor.

## 4. Threading model

Two threads run alongside the JavaFX application thread:

| Thread | Started by | What it does | Communication back to UI |
|---|---|---|---|
| `invoice-accept` (daemon) | `InvoiceServer.start()` | `ServerSocket.accept()` loop, dispatches each connection to a `CachedThreadPool` (`workers`). | Replies on the same socket; no UI coupling. |
| `stock-monitor` (daemon) | `Main` after the login screen opens | Sleeps 30 s, queries `PartDao.findLowStock()`, if non-empty publishes a `LowStockEvent` on the `EventBus`. | Subscribers (e.g. dashboard) re-route to the FX thread via `Platform.runLater`. |

Concurrency safety:

- `ConnectionFactory.get()` is `synchronized` — the JDBC `Connection` is shared but each statement is short-lived.
- Sale creation in `SaleDao` runs inside an explicit transaction (`setAutoCommit(false)` → multiple statements → `commit()` / `rollback()`). The `UPDATE parts SET quantity = quantity - ? WHERE id = ? AND quantity >= ?` uses MySQL's row lock to make stock decrement atomic; a concurrent over-sell returns `0` rows updated and raises a friendly `DaoException`.

## 5. Sockets

`InvoiceServer` listens on TCP **5555** by default (overridable with `-Dmatraknhash.port=NNNN`). The wire format is Java serialization of `InvoiceMessage`, an enum-tagged envelope:

```java
public enum Type { INVOICE_REQUEST, INVOICE_ACK, INVOICE_ERROR, STOCK_UPDATE }
```

The cashier UI (`SalesController`) builds a `Sale`, hands it to an `InvoiceClient`, and the same JVM's `InvoiceServer` persists it. This decoupling means a future thin-client cashier on a Raspberry Pi could connect to the same server with no code changes.

## 6. Database & data layer

Originally we used SQLite for zero-setup, but mid-project we migrated the data to **MySQL hosted on Railway.app** so every teammate's PC sees the same shared inventory. `application.properties` carries the credentials; `application.properties.example` is committed and the real one is gitignored.

Schema highlights (`database/schema.sql`):

- 7 tables, all `InnoDB` + `utf8mb4`.
- Foreign keys with `ON DELETE CASCADE` for items and `ON DELETE SET NULL` for `parts.supplier_id`.
- `users.role` constrained by `CHECK (role IN ('ADMIN','EMPLOYEE','SELLER'))`.
- Composite indexes on `parts(sku)`, `sales(created_at)`, `sale_items(sale_id)`.

`database/seed.sql` populates 10 suppliers, 30 parts, 30 days of historical sales (so the dashboard chart looks alive in screenshots), and 4 sample purchases.

## 7. Testing

`src/test/java/com/matraknhash/IntegrationSmokeTest.java` is an end-to-end harness against the **live Railway database**. It boots the server on a non-clashing port and verifies:

1. Schema and seed are present.
2. `PartService` round-trips an insert + delete.
3. A sale submitted **over TCP** decrements stock atomically.
4. A purchase increments stock.
5. All Reports queries (`dailyTotals`, `topSelling`, `totalSalesLast30Days`, `totalProfitLast30Days`, `totalPurchasesAllTime`) execute on MySQL.
6. `findLowStock` query is well-formed.
7. `AuthService` accepts the three default users and rejects bad credentials.

Run it with: `mvn test -Dtest=IntegrationSmokeTest`. Latest run: **7 / 7 passing**.

## 8. AI tools usage

We used GitHub Copilot and Cascade (Windsurf IDE) during development. Specifically:

- **Boilerplate generation** — Cascade scaffolded the FXML controllers and the `BaseDao<T>` after we wrote a one-paragraph prompt describing the layered architecture.
- **Bug fixing** — when migrating from SQLite to MySQL we hit several "X NOT NULL but the INSERT didn't supply X" bugs (`sale_items.subtotal`, `purchases.supplier_name`, `purchase_items.subtotal`). Cascade pin-pointed them from the JDBC stack trace and patched only the offending DAOs without touching unrelated code.
- **Test writing** — the integration smoke test was AI-drafted from the architecture diagram, then we corrected the `InvoiceClient.send` signature it had hallucinated.

We **wrote and reviewed** every commit ourselves before pushing; we treat the AI as a fast pair programmer, not as the author. Critical design choices (layered architecture, the choice to host on Railway, the bcrypt cost factor, the choice not to use Hibernate) were team decisions discussed in person.

## 9. Contributions

| Member | Owned modules | Notable commits |
|---|---|---|
| Ahmed Sayed | DB layer, schema design, MySQL migration | `db: migrate from SQLite to MySQL on Railway` |
| Khaled Ahmed | Models, controllers, polish & bug-fixing | `fix: NOT NULL columns in sale_items/purchase_items, login UX` |
| Mohamed Ahmed Ismael | DAO + service layer | `dao: BaseDao<T> generic CRUD`, supplier/parts services |
| Mohamed Hamdey Besher | Tests, dashboard, FXGL chart | `test: end-to-end smoke test against the live database` |
| Mohamed Soliman | UI / FXML, CSS, login flow | `ui: Login + MainShell with sidebar navigation` |
| Taha Yasser | Threads, sockets, project lead, documentation | `thread: background StockMonitorTask` · `docs: design notes` |

(Run `git shortlog -sn` to verify.)

## 10. Screenshots

> **Add screenshots before exporting to PDF.** Suggested set:
> 1. Login screen.
> 2. Dashboard — stat cards + 30-day sales chart + low-stock list.
> 3. Spare Parts grid with a search filter applied.
> 4. POS / Sales screen mid-invoice.
> 5. Reports — bar chart + pie chart + KPI strip.
> 6. Settings — confirms socket is running and DB URL.
> 7. Railway Data tab showing the 7 tables.

## 11. How to run (grader's quick path)

```bash
# 1) Copy the credentials template
cp src/main/resources/application.properties.example src/main/resources/application.properties
# 2) Edit application.properties with the values the team provided.
# 3) Build & launch the desktop app:
mvn javafx:run
# 4) Login: admin / admin123 (or employee / emp123, seller / sell123).
```

To run the integration smoke (will hit the live shared DB):

```bash
mvn test -Dtest=IntegrationSmokeTest
```

## 12. Known limitations / future work

- No reset-password flow; admins overwrite a user's password directly.
- The chart on the dashboard is FXGL-based; if the graphics driver lacks OpenGL support it falls back to a static chart.
- The TCP socket protocol is Java-serialized objects; it would be cleaner as JSON, but `Serializable` keeps the inter-JVM contract trivial for a desktop project.
- The Railway free tier sleeps after ~10 min idle; the first query after a sleep takes ~1 s.

## 13. Conclusion

Metrkansh exercises every required topic — OOP, JDBC, multithreading, sockets, generics, JavaFX — on a single realistic, testable, multi-user codebase, deployed on real hosted infrastructure. Total lines of Java: see `cloc src/main/java`.
