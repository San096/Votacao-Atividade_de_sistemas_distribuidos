public class Candidato {
    String nome;
    int votos;

    public Candidato(String nome) {
        this.nome = nome;
        this.votos = 0;
    }

    public synchronized void adicionarVoto() {
        votos++;
    }
}
