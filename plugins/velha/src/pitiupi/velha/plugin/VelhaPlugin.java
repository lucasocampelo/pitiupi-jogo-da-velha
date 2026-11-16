package pitiupi.velha.plugin;

import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.velha.control.VelhaController;
import pitiupi.velha.gui.VelhaPanel;
import pitiupi.velha.net.VelhaMessage;

import javax.swing.SwingUtilities;
import java.util.List;

public class VelhaPlugin extends Plugin {

    private VelhaController controller;

    @Override
    public String getName() {
        return "Jogo da Velha";
    }

    @Override
    public String getAuthor() {
        return "Lucas";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getIdentifier() {
        return "pitiupi.velha";
    }

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        controller = new VelhaController(connectionManager);
        bindShellWorkspaceContent(new VelhaPanel(controller));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(VelhaMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof VelhaMessage m) {
            SwingUtilities.invokeLater(() -> controller.receber(m));
        }
    }
}
