-- Script SQL pour le module de Gestion des Congés
-- Base de données: defaultdb (Aiven MySQL)
-- Connexion: hrflow-hrflow.f.aivencloud.com:21031  user=avnadmin
-- Schéma synchronisé avec la vraie BDD le 2026-05-13

-- ============================================
-- Table: leave_balance
-- Description: Solde de congés par employé (1.8 jours/mois)
-- FK → employees(id) (Doctrine/web crée cette table)
-- ============================================

CREATE TABLE IF NOT EXISTS leave_balance (
    id                INT          NOT NULL AUTO_INCREMENT,
    employee_name     VARCHAR(200)          DEFAULT NULL,
    available_days    DECIMAL(8,2) NOT NULL,
    total_accrued     DECIMAL(8,2) NOT NULL,
    total_used        DECIMAL(8,2) NOT NULL,
    last_accrual_date DATE                  DEFAULT NULL,
    hire_date         DATE                  DEFAULT NULL,
    employee_id       INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lb_employee (employee_id),
    CONSTRAINT fk_lb_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: leave_requests
-- Description: Stocke toutes les demandes de congés
-- FK → employees(id)
-- ============================================

CREATE TABLE IF NOT EXISTS leave_requests (
    id                     INT          NOT NULL AUTO_INCREMENT,
    employee_name          VARCHAR(200) NOT NULL,
    start_date             DATE         NOT NULL,
    end_date               DATE         NOT NULL,
    leave_type             VARCHAR(50)  NOT NULL,
    reason                 LONGTEXT              DEFAULT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ATTENTE',
    request_date           DATE         NOT NULL,
    rh_comment             LONGTEXT              DEFAULT NULL,
    days_count             INT          NOT NULL,
    employee_id            INT          NOT NULL,
    request_category       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    workflow_status        VARCHAR(50)           DEFAULT NULL,
    urgency_level          VARCHAR(20)           DEFAULT NULL,
    expected_return_date   DATE                  DEFAULT NULL,
    attachment_path        VARCHAR(255)          DEFAULT NULL,
    admin_comment          LONGTEXT              DEFAULT NULL,
    rh_decision_at         DATETIME              DEFAULT NULL,
    rh_decision_by         VARCHAR(120)          DEFAULT NULL,
    admin_decision_at      DATETIME              DEFAULT NULL,
    admin_decision_by      VARCHAR(120)          DEFAULT NULL,
    audit_log              LONGTEXT              DEFAULT NULL,
    attachment_ocr_text    LONGTEXT              DEFAULT NULL,
    attachment_ocr_summary LONGTEXT              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_lr_employee (employee_id),
    KEY idx_lr_status   (status),
    CONSTRAINT fk_lr_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Colonnes optionnelles — ajoutées si absentes
-- (migration douce pour tables déjà existantes)
-- ============================================

ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS request_category       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL';
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS workflow_status        VARCHAR(50)           DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS urgency_level          VARCHAR(20)           DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS expected_return_date   DATE                  DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_path        VARCHAR(255)          DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_ocr_text    LONGTEXT              DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_ocr_summary LONGTEXT              DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_comment          LONGTEXT              DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS rh_decision_at         DATETIME              DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS rh_decision_by         VARCHAR(120)          DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_decision_at      DATETIME              DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_decision_by      VARCHAR(120)          DEFAULT NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS audit_log              LONGTEXT              DEFAULT NULL;

-- ============================================
-- SELECT — consultation des données
-- ============================================

-- Toutes les tables de la base
SELECT table_name, table_rows, create_time
FROM information_schema.tables
WHERE table_schema = 'defaultdb'
ORDER BY table_name;

-- Nombre de demandes par statut
SELECT status, COUNT(*) AS nb
FROM leave_requests
GROUP BY status
ORDER BY nb DESC;

-- Demandes en attente (les plus récentes d'abord)
SELECT lr.id, lr.employee_name, lr.leave_type, lr.start_date, lr.end_date,
       lr.days_count, lr.status, lr.request_date, lr.urgency_level
FROM leave_requests lr
WHERE lr.status = 'ATTENTE'
ORDER BY lr.request_date DESC
LIMIT 20;

-- Soldes de congés — tous les employés
SELECT lb.employee_id, lb.employee_name,
       lb.available_days, lb.total_accrued, lb.total_used,
       lb.hire_date, lb.last_accrual_date
FROM leave_balance lb
ORDER BY lb.available_days DESC;

-- Vue croisée : demandes + solde de l'employé
SELECT lr.id, lr.employee_name, lr.leave_type,
       lr.start_date, lr.end_date, lr.days_count,
       lr.status, lr.workflow_status,
       lb.available_days AS solde_dispo
FROM leave_requests lr
LEFT JOIN leave_balance lb ON lb.employee_id = lr.employee_id
ORDER BY lr.request_date DESC
LIMIT 30;

-- Demandes exceptionnelles (workflow RH/Admin)
SELECT id, employee_name, leave_type, start_date, end_date,
       request_category, workflow_status, urgency_level, status
FROM leave_requests
WHERE request_category = 'EXCEPTION'
ORDER BY request_date DESC;


-- ============================================
-- Table: leave_requests
-- Description: Stocke toutes les demandes de congés
-- ============================================

CREATE TABLE IF NOT EXISTS leave_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT NOT NULL,
    employee_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_type VARCHAR(100) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ATTENTE',
    request_date DATE NOT NULL,
    rh_comment TEXT,
    days_count INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_request_date (request_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Vérification de l'existence de la table
-- ============================================

-- Pour vérifier si la table existe:
-- SHOW TABLES LIKE 'leave_requests';

-- Pour voir la structure de la table:
-- DESCRIBE leave_requests;

-- ============================================
-- Requêtes utiles pour l'administration
-- ============================================

-- Compter le nombre total de demandes
-- SELECT COUNT(*) as total_requests FROM leave_requests;

-- Compter les demandes par statut
-- SELECT status, COUNT(*) as count 
-- FROM leave_requests 
-- GROUP BY status;

-- Voir les demandes en attente
-- SELECT * FROM leave_requests 
-- WHERE status = 'ATTENTE' 
-- ORDER BY request_date DESC;

-- Voir toutes les demandes d'un employé spécifique
-- SELECT * FROM leave_requests 
-- WHERE employee_id = ? 
-- ORDER BY request_date DESC;

-- Calculer le nombre total de jours de congés approuvés par employé
-- SELECT employee_id, employee_name, SUM(days_count) as total_days
-- FROM leave_requests
-- WHERE status = 'ACCEPTE'
-- GROUP BY employee_id, employee_name
-- ORDER BY total_days DESC;

-- ============================================
-- Données de test (optionnel)
-- ============================================

-- Insérer des demandes de test (décommenter si nécessaire)
/*
INSERT INTO leave_requests 
(employee_id, employee_name, start_date, end_date, leave_type, reason, status, request_date, days_count)
VALUES
(1, 'Jean Dupont', '2026-03-01', '2026-03-10', 'Congé annuel', 'Vacances familiales', 'ATTENTE', CURDATE(), 10),
(1, 'Jean Dupont', '2026-04-15', '2026-04-20', 'Congé maladie', 'Consultation médicale', 'ACCEPTE', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 6),
(2, 'Marie Martin', '2026-03-15', '2026-03-25', 'Congé annuel', 'Voyage', 'ATTENTE', CURDATE(), 11);
*/

-- ============================================
-- Nettoyage (à utiliser avec précaution)
-- ============================================

-- Supprimer toutes les demandes (ATTENTION: irréversible!)
-- DELETE FROM leave_requests;

-- Supprimer la table complètement
-- DROP TABLE IF EXISTS leave_requests;

-- ============================================
-- Maintenance
-- ============================================

-- Analyser la table pour optimiser les performances
-- ANALYZE TABLE leave_requests;

-- Optimiser la table
-- OPTIMIZE TABLE leave_requests;

-- Vérifier l'intégrité de la table
-- CHECK TABLE leave_requests;

-- ============================================
-- Statistiques utiles
-- ============================================

-- Demandes par mois
/*
SELECT 
    DATE_FORMAT(request_date, '%Y-%m') as month,
    COUNT(*) as total_requests,
    SUM(CASE WHEN status = 'ACCEPTE' THEN 1 ELSE 0 END) as accepted,
    SUM(CASE WHEN status = 'REFUSE' THEN 1 ELSE 0 END) as rejected,
    SUM(CASE WHEN status = 'ATTENTE' THEN 1 ELSE 0 END) as pending
FROM leave_requests
GROUP BY month
ORDER BY month DESC;
*/

-- Top 5 employés avec le plus de jours de congé
/*
SELECT 
    employee_name,
    COUNT(*) as total_requests,
    SUM(days_count) as total_days,
    SUM(CASE WHEN status = 'ACCEPTE' THEN days_count ELSE 0 END) as approved_days
FROM leave_requests
GROUP BY employee_id, employee_name
ORDER BY approved_days DESC
LIMIT 5;
*/

-- ============================================
-- Backup et Restore
-- ============================================

-- Exporter les données (à exécuter en ligne de commande)
-- mysqldump -h hrflow-hrflow.f.aivencloud.com -P 21031 -u avnadmin -p defaultdb leave_requests > leave_requests_backup.sql

-- Importer les données
-- mysql -h hrflow-hrflow.f.aivencloud.com -P 21031 -u avnadmin -p defaultdb < leave_requests_backup.sql
