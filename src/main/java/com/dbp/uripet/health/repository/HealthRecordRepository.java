package com.dbp.uripet.health.repository;

import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.health.domain.HealthRecordType;
import com.dbp.uripet.pet.domain.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByPet(Pet pet);

    List<HealthRecord> findByPetAndType(Pet pet, HealthRecordType type);

    Page<HealthRecord> findByPet(Pet pet, Pageable pageable);

    Page<HealthRecord> findByPetAndType(Pet pet, HealthRecordType type, Pageable pageable);
}