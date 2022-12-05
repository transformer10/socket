public interface IFileTransferControl {
    void suspend();

    void resume();

    void cancel();

    void showProgress();
}
