package org.ugp.mc.deobfuscator;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.swing.filechooser.FileSystemView;
import net.lingala.zip4j.ZipFile;
import org.ugp.mc.deobfuscator.ClassTypeLoader;

public class Deobfuscator3000 {
    public static final DecompilerSettings DECOMP_SETTINGS = DecompilerSettings.javaDefaults();
    @FXML
    public Button deobf;
    @FXML
    public VBox log;
    @FXML
    public ScrollPane logScoll;
    @FXML
    public TextField t1;
    @FXML
    public Button dir1;
    @FXML
    public Button file1;
    @FXML
    public HBox mappingsPathBox;
    @FXML
    public TextField t2;
    @FXML
    public Button dir2;
    @FXML
    public Button file2;
    @FXML
    public HBox modPathBox;
    @FXML
    public CheckBox shouldDeobf;
    @FXML
    public ProgressBar progress;
    @FXML
    public Label progressText;
    private KeyCode deobfKey = KeyCode.ENTER;
    private Thread deobfThread;
    private boolean isDeobfRunning;
    private File actualMapping;
    private File actualMod;
    private HashMap<String, String> deobfusctionMappings = new HashMap();

    public void initialize(Application app, Stage primaryStage) throws Exception {
        primaryStage.setTitle("Minecraft Deobfuscator3000 v1.2.3");
        primaryStage.getIcons().add(new Image("org/ugp/mc/deobfuscator/assets/icon.png"));
        primaryStage.setMinWidth(450.0);
        primaryStage.setMinHeight(400.0);
        this.doLog("Log will appear here! Hold Ctrl + Middle mouse to clear! Hold Ctrl + 2x Left click to copy to clipboard!", Color.GRAY, false);
        this.mappingsPathBox.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.LINK);
            }
        });
        this.mappingsPathBox.setOnDragDropped(ev -> {
            this.t1.setText(ev.getDragboard().getFiles().get(0).getAbsolutePath());
            ev.setDropCompleted(true);
        });
        this.t1.setTooltip(new Tooltip("Path to file/folder with excel deobfuscation mapping files (fields.csv, methods.csv and optionally params.csv)!"));
        this.t1.textProperty().addListener((observable, oldValue, newValue) -> {
            File mappingFile = new File(newValue = newValue.trim());
            if (this.loadMappings(mappingFile)) {
                this.t1.setStyle("-fx-text-inner-color: black;");
            } else {
                this.t1.setStyle("-fx-text-inner-color: red;");
            }
        });
        this.dir1.setTooltip(new Tooltip("Browse directory..."));
        this.dir1.setOnAction(ev -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose a folder with valid MC deobf mappings the excel files. Usually fields.csv, methods.csv and params.csv!");
            chooser.setInitialDirectory(this.actualMapping == null ? FileSystemView.getFileSystemView().getHomeDirectory() : (this.actualMapping.isDirectory() ? this.actualMapping : this.actualMapping.getParentFile()));
            File f = chooser.showDialog(primaryStage);
            if (f != null) {
                this.t1.setText(f.getAbsolutePath());
            }
        });
        this.file1.setTooltip(new Tooltip("Browse file..."));
        this.file1.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose a file with valid MC deobf mappings the excel files. Or excel file itself!");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mapping files!", "*.jar", "*.zip", "*.csv"));
            chooser.setInitialDirectory(this.actualMapping == null ? FileSystemView.getFileSystemView().getHomeDirectory() : (this.actualMapping.isDirectory() ? this.actualMapping : this.actualMapping.getParentFile()));
            File f = chooser.showOpenDialog(primaryStage);
            if (f != null) {
                this.t1.setText(f.getAbsolutePath());
            }
        });
        Preferences prefers = Preferences.userNodeForPackage(this.getClass());
        this.t1.setText(prefers.get("lastMappings", ""));
        this.modPathBox.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.LINK);
            }
        });
        this.modPathBox.setOnDragDropped(ev -> {
            this.t2.setText(ev.getDragboard().getFiles().get(0).getAbsolutePath());
            ev.setDropCompleted(true);
        });
        this.t2.setTooltip(new Tooltip("Path to minecraft mod files to deobfuscate using selected mappings!"));
        this.t2.textProperty().addListener((observable, oldValue, newValue) -> {
            File modFile = new File(newValue = newValue.trim());
            if (this.loadMod(modFile)) {
                this.t2.setStyle("-fx-text-inner-color: black;");
            } else {
                this.t2.setStyle("-fx-text-inner-color: red;");
            }
        });
        this.dir2.setTooltip(new Tooltip("Browse directory..."));
        this.dir2.setOnAction(ev -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select your mod folder!");
            chooser.setInitialDirectory(this.actualMod == null ? FileSystemView.getFileSystemView().getHomeDirectory() : (this.actualMod.isDirectory() ? this.actualMod : this.actualMod.getParentFile()));
            File f = chooser.showDialog(primaryStage);
            if (f != null) {
                this.t2.setText(f.getAbsolutePath());
            }
        });
        this.file2.setTooltip(new Tooltip("Browse file..."));
        this.file2.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select your mod file!");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java frienly files!", "*.java", "*.class", "*.jar", "*.zip"));
            chooser.setInitialDirectory(this.actualMod == null ? FileSystemView.getFileSystemView().getHomeDirectory() : (this.actualMod.isDirectory() ? this.actualMod : this.actualMod.getParentFile()));
            File f = chooser.showOpenDialog(primaryStage);
            if (f != null) {
                this.t2.setText(f.getAbsolutePath());
            }
        });
        MenuItem copyLog = new MenuItem("Copy log");
        copyLog.setOnAction(ev -> this.childrensToClipboard(this.log));
        MenuItem clsLog = new MenuItem("Clear log");
        clsLog.setOnAction(ev -> this.log.getChildren().clear());
        ContextMenu logContextMenu = new ContextMenu(copyLog, clsLog);
        this.logScoll.setTooltip(new Tooltip("Log will appear here! Hold Ctrl + Middle mouse to clear! Hold Ctrl + 2x Left click to copy to clipboard!"));
        this.log.setSpacing(-10.0);
        this.log.setStyle("-fx-background-color: white; -fx-border-color: lightgray; -fx-control-inner-background: TRANSPARENT;");
        this.log.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.LINK);
            }
        });
        this.log.setOnDragDropped(ev -> {
            this.t2.setText(ev.getDragboard().getFiles().get(0).getAbsolutePath());
            ev.setDropCompleted(true);
        });
        this.log.addEventFilter(MouseEvent.MOUSE_RELEASED, ev -> {
            if (ev.getButton() == MouseButton.MIDDLE && ev.isControlDown()) {
                this.log.getChildren().clear();
            } else if (!this.log.getChildren().isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() > 1 && ev.isControlDown()) {
                this.childrensToClipboard(this.log);
            } else if (ev.getButton() == MouseButton.SECONDARY) {
                logContextMenu.show(this.logScoll, ev.getScreenX(), ev.getScreenY());
            } else {
                logContextMenu.hide();
            }
        });
        this.log.heightProperty().addListener((obs, oldV, newV) -> {
            if (!this.logScoll.isPressed()) {
                this.logScoll.setVvalue(1.0);
            }
        });
        this.log.getChildren().addListener(elm -> {
            clsLog.setDisable(elm.getList().isEmpty());
            copyLog.setDisable(elm.getList().isEmpty());
        });
        this.deobf.setTooltip(new Tooltip());
        this.deobf.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            if (!(this.actualMod == null || !this.actualMod.exists() || (this.actualMapping == null || !this.actualMapping.exists() || this.t1.getStyle().contains("red;") || this.t1.getText().isEmpty()) && this.shouldDeobf.isSelected() || this.t2.getStyle().contains("red;") || this.t2.getText().isEmpty())) {
                return false;
            }
            return true;
        }, this.t1.textProperty(), this.t2.textProperty(), this.t1.styleProperty(), this.t2.styleProperty(), this.shouldDeobf.selectedProperty()));
        this.deobf.setOnAction(ev -> {
            if (this.isDeobfRunning || this.deobfThread != null && this.deobfThread.isAlive()) {
                this.isDeobfRunning = false;
            } else {
                boolean isZip;
                boolean bl = isZip = this.actualMod.getAbsolutePath().toLowerCase().endsWith(".zip") || this.actualMod.getAbsolutePath().toLowerCase().endsWith(".jar");
                if (isZip) {
                    this.doLog("Extracting mod files!");
                    File p = new File(String.valueOf(this.actualMod.getAbsolutePath().replaceAll(".zip|.jar", "")) + " deobf");
                    if (p.exists()) {
                        Alert sureUwantReplace = new Alert(Alert.AlertType.CONFIRMATION, "Mod was probably already deobfuscated before!\nAre you sure you want to override?\n", ButtonType.YES, ButtonType.CANCEL);
                        sureUwantReplace.setTitle("Deobfuscated?");
                        sureUwantReplace.setHeaderText("\"" + p + "\" already exists!");
                        sureUwantReplace.setOnCloseRequest(ev2 -> new Thread(() -> {
                            try {
                                Thread.sleep(1000L);
                                this.deobfKey = KeyCode.ENTER;
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start());
                        this.deobfKey = null;
                        if (sureUwantReplace.showAndWait().get() != ButtonType.YES) {
                            this.doLog("Canceling files extraction and deobfuscation of \"" + this.actualMod + "\"!");
                            this.doLogEndl();
                            ev.consume();
                            return;
                        }
                        this.deleteDirectory(p);
                    }
                }
                this.isDeobfRunning = true;
                this.deobfThread = new Thread(() -> {
                    try {
                        File f;
                        double t0 = System.nanoTime();
                        Platform.runLater(() -> this.progressText.setText("Preparing... "));
                        DECOMP_SETTINGS.setTypeLoader(new ClassTypeLoader());
                        File file = f = isZip ? this.unzip(this.actualMod, String.valueOf(this.actualMod.getAbsolutePath().replaceAll(".zip|.jar", "")) + " deobf") : this.actualMod;
                        if (isZip) {
                            this.doLog("Mod files successfully extracted to \"" + f + "\"!", Color.GREEN);
                        }
                        Map.Entry[] mappings = this.deobfusctionMappings.entrySet().toArray(new Map.Entry[this.deobfusctionMappings.size()]);
                        int[] count = this.processMod(new int[2], this.shouldDeobf.isSelected(), f, mappings);
                        double tDiff = ((double)System.nanoTime() - t0) / 1.0E9;
                        if (!this.isDeobfRunning) {
                            this.doLog("Canceling deobfuscation of \"" + this.actualMod + "\"!");
                            if (count[0] + count[1] > 1) {
                                this.doLog("However " + (count[0] + count[1]) + " files that were processed before cancelation will remain!");
                            }
                            Platform.runLater(() -> {
                                String str = this.progressText.getText();
                                double prog = this.progress.getProgress();
                                this.t2.setText(f.getAbsolutePath());
                                Platform.runLater(() -> {
                                    if (str.length() > 1) {
                                        this.progress.setProgress(prog);
                                        this.progressText.setText(String.valueOf(str.substring(0, str.length() - 1)) + " (canceled)!");
                                    }
                                });
                            });
                        } else {
                            this.doLog("#-------------------------------------------------------------------# Complete #-------------------------------------------------------------------#", Color.GREEN);
                            if (count[1] + count[0] > 0) {
                                this.doLog("Processing of " + (isZip ? "mod " : "") + "\"" + this.actualMod + "\" has just finished!", Color.GREEN);
                                if (this.shouldDeobf.isSelected()) {
                                    this.doLog("Mappings used: \"" + this.actualMapping + "\"", Color.GREEN);
                                }
                                if (count[1] > 0) {
                                    this.doLog("Deobfuscated files: " + count[1], Color.GREEN);
                                }
                                if (count[0] > 0) {
                                    this.doLog("Decompiled files: " + count[0], Color.GREEN);
                                }
                                this.doLog("It took " + String.format("%.2f", tDiff) + " sec! ", Color.GREEN);
                            } else {
                                this.doLog("Nothing to process in \"" + this.actualMod + "\"! It took " + String.format("%.2f", tDiff) + " sec!\"", Color.DARKORANGE);
                            }
                        }
                        if (f.exists()) {
                            Button open = new Button("Open processed files");
                            VBox.setMargin(open, new Insets(7.0));
                            open.setOnAction(ev2 -> {
                                try {
                                    Desktop.getDesktop().open(f);
                                    open.setStyle("-fx-text-fill: black;");
                                }
                                catch (Exception e) {
                                    open.setStyle("-fx-text-fill: red;");
                                }
                            });
                            this.doLog(open);
                        }
                        this.doLogEndl();
                    }
                    catch (Exception e) {
                        this.doLog("Processing failed due to " + e.toString() + "!", Color.RED);
                        Platform.runLater(() -> this.progressText.setText("Failure..."));
                    }
                    this.isDeobfRunning = false;
                });
                this.deobfThread.start();
            }
        });
        Scene sc = primaryStage.getScene();
        sc.setOnKeyReleased(ev -> {
            if (ev.getCode() == this.deobfKey) {
                this.deobf.fire();
            }
        });
        sc.setOnMouseEntered(ev -> {
            if (this.isDeobfRunning) {
                return;
            }
            if (!this.t1.getText().trim().isEmpty()) {
                if (this.loadMappings(new File(this.t1.getText().trim()))) {
                    this.t1.setStyle("-fx-text-inner-color: black;");
                } else {
                    this.t1.setStyle("-fx-text-inner-color: red;");
                }
            }
            if (!this.t2.getText().trim().isEmpty()) {
                if (this.loadMod(new File(this.t2.getText().trim()))) {
                    this.t2.setStyle("-fx-text-inner-color: black;");
                } else {
                    this.t2.setStyle("-fx-text-inner-color: red;");
                }
            }
        });
        new AnimationTimer(){

            @Override
            public void handle(long now) {
                Deobfuscator3000.this.mappingsPathBox.setDisable(Deobfuscator3000.this.isDeobfRunning || !Deobfuscator3000.this.shouldDeobf.isSelected());
                Deobfuscator3000.this.modPathBox.setDisable(Deobfuscator3000.this.isDeobfRunning);
                Deobfuscator3000.this.shouldDeobf.setDisable(Deobfuscator3000.this.isDeobfRunning);
                Deobfuscator3000.this.deobf.setText(Deobfuscator3000.this.deobfThread == null || !Deobfuscator3000.this.deobfThread.isAlive() ? (Deobfuscator3000.this.shouldDeobf.isSelected() ? "Deobfuscate" : "Decompile") : "Cancel");
                Deobfuscator3000.this.deobf.getTooltip().setText(!Deobfuscator3000.this.isDeobfRunning ? "Press Enter or click this button to deobfuscate your mod!" : "Press Enter or click this button to cancel running deobfuscation!");
            }
        }.start();
        Alert aClose = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to leave?\nIn progress deobfuscation will be canceled!", ButtonType.YES, ButtonType.NO);
        aClose.setTitle("Leave?");
        aClose.setHeaderText("Deobfuscation in progress!");
        primaryStage.setOnCloseRequest(ev -> {
            if (this.isDeobfRunning) {
                if (aClose.isShowing() || aClose.showAndWait().get() != ButtonType.YES) {
                    ev.consume();
                    return;
                }
                if (this.isDeobfRunning) {
                    this.deobf.fire();
                    while (this.deobfThread.isAlive()) {
                    }
                }
            }
            if (this.actualMapping != null && !this.t1.getStyle().contains("red;")) {
                prefers.put("lastMappings", this.t1.getText());
            } else {
                prefers.put("lastMappings", "");
            }
            Platform.exit();
            System.exit(0);
        });
    }

    private List<File> walkThrough(List<File> alreadyKnownFiles, File dir) {
        alreadyKnownFiles = alreadyKnownFiles == null ? new ArrayList<File>() : alreadyKnownFiles;
        File[] files = dir.listFiles();
        if (files == null) {
            files = new File[]{dir};
        }
        File[] fileArray = files;
        int n = files.length;
        int n2 = 0;
        while (n2 < n) {
            File f = fileArray[n2];
            if (f.isFile()) {
                alreadyKnownFiles.add(f);
            } else {
                this.walkThrough(alreadyKnownFiles, f);
            }
            ++n2;
        }
        return alreadyKnownFiles;
    }

    private int[] processMod(int[] counts, boolean deobf, File mod, Map.Entry<String, String>[] mappings) {
        ArrayList<File> innerClassRemnants = new ArrayList<File>();
        List<File> files = this.walkThrough(null, mod);
        double processed = 0.0;
        for (File f : files) {
            String str = f.getAbsolutePath();
            if (!str.endsWith(".class") || str.indexOf(36) == -1) {
                if (!this.isDeobfRunning) break;
                boolean[] flags = this.decompileAndDeobfuscate(deobf, f, mappings);
                counts[0] = counts[0] + (flags[0] ? 1 : 0);
                counts[1] = counts[1] + (flags[1] ? 1 : 0);
                if (flags[1]) {
                    this.doLog("Deobfuscating: \"" + f + "\"!");
                } else if (!flags[0]) {
                    this.doLog("Ignoring: \"" + f + "\"!", Color.DARKORANGE);
                }
            } else {
                innerClassRemnants.add(f);
            }
            this.updateProgress(processed += 1.0, files.size());
        }
        if (!innerClassRemnants.isEmpty()) {
            this.doLog("Injecting and deleting " + innerClassRemnants.size() + " inner classes!");
            for (File file : innerClassRemnants) {
                file.delete();
            }
        }
        return counts;
    }

    private String decompile(String sourcePath) {
        if (!sourcePath.endsWith(".class")) {
            return null;
        }
        StringWriter sw = new StringWriter();
        Decompiler.decompile(sourcePath, new PlainTextOutput(sw), DECOMP_SETTINGS);
        String result = sw.toString();
        return result;
    }

    private boolean[] decompileAndDeobfuscate(boolean deobf, File source, Map.Entry<String, String>[] mappings) {
        String path = source.getAbsolutePath();
        if (path.endsWith(".java") || path.endsWith(".class")) {
            String orig = null;
            String decompErr = "!!! ERROR: ";
            boolean wasDecomp = false;
            boolean wasDeobf = false;
            try {
                String str = this.decompile(path);
                if (str == null) {
                    String l;
                    BufferedReader r = new BufferedReader(new FileReader(source));
                    StringBuilder sb = new StringBuilder();
                    while ((l = r.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                    str = orig = sb.toString();
                    r.close();
                } else if (str.startsWith(decompErr)) {
                    StringBuilder sb = new StringBuilder("Decompiler f" + str.substring(decompErr.length() + 1, str.length() - 2) + "\"!");
                    this.doLog(sb.insert(sb.indexOf("class") + 6, "\""), Color.RED);
                } else {
                    this.doLog("Decompiling: \"" + source + "\"!");
                    File file = source;
                    source = new File(Deobfuscator3000.fastReplace(path, ".class", ".java"));
                    file.renameTo(source);
                    wasDecomp = true;
                    orig = str;
                }
                if (deobf) {
                    if (str.startsWith("//Deobfuscated with")) {
                        return new boolean[]{wasDecomp, wasDeobf};
                    }
                    int i = 0;
                    while (i < mappings.length) {
                        Map.Entry<String, String> ent = mappings[i];
                        str = Deobfuscator3000.fastReplace(str, ent.getKey(), ent.getValue());
                        ++i;
                    }
                    boolean bl = wasDeobf = !str.equals(orig);
                }
                if (wasDecomp || wasDeobf) {
                    if (wasDecomp) {
                        str = "//Decompiled by Procyon!\n\n" + str;
                    }
                    if (deobf) {
                        str = "//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings \"" + this.actualMapping.getAbsolutePath() + "\"!\n\n" + str;
                    }
                    if (!source.exists()) {
                        return new boolean[2];
                    }
                    BufferedWriter bf = new BufferedWriter(new FileWriter(source));
                    bf.write(str);
                    bf.close();
                    return new boolean[]{wasDecomp, wasDeobf};
                }
            }
            catch (Exception e) {
                this.doLog(String.valueOf(e.toString()) + " while deobfuscating " + source, Color.RED);
                e.printStackTrace();
            }
        }
        return new boolean[2];
    }

    static String fastReplace(String str, String target, String replacement) {
        int targetLength = target.length();
        if (targetLength == 0) {
            return str;
        }
        int i1 = 0;
        int i2 = str.indexOf(target);
        if (i2 < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(targetLength > replacement.length() ? str.length() : str.length() * 2);
        do {
            sb.append(str, i1, i2).append(replacement);
        } while ((i2 = str.indexOf(target, i1 = i2 + targetLength)) > 0);
        return sb.append(str, i1, str.length()).toString();
    }

    private boolean loadMod(File modFile) {
        String absPath = modFile.getAbsolutePath().toLowerCase();
        if (!modFile.exists() || absPath.endsWith(":") || absPath.endsWith(":\\") || absPath.endsWith(":/") || absPath.equalsIgnoreCase(new File("").getAbsolutePath()) || FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath().contains(absPath)) {
            return false;
        }
        if (this.actualMod == null || !this.actualMod.equals(modFile)) {
            this.actualMod = modFile;
            this.doLog("Mod successfully loaded from \"" + modFile + "\"!", Color.GREEN);
            this.updateProgress(0.0, 1.0);
        }
        return true;
    }

    private boolean loadMappings(File mappingFile) {
        String absPath = mappingFile.getAbsolutePath().toLowerCase();
        if (!mappingFile.exists() || absPath.endsWith(":") || absPath.endsWith(":\\") || absPath.endsWith(":/") || FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath().contains(absPath)) {
            return false;
        }
        if (absPath.endsWith(".zip") || absPath.endsWith(".jar")) {
            try {
                String dest = mappingFile.getAbsolutePath().replaceAll(".zip|.jar", "");
                mappingFile = this.unzip(mappingFile, dest);
                this.t1.setText(dest);
                this.doLog("Extracting mappings to \"" + dest + "\"!", Color.BLACK);
            }
            catch (Exception e) {
                this.doLog("Unable to load mappings because " + e.toString(), Color.RED);
            }
        }
        this.deobfusctionMappings = new HashMap();
        this.addMappingsFromFile(mappingFile, 6);
        if (this.deobfusctionMappings.isEmpty()) {
            return false;
        }
        if (!mappingFile.equals(this.actualMapping)) {
            this.actualMapping = mappingFile;
            this.doLog("Mapping successfully loaded from \"" + mappingFile + "\"!", Color.GREEN);
        }
        return true;
    }

    private void addMappingsFromFile(File mappingFile, int dirHops) {
        if (mappingFile.isFile()) {
            this.deobfusctionMappings = this.mappingFromExcel(mappingFile);
        } else {
            File fi = null;
            File me = null;
            File pa = null;
            File[] fileArray = mappingFile.listFiles();
            int n = fileArray.length;
            int n2 = 0;
            while (n2 < n) {
                File f = fileArray[n2];
                if (!f.isDirectory()) {
                    if (f.getPath().endsWith("fields.csv")) {
                        fi = f;
                        this.deobfusctionMappings.putAll(this.mappingFromExcel(fi));
                    } else if (f.getPath().endsWith("methods.csv")) {
                        me = f;
                        this.deobfusctionMappings.putAll(this.mappingFromExcel(me));
                    } else if (f.getPath().endsWith("params.csv")) {
                        pa = f;
                        this.deobfusctionMappings.putAll(this.mappingFromExcel(pa));
                    }
                    if (!this.deobfusctionMappings.isEmpty() && fi != null && me != null && pa != null) {
                        break;
                    }
                } else if (dirHops-- > 0) {
                    this.addMappingsFromFile(f, dirHops);
                }
                ++n2;
            }
        }
    }

    private HashMap<String, String> mappingFromExcel(File csvExcelFile) {
        HashMap<String, String> mapping = new HashMap<String, String>();
        String l = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvExcelFile));
            while ((l = br.readLine()) != null) {
                String[] s = l.split(",");
                if (s.length <= 1 || !s[0].contains("_") && !s[0].startsWith("#")) continue;
                mapping.put(Deobfuscator3000.fastReplace(s[0], "#", ""), s[1]);
            }
            br.close();
        }
        catch (IOException e) {
            this.doLog(String.valueOf(e.toString()) + " while creating mappings from " + csvExcelFile, Color.RED);
        }
        return mapping;
    }

    public File unzip(File zip, String unzipTo) throws IOException {
        File f = new File(unzipTo);
        ZipFile zipFile = new ZipFile(zip);
        zipFile.extractAll(f.getAbsolutePath());
        zipFile.close();
        return f;
    }

    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] fileArray = directory.listFiles();
            int n = fileArray.length;
            int n2 = 0;
            while (n2 < n) {
                File f = fileArray[n2];
                if (f.isDirectory()) {
                    this.deleteDirectory(f);
                } else {
                    f.delete();
                }
                ++n2;
            }
        }
        return directory.delete();
    }

    private void childrensToClipboard(Pane node) {
        String str = "";
        for (Node c : node.getChildren()) {
            str = c instanceof TextInputControl ? String.valueOf(str) + ((TextInputControl)c).getText() + "\n" : String.valueOf(str) + "\n";
        }
        StringSelection s = new StringSelection(str);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
    }

    private void updateProgress(double fraction, double of) {
        Platform.runLater(() -> {
            this.progress.setProgress(fraction / of);
            this.progressText.setText(fraction / of <= 0.0 ? "" : (fraction / of >= 1.0 ? "Finished!" : String.valueOf((int)fraction) + "/" + (int)of + " files processed!"));
        });
    }

    private void doLog(Object s) {
        this.doLog(s, Color.BLACK);
    }

    private void doLog(Object s, Color c) {
        this.doLog(s, c, true);
    }

    private void doLog(Object s, Color c, boolean date) {
        String str = date ? String.valueOf(new SimpleDateFormat("HH:mm:ss | ").format(new Date())) + s : s.toString();
        TextField l = new TextField(str);
        l.setMinWidth(new Text(str).getLayoutBounds().getWidth() + 20.0);
        l.setEditable(false);
        l.setStyle("-fx-text-inner-color: rgba(" + c.getRed() * 255.0 + ", " + c.getGreen() * 255.0 + ", " + c.getBlue() * 255.0 + ", 1);" + "-fx-background-color: transparent;" + "-fx-border-color: transparent;");
        l.setOnMouseClicked(this.log.getOnMouseClicked());
        this.doLog(l);
    }

    private void doLog(Node node) {
        Platform.runLater(() -> {
            TextField tf;
            Node text;
            if (this.log.getChildren().size() > 0 && (text = (Node)this.log.getChildren().get(0)) instanceof TextField && (tf = (TextField)text).getText().indexOf(124) == -1) {
                this.log.getChildren().clear();
            }
            this.log.getChildren().add(node);
        });
    }

    private void doLogEndl() {
        Platform.runLater(() -> {
            Label l = new Label();
            this.log.getChildren().add(l);
        });
    }
}
