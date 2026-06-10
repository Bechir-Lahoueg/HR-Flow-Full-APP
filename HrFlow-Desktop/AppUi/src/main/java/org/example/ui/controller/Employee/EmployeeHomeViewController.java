package org.example.ui.controller.Employee;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.model.Employee;
import org.example.ui.service.WelcomeApiService;
import org.example.ui.service.WelcomeApiService.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Contrôleur pour la vue d'accueil Employé
 */
public class EmployeeHomeViewController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalLeaveLabel;
    @FXML private Label totalRequestsLabel;
    @FXML private Label totalFormationsLabel;
    @FXML private Label totalNotificationsLabel;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;

    // Widgets API
    @FXML private Label apiStatusLabel;
    @FXML private Label weatherIconLabel;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherCityLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherHumidityLabel;
    @FXML private Label weatherWindLabel;
    @FXML private Label weatherFeelsLabel;
    @FXML private HBox  forecastBox;
    @FXML private VBox  newsBox;
    @FXML private VBox  holidaysBox;
    @FXML private Label adviceLabel;
    @FXML private Label quoteLabel;
    @FXML private Label quoteAuthorLabel;

    private Employee currentEmployee;
    private EmployeeDashboardController dashboardController;
    private Timeline clock;
    private final WelcomeApiService apiService = new WelcomeApiService();

    public void setDashboardController(EmployeeDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        setDefaults();
        startClock();
        loadExternalData();
    }

    private void startClock() {
        updateClockLabels();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClockLabels()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateClockLabels() {
        if (timeLabel != null) {
            timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (dateLabel != null) {
            String raw = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
            dateLabel.setText(raw.substring(0, 1).toUpperCase() + raw.substring(1));
        }
    }

    @FXML
    private void handleNavigateToLeave() {
        if (dashboardController != null) dashboardController.showLeave();
    }

    @FXML
    private void handleNavigateToFormations() {
        if (dashboardController != null) dashboardController.showFormations();
    }

    @FXML
    private void handleNavigateToRequests() {
        if (dashboardController != null) dashboardController.showRequests();
    }

    public void setCurrentEmployee(Employee employee) {
        this.currentEmployee = employee;
        if (welcomeLabel != null && employee != null) {
            String name = (employee.getFirstName() != null ? employee.getFirstName() : "")
                        + " " + (employee.getLastName() != null ? employee.getLastName() : "");
            welcomeLabel.setText(name.trim().isEmpty() ? "Employé" : name.trim());
        }
        loadStats();
    }

    private void setDefaults() {
        if (totalLeaveLabel != null) totalLeaveLabel.setText("—");
        if (totalRequestsLabel != null) totalRequestsLabel.setText("—");
        if (totalFormationsLabel != null) totalFormationsLabel.setText("—");
        if (totalNotificationsLabel != null) totalNotificationsLabel.setText("—");
    }

    /**
     * Charge les statistiques de l'employé
     * À connecter aux services métier selon les besoins
     */
    private void loadStats() {
        // Placeholder - à implémenter avec les services
        setDefaults();
    }

    // ── APIs externes ─────────────────────────────────────────────────────────

    private void loadExternalData() {
        Task<DashboardData> task = apiService.loadDashboardDataAsync("bien-être travail", "motivational");
        task.setOnSucceeded(e -> Platform.runLater(() -> applyApiData(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (apiStatusLabel != null) apiStatusLabel.setText("⚠ APIs indisponibles");
        }));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void applyApiData(DashboardData data) {
        if (apiStatusLabel != null) apiStatusLabel.setText("✓ Mis à jour");

        // Météo
        WeatherData w = data.weather();
        if (w != null) {
            if (weatherIconLabel     != null) weatherIconLabel.setText(w.icon());
            if (weatherTempLabel     != null) weatherTempLabel.setText(String.format("%.0f°C", w.temp()));
            if (weatherCityLabel     != null) weatherCityLabel.setText(w.city());
            if (weatherDescLabel     != null) weatherDescLabel.setText(w.description());
            if (weatherHumidityLabel != null) weatherHumidityLabel.setText(w.humidity() + "%");
            if (weatherWindLabel     != null) weatherWindLabel.setText(String.format("%.0f km/h", w.windSpeed() * 3.6));
            if (weatherFeelsLabel    != null) weatherFeelsLabel.setText(String.format("%.0f°C", w.feelsLike()));
        }

        // Prévisions
        if (forecastBox != null && data.forecast() != null) {
            forecastBox.getChildren().clear();
            for (ForecastDay f : data.forecast()) {
                VBox dayBox = new VBox(3);
                dayBox.setAlignment(javafx.geometry.Pos.CENTER);
                dayBox.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 12; -fx-padding: 10 14;");
                dayBox.setPrefWidth(90);
                Label dayLbl  = new Label(f.day());
                dayLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 11px;");
                Label iconLbl = new Label(f.icon());
                iconLbl.setStyle("-fx-font-size: 20px;");
                Label maxLbl  = new Label(String.format("%.0f°", f.tempMax()));
                maxLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                Label minLbl  = new Label(String.format("%.0f°", f.tempMin()));
                minLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");
                dayBox.getChildren().addAll(dayLbl, iconLbl, maxLbl, minLbl);
                forecastBox.getChildren().add(dayBox);
            }
        }

        // Actualités
        if (newsBox != null && data.news() != null) {
            newsBox.getChildren().clear();
            for (NewsArticle a : data.news()) {
                VBox item = new VBox(4);
                item.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 12 0;");
                Label titleLbl = new Label(a.title());
                titleLbl.setWrapText(true);
                titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
                Label srcLbl = new Label("📰 " + a.source());
                srcLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                item.getChildren().addAll(titleLbl, srcLbl);
                newsBox.getChildren().add(item);
            }
        }

        // Jours fériés
        if (holidaysBox != null && data.holidays() != null) {
            holidaysBox.getChildren().clear();
            for (HolidayEntry h : data.holidays()) {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 10; -fx-padding: 10 14;");
                VBox info = new VBox(2);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
                Label nameLbl = new Label(h.name());
                nameLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label dateLbl = new Label(h.formatted());
                dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.65);");
                info.getChildren().addAll(nameLbl, dateLbl);
                Label daysLbl = new Label(h.daysUntil() == 0 ? "Aujourd'hui" : "J-" + h.daysUntil());
                daysLbl.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 8; "
                        + "-fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
                row.getChildren().addAll(info, daysLbl);
                holidaysBox.getChildren().add(row);
            }
        }

        // Conseil
        if (adviceLabel != null && data.advice() != null) adviceLabel.setText(data.advice());

        // Citation
        QuoteData q = data.quote();
        if (q != null) {
            if (quoteLabel       != null) quoteLabel.setText("« " + q.content() + " »");
            if (quoteAuthorLabel != null) quoteAuthorLabel.setText("— " + q.author());
        }
    }
}