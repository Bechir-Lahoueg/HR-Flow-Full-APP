package org.example.ui.controller.Rh;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Entity.Deduction;
import org.example.Entity.FichePaie;
import org.example.Entity.Prime;
import org.example.Enum.DeductionType;
import org.example.Enum.PrimeType;
import org.example.Service.CalculFiscalService;
import org.example.Service.ConversionDevisesService;
import org.example.Service.DeductionService;
import org.example.Service.ExportPdfService;
import org.example.Service.FichePaieService;
import org.example.Service.PrimeService;
import org.example.Utils.BD;
import org.example.controller.EmployeeController;
import org.example.model.Employee;
import org.example.model.User;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour la gestion de la rémunération côté RH
 * Permet de gérer les fiches de paie, primes et déductions de tous les employés
 */
public class RHRemunerationController {

    // ==================== FXML FIELDS - Statistics ====================
    @FXML private Label lblTotalFiches;
    @FXML private Label lblTotalPrimes;
    @FXML private Label lblTotalDeductions;
    @FXML private Label lblMasseSalariale;

    // ==================== FXML FIELDS - Tab 1: Fiches de Paie ====================
    @FXML private TabPane tabPane;
    @FXML private TextField txtSearchFiche;
    @FXML private TableView<FichePaie> tableFiches;
    @FXML private TableColumn<FichePaie, String> colFicheId;
    @FXML private TableColumn<FichePaie, String> colFicheEmploye;
    @FXML private TableColumn<FichePaie, String> colFicheMois;
    @FXML private TableColumn<FichePaie, String> colFicheAnnee;
    @FXML private TableColumn<FichePaie, String> colFicheSalaireBrut;
    @FXML private TableColumn<FichePaie, String> colFicheTotalPrimes;
    @FXML private TableColumn<FichePaie, String> colFicheTotalDeductions;
    @FXML private TableColumn<FichePaie, String> colFicheSalaireNet;
    @FXML private TableColumn<FichePaie, String> colFicheStatut;
    @FXML private Button btnAddFiche;
    @FXML private Button btnEditFiche;
    @FXML private Button btnDeleteFiche;
    @FXML private Button btnToggleStatut;
    @FXML private Button btnGenerateFiche;
    @FXML private Button btnRefreshFiches;
    @FXML private Button btnExportPdfFiche;

    // ==================== FXML FIELDS - Tab 2: Primes ====================
    @FXML private TextField txtSearchPrime;
    @FXML private TableView<Prime> tablePrimes;
    @FXML private TableColumn<Prime, String> colPrimeId;
    @FXML private TableColumn<Prime, String> colPrimeEmploye;
    @FXML private TableColumn<Prime, String> colPrimeType;
    @FXML private TableColumn<Prime, String> colPrimeMontant;
    @FXML private TableColumn<Prime, String> colPrimeDate;
    @FXML private Button btnAddPrime;
    @FXML private Button btnEditPrime;
    @FXML private Button btnDeletePrime;
    @FXML private Button btnRefreshPrimes;

    // ==================== FXML FIELDS - Tab 3: Déductions ====================
    @FXML private TextField txtSearchDeduction;
    @FXML private TableView<Deduction> tableDeductions;
    @FXML private TableColumn<Deduction, String> colDeductionId;
    @FXML private TableColumn<Deduction, String> colDeductionEmploye;
    @FXML private TableColumn<Deduction, String> colDeductionType;
    @FXML private TableColumn<Deduction, String> colDeductionMontant;
    @FXML private TableColumn<Deduction, String> colDeductionDate;
    @FXML private Button btnAddDeduction;
    @FXML private Button btnEditDeduction;
    @FXML private Button btnDeleteDeduction;
    @FXML private Button btnRefreshDeductions;

    // ==================== FXML FIELDS - Tab 4: Conversion Devises ====================
    @FXML private TextField txtMontantConversion;
    @FXML private Label lblConversionEUR;
    @FXML private Label lblConversionUSD;
    @FXML private Label lblTauxEUR;
    @FXML private Label lblTauxUSD;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblApiStatus;
    @FXML private Button btnRefreshRates;

    // ==================== SERVICES ====================
    private FichePaieService fichePaieService;
    private PrimeService primeService;
    private DeductionService deductionService;
    private ConversionDevisesService conversionService;
    private EmployeeController employeeController;
    private Connection connection;

    // ==================== DATA ====================
    private User currentUser;
    private final ObservableList<FichePaie> fichesList = FXCollections.observableArrayList();
    private final ObservableList<Prime> primesList = FXCollections.observableArrayList();
    private final ObservableList<Deduction> deductionsList = FXCollections.observableArrayList();
    private List<Employee> allEmployees;

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        // UI structure only — no DB or HTTP calls here
        setupFichesTable();
        setupPrimesTable();
        setupDeductionsTable();
        setupSearchFilters();
        setupConversionWidgetListeners();
        conversionService = new ConversionDevisesService();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadAllDataAsync();
    }

    private void loadAllDataAsync() {
        tableFiches.setPlaceholder(new ProgressIndicator());
        tablePrimes.setPlaceholder(new ProgressIndicator());
        tableDeductions.setPlaceholder(new ProgressIndicator());

        Task<DataBundle> task = new Task<DataBundle>() {
            @Override
            protected DataBundle call() throws Exception {
                Connection conn = BD.getInstance().getConnection();
                FichePaieService fps = new FichePaieService(conn);
                PrimeService ps = new PrimeService(conn);
                DeductionService ds = new DeductionService(conn);
                EmployeeController ec = new EmployeeController();
                List<Employee> employees = ec.handleListMyEmployees(currentUser);
                List<FichePaie> fiches = fps.getAllFiches();
                List<Prime> primes = ps.getAllPrimes();
                List<Deduction> deductions = ds.getAllDeductions();
                return new DataBundle(conn, fps, ps, ds, ec, employees, fiches, primes, deductions);
            }
        };

        task.setOnSucceeded(e -> {
            DataBundle data = task.getValue();
            connection = data.conn;
            fichePaieService = data.fichePaieService;
            primeService = data.primeService;
            deductionService = data.deductionService;
            employeeController = data.employeeController;
            allEmployees = data.employees;
            fichesList.setAll(data.fiches);
            primesList.setAll(data.primes);
            deductionsList.setAll(data.deductions);
            tableFiches.setPlaceholder(new Label("Aucune fiche de paie"));
            tablePrimes.setPlaceholder(new Label("Aucune prime"));
            tableDeductions.setPlaceholder(new Label("Aucune déduction"));
            updateStatistics();
            loadExchangeRatesAsync();
        });

        task.setOnFailed(e -> {
            tableFiches.setPlaceholder(new Label("Erreur de chargement"));
            tablePrimes.setPlaceholder(new Label("Erreur de chargement"));
            tableDeductions.setPlaceholder(new Label("Erreur de chargement"));
            showError("Erreur", "Impossible de charger les données: " + task.getException().getMessage());
        });

        Thread t = new Thread(task, "remuneration-loader");
        t.setDaemon(true);
        t.start();
    }

    private static class DataBundle {
        final Connection conn;
        final FichePaieService fichePaieService;
        final PrimeService primeService;
        final DeductionService deductionService;
        final EmployeeController employeeController;
        final List<Employee> employees;
        final List<FichePaie> fiches;
        final List<Prime> primes;
        final List<Deduction> deductions;

        DataBundle(Connection conn, FichePaieService fps, PrimeService ps, DeductionService ds,
                   EmployeeController ec, List<Employee> employees, List<FichePaie> fiches,
                   List<Prime> primes, List<Deduction> deductions) {
            this.conn = conn;
            this.fichePaieService = fps;
            this.primeService = ps;
            this.deductionService = ds;
            this.employeeController = ec;
            this.employees = employees;
            this.fiches = fiches;
            this.primes = primes;
            this.deductions = deductions;
        }
    }

    // ==================== TABLE SETUP ====================

    private void setupFichesTable() {
        colFicheId.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colFicheEmploye.setCellValueFactory(data -> 
            new SimpleStringProperty(getEmployeeName(data.getValue().getEmployeeId())));
        colFicheMois.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getMoisNom()));
        colFicheAnnee.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getAnnee())));
        colFicheSalaireBrut.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getSalaireBrut())));
        colFicheTotalPrimes.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getTotalPrimes())));
        colFicheTotalDeductions.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getTotalDeductions())));
        colFicheSalaireNet.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getSalaireNet())));
        if (colFicheStatut != null) {
            colFicheStatut.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().isStatutPaiement() ? "✅ Payé" : "⏳ En attente"));
        }

        tableFiches.setItems(fichesList);
    }

    private void setupPrimesTable() {
        colPrimeId.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colPrimeEmploye.setCellValueFactory(data -> 
            new SimpleStringProperty(getEmployeeName(data.getValue().getEmployeeId())));
        colPrimeType.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTypePrime() != null ? data.getValue().getTypePrime().label() : ""));
        colPrimeMontant.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getMontant())));
        colPrimeDate.setCellValueFactory(data -> 
            new SimpleStringProperty(formatDate(data.getValue().getDateAttribution())));

        tablePrimes.setItems(primesList);
    }

    private void setupDeductionsTable() {
        colDeductionId.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colDeductionEmploye.setCellValueFactory(data -> 
            new SimpleStringProperty(getEmployeeName(data.getValue().getEmployeeId())));
        colDeductionType.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTypeDeduction() != null ? data.getValue().getTypeDeduction().label() : ""));
        colDeductionMontant.setCellValueFactory(data -> 
            new SimpleStringProperty(formatMontant(data.getValue().getMontant())));
        colDeductionDate.setCellValueFactory(data -> 
            new SimpleStringProperty(formatDate(data.getValue().getDateDeduction())));

        tableDeductions.setItems(deductionsList);
    }

    private void setupSearchFilters() {
        // Recherche Fiches - par nom employé, mois, année, statut
        txtSearchFiche.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.toLowerCase().trim();
            if (q.isEmpty()) {
                tableFiches.setItems(fichesList);
            } else {
                ObservableList<FichePaie> filtered = FXCollections.observableArrayList();
                for (FichePaie f : fichesList) {
                    String emp = getEmployeeName(f.getEmployeeId()).toLowerCase();
                    String mois = f.getMoisNom().toLowerCase();
                    String annee = String.valueOf(f.getAnnee());
                    String statut = f.isStatutPaiement() ? "payé" : "en attente";
                    if (emp.contains(q) || mois.contains(q) || annee.contains(q) || statut.contains(q)) {
                        filtered.add(f);
                    }
                }
                tableFiches.setItems(filtered);
            }
        });

        // Recherche Primes - par nom employé, type
        txtSearchPrime.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.toLowerCase().trim();
            if (q.isEmpty()) {
                tablePrimes.setItems(primesList);
            } else {
                ObservableList<Prime> filtered = FXCollections.observableArrayList();
                for (Prime p : primesList) {
                    String emp = getEmployeeName(p.getEmployeeId()).toLowerCase();
                    String type = p.getTypePrime() != null ? p.getTypePrime().label().toLowerCase() : "";
                    if (emp.contains(q) || type.contains(q)) {
                        filtered.add(p);
                    }
                }
                tablePrimes.setItems(filtered);
            }
        });

        // Recherche Déductions - par nom employé, type
        txtSearchDeduction.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.toLowerCase().trim();
            if (q.isEmpty()) {
                tableDeductions.setItems(deductionsList);
            } else {
                ObservableList<Deduction> filtered = FXCollections.observableArrayList();
                for (Deduction d : deductionsList) {
                    String emp = getEmployeeName(d.getEmployeeId()).toLowerCase();
                    String type = d.getTypeDeduction() != null ? d.getTypeDeduction().label().toLowerCase() : "";
                    if (emp.contains(q) || type.contains(q)) {
                        filtered.add(d);
                    }
                }
                tableDeductions.setItems(filtered);
            }
        });
    }

    // ==================== DATA LOADING ====================

    private void loadAllEmployees() {
        try {
            allEmployees = employeeController.handleListMyEmployees(currentUser);
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les employés: " + e.getMessage());
        }
    }

    private void loadAllData() {
        loadFiches();
        loadPrimes();
        loadDeductions();
        updateStatistics();
    }

    private void loadFiches() {
        try {
            List<FichePaie> fiches = fichePaieService.getAllFiches();
            fichesList.clear();
            fichesList.addAll(fiches);
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les fiches de paie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPrimes() {
        try {
            List<Prime> primes = primeService.getAllPrimes();
            primesList.clear();
            primesList.addAll(primes);
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les primes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDeductions() {
        try {
            List<Deduction> deductions = deductionService.getAllDeductions();
            deductionsList.clear();
            deductionsList.addAll(deductions);
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les déductions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        // Nombre de fiches
        lblTotalFiches.setText(String.valueOf(fichesList.size()));
        
        // Total des primes en DT
        BigDecimal totalPrimes = primesList.stream()
                .map(Prime::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalPrimes.setText(formatMontant(totalPrimes));
        
        // Total des déductions en DT
        BigDecimal totalDeductions = deductionsList.stream()
                .map(Deduction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalDeductions.setText(formatMontant(totalDeductions));
        
        // Masse salariale (sum des salaires nets)
        BigDecimal masseSalariale = fichesList.stream()
                .map(FichePaie::getSalaireNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblMasseSalariale != null) {
            lblMasseSalariale.setText(formatMontant(masseSalariale));
        }
    }

    // ==================== EVENT HANDLERS - Fiches de Paie ====================

    @FXML
    private void handleAddFiche() {
        Dialog<FichePaie> dialog = createFicheDialog(null);
        Optional<FichePaie> result = dialog.showAndWait();

        result.ifPresent(fiche -> {
            try {
                fichePaieService.addFiche(fiche);
                loadFiches();
                updateStatistics();
                showInfo("Succès", "Fiche de paie ajoutée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter la fiche de paie: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEditFiche() {
        FichePaie selected = tableFiches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une fiche de paie.");
            return;
        }

        Dialog<FichePaie> dialog = createFicheDialog(selected);
        Optional<FichePaie> result = dialog.showAndWait();

        result.ifPresent(fiche -> {
            try {
                fichePaieService.updateFiche(fiche);
                loadFiches();
                showInfo("Succès", "Fiche de paie modifiée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier la fiche de paie: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteFiche() {
        FichePaie selected = tableFiches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une fiche de paie.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la fiche de paie ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette fiche de paie ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    fichePaieService.deleteFiche(selected.getIdFiche());
                    loadFiches();
                    updateStatistics();
                    showInfo("Succès", "Fiche de paie supprimée avec succès!");
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer la fiche de paie: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleExportPdfFiche() {
        FichePaie selected = tableFiches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une fiche de paie à exporter.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter la fiche de paie en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        
        Employee emp = getEmployeeById(selected.getEmployeeId());
        String employeeName = emp != null ? emp.getLastName() : "Employe";
        fc.setInitialFileName("FichePaie_" + employeeName + "_" + selected.getMoisNom() + "_" + selected.getAnnee() + ".pdf");
        
        Stage stage = (Stage) tableFiches.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        
        if (file != null) {
            try {
                String nomComplet = emp != null ? emp.getFirstName() + " " + emp.getLastName() : "Employé";
                String poste = emp != null ? emp.getJobTitle() : null;
                
                new ExportPdfService().exportFichePaiePDF(selected, nomComplet, poste, file.toPath());
                showInfo("Export PDF réussi", "Fiche de paie exportée vers :\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showError("Erreur d'export", "Impossible de générer le PDF :\n" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleGenerateFiche() {
        CalculFiscalService fiscal = new CalculFiscalService();

        // ── Dialog de simulation / génération ──────────────────────────────
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Générer une Fiche de Paie");
        dialog.setHeaderText("Calcul automatique CNSS / AMG / IRPP — Barème Tunisie 2025");

        ButtonType btnGenerer  = new ButtonType("Générer & Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnExportPdf = new ButtonType("Simuler + Exporter PDF", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerer, btnExportPdf, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 120, 10, 10));

        ComboBox<Employee> cbEmploye = new ComboBox<>(FXCollections.observableArrayList(allEmployees));
        cbEmploye.setConverter(new javafx.util.StringConverter<Employee>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getFirstName() + " " + e.getLastName(); }
            @Override public Employee fromString(String s) { return null; }
        });
        cbEmploye.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbMois = new ComboBox<>(FXCollections.observableArrayList(
            "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
        ));
        cbMois.setValue(LocalDate.now().getMonth().getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.FRENCH));

        TextField txtAnnee       = new TextField(String.valueOf(LocalDate.now().getYear()));
        TextField txtSalaireBrut = new TextField();
        TextField txtPrimes      = new TextField("0");
        txtSalaireBrut.setPromptText("Ex: 1800.000");

        // Résultats calculés (lecture seule)
        Label lblCNSS   = new Label("—");
        Label lblAMG    = new Label("—");
        Label lblIRPP   = new Label("—");
        Label lblNet    = new Label("—");
        lblNet.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e5c8a;");

        // Recalcul en temps réel à chaque frappe
        Runnable recalculer = () -> {
            try {
                String brutTxt = txtSalaireBrut.getText().replace(",", ".").trim();
                String primTxt = txtPrimes.getText().replace(",", ".").trim();
                if (brutTxt.isEmpty()) return;
                BigDecimal brut   = new BigDecimal(brutTxt);
                BigDecimal primes = primTxt.isEmpty() ? BigDecimal.ZERO : new BigDecimal(primTxt);
                BigDecimal[] d = fiscal.calculerDeductionsFiscales(brut);
                lblCNSS.setText(String.format("%.3f DT", d[0]));
                lblAMG.setText(String.format("%.3f DT",  d[1]));
                lblIRPP.setText(String.format("%.3f DT", d[2]));
                BigDecimal net = brut.add(primes).subtract(d[3]).setScale(3, RoundingMode.HALF_UP);
                lblNet.setText(String.format("%.3f DT", net));
            } catch (NumberFormatException ignored) {
                lblCNSS.setText("—"); lblAMG.setText("—"); lblIRPP.setText("—"); lblNet.setText("—");
            }
        };
        txtSalaireBrut.textProperty().addListener((o, ov, nv) -> recalculer.run());
        txtPrimes.textProperty().addListener((o, ov, nv) -> recalculer.run());

        int row = 0;
        grid.add(new Label("Employé :"),        0, row); grid.add(cbEmploye,       1, row++); 
        grid.add(new Label("Mois :"),           0, row); grid.add(cbMois,          1, row++);
        grid.add(new Label("Année :"),          0, row); grid.add(txtAnnee,        1, row++);
        grid.add(new Label("Salaire Brut (DT):"), 0, row); grid.add(txtSalaireBrut, 1, row++);
        grid.add(new Label("Primes (DT) :"),    0, row); grid.add(txtPrimes,       1, row++);
        grid.add(new Separator(),               0, row, 2, 1); row++;
        grid.add(new Label("→ CNSS (9.18%) :"), 0, row); grid.add(lblCNSS,         1, row++);
        grid.add(new Label("→ AMG (4%) :"),     0, row); grid.add(lblAMG,          1, row++);
        grid.add(new Label("→ IRPP :"),         0, row); grid.add(lblIRPP,         1, row++);
        grid.add(new Separator(),               0, row, 2, 1); row++;
        grid.add(new Label("Salaire Net :"),    0, row); grid.add(lblNet,          1, row);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (!result.isPresent() || result.get() == ButtonType.CANCEL) return;

        boolean enregistrer = result.get() == btnGenerer;
        boolean exportPdf   = result.get() == btnExportPdf;

        try {
            Employee emp = cbEmploye.getValue();
            if (emp == null)              throw new IllegalArgumentException("Veuillez sélectionner un employé.");
            if (cbMois.getValue() == null) throw new IllegalArgumentException("Veuillez sélectionner un mois.");
            String brutTxt = txtSalaireBrut.getText().replace(",", ".").trim();
            if (brutTxt.isEmpty())        throw new IllegalArgumentException("Saisissez le salaire brut.");

            BigDecimal brut   = new BigDecimal(brutTxt);
            BigDecimal primes = new BigDecimal(txtPrimes.getText().replace(",", ".").trim());
            BigDecimal[] d    = fiscal.calculerDeductionsFiscales(brut);
            BigDecimal totalDeductions = d[3]; // cnss + amg + irpp
            BigDecimal net = brut.add(primes).subtract(totalDeductions).setScale(3, RoundingMode.HALF_UP);
            int annee = Integer.parseInt(txtAnnee.getText().trim());

            FichePaie fiche = new FichePaie(getMoisIndex(cbMois.getValue()), annee,
                    brut, primes, totalDeductions, net, emp.getId());

            if (enregistrer) {
                // Persiste la fiche de paie
                fichePaieService.addFiche(fiche);

                // Crée automatiquement les déductions CNSS, AMG, IRPP
                DeductionService ds = new DeductionService(connection);
                ds.addDeduction(fiscal.genererDeductionCNSS(brut, emp.getId()));
                ds.addDeduction(fiscal.genererDeductionAMG(brut, emp.getId()));
                BigDecimal baseIrpp = brut.subtract(d[0]).subtract(d[1]);
                ds.addDeduction(fiscal.genererDeductionIRPP(baseIrpp, emp.getId()));

                loadAllData();
                showInfo("Succès",
                    "Fiche générée pour " + emp.getFirstName() + " " + emp.getLastName()
                    + "\nCNSS: " + String.format("%.3f DT", d[0])
                    + "  AMG: " + String.format("%.3f DT", d[1])
                    + "  IRPP: " + String.format("%.3f DT", d[2])
                    + "\nSalaire Net: " + String.format("%.3f DT", net));

            } else if (exportPdf) {
                // Simulation seulement + export PDF
                FileChooser fc = new FileChooser();
                fc.setTitle("Enregistrer la simulation PDF");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
                fc.setInitialFileName("Simulation_" + emp.getLastName()
                        + "_" + cbMois.getValue() + "_" + annee + ".pdf");
                Stage stage = (Stage) tableFiches.getScene().getWindow();
                File file = fc.showSaveDialog(stage);
                if (file != null) {
                    String nomComplet = emp.getFirstName() + " " + emp.getLastName();
                    new ExportPdfService().exportFichePaiePDF(
                            fiche, nomComplet, emp.getJobTitle(), file.toPath());
                    showInfo("Export PDF", "Simulation exportée vers :\n" + file.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            showError("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleRefreshFiches() {
        loadFiches();
        updateStatistics();
    }

    @FXML
    private void handleToggleStatut() {
        FichePaie selected = tableFiches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une fiche de paie.");
            return;
        }
        try {
            fichePaieService.toggleStatutPaiement(selected.getId());
            loadFiches();
            showInfo("Succès", "Statut de paiement mis à jour.");
        } catch (SQLException e) {
            showError("Erreur", "Impossible de modifier le statut : " + e.getMessage());
        }
    }

    // ==================== EVENT HANDLERS - Primes ====================

    @FXML
    private void handleAddPrime() {
        Dialog<Prime> dialog = createPrimeDialog(null);
        Optional<Prime> result = dialog.showAndWait();

        result.ifPresent(prime -> {
            try {
                primeService.addPrime(prime);
                loadPrimes();
                updateStatistics();
                showInfo("Succès", "Prime ajoutée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter la prime: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEditPrime() {
        Prime selected = tablePrimes.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une prime.");
            return;
        }

        Dialog<Prime> dialog = createPrimeDialog(selected);
        Optional<Prime> result = dialog.showAndWait();

        result.ifPresent(prime -> {
            try {
                primeService.updatePrime(prime);
                loadPrimes();
                showInfo("Succès", "Prime modifiée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier la prime: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeletePrime() {
        Prime selected = tablePrimes.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une prime.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la prime ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette prime ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    primeService.deletePrime(selected.getId());
                    loadPrimes();
                    updateStatistics();
                    showInfo("Succès", "Prime supprimée avec succès!");
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer la prime: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefreshPrimes() {
        loadPrimes();
        updateStatistics();
    }

    // ==================== EVENT HANDLERS - Déductions ====================

    @FXML
    private void handleAddDeduction() {
        Dialog<Deduction> dialog = createDeductionDialog(null);
        Optional<Deduction> result = dialog.showAndWait();

        result.ifPresent(deduction -> {
            try {
                deductionService.addDeduction(deduction);
                loadDeductions();
                updateStatistics();
                showInfo("Succès", "Déduction ajoutée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter la déduction: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEditDeduction() {
        Deduction selected = tableDeductions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une déduction.");
            return;
        }

        Dialog<Deduction> dialog = createDeductionDialog(selected);
        Optional<Deduction> result = dialog.showAndWait();

        result.ifPresent(deduction -> {
            try {
                deductionService.updateDeduction(deduction);
                loadDeductions();
                showInfo("Succès", "Déduction modifiée avec succès!");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier la déduction: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteDeduction() {
        Deduction selected = tableDeductions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner une déduction.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la déduction ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette déduction ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    deductionService.deleteDeduction(selected.getId());
                    loadDeductions();
                    updateStatistics();
                    showInfo("Succès", "Déduction supprimée avec succès!");
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer la déduction: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefreshDeductions() {
        loadDeductions();
        updateStatistics();
    }

    // ==================== DIALOG CREATORS ====================

    private Dialog<FichePaie> createFicheDialog(FichePaie existingFiche) {
        Dialog<FichePaie> dialog = new Dialog<>();
        dialog.setTitle(existingFiche == null ? "Nouvelle Fiche de Paie" : "Modifier Fiche de Paie");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Employee> cbEmploye = new ComboBox<>();
        cbEmploye.setItems(FXCollections.observableArrayList(allEmployees));
        cbEmploye.setConverter(new javafx.util.StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.getFirstName() + " " + employee.getLastName();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });

        ComboBox<String> cbMoisLocal = new ComboBox<>(FXCollections.observableArrayList(
            "Janvier","F\u00e9vrier","Mars","Avril","Mai","Juin",
            "Juillet","Ao\u00fbt","Septembre","Octobre","Novembre","D\u00e9cembre"));
        TextField txtAnnee = new TextField();
        TextField txtSalaireBrut = new TextField();
        TextField txtTotalPrimes = new TextField();
        TextField txtTotalDeductions = new TextField();
        TextField txtSalaireNet = new TextField();
        txtSalaireNet.setEditable(false);
        txtSalaireNet.setStyle("-fx-opacity: 0.8;");

        Runnable updateNetSalary = () -> {
            try {
                if (txtSalaireBrut.getText().isEmpty() || 
                    txtTotalPrimes.getText().isEmpty() || 
                    txtTotalDeductions.getText().isEmpty()) {
                    txtSalaireNet.setText("");
                    return;
                }
                BigDecimal brut = new BigDecimal(txtSalaireBrut.getText().replace(",", ".").trim());
                BigDecimal primes = new BigDecimal(txtTotalPrimes.getText().replace(",", ".").trim());
                BigDecimal deductions = new BigDecimal(txtTotalDeductions.getText().replace(",", ".").trim());
                BigDecimal net = brut.add(primes).subtract(deductions);
                txtSalaireNet.setText(net.toString());
            } catch (NumberFormatException e) {
                txtSalaireNet.setText("Erreur");
            }
        };

        txtSalaireBrut.textProperty().addListener((obs, old, newVal) -> updateNetSalary.run());
        txtTotalPrimes.textProperty().addListener((obs, old, newVal) -> updateNetSalary.run());
        txtTotalDeductions.textProperty().addListener((obs, old, newVal) -> updateNetSalary.run());

        if (existingFiche != null) {
            Employee emp = getEmployeeById(existingFiche.getEmployeeId());
            cbEmploye.setValue(emp);
            cbMoisLocal.setValue(existingFiche.getMoisNom());
            txtAnnee.setText(String.valueOf(existingFiche.getAnnee()));
            txtSalaireBrut.setText(existingFiche.getSalaireBrut().toString());
            txtTotalPrimes.setText(existingFiche.getTotalPrimes().toString());
            txtTotalDeductions.setText(existingFiche.getTotalDeductions().toString());
            txtSalaireNet.setText(existingFiche.getSalaireNet().toString());
        }

        grid.add(new Label("Employé:"), 0, 0);
        grid.add(cbEmploye, 1, 0);
        grid.add(new Label("Mois:"), 0, 1);
        grid.add(cbMoisLocal, 1, 1);
        grid.add(new Label("Année:"), 0, 2);
        grid.add(txtAnnee, 1, 2);
        grid.add(new Label("Salaire Brut:"), 0, 3);
        grid.add(txtSalaireBrut, 1, 3);
        grid.add(new Label("Total Primes:"), 0, 4);
        grid.add(txtTotalPrimes, 1, 4);
        grid.add(new Label("Total Déductions:"), 0, 5);
        grid.add(txtTotalDeductions, 1, 5);
        grid.add(new Label("Salaire Net:"), 0, 6);
        grid.add(txtSalaireNet, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Employee selectedEmp = cbEmploye.getValue();
                    if (selectedEmp == null) throw new IllegalArgumentException("Veuillez sélectionner un employé");

                    BigDecimal salaireBrut = new BigDecimal(txtSalaireBrut.getText().replace(",", ".").trim());
                    BigDecimal totalPrimes = new BigDecimal(txtTotalPrimes.getText().replace(",", ".").trim());
                    BigDecimal totalDeductions = new BigDecimal(txtTotalDeductions.getText().replace(",", ".").trim());
                    BigDecimal salaireNet = salaireBrut.add(totalPrimes).subtract(totalDeductions);

                    int moisVal = getMoisIndex(cbMoisLocal.getValue());
                    if (existingFiche == null) {
                        return new FichePaie(
                            moisVal,
                            Integer.parseInt(txtAnnee.getText()),
                            salaireBrut,
                            totalPrimes,
                            totalDeductions,
                            salaireNet,
                            selectedEmp.getId()
                        );
                    } else {
                        existingFiche.setMois(moisVal);
                        existingFiche.setAnnee(Integer.parseInt(txtAnnee.getText()));
                        existingFiche.setSalaireBrut(salaireBrut);
                        existingFiche.setTotalPrimes(totalPrimes);
                        existingFiche.setTotalDeductions(totalDeductions);
                        existingFiche.setSalaireNet(salaireNet);
                        existingFiche.setEmployeeId(selectedEmp.getId());
                        return existingFiche;
                    }
                } catch (Exception e) {
                    showError("Erreur de validation", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private Dialog<Prime> createPrimeDialog(Prime existingPrime) {
        Dialog<Prime> dialog = new Dialog<>();
        dialog.setTitle(existingPrime == null ? "Nouvelle Prime" : "Modifier Prime");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Employee> cbEmploye = new ComboBox<>();
        cbEmploye.setItems(FXCollections.observableArrayList(allEmployees));
        cbEmploye.setConverter(new javafx.util.StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.getFirstName() + " " + employee.getLastName();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });

        ComboBox<PrimeType> cbType = new ComboBox<>(FXCollections.observableArrayList(PrimeType.values()));
        cbType.setConverter(new javafx.util.StringConverter<PrimeType>() {
            @Override public String toString(PrimeType t) { return t == null ? "" : t.label(); }
            @Override public PrimeType fromString(String s) { return PrimeType.fromLabel(s); }
        });
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setValue(PrimeType.PRIME);
        TextField txtMontant = new TextField();
        DatePicker dpDate = new DatePicker(LocalDate.now());
        TextArea txtMotif = new TextArea();
        txtMotif.setPromptText("Motif (facultatif)");
        txtMotif.setPrefRowCount(2);
        txtMotif.setWrapText(true);

        if (existingPrime != null) {
            Employee emp = getEmployeeById(existingPrime.getEmployeeId());
            cbEmploye.setValue(emp);
            cbType.setValue(existingPrime.getTypePrime());
            txtMontant.setText(existingPrime.getMontant().toString());
            dpDate.setValue(existingPrime.getDateAttribution());
            txtMotif.setText(existingPrime.getMotif() != null ? existingPrime.getMotif() : "");
        }

        grid.add(new Label("Employé:"), 0, 0);
        grid.add(cbEmploye, 1, 0);
        grid.add(new Label("Type de Prime:"), 0, 1);
        grid.add(cbType, 1, 1);
        grid.add(new Label("Montant:"), 0, 2);
        grid.add(txtMontant, 1, 2);
        grid.add(new Label("Date Attribution:"), 0, 3);
        grid.add(dpDate, 1, 3);
        grid.add(new Label("Motif:"), 0, 4);
        grid.add(txtMotif, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Employee selectedEmp = cbEmploye.getValue();
                    if (selectedEmp == null) throw new IllegalArgumentException("Veuillez sélectionner un employé");

                    PrimeType selectedType = cbType.getValue();
                    if (selectedType == null) throw new IllegalArgumentException("Veuillez sélectionner un type de prime");
                    if (dpDate.getValue() == null) throw new IllegalArgumentException("Veuillez sélectionner une date");
                    BigDecimal montant = new BigDecimal(txtMontant.getText().replace(",", ".").trim());
                    if (existingPrime == null) {
                        Prime p = new Prime(
                            selectedType,
                            montant,
                            dpDate.getValue(),
                            selectedEmp.getId()
                        );
                        p.setMotif(txtMotif.getText().trim().isEmpty() ? null : txtMotif.getText().trim());
                        return p;
                    } else {
                        existingPrime.setTypePrime(selectedType);
                        existingPrime.setMontant(montant);
                        existingPrime.setDateAttribution(dpDate.getValue());
                        existingPrime.setEmployeeId(selectedEmp.getId());
                        existingPrime.setMotif(txtMotif.getText().trim().isEmpty() ? null : txtMotif.getText().trim());
                        return existingPrime;
                    }
                } catch (Exception e) {
                    showError("Erreur de validation", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private Dialog<Deduction> createDeductionDialog(Deduction existingDeduction) {
        Dialog<Deduction> dialog = new Dialog<>();
        dialog.setTitle(existingDeduction == null ? "Nouvelle Déduction" : "Modifier Déduction");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Employee> cbEmploye = new ComboBox<>();
        cbEmploye.setItems(FXCollections.observableArrayList(allEmployees));
        cbEmploye.setConverter(new javafx.util.StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.getFirstName() + " " + employee.getLastName();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });

        ComboBox<DeductionType> cbType = new ComboBox<>(FXCollections.observableArrayList(DeductionType.values()));
        cbType.setConverter(new javafx.util.StringConverter<DeductionType>() {
            @Override public String toString(DeductionType t) { return t == null ? "" : t.label(); }
            @Override public DeductionType fromString(String s) { return DeductionType.fromLabel(s); }
        });
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setValue(DeductionType.CNSS);
        TextField txtMontant = new TextField();
        DatePicker dpDate = new DatePicker(LocalDate.now());
        TextArea txtMotifDed = new TextArea();
        txtMotifDed.setPromptText("Motif (facultatif)");
        txtMotifDed.setPrefRowCount(2);
        txtMotifDed.setWrapText(true);

        if (existingDeduction != null) {
            Employee emp = getEmployeeById(existingDeduction.getEmployeeId());
            cbEmploye.setValue(emp);
            cbType.setValue(existingDeduction.getTypeDeduction());
            txtMontant.setText(existingDeduction.getMontant().toString());
            dpDate.setValue(existingDeduction.getDateDeduction());
            txtMotifDed.setText(existingDeduction.getMotif() != null ? existingDeduction.getMotif() : "");
        }

        grid.add(new Label("Employé:"), 0, 0);
        grid.add(cbEmploye, 1, 0);
        grid.add(new Label("Type de Déduction:"), 0, 1);
        grid.add(cbType, 1, 1);
        grid.add(new Label("Montant:"), 0, 2);
        grid.add(txtMontant, 1, 2);
        grid.add(new Label("Date Déduction:"), 0, 3);
        grid.add(dpDate, 1, 3);
        grid.add(new Label("Motif:"), 0, 4);
        grid.add(txtMotifDed, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Employee selectedEmp = cbEmploye.getValue();
                    if (selectedEmp == null) throw new IllegalArgumentException("Veuillez sélectionner un employé");

                    DeductionType selectedType = cbType.getValue();
                    if (selectedType == null) throw new IllegalArgumentException("Veuillez sélectionner un type de déduction");
                    if (dpDate.getValue() == null) throw new IllegalArgumentException("Veuillez sélectionner une date");
                    BigDecimal montant = new BigDecimal(txtMontant.getText().replace(",", ".").trim());
                    if (existingDeduction == null) {
                        Deduction d = new Deduction(
                            selectedType,
                            montant,
                            dpDate.getValue(),
                            selectedEmp.getId()
                        );
                        d.setMotif(txtMotifDed.getText().trim().isEmpty() ? null : txtMotifDed.getText().trim());
                        return d;
                    } else {
                        existingDeduction.setTypeDeduction(selectedType);
                        existingDeduction.setMontant(montant);
                        existingDeduction.setDateDeduction(dpDate.getValue());
                        existingDeduction.setEmployeeId(selectedEmp.getId());
                        existingDeduction.setMotif(txtMotifDed.getText().trim().isEmpty() ? null : txtMotifDed.getText().trim());
                        return existingDeduction;
                    }
                } catch (Exception e) {
                    showError("Erreur de validation", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    // ==================== HELPER METHODS ====================

    private String getEmployeeName(int employeeId) {
        if (allEmployees == null) return "Employé #" + employeeId;
        
        return allEmployees.stream()
            .filter(e -> e.getId() == employeeId)
            .findFirst()
            .map(e -> e.getFirstName() + " " + e.getLastName())
            .orElse("Employé #" + employeeId);
    }

    private Employee getEmployeeById(int employeeId) {
        if (allEmployees == null) return null;
       
        return allEmployees.stream()
            .filter(e -> e.getId() == employeeId)
            .findFirst()
            .orElse(null);
    }

    private String formatMontant(BigDecimal montant) {
        if (montant == null) return "0.00 DT";
        return String.format("%.2f DT", montant);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.toString();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== MONTH HELPERS ====================

    private static final String[] MOIS_NOMS = {
        "", "Janvier", "F\u00e9vrier", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Ao\u00fbt", "Septembre", "Octobre", "Novembre", "D\u00e9cembre"
    };

    private String getMoisNom(int mois) {
        return (mois >= 1 && mois <= 12) ? MOIS_NOMS[mois] : String.valueOf(mois);
    }

    private int getMoisIndex(String moisNom) {
        if (moisNom == null) return LocalDate.now().getMonthValue();
        for (int i = 1; i <= 12; i++) {
            if (MOIS_NOMS[i].equalsIgnoreCase(moisNom)) return i;
        }
        try { return Integer.parseInt(moisNom); } catch (NumberFormatException e) {
            return LocalDate.now().getMonthValue();
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== CONVERSION DE DEVISES ====================

    private void setupConversionWidgetListeners() {
        if (txtMontantConversion == null) return;
        txtMontantConversion.textProperty().addListener((obs, oldVal, newVal) -> calculateConversion());
    }

    @FXML
    private void handleRefreshRates() {
        loadExchangeRatesAsync();
    }

    private void loadExchangeRatesAsync() {
        if (lblApiStatus != null) lblApiStatus.setText("🔄 Chargement des taux...");

        Task<BigDecimal[]> task = new Task<BigDecimal[]>() {
            @Override
            protected BigDecimal[] call() throws Exception {
                BigDecimal tauxEUR = conversionService.getTauxDeChange("TND", "EUR");
                BigDecimal tauxUSD = conversionService.getTauxDeChange("TND", "USD");
                return new BigDecimal[]{tauxEUR, tauxUSD};
            }
        };

        task.setOnSucceeded(e -> {
            BigDecimal[] rates = task.getValue();
            if (lblTauxEUR != null) lblTauxEUR.setText(String.format("%.4f €", rates[0]));
            if (lblTauxUSD != null) lblTauxUSD.setText(String.format("%.4f $", rates[1]));
            if (lblLastUpdate != null) lblLastUpdate.setText("Dernière mise à jour: " +
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (lblApiStatus != null) {
                lblApiStatus.setText("✅ API connectée — Taux mis à jour en temps réel");
                lblApiStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60; -fx-background-color: #d5f4e6; -fx-padding: 6 10; -fx-background-radius: 6;");
            }
            calculateConversion();
        });

        task.setOnFailed(e -> {
            if (lblApiStatus != null) {
                lblApiStatus.setText("⚠️ API indisponible — Taux par défaut utilisés");
                lblApiStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0392b; -fx-background-color: #fadbd8; -fx-padding: 6 10; -fx-background-radius: 6;");
            }
            if (lblTauxEUR != null) lblTauxEUR.setText("0.2900 €");
            if (lblTauxUSD != null) lblTauxUSD.setText("0.3200 $");
        });

        Thread t = new Thread(task, "exchange-rates-loader");
        t.setDaemon(true);
        t.start();
    }

    private void calculateConversion() {
        if (txtMontantConversion == null || lblConversionEUR == null || lblConversionUSD == null) return;

        String text = txtMontantConversion.getText().replace(",", ".").trim();
        if (text.isEmpty()) {
            lblConversionEUR.setText("—");
            lblConversionUSD.setText("—");
            return;
        }

        try {
            BigDecimal montantTND = new BigDecimal(text);
            BigDecimal montantEUR = conversionService.convertirTndVersEur(montantTND);
            BigDecimal montantUSD = conversionService.convertirTndVersUsd(montantTND);

            lblConversionEUR.setText(String.format("%.3f €", montantEUR));
            lblConversionUSD.setText(String.format("%.3f $", montantUSD));

        } catch (NumberFormatException e) {
            lblConversionEUR.setText("—");
            lblConversionUSD.setText("—");
        } catch (Exception e) {
            lblConversionEUR.setText("Erreur API");
            lblConversionUSD.setText("Erreur API");
        }
    }
}
