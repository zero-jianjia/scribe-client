package com.zero.scribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import java.util.concurrent.TimeUnit;


/**
 * Created by jianjia1 on 16/07/28.
 */

public class ScribeTest {
    private static final Logger LOG = LoggerFactory.getLogger(Test.class);

    @Test
    public void globalTest() {
        while(true){
            for (int i = 0; i < 1000; i++) {
                LOG.info("zero00" + i);
            }
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
