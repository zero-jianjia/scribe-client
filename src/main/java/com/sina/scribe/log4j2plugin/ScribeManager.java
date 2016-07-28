package com.sina.scribe.log4j2plugin;

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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
        String[] args = host.split(";");
        if (args.length != 2) {
            throw new IllegalArgumentException("host is not vaild.");
        }
        this.category = category;
        this.scribe_ip = args[0];
        this.scribe_port = Integer.valueOf(args[1]);
    }

    public void startup() {
        try {
            TSocket sock = new TSocket(new Socket(scribe_ip, scribe_port));
            transport = new TFramedTransport(sock);
            TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
            client = new Scribe.Client(protocol, protocol);
        } catch (TTransportException e) {
            //TODO
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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

    public void send(final String msg) throws ExecutionException, InterruptedException, TimeoutException {
        isConnect();
        final LogEntry entry = new LogEntry(category, msg);
        try {
            client.send_Log(Arrays.asList(entry));
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void releaseSub() {
        super.releaseSub();
        transport.close();
    }


}
