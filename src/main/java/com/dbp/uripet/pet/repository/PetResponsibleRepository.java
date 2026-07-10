package com.dbp.uripet.pet.repository;

import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.domain.PetResponsible;
import com.dbp.uripet.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PetResponsibleRepository extends JpaRepository<PetResponsible, Long> {

    @Query("SELECT pr FROM PetResponsible pr JOIN FETCH pr.pet WHERE pr.user = :user")
    List<PetResponsible> findByUser(User user);

    @Query(
            value = "SELECT pr FROM PetResponsible pr JOIN FETCH pr.pet WHERE pr.user = :user",
            countQuery = "SELECT COUNT(pr) FROM PetResponsible pr WHERE pr.user = :user"
    )
    Page<PetResponsible> findByUser(User user, Pageable pageable);

    @Query(
            value = "SELECT pr FROM PetResponsible pr JOIN FETCH pr.pet WHERE pr.user = :user AND LOWER(pr.pet.name) LIKE LOWER(CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(pr) FROM PetResponsible pr WHERE pr.user = :user AND LOWER(pr.pet.name) LIKE LOWER(CONCAT('%', :search, '%'))"
    )
    Page<PetResponsible> findByUserAndPetNameContainingIgnoreCase(
            User user,
            String search,
            Pageable pageable
    );

    @Query("SELECT pr FROM PetResponsible pr JOIN FETCH pr.user WHERE pr.pet = :pet")
    List<PetResponsible> findByPet(Pet pet);

    @Query("SELECT pr FROM PetResponsible pr JOIN FETCH pr.user WHERE pr.pet = :pet AND pr.user = :user")
    Optional<PetResponsible> findByPetAndUser(Pet pet, User user);

    boolean existsByPetAndUser(Pet pet, User user);

    int countByPetAndResponsibleRole(Pet pet, com.dbp.uripet.pet.domain.enums.ResponsibleRole role);
}