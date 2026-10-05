package com.bancoxyz.core.repository;

import com.bancoxyz.core.model.EstadoCuentaAnual;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EstadoCuentaAnualRepository extends JpaRepository<EstadoCuentaAnual, Long> {
    List<EstadoCuentaAnual> findByCuentaIdOrderByFechaDesc(Long cuentaId);
    List<EstadoCuentaAnual> findByCuentaIdAndFechaBetweenOrderByFechaDesc(Long cuentaId, LocalDate desde, LocalDate hasta);
}
