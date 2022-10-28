package com.xinyi.geekbang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author guxinxin
 */
@SpringBootApplication
public class GeekBangApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeekBangApplication.class, args);

        System.out.println("GeekBangApplication 启动成功");
    }

}
