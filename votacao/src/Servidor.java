import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;

public class Servidor {
    private static final int PORTA_TCP = 5000;
    private static final int PORTA_UDP = 6000;
    private static final String GRUPO_MULTICAST = "230.0.0.1";
    private static final int TEMPO_VOTACAO = 30000; // 30s

    private static boolean votacaoAtiva = true;
    private static Map<String, String> usuarios = new ConcurrentHashMap<>();
    private static List<Candidato> candidatos = new CopyOnWriteArrayList<>();
    private static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        // Usuários de teste
        usuarios.put("joao", "123");
        usuarios.put("maria", "123");
        usuarios.put("admin", "admin");

        // Candidatos iniciais
        candidatos.add(new Candidato("Alice"));
        candidatos.add(new Candidato("Bruno"));

        // Inicia servidor TCP
        ServerSocket servidor = new ServerSocket(PORTA_TCP);
        System.out.println("Servidor iniciado na porta " + PORTA_TCP);

        // Thread para encerrar votação
        new Thread(() -> {
            try {
                Thread.sleep(TEMPO_VOTACAO);
                votacaoAtiva = false;
                System.out.println("Tempo de votação encerrado.");
            } catch (InterruptedException e) { }
        }).start();

        // Aceita clientes
        while (true) {
            Socket cliente = servidor.accept();
            new Thread(new ClienteHandler(cliente)).start();
        }
    }

    // Classe interna para tratar conexões
    static class ClienteHandler implements Runnable {
        private Socket socket;

        ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String recebido = in.readLine();
                Mensagem msg = gson.fromJson(recebido, Mensagem.class);

                if (!usuarios.containsKey(msg.usuario) ||
                    !usuarios.get(msg.usuario).equals(msg.senha)) {
                    out.println("Login inválido");
                    return;
                }

                if (msg.usuario.equals("admin")) {
                    tratarAdmin(in, out);
                } else {
                    tratarVotante(in, out);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void tratarVotante(BufferedReader in, PrintWriter out) throws IOException {
            // Envia lista de candidatos
            Mensagem listaMsg = new Mensagem();
            listaMsg.tipo = "lista";
            listaMsg.conteudo = gson.toJson(candidatos);
            out.println(gson.toJson(listaMsg));

            // Recebe voto
            String votoJson = in.readLine();
            Mensagem votoMsg = gson.fromJson(votoJson, Mensagem.class);

            if (!votacaoAtiva) {
                out.println("Votação encerrada.");
                return;
            }

            for (Candidato c : candidatos) {
                if (c.nome.equalsIgnoreCase(votoMsg.candidato)) {
                    c.adicionarVoto();
                }
            }

            out.println("Voto registrado com sucesso!");

            // Envia resultado se votação acabou
            if (!votacaoAtiva) {
                enviarResultado(out);
            }
        }

        private void tratarAdmin(BufferedReader in, PrintWriter out) throws IOException {
            out.println("Admin conectado. Digite comandos (ADD, DEL, MSG):");
            while (true) {
                String comando = in.readLine();
                if (comando == null) break;

                if (comando.startsWith("ADD")) {
                    String nome = comando.substring(4);
                    candidatos.add(new Candidato(nome));
                    out.println("Candidato " + nome + " adicionado.");
                } else if (comando.startsWith("DEL")) {
                    String nome = comando.substring(4);
                    candidatos.removeIf(c -> c.nome.equalsIgnoreCase(nome));
                    out.println("Candidato " + nome + " removido.");
                } else if (comando.startsWith("MSG")) {
                    String texto = comando.substring(4);
                    enviarMulticast(texto);
                    out.println("Mensagem enviada via multicast.");
                }
            }
        }

        private void enviarResultado(PrintWriter out) {
            int total = candidatos.stream().mapToInt(c -> c.votos).sum();
            Candidato vencedor = candidatos.stream()
                    .max(Comparator.comparingInt(c -> c.votos))
                    .orElse(null);

            StringBuilder sb = new StringBuilder();
            for (Candidato c : candidatos) {
                double perc = total > 0 ? (c.votos * 100.0 / total) : 0;
                sb.append(c.nome).append(": ").append(c.votos)
                        .append(" votos (").append(String.format("%.1f", perc)).append("%)\n");
            }
            sb.append("Vencedor: ").append(vencedor != null ? vencedor.nome : "Nenhum");

            out.println(sb.toString());
        }

        private void enviarMulticast(String texto) {
            try {
                InetAddress grupo = InetAddress.getByName(GRUPO_MULTICAST);
                DatagramSocket socketUDP = new DatagramSocket();
                byte[] dados = texto.getBytes();
                DatagramPacket pacote = new DatagramPacket(dados, dados.length, grupo, PORTA_UDP);
                socketUDP.send(pacote);
                socketUDP.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
