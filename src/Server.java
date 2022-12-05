import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends FileTransferControl {
    private int port;

    private void connect() {
        new Thread(() -> {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(port);
                while (true) {
                    Socket socket = ss.accept();
                    Thread thread = new Thread(()->{
                       receive(socket);
                    });
                    threadList.add(thread);
                    thread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void receive(Socket socket) {
        Thread thread = new Thread(() -> {
            DataInputStream dis = null;
            Map<String, Double> progress = new HashMap<>();
            progressList.add(progress);
            try {
                dis = new DataInputStream(socket.getInputStream());
                int totalFiles = dis.readInt();
                List<String> fileNames = new ArrayList<>();
                List<Long> fileLengths = new ArrayList<>();
                for (int i = 0; i < totalFiles; ++i) {
                    String path = dis.readUTF(); // static/data.txt
                    long length = dis.readLong();
                    String[] _path = path.split("/"); // [static, data.txt]
                    String filename = _path[_path.length - 1];
                    fileNames.add(filename);
                    fileLengths.add(length);
                }
                for (String s : fileNames) {
                    progress.put(s, 0.0);
                }

                byte[] buf = new byte[1024];
                int readSize = 0, i = 0;
                long currentSize = 0;
                File file = new File(fileNames.get(i));
                FileOutputStream fos = new FileOutputStream(file);
                while ((readSize = dis.read(buf)) != -1) {
                    if (mode == Mode.Suspend) { //将当前线程挂起
                        synchronized (this) {
                            this.wait();
                        }
                        mode = Mode.Resume;
                    } else if (currentSize + readSize <= fileLengths.get(i)) {
                        fos.write(buf, 0, readSize);
                        currentSize += readSize;
                        progress.put(fileNames.get(i), currentSize * 1.0 / fileLengths.get(i));
                    } else {
                        int size = (int) (fileLengths.get(i) - currentSize);
                        progress.put(fileNames.get(i), 100.0);
                        fos.write(buf, 0, size);
                        fos.close();
                        i += 1;
                        if (i < totalFiles) {
                            file = new File(fileNames.get(i));
                            fos = new FileOutputStream(file);
                            currentSize = readSize - size;
                            fos.write(buf, size, (int) currentSize);
                            progress.put(fileNames.get(i), currentSize * 1.0 / fileLengths.get(i));
                        }
                    }
                }
                if (i < totalFiles && file.length() < fileLengths.get(i)) {
                    fos.close();
                    file.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
        threadList.add(thread);
        thread.start();
    }

    public Server(int port) {
        this.port = port;
        connect();
    }

    public String display() throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("choose the command: ");
        System.out.println("1. query progress");
        System.out.println("2. suspend");
        System.out.println("3. resume");
        String op = bf.readLine();
        return op;
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(10088);
        while (true) {
            switch (server.display()) {
                case "1": {
                    server.showProgress();
                    break;
                }
                case "2": {
                    server.suspend();
                    break;
                }
                case "3": {
                    server.resume();
                    break;
                }
            }
        }
    }
}
