package com.zero.scribe;

import com.zero.scribe.core.MessageEntry;
import com.zero.scribe.core.Scribe;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by jianjia1 on 16/08/01.
 */
public class DefaultScribeClientFactoryTest {
    private DefaultScribeClientFactory clientFactory;

    @Before
    public void initFactory() {
        clientFactory = new DefaultScribeClientFactory("10.13.88.196", 1463);
    }

    @Test
    public void testCreateAsyncClient() {
        try {
            Scribe.AsyncClient asyncClient = clientFactory.createAsyncClient();
            asyncClient.Log(Arrays.asList(createEntry()), new AsyncMethodCallback<Scribe.AsyncClient.Log_call>() {
                @Override
                public void onComplete(Scribe.AsyncClient.Log_call result) {
                    System.out.println(Thread.currentThread().getName());
                    try {
                        System.out.println(result.getResult());
                    } catch (TException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        }
    }


    private MessageEntry createEntry() {
        byte[] msg = "Hello".getBytes();
        return new MessageEntry("EA_IMPMONITOR",
                ByteBuffer.wrap(Arrays.copyOf(msg, msg.length)));
    }
}
