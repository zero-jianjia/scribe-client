package com.sina.scribe;

import com.facebook.fb303.fb_status;
import com.sina.scribe.core.Scribe;
import org.apache.thrift.TException;
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

    public DefaultScribeClientFactory(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Scribe.Client createClient() throws IOException, TTransportException, TException {
        // NOTE: when scribe process is killed (scribe process is running before),
        //        transport is still open.
        if (transport != null) {
            transport.close();
        }

        TSocket tSocket = new TSocket(new Socket(ip, port));
        transport = new TFramedTransport(tSocket);
        TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
        Scribe.Client client = new Scribe.Client(protocol, protocol);

        if (client.getStatus() == fb_status.ALIVE) {//client.getStatus() is blocking method.
            return client;
        } else {
            return null;
        }
    }


    public static TNonblockingTransport intTNonblockingTransport(TNonblockingTransport transport, String ip, int port)
            throws IOException, TTransportException {
        if (transport != null) {
            //NOTE: when scribe process is killed (scribe process is running before),
            //      transport is still open.
            transport.close();
        }
        transport = new TNonblockingSocket(ip, port);
        return transport;
    }

    public static Scribe.AsyncClient createAsyncClient(TNonblockingTransport transport)
            throws IOException, TTransportException {
        // 异步调用管理器
        TAsyncClientManager clientManager = new TAsyncClientManager();
        TProtocolFactory tprotocol = new TBinaryProtocol.Factory();
        return new Scribe.AsyncClient(tprotocol, clientManager, transport);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void close(){
        if(transport!=null){
            transport.close();
        }
    }

    @Override
    public String toString() {
        return "Remote socket address is " +
                ip + ":" + port +
                ", transport.isOpen=" + transport.isOpen();
    }
}
