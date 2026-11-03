package com.yunshang.market;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yunshang.market.mapper")
@SpringBootApplication
public class YunshangMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunshangMarketApplication.class, args);
    }
}
