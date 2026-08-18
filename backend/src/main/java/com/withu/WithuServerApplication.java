package com.withu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

// 그룹이 정한 시간에 일일 미션을 자동 생성한다 (DailyMissionScheduler).
@EnableScheduling
@SpringBootApplication
public class WithuServerApplication {

	/**
	 * 서비스 시간대를 한국으로 고정한다.
	 *
	 * <p>이 앱은 날짜와 시각에 크게 기댄다 — 오늘의 미션, 미션 도착 시각, 끼니 슬롯, 7일 사이클,
	 * 연속 인증. 그런데 {@code LocalDate.now()}는 <b>서버(JVM)의 기본 시간대</b>를 따르고,
	 * 클라우드 서버는 대부분 UTC로 뜬다. 고정하지 않으면 서버를 옮기는 것만으로 다음이 어긋난다.
	 *
	 * <pre>
	 *   UTC 서버에서 "오늘"이 바뀌는 시점   → 한국 시간 오전 9시
	 *   미션 시각을 오전 9시로 설정하면      → 실제로는 오후 6시에 도착
	 *   밤 10시에 한 인증                  → 다음 날 기록으로 저장
	 * </pre>
	 *
	 * <p>사용자는 전부 한국에 있고 심사도 한국에서 하므로 한국 시간이 곧 서비스 시간이다.
	 * 서버 환경변수(TZ)에 기대지 않고 코드에서 정한다 — 옮겨 갈 서버의 설정을 우리가 정할 수
	 * 없기 때문이다. 대회에서 받는 서버가 UTC여도 이 줄 하나로 그대로 동작한다.
	 *
	 * <p><b>반드시 {@code SpringApplication.run()} 전에 불러야 한다.</b> {@code @PostConstruct}로
	 * 늦게 부르면 그 사이에 만들어진 DB 커넥션 풀이 옛 시간대를 붙잡는다. 실제로 UTC 서버를
	 * 흉내 내서 띄웠더니 {@code created_at}은 한국 시간으로 맞는데 미션 날짜만 하루 전으로
	 * 저장됐다 — MySQL 드라이버가 커넥션의 시간대로 날짜를 변환하기 때문이다.
	 */
	private static void useKoreanTime() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		useKoreanTime();
		SpringApplication.run(WithuServerApplication.class, args);
	}

}
