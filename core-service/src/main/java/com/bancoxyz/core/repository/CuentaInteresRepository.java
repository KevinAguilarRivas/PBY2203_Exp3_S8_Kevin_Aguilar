package com.bancoxyz.core.repository;

import com.bancoxyz.core.model.CuentaInteres;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CuentaInteresRepository extends JpaRepository<CuentaInteres, Long> {
    Optional<CuentaInteres> findFirstByCuentaIdOrderByIdDesc(Long cuentaId);
}
