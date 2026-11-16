package pitiupi.velha.gui;

import pitiupi.application.presence.UserPeerView;
import pitiupi.velha.control.VelhaController;

import javax.swing.*;
import java.awt.*;

public class VelhaPanel extends JPanel {

    private final VelhaController controller;
    private final JComboBox<UserPeerViewItem> peers = new JComboBox<>();
    private final JButton[] casas = new JButton[9];
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel convite = new JPanel();
    private final JButton convidar = new JButton("Convidar");

    public VelhaPanel(VelhaController controller) {
        super(new BorderLayout(8, 8));
        this.controller = controller;

        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> atualizarPeers());
        convidar.addActionListener(e -> {
            UserPeerViewItem item = (UserPeerViewItem) peers.getSelectedItem();
            if (item != null) {
                controller.convidar(item.peer.getInetAddress().get(), item.peer.getDisplayName());
            }
        });
        JPanel topo = new JPanel();
        topo.add(new JLabel("Adversário:"));
        topo.add(peers);
        topo.add(atualizar);
        topo.add(convidar);

        JButton aceitar = new JButton("Aceitar");
        JButton recusar = new JButton("Recusar");
        aceitar.addActionListener(e -> controller.aceitar());
        recusar.addActionListener(e -> controller.recusar());
        convite.add(aceitar);
        convite.add(recusar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(topo, BorderLayout.NORTH);
        norte.add(convite, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        JPanel grade = new JPanel(new GridLayout(3, 3, 4, 4));
        grade.setPreferredSize(new Dimension(300, 300));
        for (int i = 0; i < 9; i++) {
            int pos = i;
            casas[i] = new JButton();
            casas[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
            casas[i].addActionListener(e -> controller.jogar(pos));
            grade.add(casas[i]);
        }
        JPanel centro = new JPanel(new GridBagLayout());
        centro.add(grade);
        add(centro, BorderLayout.CENTER);

        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        add(status, BorderLayout.SOUTH);

        controller.setAoMudar(this::atualizar);
        atualizarPeers();
        atualizar();
    }

    public void atualizarPeers() {
        peers.removeAllItems();
        for (UserPeerView p : controller.peers()) {
            peers.addItem(new UserPeerViewItem(p));
        }
    }

    private void atualizar() {
        for (int i = 0; i < 9; i++) {
            char c = controller.casa(i);
            casas[i].setText(c == ' ' ? "" : String.valueOf(c));
            casas[i].setEnabled(controller.minhaVez() && c == ' ');
        }
        String adv = controller.getAdversarioNome();
        status.setText(switch (controller.getEstado()) {
            case LIVRE -> "Escolha um adversário e clique em Convidar.";
            case AGUARDANDO -> "Aguardando " + adv + "...";
            case CONVITE_RECEBIDO -> adv + " te convidou para jogar.";
            case JOGANDO -> "Você é " + controller.getMeuSimbolo() + " — "
                    + (controller.minhaVez() ? "sua vez" : "vez de " + adv);
            case FIM -> controller.getResultado();
        });
        VelhaController.Estado e = controller.getEstado();
        convite.setVisible(e == VelhaController.Estado.CONVITE_RECEBIDO);
        convidar.setEnabled(e == VelhaController.Estado.LIVRE || e == VelhaController.Estado.FIM);
        revalidate();
        repaint();
    }

    private record UserPeerViewItem(UserPeerView peer) {
        @Override
        public String toString() {
            return peer.getDisplayName() + " (" + peer.getInetAddress().get().getHostAddress() + ")";
        }
    }
}
