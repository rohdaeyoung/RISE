package com.withu.auth.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 10)
    private String nickname;

    @Column(nullable = false)
    private int coins;

    /** 전체 랭킹 기준이 되는 누적 점수 — 코인과 달리 소비되지 않는다 (PRD 10. 랭킹 시스템). */
    @Column(nullable = false)
    private int points;

    @Builder
    private User(String email, String password) {
        this.email = email;
        this.password = password;
        this.coins = 0;
        this.points = 0;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void spendCoins(int amount) {
        this.coins -= amount;
    }
}
