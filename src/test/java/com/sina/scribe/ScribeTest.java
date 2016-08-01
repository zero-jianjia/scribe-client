package com.sina.scribe;

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
        System.out.println("---");
        LOG.info("zero007");
        System.out.println("over");
    }
}
