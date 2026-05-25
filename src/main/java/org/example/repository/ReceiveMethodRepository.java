package org.example.repository;

import org.example.entity.ReceiveMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiveMethodRepository extends JpaRepository<ReceiveMethod, Long> {

    List<ReceiveMethod> findAllByStatusOrderByDisplayOrderAsc(String status);
}

