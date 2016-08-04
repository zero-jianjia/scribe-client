package com.zero.scribe;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by jianjia1 on 16/07/28.
 */

public class ScribeTest {
    private static final Logger LOG = LoggerFactory.getLogger(Test.class);

    @Test
    public void globalTest() {
        LOG.info("zero");
    }
}
