package org.example;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main implements Watcher{
    private static final String ZNODE_PATH = "/a";
    private static ZooKeeper zk;

    private static String externalApp;
    private static Process externalProcess = null;

    public static void main(String[] args) throws Exception {
        String zkHostPort = args[0];
        externalApp = args[1];

        CountDownLatch connectedSignal = new CountDownLatch(1);
        zk = new ZooKeeper(zkHostPort, 3000, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();

        new Main().watchZnode();
        Thread.sleep(Long.MAX_VALUE);
    }


    public void watchZnode() throws KeeperException, InterruptedException, IOException {
        Stat stat = zk.exists(ZNODE_PATH, this);
        if (stat != null) {
            handleZnodeCreated();
            watchChildren();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            String path = event.getPath();

            switch (event.getType()) {
                case NodeCreated:
                    if (ZNODE_PATH.equals(path)) {
                        handleZnodeCreated();
                        zk.exists(ZNODE_PATH, this);
                        watchChildren();
                    }
                    break;

                case NodeDeleted:
                    if (ZNODE_PATH.equals(path)) {
                        handleZnodeDeleted();
                        zk.exists(ZNODE_PATH, this);
                    }
                    break;

                case NodeChildrenChanged:
                    if (ZNODE_PATH.equals(path)) {
                        showChildCount();
                        printTree(ZNODE_PATH, "");
                        watchChildren();
                    }
                    break;

                default:
                    zk.exists(ZNODE_PATH, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleZnodeCreated() throws IOException {
        System.out.println("Znode 'a' utworzony. Uruchamiam aplikację: " + externalApp);
        externalProcess = Runtime.getRuntime().exec(externalApp);
    }

    private void handleZnodeDeleted() {
        System.out.println("Znode 'a' usunięty. Zatrzymuję aplikację.");
        if (externalProcess != null) {
            externalProcess.destroy();
            externalProcess = null;
        }
    }

    private void watchChildren() throws KeeperException, InterruptedException {
        zk.getChildren(ZNODE_PATH, this);
    }

    private void showChildCount() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(ZNODE_PATH, false);
        int count = children.size();

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    "Aktualna liczba potomków znode '/a': " + count,
                    "Informacja o potomkach",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public static void printTree(String path, String indent) throws KeeperException, InterruptedException {
        StringBuilder treeBuilder = new StringBuilder();
        buildTreeString(path, indent, treeBuilder);
        System.out.println(treeBuilder.toString());
    }

    private static void buildTreeString(String path, String indent, StringBuilder treeBuilder) throws KeeperException, InterruptedException {
        Stat stat = zk.exists(path, null);
        if (stat != null) {
            treeBuilder.append(indent).append(path).append(System.lineSeparator());
            List<String> children = zk.getChildren(path, false);
            for (String child : children) {
                buildTreeString(path + "/" + child, indent + "  ", treeBuilder);
            }
        }
    }
}