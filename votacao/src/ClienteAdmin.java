import java.io.*;
import java.net.*;
import com.google.gson.*;
import java.util.*;

public class ClienteAdmin {
    private static final String HOST = "localhost";
    private static final int PORTA_TCP = 5000;

    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        Socket socket = new Socket(HOST, PORTA_TCP);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner sc = new Scanner(System.in);

        // Login admin
        Mensagem login = new Mensagem();
        login.tipo = "login";
        login.usuario = "admin";
        login.senha = "admin";
        out.println(gson.toJson(login));

        System.out.println(in.readLine());

        while (true) {
            System.out.print("> ");
            String cmd = sc.nextLine();
            out.println(cmd);
            System.out.println(in.readLine());
        }
    }
}
