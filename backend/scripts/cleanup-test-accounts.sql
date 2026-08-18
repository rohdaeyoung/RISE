-- 검증용으로 만든 계정을 정리한다.
--
-- 왜 이 파일이 필요한가: 계정 삭제 API(DELETE /api/auth/me)는 본인 로그인이 필요한데,
-- 검증하며 만든 계정은 이메일이 임의 문자열이라 다시 로그인할 수 없다. 관리자용
-- 삭제 경로는 일부러 만들지 않았으므로(심사 직전에 공격 면을 늘리고 싶지 않다) DB에서 지운다.
--
-- 사용법
--   1) STEP 1을 실행해 남길 계정을 눈으로 확인한다.
--   2) STEP 2의 @keep 목록을 실제로 남길 이메일로 고친다.
--   3) STEP 3을 실행한다.
--
-- 되돌릴 수 없다. STEP 1을 건너뛰지 말 것.


-- ────────────────────────────────────────────────────────────
-- STEP 1. 지금 어떤 계정이 있는지 본다 (지우지 않음)
-- ────────────────────────────────────────────────────────────
SELECT u.id,
       u.email,
       u.nickname,
       DATE(u.created_at) AS 가입일,
       (SELECT COUNT(*) FROM missions m WHERE m.user_id = u.id) AS 미션수
FROM users u
ORDER BY u.id;


-- ────────────────────────────────────────────────────────────
-- STEP 2. 남길 계정을 정한다
--
-- 데모 시더 계정(테스터·민준·서연·수아)은 서버가 뜰 때마다 다시 만들어지므로
-- 지워도 복구되지만, 심사 직전에 굳이 흔들 이유가 없어 목록에 넣어둔다.
-- 팀원 본인 계정은 STEP 1 결과를 보고 직접 채워 넣을 것.
-- ────────────────────────────────────────────────────────────
SET @keep = CONCAT(
  'test@withu.app',        -- 심사용 데모 계정 (제출 서류에 적은 것)
  ',demo-minjun@withu.app',
  ',demo-seoyeon@withu.app',
  ',demo-sua@withu.app'
  -- ,',여기에 팀원 계정 이메일'   ← STEP 1을 보고 추가
);


-- ────────────────────────────────────────────────────────────
-- STEP 3. 남길 계정을 뺀 나머지를 지운다
--
-- 자식 행부터 지운다. users를 먼저 지우면 외래키에 걸리거나 고아 행이 남는다.
-- ────────────────────────────────────────────────────────────
-- 임시(TEMPORARY) 테이블이 아니라 일반 테이블로 만든다. MySQL은 임시 테이블을 한 문장에서
-- 두 번 참조하지 못해서(예: feed_reactions는 actor/target 두 컬럼을 함께 봐야 한다)
-- "Can't reopen table"로 실패한다. 맨 끝에서 지운다.
DROP TABLE IF EXISTS doomed;
CREATE TABLE doomed AS
SELECT u.id
FROM users u
WHERE FIND_IN_SET(u.email, @keep) = 0;

-- 사진 본문은 stored_files에 있는데, 미션/식단은 그 id를 '/api/files/{uuid}' 형태의
-- 주소 문자열로만 들고 있다(외래키가 아니다). 행이 지워지면 어떤 파일이 고아인지 알 수 없으므로
-- 지우기 전에 uuid만 뽑아 모아둔다. 이걸 빼먹으면 사진 BLOB이 DB에 영원히 남는다.
DROP TABLE IF EXISTS doomed_files;
CREATE TABLE doomed_files (id VARCHAR(255));

INSERT INTO doomed_files
SELECT SUBSTRING_INDEX(photo_url, '/', -1)
  FROM missions WHERE user_id IN (SELECT id FROM doomed) AND photo_url IS NOT NULL;

INSERT INTO doomed_files
SELECT SUBSTRING_INDEX(photo_url, '/', -1)
  FROM meals    WHERE user_id IN (SELECT id FROM doomed) AND photo_url IS NOT NULL;

DELETE FROM feed_reactions
 WHERE actor_user_id IN (SELECT id FROM doomed)
    OR target_user_id IN (SELECT id FROM doomed);
DELETE FROM feed_comments      WHERE author_user_id IN (SELECT id FROM doomed);
DELETE FROM challenge_results  WHERE user_id IN (SELECT id FROM doomed);
DELETE FROM user_badges        WHERE user_id IN (SELECT id FROM doomed);
DELETE FROM missions           WHERE user_id IN (SELECT id FROM doomed);
DELETE FROM meals              WHERE user_id IN (SELECT id FROM doomed);
DELETE FROM onboardings        WHERE user_id IN (SELECT id FROM doomed);
DELETE FROM group_members      WHERE user_id IN (SELECT id FROM doomed);

DELETE FROM character_owned_outfits
 WHERE character_id IN (SELECT id FROM characters WHERE user_id IN (SELECT id FROM doomed));
DELETE FROM characters         WHERE user_id IN (SELECT id FROM doomed);

DELETE FROM stored_files WHERE id IN (SELECT id FROM doomed_files);

DELETE FROM users WHERE id IN (SELECT id FROM doomed);

-- 주인이 없는 행을 함께 치운다.
--
-- 과거에 users만 직접 지운 적이 있으면 자식 행이 그대로 남는다(외래키가 걸려 있지 않다).
-- 이걸 안 치우면 바로 아래 "빈 방 정리"가 헛돈다 — 이미 사라진 사람의 group_members 행이
-- 남아 있어서, 아무도 없는 방이 여전히 "그룹원이 있는 방"으로 보이기 때문이다.
DELETE FROM feed_reactions     WHERE actor_user_id  NOT IN (SELECT id FROM users);
DELETE FROM feed_reactions     WHERE target_user_id NOT IN (SELECT id FROM users);
DELETE FROM feed_comments      WHERE author_user_id NOT IN (SELECT id FROM users);
DELETE FROM challenge_results  WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM user_badges        WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM missions           WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM meals              WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM onboardings        WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM group_members      WHERE user_id NOT IN (SELECT id FROM users);
-- character_owned_outfits → characters 는 외래키가 실제로 걸려 있는 유일한 곳이다.
-- 캐릭터를 먼저 지우려 하면 제약에 막히므로, 지울 캐릭터의 의상 행부터 없앤다.
DELETE FROM character_owned_outfits
 WHERE character_id IN (SELECT id FROM characters WHERE user_id NOT IN (SELECT id FROM users));
DELETE FROM character_owned_outfits
 WHERE character_id NOT IN (SELECT id FROM characters);
DELETE FROM characters         WHERE user_id NOT IN (SELECT id FROM users);

-- 그룹원이 아무도 남지 않은 방은 같이 정리한다. 남겨두면 전체 랭킹에 빈 방이 계속 뜬다.
DELETE FROM study_groups
 WHERE id NOT IN (SELECT DISTINCT group_id FROM group_members);

DROP TABLE doomed_files;
DROP TABLE doomed;


-- ────────────────────────────────────────────────────────────
-- STEP 4. 남은 계정 확인
-- ────────────────────────────────────────────────────────────
SELECT u.id, u.email, u.nickname FROM users u ORDER BY u.id;
