package com.matraknhash.app;

import com.matraknhash.model.Part;
import com.matraknhash.model.SaleItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Per-session shopping cart for the customer marketplace flow.
 *
 * Lives on {@link Session} alongside the current user, so it survives screen
 * navigation but resets at logout. Backed by an {@link ObservableList} so the
 * checkout UI updates reactively as items are added/removed.
 *
 * Constraint: a single cart can only contain items from ONE seller at a time
 * (the marketplace order model in `sales` has a single seller_id per row).
 * Adding a part from a different seller triggers a "clear cart" prompt in the
 * UI -- this class itself just exposes {@link #sellerId()}.
 */
public final class Cart {

    private final ObservableList<SaleItem> items = FXCollections.observableArrayList();
    private Integer sellerId;
    private String  sellerName;

    public ObservableList<SaleItem> items() { return items; }
    public Integer sellerId() { return sellerId; }
    public String sellerName() { return sellerName; }
    public int size() { return items.size(); }
    public boolean isEmpty() { return items.isEmpty(); }

    public double subtotal() {
        return items.stream().mapToDouble(SaleItem::getSubtotal).sum();
    }

    /** Add or merge a part. Throws if mixing sellers. */
    public void add(Part p, int qty) {
        if (sellerId != null && p.getSellerId() != null && !sellerId.equals(p.getSellerId()))
            throw new IllegalStateException("Cart already contains items from another seller. Empty the cart first.");
        if (sellerId == null) {
            sellerId   = p.getSellerId();
            sellerName = null; // resolved by the UI controller when needed
        }
        for (SaleItem existing : items) {
            if (existing.getPartId() == p.getId()) {
                existing.setQuantity(existing.getQuantity() + qty);
                items.set(items.indexOf(existing), existing); // trigger change listener
                return;
            }
        }
        items.add(new SaleItem(p.getId(), p.getSku(), p.getName(), qty, p.getSellPrice()));
    }

    public void remove(int index) {
        if (index < 0 || index >= items.size()) return;
        items.remove(index);
        if (items.isEmpty()) clear();
    }

    public void clear() {
        items.clear();
        sellerId = null;
        sellerName = null;
    }

    public void rememberSellerName(String name) { this.sellerName = name; }
}
