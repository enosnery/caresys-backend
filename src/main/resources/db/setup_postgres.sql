-- ==============================================================================
-- DDL & DML COMPLETE SETUP & MIGRATION SCRIPT - CARESYS (CLÍNICA DE FISIOTERAPIA)
-- BANCO DE DADOS: PostgreSQL (14+)
-- IDEMPOTENTE: Seguro para ser executado como script inicial ou de atualização.
-- ==============================================================================

-- 1. Criação das Extensões Necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 2. DDL - CRIAÇÃO OU MIGRAÇÃO DE TABELAS (IF NOT EXISTS)
-- ==============================================================================

-- 2.1 Tabela de Usuários para Autenticação
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_PHYSIO',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.2 Tabela de Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.3 Tabela de Planos de Saúde / Operadoras
CREATE TABLE IF NOT EXISTS health_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    staff_value NUMERIC(10, 2) DEFAULT 0.00,
    expected_payment_days INTEGER DEFAULT 30,
    tax_rate NUMERIC(5, 2) DEFAULT 0.00,
    contact_phone VARCHAR(30),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.4 Tabela de Colaboradores (Fisioterapeutas e Equipe - Vínculo 1:1 com Usuário)
CREATE TABLE IF NOT EXISTS staff (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(100) NOT NULL,
    crefito VARCHAR(50),
    phone VARCHAR(30),
    email VARCHAR(150),
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.5 Tabela de Pacientes (com biometria, queixa principal e fisioterapeuta responsável)
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    age INTEGER NOT NULL CHECK (age >= 0),
    weight NUMERIC(5, 2) NOT NULL CHECK (weight > 0),
    height NUMERIC(4, 2) NOT NULL CHECK (height > 0),
    chief_complaint TEXT NOT NULL,
    has_health_plan BOOLEAN DEFAULT FALSE,
    health_plan_id BIGINT REFERENCES health_plans(id) ON DELETE SET NULL,
    staff_id BIGINT REFERENCES staff(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.6 Tabela de Prontuários (Histórico Cumulativo Append-Only)
CREATE TABLE IF NOT EXISTS medical_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE RESTRICT,
    evolution_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    physical_evolution TEXT NOT NULL,
    treatment_conducted TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.7 Tabela de Agendamentos (Consultas com Ações do Dia em Destaque)
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE RESTRICT,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER DEFAULT 50,
    actions_for_day TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'scheduled',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.8 Tabela de Fechamentos de Período de Faturamento
CREATE TABLE IF NOT EXISTS billing_closures (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) UNIQUE NOT NULL,
    description VARCHAR(180),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    appointments_count INTEGER NOT NULL DEFAULT 0,
    total_gross_revenue NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_staff_payment NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_clinic_value NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    closed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2.9 Tabela de Registros Financeiros & Cobranças (Faturamento)
CREATE TABLE IF NOT EXISTS billing_records (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE RESTRICT,
    billing_date DATE NOT NULL,
    billing_time TIME NOT NULL,
    is_health_plan BOOLEAN DEFAULT FALSE,
    health_plan_id BIGINT REFERENCES health_plans(id) ON DELETE SET NULL,
    health_plan_name VARCHAR(120),
    value NUMERIC(10, 2) NOT NULL,
    staff_value NUMERIC(10, 2) DEFAULT 0.00,
    procedure_code VARCHAR(100),
    payment_method VARCHAR(50),
    invoice_number VARCHAR(100) NOT NULL,
    billing_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    billing_closure_id BIGINT REFERENCES billing_closures(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 3. UPGRADES E AJUSTES DE ESTRUTURA (SCHEMA UPDATES & INDEXES)
-- ==============================================================================

-- Novas colunas idempotentes para a tabela de usuários
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Remoção da obrigatoriedade e constraint antiga de CREFITO na tabela de colaboradores
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'staff' AND column_name = 'crefito'
    ) THEN
        ALTER TABLE staff ALTER COLUMN crefito DROP NOT NULL;
    END IF;
END $$;
ALTER TABLE staff DROP CONSTRAINT IF EXISTS uk_staff_crefito;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- Garantia da relação 1:1 entre Usuário e Colaborador (staff.user_id UNIQUE)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_staff_user_id'
    ) THEN
        ALTER TABLE staff ADD CONSTRAINT uk_staff_user_id UNIQUE (user_id);
    END IF;
END $$;

-- Coluna de fisioterapeuta responsável na tabela de pacientes
ALTER TABLE patients ADD COLUMN IF NOT EXISTS staff_id BIGINT REFERENCES staff(id) ON DELETE SET NULL;

-- Migração dos Planos de Saúde: substituição de coparticipação por valor colaborador, prazo de pagamento e alíquota de imposto
ALTER TABLE health_plans ADD COLUMN IF NOT EXISTS staff_value NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE health_plans ADD COLUMN IF NOT EXISTS expected_payment_days INTEGER DEFAULT 30;
ALTER TABLE health_plans ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5, 2) DEFAULT 0.00;
ALTER TABLE health_plans ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE health_plans DROP COLUMN IF EXISTS copay_rate;

-- Migração do Faturamento (billing_records): substituição de copay_value por staff_value e vínculo com fechamento de período
ALTER TABLE billing_records ADD COLUMN IF NOT EXISTS staff_value NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE billing_records DROP COLUMN IF EXISTS copay_value;
ALTER TABLE billing_records ADD COLUMN IF NOT EXISTS billing_closure_id BIGINT REFERENCES billing_closures(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_billing_closure ON billing_records(billing_closure_id);

-- Criação de Índices de Alta Performance
CREATE INDEX IF NOT EXISTS idx_appointments_date_staff ON appointments(appointment_date, staff_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date_patient ON appointments(appointment_date, patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_patient ON medical_records(patient_id, evolution_date DESC);
CREATE INDEX IF NOT EXISTS idx_billing_plan ON billing_records(is_health_plan, health_plan_id);
CREATE INDEX IF NOT EXISTS idx_billing_date ON billing_records(billing_date);
CREATE INDEX IF NOT EXISTS idx_patients_name ON patients(name);
CREATE INDEX IF NOT EXISTS idx_patients_staff ON patients(staff_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- ==============================================================================
-- 4. DML - CARGA DE DADOS IDEMPOTENTE (UPSERT / ON CONFLICT)
-- ==============================================================================

-- 4.1 Usuários Padrão do Sistema
-- OBS: Senha inicial de teste: admin123
-- SHA-256("admin123") = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a
INSERT INTO users (username, email, password_hash, name, role, active)
VALUES
('admin', 'admin@fisicare.com', '$2a$10$0PfqfFv5cRkQjQ1k7D/BwO5rY6fJpY8v8B7N3T8u5z5xXwW.Y7G.S', 'Administrador da Clínica', 'ROLE_ADMIN', TRUE),
('recepcao', 'recepcao@fisicare.com', '$2a$10$0PfqfFv5cRkQjQ1k7D/BwO5rY6fJpY8v8B7N3T8u5z5xXwW.Y7G.S', 'Recepção Central', 'ROLE_RECEPTIONIST', TRUE),
('camila.alencar', 'camila.alencar@fisicare.com', '$2a$10$0PfqfFv5cRkQjQ1k7D/BwO5rY6fJpY8v8B7N3T8u5z5xXwW.Y7G.S', 'Dra. Camila Alencar', 'ROLE_PHYSIO', TRUE),
('rodrigo.menezes', 'rodrigo.menezes@fisicare.com', '$2a$10$0PfqfFv5cRkQjQ1k7D/BwO5rY6fJpY8v8B7N3T8u5z5xXwW.Y7G.S', 'Dr. Rodrigo Menezes', 'ROLE_PHYSIO', TRUE)
ON CONFLICT (username) DO UPDATE
SET email = EXCLUDED.email,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    active = EXCLUDED.active;

-- 4.2 Planos de Saúde (com Valor Colaborador, Prazo Previsto de Pagamento em Dias e Alíquota de Imposto %)
INSERT INTO health_plans (name, code, staff_value, expected_payment_days, tax_rate, contact_phone, active)
VALUES
('Unimed Seguros', 'UNI-01', 55.00, 30, 6.00, '(11) 3222-1000', TRUE),
('Bradesco Saúde', 'BRAD-02', 50.00, 45, 8.00, '(11) 3333-2000', TRUE),
('Amil Assistência Médica', 'AMIL-03', 48.00, 30, 6.00, '(11) 3444-3000', TRUE),
('SulAmérica Saúde', 'SUL-04', 60.00, 30, 7.50, '(11) 3555-4000', TRUE),
('NotreDame Intermédica', 'GNDI-05', 45.00, 60, 6.00, '(11) 3666-5000', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    staff_value = EXCLUDED.staff_value,
    expected_payment_days = EXCLUDED.expected_payment_days,
    tax_rate = EXCLUDED.tax_rate,
    contact_phone = EXCLUDED.contact_phone,
    active = EXCLUDED.active;

-- Garante que planos customizados já existentes não fiquem com valores nulos nos novos campos
UPDATE health_plans SET staff_value = 50.00 WHERE staff_value IS NULL OR staff_value = 0;
UPDATE health_plans SET expected_payment_days = 30 WHERE expected_payment_days IS NULL OR expected_payment_days = 0;
UPDATE health_plans SET tax_rate = 6.00 WHERE tax_rate IS NULL;

-- 4.3 Colaboradores (com link 1:1 dinâmico ao id do usuário correspondente)
INSERT INTO staff (name, role, phone, email, user_id, active)
SELECT 'Dra. Camila Alencar', 'Fisioterapeuta Traumato-Ortopédica', '(11) 98765-4321', 'camila.alencar@fisicare.com',
       (SELECT id FROM users WHERE username = 'camila.alencar' LIMIT 1), TRUE
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE name = 'Dra. Camila Alencar');

INSERT INTO staff (name, role, phone, email, user_id, active)
SELECT 'Dr. Rodrigo Menezes', 'Fisioterapeuta Neurofuncional', '(11) 97654-3210', 'rodrigo.menezes@fisicare.com',
       (SELECT id FROM users WHERE username = 'rodrigo.menezes' LIMIT 1), TRUE
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE name = 'Dr. Rodrigo Menezes');

INSERT INTO staff (name, role, phone, email, user_id, active)
SELECT 'Dra. Mariana Rios', 'Fisioterapeuta Respiratória & Pilates', '(11) 96543-2109', 'mariana.rios@fisicare.com', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE name = 'Dra. Mariana Rios');

INSERT INTO staff (name, role, phone, email, user_id, active)
SELECT 'Dr. Lucas Ferreira', 'Fisioterapeuta Desportivo', '(11) 95432-1098', 'lucas.ferreira@fisicare.com', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE name = 'Dr. Lucas Ferreira');

-- Garante que colaboradores já existentes estejam com user_id 1:1 atualizado
UPDATE staff SET user_id = (SELECT id FROM users WHERE username = 'camila.alencar' LIMIT 1)
WHERE name = 'Dra. Camila Alencar' AND user_id IS NULL;

UPDATE staff SET user_id = (SELECT id FROM users WHERE username = 'rodrigo.menezes' LIMIT 1)
WHERE name = 'Dr. Rodrigo Menezes' AND user_id IS NULL;

-- 4.4 Pacientes (Inserção condicional com vínculo ao fisioterapeuta responsável)
INSERT INTO patients (name, phone, age, weight, height, chief_complaint, has_health_plan, health_plan_id, staff_id)
SELECT 'Arthur Vinícius Ramos', '(11) 98111-2233', 34, 78.50, 1.78, 'Lombalgia crônica irradiando para glúteo esquerdo após esforço físico.', TRUE, hp.id,
       (SELECT id FROM staff WHERE name = 'Dra. Camila Alencar' LIMIT 1)
FROM health_plans hp WHERE hp.code = 'UNI-01'
AND NOT EXISTS (SELECT 1 FROM patients WHERE name = 'Arthur Vinícius Ramos');

INSERT INTO patients (name, phone, age, weight, height, chief_complaint, has_health_plan, health_plan_id, staff_id)
SELECT 'Beatriz Vasconcelos', '(11) 98222-3344', 28, 62.00, 1.65, 'Pós-operatório de LCA e menisco medial no joelho direito há 4 semanas.', FALSE, NULL,
       (SELECT id FROM staff WHERE name = 'Dr. Rodrigo Menezes' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM patients WHERE name = 'Beatriz Vasconcelos');

INSERT INTO patients (name, phone, age, weight, height, chief_complaint, has_health_plan, health_plan_id, staff_id)
SELECT 'Carlos Eduardo Nogueira', '(11) 98333-4455', 52, 84.00, 1.74, 'Bursite e tendinopatia do supraespinhal no ombro direito com limitação de abdução.', TRUE, hp.id,
       (SELECT id FROM staff WHERE name = 'Dra. Camila Alencar' LIMIT 1)
FROM health_plans hp WHERE hp.code = 'BRAD-02'
AND NOT EXISTS (SELECT 1 FROM patients WHERE name = 'Carlos Eduardo Nogueira');

INSERT INTO patients (name, phone, age, weight, height, chief_complaint, has_health_plan, health_plan_id, staff_id)
SELECT 'Daniela Meireles Prado', '(11) 98444-5566', 41, 59.00, 1.62, 'Cervicobraquialgia com parestesia em membros superiores decorrente de postura em home office.', TRUE, hp.id,
       (SELECT id FROM staff WHERE name = 'Dra. Mariana Rios' LIMIT 1)
FROM health_plans hp WHERE hp.code = 'AMIL-03'
AND NOT EXISTS (SELECT 1 FROM patients WHERE name = 'Daniela Meireles Prado');

INSERT INTO patients (name, phone, age, weight, height, chief_complaint, has_health_plan, health_plan_id, staff_id)
SELECT 'Eduardo Silveira Costa', '(11) 98555-6677', 65, 88.00, 1.70, 'Artrose severa de quadril com marcha claudicante e rigidez matinal.', FALSE, NULL,
       (SELECT id FROM staff WHERE name = 'Dr. Lucas Ferreira' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM patients WHERE name = 'Eduardo Silveira Costa');

-- 4.5 Prontuários (Evoluções Iniciais Condicionais)
INSERT INTO medical_records (patient_id, staff_id, evolution_date, physical_evolution, treatment_conducted, notes)
SELECT p.id, s.id, CURRENT_TIMESTAMP - INTERVAL '15 days', 'EVA 7/10 em dor aguda ao sentar por tempo prolongado.', 'Terapia manual miofascial e eletroanalgesia TENS 100Hz por 20min.', 'Avaliação cinesiológica funcional. Lasègue negativo.'
FROM patients p, staff s
WHERE p.name = 'Arthur Vinícius Ramos' AND s.name = 'Dra. Camila Alencar'
AND NOT EXISTS (SELECT 1 FROM medical_records WHERE patient_id = p.id AND physical_evolution LIKE 'EVA 7/10%');

INSERT INTO medical_records (patient_id, staff_id, evolution_date, physical_evolution, treatment_conducted, notes)
SELECT p.id, s.id, CURRENT_TIMESTAMP - INTERVAL '5 days', 'EVA 4/10. Amplitude de flexão lombar aumentada em 15 graus.', 'Fortalecimento de core isométrico, ponte glútea e ponte lateral supervisionada.', 'Paciente relata alívio significativo.'
FROM patients p, staff s
WHERE p.name = 'Arthur Vinícius Ramos' AND s.name = 'Dra. Camila Alencar'
AND NOT EXISTS (SELECT 1 FROM medical_records WHERE patient_id = p.id AND physical_evolution LIKE 'EVA 4/10%');

-- 4.6 Agendamentos para o Dia Corrente (Least Privilege & Ações do Dia)
INSERT INTO appointments (patient_id, staff_id, appointment_date, appointment_time, duration_minutes, actions_for_day, status, notes)
SELECT p.id, s.id, CURRENT_DATE, '08:00:00', 50, 'Liberação miofascial tóraco-lombar + Exercícios com bola suíça para ativação do transverso abdominal.', 'completed', 'Paciente compareceu pontualmente.'
FROM patients p, staff s
WHERE p.name = 'Arthur Vinícius Ramos' AND s.name = 'Dra. Camila Alencar'
AND NOT EXISTS (SELECT 1 FROM appointments WHERE appointment_date = CURRENT_DATE AND appointment_time = '08:00:00' AND staff_id = s.id);

INSERT INTO appointments (patient_id, staff_id, appointment_date, appointment_time, duration_minutes, actions_for_day, status, notes)
SELECT p.id, s.id, CURRENT_DATE, '09:00:00', 50, 'Treino de propriocepção unipodal no disco de equilíbrio + Agachamento até 60° com elástico.', 'in_progress', 'Avaliar fadiga do quadríceps.'
FROM patients p, staff s
WHERE p.name = 'Beatriz Vasconcelos' AND s.name = 'Dr. Rodrigo Menezes'
AND NOT EXISTS (SELECT 1 FROM appointments WHERE appointment_date = CURRENT_DATE AND appointment_time = '09:00:00' AND staff_id = s.id);

INSERT INTO appointments (patient_id, staff_id, appointment_date, appointment_time, duration_minutes, actions_for_day, status, notes)
SELECT p.id, s.id, CURRENT_DATE, '10:00:00', 50, 'Laserterapia no manguito rotador + Fortalecimento de rotadores externos com faixa elástica amarela.', 'scheduled', 'Reavaliar teste de Hawkins-Kennedy.'
FROM patients p, staff s
WHERE p.name = 'Carlos Eduardo Nogueira' AND s.name = 'Dra. Camila Alencar'
AND NOT EXISTS (SELECT 1 FROM appointments WHERE appointment_date = CURRENT_DATE AND appointment_time = '10:00:00' AND staff_id = s.id);

-- 4.7 Faturamento / Cobranças Iniciais
INSERT INTO billing_records (appointment_id, patient_id, staff_id, billing_date, billing_time, is_health_plan, health_plan_id, health_plan_name, value, staff_value, procedure_code, payment_method, invoice_number, billing_status)
SELECT a.id, p.id, s.id, CURRENT_DATE, '08:00:00', TRUE, hp.id, hp.name, 120.00, hp.staff_value, 'FISIO-LOMBAR-01', 'Guia TISS', 'FAT-2026-0001', 'submitted'
FROM appointments a
JOIN patients p ON a.patient_id = p.id
JOIN staff s ON a.staff_id = s.id
JOIN health_plans hp ON hp.code = 'UNI-01'
WHERE a.appointment_date = CURRENT_DATE AND a.appointment_time = '08:00:00' AND s.name = 'Dra. Camila Alencar'
AND NOT EXISTS (SELECT 1 FROM billing_records WHERE invoice_number = 'FAT-2026-0001');

INSERT INTO billing_records (appointment_id, patient_id, staff_id, billing_date, billing_time, is_health_plan, health_plan_id, health_plan_name, value, staff_value, procedure_code, payment_method, invoice_number, billing_status)
SELECT NULL, p.id, s.id, CURRENT_DATE, '09:00:00', FALSE, NULL, 'Particular', 160.00, 80.00, 'FISIO-JOELHO-PART', 'Pix', 'REC-2026-0001', 'paid'
FROM patients p, staff s
WHERE p.name = 'Beatriz Vasconcelos' AND s.name = 'Dr. Rodrigo Menezes'
AND NOT EXISTS (SELECT 1 FROM billing_records WHERE invoice_number = 'REC-2026-0001');

-- Atualiza registros financeiros já existentes para preencher staff_value de acordo com o plano ou particular
UPDATE billing_records br
SET staff_value = hp.staff_value
FROM health_plans hp
WHERE br.is_health_plan = TRUE
  AND br.health_plan_id = hp.id
  AND (br.staff_value IS NULL OR br.staff_value = 0);

UPDATE billing_records
SET staff_value = 80.00
WHERE (is_health_plan = FALSE OR health_plan_id IS NULL)
  AND (staff_value IS NULL OR staff_value = 0);

-- Fim do script de DDL / DML
