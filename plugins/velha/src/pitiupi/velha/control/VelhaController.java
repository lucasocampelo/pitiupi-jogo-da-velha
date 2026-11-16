package pitiupi.velha.control;

import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.velha.net.VelhaMessage;
import pitiupi.velha.net.VelhaMessage.Tipo;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Regras do jogo + protocolo. Chamado sempre na thread do Swing. */
public class VelhaController {

    public enum Estado { LIVRE, AGUARDANDO, CONVITE_RECEBIDO, JOGANDO, FIM }

    private static final int[][] LINHAS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final ConnectionManager cm;
    private Runnable aoMudar = () -> { };

    private final char[] casas = new char[9];
    private int jogadas;
    private Estado estado = Estado.LIVRE;
    private char meuSimbolo;
    private InetAddress adversarioIp;
    private String adversarioNome;
    private String resultado = "";

    public VelhaController(ConnectionManager cm) {
        this.cm = cm;
        Arrays.fill(casas, ' ');
    }

    public void setAoMudar(Runnable r) {
        this.aoMudar = r;
    }

    // ---------- ações do usuário

    public void convidar(InetAddress ip, String nome) {
        if (estado != Estado.LIVRE && estado != Estado.FIM) {
            return;
        }
        adversarioIp = ip;
        adversarioNome = nome;
        meuSimbolo = 'X';
        estado = Estado.AGUARDANDO;
        enviar(Tipo.CONVITE, -1);
        aoMudar.run();
    }

    public void aceitar() {
        if (estado != Estado.CONVITE_RECEBIDO) {
            return;
        }
        meuSimbolo = 'O';
        novaPartida();
        enviar(Tipo.ACEITE, -1);
        aoMudar.run();
    }

    public void recusar() {
        if (estado != Estado.CONVITE_RECEBIDO) {
            return;
        }
        enviar(Tipo.RECUSA, -1);
        estado = Estado.LIVRE;
        aoMudar.run();
    }

    public void jogar(int pos) {
        if (!minhaVez() || casas[pos] != ' ') {
            return;
        }
        marcar(meuSimbolo, pos);
        enviar(Tipo.JOGADA, pos);
        aoMudar.run();
    }

    // ---------- mensagens recebidas

    public void receber(VelhaMessage m) {
        switch (m.tipo) {
            case CONVITE -> {
                if (estado == Estado.LIVRE || estado == Estado.FIM) {
                    adversarioIp = resolverIp(m);
                    adversarioNome = m.nome;
                    estado = Estado.CONVITE_RECEBIDO;
                }
            }
            case ACEITE -> {
                if (estado == Estado.AGUARDANDO) {
                    novaPartida();
                }
            }
            case RECUSA -> {
                if (estado == Estado.AGUARDANDO) {
                    estado = Estado.LIVRE;
                }
            }
            case JOGADA -> {
                char adv = meuSimbolo == 'X' ? 'O' : 'X';
                if (estado == Estado.JOGANDO && vez() == adv
                        && m.posicao >= 0 && m.posicao < 9 && casas[m.posicao] == ' ') {
                    marcar(adv, m.posicao);
                }
            }
        }
        aoMudar.run();
    }

    // ---------- regras

    private void novaPartida() {
        Arrays.fill(casas, ' ');
        jogadas = 0;
        resultado = "";
        estado = Estado.JOGANDO;
    }

    private void marcar(char simbolo, int pos) {
        casas[pos] = simbolo;
        jogadas++;
        for (int[] l : LINHAS) {
            if (casas[l[0]] != ' ' && casas[l[0]] == casas[l[1]] && casas[l[0]] == casas[l[2]]) {
                estado = Estado.FIM;
                resultado = casas[l[0]] == meuSimbolo ? "Você venceu!" : adversarioNome + " venceu.";
                return;
            }
        }
        if (jogadas == 9) {
            estado = Estado.FIM;
            resultado = "Empate.";
        }
    }

    private char vez() {
        return jogadas % 2 == 0 ? 'X' : 'O';
    }

    // ---------- rede

    private void enviar(Tipo tipo, int posicao) {
        VelhaMessage m = new VelhaMessage();
        m.tipo = tipo;
        m.posicao = posicao;
        m.nome = cm.getUserName();
        m.instancia = cm.getIdentifier();
        m.ip = LocalNetworkInfo.bestLocalIpv4();
        InetAddress destino = adversarioIp;
        new Thread(() -> cm.sendTcp(m, destino)).start();
    }

    /** Procura o remetente na lista de presença; se não achar, usa o IP da mensagem. */
    private InetAddress resolverIp(VelhaMessage m) {
        for (UserPeerView p : cm.getPresenceService().listPeerViews()) {
            if (m.instancia != null && m.instancia.equals(p.getInstanceId()) && p.getInetAddress().isPresent()) {
                return p.getInetAddress().get();
            }
        }
        try {
            return InetAddress.getByName(m.ip);
        } catch (Exception e) {
            return null;
        }
    }

    public List<UserPeerView> peers() {
        List<UserPeerView> lista = new ArrayList<>();
        for (UserPeerView p : cm.getPresenceService().listPeerViews()) {
            if (!p.isSelf() && !p.isBroadcastTarget() && p.getInetAddress().isPresent()) {
                lista.add(p);
            }
        }
        return lista;
    }

    // ---------- leitura para a tela

    public Estado getEstado() { return estado; }
    public char casa(int pos) { return casas[pos]; }
    public boolean minhaVez() { return estado == Estado.JOGANDO && vez() == meuSimbolo; }
    public char getMeuSimbolo() { return meuSimbolo; }
    public String getAdversarioNome() { return adversarioNome; }
    public String getResultado() { return resultado; }
}
