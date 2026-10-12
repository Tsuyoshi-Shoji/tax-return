package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.ReceiveMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiveMethodRepository extends JpaRepository<ReceiveMethod, Long> {
    List<ReceiveMethod> findAllByOrderByIdAsc();

    Optional<ReceiveMethod> findByMethodCode(String methodCode);
}
