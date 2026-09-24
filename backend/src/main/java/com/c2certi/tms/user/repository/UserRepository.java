package com.c2certi.tms.user.repository;

import com.c2certi.tms.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsernameIgnoreCase(String username);

  Optional<User> findByIdAndActiveTrue(Long id);

  List<User> findAllByActiveTrueOrderByDisplayNameAsc();

  boolean existsByUsernameIgnoreCase(String username);
}
