# 빌드 단계 — 소스를 jar로 굽는다. 이 단계의 결과물만 다음 단계로 넘기므로
# Gradle과 소스는 최종 이미지에 남지 않는다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 정보를 먼저 복사해 받아두면, 소스만 바뀐 재배포에서 이 레이어가 캐시된다.
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
# 테스트는 DB가 필요해 빌드 환경에서 돌릴 수 없으므로 제외한다.
RUN ./gradlew bootJar --no-daemon -x test

# 실행 단계 — JDK 대신 JRE만 담아 이미지를 가볍게 한다.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Railway 등은 PORT 환경변수로 포트를 지정한다. 없으면 8080을 쓴다.
ENV PORT=8080
EXPOSE 8080

# 컨테이너 기본 시간대가 UTC라 그대로 두면 "오늘"이 한국 시간과 어긋나
# 미션 생성일·챌린지 일차 계산이 하루씩 밀린다.
ENV TZ=Asia/Seoul

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
