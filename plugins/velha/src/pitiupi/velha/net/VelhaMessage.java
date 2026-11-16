package pitiupi.velha.net;

import pitiupi.models.Messages.Message;

public class VelhaMessage extends Message {

    private static final long serialVersionUID = 1L;

    public enum Tipo { CONVITE, ACEITE, RECUSA, JOGADA }

    public Tipo tipo;
    public String nome;       // nome de quem enviou
    public String instancia;  // id da instância do pitiupi de quem enviou
    public String ip;         // IPv4 de quem enviou
    public int posicao = -1;  // JOGADA: casa 0..8

    public VelhaMessage() {
    }
}
