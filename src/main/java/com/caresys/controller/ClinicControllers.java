package com.caresys.controller;

import com.caresys.model.*;
import com.caresys.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping
@SecurityRequirement(name = "Bearer Authentication")
public class ClinicControllers {

    /**
     * Verifica se o usuário autenticado possui perfil restrito de Fisioterapeuta (ROLE_PHYSIO),
     * ou seja, não é Administrador (ROLE_ADMIN) nem Recepção (ROLE_RECEPTIONIST).
     */
    public static boolean isPhysioRestricted(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST"));
    }

    // =========================================================================
    // 1. PACIENTES
    // =========================================================================
    @RestController
    @RequestMapping("/patients")
    @Tag(name = "2. Pacientes", description = "CRUD de pacientes com biometria e flag de convênio")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class PatientController {

        @Autowired
        private PatientRepository patientRepository;

        @Autowired
        private HealthPlanRepository healthPlanRepository;

        @Autowired
        private StaffRepository staffRepository;

        @GetMapping
        @Operation(summary = "Listar pacientes (ROLE_PHYSIO visualiza apenas pacientes vinculados ao seu colaborador)")
        public List<Patient> getAll(
                @RequestParam(required = false) String search,
                org.springframework.security.core.Authentication authentication) {

            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return List.of();
                }
                List<Patient> linked = patientRepository.findLinkedToStaff(currentStaff.getId());
                if (search != null && !search.isBlank()) {
                    String q = search.trim().toLowerCase();
                    return linked.stream()
                            .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(q))
                            .collect(Collectors.toList());
                }
                return linked;
            }

            if (search != null && !search.isBlank()) {
                return patientRepository.findByNameContainingIgnoreCase(search.trim());
            }
            return patientRepository.findAllByOrderByIdAsc();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Buscar paciente por ID")
        public ResponseEntity<Patient> getById(@PathVariable Long id) {
            return patientRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping
        @Operation(summary = "Criar novo paciente com validação de biometria e convênio")
        public ResponseEntity<?> create(
                @RequestBody Patient patient,
                org.springframework.security.core.Authentication authentication) {
            if (patient.getName() == null || patient.getName().trim().length() < 3) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nome do paciente deve conter ao menos 3 caracteres."));
            }
            if (patient.getAge() == null || patient.getAge() < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Idade do paciente é obrigatória e não pode ser negativa."));
            }
            if (patient.getWeight() == null || patient.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Peso do paciente deve ser maior que zero."));
            }
            if (patient.getHeight() == null || patient.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Altura do paciente deve ser maior que zero."));
            }
            if (patient.getChiefComplaint() == null || patient.getChiefComplaint().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Queixa principal do paciente é obrigatória."));
            }

            if (Boolean.TRUE.equals(patient.getHasHealthPlan()) && patient.getHealthPlan() != null && patient.getHealthPlan().getId() != null) {
                HealthPlan hp = healthPlanRepository.findById(patient.getHealthPlan().getId()).orElse(null);
                patient.setHealthPlan(hp);
            } else if (!Boolean.TRUE.equals(patient.getHasHealthPlan())) {
                patient.setHealthPlan(null);
            }

            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff != null) {
                    patient.setStaff(currentStaff);
                }
            } else if (patient.getStaff() != null && patient.getStaff().getId() != null) {
                Staff s = staffRepository.findById(patient.getStaff().getId()).orElse(null);
                patient.setStaff(s);
            } else {
                patient.setStaff(null);
            }

            return ResponseEntity.ok(patientRepository.save(patient));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar dados do paciente")
        public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Patient updated) {
            return patientRepository.findById(id).map(patient -> {
                if (updated.getName() != null && updated.getName().trim().length() >= 3) {
                    patient.setName(updated.getName().trim());
                }
                if (updated.getPhone() != null) patient.setPhone(updated.getPhone());
                if (updated.getAge() != null && updated.getAge() >= 0) patient.setAge(updated.getAge());
                if (updated.getWeight() != null && updated.getWeight().compareTo(BigDecimal.ZERO) > 0) patient.setWeight(updated.getWeight());
                if (updated.getHeight() != null && updated.getHeight().compareTo(BigDecimal.ZERO) > 0) patient.setHeight(updated.getHeight());
                if (updated.getChiefComplaint() != null && !updated.getChiefComplaint().isBlank()) patient.setChiefComplaint(updated.getChiefComplaint());

                if (updated.getHasHealthPlan() != null) {
                    patient.setHasHealthPlan(updated.getHasHealthPlan());
                    if (Boolean.TRUE.equals(updated.getHasHealthPlan()) && updated.getHealthPlan() != null && updated.getHealthPlan().getId() != null) {
                        healthPlanRepository.findById(updated.getHealthPlan().getId()).ifPresent(patient::setHealthPlan);
                    } else if (Boolean.FALSE.equals(updated.getHasHealthPlan())) {
                        patient.setHealthPlan(null);
                    }
                }

                if (updated.getStaff() != null) {
                    if (updated.getStaff().getId() != null) {
                        staffRepository.findById(updated.getStaff().getId()).ifPresent(patient::setStaff);
                    } else {
                        patient.setStaff(null);
                    }
                }

                return ResponseEntity.ok(patientRepository.save(patient));
            }).orElse(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Excluir paciente")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            if (patientRepository.existsById(id)) {
                patientRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("message", "Paciente excluído com sucesso."));
            }
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Regra de Negócio: Ao concluir um atendimento, gera automaticamente o registro financeiro (BillingRecord)
     * vinculado ao atendimento, paciente, profissional e plano de saúde (ou particular).
     */
    public static BillingRecord generateAutomaticBillingForAppointment(
            Appointment appointment,
            BillingRecordRepository billingRepository) {
        if (appointment == null || appointment.getId() == null) {
            return null;
        }
        if (billingRepository.existsByAppointmentId(appointment.getId())) {
            return null;
        }

        Patient patient = appointment.getPatient();
        Staff staff = appointment.getStaff();
        if (patient == null || staff == null) {
            return null;
        }

        BillingRecord billing = new BillingRecord();
        billing.setAppointment(appointment);
        billing.setPatient(patient);
        billing.setStaff(staff);
        billing.setBillingDate(appointment.getAppointmentDate() != null ? appointment.getAppointmentDate() : LocalDate.now());
        billing.setBillingTime(appointment.getAppointmentTime() != null ? appointment.getAppointmentTime() : LocalTime.now());

        String procedureDesc = appointment.getActionsForDay();
        if (procedureDesc == null || procedureDesc.isBlank()) {
            procedureDesc = "Sessão de Fisioterapia";
        } else if (procedureDesc.length() > 95) {
            procedureDesc = procedureDesc.substring(0, 95) + "...";
        }
        billing.setProcedureCode(procedureDesc);

        boolean hasPlan = Boolean.TRUE.equals(patient.getHasHealthPlan()) && patient.getHealthPlan() != null;
        billing.setIsHealthPlan(hasPlan);

        if (hasPlan) {
            HealthPlan hp = patient.getHealthPlan();
            billing.setHealthPlan(hp);
            billing.setHealthPlanName(hp.getName());
            BigDecimal baseValue = new BigDecimal("135.00");
            billing.setValue(baseValue);

            BigDecimal copayRate = hp.getCopayRate() != null ? hp.getCopayRate() : BigDecimal.ZERO;
            BigDecimal copayVal = baseValue.multiply(copayRate)
                    .divide(new BigDecimal("100.00"), 2, java.math.RoundingMode.HALF_UP);
            billing.setCopayValue(copayVal);
            billing.setPaymentMethod("Guia TISS");
            billing.setInvoiceNumber(String.format("FAT-%d-%04d", LocalDate.now().getYear(),
                    (appointment.getId() * 100 + (System.currentTimeMillis() % 100)) % 10000));
            billing.setBillingStatus("pending");
        } else {
            billing.setHealthPlan(null);
            billing.setHealthPlanName("Particular");
            billing.setValue(new BigDecimal("160.00"));
            billing.setCopayValue(BigDecimal.ZERO);
            billing.setPaymentMethod("Pix");
            billing.setInvoiceNumber(String.format("REC-%d-%04d", LocalDate.now().getYear(),
                    (appointment.getId() * 100 + (System.currentTimeMillis() % 100)) % 10000));
            billing.setBillingStatus("pending");
        }

        return billingRepository.save(billing);
    }

    // =========================================================================
    // 2. PRONTUÁRIOS (APPEND-ONLY 1:1)
    // =========================================================================
    @RestController
    @RequestMapping("/medical-records")
    @Tag(name = "3. Prontuários", description = "Histórico cumulativo append-only do paciente")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class MedicalRecordController {

        @Autowired
        private MedicalRecordRepository recordRepository;

        @Autowired
        private PatientRepository patientRepository;

        @Autowired
        private StaffRepository staffRepository;

        @Autowired
        private AppointmentRepository appointmentRepository;

        @Autowired
        private BillingRecordRepository billingRepository;

        @GetMapping("/patient/{patientId}")
        @Operation(summary = "Obter histórico cronológico de prontuário do paciente")
        public List<MedicalRecord> getPatientHistory(
                @PathVariable Long patientId,
                org.springframework.security.core.Authentication authentication) {
            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return List.of();
                }
                return recordRepository.findByPatientIdAndStaffIdOrderByEvolutionDateDesc(patientId, currentStaff.getId());
            }
            return recordRepository.findByPatientIdOrderByEvolutionDateDesc(patientId);
        }

        @PostMapping("/patient/{patientId}")
        @Operation(summary = "Adicionar nova evolução ao prontuário (Append-only) e concluir atendimento vinculado")
        public ResponseEntity<?> addRecord(
                @PathVariable Long patientId,
                @RequestParam(required = false) Long appointmentId,
                @RequestBody MedicalRecord record,
                org.springframework.security.core.Authentication authentication) {

            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Paciente não encontrado."));
            }

            if (record.getPhysicalEvolution() == null || record.getPhysicalEvolution().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "A evolução física e funcional é obrigatória."));
            }
            if (record.getTreatmentConducted() == null || record.getTreatmentConducted().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "A conduta e tratamento realizado são obrigatórios."));
            }

            // Identificar profissional de atendimento (respeitando relação 1:1 para ROLE_PHYSIO)
            Staff staff = null;
            if (isPhysioRestricted(authentication)) {
                staff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (staff == null) {
                    return ResponseEntity.status(403).body(Map.of("message", "Seu usuário de Fisioterapeuta não está vinculado a nenhum colaborador."));
                }
            } else {
                if (record.getStaff() != null && record.getStaff().getId() != null) {
                    staff = staffRepository.findById(record.getStaff().getId()).orElse(null);
                }
                if (staff == null && authentication != null) {
                    staff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                }
                if (staff == null) {
                    staff = staffRepository.findByActiveTrue().stream().findFirst().orElse(null);
                }
            }

            if (staff == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nenhum profissional responsável associado."));
            }

            record.setPatient(patient);
            record.setStaff(staff);
            if (record.getEvolutionDate() == null) {
                record.setEvolutionDate(Instant.now());
            }

            MedicalRecord savedRecord = recordRepository.save(record);

            // Regra de negócio: Concluir atendimento vinculado ao adicionar prontuário e gerar registro financeiro
            Appointment targetAppointment = null;
            if (appointmentId != null) {
                targetAppointment = appointmentRepository.findById(appointmentId).orElse(null);
            } else {
                List<Appointment> todayApts = appointmentRepository.findActiveByDateAndPatient(LocalDate.now(), patientId);
                targetAppointment = todayApts.stream()
                        .filter(a -> "in_progress".equalsIgnoreCase(a.getStatus()))
                        .findFirst()
                        .orElseGet(() -> todayApts.stream()
                                .filter(a -> "confirmed".equalsIgnoreCase(a.getStatus()))
                                .findFirst()
                                .orElse(null));
            }

            if (targetAppointment != null && !"completed".equalsIgnoreCase(targetAppointment.getStatus())) {
                targetAppointment.setStatus("completed");
                Appointment completedApt = appointmentRepository.save(targetAppointment);
                generateAutomaticBillingForAppointment(completedApt, billingRepository);
            } else if (targetAppointment != null && "completed".equalsIgnoreCase(targetAppointment.getStatus())) {
                generateAutomaticBillingForAppointment(targetAppointment, billingRepository);
            }

            return ResponseEntity.ok(savedRecord);
        }
    }

    // =========================================================================
    // 3. COLABORADORES
    // =========================================================================
    @RestController
    @RequestMapping("/staff")
    @Tag(name = "4. Colaboradores", description = "CRUD de fisioterapeutas e profissionais com vínculo 1:1 de Usuário")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class StaffController {

        @Autowired
        private StaffRepository staffRepository;

        @Autowired
        private UserRepository userRepository;

        @GetMapping
        @Operation(summary = "Listar colaboradores (ROLE_PHYSIO vê apenas seu próprio registro vinculado)")
        public List<Staff> getAll(org.springframework.security.core.Authentication authentication) {
            if (isPhysioRestricted(authentication)) {
                return staffRepository.findByUserUsername(authentication.getName())
                        .map(List::of)
                        .orElse(List.of());
            }
            return staffRepository.findAllByOrderByIdAsc();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Buscar colaborador por ID")
        public ResponseEntity<Staff> getById(@PathVariable Long id) {
            return staffRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping
        @Operation(summary = "Cadastrar colaborador com vínculo opcional 1:1 de usuário")
        public ResponseEntity<?> create(@RequestBody Staff staff) {
            if (staff.getName() == null || staff.getName().trim().length() < 3) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nome do colaborador deve conter ao menos 3 caracteres."));
            }
            if (staff.getRole() == null || staff.getRole().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Especialidade/Função é obrigatória."));
            }

            staff.setName(staff.getName().trim());
            staff.setRole(staff.getRole().trim());
            if (staff.getCrefito() != null && !staff.getCrefito().isBlank()) {
                staff.setCrefito(staff.getCrefito().trim());
            } else {
                staff.setCrefito(null);
            }

            // Vínculo 1:1 com Usuário
            Long reqUserId = staff.getRequestedUserId();
            if (reqUserId != null) {
                User user = userRepository.findById(reqUserId).orElse(null);
                if (user == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Usuário selecionado para vínculo não foi encontrado."));
                }
                Staff existingLinked = staffRepository.findByUserId(reqUserId).orElse(null);
                if (existingLinked != null) {
                    return ResponseEntity.badRequest().body(Map.of("message",
                            "O usuário '" + user.getName() + "' já está vinculado ao colaborador '" + existingLinked.getName() + "' (relação 1:1)."));
                }
                staff.setUser(user);
            } else {
                staff.setUser(null);
            }

            if (staff.getActive() == null) {
                staff.setActive(true);
            }
            return ResponseEntity.ok(staffRepository.save(staff));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar colaborador e seu vínculo 1:1 de usuário")
        public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Staff updated) {
            return staffRepository.findById(id).map(staff -> {
                if (updated.getName() != null && updated.getName().trim().length() >= 3) {
                    staff.setName(updated.getName().trim());
                }
                if (updated.getRole() != null && !updated.getRole().isBlank()) {
                    staff.setRole(updated.getRole().trim());
                }
                if (updated.getPhone() != null) staff.setPhone(updated.getPhone());
                if (updated.getEmail() != null) staff.setEmail(updated.getEmail());
                if (updated.getActive() != null) staff.setActive(updated.getActive());

                if (updated.isUserIdProvided()) {
                    Long reqUserId = updated.getRequestedUserId();
                    if (reqUserId == null) {
                        staff.setUser(null);
                    } else {
                        User user = userRepository.findById(reqUserId).orElse(null);
                        if (user == null) {
                            return ResponseEntity.badRequest().body(Map.of("message", "Usuário selecionado para vínculo não foi encontrado."));
                        }
                        Staff existingLinked = staffRepository.findByUserId(reqUserId).orElse(null);
                        if (existingLinked != null && !existingLinked.getId().equals(id)) {
                            return ResponseEntity.badRequest().body(Map.of("message",
                                    "O usuário '" + user.getName() + "' já está vinculado ao colaborador '" + existingLinked.getName() + "' (relação 1:1)."));
                        }
                        staff.setUser(user);
                    }
                }

                return ResponseEntity.ok(staffRepository.save(staff));
            }).orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/{id}/toggle-active")
        @Operation(summary = "Ativar ou desativar colaborador")
        public ResponseEntity<?> toggleActive(@PathVariable Long id) {
            return staffRepository.findById(id).map(staff -> {
                staff.setActive(!Boolean.TRUE.equals(staff.getActive()));
                return ResponseEntity.ok(staffRepository.save(staff));
            }).orElse(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Remover ou desativar colaborador")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            return staffRepository.findById(id).map(staff -> {
                try {
                    staffRepository.delete(staff);
                    staffRepository.flush();
                    return ResponseEntity.ok(Map.of("message", "Colaborador excluído com sucesso."));
                } catch (Exception e) {
                    staff.setActive(false);
                    staff.setUser(null);
                    staffRepository.save(staff);
                    return ResponseEntity.ok(Map.of("message", "Colaborador possui histórico vinculado e foi desativado."));
                }
            }).orElse(ResponseEntity.notFound().build());
        }
    }

    // =========================================================================
    // 4. PLANOS DE SAÚDE
    // =========================================================================
    @RestController
    @RequestMapping("/health-plans")
    @Tag(name = "5. Planos de Saúde", description = "CRUD de convênios e operadoras de saúde")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class HealthPlanController {

        @Autowired
        private HealthPlanRepository planRepository;

        @GetMapping
        @Operation(summary = "Listar operadoras de saúde")
        public List<HealthPlan> getAll() {
            return planRepository.findAllByOrderByIdAsc();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Buscar plano de saúde por ID")
        public ResponseEntity<HealthPlan> getById(@PathVariable Long id) {
            return planRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping
        @Operation(summary = "Cadastrar plano de saúde")
        public ResponseEntity<?> create(@RequestBody HealthPlan plan) {
            if (plan.getName() == null || plan.getName().trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nome da operadora é obrigatório."));
            }
            if (plan.getCode() == null || plan.getCode().trim().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Código identificador do plano é obrigatório."));
            }
            if (planRepository.existsByCode(plan.getCode().trim().toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Já existe uma operadora com este código."));
            }

            plan.setName(plan.getName().trim());
            plan.setCode(plan.getCode().trim().toUpperCase());
            if (plan.getCopayRate() == null || plan.getCopayRate().compareTo(BigDecimal.ZERO) < 0) {
                plan.setCopayRate(BigDecimal.ZERO);
            }
            if (plan.getActive() == null) {
                plan.setActive(true);
            }

            return ResponseEntity.ok(planRepository.save(plan));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar plano de saúde")
        public ResponseEntity<?> update(@PathVariable Long id, @RequestBody HealthPlan updated) {
            return planRepository.findById(id).map(plan -> {
                if (updated.getName() != null && !updated.getName().isBlank()) {
                    plan.setName(updated.getName().trim());
                }
                if (updated.getCode() != null && !updated.getCode().isBlank()) {
                    String formattedCode = updated.getCode().trim().toUpperCase();
                    if (!plan.getCode().equalsIgnoreCase(formattedCode) && planRepository.existsByCode(formattedCode)) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Já existe outra operadora com este código."));
                    }
                    plan.setCode(formattedCode);
                }
                if (updated.getCopayRate() != null && updated.getCopayRate().compareTo(BigDecimal.ZERO) >= 0) {
                    plan.setCopayRate(updated.getCopayRate());
                }
                if (updated.getContactPhone() != null) {
                    plan.setContactPhone(updated.getContactPhone());
                }
                if (updated.getActive() != null) {
                    plan.setActive(updated.getActive());
                }

                return ResponseEntity.ok(planRepository.save(plan));
            }).orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/{id}/toggle-active")
        @Operation(summary = "Ativar ou desativar plano de saúde")
        public ResponseEntity<?> toggleActive(@PathVariable Long id) {
            return planRepository.findById(id).map(plan -> {
                plan.setActive(!Boolean.TRUE.equals(plan.getActive()));
                return ResponseEntity.ok(planRepository.save(plan));
            }).orElse(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Remover plano de saúde")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            return planRepository.findById(id).map(plan -> {
                try {
                    planRepository.delete(plan);
                    planRepository.flush();
                    return ResponseEntity.ok(Map.of("message", "Plano de saúde excluído com sucesso."));
                } catch (Exception e) {
                    plan.setActive(false);
                    planRepository.save(plan);
                    return ResponseEntity.ok(Map.of("message", "Plano de saúde desativado com sucesso."));
                }
            }).orElse(ResponseEntity.notFound().build());
        }
    }

    // =========================================================================
    // 5. AGENDAMENTOS (AGENDA E ATENDIMENTOS DO DIA)
    // =========================================================================
    @RestController
    @RequestMapping("/appointments")
    @Tag(name = "6. Agendamentos & Agenda", description = "Consultas por blocos de horários e ações do dia em destaque")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class AppointmentController {

        @Autowired
        private AppointmentRepository appointmentRepository;

        @Autowired
        private PatientRepository patientRepository;

        @Autowired
        private StaffRepository staffRepository;

        @Autowired
        private BillingRecordRepository billingRepository;

        @GetMapping
        @Operation(summary = "Consultar agendamentos com menor privilégio (ROLE_PHYSIO vê apenas os seus, ADMIN/RECEPÇÃO veem todos)")
        public List<Appointment> getAppointments(
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                @RequestParam(required = false) Long staffId,
                org.springframework.security.core.Authentication authentication) {

            Long targetStaffId = staffId;

            // Princípio do Menor Privilégio (Least Privilege 1:1):
            // Se o usuário autenticado for um fisioterapeuta (ROLE_PHYSIO), ele OBRIGATORIAMENTE só pode ver os seus agendamentos
            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff != null) {
                    targetStaffId = currentStaff.getId();
                } else {
                    return List.of();
                }
            }

            if (date != null && targetStaffId != null) {
                return appointmentRepository.findByAppointmentDateAndStaffIdOrderByAppointmentTimeAsc(date, targetStaffId);
            } else if (date != null) {
                return appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(date);
            } else if (targetStaffId != null) {
                return appointmentRepository.findByStaffIdOrderByAppointmentDateDescAppointmentTimeAsc(targetStaffId);
            }

            return appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeAsc();
        }

        @PostMapping
        @Operation(summary = "Criar novo agendamento com Ações do Dia e Validação de Conflitos")
        public ResponseEntity<?> create(
                @RequestBody Appointment appointment,
                org.springframework.security.core.Authentication authentication) {
            if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Paciente é obrigatório."));
            }

            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return ResponseEntity.status(403).body(Map.of("message", "Seu usuário de Fisioterapeuta não está vinculado a nenhum colaborador."));
                }
                appointment.setStaff(currentStaff);
            }

            if (appointment.getStaff() == null || appointment.getStaff().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Profissional/Fisioterapeuta é obrigatório."));
            }
            if (appointment.getAppointmentDate() == null || appointment.getAppointmentTime() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Data e horário do agendamento são obrigatórios."));
            }

            Patient patient = patientRepository.findById(appointment.getPatient().getId()).orElse(null);
            if (patient == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Paciente não encontrado."));
            }
            Staff staff = staffRepository.findById(appointment.getStaff().getId()).orElse(null);
            if (staff == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Profissional não encontrado."));
            }
            appointment.setPatient(patient);
            appointment.setStaff(staff);

            if (appointment.getDurationMinutes() == null || appointment.getDurationMinutes() <= 0) {
                appointment.setDurationMinutes(50);
            }

            // Validação de conflito de horário
            String conflictError = checkConflict(null, appointment);
            if (conflictError != null) {
                return ResponseEntity.status(409).body(Map.of("message", conflictError));
            }

            Appointment saved = appointmentRepository.save(appointment);
            if ("completed".equalsIgnoreCase(saved.getStatus())) {
                generateAutomaticBillingForAppointment(saved, billingRepository);
            }
            return ResponseEntity.ok(saved);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar agendamento (horário, status ou ações)")
        public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Appointment updated) {
            return appointmentRepository.findById(id).map(appointment -> {
                if (updated.getAppointmentDate() != null) appointment.setAppointmentDate(updated.getAppointmentDate());
                if (updated.getAppointmentTime() != null) appointment.setAppointmentTime(updated.getAppointmentTime());
                if (updated.getDurationMinutes() != null) appointment.setDurationMinutes(updated.getDurationMinutes());
                if (updated.getActionsForDay() != null) appointment.setActionsForDay(updated.getActionsForDay());
                if (updated.getStatus() != null) appointment.setStatus(updated.getStatus());
                if (updated.getNotes() != null) appointment.setNotes(updated.getNotes());

                if (updated.getPatient() != null && updated.getPatient().getId() != null) {
                    patientRepository.findById(updated.getPatient().getId()).ifPresent(appointment::setPatient);
                }
                if (updated.getStaff() != null && updated.getStaff().getId() != null) {
                    staffRepository.findById(updated.getStaff().getId()).ifPresent(appointment::setStaff);
                }

                if (!"cancelled".equalsIgnoreCase(appointment.getStatus())) {
                    String conflictError = checkConflict(id, appointment);
                    if (conflictError != null) {
                        return ResponseEntity.status(409).body(Map.of("message", conflictError));
                    }
                }

                Appointment saved = appointmentRepository.save(appointment);
                if ("completed".equalsIgnoreCase(saved.getStatus())) {
                    generateAutomaticBillingForAppointment(saved, billingRepository);
                }
                return ResponseEntity.ok(saved);
            }).orElse(ResponseEntity.notFound().build());
        }

        private String checkConflict(Long currentAppointmentId, Appointment appointment) {
            int duration = (appointment.getDurationMinutes() != null && appointment.getDurationMinutes() > 0)
                    ? appointment.getDurationMinutes() : 50;
            LocalTime newStart = appointment.getAppointmentTime();
            LocalTime newEnd = newStart.plusMinutes(duration);

            // 1. Checa conflito de horário para o fisioterapeuta/staff
            List<Appointment> staffAppointments = appointmentRepository.findActiveByDateAndStaff(
                    appointment.getAppointmentDate(), appointment.getStaff().getId());
            for (Appointment existing : staffAppointments) {
                if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) {
                    continue;
                }
                int existingDuration = (existing.getDurationMinutes() != null && existing.getDurationMinutes() > 0)
                        ? existing.getDurationMinutes() : 50;
                LocalTime existStart = existing.getAppointmentTime();
                LocalTime existEnd = existStart.plusMinutes(existingDuration);

                if (newStart.isBefore(existEnd) && existStart.isBefore(newEnd)) {
                    return String.format("Conflito de agenda: O profissional %s já possui agendamento das %s às %s neste dia.",
                            appointment.getStaff().getName(), existStart, existEnd);
                }
            }

            // 2. Checa se o próprio paciente já tem outro agendamento no mesmo horário
            List<Appointment> patientAppointments = appointmentRepository.findActiveByDateAndPatient(
                    appointment.getAppointmentDate(), appointment.getPatient().getId());
            for (Appointment existing : patientAppointments) {
                if (currentAppointmentId != null && existing.getId().equals(currentAppointmentId)) {
                    continue;
                }
                int existingDuration = (existing.getDurationMinutes() != null && existing.getDurationMinutes() > 0)
                        ? existing.getDurationMinutes() : 50;
                LocalTime existStart = existing.getAppointmentTime();
                LocalTime existEnd = existStart.plusMinutes(existingDuration);

                if (newStart.isBefore(existEnd) && existStart.isBefore(newEnd)) {
                    return String.format("Conflito de agenda: O paciente %s já possui outro agendamento das %s às %s neste dia.",
                            appointment.getPatient().getName(), existStart, existEnd);
                }
            }

            return null;
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Alterar status do atendimento (scheduled, confirmed, in_progress, completed, cancelled)")
        public ResponseEntity<Appointment> updateStatus(@PathVariable Long id, @RequestParam String status) {
            return appointmentRepository.findById(id).map(appointment -> {
                appointment.setStatus(status);
                Appointment saved = appointmentRepository.save(appointment);
                if ("completed".equalsIgnoreCase(saved.getStatus())) {
                    generateAutomaticBillingForAppointment(saved, billingRepository);
                }
                return ResponseEntity.ok(saved);
            }).orElse(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Cancelar/Excluir agendamento")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            if (appointmentRepository.existsById(id)) {
                appointmentRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("message", "Agendamento excluído com sucesso."));
            }
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // 6. FATURAMENTO E REGISTRO FINANCEIRO
    // =========================================================================
    @RestController
    @RequestMapping("/billing")
    @Tag(name = "7. Financeiro & Cobranças", description = "Faturamento particionado entre particulares e planos de saúde")
    @SecurityRequirement(name = "Bearer Authentication")
    public static class BillingController {

        @Autowired
        private BillingRecordRepository billingRepository;

        @Autowired
        private PatientRepository patientRepository;

        @Autowired
        private StaffRepository staffRepository;

        @Autowired
        private HealthPlanRepository healthPlanRepository;

        @Autowired
        private AppointmentRepository appointmentRepository;

        @GetMapping
        @Operation(summary = "Listar registros financeiros (ROLE_PHYSIO vê apenas os vinculados ao seu colaborador)")
        public List<BillingRecord> getAll(org.springframework.security.core.Authentication authentication) {
            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return List.of();
                }
                return billingRepository.findByStaffIdOrderByBillingDateDescBillingTimeDesc(currentStaff.getId());
            }
            return billingRepository.findAllByOrderByBillingDateDescBillingTimeDesc();
        }

        @GetMapping("/grouped-by-plan")
        @Operation(summary = "Agrupar atendimentos por plano de saúde com subtotais")
        public Map<String, List<BillingRecord>> getGroupedByHealthPlan(org.springframework.security.core.Authentication authentication) {
            List<BillingRecord> healthPlanRecords;
            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return Map.of();
                }
                healthPlanRecords = billingRepository.findByStaffIdAndIsHealthPlan(currentStaff.getId(), true);
            } else {
                healthPlanRecords = billingRepository.findByIsHealthPlan(true);
            }
            return healthPlanRecords.stream()
                    .collect(Collectors.groupingBy(b -> b.getHealthPlanName() != null ? b.getHealthPlanName() : "Outros Convênios"));
        }

        @GetMapping("/private")
        @Operation(summary = "Listar atendimentos particulares")
        public List<BillingRecord> getPrivateBilling(org.springframework.security.core.Authentication authentication) {
            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return List.of();
                }
                return billingRepository.findByStaffIdAndIsHealthPlan(currentStaff.getId(), false);
            }
            return billingRepository.findByIsHealthPlan(false);
        }

        @PostMapping
        @Operation(summary = "Registrar novo atendimento faturado / cobrança com validações financeiras")
        public ResponseEntity<?> create(
                @RequestBody BillingRecord record,
                org.springframework.security.core.Authentication authentication) {
            if (record.getPatient() == null || record.getPatient().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Paciente é obrigatório para faturamento."));
            }

            if (isPhysioRestricted(authentication)) {
                Staff currentStaff = staffRepository.findByUserUsername(authentication.getName()).orElse(null);
                if (currentStaff == null) {
                    return ResponseEntity.status(403).body(Map.of("message", "Seu usuário de Fisioterapeuta não está vinculado a nenhum colaborador."));
                }
                record.setStaff(currentStaff);
            }

            if (record.getStaff() == null || record.getStaff().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Profissional responsável é obrigatório."));
            }
            if (record.getValue() == null || record.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Valor do atendimento deve ser maior que zero."));
            }

            Patient patient = patientRepository.findById(record.getPatient().getId()).orElse(null);
            if (patient == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Paciente não encontrado."));
            }
            Staff staff = staffRepository.findById(record.getStaff().getId()).orElse(null);
            if (staff == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Profissional não encontrado."));
            }

            record.setPatient(patient);
            record.setStaff(staff);

            if (record.getAppointment() != null && record.getAppointment().getId() != null) {
                appointmentRepository.findById(record.getAppointment().getId()).ifPresent(record::setAppointment);
            } else {
                record.setAppointment(null);
            }

            if (Boolean.TRUE.equals(record.getIsHealthPlan())) {
                if (record.getHealthPlan() != null && record.getHealthPlan().getId() != null) {
                    HealthPlan hp = healthPlanRepository.findById(record.getHealthPlan().getId()).orElse(null);
                    if (hp != null) {
                        record.setHealthPlan(hp);
                        if (record.getHealthPlanName() == null || record.getHealthPlanName().isBlank()) {
                            record.setHealthPlanName(hp.getName());
                        }
                    }
                }
            } else {
                record.setIsHealthPlan(false);
                record.setHealthPlan(null);
                record.setHealthPlanName("Particular");
            }

            if (record.getBillingDate() == null) {
                record.setBillingDate(LocalDate.now());
            }
            if (record.getBillingTime() == null) {
                record.setBillingTime(LocalTime.now());
            }
            if (record.getCopayValue() == null) {
                record.setCopayValue(BigDecimal.ZERO);
            }
            if (record.getInvoiceNumber() == null || record.getInvoiceNumber().isBlank()) {
                String prefix = Boolean.TRUE.equals(record.getIsHealthPlan()) ? "FAT" : "REC";
                record.setInvoiceNumber(String.format("%s-%d-%04d", prefix, LocalDate.now().getYear(), System.currentTimeMillis() % 10000));
            }
            if (record.getBillingStatus() == null || record.getBillingStatus().isBlank()) {
                record.setBillingStatus("pending");
            }

            return ResponseEntity.ok(billingRepository.save(record));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar valores e dados da cobrança")
        public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BillingRecord updated) {
            return billingRepository.findById(id).map(record -> {
                if (updated.getValue() != null && updated.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    record.setValue(updated.getValue());
                }
                if (updated.getCopayValue() != null && updated.getCopayValue().compareTo(BigDecimal.ZERO) >= 0) {
                    record.setCopayValue(updated.getCopayValue());
                }
                if (updated.getBillingStatus() != null && !updated.getBillingStatus().isBlank()) {
                    record.setBillingStatus(updated.getBillingStatus());
                }
                if (updated.getPaymentMethod() != null) {
                    record.setPaymentMethod(updated.getPaymentMethod());
                }
                if (updated.getProcedureCode() != null) {
                    record.setProcedureCode(updated.getProcedureCode());
                }
                if (updated.getBillingDate() != null) {
                    record.setBillingDate(updated.getBillingDate());
                }
                if (updated.getBillingTime() != null) {
                    record.setBillingTime(updated.getBillingTime());
                }
                if (updated.getPatient() != null && updated.getPatient().getId() != null) {
                    patientRepository.findById(updated.getPatient().getId()).ifPresent(record::setPatient);
                }
                if (updated.getStaff() != null && updated.getStaff().getId() != null) {
                    staffRepository.findById(updated.getStaff().getId()).ifPresent(record::setStaff);
                }
                if (updated.getIsHealthPlan() != null) {
                    record.setIsHealthPlan(updated.getIsHealthPlan());
                    if (Boolean.TRUE.equals(updated.getIsHealthPlan()) && updated.getHealthPlan() != null && updated.getHealthPlan().getId() != null) {
                        healthPlanRepository.findById(updated.getHealthPlan().getId()).ifPresent(hp -> {
                            record.setHealthPlan(hp);
                            record.setHealthPlanName(hp.getName());
                        });
                    } else if (Boolean.FALSE.equals(updated.getIsHealthPlan())) {
                        record.setHealthPlan(null);
                        record.setHealthPlanName("Particular");
                    }
                }

                return ResponseEntity.ok(billingRepository.save(record));
            }).orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Atualizar status da cobrança (pending, approved, submitted, paid)")
        public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
            return billingRepository.findById(id).map(record -> {
                record.setBillingStatus(status);
                return ResponseEntity.ok(billingRepository.save(record));
            }).orElse(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Remover registro financeiro / estorno")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            if (billingRepository.existsById(id)) {
                billingRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("message", "Registro financeiro excluído com sucesso."));
            }
            return ResponseEntity.notFound().build();
        }
    }
}

