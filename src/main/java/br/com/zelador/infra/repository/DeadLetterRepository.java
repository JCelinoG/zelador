package br.com.zelador.infra.repository;

import br.com.zelador.domain.DeadLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, UUID> {

    Optional<DeadLetter> findByDeliveryIdAndReplayedAtIsNull(UUID deliveryId);

    Page<DeadLetter> findByReplayedAtIsNullOrderByMovedAtDesc(Pageable pageable);

    long countByReplayedAtIsNull();
}