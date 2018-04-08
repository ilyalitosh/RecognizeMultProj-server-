import javax.swing.*;
import java.awt.*;

public class CamPreview extends JFrame {

    private static Graphics gPreview;

    public Graphics getGCameraPreview() {

        return gPreview;
    }

    public CamPreview(){
        showCamPreview();
    }

    public void showCamPreview(){
        setSize(950, 350);
        setTitle("CamPreview");
        setLayout(null);
        setLocationRelativeTo(null);
        setVisible(true);

        initGraphics();
    }

    private void initGraphics(){
        gPreview = getGraphics();
    }

}
