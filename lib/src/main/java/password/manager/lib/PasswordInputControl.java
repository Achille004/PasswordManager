package password.manager.lib;

public interface PasswordInputControl {
    String getText();
    void setText(String text);
    void setReadable(boolean readable);
    boolean isReadable();
    void requestFocus();
}