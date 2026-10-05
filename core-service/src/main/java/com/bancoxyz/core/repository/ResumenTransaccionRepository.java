package com.bancoxyz.core.repository;

import com.bancoxyz.core.model.ResumenTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ResumenTransaccionRepository extends JpaRepository<ResumenTransaccion, Long> {
    List<ResumenTransaccion> findByFecha(LocalDate fecha);
}
