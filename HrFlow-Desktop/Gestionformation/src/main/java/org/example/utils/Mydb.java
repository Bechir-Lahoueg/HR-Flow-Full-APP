package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Mydb {
    private static Mydb instance;
    private static Connection connection;

    // Hardcoded database credentials
    private static final String URL = "jdbc:mysql://hrflow-hrflow.f.aivencloud.com:21031/defaultdb?useSSL=true&requireSSL=true&autoReconnect=true&failOverReadOnly=false&maxReconnects=5";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "your_db_password_here";

    // Private constructor (Singleton)
    private Mydb() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Connected to the database!");
            initializeDatabase(); // create tables if needed
        } catch (SQLException e) {
            System.err.println("Database connection failed:");
            e.printStackTrace();
        }
    }

    public static Mydb getInstance() {
        if (instance == null) {
            instance = new Mydb();
        }
        return instance;
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                instance.connect();
            }
        } catch (SQLException e) {
            instance.connect();
        }
        return connection;
    }


    public void initializeDatabasePublic() {
        initializeDatabase(); // appelle la méthode private interne
    }

    private void initializeDatabase() {
        try (var stmt = connection.createStatement()) {

            // Table formation
            String formationSQL = """
            CREATE TABLE IF NOT EXISTS formation (
                id_formation INT AUTO_INCREMENT PRIMARY KEY,
                titre VARCHAR(255) NOT NULL,
                description TEXT,
                type VARCHAR(100) NOT NULL COMMENT 'Technique, Management, Soft Skills, etc.',
                duree INT NOT NULL COMMENT 'Durée en jours',
                organisme VARCHAR(255) NOT NULL COMMENT 'Organisme formateur',
                objectifs TEXT COMMENT 'Objectifs pédagogiques (un par ligne)',
                id_rh INT NULL COMMENT 'ID du RH créateur - permet de filtrer par RH',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_type (type),
                INDEX idx_id_rh (id_rh),
                INDEX idx_titre (titre)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """;
            stmt.execute(formationSQL);

            // Table session_formation
            String sessionSQL = """
            CREATE TABLE IF NOT EXISTS session_formation (
                id_session INT AUTO_INCREMENT PRIMARY KEY,
                id_formation INT NOT NULL,
                date_debut DATE NOT NULL,
                date_fin DATE NOT NULL,
                lieu VARCHAR(255) NOT NULL COMMENT 'Lieu physique ou URL pour formation distancielle',
                mode VARCHAR(50) NOT NULL COMMENT 'Présentiel, Distanciel, Hybride',
                capacite_max INT NOT NULL COMMENT 'Nombre maximum de participants',
                statut VARCHAR(50) NOT NULL DEFAULT 'Planifiée' COMMENT 'Planifiée, En cours, Terminée, Annulée',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (id_formation) REFERENCES formation(id_formation) ON DELETE CASCADE,
                INDEX idx_dates (date_debut, date_fin),
                INDEX idx_statut (statut),
                INDEX idx_formation (id_formation)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """;
            stmt.execute(sessionSQL);

            // Table participation_formation - Updated with correct column name id_utilisateur_id
            String participationSQL = """
            CREATE TABLE IF NOT EXISTS participation_formation (
                id_participation INT AUTO_INCREMENT PRIMARY KEY,
                id_session INT NOT NULL,
                id_utilisateur_id INT NOT NULL COMMENT 'ID de l''employé participant',
                date_inscription DATE NOT NULL,
                statut_participation VARCHAR(50) NOT NULL DEFAULT 'Inscrit' COMMENT 'Inscrit, Présent, Absent, Annulé',
                resultat VARCHAR(50) NULL COMMENT 'Réussi, Échoué, En cours',
                note DECIMAL(5,2) NULL COMMENT 'Note obtenue (optionnel)',
                certificat_obtenu BOOLEAN DEFAULT FALSE,
                token VARCHAR(255) UNIQUE NULL COMMENT 'Token for invitation',
                quiz_score INT NULL COMMENT 'Quiz score',
                quiz_passed BOOLEAN DEFAULT FALSE,
                quiz_attempted_at TIMESTAMP NULL,
                quiz_correct_count INT DEFAULT 0,
                quiz_total_questions INT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (id_session) REFERENCES session_formation(id_session) ON DELETE CASCADE,
                FOREIGN KEY (id_utilisateur_id) REFERENCES employees(id) ON DELETE CASCADE,
                INDEX idx_utilisateur (id_utilisateur_id),
                INDEX idx_statut (statut_participation),
                INDEX idx_session (id_session),
                UNIQUE KEY unique_participation (id_session, id_utilisateur_id) COMMENT 'Un utilisateur ne peut s''inscrire qu''une fois par session'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """;
            stmt.execute(participationSQL);

            System.out.println("✓ Toutes les tables ont été vérifiées/créées avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("❌ Impossible d'initialiser les tables", e);
        }
    }

}