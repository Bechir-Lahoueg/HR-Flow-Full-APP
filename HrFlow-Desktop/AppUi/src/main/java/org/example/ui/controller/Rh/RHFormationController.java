package org.example.ui.controller.Rh;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import org.example.model.User;
import org.example.models.Formation;
import org.example.models.SessionFormation;
import org.example.models.ParticipationFormation;
import org.example.models.Presence;
import org.example.services.FormationService;
import org.example.services.SessionFormationService;
import org.example.services.ParticipationFormationService;
import org.example.services.PresenceService;
import org.example.services.SessionFeedbackService;
import org.example.services.FormationChangeNotificationService;
import org.example.services.JitsiMeetService;
import org.example.services.OpenAIService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur unifié pour la gestion des formations côté RH.
 *
 * Améliorations apportées (inspirées du contrôleur Symfony) :
 *  1. Séparation des responsabilités — toute logique métier délégée aux services.
 *  2. Filtrage/tri/recherche dynamiques sur les formations.
 *  3. Notifications automatiques lors des modifications/suppressions (formations & sessions).
 *  4. Gestion des présences par session (sessions "En cours" uniquement).
 *  5. Vue dédiée aux feedbacks/avis par formation.
 *  6. Approbation avec priorité (priorityOnly) et refus avec motif.
 *  7. Vue "Sessions actives" (En cours) séparée.
 *  8. Génération automatique de lien Jitsi pour les sessions en ligne.
 *  9. Statistiques enrichies : top formations, top formateurs, insights RH.
 * 10. Validation renforcée côté dialogue (dates, lieu, capacité, URL en ligne).
 */
public class RHFormationController {

    // ==================== TAB 1: FORMATIONS ====================
    @FXML private FlowPane formationsContainer;
    @FXML private TextField searchFieldFormations;
    @FXML private ComboBox<String> cbFilterType;
    @FXML private ComboBox<String> cbSortFormations;
    @FXML private Button btnRefresh;
    @FXML private Button btnAddFormation;
    @FXML private Button btnGenerateObjectives;
    @FXML private TextArea txtObjectifs;
    @FXML private TextField txtTitre;
    @FXML private Label lblTotalFormationsCard;   // ← AJOUTER CETTE LIGNE


    // Stats globales
    @FXML private Label lblTotalFormations;
    @FXML private Label lblTopFormation;
    @FXML private Label lblTopFormateur;

    // ==================== TAB 2: SESSIONS ====================
    @FXML private FlowPane sessionsContainer;
    @FXML private ComboBox<Formation> filterSessionFormation;
    @FXML private Button btnAddSession;
    @FXML private Button btnViewActiveSessions;
    @FXML private Label lblTotalSessions;
    @FXML private Label lblSessionsPlanifiees;
    @FXML private Label lblSessionsEnCours;
    @FXML private Label lblSessionsTerminees;

    // ==================== TAB 3: PARTICIPANTS ====================
    @FXML private ComboBox<Formation> cbFormationForParticipants;
    @FXML private ComboBox<String> cbFilterStatutParticipants;
    @FXML private CheckBox chkPriorityOnly;
    @FXML private VBox vboxParticipants;
    @FXML private Button btnRefreshParticipants;

    // ==================== TAB 4: PRÉSENCES ====================
    @FXML private VBox presencesPanel;
    @FXML private ComboBox<SessionFormation> cbSessionForPresence;
    @FXML private DatePicker dpPresenceDate;
    @FXML private VBox vboxPresences;

    // ==================== TAB 5: FEEDBACKS ====================
    @FXML private VBox feedbacksPanel;
    @FXML private ComboBox<Formation> cbFormationForFeedbacks;
    @FXML private VBox vboxFeedbacks;

    // ==================== NAVIGATION ====================
    @FXML private Button btnTabFormations;
    @FXML private Button btnTabSessions;
    @FXML private Button btnTabParticipants;
    @FXML private Button btnTabPresences;
    @FXML private Button btnTabFeedbacks;
    @FXML private VBox formationsPanel;
    @FXML private VBox sessionsPanel;
    @FXML private VBox participantsPanel;

    // ==================== SERVICES ====================
    private final FormationService formationService;
    private final SessionFormationService sessionService;
    private final ParticipationFormationService participationService;
    private final PresenceService presenceService;
    private final SessionFeedbackService feedbackService;
    private final FormationChangeNotificationService notificationService;
    private final JitsiMeetService jitsiMeetService;

    // ==================== STATE ====================
    private final ObservableList<Formation> formationsList = FXCollections.observableArrayList();
    private final ObservableList<SessionFormation> sessionsList = FXCollections.observableArrayList();
    private User currentUser;
    private int rhId;

    public RHFormationController() {
        this.formationService = new FormationService();
        this.sessionService = new SessionFormationService();
        this.participationService = new ParticipationFormationService();
        this.presenceService = new PresenceService();
        this.feedbackService = new SessionFeedbackService();
        this.notificationService = new FormationChangeNotificationService();
        this.jitsiMeetService = new JitsiMeetService();
    }

    // ========================================================
    //  INITIALISATION
    // ========================================================

    @FXML
    public void initialize() {
        setupFormationsTab();
        setupSessionsTab();
        setupParticipantsTab();
        setupPresencesTab();
        setupFeedbacksTab();
        setupModernNavigation();

        configureFlowPane(formationsContainer);
        configureFlowPane(sessionsContainer);

        // Bouton IA pour la génération d'objectifs depuis le formulaire principal
        if (btnGenerateObjectives != null && txtObjectifs != null && txtTitre != null) {
            btnGenerateObjectives.setOnAction(e -> generateObjectivesAsync(txtTitre, txtObjectifs));
        }
    }

    private void configureFlowPane(FlowPane pane) {
        if (pane == null) return;
        pane.setHgap(12);
        pane.setVgap(12);
        pane.setPadding(new Insets(12));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.rhId = user.getId();
        loadAllDataAsync();
    }

    private void loadAllDataAsync() {
        javafx.application.Platform.runLater(() -> {
            loadAllData();
            displayParticipantsForFormation();
        });
    }

    // ========================================================
    //  SETUP ONGLETS
    // ========================================================

    private void setupFormationsTab() {
        // Filtres dynamiques (type + tri) — comme le contrôleur Symfony
        if (cbFilterType != null) {
            cbFilterType.setItems(FXCollections.observableArrayList(
                    "Tous les types", "Technique", "Soft Skills", "Management", "Langues", "Autre"
            ));
            cbFilterType.setValue("Tous les types");
            cbFilterType.setOnAction(e -> loadFormations());
        }

        if (cbSortFormations != null) {
            cbSortFormations.setItems(FXCollections.observableArrayList(
                    "Titre A→Z", "Titre Z→A", "Durée croissante", "Durée décroissante", "Mieux notées"
            ));
            cbSortFormations.setValue("Titre A→Z");
            cbSortFormations.setOnAction(e -> loadFormations());
        }

        if (searchFieldFormations != null) {
            searchFieldFormations.textProperty().addListener((obs, o, n) -> filterFormations(n));
        }
    }

    private void setupSessionsTab() {
        if (filterSessionFormation != null) {
            filterSessionFormation.setItems(formationsList);
            filterSessionFormation.setConverter(formationStringConverter("Toutes les formations"));
            filterSessionFormation.setOnAction(e -> loadSessions());
        }

        if (btnViewActiveSessions != null) {
            btnViewActiveSessions.setOnAction(e -> showActiveSessionsDialog());
        }
    }

    private void setupParticipantsTab() {
        if (cbFormationForParticipants != null) {
            cbFormationForParticipants.setItems(formationsList);
            cbFormationForParticipants.setConverter(formationStringConverter("Toutes les formations"));
            cbFormationForParticipants.setOnAction(e -> displayParticipantsForFormation());
        }

        // Filtre par statut — comme le paramètre ?status= du contrôleur Symfony
        if (cbFilterStatutParticipants != null) {
            cbFilterStatutParticipants.setItems(FXCollections.observableArrayList(
                    "Tous", "En attente", "Accepte", "Refuse"
            ));
            cbFilterStatutParticipants.setValue("Tous");
            cbFilterStatutParticipants.setOnAction(e -> displayParticipantsForFormation());
        }

        // Filtre priorité — comme le paramètre ?priorityOnly= du contrôleur Symfony
        if (chkPriorityOnly != null) {
            chkPriorityOnly.setOnAction(e -> displayParticipantsForFormation());
        }

        if (btnRefreshParticipants != null) {
            btnRefreshParticipants.setOnAction(e -> {
                loadAllData();
                displayParticipantsForFormation();
            });
        }
    }

    private void setupPresencesTab() {
        if (cbSessionForPresence != null) {
            cbSessionForPresence.setConverter(new javafx.util.StringConverter<SessionFormation>() {
                @Override public String toString(SessionFormation s) {
                    return s == null ? "Sélectionnez une session" : s.getLieu() + " (" + s.getDateDebut() + ")";
                }
                @Override public SessionFormation fromString(String s) { return null; }
            });
            cbSessionForPresence.setOnAction(e -> loadPresences());
        }

        if (dpPresenceDate != null) {
            dpPresenceDate.setValue(LocalDate.now());
            dpPresenceDate.setOnAction(e -> loadPresences());
        }
    }

    private void setupFeedbacksTab() {
        if (cbFormationForFeedbacks != null) {
            cbFormationForFeedbacks.setItems(formationsList);
            cbFormationForFeedbacks.setConverter(formationStringConverter("Sélectionnez une formation"));
            cbFormationForFeedbacks.setOnAction(e -> loadFeedbacks());
        }
    }

    private void setupModernNavigation() {
        bindNav(btnTabFormations, "formations");
        bindNav(btnTabSessions, "sessions");
        bindNav(btnTabParticipants, "participants");
        bindNav(btnTabPresences, "presences");
        bindNav(btnTabFeedbacks, "feedbacks");
        navigateToPanel("formations");
    }

    private void bindNav(Button btn, String panel) {
        if (btn != null) btn.setOnAction(e -> navigateToPanel(panel));
    }

    private void navigateToPanel(String panelName) {
        List.of(formationsPanel, sessionsPanel, participantsPanel, presencesPanel, feedbacksPanel)
                .forEach(p -> { if (p != null) p.setVisible(false); });

        updateNavigationButtonStyles(panelName);

        VBox target = switch (panelName.toLowerCase()) {
            case "formations"  -> formationsPanel;
            case "sessions"    -> sessionsPanel;
            case "participants"-> participantsPanel;
            case "presences"   -> presencesPanel;
            case "feedbacks"   -> feedbacksPanel;
            default            -> null;
        };
        if (target != null) target.setVisible(true);
    }

    private void updateNavigationButtonStyles(String activePanel) {
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #a0aec0; -fx-padding: 16 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-width: 0 0 3 0; -fx-border-color: transparent; -fx-border-radius: 0; -fx-effect: null;";
        String active   = "-fx-background-color: transparent; -fx-text-fill: #667eea; -fx-padding: 16 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-width: 0 0 3 0; -fx-border-color: #667eea; -fx-border-radius: 0; -fx-effect: null;";

        Map<String, Button> navMap = Map.of(
                "formations",   btnTabFormations   != null ? btnTabFormations   : new Button(),
                "sessions",     btnTabSessions     != null ? btnTabSessions     : new Button(),
                "participants", btnTabParticipants != null ? btnTabParticipants : new Button(),
                "presences",    btnTabPresences    != null ? btnTabPresences    : new Button(),
                "feedbacks",    btnTabFeedbacks    != null ? btnTabFeedbacks    : new Button()
        );

        navMap.forEach((panel, btn) -> btn.setStyle(panel.equals(activePanel.toLowerCase()) ? active : inactive));
    }

    // ========================================================
    //  CHARGEMENT DES DONNÉES
    // ========================================================

    private void loadAllData() {
        loadFormations();
        loadSessions();
        refreshPresenceSessionCombo();
    }

    @FXML
    private void handleRefresh() {
        loadAllData();
        displayParticipantsForFormation();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation", "✅ Les données ont été actualisées avec succès !");
    }

    // -------------------------------------------------------
    //  Formations — avec filtres & tri dynamiques
    // -------------------------------------------------------

    private void loadFormations() {
        formationsContainer.getChildren().clear();
        formationsList.clear();

        String search  = searchFieldFormations != null ? searchFieldFormations.getText().toLowerCase() : "";
        String type    = cbFilterType != null && !"Tous les types".equals(cbFilterType.getValue()) ? cbFilterType.getValue() : "";
        String sort    = cbSortFormations != null ? cbSortFormations.getValue() : "Titre A→Z";

        // Récupération filtrée depuis le service (comme le contrôleur Symfony)
        List<Formation> formations = formationService.getFormationsByRhFiltered(this.rhId, search, type, sort);
        formationsList.addAll(formations);

        // Calcul des ratings en lot (comme getAverageRatingsByFormationIds)
        List<Integer> ids = formations.stream().map(Formation::getIdFormation).toList();
        Map<Integer, Double> ratingMap = feedbackService.getAverageRatingsByFormationIds(ids);

        // Top insights — comme getTopInsightsByRhId dans Symfony
        Map<String, Object> insights = formationService.getTopInsightsByRhId(this.rhId);

        updateFormationStats(formations.size(), insights);

        for (Formation f : formations) {
            double rating = ratingMap.getOrDefault(f.getIdFormation(), 0.0);
            VBox card = createFormationCard(f, rating);
            formationsContainer.getChildren().add(card);
        }
    }

    private void updateFormationStats(int total, Map<String, Object> insights) {
        if (lblTotalFormations   != null) lblTotalFormations.setText(String.valueOf(total));
        if (lblTotalFormationsCard != null) lblTotalFormationsCard.setText(String.valueOf(total));
        if (lblTopFormation != null && insights.containsKey("topFormation"))
            lblTopFormation.setText((String) insights.get("topFormation"));
        if (lblTopFormateur != null && insights.containsKey("topFormateur"))
            lblTopFormateur.setText((String) insights.get("topFormateur"));
    }

    @FXML
    private void handleRefreshFormations() { loadFormations(); }

    // -------------------------------------------------------
    //  Sessions
    // -------------------------------------------------------

    private void loadSessions() {
        try {
            sessionsList.clear();
            List<Formation> rhFormations = formationService.getFormationsByRh(this.rhId);
            Set<Integer> rhFormationIds = rhFormations.stream()
                    .map(Formation::getIdFormation)
                    .collect(Collectors.toSet());

            sessionService.getAllSessions().stream()
                    .filter(s -> rhFormationIds.contains(s.getIdFormation()))
                    .forEach(sessionsList::add);

            sessionsContainer.getChildren().clear();
            Formation selected = filterSessionFormation != null ? filterSessionFormation.getValue() : null;

            for (SessionFormation s : sessionsList) {
                if (selected == null || s.getIdFormation() == selected.getIdFormation()) {
                    sessionsContainer.getChildren().add(createSessionCard(s));
                }
            }

            updateSessionStats();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefreshSessions() { loadSessions(); }

    /**
     * Vue dédiée aux sessions "En cours" — comme rh_formation_active_sessions dans Symfony.
     */
    private void showActiveSessionsDialog() {
        List<SessionFormation> activeSessions = sessionService.getActiveSessionsByRh(this.rhId);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sessions actives");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setPrefWidth(560);

        if (activeSessions.isEmpty()) {
            content.getChildren().add(new Label("Aucune session en cours."));
        } else {
            for (SessionFormation s : activeSessions) {
                VBox card = createSessionCard(s);
                content.getChildren().add(card);
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    // -------------------------------------------------------
    //  Présences — comme rh_formation_session_presence dans Symfony
    // -------------------------------------------------------

    private void refreshPresenceSessionCombo() {
        if (cbSessionForPresence == null) return;
        List<SessionFormation> allSessions = sessionService.getAllSessionsByRh(this.rhId);
        cbSessionForPresence.setItems(FXCollections.observableArrayList(allSessions));
    }

    private void loadPresences() {
        if (vboxPresences == null || cbSessionForPresence == null) return;
        vboxPresences.getChildren().clear();

        SessionFormation session = cbSessionForPresence.getValue();
        if (session == null) return;

        boolean editable = !"Terminee".equals(session.getStatut());

        LocalDate selectedDate = dpPresenceDate != null ? dpPresenceDate.getValue() : LocalDate.now();
        if (selectedDate == null) selectedDate = LocalDate.now();

        List<ParticipationFormation> accepted = participationService
                .getParticipationsBySession(session.getIdSession()).stream()
                .filter(p -> "Accepte".equals(p.getStatutParticipation()))
                .toList();

        Map<Integer, String> existing = presenceService
                .getPresencesBySessionAndDate(session.getIdSession(), selectedDate).stream()
                .collect(Collectors.toMap(Presence::getIdParticipation, Presence::getStatut));

        if (!editable) {
            Label readOnlyNote = new Label("📋 Session " + session.getStatut() + " — affichage lecture seule");
            readOnlyNote.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 0 0 8 0;");
            vboxPresences.getChildren().add(readOnlyNote);
        }

        for (ParticipationFormation p : accepted) {
            HBox row = buildPresenceRow(p, existing.getOrDefault(p.getIdParticipation(), "Absent"), session, selectedDate, editable);
            vboxPresences.getChildren().add(row);
        }

        if (accepted.isEmpty()) {
            vboxPresences.getChildren().add(new Label("Aucun participant accepté pour cette session."));
        }
    }

    private HBox buildPresenceRow(ParticipationFormation p, String currentStatut,
                                  SessionFormation session, LocalDate date, boolean editable) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label name = new Label("👤 " + p.getNomEmployee());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #022E69;");
        name.setPrefWidth(200);

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbPresent = new RadioButton("Présent");
        RadioButton rbAbsent  = new RadioButton("Absent");
        rbPresent.setToggleGroup(tg);
        rbAbsent.setToggleGroup(tg);
        rbPresent.setSelected("Present".equals(currentStatut));
        rbAbsent.setSelected(!"Present".equals(currentStatut));

        Button btnSave = new Button("💾");
        btnSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
        rbPresent.setDisable(!editable);
        rbAbsent.setDisable(!editable);
        btnSave.setDisable(!editable);
        if (!editable) btnSave.setVisible(false);

        btnSave.setOnAction(e -> {
            String statut = rbPresent.isSelected() ? "Present" : "Absent";
            presenceService.savePresence(session.getIdSession(), p.getIdParticipation(), date, statut);
            showAlert(Alert.AlertType.INFORMATION, "Présence", "✅ Présence enregistrée !");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(name, rbPresent, rbAbsent, spacer, btnSave);
        return row;
    }

    // -------------------------------------------------------
    //  Feedbacks — comme rh_formation_feedbacks dans Symfony
    // -------------------------------------------------------

    private void loadFeedbacks() {
        if (vboxFeedbacks == null || cbFormationForFeedbacks == null) return;
        vboxFeedbacks.getChildren().clear();

        Formation formation = cbFormationForFeedbacks.getValue();
        if (formation == null) return;

        List<?> feedbacks = feedbackService.getFeedbacksByFormation(formation.getIdFormation());
        if (feedbacks.isEmpty()) {
            vboxFeedbacks.getChildren().add(new Label("Aucun feedback pour cette formation."));
            return;
        }

        for (Object fb : feedbacks) {
            VBox card = createFeedbackCard(fb);
            vboxFeedbacks.getChildren().add(card);
        }
    }

    @SuppressWarnings("unchecked")
    private VBox createFeedbackCard(Object fb) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #D0D0D0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;");

        if (fb instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) fb;

            // Note (étoiles)
            int rating = map.get("rating") instanceof Integer ? (int) map.get("rating") : 0;
            String stars = "⭐".repeat(Math.max(0, Math.min(5, rating)));
            Label lblRating = new Label(stars + "  (" + rating + "/5)");
            lblRating.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #f39c12;");

            // Commentaire général
            String comment = map.get("comment") instanceof String ? (String) map.get("comment") : "";
            if (comment != null && !comment.isBlank()) {
                Label lblComment = new Label("💬 " + comment);
                lblComment.setWrapText(true);
                lblComment.setStyle("-fx-text-fill: #34495e;");
                card.getChildren().addAll(lblRating, lblComment);
            } else {
                card.getChildren().add(lblRating);
            }

            // Date
            Object createdAt = map.get("createdAt");
            if (createdAt != null) {
                Label lblDate = new Label("🕐 " + createdAt.toString().substring(0, 10));
                lblDate.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
                card.getChildren().add(lblDate);
            }
        } else {
            Label lblContent = new Label(fb.toString());
            lblContent.setWrapText(true);
            lblContent.setStyle("-fx-text-fill: #34495e;");
            card.getChildren().add(lblContent);
        }
        return card;
    }

    // ========================================================
    //  PARTICIPANTS — avec filtre statut + priorité (Symfony)
    // ========================================================

    private void displayParticipantsForFormation() {
        if (vboxParticipants == null) return;
        vboxParticipants.getChildren().clear();

        Formation selectedFormation = cbFormationForParticipants != null ? cbFormationForParticipants.getValue() : null;
        String statusFilter = cbFilterStatutParticipants != null ? cbFilterStatutParticipants.getValue() : "Tous";
        boolean priorityOnly = chkPriorityOnly != null && chkPriorityOnly.isSelected();

        Integer formationId = selectedFormation != null ? selectedFormation.getIdFormation() : null;

        try {
            // Délégation au service comme participationService.getRhParticipations(...) dans Symfony
            List<ParticipationFormation> allParticipations = participationService.getRhParticipations(
                    this.rhId,
                    "Tous".equals(statusFilter) ? "" : statusFilter,
                    formationId,
                    priorityOnly
            );

            if (allParticipations.isEmpty()) {
                displayNoParticipantsMessage();
                return;
            }

            // Grouper par formation → session
            Map<Integer, List<ParticipationFormation>> bySession = allParticipations.stream()
                    .collect(Collectors.groupingBy(ParticipationFormation::getIdSession));

            for (Map.Entry<Integer, List<ParticipationFormation>> entry : bySession.entrySet()) {
                SessionFormation session = sessionService.getSessionById(entry.getKey());
                if (session == null) continue;

                VBox card = createSessionParticipantsCard(session, entry.getValue());
                vboxParticipants.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les participants : " + e.getMessage());
        }
    }

    // ========================================================
    //  HANDLERS FORMATIONS
    // ========================================================

    @FXML
    private void handleAddFormation() {
        Dialog<Formation> dialog = createFormationDialog(null);
        dialog.showAndWait().ifPresent(formation -> {
            formationService.addFormation(formation);
            loadFormations();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Formation ajoutée avec succès !");
        });
    }

    private void handleEditFormationFromCard(Formation formation) {
        if (formation == null) return;
        createFormationDialog(formation).showAndWait().ifPresent(updated -> {
            formationService.updateFormation(updated);
            // Notification automatique des inscrits — comme Symfony
            int notified = notificationService.notifyFormationUpdated(updated);
            loadFormations();
            String msg = "✅ Formation modifiée avec succès !";
            if (notified > 0) msg += "\n📧 " + notified + " notification(s) envoyée(s) aux inscrits.";
            showAlert(Alert.AlertType.INFORMATION, "Succès", msg);
        });
    }

    private void handleDeleteFormationFromCard(Formation formation) {
        if (formation == null) return;

        Alert confirm = buildStyledConfirmation(
                "🗑️ Supprimer la formation", "Cette action est permanente", "#e74c3c",
                "Êtes-vous sûr de vouloir supprimer cette formation ?\n\n" +
                        "Formation : " + formation.getTitre() + "\n⚠️ Cette action ne peut pas être annulée !"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Notification avant suppression — comme notifyFormationDeleted dans Symfony
                int notified = notificationService.notifyFormationDeleted(formation);
                formationService.deleteFormationWithRelations(formation.getIdFormation());
                loadFormations();
                String msg = "✅ Formation supprimée avec succès !";
                if (notified > 0) msg += "\n📧 " + notified + " notification(s) envoyée(s) aux inscrits.";
                showAlert(Alert.AlertType.INFORMATION, "Succès", msg);
            }
        });
    }

    // ========================================================
    //  HANDLERS SESSIONS
    // ========================================================

    @FXML
    private void handleAddSession() {
        createSessionDialog(null).showAndWait().ifPresent(session -> {
            // Auto-lien Jitsi si mode en ligne — comme applyOnlineMeetingLinkIfNeeded dans Symfony
            applyOnlineMeetingLinkIfNeeded(session);
            sessionService.addSession(session);
            loadSessions();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Session ajoutée avec succès !");
        });
    }

    private void handleEditSessionFromCard(SessionFormation session) {
        if (session == null) return;
        createSessionDialog(session).showAndWait().ifPresent(updated -> {
            applyOnlineMeetingLinkIfNeeded(updated);
            sessionService.updateSession(updated);
            // Notification automatique — comme notifySessionUpdated dans Symfony
            int notified = notificationService.notifySessionUpdated(updated);
            loadSessions();
            String msg = "✅ Session modifiée avec succès !";
            if (notified > 0) msg += "\n📧 " + notified + " notification(s) envoyée(s) aux inscrits.";
            showAlert(Alert.AlertType.INFORMATION, "Succès", msg);
        });
    }

    private void handleDeleteSessionFromCard(SessionFormation session) {
        if (session == null) return;

        Alert confirm = buildStyledConfirmation(
                "🗑️ Supprimer la session", "Cette action est permanente", "#e74c3c",
                "Session à " + session.getLieu() +
                        " du " + session.getDateDebut() + " au " + session.getDateFin() +
                        "\n\n⚠️ Cette action ne peut pas être annulée !"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int notified = notificationService.notifySessionDeleted(session);
                    sessionService.deleteSession(session.getIdSession());
                    loadSessions();
                    String msg = "✅ Session supprimée avec succès !";
                    if (notified > 0) msg += "\n📧 " + notified + " notification(s) envoyée(s) aux inscrits.";
                    showAlert(Alert.AlertType.INFORMATION, "Succès", msg);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur lors de la suppression de la session.");
                }
            }
        });
    }

    // ========================================================
    //  HANDLERS PARTICIPANTS
    // ========================================================

    private void handleValidateParticipantCard(ParticipationFormation participation) {
        int placesDisponibles = sessionService.getPlacesDisponibles(participation.getIdSession());
        if (placesDisponibles <= 0) {
            showAlert(Alert.AlertType.WARNING, "Aucune place disponible",
                    "Cette session n'a plus de places disponibles. Impossible d'accepter ce participant.");
            return;
        }

        SessionFormation session = sessionService.getSessionById(participation.getIdSession());
        String capacityInfo = session != null
                ? "\n\nPlaces restantes après acceptation : " + (placesDisponibles - 1) + "/" + session.getCapaciteMax()
                : "";

        Alert confirm = buildStyledConfirmation(
                "✅ Accepter le participant", null, "#27ae60",
                "Employé : " + participation.getNomEmployee() + capacityInfo
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Approbation avec priorité — comme approveWithPriority dans Symfony
                Map<String, Object> result = participationService.approveWithPriority(participation.getIdParticipation());
                boolean ok = (boolean) result.getOrDefault("ok", false);
                String message = (String) result.getOrDefault("message", ok ? "✅ Participant accepté !" : "❌ Erreur.");
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, ok ? "Succès" : "Erreur", message);
                displayParticipantsForFormation();
            }
        });
    }

    private void handleRejectParticipantCard(ParticipationFormation participation) {
        // Saisie du motif de refus — comme le paramètre refusal_reason dans Symfony
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Motif de refus");
        reasonDialog.setHeaderText("Refuser la participation de " + participation.getNomEmployee());
        reasonDialog.setContentText("Motif (optionnel) :");

        reasonDialog.showAndWait().ifPresent(reason -> {
            participationService.updateStatus(
                    participation.getIdParticipation(),
                    "Refuse",
                    reason.isBlank() ? null : reason.trim()
            );
            displayParticipantsForFormation();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "❌ Participant refusé !");
        });
    }

    // ========================================================
    //  CRÉATION DE CARTES UI
    // ========================================================

    private VBox createFormationCard(Formation f, double avgRating) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #D0D0D0; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 3);");

        Label titleLabel = new Label(f.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #022E69;");

        // Rating depuis le ratingMap — comme dans le contrôleur Symfony
        HBox ratingBox = new HBox(5);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        Label lblStars = new Label(avgRating > 0 ? getStarRating(avgRating) : "☆☆☆☆☆");
        lblStars.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");
        Label lblNote = new Label(avgRating > 0 ? String.format("(%.1f/5)", avgRating) : "(Aucun avis)");
        lblNote.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        ratingBox.getChildren().addAll(lblStars, lblNote);

        Label descriptionLabel = new Label(f.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(60);
        descriptionLabel.setStyle("-fx-text-fill: #34495e;");

        Label infoLabel = new Label(f.getType() + " • " + f.getDuree() + " jours");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        Label objectifsTitle = new Label("🎯 Objectifs");
        objectifsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #145EB7; -fx-font-size: 13px;");
        Label objectifsContent = new Label(f.getObjectifs() != null ? f.getObjectifs() : "Non définis");
        objectifsContent.setWrapText(true);
        objectifsContent.setMaxHeight(80);
        objectifsContent.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px;");

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit     = styledButton("✏️ Modifier",   "#145EB7");
        Button btnDelete   = styledButton("🗑️ Supprimer",  "#e74c3c");
        Button btnSessions = styledButton("📅 Sessions",   "#3FA9F5");
        Button btnFeedback = styledButton("⭐ Avis",       "#f39c12");

        btnEdit.setOnAction(e -> handleEditFormationFromCard(f));
        btnDelete.setOnAction(e -> handleDeleteFormationFromCard(f));
        btnSessions.setOnAction(e -> {
            if (filterSessionFormation != null) filterSessionFormation.setValue(f);
            navigateToPanel("sessions");
        });
        // Raccourci vers l'onglet feedbacks — nouveau
        btnFeedback.setOnAction(e -> {
            if (cbFormationForFeedbacks != null) cbFormationForFeedbacks.setValue(f);
            navigateToPanel("feedbacks");
            loadFeedbacks();
        });

        buttonBox.getChildren().addAll(btnEdit, btnDelete, btnSessions, btnFeedback);
        card.getChildren().addAll(titleLabel, ratingBox, infoLabel, descriptionLabel,
                new VBox(5, objectifsTitle, objectifsContent), buttonBox);
        return card;
    }

    private VBox createSessionCard(SessionFormation s) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setCursor(Cursor.HAND);
        card.setStyle("-fx-background-color: white; -fx-border-color: #D0D0D0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        DropShadow hover = new DropShadow(6, 0, 2, Color.rgb(20, 94, 183, 0.2));
        card.setOnMouseEntered(e -> card.setEffect(hover));
        card.setOnMouseExited(e -> card.setEffect(null));

        String nomFormation = formationsList.stream()
                .filter(f -> f.getIdFormation() == s.getIdFormation())
                .map(Formation::getTitre).findFirst().orElse("Formation inconnue");

        Label lblFormation = new Label("📚 " + nomFormation);
        lblFormation.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblFormation.setStyle("-fx-text-fill: #022E69;");
        lblFormation.setWrapText(true);

        int placesDisponibles = sessionService.getPlacesDisponibles(s.getIdSession());
        int capaciteMax = s.getCapaciteMax();
        Label lblCap = new Label(placesDisponibles + "/" + capaciteMax + " places disponibles");
        lblCap.setStyle(placesDisponibles == 0
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-font-weight: bold;"
                : placesDisponibles <= capaciteMax / 4
                ? "-fx-text-fill: #f39c12; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: #27ae60; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label lblLieu = new Label("📍 " + s.getLieu());
        lblLieu.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblLieu.setStyle("-fx-text-fill: #145EB7;");

        HBox header = new HBox(8, lblLieu, new Region(), lblCap);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblDate = new Label("📅 " + s.getDateDebut() + " au " + s.getDateFin());
        lblDate.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12px;");

        Label lblMode = new Label("💻 Mode : " + s.getMode());
        lblMode.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12px;");

        // Statut calculé dynamiquement
        LocalDate today = LocalDate.now();
        String statut;
        String statutColor;
        if (today.isBefore(s.getDateDebut()))       { statut = "Planifiée"; statutColor = "#f39c12"; }
        else if (today.isAfter(s.getDateFin()))     { statut = "Terminée";  statutColor = "#3FA9F5"; }
        else                                         { statut = "En cours";  statutColor = "#27ae60"; }

        Label lblStatut = new Label("● " + statut);
        lblStatut.setStyle("-fx-text-fill: " + statutColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit    = styledButton("✏️ Modifier",   "#145EB7");
        Button btnDelete  = styledButton("🗑️ Supprimer",  "#e74c3c");
        Button btnPresence = styledButton("📋 Présences", "#27ae60");

        btnEdit.setOnAction(e -> handleEditSessionFromCard(s));
        btnDelete.setOnAction(e -> handleDeleteSessionFromCard(s));
        // Raccourci vers l'onglet présences — nouveau
        btnPresence.setOnAction(e -> {
            if (cbSessionForPresence != null) cbSessionForPresence.setValue(s);
            navigateToPanel("presences");
            loadPresences();
        });

        buttonBox.getChildren().addAll(btnEdit, btnDelete, btnPresence);
        card.getChildren().addAll(lblFormation, header, lblDate, lblMode, lblStatut, buttonBox);
        return card;
    }

    private VBox createSessionParticipantsCard(SessionFormation session, List<ParticipationFormation> allParticipants) {
        VBox mainCard = new VBox(15);
        mainCard.setPadding(new Insets(20));
        mainCard.setStyle("-fx-background-color: white; -fx-border-color: #D0D0D0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        Label titleLabel = new Label("📅 Session du " + session.getDateDebut() + " au " + session.getDateFin());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #022E69;");
        Label locationLabel = new Label("📍 " + session.getLieu() + " • 💻 " + session.getMode());
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        int placesDisponibles = sessionService.getPlacesDisponibles(session.getIdSession());
        int capaciteMax = session.getCapaciteMax();
        Label placesLabel = new Label("👥 Places : " + placesDisponibles + "/" + capaciteMax);
        placesLabel.setStyle(placesDisponibles == 0
                ? "-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                : "-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        mainCard.getChildren().addAll(titleLabel, locationLabel, placesLabel, new Separator());

        Map<String, List<ParticipationFormation>> byStatus = allParticipants.stream()
                .collect(Collectors.groupingBy(ParticipationFormation::getStatutParticipation));

        addParticipantSection(mainCard, byStatus.get("En attente"), "⏳ Demandes en attente", "#f39c12", "pending");
        addParticipantSection(mainCard, byStatus.get("Accepte"),    "✅ Participants acceptés", "#27ae60", "approved");
        addParticipantSection(mainCard, byStatus.get("Refuse"),     "❌ Participants refusés",  "#e74c3c", "rejected");

        return mainCard;
    }

    private void addParticipantSection(VBox parent, List<ParticipationFormation> participants,
                                       String sectionTitle, String color, String status) {
        if (participants == null || participants.isEmpty()) return;
        VBox section = new VBox(10);
        Label title = new Label(sectionTitle + " (" + participants.size() + ")");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 13px;");
        section.getChildren().add(title);
        // Session is not needed in this context — pass null as sessions require lookup
        for (ParticipationFormation p : participants) {
            section.getChildren().add(createParticipantCard(p, null, status));
        }
        parent.getChildren().add(section);
    }

    private VBox createParticipantCard(ParticipationFormation participation, SessionFormation session, String status) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #E0E0E0; -fx-border-width: 1;");

        Label employeeLabel = new Label("👤 " + participation.getNomEmployee());
        employeeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #022E69; -fx-font-size: 12px;");

        Label dateLabel = new Label("📅 Inscription : " + participation.getDateInscription());
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        // Afficher le motif de refus si disponible — nouveau
        if ("rejected".equals(status) && participation.getMotifRefus() != null && !participation.getMotifRefus().isBlank()) {
            Label motif = new Label("💬 Motif : " + participation.getMotifRefus());
            motif.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-style: italic;");
            card.getChildren().addAll(employeeLabel, dateLabel, motif);
        } else {
            card.getChildren().addAll(employeeLabel, dateLabel);
        }

        // Badge priorité — nouveau
        if (participation.isPriority()) {
            Label lblPriority = new Label("⭐ Prioritaire");
            lblPriority.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 11px;");
            card.getChildren().add(lblPriority);
        }

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        if ("pending".equals(status)) {
            Button btnValidate = styledButton("✅ Accepter", "#27ae60");
            Button btnReject   = styledButton("❌ Refuser",  "#e74c3c");
            btnValidate.setOnAction(e -> handleValidateParticipantCard(participation));
            btnReject.setOnAction(e -> handleRejectParticipantCard(participation));
            buttonBox.getChildren().addAll(btnValidate, btnReject);
        } else if ("approved".equals(status)) {
            Button btnDetails = styledButton("📋 Détails",  "#145EB7");
            Button btnReset   = styledButton("🔄 Modifier", "#3FA9F5");
            btnDetails.setOnAction(e -> showParticipantDetails(participation));
            btnReset.setOnAction(e -> {
                participation.setStatutParticipation("Pending");
                participationService.updateParticipation(participation);
                displayParticipantsForFormation();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut remis en attente !");
            });
            buttonBox.getChildren().addAll(btnDetails, btnReset);
        }

        card.getChildren().add(buttonBox);
        return card;
    }

    private void showParticipantDetails(ParticipationFormation participation) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du participant");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(25));

        content.getChildren().addAll(
                styledDetailLabel("👤 Employé",      participation.getNomEmployee()),
                styledDetailLabel("📅 Inscription",  participation.getDateInscription().toString()),
                styledDetailLabel("⚙️ Statut",       participation.getStatutParticipation()),
                styledDetailLabel("⭐ Prioritaire",  participation.isPriority() ? "Oui" : "Non")
        );

        if (participation.getResultat() != null && !participation.getResultat().isEmpty())
            content.getChildren().add(styledDetailLabel("🎯 Résultat", participation.getResultat()));
        if (participation.getMotifRefus() != null && !participation.getMotifRefus().isBlank())
            content.getChildren().add(styledDetailLabel("💬 Motif refus", participation.getMotifRefus()));

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // ========================================================
    //  DIALOGUES FORMATION & SESSION
    // ========================================================

    private Dialog<Formation> createFormationDialog(Formation formation) {
        Dialog<Formation> dialog = new Dialog<>();
        dialog.setTitle(formation == null ? "Nouvelle Formation" : "Modifier Formation");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ffffff;");
        dialogPane.setHeader(buildDialogHeader(
                formation == null ? "🆕 Créer une nouvelle formation" : "✏️ Modifier la formation",
                "Remplissez les informations de la formation ci-dessous",
                "linear-gradient(to right, #022E69, #145EB7)"
        ));

        ButtonType saveBtn = new ButtonType("✅ ENREGISTRER", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        String inputStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #D0D0D0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10; -fx-font-size: 12px;";

        GridPane grid = buildDialogGrid();

        TextField tfTitre     = styledTextField(formation != null ? formation.getTitre() : "", "Saisissez le titre...", inputStyle);
        TextField tfOrganisme = styledTextField(formation != null ? formation.getOrganisme() : "", "Nom de l'organisme...", inputStyle);
        TextField tfDuree     = styledTextField(formation != null ? String.valueOf(formation.getDuree()) : "", "Nombre de jours (ex: 5)", inputStyle);

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "Technique", "Soft Skills", "Management", "Langues", "Autre"));
        cbType.setStyle(inputStyle);
        cbType.setPrefHeight(35);
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setValue(formation != null ? formation.getType() : "Technique");

        TextArea taDescription = styledTextArea(formation != null ? formation.getDescription() : "", "Décrivez le contenu...", inputStyle, 2);
        TextArea taObjectifs   = styledTextArea(formation != null ? formation.getObjectifs()   : "", "Énumérez les objectifs...", inputStyle, 3);

        Button btnGenIA = new Button("✨ Générer avec l'IA");
        btnGenIA.setStyle("-fx-background-color: #3FA9F5; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 16; -fx-font-size: 12px;");
        btnGenIA.setOnAction(e -> generateObjectivesAsync(tfTitre, taObjectifs));

        VBox objectifsBox = new VBox(10, taObjectifs, btnGenIA);
        objectifsBox.setAlignment(Pos.TOP_RIGHT);

        grid.add(createStyledLabel("📋 Titre :"),         0, 0); grid.add(tfTitre, 1, 0);
        grid.add(createStyledLabel("🏢 Organisme :"),     0, 1); grid.add(tfOrganisme, 1, 1);
        grid.add(createStyledLabel("🎯 Type :"),          0, 2); grid.add(cbType, 1, 2);
        grid.add(createStyledLabel("⏱️ Durée (jours) :"), 0, 3); grid.add(tfDuree, 1, 3);
        grid.add(createStyledLabel("📝 Description :"),   0, 4); grid.add(taDescription, 1, 4);
        grid.add(createStyledLabel("🎓 Objectifs :"),     0, 5); grid.add(objectifsBox, 1, 5);

        dialogPane.setContent(grid);
        styleDialogButtons(dialogPane, saveBtn, "#27ae60");

        dialogPane.lookupButton(saveBtn).disableProperty().bind(
                tfTitre.textProperty().isEmpty()
                        .or(tfOrganisme.textProperty().isEmpty())
                        .or(tfDuree.textProperty().isEmpty())
        );

        dialog.setResultConverter(db -> {
            if (db != saveBtn) return null;
            try {
                int duree = Integer.parseInt(tfDuree.getText());
                if (formation == null) {
                    return new Formation(tfTitre.getText(), taDescription.getText(),
                            cbType.getValue(), duree, tfOrganisme.getText(),
                            taObjectifs.getText(), this.rhId, null);
                } else {
                    formation.setTitre(tfTitre.getText());
                    formation.setDescription(taDescription.getText());
                    formation.setType(cbType.getValue());
                    formation.setDuree(duree);
                    formation.setOrganisme(tfOrganisme.getText());
                    formation.setObjectifs(taObjectifs.getText());
                    return formation;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La durée doit être un nombre entier !");
                return null;
            }
        });

        return dialog;
    }

    private Dialog<SessionFormation> createSessionDialog(SessionFormation session) {
        Dialog<SessionFormation> dialog = new Dialog<>();
        dialog.setTitle(session == null ? "Nouvelle Session" : "Modifier une Session");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ffffff;");
        dialogPane.setHeader(buildDialogHeader(
                session == null ? "📅 Créer une nouvelle session" : "✏️ Modifier la session",
                "Configurez les détails de la session de formation",
                "linear-gradient(to right, #145EB7, #3FA9F5)"
        ));

        ButtonType saveBtn = new ButtonType("✅ ENREGISTRER", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        String fieldStyle = "-fx-padding: 10; -fx-border-color: #D0D0D0; -fx-border-radius: 6; -fx-background-color: #f8f9fa; -fx-font-size: 12px;";

        GridPane grid = buildDialogGrid();

        final Formation[] selectedFormationRef = {null};

        if (session == null) {
            ComboBox<Formation> cbFormation = new ComboBox<>(formationsList);
            cbFormation.setConverter(formationStringConverter("Sélectionnez une formation"));
            cbFormation.setStyle(fieldStyle);
            cbFormation.setPrefHeight(35);
            cbFormation.setMaxWidth(Double.MAX_VALUE);
            cbFormation.valueProperty().addListener((obs, o, n) -> selectedFormationRef[0] = n);
            grid.add(createStyledLabel("🎓 Formation :"), 0, 0);
            grid.add(cbFormation, 1, 0);
        } else {
            selectedFormationRef[0] = formationsList.stream()
                    .filter(f -> f.getIdFormation() == session.getIdFormation())
                    .findFirst().orElse(null);
            Label lbl = new Label(selectedFormationRef[0] != null ? selectedFormationRef[0].getTitre() : "Inconnue");
            lbl.setStyle(fieldStyle + "; -fx-text-fill: #022E69; -fx-font-weight: bold;");
            grid.add(createStyledLabel("🎓 Formation :"), 0, 0);
            grid.add(lbl, 1, 0);
        }

        DatePicker dpDebut = new DatePicker(session != null ? session.getDateDebut() : LocalDate.now());
        DatePicker dpFin   = new DatePicker(session != null ? session.getDateFin()   : LocalDate.now().plusDays(7));
        dpDebut.setStyle(fieldStyle); dpDebut.setPrefHeight(35);
        dpFin.setStyle(fieldStyle + "; -fx-opacity: 0.7;"); dpFin.setPrefHeight(35);
        dpFin.setDisable(true);

        // Calcul automatique de la date de fin — comme dans Symfony
        dpDebut.valueProperty().addListener((obs, o, n) -> {
            if (n != null && selectedFormationRef[0] != null)
                dpFin.setValue(calculateEndDate(n, selectedFormationRef[0].getDuree()));
        });

        TextField tfLieu     = styledTextField(session != null ? session.getLieu() : "", "Lieu de la session...", fieldStyle);
        ComboBox<String> cbMode = new ComboBox<>(FXCollections.observableArrayList("Présentiel", "Distanciel", "Hybride"));
        cbMode.setStyle(fieldStyle); cbMode.setPrefHeight(35);
        cbMode.setValue(session != null ? session.getMode() : "Présentiel");

        // Validation URL pour les sessions en ligne — comme validateSessionLocationField dans Symfony
        Label lblLieuError = new Label();
        lblLieuError.setTextFill(Color.RED);
        lblLieuError.setFont(new Font(10));

        cbMode.valueProperty().addListener((obs, o, n) -> {
            boolean online = "Distanciel".equals(n);
            tfLieu.setPromptText(online ? "Lien Teams / Zoom / Meet..." : "Lieu de la session...");
            if (online && tfLieu.getText().isBlank()) {
                lblLieuError.setText("⚠️ Un lien de réunion est obligatoire pour ce mode");
            } else {
                lblLieuError.setText("");
            }
        });

        TextField tfCapacite = styledTextField(session != null ? String.valueOf(session.getCapaciteMax()) : "20", "Nombre de places", fieldStyle);

        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("Planifiée", "EnCours", "Terminée", "Annulée"));
        cbStatut.setStyle(fieldStyle); cbStatut.setPrefHeight(35);
        cbStatut.setValue(session != null ? session.getStatut() : "Planifiée");

        VBox endDateBox = new VBox(3,
                createStyledLabel("📅 Date de fin :"),
                new Label("(Calculée automatiquement)") {{
                    setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-style: italic;");
                }}
        );

        grid.add(createStyledLabel("📅 Date de début :"), 0, 1); grid.add(dpDebut, 1, 1);
        grid.add(endDateBox,                               0, 2); grid.add(dpFin, 1, 2);
        grid.add(createStyledLabel("📍 Lieu :"),           0, 3); grid.add(new VBox(3, tfLieu, lblLieuError), 1, 3);
        grid.add(createStyledLabel("💻 Mode :"),           0, 4); grid.add(cbMode, 1, 4);
        grid.add(createStyledLabel("👥 Capacité max :"),   0, 5); grid.add(tfCapacite, 1, 5);
        grid.add(createStyledLabel("⚙️ Statut :"),         0, 6); grid.add(cbStatut, 1, 6);

        dialogPane.setContent(grid);
        styleDialogButtons(dialogPane, saveBtn, "#27ae60");

        dialog.setResultConverter(db -> {
            if (db != saveBtn) return null;
            if (selectedFormationRef[0] == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez sélectionner une formation !");
                return null;
            }

            // Validation mode en ligne — comme validateSessionLocationField dans Symfony
            String mode = cbMode.getValue();
            String lieu = tfLieu.getText().trim();
            if ("Distanciel".equals(mode) && !lieu.isBlank() && !isValidHttpUrl(lieu)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Pour une session en ligne, veuillez renseigner un lien valide (Teams, Zoom, Meet...).");
                return null;
            }

            try {
                int capacite = Integer.parseInt(tfCapacite.getText());
                if (dpDebut.getValue().isAfter(dpFin.getValue())) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "La date de début ne peut pas être après la date de fin !");
                    return null;
                }
                if (session == null) {
                    return new SessionFormation(dpDebut.getValue(), dpFin.getValue(),
                            lieu.isBlank() ? null : lieu, mode, capacite,
                            cbStatut.getValue(), selectedFormationRef[0].getIdFormation());
                } else {
                    session.setIdFormation(selectedFormationRef[0].getIdFormation());
                    session.setDateDebut(dpDebut.getValue());
                    session.setDateFin(dpFin.getValue());
                    session.setLieu(lieu.isBlank() ? null : lieu);
                    session.setMode(mode);
                    session.setCapaciteMax(capacite);
                    session.setStatut(cbStatut.getValue());
                    return session;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La capacité doit être un nombre entier !");
                return null;
            }
        });

        return dialog;
    }

    // ========================================================
    //  FILTRES
    // ========================================================

    private void filterFormations(String searchText) {
        formationsContainer.getChildren().clear();
        List<Integer> ids = formationsList.stream().map(Formation::getIdFormation).toList();
        Map<Integer, Double> ratingMap = feedbackService.getAverageRatingsByFormationIds(ids);

        for (Formation f : formationsList) {
            if (searchText == null || searchText.isEmpty() ||
                    f.getTitre().toLowerCase().contains(searchText.toLowerCase()) ||
                    f.getType().toLowerCase().contains(searchText.toLowerCase()) ||
                    f.getOrganisme().toLowerCase().contains(searchText.toLowerCase())) {
                formationsContainer.getChildren().add(
                        createFormationCard(f, ratingMap.getOrDefault(f.getIdFormation(), 0.0)));
            }
        }
    }

    // ========================================================
    //  STATS SESSIONS
    // ========================================================

    private void updateSessionStats() {
        if (lblTotalSessions == null) return;
        int total = sessionsList.size(), planifiees = 0, enCours = 0, terminees = 0;
        LocalDate today = LocalDate.now();
        for (SessionFormation s : sessionsList) {
            if (today.isBefore(s.getDateDebut()))      planifiees++;
            else if (today.isAfter(s.getDateFin()))    terminees++;
            else                                        enCours++;
        }
        lblTotalSessions.setText(String.valueOf(total));
        if (lblSessionsPlanifiees != null) lblSessionsPlanifiees.setText(String.valueOf(planifiees));
        if (lblSessionsEnCours    != null) lblSessionsEnCours.setText(String.valueOf(enCours));
        if (lblSessionsTerminees  != null) lblSessionsTerminees.setText(String.valueOf(terminees));
    }

    // ========================================================
    //  UTILITAIRES MÉTIER
    // ========================================================

    /**
     * Génère automatiquement un lien Jitsi pour les sessions en ligne sans lieu — comme Symfony.
     */
    private void applyOnlineMeetingLinkIfNeeded(SessionFormation session) {
        if (!"Distanciel".equals(session.getMode())) return;
        String lieu = session.getLieu();
        if (lieu != null && !lieu.isBlank()) return;
        int formationId = session.getIdFormation();
        session.setLieu(jitsiMeetService.generateFormationSessionLink(formationId, session.getIdSession()));
    }

    private boolean isValidHttpUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private LocalDate calculateEndDate(LocalDate startDate, int workingDays) {
        if (startDate == null || workingDays <= 0) return startDate;
        LocalDate current = startDate;
        int added = 0;
        while (added < workingDays) {
            current = current.plusDays(1);
            int dow = current.getDayOfWeek().getValue();
            if (dow != 6 && dow != 7) added++;
        }
        return current;
    }

    private String getStarRating(double moyenne) {
        int stars = (int) Math.round(moyenne);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < stars ? "★" : "☆");
        return sb.toString();
    }

    private void generateObjectivesAsync(TextField titleField, TextArea objectifsArea) {
        String titre = titleField.getText().trim();
        if (titre.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer un titre avant de générer les objectifs.");
            return;
        }
        objectifsArea.setText("⏳ Génération en cours...");
        new Thread(() -> {
            try {
                String generated = OpenAIService.generateObjectives(titre);
                javafx.application.Platform.runLater(() -> objectifsArea.setText(generated));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    objectifsArea.setText("");
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer les objectifs.");
                });
            }
        }).start();
    }

    // ========================================================
    //  UTILITAIRES UI
    // ========================================================

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px;");
        return btn;
    }

    private TextField styledTextField(String value, String prompt, String style) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(style);
        tf.setPrefHeight(35);
        return tf;
    }

    private TextArea styledTextArea(String value, String prompt, String style, int rows) {
        TextArea ta = new TextArea(value);
        ta.setPromptText(prompt);
        ta.setStyle(style);
        ta.setPrefRowCount(rows);
        ta.setWrapText(true);
        return ta;
    }

    private GridPane buildDialogGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);
        grid.setPadding(new Insets(30, 35, 25, 35));
        grid.setStyle("-fx-background-color: #ffffff;");
        return grid;
    }

    private VBox buildDialogHeader(String title, String subtitle, String bgStyle) {
        VBox header = new VBox(8);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: " + bgStyle + ";");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-text-fill: #D0D0D0; -fx-font-size: 13px;");
        header.getChildren().addAll(lblTitle, lblSub);
        return header;
    }

    private void styleDialogButtons(DialogPane pane, ButtonType primaryBtn, String primaryColor) {
        Button save   = (Button) pane.lookupButton(primaryBtn);
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (save   != null) save.setStyle("-fx-background-color: " + primaryColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px;");
        if (cancel != null) cancel.setStyle("-fx-background-color: #D0D0D0; -fx-text-fill: #34495e; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private Alert buildStyledConfirmation(String headerTitle, String headerSub, String color, String content) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText(content);
        DialogPane dp = confirm.getDialogPane();
        dp.setStyle("-fx-background-color: #ffffff;");

        VBox header = new VBox(8);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: " + color + ";");
        Label lbl = new Label(headerTitle);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        header.getChildren().add(lbl);
        if (headerSub != null) {
            Label sub = new Label(headerSub);
            sub.setStyle("-fx-text-fill: #D0D0D0; -fx-font-size: 12px;");
            header.getChildren().add(sub);
        }
        dp.setHeader(header);

        for (ButtonType bt : dp.getButtonTypes()) {
            Button btn = (Button) dp.lookupButton(bt);
            btn.setStyle(bt == ButtonType.OK
                    ? "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 5;"
                    : "-fx-background-color: #D0D0D0; -fx-text-fill: #34495e; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 5;");
        }
        return confirm;
    }

    private Label createStyledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #022E69; -fx-font-weight: bold; -fx-font-size: 13px;");
        return lbl;
    }

    private Label styledDetailLabel(String key, String value) {
        Label lbl = new Label(key + " : " + value);
        lbl.setStyle("-fx-text-fill: #34495e; -fx-font-size: 13px;");
        return lbl;
    }

    private javafx.util.StringConverter<Formation> formationStringConverter(String nullLabel) {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(Formation f) { return f == null ? nullLabel : f.getTitre(); }
            @Override public Formation fromString(String s) { return null; }
        };
    }

    private void displayNoParticipantsMessage() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50, 30, 50, 30));
        box.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #D0D0D0; -fx-border-width: 1;");
        box.getChildren().addAll(
                new Label("📭") {{ setStyle("-fx-font-size: 48px;"); }},
                new Label("Aucun participant") {{ setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #022E69;"); }},
                new Label("Cette formation n'a pas encore de participants inscrits.") {{ setWrapText(true); setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;"); }}
        );
        vboxParticipants.getChildren().add(box);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: #f5f5f5;");

        String color = switch (type) {
            case INFORMATION -> "#3FA9F5";
            case WARNING     -> "#f39c12";
            case ERROR       -> "#e74c3c";
            default          -> "#27ae60";
        };
        String icon = switch (type) {
            case INFORMATION -> "ℹ️";
            case WARNING     -> "⚠️";
            case ERROR       -> "❌";
            default          -> "❓";
        };

        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + color + ";");
        Label lbl = new Label(icon + " " + title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        header.getChildren().add(lbl);
        dp.setHeader(header);

        Label content = new Label(message);
        content.setStyle("-fx-text-fill: #34495e; -fx-font-size: 13px;");
        content.setWrapText(true);
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20, 25, 25, 25));
        contentBox.getChildren().add(content);
        dp.setContent(contentBox);

        for (ButtonType bt : dp.getButtonTypes()) {
            Button btn = (Button) dp.lookupButton(bt);
            btn.setStyle("-fx-background-color: #145EB7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        }

        alert.showAndWait();
    }
}
