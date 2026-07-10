package com.dbp.uripet.jpa;

import com.dbp.uripet.auth.domain.QrLoginSession;
import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.domain.PetResponsible;
import com.dbp.uripet.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JpaRelationshipTest {

    @Test
    void testUserRelationships() throws NoSuchFieldException {
        // responsibilities
        Field responsibilitiesField = User.class.getDeclaredField("responsibilities");
        OneToMany oneToMany = responsibilitiesField.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany, "responsibilities should be annotated with @OneToMany");
        assertEquals("user", oneToMany.mappedBy());
        assertEquals(FetchType.LAZY, oneToMany.fetch());
        assertTrue(Arrays.asList(oneToMany.cascade()).contains(CascadeType.ALL));

        // healthRecords
        Field healthRecordsField = User.class.getDeclaredField("healthRecords");
        OneToMany healthOneToMany = healthRecordsField.getAnnotation(OneToMany.class);
        assertNotNull(healthOneToMany, "healthRecords should be annotated with @OneToMany");
        assertEquals("createdBy", healthOneToMany.mappedBy());
        assertEquals(FetchType.LAZY, healthOneToMany.fetch());
        assertTrue(Arrays.asList(healthOneToMany.cascade()).contains(CascadeType.ALL));

        // qrLoginSessions
        Field qrLoginSessionsField = User.class.getDeclaredField("qrLoginSessions");
        OneToMany qrOneToMany = qrLoginSessionsField.getAnnotation(OneToMany.class);
        assertNotNull(qrOneToMany, "qrLoginSessions should be annotated with @OneToMany");
        assertEquals("user", qrOneToMany.mappedBy());
        assertEquals(FetchType.LAZY, qrOneToMany.fetch());
        assertTrue(Arrays.asList(qrOneToMany.cascade()).contains(CascadeType.ALL));
    }

    @Test
    void testPetRelationships() throws NoSuchFieldException {
        // responsibilities
        Field responsibilitiesField = Pet.class.getDeclaredField("responsibilities");
        OneToMany oneToMany = responsibilitiesField.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany, "responsibilities should be annotated with @OneToMany");
        assertEquals("pet", oneToMany.mappedBy());
        assertEquals(FetchType.LAZY, oneToMany.fetch());
        assertTrue(Arrays.asList(oneToMany.cascade()).contains(CascadeType.ALL));

        // healthRecords
        Field healthRecordsField = Pet.class.getDeclaredField("healthRecords");
        OneToMany healthOneToMany = healthRecordsField.getAnnotation(OneToMany.class);
        assertNotNull(healthOneToMany, "healthRecords should be annotated with @OneToMany");
        assertEquals("pet", healthOneToMany.mappedBy());
        assertEquals(FetchType.LAZY, healthOneToMany.fetch());
        assertTrue(Arrays.asList(healthOneToMany.cascade()).contains(CascadeType.ALL));
    }

    @Test
    void testPetResponsibleRelationships() throws NoSuchFieldException {
        // pet
        Field petField = PetResponsible.class.getDeclaredField("pet");
        ManyToOne manyToOnePet = petField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOnePet, "pet should be annotated with @ManyToOne");
        assertEquals(FetchType.LAZY, manyToOnePet.fetch());

        // user
        Field userField = PetResponsible.class.getDeclaredField("user");
        ManyToOne manyToOneUser = userField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOneUser, "user should be annotated with @ManyToOne");
        assertEquals(FetchType.LAZY, manyToOneUser.fetch());
    }

    @Test
    void testHealthRecordRelationships() throws NoSuchFieldException {
        // pet
        Field petField = HealthRecord.class.getDeclaredField("pet");
        ManyToOne manyToOnePet = petField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOnePet, "pet should be annotated with @ManyToOne");
        assertEquals(FetchType.LAZY, manyToOnePet.fetch());

        // createdBy
        Field createdByField = HealthRecord.class.getDeclaredField("createdBy");
        ManyToOne manyToOneCreatedBy = createdByField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOneCreatedBy, "createdBy should be annotated with @ManyToOne");
        assertEquals(FetchType.LAZY, manyToOneCreatedBy.fetch());
    }

    @Test
    void testQrLoginSessionRelationships() throws NoSuchFieldException {
        // user
        Field userField = QrLoginSession.class.getDeclaredField("user");
        ManyToOne manyToOneUser = userField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOneUser, "user should be annotated with @ManyToOne");
        assertEquals(FetchType.LAZY, manyToOneUser.fetch());
    }
}
