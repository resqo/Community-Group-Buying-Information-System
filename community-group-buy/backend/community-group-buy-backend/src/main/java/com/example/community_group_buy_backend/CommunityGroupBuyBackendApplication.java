package com.example.community_group_buy_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.community_group_buy_backend.mapper")
public class CommunityGroupBuyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunityGroupBuyBackendApplication.class, args);
	}

}
