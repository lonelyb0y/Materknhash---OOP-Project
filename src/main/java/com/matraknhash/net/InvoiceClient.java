package com.matraknhash.net;

import com.matraknhash.model.Sale;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Thin TCP client used by the POS screen to push invoices to the server. */
public class InvoiceClient {

    private final String host;
    private final int port;

    public InvoiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public InvoiceMessage send(Sale sale) throws IOException, ClassNotFoundException {
        try (Socket sock = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream in   = new ObjectInputStream(sock.getInputStream())) {
            out.writeObject(InvoiceMessage.request(sale));
            out.flush();
            return (InvoiceMessage) in.readObject();
        }
    }
}
