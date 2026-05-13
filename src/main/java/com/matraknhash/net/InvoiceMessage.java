package com.matraknhash.net;

import com.matraknhash.model.Sale;

import java.io.Serializable;

/** Wire-format envelope between cashier client and ERP server. */
public class InvoiceMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { INVOICE_REQUEST, INVOICE_ACK, INVOICE_ERROR, STOCK_UPDATE }

    private final Type type;
    private final Sale sale;
    private final String info;

    public InvoiceMessage(Type type, Sale sale, String info) {
        this.type = type;
        this.sale = sale;
        this.info = info;
    }

    public static InvoiceMessage request(Sale s)         { return new InvoiceMessage(Type.INVOICE_REQUEST, s, null); }
    public static InvoiceMessage ack(int saleId)         { return new InvoiceMessage(Type.INVOICE_ACK, null, "sale#" + saleId); }
    public static InvoiceMessage error(String msg)       { return new InvoiceMessage(Type.INVOICE_ERROR, null, msg); }
    public static InvoiceMessage stockUpdate(String msg) { return new InvoiceMessage(Type.STOCK_UPDATE, null, msg); }

    public Type getType()  { return type; }
    public Sale getSale()  { return sale; }
    public String getInfo(){ return info; }
}
