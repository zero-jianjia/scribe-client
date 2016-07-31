package com.sina.scribe.log4j2plugin;

import com.sina.scribe.DefaultScribeClientFactory;
import com.sina.scribe.core.*;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by jianjia1 on 16/07/28.
 */
public class ScribeManager extends AbstractManager {
    private final DefaultScribeClientFactory clientFactory;
    private final String category;
    private final int intervalMS;
    private final int bufferSize;
    private final int retries = 10;
    private final int try_later_time = 50;
    private final String fileName;
    private Scribe.Client client;

    private final ScheduledExecutorService sendExecutor;
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    public ScribeManager(String name, String host, String category, String fileName) {
        super(name);
        String[] args = host.split(":");
        if (args.length != 2) {
            throw new IllegalArgumentException("host is not vaild.");
        }
        this.category = category;
        this.clientFactory = new DefaultScribeClientFactory(args[0], Integer.valueOf(args[1]));
        this.intervalMS = 50;
        this.bufferSize = 20;
        this.fileName = fileName != null ? fileName : category + ".log";

        this.sendExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "scribe-append-pool");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    public void startup() {
        initClient();
        sendExecutor.scheduleWithFixedDelay(() -> {
            final List<MessageEntry> entrys = new ArrayList<>(bufferSize);
            for (int i = 0; i < bufferSize; i++) {
                byte[] msg = queue.poll();
                if (msg == null) {
                    break;
                }
                entrys.add(new MessageEntry(category, ByteBuffer.wrap(Arrays.copyOf(msg, msg.length))));
            }
            if (entrys.size() > 0) {
                boolean success = send(entrys);
                if (!success) {
                    writeFile(entrys);
                } else {
                    dealFailedlMsgs();
                }
            }
        }, 0, intervalMS, TimeUnit.MILLISECONDS);
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

    @Override
    protected void releaseSub() {
        super.releaseSub();
        clientFactory.close();
        sendExecutor.shutdown();
    }

    /**
     * send MessageEntry to Scribe, if fail retry until retries
     * if TException throwed, rebuild client.
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
                System.out.println("---" + result);
                if (result == ResultCode.SUCCESS) {
                    sendRst = true;
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(try_later_time);
                } catch (InterruptedException e) {
                }
            }
        } catch (TException e) {
            initClient();
            e.printStackTrace();
        }
        return sendRst;
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
        final List<MessageEntry> entrys = new ArrayList<>(bufferSize);
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
