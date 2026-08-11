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

    @Builder
    private User(String email, String password) {
        this.email = email;
        this.password = password;
        this.coins = 0;
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
