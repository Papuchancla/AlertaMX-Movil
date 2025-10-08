package com.example.alertamx;

import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.userLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        this.loadingLiveData = new MutableLiveData<>();

        // Verificar usuario actual al inicializar
        checkCurrentUser();
    }

    // Método de registro (ya existente)
    public void register(String name, String email, String password) {
        loadingLiveData.setValue(true);

        if (!isValidEmail(email)) {
            errorLiveData.setValue("El formato del email no es válido");
            loadingLiveData.setValue(false);
            return;
        }

        if (!isValidPassword(password)) {
            errorLiveData.setValue("La contraseña debe tener al menos 6 caracteres");
            loadingLiveData.setValue(false);
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            saveUserToFirestore(firebaseUser.getUid(), name, email);
                                            userLiveData.setValue(firebaseUser);
                                        } else {
                                            errorLiveData.setValue("Error al actualizar perfil: " +
                                                    profileTask.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        errorLiveData.setValue("Error en registro: " +
                                task.getException().getMessage());
                    }
                });
    }

    // Nuevo método para login
    public void login(String email, String password) {
        loadingLiveData.setValue(true);

        // Validaciones de seguridad
        if (!isValidEmail(email)) {
            errorLiveData.setValue("El formato del email no es válido");
            loadingLiveData.setValue(false);
            return;
        }

        if (!isValidPassword(password)) {
            errorLiveData.setValue("La contraseña debe tener al menos 6 caracteres");
            loadingLiveData.setValue(false);
            return;
        }

        if (isSuspiciousEmail(email)) {
            errorLiveData.setValue("Credenciales incorrectas");
            loadingLiveData.setValue(false);
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            userLiveData.setValue(firebaseUser);
                        }
                    } else {
                        // Mensaje genérico por seguridad
                        errorLiveData.setValue("Credenciales incorrectas. Verifique su email y contraseña");
                    }
                })
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Error de conexión. Intente nuevamente");
                });
    }

    // Método para verificar si el usuario ya está autenticado
    public void checkCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            userLiveData.setValue(currentUser);
        }
    }

    // Método para cerrar sesión
    public void logout() {
        firebaseAuth.signOut();
        userLiveData.setValue(null);
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);

        firestore.collection("users")
                .document(uid)
                .set(userData)
                .addOnFailureListener(e -> {
                    errorLiveData.setValue("Error al guardar usuario: " + e.getMessage());
                });
    }

    // ===== NUEVOS MÉTODOS PARA LA FUNCIONALIDAD DE REPORTES =====

    // Obtener datos del usuario desde Firestore
    public Task<DocumentSnapshot> getUserData(String uid) {
        return firestore.collection("users").document(uid).get();
    }

    // Guardar datos del usuario en Firestore
    public Task<Void> saveUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());

        return firestore.collection("users").document(user.getUid()).set(userData);
    }

    // Obtener usuario actual con datos completos en formato User
    public User getCurrentUserData() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            return new User(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario",
                    firebaseUser.getEmail()
            );
        }
        return null;
    }

    // Verificar si hay un usuario logueado
    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Obtener el usuario actual de Firebase
    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Validaciones de seguridad mejoradas
    private boolean isValidEmail(String email) {
        return email != null &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.length() <= 254; // Longitud máxima estándar para emails
    }

    private boolean isValidPassword(String password) {
        return password != null &&
                password.length() >= 6 &&
                password.length() <= 128; // Longitud máxima razonable
    }

    // Detección de emails sospechosos (protección básica)
    private boolean isSuspiciousEmail(String email) {
        String[] suspiciousPatterns = {
                "..",
                ".@",
                "@.",
                "''",
                "\\\"\\\""
        };

        for (String pattern : suspiciousPatterns) {
            if (email.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public MutableLiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }
}