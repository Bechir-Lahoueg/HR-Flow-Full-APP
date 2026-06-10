package org.example.ui.controller.Rh;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.controller.EmployeeController;
import org.example.model.Employee;
import org.example.model.User;
import org.example.ui.service.WelcomeApiService;
import org.example.ui.service.WelcomeApiService.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour la vue d'accueil RH
 */
public class RHHomeViewController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalEmployeesLabel;
    @FXML private Label totalEmployeesLabel2;
    @FXML private Label pendingLeavesLabel;
    @FXML private Label formationsCountLabel;
    @FXML private Label recrutementCountLabel;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;

    // Widgets API externes
    @FXML private Label apiStatusLabel;
    @FXML private Label weatherIconLabel;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherCityLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherHumidityLabel;
    @FXML private Label weatherWindLabel;
    @FXML private Label weatherFeelsLabel;
    @FXML private HBox  forecastBox;
    @FXML private VBox  ratesBox;
    @FXML private VBox  newsBox;
    @FXML private VBox  holidaysBox;
    @FXML private Label quoteLabel;
    @FXML private Label quoteAuthorLabel;

    private final EmployeeController employeeController = new EmployeeController();
    private final WelcomeApiService  apiService         = new WelcomeApiService();
    private final ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private User currentUser;
    private RHDashboardController dashboardController;
    private Timeline clock;

    public void setDashboardController(RHDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    private void handleNavigateToEmployees() {
        if (dashboardController != null) dashboardController.showEmployees();
    }

    @FXML
    private void handleNavigateToLeave() {
        if (dashboardController != null) dashboardController.showLeave();
    }

    @FXML
    private void handleNavigateToRecruitment() {
        if (dashboardController != null) dashboardController.showRecruitment();
    }

    @FXML
    private void initialize() {
        startClock();
        loadDashboardData();
        loadExternalData();
    }

    private void startClock() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", java.util.Locale.FRENCH);
        updateClockLabels(timeFmt, dateFmt);
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClockLabels(timeFmt, dateFmt)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updateClockLabels(DateTimeFormatter timeFmt, DateTimeFormatter dateFmt) {
        if (timeLabel != null) timeLabel.setText(LocalTime.now().format(timeFmt));
        if (dateLabel != null) {
            String raw = LocalDate.now().format(dateFmt);
            dateLabel.setText(raw.substring(0, 1).toUpperCase() + raw.substring(1));
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText(user.getUsername());
        }
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (currentUser != null) {
            loadEmployeeListAsync();
        }
    }

    private void loadEmployeeListAsync() {
        javafx.concurrent.Task<List<Employee>> task = new javafx.concurrent.Task<List<Employee>>() {
            @Override
            protected List<Employee> call() {
                return employeeController.handleListMyEmployees(currentUser);
            }
        };
        task.setOnSucceeded(e -> {
            List<Employee> employees = task.getValue();
            employeeList.clear();
            if (employees != null) employeeList.addAll(employees);
            loadStats();
        });
        Thread t = new Thread(task, "home-stats-loader");
        t.setDaemon(true);
        t.start();
    }

    private void loadStats() {
        if (totalEmployeesLabel != null) totalEmployeesLabel.setText(String.valueOf(employeeList.size()));
        if (totalEmployeesLabel2 != null) totalEmployeesLabel2.setText(String.valueOf(employeeList.size()));
        if (pendingLeavesLabel   != null) pendingLeavesLabel.setText("—");
        if (formationsCountLabel != null) formationsCountLabel.setText("—");
        if (recrutementCountLabel != null) recrutementCountLabel.setText("—");
    }

    // ── Chargement des données API externes ───────────────────────────────────

    private void loadExternalData() {
        Task<DashboardData> task = apiService.loadDashboardDataAsync("ressources humaines", "leadership");
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

        // ── Météo ────────────────────────────────────────────────────────────
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

        // ── Prévisions ──────────────────────────────────────────────────────
        if (forecastBox != null && data.forecast() != null) {
            forecastBox.getChildren().clear();
            for (ForecastDay f : data.forecast()) {
                VBox dayBox = new VBox(3);
                dayBox.setAlignment(javafx.geometry.Pos.CENTER);
                dayBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 12; -fx-padding: 10 14;");
                dayBox.setPrefWidth(90);
                Label dayLbl  = new Label(f.day());
                dayLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
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

        // ── Taux de change ──────────────────────────────────────────────────
        if (ratesBox != null && data.rates() != null) {
            ratesBox.getChildren().clear();
            for (ExchangeRate r : data.rates()) {
                HBox row = new HBox(8);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10; -fx-padding: 10 14;");
                Label symLbl  = new Label(r.symbol());
                symLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #145EB7;");
                symLbl.setMinWidth(30);
                Label codeLbl = new Label(r.code());
                codeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                Label rateLbl = new Label(r.rate() + " TND");
                rateLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
                row.getChildren().addAll(symLbl, codeLbl, spacer, rateLbl);
                ratesBox.getChildren().add(row);
            }
        }

        // ── Actualités ──────────────────────────────────────────────────────
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

        // ── Jours fériés ────────────────────────────────────────────────────
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

        // ── Citation ─────────────────────────────────────────────────────────
        QuoteData q = data.quote();
        if (q != null) {
            if (quoteLabel       != null) quoteLabel.setText("« " + q.content() + " »");
            if (quoteAuthorLabel != null) quoteAuthorLabel.setText("— " + q.author());
        }
    }
}