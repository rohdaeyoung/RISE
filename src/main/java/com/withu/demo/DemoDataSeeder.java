package com.withu.demo;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.challenge.service.ChallengeService;
import com.withu.character.repository.CharacterRepository;
import com.withu.feed.entity.FeedComment;
import com.withu.feed.entity.FeedReaction;
import com.withu.feed.repository.FeedCommentRepository;
import com.withu.feed.repository.FeedReactionRepository;
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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
 * 기능을 볼 방법이 없다. 그래서 <b>이미 6일을 함께 달려온 그룹</b>을 미리 만들어 둔다. 정원 4명 중 세 자리만 채우고
 * 한 자리는 비워 둔다 — 부스에서 직접 가입해 합류해보는 사람을 위한 자리다.
 *
 * <p>서버가 뜰 때, 그리고 <b>날짜가 바뀔 때마다</b> 데모 계정 데이터를 지우고 다시 만든다.
 * 기동 시에만 만들면 배포일과 심사일이 다를 때(8/19 배포 → 8/21 심사) 문제가 생긴다. 동료 계정은
 * 아무도 앱을 켜지 않으므로 그날 미션이 전부 미완료로 남고, 그룹 피드가 전원 0%·슬픈 표정에
 * 사진 한 장 없는 상태가 된다. 누군가 "계속하기"를 눌러 Day 1로 돌아간 경우도 함께 복구된다.
 *
 * <p>데모 계정 외의 실제 가입자 데이터는 어떤 경로로도 건드리지 않는다.
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

    /**
     * 데모 그룹을 채워줄 동료들 — 그룹 피드·랭킹이 비어 보이지 않게 함께 만든다.
     *
     * <p><b>정원(4명)을 다 채우지 않고 세 명만 만든다.</b> 부스에서 앱을 본 사람이 직접 가입해
     * {@code TEAM33}을 넣어보는 흐름이 대회 투표와 직결되는데, 넷을 다 채우면 그 사람은
     * "그룹 인원이 가득 찼습니다"만 보게 된다. 혼자 방을 새로 만들면 그룹 랭킹·피드가 텅 비어
     * 이 앱의 핵심인 "함께"가 하나도 보이지 않는다. 한 자리를 비워 두면 이미 6일을 달려온
     * 그룹에 바로 합류하는 경험이 된다.
     *
     * <p>달성률은 높음(민준)·중간(테스터)·낮음(서연)으로 벌려 둔다. 난이도 자동 조절이
     * 무엇을 보고 움직이는지가 랭킹 한 화면에서 드러나야 하기 때문이다.
     */
    private static final List<Member> MEMBERS = List.of(
            new Member(DEMO_EMAIL, "테스터", "dino", Goal.DIET, Gender.FEMALE, 24, 165, 60,
                    new int[]{3, 2, 3, 2, 3, 2}),
            new Member("mate1@withu.app", "민준", "tit", Goal.BULK, Gender.MALE, 22, 175, 65,
                    new int[]{3, 3, 3, 3, 2, 3}),
            new Member("mate2@withu.app", "서연", "hedgehog", Goal.HEALTH, Gender.FEMALE, 28, 162, 55,
                    new int[]{1, 1, 0, 1, 0, 1})
    );

    /**
     * 예전 배포에서 만들었지만 지금은 만들지 않는 데모 계정.
     *
     * <p>정리는 {@link #MEMBERS}의 이메일로 대상을 고르기 때문에, 목록에서 빼기만 하면 이미 DB에
     * 있는 계정이 영영 지워지지 않고 전체 랭킹에 남는다. 빼는 순간 여기로 옮겨야 한다.
     */
    private static final List<String> RETIRED_EMAILS = List.of("mate3@withu.app");

    private static final List<String> DIET_TITLES = List.of(
            "아침 든든하게 챙겨 먹기", "점심 단백질 위주로 먹기", "저녁 탄수화물 줄이기",
            "채소 반찬 두 가지 이상 먹기", "야식 대신 물 마시기", "간식은 과일로 바꾸기");
    private static final List<String> LIFESTYLE_TITLES = List.of(
            "물 1.5L 마시기", "점심 후 10분 걷기", "자기 전 스트레칭 5분");
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
    private final FeedReactionRepository feedReactionRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ChallengeService challengeService;
    private final EntityManager entityManager;

    /** 마지막으로 데모 데이터를 만든 날. 날짜가 바뀌면 다시 만든다. */
    private volatile LocalDate seededOn;

    // seed()를 private으로 두고 진입점 두 곳에 @Transactional을 건다. 같은 빈 안에서 seed()를
    // 호출하면 프록시를 거치지 않아 트랜잭션이 걸리지 않기 때문이다.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    /**
     * 10분마다 날짜가 넘어갔는지 확인한다. 자정 정각 cron 하나만 두면 그 시각에 서버가 재시작 중이거나
     * 잠깐 죽어 있으면 그날 하루가 통째로 비어버리므로, 놓쳐도 곧 따라잡는 폴링 방식을 쓴다.
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    @Transactional
    public void reseedIfDateChanged() {
        if (LocalDate.now().equals(seededOn)) {
            return;
        }
        log.info("날짜가 바뀌어 심사용 데모 데이터를 다시 만듭니다.");
        seed();
    }

    private void seed() {
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

        seedFeed(group, users, today);

        settleChallenge(users.get(0).getId());

        seededOn = today;
        log.info("심사용 데모 데이터를 생성했습니다. 계정={} / 그룹코드={} / {}일차 (7일 결과 정산 완료)",
                DEMO_EMAIL, DEMO_GROUP_CODE, CYCLE_DAYS);
    }

    /**
     * 그룹 피드에 오늘의 반응과 댓글을 채워 둔다.
     *
     * <p>이걸 안 하면 심사위원이 "그룹 피드" 탭을 열었을 때 반응 0개, 댓글 0개인 빈 화면을 본다.
     * 미션·랭킹은 채워 두면서 정작 <b>이 앱이 개인 기록 앱과 다르다고 주장하는 화면</b>만 비어
     * 있으면, 트랙 심사의 "차별적 새로움"을 스스로 지우는 셈이다.
     *
     * <p>테스터(심사 계정)는 <b>반응을 남기지 않은 상태</b>로 둔다. 받은 반응만 보이고 내가 누른
     * 것은 없어야, 심사위원이 직접 눌러보는 여지가 남는다.
     */
    private void seedFeed(Group group, List<User> users, LocalDate today) {
        Long tester = users.get(0).getId();
        Long minjun = users.get(1).getId();
        Long seoyeon = users.get(2).getId();

        feedReactionRepository.saveAll(List.of(
                reaction(group, today, minjun, seoyeon, "👍"),
                reaction(group, today, seoyeon, minjun, "❤️"),
                reaction(group, today, minjun, tester, "❤️"),
                reaction(group, today, seoyeon, tester, "😂")
        ));

        feedCommentRepository.saveAll(List.of(
                comment(group, today, minjun, "다들 오늘도 화이팅! 저녁에 같이 걸을 사람?"),
                comment(group, today, seoyeon, "요즘 계속 놓쳤는데 오늘은 하나 했어요 ㅎㅎ"),
                comment(group, today, minjun, "서연님 오늘 사진 좋다 👏")
        ));
    }

    private FeedReaction reaction(Group group, LocalDate date, Long actor, Long target, String emoji) {
        return FeedReaction.builder()
                .groupId(group.getId())
                .feedDate(date)
                .actorUserId(actor)
                .targetUserId(target)
                .emoji(emoji)
                .build();
    }

    private FeedComment comment(Group group, LocalDate date, Long author, String text) {
        return FeedComment.builder()
                .groupId(group.getId())
                .feedDate(date)
                .authorUserId(author)
                .text(text)
                .build();
    }

    /**
     * 7일 결과를 미리 정산해 둔다.
     *
     * <p>이걸 안 하면 결과는 심사위원이 그룹 화면에서 "7일 챌린지 결과 보기"를 <b>눌러야</b> 만들어진다.
     * 사람이라면 누르겠지만, 심사를 AI가 대신 훑는 경우에는 그 버튼을 지나칠 수 있고 그러면
     * 순위·뱃지·보너스 코인 같은 이 앱의 마무리가 통째로 안 보인다. 미리 만들어 두면
     * {@code GET /api/challenges/summary}가 처음부터 결과를 돌려준다.
     *
     * <p>버튼을 눌러도 문제없다. {@code endChallenge}는 이미 정산된 사이클이면 저장된 결과를
     * 그대로 돌려주므로 보상이 두 번 지급되지 않는다.
     *
     * <p>정산해도 오늘 미션·식단 인증·그룹 피드는 그대로 쓸 수 있다. 결과 화면은 따로 열리는 것이지
     * 앱을 잠그는 것이 아니라서, 심사위원이 사진 인증을 직접 해보는 시연도 그대로 가능하다.
     */
    private void settleChallenge(Long demoUserId) {
        // 시작일은 위에서 JPA를 우회해 SQL로 바꿨다. 그래서 영속성 컨텍스트에 들고 있는 Group은
        // 아직 "오늘 시작한 그룹"이고, 그대로 정산을 부르면 isLastDay()가 false라
        // "아직 챌린지가 끝나지 않았어요"로 튕긴다. 캐시를 비워 DB에서 다시 읽게 한다.
        entityManager.flush();
        entityManager.clear();

        challengeService.endChallenge(demoUserId);
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
        // 셋 다 0%면 그룹 피드가 전원 슬픈 표정이라 앱이 죽어 보인다.
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
        return Mission.builder()
                .userId(userId)
                .groupId(group.getId())
                .missionDate(date)
                .seq(seq)
                .type(lifestyle ? MissionType.LIFESTYLE : MissionType.DIET)
                .title(title)
                // 실제 서비스와 같이 하루치를 한 번에 연다(MissionSetCreator도 null을 넣는다).
                // 예전처럼 시간차로 열면 심사위원에게 미션이 1개만 보이고 "다음 미션은 오후 4:00에
                // 도착해요"가 떠서, 생활습관 미션의 사진 인증을 오후까지 시연할 수 없다.
                .unlockTime(null)
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
        List<String> emails = new ArrayList<>(MEMBERS.stream().map(Member::email).toList());
        emails.addAll(RETIRED_EMAILS);
        List<Long> userIds = userRepository.findAll().stream()
                .filter(u -> emails.contains(u.getEmail()))
                .map(User::getId)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        String inClause = userIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElseThrow();

        // 사진 본문(stored_files)은 meals가 '/api/files/{uuid}' 문자열로만 가리킨다(외래키가 아니다).
        // meals를 먼저 지우면 어떤 파일이 고아인지 알 방법이 없어져, 재기동할 때마다 사진이
        // DB에 그대로 쌓인다. 실제로 며칠 만에 고아 사진 8건이 남아 있었다. 먼저 지운다.
        jdbcTemplate.update("DELETE FROM stored_files WHERE id IN ("
                + "SELECT id FROM (SELECT SUBSTRING_INDEX(photo_url, '/', -1) AS id FROM meals "
                + "WHERE user_id IN (" + inClause + ") AND photo_url IS NOT NULL) AS doomed)");

        // 피드는 group_id로만 묶여 있어 사용자 삭제로는 따라 지워지지 않는다. 남겨두면 다음 사이클의
        // 피드에 지난 반응이 섞여 보인다.
        jdbcTemplate.update("DELETE FROM feed_reactions WHERE actor_user_id IN (" + inClause + ")"
                + " OR target_user_id IN (" + inClause + ")");
        jdbcTemplate.update("DELETE FROM feed_comments WHERE author_user_id IN (" + inClause + ")");

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
