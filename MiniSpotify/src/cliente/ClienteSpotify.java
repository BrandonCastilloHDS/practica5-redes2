package cliente;

import javazoom.jl.player.Player;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

// Cambiamos la clase para que herede de JFrame (la ventana)
public class ClienteSpotify extends JFrame {
    private JTextField txtCancion;
    private JTextArea txtAreaLog;
    private JButton btnReproducir;

    public ClienteSpotify() {
        // 1. Configuración de la ventana principal
        setTitle("Mini Spotify Player");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Panel superior (Entrada de texto y Botón)
        JPanel panelControl = new JPanel(new FlowLayout());
        panelControl.add(new JLabel("Nombre de canción:"));
        txtCancion = new JTextField(20);
        panelControl.add(txtCancion);
        btnReproducir = new JButton("Play");
        panelControl.add(btnReproducir);

        // 3. Panel central (Área de texto para mensajes del sistema)
        txtAreaLog = new JTextArea();
        txtAreaLog.setEditable(false);
        txtAreaLog.setBackground(new Color(30, 30, 30)); // Estilo oscuro
        txtAreaLog.setForeground(Color.GREEN);
        JScrollPane scrollLog = new JScrollPane(txtAreaLog);

        // Agregar paneles a la ventana
        add(panelControl, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);

        // 4. Lógica del botón Play
        btnReproducir.addActionListener(e -> {
            String cancion = txtCancion.getText().trim();
            if (!cancion.isEmpty()) {
                // Ejecutamos en un hilo separado para que la ventana no se trabe al sonar
                new Thread(() -> iniciarStreaming(cancion)).start();
            } else {
                txtAreaLog.append("Por favor, escribe el nombre de una canción.\n");
            }
        });
    }

    private void iniciarStreaming(String cancion) {
        try {
            txtAreaLog.append("\n[Solicitud] Enviando a Router (8000): " + cancion + "\n");

            URL url = new URL("http://localhost:8000/" + cancion);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Permitir que el cliente siga la redirección automáticamente
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                // Mostramos a qué puerto nos mandó el servidor 8000 (8081, 8082, 8083)
                txtAreaLog.append("[Éxito] Redirigido al servidor: " + conn.getURL().getPort() + "\n");
                txtAreaLog.append("[Streaming] Reproduciendo ahora...\n");

                // Streaming directo con JLayer
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Player reproductor = new Player(in); //
                reproductor.play();

            } else {
                txtAreaLog.append("[Error] Código " + status + ": Canción no encontrada.\n");
            }
        } catch (Exception e) {
            txtAreaLog.append("[Error de Conexión] " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        // Iniciar la interfaz visual
        SwingUtilities.invokeLater(() -> {
            new ClienteSpotify().setVisible(true);
        });
    }
}