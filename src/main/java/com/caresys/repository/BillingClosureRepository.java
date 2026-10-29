package com.caresys.repository;

import com.caresys.model.BillingClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingClosureRepository extends JpaRepository<BillingClosure, Long> {
    List<BillingClosure> findAllByOrderByClosedAtDesc();
}
