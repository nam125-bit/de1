import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ChatApp {
    static final int PORT = 5000;
    // TODO: sua thong tin cua ban
    static final String HEADER = "Username: DOCKER_USERNAME | StudentID: MASV | Ho ten: HO TEN SV";
    static final String LOG_FILE = System.getenv().getOrDefault("LOG_FILE", "/app/logs/chat.log");

    static final ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();
    static PrintWriter log;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "server";
        if (mode.equals("server")) runServer(); else runClient();
    }

    // ---------------- SERVER ----------------
    static void runServer() throws Exception {
        File f = new File(LOG_FILE);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        log = new PrintWriter(new FileWriter(f, false), true); // ghi de moi lan chay
        log.println(HEADER);
        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Server listening on " + PORT);
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handle(s)).start();
            }
        }
    }

    static void handle(Socket s) {
        String nick = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true)) {

            out.println("Nhap nickname (vd: MaSV_1):");
            while (true) {
                String n = in.readLine();
                if (n == null) return;
                n = n.trim();
                if (n.isEmpty()) { out.println("Nickname khong duoc rong, nhap lai:"); continue; }
                if (clients.putIfAbsent(n, out) == null) { nick = n; break; } // atomic -> khong trung
                out.println("Nickname da ton tai, nhap lai:");
            }
            out.println("Dang nhap thanh cong. Bat dau chat!");
            broadcast("[" + nick + "][" + now() + "]: da tham gia phong chat");

            String msg;
            while ((msg = in.readLine()) != null) {
                broadcast("[" + nick + "][" + now() + "]: " + msg);
            }
        } catch (IOException ignored) {
        } finally {
            if (nick != null) {
                clients.remove(nick);
                broadcast("[" + nick + "][" + now() + "]: da roi phong chat");
            }
            try { s.close(); } catch (IOException ignored) {}
        }
    }

    static synchronized void broadcast(String line) {
        System.out.println(line);
        log.println(line);
        for (PrintWriter w : clients.values()) w.println(line);
    }

    static String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // ---------------- CLIENT ----------------
    static void runClient() throws Exception {
        String host = System.getenv().getOrDefault("SERVER_HOST", "server");
        Socket s = new Socket(host, PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);

        Thread reader = new Thread(() -> {
            try {
                String l;
                while ((l = in.readLine()) != null) System.out.println(l);
            } catch (IOException ignored) {}
            System.out.println("Mat ket noi server.");
            System.exit(0);
        });
        reader.setDaemon(true);
        reader.start();

        BufferedReader kb = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String l;
        while ((l = kb.readLine()) != null) out.println(l);
    }
}
