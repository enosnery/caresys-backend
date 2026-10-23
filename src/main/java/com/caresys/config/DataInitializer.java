package com.caresys.config;

import com.caresys.model.*;
import com.caresys.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Inicializa os usuários e dados padrão no banco PostgreSQL.
 *
 * Configura usuários com perfil e relacionamento 1:1 com Staff para testes de Least Privilege:
 * - admin (ROLE_ADMIN): visualiza todos os agendamentos da clínica
 * - recepcao (ROLE_RECEPTIONIST): visualiza todos os agendamentos da clínica
 * - camila.alencar (ROLE_PHYSIO): vinculada à Dra. Camila Alencar (vê apenas os seus)
 * - rodrigo.menezes (ROLE_PHYSIO): vinculado ao Dr. Rodrigo Menezes (vê apenas os seus)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HealthPlanRepository healthPlanRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public void run(String... args) {
        final String sha256OfAdmin123 = sha256Hex("admin123");

        System.out.printf("[DataInitializer] Inicializando dados com SHA-256(\"admin123\") = %s%n", sha256OfAdmin123);

        // 1. Usuários
        User adminUser = createOrUpdateUser("admin", "admin@fisicare.com", sha256OfAdmin123, "Administrador da Clínica", "ROLE_ADMIN");
        User recepcaoUser = createOrUpdateUser("recepcao", "recepcao@fisicare.com", sha256OfAdmin123, "Recepção Central", "ROLE_RECEPTIONIST");
        User camilaUser = createOrUpdateUser("camila.alencar", "camila.alencar@fisicare.com", sha256OfAdmin123, "Dra. Camila Alencar", "ROLE_PHYSIO");
        User rodrigoUser = createOrUpdateUser("rodrigo.menezes", "rodrigo.menezes@fisicare.com", sha256OfAdmin123, "Dr. Rodrigo Menezes", "ROLE_PHYSIO");

        // 2. Planos de Saúde (caso não existam)
        HealthPlan unimed = healthPlanRepository.findAll().stream()
                .filter(p -> "UNI-01".equals(p.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    HealthPlan p = new HealthPlan();
                    p.setName("Unimed Seguros");
                    p.setCode("UNI-01");
                    p.setCopayRate(new BigDecimal("20.00"));
                    p.setContactPhone("(11) 3222-1000");
                    return healthPlanRepository.save(p);
                });

        HealthPlan bradesco = healthPlanRepository.findAll().stream()
                .filter(p -> "BRAD-02".equals(p.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    HealthPlan p = new HealthPlan();
                    p.setName("Bradesco Saúde");
                    p.setCode("BRAD-02");
                    p.setCopayRate(new BigDecimal("15.00"));
                    p.setContactPhone("(11) 3333-2000");
                    return healthPlanRepository.save(p);
                });

        // 3. Colaboradores (Staff) vinculados aos Usuários (Relação 1:1)
        Staff staffCamila = staffRepository.findByUserUsername("camila.alencar").orElseGet(() -> {
            Staff s = staffRepository.findAll().stream()
                    .filter(st -> st.getName().contains("Camila"))
                    .findFirst()
                    .orElseGet(Staff::new);
            s.setName("Dra. Camila Alencar");
            s.setRole("Fisioterapeuta Traumato-Ortopédica");
            s.setPhone("(11) 98765-4321");
            s.setEmail("camila.alencar@fisicare.com");
            s.setUser(camilaUser);
            s.setActive(true);
            return staffRepository.save(s);
        });
        if (staffCamila.getUser() == null) {
            staffCamila.setUser(camilaUser);
            staffRepository.save(staffCamila);
        }

        Staff staffRodrigo = staffRepository.findByUserUsername("rodrigo.menezes").orElseGet(() -> {
            Staff s = staffRepository.findAll().stream()
                    .filter(st -> st.getName().contains("Rodrigo"))
                    .findFirst()
                    .orElseGet(Staff::new);
            s.setName("Dr. Rodrigo Menezes");
            s.setRole("Fisioterapeuta Neurofuncional");
            s.setPhone("(11) 97654-3210");
            s.setEmail("rodrigo.menezes@fisicare.com");
            s.setUser(rodrigoUser);
            s.setActive(true);
            return staffRepository.save(s);
        });
        if (staffRodrigo.getUser() == null) {
            staffRodrigo.setUser(rodrigoUser);
            staffRepository.save(staffRodrigo);
        }

        Staff staffMariana = staffRepository.findAll().stream()
                .filter(st -> st.getName().contains("Mariana"))
                .findFirst()
                .orElseGet(() -> {
                    Staff s = new Staff();
                    s.setName("Dra. Mariana Rios");
                    s.setRole("Fisioterapeuta Respiratória & Pilates");
                    s.setPhone("(11) 96543-2109");
                    s.setEmail("mariana.rios@fisicare.com");
                    s.setActive(true);
                    return staffRepository.save(s);
                });

        Staff staffLucas = staffRepository.findAll().stream()
                .filter(st -> st.getName().contains("Lucas"))
                .findFirst()
                .orElseGet(() -> {
                    Staff s = new Staff();
                    s.setName("Dr. Lucas Ferreira");
                    s.setRole("Fisioterapeuta Desportivo");
                    s.setPhone("(11) 95432-1098");
                    s.setEmail("lucas.ferreira@fisicare.com");
                    s.setActive(true);
                    return staffRepository.save(s);
                });

        // 4. Pacientes (caso não existam)
        Patient patient1 = patientRepository.findAll().stream()
                .filter(p -> p.getName().contains("Arthur"))
                .findFirst()
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setName("Arthur Vinícius Ramos");
                    p.setPhone("(11) 98111-2233");
                    p.setAge(34);
                    p.setWeight(new BigDecimal("78.50"));
                    p.setHeight(new BigDecimal("1.78"));
                    p.setChiefComplaint("Lombalgia crônica irradiando para glúteo esquerdo após esforço físico.");
                    p.setHasHealthPlan(true);
                    p.setHealthPlan(unimed);
                    p.setStaff(staffCamila);
                    return patientRepository.save(p);
                });
        if (patient1.getStaff() == null) {
            patient1.setStaff(staffCamila);
            patientRepository.save(patient1);
        }

        Patient patient2 = patientRepository.findAll().stream()
                .filter(p -> p.getName().contains("Beatriz"))
                .findFirst()
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setName("Beatriz Vasconcelos");
                    p.setPhone("(11) 98222-3344");
                    p.setAge(28);
                    p.setWeight(new BigDecimal("62.00"));
                    p.setHeight(new BigDecimal("1.65"));
                    p.setChiefComplaint("Pós-operatório de LCA e menisco medial no joelho direito há 4 semanas.");
                    p.setHasHealthPlan(false);
                    p.setStaff(staffRodrigo);
                    return patientRepository.save(p);
                });
        if (patient2.getStaff() == null) {
            patient2.setStaff(staffRodrigo);
            patientRepository.save(patient2);
        }

        Patient patient3 = patientRepository.findAll().stream()
                .filter(p -> p.getName().contains("Carlos"))
                .findFirst()
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setName("Carlos Eduardo Nogueira");
                    p.setPhone("(11) 98333-4455");
                    p.setAge(52);
                    p.setWeight(new BigDecimal("84.00"));
                    p.setHeight(new BigDecimal("1.74"));
                    p.setChiefComplaint("Bursite e tendinopatia do supraespinhal no ombro direito.");
                    p.setHasHealthPlan(true);
                    p.setHealthPlan(bradesco);
                    p.setStaff(staffCamila);
                    return patientRepository.save(p);
                });
        if (patient3.getStaff() == null) {
            patient3.setStaff(staffCamila);
            patientRepository.save(patient3);
        }

        // 5. Agendamentos para o dia atual (testes imediatos na Agenda)
        LocalDate today = LocalDate.now();

        if (appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(today).isEmpty()) {
            // Consulta para Dra. Camila (Arthur)
            Appointment a1 = new Appointment();
            a1.setPatient(patient1);
            a1.setStaff(staffCamila);
            a1.setAppointmentDate(today);
            a1.setAppointmentTime(LocalTime.of(8, 0));
            a1.setDurationMinutes(50);
            a1.setActionsForDay("Liberação miofascial lombar e exercícios com bola suíça para core.");
            a1.setStatus("completed");
            a1.setNotes("Paciente compareceu pontualmente.");
            appointmentRepository.save(a1);

            // Outra consulta para Dra. Camila (Carlos)
            Appointment a2 = new Appointment();
            a2.setPatient(patient3);
            a2.setStaff(staffCamila);
            a2.setAppointmentDate(today);
            a2.setAppointmentTime(LocalTime.of(10, 0));
            a2.setDurationMinutes(50);
            a2.setActionsForDay("Laserterapia no manguito rotador + rotação externa com elástico amarelo.");
            a2.setStatus("scheduled");
            a2.setNotes("Reavaliar dor na abdução.");
            appointmentRepository.save(a2);

            // Consulta para Dr. Rodrigo (Eduardo)
            Appointment a3 = new Appointment();
            a3.setPatient(patient2);
            a3.setStaff(staffRodrigo);
            a3.setAppointmentDate(today);
            a3.setAppointmentTime(LocalTime.of(9, 0));
            a3.setDurationMinutes(50);
            a3.setActionsForDay("Treino proprioceptivo unipodal no disco de equilíbrio + isometria de quadríceps.");
            a3.setStatus("in_progress");
            a3.setNotes("Pós-LCA recente.");
            appointmentRepository.save(a3);

            // Consulta para Dr. Lucas
            Appointment a4 = new Appointment();
            a4.setPatient(patient2);
            a4.setStaff(staffLucas);
            a4.setAppointmentDate(today);
            a4.setAppointmentTime(LocalTime.of(14, 0));
            a4.setDurationMinutes(50);
            a4.setActionsForDay("Crioterapia compressiva e mobilização patelar.");
            a4.setStatus("scheduled");
            appointmentRepository.save(a4);

            System.out.println("[DataInitializer] Agendamentos de teste inseridos para hoje (" + today + ").");
        }
    }

    private User createOrUpdateUser(String username, String email, String sha256Hash,
                                    String name, String role) {
        String bcryptHash = passwordEncoder.encode(sha256Hash);

        return userRepository.findByUsername(username).map(user -> {
            user.setPasswordHash(bcryptHash);
            user.setRole(role);
            user.setName(name);
            user.setEmail(email);
            return userRepository.save(user);
        }).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(bcryptHash);
            user.setName(name);
            user.setRole(role);
            user.setActive(true);
            return userRepository.save(user);
        });
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String byteHex = Integer.toHexString(0xff & b);
                if (byteHex.length() == 1) hex.append('0');
                hex.append(byteHex);
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
