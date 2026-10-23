package com.caresys.repository;

import com.caresys.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findAllByOrderByIdAsc();
    List<Staff> findByActiveTrue();

    @Query("SELECT s FROM Staff s WHERE s.user.id = :userId")
    Optional<Staff> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Staff s WHERE s.user.username = :username")
    Optional<Staff> findByUserUsername(@Param("username") String username);

    boolean existsByCrefito(String crefito);
}

