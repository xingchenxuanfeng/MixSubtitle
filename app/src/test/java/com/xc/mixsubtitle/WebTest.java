package com.xc.mixsubtitle;


import org.junit.Test;

/**
 * Created by xc on 17-9-18.
 */
public class WebTest {
    @Test
    public void getTips() throws Exception {
        String tips = Web.getTips("the real url");
        System.out.print(tips);
    }

}