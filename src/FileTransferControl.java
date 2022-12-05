import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract public class FileTransferControl implements IFileTransferControl {
    protected List<Map<String, Double>> progressList;
    protected List<Thread> threadList;
    protected Mode mode;

    public FileTransferControl() {
        progressList = new ArrayList<>();
        threadList = new ArrayList<>();
        mode = Mode.Resume;
    }

    @Override
    public void suspend() {
        mode = Mode.Suspend;
    }

    @Override
    public void resume() {
        synchronized (this){
            notifyAll();
        }
    }

    @Override
    public void cancel() {
        mode = Mode.Cancel;
    }

    @Override
    public void showProgress() {
        for (int i = 0; i < progressList.size(); ++i) {
            System.out.printf("Task %d\n", i + 1);
            Map<String, Double> map = progressList.get(i);
            for (String s : map.keySet()) {
                System.out.printf("%s: %.2f%%\n", s, map.get(s) * 100);
            }
        }
    }
}
