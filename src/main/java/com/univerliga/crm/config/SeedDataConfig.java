package com.univerliga.crm.config;

import com.univerliga.crm.model.IdentityStatus;
import com.univerliga.crm.model.PersonEntity;
import com.univerliga.crm.model.PersonRole;
import com.univerliga.crm.model.TaskEntity;
import com.univerliga.crm.model.TaskStatus;
import com.univerliga.crm.repository.PersonRepository;
import com.univerliga.crm.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedData(PersonRepository personRepository, TaskRepository taskRepository) {
        return args -> {
            if (personRepository.count() > 0 || taskRepository.count() > 0) {
                return;
            }

            List<PersonEntity> seedPeople = List.of(
                    person("p_admin", "Admin User", "admin@univerliga.com", PersonRole.ADMIN),
                    person("p_manager", "Manager User", "manager@univerliga.com", PersonRole.MANAGER),
                    person("p_hr", "HR User", "hr@univerliga.com", PersonRole.HR),
                    person("p_employee", "Employee User", "employee@univerliga.com", PersonRole.EMPLOYEE),
                    person("p_101", "Alex Taylor", "alex@univerliga.com", PersonRole.EMPLOYEE),
                    person("p_102", "Maria White", "maria@univerliga.com", PersonRole.EMPLOYEE),
                    person("p_103", "Chris Moore", "chris@univerliga.com", PersonRole.EMPLOYEE),
                    person("p_104", "Elena Scott", "elena@univerliga.com", PersonRole.MANAGER),
                    person("p_105", "Sam Green", "sam@univerliga.com", PersonRole.HR),
                    person("p_106", "Nina Gray", "nina@univerliga.com", PersonRole.EMPLOYEE)
            );
            personRepository.saveAll(seedPeople);

            for (int i = 1; i <= 10; i++) {
                TaskEntity task = new TaskEntity();
                task.setId("task_" + UUID.randomUUID());
                task.setTitle("Seed task " + i);
                task.setDescription("Auto-generated task " + i);
                task.setStatus(i % 3 == 0 ? TaskStatus.ACTIVE : TaskStatus.DRAFT);
                task.setPeriodFrom(LocalDate.now().minusDays(10));
                task.setPeriodTo(LocalDate.now().plusDays(20));
                task.setOwnerId("p_manager");
                task.setAssigneeId("p_employee");
                task.setParticipantIds(Set.of("p_employee", "p_101"));
                taskRepository.save(task);
            }
        };
    }

    private PersonEntity person(String id, String name, String email, PersonRole role) {
        PersonEntity person = new PersonEntity();
        person.setId(id);
        person.setDisplayName(name);
        person.setEmail(email);
        person.setDepartmentId("d_1");
        person.setTeamId("t_1");
        person.setRole(role);
        person.setActive(true);
        person.setIdentityStatus(IdentityStatus.PENDING);
        return person;
    }
}
