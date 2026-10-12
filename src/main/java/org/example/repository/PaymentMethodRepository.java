package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findAllByOrderByIdAsc();

    Optional<PaymentMethod> findByMethodCode(String methodCode);
}
