package com.sina.scribe.log4j2plugin;

import com.sina.scribe.DefaultScribeClientFactory;
import com.sina.scribe.core.MessageEntry;
import com.sina.scribe.core.ResultCode;
import com.sina.scribe.core.Scribe;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static mockit.Deencapsulation.invoke;

/**
 * Created by jianjia1 on 16/08/02.
 */
@RunWith(JMockit.class)
public class ScribeManagerTest {
    @Tested
    @Mocked
    ScribeManager manager;

    @Before
    public void init() {
        manager = new ScribeManager("", "127.0.0.1:80", "", "", 0, 0);

        new MockUp<DefaultScribeClientFactory>() {
            @Mock
            public Scribe.Client createClient()
                    throws IOException, TTransportException, TException {
                return new Scribe.Client(null);
            }
        };

        new MockUp<ThreadPoolExecutor>() {
            @Mock
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void testSend() {
        byte[] msg = "Hello".getBytes();
        MessageEntry entry = new MessageEntry("EA_IMPMONITOR",
                ByteBuffer.wrap(Arrays.copyOf(msg, msg.length)));

        new MockUp<Scribe.Client>() {
            @Mock
            public ResultCode Log(List<MessageEntry> msgs) throws TException {
                return ResultCode.TRY_LATER;
            }
        };
    }

    @Test
    public void testInitClient() {
        invoke(manager, "initClient");
    }
}
