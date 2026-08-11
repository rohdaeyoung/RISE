package com.withu.character.repository;

import com.withu.character.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    Optional<Character> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Character> findByUserIdIn(List<Long> userIds);
}
