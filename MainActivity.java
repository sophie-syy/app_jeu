package com.example.myapplication;

// base
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

// time
import android.os.Handler;
import android.os.Looper;

// aléatoire
import java.util.Random;

// bouton et texte
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

// fenêtre
import android.app.AlertDialog;


public class MainActivity extends AppCompatActivity {
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    private static final String[] NOMS = {"ROUGE", "JAUNE", "BLEU", "ORANGE",
                                          "VERT", "VIOLET", "NOIR", "BLANC"};
    private static final int[] COLORS = {0xFFFF0000, 0xFFFFFF00, 0xFF0000FF, 0xFFFFA500,
                                         0xFF00AA00, 0xFF800080, 0xFF000000, 0xFFFFFFFF};
    private static final int GAME_OVER = 30;

    private Button btnOui, btnNon;
    private ImageButton pauseButton, arrowButton, regleButton;
    private TextView timeDisplay, scoreDisplay, streakDisplay;
    private TextView g1, g2;

    private Runnable timerRunnable; // Contient le code exécuté toutes les secondes
    private int seconds = 0;
    private int score = 0;
    private int streak = 1;
    private int incorrectCount = 0;
    private boolean gameRun = false;
    private boolean paused = false;


    /** Lancer l'application */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Initialise l'activité Android.
        setContentView(R.layout.activity_main); // Charge le fichier XML
        initialize();
        setupClick();
        startGame();
    }

    /** Initialise les références aux éléments de l'interface */
    private void initialize() {
        btnOui = findViewById(R.id.btnOui);
        btnNon = findViewById(R.id.btnNon);
        pauseButton = findViewById(R.id.pause);
        arrowButton = findViewById(R.id.arrow);
        regleButton = findViewById(R.id.regle);
        timeDisplay = findViewById(R.id.time);
        scoreDisplay = findViewById(R.id.scoreG);
        streakDisplay = findViewById(R.id.puissanceG);
        g1 = findViewById(R.id.g1);
        g2 = findViewById(R.id.g2);
    }

    /** Définir les click des boutons */
    private void setupClick() {
        btnOui.setOnClickListener(v -> True_False(true));
        btnNon.setOnClickListener(v -> True_False(false));
        pauseButton.setOnClickListener(v -> togglePause());
        arrowButton.setOnClickListener(v -> restart());
        regleButton.setOnClickListener(v -> showRules());
    }

    /** Saute entre pause et reprendre*/
    private void togglePause() {
        if (gameRun) {
            showPause();
        } else if (paused) {
            startGame();
        }
    }

    /** Afficher la fenêtre pause */
    private void showPause() {
        stopGame();
        new AlertDialog.Builder(this)
                .setTitle("PAUSE")
                .setMessage("Le jeu est en pause")
                .setPositiveButton("Reprendre", (dialog, which) -> startGame())
                .setNegativeButton("Recommencer", (dialog, which) -> restart())
                .setCancelable(false)
                .show();
    }

    /** Affiche la fenêtre des règles du jeu */
    private void showRules() {
        if (gameRun) stopGame();

        new AlertDialog.Builder(this)
                .setTitle("REGLES")
                .setMessage("• Pour avoir bon, il faut que le mot du haut corresponde à la couleur du bas.\n" +
                        "• Appuie sur OUI si c'est correct, NON sinon.\n" +
                        "• Chaque bonne réponse augmente ta \"puissance\" et multiplie les points gagnés (max:5).\n" +
                        "• Le jeu dure 60 secondes.\n\n")
                .setPositiveButton("Fermer", (dialog, which) -> {if (paused) startGame();})
                .setCancelable(false) // interdit de fermer la fenêtre autre qu'appuié sur les boutons
                .show();
    }

    /** Affiche la fenêtre du score final */
    private void showEnd() {
        stopGame();
        new AlertDialog.Builder(this)
                .setTitle("MATCH FIN")
                .setMessage("Fin du jeu ! Score: " + score)
                .setPositiveButton("Rejouer", (dialog, which) -> restart())
                .setCancelable(false)
                .show();
    }

    /** Traite la réponse de l'utilisateur (OUI/NON)
     * answerYes=true si l'utilisateur a cliqué OUI, false sinon */
    private void True_False(boolean answerYes) {
        if (!gameRun) return;

        // Récupère le texte du mot et la couleur affichée
        String texte = g1.getText().toString();
        int couleurAffichee = g2.getCurrentTextColor();

        // Vérifie si la réponse est correcte
        boolean match = compareColor(texte, couleurAffichee);
        boolean isCorrect = (answerYes && match) || (!answerYes && !match);

        // Si 3 erreurs ou plus, force la prochaine réponse à être correcte
        if (incorrectCount >= 3) {
            isCorrect = true;
            incorrectCount = 0;
        } else if (!isCorrect) {
            incorrectCount++;
        }

        // Met à jour le score et la puissance
        if (isCorrect) {
            score += streak;
            if (streak < 5) streak++;
        } else {
            streak = 1;
        }

        // Génère le prochain mot et affiche le feedback
        updateNext();
        showFeedback(isCorrect);
    }

    /** Compare si la couleur du mot correspond à la couleur affichée
     *  motTexte le texte du mot
     *  couleurInt la couleur affichée */
    private boolean compareColor(String motTexte, int couleurInt) {
        for (int i = 0; i < NOMS.length; i++) {
            if (NOMS[i].equals(motTexte)) {
                return COLORS[i] == couleurInt;
            }
        }
        return false;
    }

    /** Génère le prochain mot et met à jour l'interface */
    private void updateNext() {
        RandomWord(g1, g2);
        updateScore();
    }

    /** Affiche un retour visuel (couleur) après la réponse
     *  isCorrect couleur du fond coloré */
    private void showFeedback(boolean isCorrect) {
        // Affiche vert si correct, rouge si incorrect
        int feedbackColor = isCorrect ? 0xFF90EE90 : 0xFFFF6B6B;
        g2.setBackgroundColor(feedbackColor);

        // Initialiser la couleur originale après 300ms
        timerHandler.postDelayed(() -> g2.setBackgroundColor(0xFF6a7192), 300);
    }

    /** Affiche deux mots aléatoires dans la TextView, un avec une couleur aléatoire */
    private void RandomWord(TextView g1, TextView g2) {
        int idx1 = random.nextInt(NOMS.length);
        int idx2 = random.nextInt(NOMS.length);
        int colorIdx = random.nextInt(COLORS.length);

        g1.setText(NOMS[idx1]);
        g2.setText(NOMS[idx2]);
        g2.setTextColor(COLORS[colorIdx]);
    }

    /** Met à jour l'affichage du score et de la puissance */
    private void updateScore() {
        scoreDisplay.setText("Score : " + score);
        streakDisplay.setText("Puissance: x" + streak);
    }

    /** Démarre le jeu et le timer */
    private void startGame() {
        if (gameRun) return;
        gameRun = true;

        // Initialise le jeu s'il n'a pas été pausé
        if (!paused) {
            seconds = 0;
            score = 0;
            streak = 1;
            RandomWord(g1, g2);
        }

        paused = false;
        updateScore();

        // Lance le timer
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                timeDisplay.setText("Temps : " + seconds);

                // Vérifie si le temps est écoulé
                if (seconds >= GAME_OVER) {
                    showEnd();
                } else {
                    // Continue le timer
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    /** Arrête le jeu et met le jeu en pause */
    private void stopGame() {
        gameRun = false;
        paused = true;
        timerHandler.removeCallbacks(timerRunnable); // Supprime le timer
    }

    /** Redémarre le jeu (réinitialise le score et le temps) */
    private void restart() {
        stopGame();
        timeDisplay.setText("Temps : 0");
        seconds = 0;
        score = 0;
        streak = 1;
        paused = false;
        startGame();
    }
}
