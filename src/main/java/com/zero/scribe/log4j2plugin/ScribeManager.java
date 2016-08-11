package com.zero.scribe.log4j2plugin;

import com.zero.scribe.DefaultScribeClientFactory;
import com.zero.scribe.core.MessageEntry;
import com.zero.scribe.core.ResultCode;
import com.zero.scribe.core.Scribe;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by jianjia1 on 16/07/28.
 */
public class ScribeManager extends AbstractManager {
    private final DefaultScribeClientFactory clientFactory;
    private final String category;
    private final int retries = 3;
    private final int batchSize; //default 20
    private final String fileName;
    private final ExecutorService sendExecutor;
//    private final int interval;

    private Scribe.Client client;
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    public ScribeManager(String name, String host, String category, String fileName,
            /*int interval,*/ int batchSize) {
        super(name);
        String[] args = host.split(":");
        if (args.length != 2) {
            throw new IllegalArgumentException("host is not vaild.");
        }
        this.category = category;
        this.clientFactory = new DefaultScribeClientFactory(args[0], Integer.valueOf(args[1]));

        this.fileName = fileName != null ? fileName : category + ".log";
//        this.interval = interval;
        this.batchSize = batchSize > 0 ? batchSize : 20;

        this.sendExecutor = Executors.newSingleThreadExecutor((r) -> {
            Thread t = new Thread(r, this.category + "-scribe-sender-thread");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    public void startup() {
        initClient();

        sendExecutor.execute(() -> {
            for (; ; ) {
                try {
                    final List<MessageEntry> entrys = new ArrayList<>(batchSize);
                    for (int i = 0; i < batchSize; i++) {
                        byte[] msg = queue.poll(3000, TimeUnit.MILLISECONDS);
                        if (msg != null) {
                            entrys.add(new MessageEntry(category, ByteBuffer.wrap(Arrays.copyOf(msg, msg.length))));
                        }
                    }
                    if (entrys.size() > 0) {
                        if (!send(entrys)) {//发送失败则写入文件
                            writeFile(entrys);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initClient() {
        try {
            client = clientFactory.createClient();
            LOGGER.info("scribe client created, {}, category is {}.",
                    clientFactory.toString(), category);
        } catch (IOException e) {
            LOGGER.error("Socket can not connection, ip = {}, port = {}.",
                    clientFactory.getIp(), clientFactory.getPort());
            e.printStackTrace();
        } catch (TTransportException e) {
            LOGGER.error("TSocket can not create.");
            e.printStackTrace();
        } catch (TException ex) {
            LOGGER.error("scribe client can not created.");
            ex.printStackTrace();
        }
    }

    public boolean append(final byte[] msg) throws InterruptedException {
        return queue.offer(msg, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * send MessageEntry to Scribe.
     * if fail, retry until retries times.
     * if Exception throwed, rebuild client.
     * @param msgs
     */
    private boolean send(List<MessageEntry> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return true;
        }

        boolean sendRst = false;
        try {
            for (int i = 0; i < retries; i++) {
                ResultCode result = client.Log(msgs);
                if (result == ResultCode.SUCCESS) {
                    sendRst = true;
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(5 + i);
                } catch (InterruptedException e) {
                }
            }
        } catch (Exception e) {
            initClient();
            e.printStackTrace();
        }
        return sendRst;
    }

    @Override
    protected void releaseSub() {
        super.releaseSub();
        sendExecutor.shutdown();
        try {
            sendExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            LOGGER.warn("ScribeManager Thread pool failed to shut down", e);
        }
        clientFactory.close();
    }

    public void writeFile(List<MessageEntry> msgs) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            for (MessageEntry entry : msgs) {
                writer.write(new String(entry.getContent()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dealFailedlMsgs() {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        final List<MessageEntry> entrys = new ArrayList<>(batchSize);
        String line;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            while ((line = in.readLine()) != null) {
                byte[] msg = line.getBytes();
                entrys.add(new MessageEntry(category, ByteBuffer.wrap(Arrays.copyOf(msg, msg.length))));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (send(entrys)) {
            file.delete();
        }
    }
}
