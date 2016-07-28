package com.sina.scribe;

import java.util.concurrent.TimeUnit;

/**
 * Created by jianjia1 on 16/07/28.
 */
public class Test {
//    private static final Logger LOG = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(i);
//            LOG.info("hello");
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("over");
    }
}
