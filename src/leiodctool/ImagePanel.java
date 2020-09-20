/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 *
 * @author dell
 */
public class ImagePanel extends JPanel {
    private BufferedImage image;

    public ImagePanel(String filename) {
        URL imgURL = getClass().getResource("/leiodctool/" + filename);
        //System.out.println("URL.getPath() " + imgURL.getPath());

        if (imgURL != null) {
            try {
                image = ImageIO.read(imgURL);
            } catch (IOException ex) {
                // handle exception...
                Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, imgURL.getPath(), ex);
                return;
            }
            //setPreferredSize(new Dimension(136, 512));
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters
    }
}
