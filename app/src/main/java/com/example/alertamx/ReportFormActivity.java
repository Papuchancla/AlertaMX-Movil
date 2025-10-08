package com.example.alertamx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportFormActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int STORAGE_PERMISSION_REQUEST = 102;

    private EditText descripcionEditText, ubicacionEditText;
    private AutoCompleteTextView tipoReporteAutoComplete;
    private ImageView imagePreview;
    private Button btnSelectImage, btnTakePhoto, btnSubmit;
    private ProgressBar progressBar;

    private Uri imageUri;
    private String currentPhotoPath;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private AuthViewModel authViewModel;

    private final String[] TIPOS_REPORTE = {
            "Bache en la vía",
            "Robo o asalto",
            "Falta de agua potable",
            "Falta de energía eléctrica",
            "Falta de servicios médicos",
            "Alumbrado público dañado",
            "Recolección de basura",
            "Otro problema comunitario"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_form);

        // Verificar autenticación antes de continuar
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (!authViewModel.isUserLoggedIn()) {
            Toast.makeText(this, "Debes iniciar sesión para enviar reportes", Toast.LENGTH_SHORT).show();
            // Redirigir al login
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
        setupFirebase();
        setupDropdown();
        setupClickListeners();
        setupUserObserver();
    }

    private void initializeViews() {
        tipoReporteAutoComplete = findViewById(R.id.tipoReporteAutoComplete);
        descripcionEditText = findViewById(R.id.descripcionEditText);
        ubicacionEditText = findViewById(R.id.ubicacionEditText);
        imagePreview = findViewById(R.id.imagePreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void setupDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                TIPOS_REPORTE
        );
        tipoReporteAutoComplete.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            }
        });

        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            }
        });

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void setupUserObserver() {
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // Usuario autenticado, podemos mostrar información si es necesario
                String welcomeMessage = "Enviando reporte como: " +
                        (user.getName() != null ? user.getName() : user.getEmail());
                // Puedes mostrar esto en un TextView si quieres
                // Por ejemplo: userInfoTextView.setText(welcomeMessage);
            }
        });
    }

    private void submitReport() {
        // Verificar que el usuario aún esté autenticado
        if (!authViewModel.isUserLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        String tipoReporte = tipoReporteAutoComplete.getText().toString().trim();
        String descripcion = descripcionEditText.getText().toString().trim();
        String ubicacion = ubicacionEditText.getText().toString().trim();

        if (validateForm(tipoReporte, descripcion, ubicacion)) {
            showLoading(true);
            if (imageUri != null) {
                uploadImageAndSaveReport(tipoReporte, descripcion, ubicacion);
            } else {
                saveReportToFirestore(tipoReporte, descripcion, ubicacion, null);
            }
        }
    }

    private boolean validateForm(String tipoReporte, String descripcion, String ubicacion) {
        if (tipoReporte.isEmpty()) {
            Toast.makeText(this, "Selecciona el tipo de reporte", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (descripcion.isEmpty()) {
            Toast.makeText(this, "Ingresa una descripción", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ubicacion.isEmpty()) {
            Toast.makeText(this, "Ingresa la ubicación", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void uploadImageAndSaveReport(String tipoReporte, String descripcion, String ubicacion) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageName = "report_" + timestamp + ".jpg";
        StorageReference imageRef = storageRef.child("report_images/" + imageName);

        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                saveReportToFirestore(tipoReporte, descripcion, ubicacion, imageUrl);
            });
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveReportToFirestore(String tipoReporte, String descripcion, String ubicacion, String imageUrl) {
        User currentUser = authViewModel.getCurrentUserData();

        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("tipoReporte", tipoReporte);
        report.put("descripcion", descripcion);
        report.put("ubicacion", ubicacion);
        report.put("imageUrl", imageUrl);
        report.put("fecha", new Date());
        report.put("estado", "Pendiente");

        // Información del usuario que reporta - CORREGIDO para usar tu estructura User
        report.put("userId", currentUser.getUid());
        report.put("userEmail", currentUser.getEmail());
        report.put("userName", currentUser.getName()); // Cambiado de userDisplayName a userName

        db.collection("reportes")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Toast.makeText(this, "Reporte enviado exitosamente", Toast.LENGTH_SHORT).show();
                    clearForm();

                    // Opcional: Regresar a la actividad anterior o mostrar confirmación
                    // finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error al enviar reporte: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
        btnSelectImage.setEnabled(!show);
        btnTakePhoto.setEnabled(!show);
    }

    private void clearForm() {
        tipoReporteAutoComplete.setText("");
        descripcionEditText.setText("");
        ubicacionEditText.setText("");
        imagePreview.setVisibility(View.GONE);
        imageUri = null;
        currentPhotoPath = null;
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para tomar fotos",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Se necesita permiso de almacenamiento para seleccionar imágenes",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear el archivo", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        "com.example.alertamx.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(View.VISIBLE);
            }
        }
    }
}