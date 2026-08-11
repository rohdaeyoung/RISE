package com.withu.demo;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.file.entity.StoredFile;
import com.withu.file.repository.StoredFileRepository;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.group.repository.GroupRepository;
import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;
import com.withu.meal.repository.MealRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import com.withu.mission.repository.MissionRepository;
import com.withu.onboarding.entity.Gender;
import com.withu.onboarding.entity.Goal;
import com.withu.onboarding.entity.Onboarding;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 심사용 데모 데이터를 만든다 (`withu.demo.seed=true`일 때만 동작).
 *
 * <p>대회 심사에서는 심사위원(또는 심사용 AI)이 테스트 계정 하나로 앱 전체를 둘러본다.
 * 그런데 방금 배포한 서버는 그룹이 Day 1이라, 7일 챌린지 결과 화면처럼 "시간이 지나야 보이는"
 * 기능을 볼 방법이 없다. 그래서 <b>이미 6일을 함께 달려온 4인 그룹</b>을 미리 만들어 둔다.
 *
 * <p>서버가 뜰 때마다 데모 계정 데이터를 지우고 다시 만든다. 누군가 "계속하기"를 눌러
 * Day 1로 돌아가더라도 재배포/재시작하면 심사용 상태가 원래대로 복구된다.
 * 데모 계정 외의 실제 가입자 데이터는 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "withu.demo", name = "seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    /** 제출 서류에 적을 심사용 계정. */
    public static final String DEMO_EMAIL = "test@withu.app";
    public static final String DEMO_PASSWORD = "withu1234";
    private static final String DEMO_GROUP_CODE = "TEAM33";

    /** 데모 그룹을 채워줄 동료들 — 그룹 피드·랭킹이 비어 보이지 않게 함께 만든다. */
    private static final List<Member> MEMBERS = List.of(
            new Member(DEMO_EMAIL, "테스터", "dino", Goal.DIET, Gender.FEMALE, 24, 165, 60,
                    new int[]{3, 2, 3, 2, 3, 2}),
            new Member("mate1@withu.app", "민준", "tit", Goal.BULK, Gender.MALE, 22, 175, 65,
                    new int[]{3, 3, 3, 3, 2, 3}),
            new Member("mate2@withu.app", "서연", "hedgehog", Goal.HEALTH, Gender.FEMALE, 28, 162, 55,
                    new int[]{1, 1, 0, 1, 0, 1}),
            new Member("mate3@withu.app", "수아", "bat", Goal.DIET, Gender.FEMALE, 26, 168, 58,
                    new int[]{2, 3, 2, 3, 2, 2})
    );

    private static final List<String> DIET_TITLES = List.of(
            "아침 든든하게 챙겨 먹기", "점심 단백질 위주로 먹기", "저녁 탄수화물 줄이기",
            "채소 반찬 두 가지 이상 먹기", "야식 대신 물 마시기", "간식은 과일로 바꾸기");
    private static final List<String> LIFESTYLE_TITLES = List.of(
            "물 1.5L 마시기", "점심 후 10분 걷기", "자기 전 스트레칭 5분");
    private static final int[] UNLOCK_OFFSET_MINUTES = {0, 210, 420};
    private static final int MISSIONS_PER_DAY = 3;
    private static final int COIN_PER_MISSION = 10;
    private static final int CYCLE_DAYS = Group.CHALLENGE_LENGTH_DAYS;

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OnboardingRepository onboardingRepository;
    private final MissionRepository missionRepository;
    private final MealRepository mealRepository;
    private final StoredFileRepository storedFileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        clearPreviousDemoData();

        // 그룹은 방장(host)이 있어야 만들 수 있으므로 계정을 먼저 만든다.
        List<User> users = MEMBERS.stream().map(this::createUser).toList();

        Group group = groupRepository.save(Group.builder()
                .code(DEMO_GROUP_CODE)
                .name("3팀 데모")
                .hostUserId(users.get(0).getId())
                .missionHour(Group.DEFAULT_MISSION_HOUR)
                .missionMinute(Group.DEFAULT_MISSION_MINUTE)
                .build());
        groupRepository.flush();

        // 오늘이 챌린지 7일차가 되도록 시작일을 6일 전으로 되돌린다. 엔티티는 생성 시점을 시작일로
        // 잡기 때문에(정상 동작) 데모용으로만 SQL로 직접 조정한다.
        LocalDateTime startedAt = LocalDateTime.now().minusDays(CYCLE_DAYS - 1L);
        jdbcTemplate.update("UPDATE study_groups SET started_at = ? WHERE id = ?", startedAt, group.getId());

        LocalDate today = LocalDate.now();
        for (int i = 0; i < MEMBERS.size(); i++) {
            seedMember(MEMBERS.get(i), users.get(i), group, today);
        }

        log.info("심사용 데모 데이터를 생성했습니다. 계정={} / 그룹코드={} / {}일차",
                DEMO_EMAIL, DEMO_GROUP_CODE, CYCLE_DAYS);
    }

    private User createUser(Member member) {
        User user = userRepository.save(User.builder()
                .email(member.email())
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .build());
        user.changeNickname(member.nickname());
        userRepository.flush();

        characterRepository.save(Character.builder()
                .userId(user.getId())
                .species(member.species())
                .build());
        return user;
    }

    private void seedMember(Member member, User user, Group group, LocalDate today) {
        groupMemberRepository.save(GroupMember.builder().group(group).userId(user.getId()).build());

        onboardingRepository.save(Onboarding.builder()
                .userId(user.getId())
                .groupId(group.getId())
                .goal(member.goal())
                .gender(member.gender())
                .age(member.age())
                .height(member.height())
                .weight(member.weight())
                .build());

        int completed = seedMissions(member, group, user.getId(), today);
        seedTodayMeals(member, user.getId(), today);

        // 코인·점수는 실제 규칙(미션 1개당 10)과 어긋나지 않게 완료 수에서 그대로 계산한다.
        int earned = completed * COIN_PER_MISSION;
        user.addCoins(earned);
        user.addPoints(earned);
        groupMemberRepository.findByUserId(user.getId())
                .ifPresent(gm -> gm.addCyclePoints(earned));
    }

    /** 지난 6일치 기록 + 오늘 미션을 만든다. 반환값은 완료한 미션 수. */
    private int seedMissions(Member member, Group group, Long userId, LocalDate today) {
        List<Mission> missions = new ArrayList<>();
        List<Integer> doneFlags = new ArrayList<>();

        for (int dayOffset = CYCLE_DAYS - 1; dayOffset >= 1; dayOffset--) {
            int doneCount = member.dailyDone()[CYCLE_DAYS - 1 - dayOffset];
            LocalDate date = today.minusDays(dayOffset);
            for (int seq = 0; seq < MISSIONS_PER_DAY; seq++) {
                missions.add(buildMission(userId, group, date, seq));
                doneFlags.add(seq < doneCount ? 1 : 0);
            }
        }
        // 심사 계정의 오늘 미션은 직접 인증해볼 수 있도록 비워두고, 동료들은 일부 완료해 둔다.
        // 넷 다 0%면 그룹 피드가 전원 슬픈 표정이라 앱이 죽어 보인다.
        int todayDone = member.email().equals(DEMO_EMAIL) ? 0 : member.dailyDone()[CYCLE_DAYS - 2];
        for (int seq = 0; seq < MISSIONS_PER_DAY; seq++) {
            missions.add(buildMission(userId, group, today, seq));
            doneFlags.add(seq < todayDone ? 1 : 0);
        }

        List<Mission> saved = missionRepository.saveAll(missions);
        missionRepository.flush();

        int completed = 0;
        for (int i = 0; i < saved.size(); i++) {
            if (doneFlags.get(i) == 1) {
                saved.get(i).complete();
                completed++;
            }
        }
        return completed;
    }

    private Mission buildMission(Long userId, Group group, LocalDate date, int seq) {
        boolean lifestyle = seq == MISSIONS_PER_DAY - 1;
        List<String> pool = lifestyle ? LIFESTYLE_TITLES : DIET_TITLES;
        String title = pool.get(Math.floorMod(date.getDayOfYear() + seq, pool.size()));
        LocalTime base = LocalTime.of(group.getMissionHour(), group.getMissionMinute());
        return Mission.builder()
                .userId(userId)
                .groupId(group.getId())
                .missionDate(date)
                .seq(seq)
                .type(lifestyle ? MissionType.LIFESTYLE : MissionType.DIET)
                .title(title)
                .unlockTime(seq == 0 ? null : base.plusMinutes(UNLOCK_OFFSET_MINUTES[seq]))
                .build();
    }

    /**
     * 오늘 아침 인증 사진을 만들어 둔다. 그룹 피드가 빈 칸투성이면 앱이 비어 보이므로,
     * 꼴찌 역할(달성률이 낮은 멤버)만 빼고 사진을 채운다.
     */
    private void seedTodayMeals(Member member, Long userId, LocalDate today) {
        if (member.dailyDone()[CYCLE_DAYS - 2] == 0) {
            return;
        }
        String photoUrl = storePlaceholderPhoto();
        mealRepository.save(Meal.builder()
                .userId(userId)
                .mealDate(today)
                .slot(MealSlot.BREAKFAST)
                .achieved(true)
                .internalFit(Meal.InternalFit.GOOD)
                .photoUrl(photoUrl)
                .build());
    }

    /** 실제 사진 대신 쓸 간단한 식단 이미지. 외부 파일에 의존하지 않도록 코드로 그린다. */
    private String storePlaceholderPhoto() {
        String id = UUID.randomUUID().toString();
        storedFileRepository.save(StoredFile.builder()
                .id(id)
                .contentType("image/jpeg")
                .data(drawMealImage())
                .build());
        return "/api/files/" + id;
    }

    private byte[] drawMealImage() {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xEB, 0xE4, 0xD7));
        g.fillRect(0, 0, size, size);
        g.setColor(Color.WHITE);
        g.fillOval(36, 36, size - 72, size - 72);

        Random random = new Random(size);
        for (int i = 0; i < 120; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * 160;
            int x = (int) (size / 2 + Math.cos(angle) * radius);
            int y = (int) (size / 2 + Math.sin(angle) * radius);
            g.setColor(new Color(60 + random.nextInt(60), 130 + random.nextInt(60), 50 + random.nextInt(40)));
            g.fillOval(x - 16, y - 16, 32, 32);
        }
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * 130;
            int x = (int) (size / 2 + Math.cos(angle) * radius);
            int y = (int) (size / 2 + Math.sin(angle) * radius);
            g.setColor(new Color(0xC6, 0x2D, 0x28));
            g.fillOval(x - 18, y - 18, 36, 36);
        }
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("데모 사진 생성 실패", e);
        }
    }

    /**
     * 이전에 만든 데모 데이터를 지운다. 이메일로 데모 계정만 골라내므로 실제 가입자에게는 영향이 없다.
     * 외래키 때문에 자식 테이블부터 지운다.
     */
    private void clearPreviousDemoData() {
        List<String> emails = MEMBERS.stream().map(Member::email).toList();
        List<Long> userIds = userRepository.findAll().stream()
                .filter(u -> emails.contains(u.getEmail()))
                .map(User::getId)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        String inClause = userIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElseThrow();
        jdbcTemplate.update("DELETE FROM challenge_results WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM user_badges WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM meals WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM missions WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM onboardings WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM group_members WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM character_owned_outfits WHERE character_id IN "
                + "(SELECT id FROM characters WHERE user_id IN (" + inClause + "))");
        jdbcTemplate.update("DELETE FROM characters WHERE user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM study_groups WHERE code = ?", DEMO_GROUP_CODE);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (" + inClause + ")");
        log.info("이전 데모 데이터를 정리했습니다. 계정 {}개", userIds.size());
    }

    private record Member(
            String email, String nickname, String species,
            Goal goal, Gender gender, int age, int height, int weight,
            /** 지난 6일간 하루에 완료한 미션 수 (오래된 날 → 어제 순). */
            int[] dailyDone
    ) {
    }
}
