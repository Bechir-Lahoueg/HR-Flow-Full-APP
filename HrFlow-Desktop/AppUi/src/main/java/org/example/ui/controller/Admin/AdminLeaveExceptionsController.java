package org.example.ui.controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.example.model.LeaveRequest;
import org.example.model.User;
import org.example.service.LeaveAiService;
import org.example.service.LeaveRequestService;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur Admin pour la validation des congés exceptionnels.
 * Reçoit les demandes pré-approuvées par RH (workflow_status = ADMIN_PENDING).
 */
public class AdminLeaveExceptionsController {

    @FXML private TableView<LeaveRequest>              exceptionsTable;
    @FXML private TableColumn<LeaveRequest, Integer>   idColumn;
    @FXML private TableColumn<LeaveRequest, String>    employeeColumn;
    @FXML private TableColumn<LeaveRequest, String>    leaveTypeColumn;
    @FXML private TableColumn<LeaveRequest, LocalDate> startDateColumn;
    @FXML private TableColumn<LeaveRequest, LocalDate> endDateColumn;
    @FXML private TableColumn<LeaveRequest, Integer>   daysColumn;
    @FXML private TableColumn<LeaveRequest, String>    urgencyColumn;
    @FXML private TableColumn<LeaveRequest, String>    workflowColumn;

    @FXML private Button  approveButton;
    @FXML private Button  rejectButton;
    @FXML private TextArea auditLogArea;
    @FXML private Label    statusLabel;

    private final LeaveRequestService leaveRequestService = new LeaveRequestService();
    private final LeaveAiService      leaveAiService      = new LeaveAiService();
    private final ObservableList<LeaveRequest> items      = FXCollections.observableArrayList();
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void initialize() {
        setupTable();
        setupButtons();
        loadData();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        employeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        leaveTypeColumn.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        daysColumn.setCellValueFactory(new PropertyValueFactory<>("daysCount"));
        urgencyColumn.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getUrgencyLabel()));
        workflowColumn.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getWorkflowStatusLabel()));

        urgencyColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.contains("CRITIQUE")
                    ? "-fx-text-fill: #c53030; -fx-font-weight: bold;"
                    : item.contains("HAUTE") ? "-fx-text-fill: #c05621; -fx-font-weight: bold;"
                    : "-fx-text-fill: #276749;");
            }
        });

        exceptionsTable.setItems(items);
        exceptionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = n != null;
            approveButton.setDisable(!sel || !LeaveRequest.WORKFLOW_ADMIN_PENDING.equals(n.getWorkflowStatus()));
            rejectButton.setDisable(!sel || !LeaveRequest.WORKFLOW_ADMIN_PENDING.equals(n.getWorkflowStatus()));
            if (sel && n.getAuditLog() != null && auditLogArea != null)
                auditLogArea.setText(n.getAuditLog());
            else if (auditLogArea != null)
                auditLogArea.clear();
        });
    }

    private void setupButtons() {
        approveButton.setDisable(true);
        rejectButton.setDisable(true);
        approveButton.setOnAction(e -> approveSelected());
        rejectButton.setOnAction(e -> rejectSelected());
    }

    private void loadData() {
        Task<List<LeaveRequest>> task = new Task<>() {
            @Override protected List<LeaveRequest> call() { return leaveRequestService.getAdminExceptionRequests(); }
        };
        task.setOnSucceeded(e -> {
            items.setAll(task.getValue());
            if (statusLabel != null) statusLabel.setText(items.size() + " demande(s) exceptionnelle(s)");
        });
        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les demandes."));
        new Thread(task, "admin-load-exceptions").start();
    }

    @FXML
    private void refresh() { loadData(); }

    private void approveSelected() {
        LeaveRequest sel = exceptionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String aiSuggestion = leaveAiService.generateAdminLeaveDecisionComment(
            "approuver", sel.getEmployeeName(), sel.getLeaveType(),
            sel.getStartDate(), sel.getEndDate(), sel.getDaysCount(),
            sel.getUrgencyLevel(), sel.getReason(), sel.getRhComment());

        String comment = showCommentDialog(
            "Approuver le congé exceptionnel",
            "Employé : " + sel.getEmployeeName() + " | " + sel.getDaysCount() + " jour(s)",
            aiSuggestion);
        if (comment == null) return;

        String adminActor = currentUser != null ? currentUser.getUsername() : "Admin";
        boolean ok = leaveRequestService.approveExceptionByAdmin(sel.getId(), comment, adminActor);
        if (ok) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le congé exceptionnel a été approuvé.");
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'approuver la demande.");
        }
    }

    private void rejectSelected() {
        LeaveRequest sel = exceptionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String aiSuggestion = leaveAiService.generateAdminLeaveDecisionComment(
            "refuser", sel.getEmployeeName(), sel.getLeaveType(),
            sel.getStartDate(), sel.getEndDate(), sel.getDaysCount(),
            sel.getUrgencyLevel(), sel.getReason(), sel.getRhComment());

        String comment = showCommentDialog(
            "Refuser le congé exceptionnel",
            "Employé : " + sel.getEmployeeName() + " | " + sel.getDaysCount() + " jour(s)",
            aiSuggestion);
        if (comment == null) return;

        String adminActor = currentUser != null ? currentUser.getUsername() : "Admin";
        boolean ok = leaveRequestService.rejectExceptionByAdmin(sel.getId(), comment, adminActor);
        if (ok) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le congé exceptionnel a été refusé.");
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de refuser la demande.");
        }
    }

    /** Shows a comment dialog with an AI suggestion pre-filled. Returns null if cancelled. */
    private String showCommentDialog(String title, String header, String aiSuggestion) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label aiLabel = new Label("💡 Suggestion IA :");
        aiLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a67d8;");
        TextArea aiArea = new TextArea(aiSuggestion);
        aiArea.setWrapText(true);
        aiArea.setPrefRowCount(3);
        aiArea.setEditable(false);
        aiArea.setStyle("-fx-background-color: #ebf4ff; -fx-border-color: #90cdf4; -fx-border-radius: 5;");

        Label commentLabel = new Label("Votre commentaire :");
        commentLabel.setStyle("-fx-font-weight: bold;");
        TextArea commentArea = new TextArea(aiSuggestion);
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setPromptText("Saisissez votre décision...");

        Button useAiBtn = new Button("📋 Utiliser suggestion IA");
        useAiBtn.setOnAction(e -> commentArea.setText(aiArea.getText()));

        content.getChildren().addAll(aiLabel, aiArea, useAiBtn, commentLabel, commentArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);

        var result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return null;
        return commentArea.getText().trim();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
