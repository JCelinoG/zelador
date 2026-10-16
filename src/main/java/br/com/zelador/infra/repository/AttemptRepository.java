package br.com.zelador.infra.repository;

import br.com.zelador.domain.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    List<Attempt> findByDeliveryIdOrderByAttemptNumberAsc(UUID deliveryId);
}