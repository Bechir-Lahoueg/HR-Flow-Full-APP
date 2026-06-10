package org.example.ui.controller.Employee.Congé;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import org.example.model.Employee;
import org.example.model.LeaveBalance;
import org.example.model.LeaveRequest;
import org.example.service.LeaveBalanceService;
import org.example.service.LeaveRequestService;

import java.time.LocalDate;

/**
 * Contrôleur orchestrateur pour la gestion des congés côté Employé.
 * Délègue le formulaire à {@link EmployeeLeaveFormController}
 * et l'historique à {@link EmployeeLeaveHistoryController}.
 */
public class EmployeeLeaveController {

    // ─── Sous-contrôleurs (injectés via fx:include) ──────────────────────────────
    @FXML private VBox leaveForm;
    @FXML private EmployeeLeaveFormController leaveFormController;
    @FXML private VBox leaveHistory;
    @FXML private EmployeeLeaveHistoryController leaveHistoryController;

    // ─── Solde de congés ─────────────────────────────────────────────────────────
    @FXML private Label balanceDaysLabel;
    @FXML private Label totalAccruedLabel;
    @FXML private Label totalUsedLabel;
    @FXML private Label nextAccrualLabel;
    @FXML private ProgressBar balanceProgressBar;
    @FXML private Label balanceProgressLabel;

    // ─── Statistiques ────────────────────────────────────────────────────────────
    @FXML private Label totalDaysLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label acceptedRequestsLabel;

    // ─── Services ────────────────────────────────────────────────────────────────
    private final LeaveRequestService leaveRequestService = new LeaveRequestService();
    private final LeaveBalanceService leaveBalanceService  = new LeaveBalanceService();

    private int currentEmployeeId;
    private String currentEmployeeName;
    private Employee currentEmployee;

    @FXML
    public void initialize() {
        // L'initialisation détaillée se fait dans initData()
    }

    /**
     * Initialiser le contrôleur avec les informations de l'employé connecté.
     * Appelé par le dashboard après le chargement du FXML.
     */
    public void initData(int employeeId, String employeeName, Employee employee) {
        this.currentEmployeeId   = employeeId;
        this.currentEmployeeName = employeeName;
        this.currentEmployee     = employee;

        // Callback de rafraîchissement global (après soumission de formulaire)
        Runnable refreshAll = () -> leaveHistoryController.refresh();
        // ↑ refresh() déclenche automatiquement updateStatistics() via onChangeCallback

        // Initialiser les sous-contrôleurs
        leaveFormController.initData(employeeId, employeeName, refreshAll);
        leaveHistoryController.initData(employeeId, this::updateStatistics);

        // Démarrer le chargement des stats en parallèle du chargement de l'historique
        updateStatistics();
    }

    // ─── Statistiques & solde ────────────────────────────────────────────────────

    private void updateStatistics() {
        Task<Integer> statsTask = new Task<>() {
            @Override
            protected Integer call() {
                return leaveRequestService.getAcceptedLeaveDaysForEmployee(currentEmployeeId);
            }
        };
        statsTask.setOnSucceeded(e -> {
            int totalDays = statsTask.getValue();
            var list = leaveHistoryController.getLeaveRequestsList();
            long pending  = list.stream().filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.ATTENTE).count();
            long accepted = list.stream().filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.ACCEPTE).count();
            if (totalDaysLabel       != null) totalDaysLabel.setText(totalDays + " jour(s) de congé approuvés");
            if (pendingRequestsLabel != null) pendingRequestsLabel.setText(pending + " demande(s) en attente");
            if (acceptedRequestsLabel!= null) acceptedRequestsLabel.setText(accepted + " demande(s) acceptée(s)");
            loadBalanceDisplay();
        });
        statsTask.setOnFailed(e ->
                System.err.println("Erreur stats: " + statsTask.getException().getMessage()));
        new Thread(statsTask, "employee-stats-loader").start();
    }

    private void loadBalanceDisplay() {
        if (currentEmployeeId == 0) return;
        LocalDate hireDate = LocalDate.now().minusYears(1);
        Task<LeaveBalance> balanceTask = new Task<>() {
            @Override
            protected LeaveBalance call() {
                return leaveBalanceService.getOrCreateBalance(
                        currentEmployeeId, currentEmployeeName, hireDate);
            }
        };
        balanceTask.setOnSucceeded(e -> {
            LeaveBalance balance = balanceTask.getValue();
            if (balance != null) {
                if (balanceDaysLabel  != null) balanceDaysLabel.setText(balance.getFormattedAvailableDays() + " j");
                if (totalAccruedLabel != null) totalAccruedLabel.setText(String.format("%.1f j", balance.getTotalAccrued()));
                if (totalUsedLabel    != null) totalUsedLabel.setText(String.format("%.1f j", balance.getTotalUsed()));
                if (nextAccrualLabel  != null) {
                    LocalDate next = balance.getNextAccrualDate();
                    nextAccrualLabel.setText(next != null ? next.toString() : "--");
                }
                if (balanceProgressBar != null && balance.getTotalAccrued() > 0) {
                    double progress = balance.getTotalUsed() / balance.getTotalAccrued();
                    balanceProgressBar.setProgress(Math.min(progress, 1.0));
                    int pct = (int) Math.round(progress * 100);
                    if (balanceProgressLabel != null) balanceProgressLabel.setText(pct + "% utilisé");
                    if (progress > 0.8)
                        balanceProgressBar.setStyle("-fx-accent: #f56565; -fx-pref-height: 8px;");
                    else if (progress > 0.5)
                        balanceProgressBar.setStyle("-fx-accent: #ed8936; -fx-pref-height: 8px;");
                    else
                        balanceProgressBar.setStyle("-fx-accent: #11998e; -fx-pref-height: 8px;");
                }
            }
        });
        balanceTask.setOnFailed(e ->
                System.err.println("Erreur chargement solde: " + balanceTask.getException().getMessage()));
        new Thread(balanceTask, "employee-balance-loader").start();
    }
}
