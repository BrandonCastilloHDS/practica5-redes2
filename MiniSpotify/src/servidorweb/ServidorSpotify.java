package servidorweb;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorSpotify {
    private int puerto;
    private int limiteHilos;
    private ThreadPoolExecutor pool;

    public static final String CARPETA_WEB = "MiniSpotify/www";

    public ServidorSpotify(int puerto, int limiteHilos) {
        this.puerto = puerto;
        this.limiteHilos = limiteHilos;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(limiteHilos);

        File directory = new File(CARPETA_WEB);
        if (!directory.exists()) directory.mkdir();
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("-> Nodo [" + puerto + "] listo y escuchando.");
            while (true) {
                Socket cliente = serverSocket.accept();
                pool.execute(new ManejadorSpotify(cliente, puerto));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ManejadorSpotify implements Runnable {
        private Socket socket; //conexión con el cliente
        private int puertoServidor;

        public ManejadorSpotify(Socket socket, int puerto) {
            this.socket = socket;
            this.puertoServidor = puerto;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

                String requestLine = reader.readLine();
                if (requestLine == null) return;

                StringTokenizer tokenizer = new StringTokenizer(requestLine); //extraemos la ruta
                tokenizer.nextToken(); // Salta GET
                String path = tokenizer.nextToken().substring(1).toLowerCase(); // Obtiene la ruta

                if (path.isEmpty()) path = "index.html";


                if (path.startsWith("lista")) {
                    enviarListaCanciones(out);
                    return;
                }


                if (puertoServidor == 8000 && !path.equals("index.html") && !path.startsWith("favicon")) {
                    if (path.contains("rock")) {
                        // Redirigimos la VENTANA (index.html) al puerto 8081 con el parámetro play
                        redirigir(out, "http://localhost:8081/index.html?play=" + path);
                        return;
                    } else if (path.contains("pop")) {
                        redirigir(out, "http://localhost:8082/index.html?play=" + path);
                        return;
                    } else if (path.contains("metal")) {
                        redirigir(out, "http://localhost:8083/index.html?play=" + path);
                        return;
                    }
                }

                // Si no es redirección, entregamos el archivo (html o mp3) localmente
                enviarArchivo(path, out);

            } catch (Exception e) {
                // Silenciamos errores de conexión interrumpida (común en navegadores)
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void enviarListaCanciones(OutputStream out) throws IOException {
            File carpeta = new File(CARPETA_WEB);
            File[] archivos = carpeta.listFiles();
            StringBuilder lista = new StringBuilder();

            if (archivos != null) {
                for (File f : archivos) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".mp3")) {
                        lista.append(f.getName()).append("\n");
                    }
                }
            }
            byte[] bytes = lista.toString().getBytes();
            String header = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n" +
                    "Access-Control-Allow-Origin: *\r\nContent-Length: " + bytes.length + "\r\n\r\n";
            out.write(header.getBytes());
            out.write(bytes);
            out.flush();
        }

        private void redirigir(OutputStream out, String url) throws IOException {
            // 302 Found mueve al navegador a la nueva URL
            String res = "HTTP/1.1 302 Found\r\nLocation: " + url + "\r\nConnection: close\r\n\r\n";
            out.write(res.getBytes());
            out.flush();
        }

        private void enviarArchivo(String fileName, OutputStream out) throws IOException {
            // Limpiamos parámetros de URL si vienen (ej: index.html?play=...)
            if (fileName.contains("?")) fileName = fileName.split("\\?")[0];

            File file = new File(CARPETA_WEB + File.separator + fileName);
            if (file.exists()) {
                String mime = "audio/mpeg";
                if (fileName.endsWith(".html")) mime = "text/html";
                if (fileName.endsWith(".css")) mime = "text/css";
                if (fileName.endsWith(".js")) mime = "application/javascript";

                String header = "HTTP/1.1 200 OK\r\nContent-Type: " + mime + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\nContent-Length: " + file.length() + "\r\n\r\n";
                out.write(header.getBytes());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int leidos;
                    while ((leidos = fis.read(buffer)) != -1) out.write(buffer, 0, leidos);
                }
                out.flush();
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n<h1>404</h1>".getBytes());
            }
        }
    }

    public static void main(String[] args) {
        int hilos = 10;
        // Levantamos los hilos
        new Thread(() -> new ServidorSpotify(8000, hilos).iniciar()).start(); // Router
        new Thread(() -> new ServidorSpotify(8081, hilos).iniciar()).start(); // cancion 1
        new Thread(() -> new ServidorSpotify(8082, hilos).iniciar()).start(); // cancion 2
        new Thread(() -> new ServidorSpotify(8083, hilos).iniciar()).start(); // cancion 3
        System.out.println("Iniciando MiniSpotify");
    }
}