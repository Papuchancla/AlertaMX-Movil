package com.example.alertamx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;

public class registrer extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrer);

        // Inicializar ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Vincular vistas
        editTextName = findViewById(R.id.editTextText);
        editTextEmail = findViewById(R.id.editTextTextEmailAddress2);
        editTextPassword = findViewById(R.id.editTextTextPassword);
        buttonRegister = findViewById(R.id.button5);
        textViewLogin = findViewById(R.id.textViewRegistrar);

        // Configurar observadores
        setupObservers();

        // Configurar listeners
        setupListeners();
    }

    private void setupObservers() {
        // Observar éxito en registro
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(registrer.this, "Registro exitoso!", Toast.LENGTH_SHORT).show();
                // Redirigir a la actividad principal
                redirectToMainActivity();
            }
        });

        // Observar errores
        authViewModel.getErrorLiveData().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(registrer.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        // Listener del botón de registro
        buttonRegister.setOnClickListener(v -> {
            attemptRegistration();
        });

        // Listener para ir al login
        textViewLogin.setOnClickListener(v -> {
            // Redirigir a la actividad de login
            Intent intent = new Intent(registrer.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegistration() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones locales adicionales
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("El nombre es requerido");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("El correo es requerido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("La contraseña es requerida");
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        // Mostrar progreso (puedes agregar un ProgressBar)
        buttonRegister.setEnabled(false);

        // Llamar al ViewModel para registrar
        authViewModel.register(name, email, password);
    }

    private void redirectToMainActivity() {
        // Cambia MainActivity por tu actividad principal
        Intent intent = new Intent(registrer.this, ReportFormActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}