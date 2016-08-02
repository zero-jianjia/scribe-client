package com.sina.scribe;

import com.facebook.fb303.fb_status;
import com.sina.scribe.core.Scribe;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.*;

import java.io.IOException;
import java.net.Socket;

import static javafx.scene.input.KeyCode.T;

/**
 * Created by jianjia1 on 16/07/29.
 */
public class DefaultScribeClientFactory {
    private final String ip;
    private final int port;
    private TFramedTransport transport;
    private TNonblockingTransport nonblockingTransport;

    public DefaultScribeClientFactory(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Scribe.Client createClient()
            throws IOException, TTransportException, TException {
        // NOTE: when scribe process is killed (scribe process is running before),
        //        transport is still open.
        if (transport != null) {
            transport.close();
        }

        TSocket tSocket = new TSocket(new Socket(ip, port));
        transport = new TFramedTransport(tSocket);
        TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
        Scribe.Client client = new Scribe.Client(protocol, protocol);

        fb_status status = client.getStatus();
        if (fb_status.ALIVE == status) {//client.getStatus() is blocking method.
            return client;
        } else {
            throw new TException("client.getStatus() is " + status.name());
        }
    }

    public Scribe.AsyncClient createAsyncClient()
            throws IOException, TTransportException, TException {
        if (nonblockingTransport != null) {
            nonblockingTransport.close();
        }
        nonblockingTransport = new TNonblockingSocket(ip, port);

        // 异步调用管理器
        TAsyncClientManager clientManager = new TAsyncClientManager();
        TProtocolFactory tprotocol = new TBinaryProtocol.Factory();

        Scribe.AsyncClient asyncClient = new Scribe.AsyncClient(tprotocol, clientManager, nonblockingTransport);
        return asyncClient;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void close() {
        if (transport != null) {
            transport.close();
        }
        if (nonblockingTransport != null) {
            nonblockingTransport.close();
        }
    }

    @Override
    public String toString() {
        return "Remote socket address is " + ip + ":" + port;
    }
}
