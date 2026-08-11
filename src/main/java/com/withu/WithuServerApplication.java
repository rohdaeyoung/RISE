package com.withu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 그룹이 정한 시간에 일일 미션을 자동 생성한다 (DailyMissionScheduler).
@EnableScheduling
@SpringBootApplication
public class WithuServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WithuServerApplication.class, args);
	}

}
