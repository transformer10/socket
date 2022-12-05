import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Client extends FileTransferControl {
    private String host;
    private int port;

    public Client(String s) {
        String[] str = s.split(":");
        host = str[0];
        port = Integer.parseInt(str[1]);
    }

    public void sendFiles(String[] filenames) {
        Thread thread = new Thread(() -> {
            try {
                Socket socket = new Socket(host, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                Map<String, Double> progress = new HashMap<>();
                progressList.add(progress);
                for (String s : filenames) {
                    progress.put(s, 0.0);
                }
                dos.writeInt(filenames.length);
                dos.flush();
                for (String s : filenames) {
                    dos.writeUTF(s);
                    dos.flush();
                    dos.writeLong(new File(s).length());
                    dos.flush();
                }
                for (String s : filenames) {
                    byte[] buf = new byte[1024];
                    File file = new File(s);
                    FileInputStream fis = new FileInputStream(file);
                    long length = file.length(), currentSize = 0;
                    int readSize = 0;
                    while ((readSize = fis.read(buf)) != -1) {
                        if (mode == Mode.Suspend) { //将当前线程挂起
                            synchronized (this) {
                                this.wait();
                            }
                            mode = Mode.Resume;
                        } else if (mode == Mode.Cancel) {
                            dos.close();
                            fis.close();
                            return;
                        } else {
                            dos.write(buf, 0, readSize);
                            currentSize += readSize;
                            progress.put(s, currentSize * 1.0 / length);
                        }
                    }
                    fis.close();
                    dos.flush();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        threadList.add(thread);
        thread.start();
    }



    public String display() throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("choose the command: ");
        System.out.println("1. transmit files");
        System.out.println("2. query progress");
        System.out.println("3. suspend");
        System.out.println("4. resume");
        System.out.println("5. cancel");
        String op = bf.readLine();
        return op;
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("127.0.0.1:10088");
        while (true) {
            switch (client.display()) {
                case "1": {
                    System.out.println("input transmit file names:");
                    BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
                    String s = bf.readLine();
                    String[] files = s.split(" ");
                    client.sendFiles(files);
                    break;
                }
                case "2": {
                    client.showProgress();
                    break;
                }
                case "3": {
                    client.suspend();
                    break;
                }
                case "4": {
                    client.resume();
                    break;
                }
                case "5": {
                    client.cancel();
                    break;
                }
            }

        }
    }
}
