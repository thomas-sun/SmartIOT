package tw.ironthomas.smartiot;


public interface OTAEvent {
    void on_ota_notification(int hr);
    void on_ota_finish(boolean bSuccess);
    void on_ota_start();
}
