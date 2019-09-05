package org.Test.mulVideoDemo;

import org.video.JVideo;
import org.video.videoManager;

import javax.swing.*;
import java.awt.*;

/**
 *  同时播放多个视频
 */
public class mulVideoDemo {
    public static void main(String[] args){
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(new FlowLayout());
        jf.setSize(1200,400);
        JVideo player1 = videoManager.getInstance("Yann Tiersen - J'y Suis Jamais Alle_Amelie [Remix].mp4");
        JVideo player2 = videoManager.getInstance("a.flv");
        player1.setPreferredSize(new Dimension(500,300));
        player2.setPreferredSize(new Dimension(500,300));
        jf.add(player1);
        jf.add(player2);
        jf.setLocationRelativeTo(null);//居中屏幕
        jf.setVisible(true);
        player1.start();
        player2.start();
    }
}
