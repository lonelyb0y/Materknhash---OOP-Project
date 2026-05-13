# Metrkansh ERP — UI Design Notes

Source: mockups provided by the team on 2026-05-13.

## Brand
- Name: **Metrkansh — Auto Parts ERP**
- Logo: car silhouette inside a gear, sidebar header

## Layout
- Fixed left **sidebar** (~220px), dark navy `#1a2332`, white labels + icons
- Top of sidebar: logo + brand
- Bottom of sidebar: Logout
- Main content: light grey `#f4f6fb` with white cards

## Sidebar nav (order)
1. Dashboard
2. Spare Parts
3. Sales
4. Purchases
5. Suppliers
6. Reports
7. Users
8. Settings
9. (separator) Logout

## Color tokens
| Token         | Hex      | Use                           |
|---------------|----------|-------------------------------|
| sidebar-bg    | #1a2332  | sidebar                       |
| sidebar-active| #3b82f6  | selected sidebar item         |
| content-bg    | #f4f6fb  | main background               |
| card-bg       | #ffffff  | cards                         |
| primary       | #3b82f6  | buttons, links, line chart    |
| success       | #16a34a  | profit, totals up             |
| warning       | #f59e0b  | parts count                   |
| danger        | #dc2626  | low stock, delete icon        |
| muted         | #64748b  | sub-labels                    |
| text          | #0f172a  | primary text                  |

## Screens
1. **Login** — centered card, username/password/role, dark backdrop.
2. **Dashboard** — 4 stat cards (sales, profit, parts, low-stock) + Sales Overview line chart (FXGL) + low-stock list + recent activities.
3. **Spare Parts** — left add/edit form, right paginated table with edit/delete icons + search + Add/Export buttons.
4. **Sales (New Invoice)** — customer info + invoice items table + summary panel (subtotal/discount/tax/total) + Save Invoice.
5. **Purchases** — same layout as Sales but increases stock and records to supplier.
6. **Suppliers** — left add form, right table.
7. **Reports** — report-type list on left, summary cards + bar chart + donut chart + monthly table.
8. **Users** — add-user form + table; admin-only.
9. **Settings** — DB path, socket port, default low-stock threshold.

## Charts
- Sales Overview (Dashboard) — **FXGL** line chart, live from `sales` table. Required for grading.
- Top Selling Parts (Dashboard) — JavaFX `PieChart`, top N by quantity sold.
- Reports — JavaFX `BarChart` + `PieChart`.

## Currency
EGP, formatted with thousands separator.
