import java.io.*;
import java.net.*;
import com.google.gson.*;
import java.util.*;

public class ClienteVotante {
    private static final String HOST = "localhost";
    private static final int PORTA_TCP = 5000;
    private static final int PORTA_UDP = 6000;
    private static final String GRUPO_MULTICAST = "230.0.0.1";

    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        Scanner sc = new Scanner(System.in);

        // Login
        System.out.print("Usuário: ");
        String usuario = sc.nextLine();
        System.out.print("Senha: ");
        String senha = sc.nextLine();

        Socket socket = new Socket(HOST, PORTA_TCP);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Mensagem login = new Mensagem();
        login.tipo = "login";
        login.usuario = usuario;
        login.senha = senha;
        out.println(gson.toJson(login));

        // Inicia thread para ouvir mensagens multicast
        new Thread(() -> {
            try (MulticastSocket ms = new MulticastSocket(PORTA_UDP)) {
                InetAddress grupo = InetAddress.getByName(GRUPO_MULTICAST);
                ms.joinGroup(grupo);
                byte[] buf = new byte[256];
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    ms.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength());
                    System.out.println("\n[MULTICAST] " + msg);
                }
            } catch (IOException e) { }
        }).start();

        // Recebe lista de candidatos
        String resposta = in.readLine();
        Mensagem listaMsg = gson.fromJson(resposta, Mensagem.class);
        Candidato[] lista = gson.fromJson(listaMsg.conteudo, Candidato[].class);

        System.out.println("Candidatos disponíveis:");
        for (Candidato c : lista) System.out.println("- " + c.nome);

        // Envia voto
        System.out.print("Digite o nome do candidato: ");
        String nome = sc.nextLine();

        Mensagem voto = new Mensagem();
        voto.tipo = "voto";
        voto.candidato = nome;
        out.println(gson.toJson(voto));

        System.out.println("Servidor: " + in.readLine());
    }
}
