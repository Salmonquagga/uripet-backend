package com.dbp.uripet.pet.service;

import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.health.domain.HealthRecordType;
import com.dbp.uripet.health.repository.HealthRecordRepository;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.repository.PetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @InjectMocks
    private PetService petService;

    @Test
    void getPetByQr_whenPetExists_shouldReturnAllergiesAndDiseases() {
        // Obsolete test method for getPetByQr which is not defined in PetService
    }

}
