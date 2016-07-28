package com.sina.scribe.log4j2plugin;

import com.facebook.fb303.fb_status;
import com.sina.scribe.core.LogEntry;
import com.sina.scribe.core.Scribe;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by jianjia1 on 16/07/28.
 */
public class ScribeManager extends AbstractManager {
    private final String scribe_ip;
    private final int scribe_port;
    private final String category;
    private Scribe.Client client;
    private TFramedTransport transport;

    public ScribeManager(String name, String host, String category) {
        super(name);
        String[] args = host.split(":");
        if (args.length != 2) {
            throw new IllegalArgumentException("host is not vaild.");
        }
        this.category = category;
        this.scribe_ip = args[0];
        this.scribe_port = Integer.valueOf(args[1]);
    }

    public synchronized void startup() {
        try {
            Socket socket = new Socket(scribe_ip, scribe_port);
            TSocket tSocket = new TSocket(socket);
            transport = new TFramedTransport(tSocket);
        } catch (IOException e) {
            LOGGER.error("Socket can not connection, ip = {}, port = {}.",
                    scribe_ip, scribe_port,
                    e);
            e.printStackTrace();
        } catch (TTransportException e) {
            LOGGER.error("TSocket can not create.", e);
            e.printStackTrace();
        }

        if (transport != null) {
            System.out.println(transport.isOpen());
            TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
            client = new Scribe.Client(protocol, protocol);
        }
        try {
            if (client != null && client.getStatus() == fb_status.ALIVE) {
                //client.getStatus() is blocking method.
                LOGGER.info("scribe client created, remote socket address is {}:{}, category is {}.",
                        scribe_ip, scribe_port, category);
            }
        } catch (TException ex) {
            LOGGER.error("scribe client can not created.", ex);
            ex.printStackTrace();
        }
    }

    /*
    * Connect to scribe if not open, reconnect if failed.
    */
    public void isConnect() {
        if (transport != null && transport.isOpen())
            return;

        if (transport != null && !transport.isOpen()) {
            transport.close();
        }
        startup();
    }

    public synchronized void send(final String msg) throws TException {
        isConnect();
        final LogEntry entry = new LogEntry(category, msg);
        if (client != null)
            try {
                client.send_Log(Arrays.asList(entry));
            } catch (TException ex) {
                transport.close();
                startup();//force to reconnect,when scribe process is killed, transport is still open.
                throw ex;
            }
    }

    @Override
    protected void releaseSub() {
        super.releaseSub();
        transport.close();
    }
}
