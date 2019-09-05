package org.video;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import javax.swing.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 音频数据处理和写入
 */
public class audioManager {
    public int sampleFormat,audioSampleRate,audioChannels;
    public float vol = 1.0f;
    public SourceDataLine sourceDataLine;

    private AudioFormat af;
    private DataLine.Info dataLineInfo;

    public audioManager(int sampleFormat, int audioSampleRate, int audioChannels){
        this.sampleFormat = sampleFormat;
        this.audioSampleRate = audioSampleRate;
        this.audioChannels = audioChannels;
        initAudio();
    }

    public void start(){
        try {
            sourceDataLine.open(af);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        sourceDataLine.flush();
        sourceDataLine.stop();
        sourceDataLine.close();
    }

    public void restart() {
        sourceDataLine.stop();
        sourceDataLine.close();
        getAudioFormat();
        try {
            sourceDataLine.open(af);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private Buffer[] buf;
    private FloatBuffer leftDataF, rightDataF;
    private ShortBuffer leftDataS, rightDataS;
    private ByteBuffer leftDataB, rightDataB;
    private byte[] leftDataT, rightDataT;
    private byte[] combineDataT;
    public void processAudioFrame(Frame audioFrame) {
        int k;
        buf = audioFrame.samples;
        switch(sampleFormat){
            case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开。
                leftDataF = (FloatBuffer)buf[0];
                leftDataB = util.floatToByteValue(leftDataF,vol);
                rightDataF = (FloatBuffer)buf[1];
                rightDataB = util.floatToByteValue(rightDataF,vol);
                leftDataT = leftDataB.array();
                rightDataT = rightDataB.array();
                combineDataT = new byte[leftDataT.length+ rightDataT.length];
                k = 0;
                for(int i = 0; i< leftDataT.length; i=i+2) {//混合两个声道。
                    for (int j = 0; j < 2; j++) {
                        combineDataT[j+4*k] = leftDataT[i + j];
                        combineDataT[j + 2+4*k] = rightDataT[i + j];
                    }
                    k++;
                }
                break;
            case avutil.AV_SAMPLE_FMT_S16://非平面型左右声道在一个buffer中。
                leftDataS = (ShortBuffer)buf[0];
                leftDataB = util.shortToByteValue(leftDataS,vol);
                combineDataT = leftDataB.array();
                break;
            case avutil.AV_SAMPLE_FMT_FLT://float非平面型
                leftDataF = (FloatBuffer)buf[0];
                leftDataB = util.floatToByteValue(leftDataF,vol);
                combineDataT = leftDataB.array();
                break;
            case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
                leftDataS = (ShortBuffer)buf[0];
                rightDataS = (ShortBuffer)buf[1];
                leftDataB = util.shortToByteValue(leftDataS,vol);
                rightDataB = util.shortToByteValue(rightDataS,vol);
                leftDataT = leftDataB.array();
                rightDataT = rightDataB.array();
                combineDataT = new byte[leftDataT.length+ rightDataT.length];
                k = 0;
                for(int i = 0; i< leftDataT.length; i=i+2) {
                    for (int j = 0; j < 2; j++) {
                        combineDataT[j+4*k] = leftDataT[i + j];
                        combineDataT[j + 2+4*k] = rightDataT[i + j];
                    }
                    k++;
                }
                break;
            default:
                JOptionPane.showMessageDialog(null,"不支持的音频格式","不支持的音频格式",JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                break;
        }
    }

    public void processAudioFrame(Frame audioFrame, audioFrame frame){
        processAudioFrame(audioFrame);
        frame.combine = combineDataT.clone();
    }

    /**
     * 传入null就用对象的combine写，否则用传入的curf的combine写
     */
    public void writeData(audioFrame curf) {
        switch(sampleFormat){
            case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开。
            case avutil.AV_SAMPLE_FMT_S16://非平面型左右声道在一个buffer中。
            case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
            case avutil.AV_SAMPLE_FMT_FLT://float非平面型
                if(curf!=null)
                    sourceDataLine.write(curf.combine,0,curf.combine.length);
                else
                    sourceDataLine.write(combineDataT,0, combineDataT.length);
                break;
            default:
                JOptionPane.showMessageDialog(null,"不支持的音频格式","不支持的音频格式",JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private void initAudio(){
        try {
            getAudioFormat();
            dataLineInfo = new DataLine.Info(SourceDataLine.class,
                    af, AudioSystem.NOT_SPECIFIED);
            sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(af);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    //这里只挑了几个常见的音频格式处理
    private void getAudioFormat() {
        switch (sampleFormat) {
            case avutil.AV_SAMPLE_FMT_U8://无符号short 8bit
                 p("未处理U8类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_S16://有符号short 16bit
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 16, audioChannels, audioChannels * 2, audioSampleRate, true);
                break;
            case avutil.AV_SAMPLE_FMT_S32:
                 p("未处理S32类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_FLT:
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 16, audioChannels, audioChannels * 2, audioSampleRate, true);
                break;
            case avutil.AV_SAMPLE_FMT_DBL:
                p("未处理DBL类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_U8P:
                p("未处理U8P类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_S16P://有符号short 16bit,平面型
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 16, audioChannels, audioChannels * 2, audioSampleRate, true);
                break;
            case avutil.AV_SAMPLE_FMT_S32P://有符号short 32bit，平面型
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 32, audioChannels, audioChannels * 2, audioSampleRate, true);
                break;
            case avutil.AV_SAMPLE_FMT_FLTP://float 平面型 需转为16bit short
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 16, audioChannels, audioChannels * 2, audioSampleRate, true);
                break;
            case avutil.AV_SAMPLE_FMT_DBLP:
                p("未处理DBLP类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_S64://有符号short 64bit 非平面型
                p("未处理S64类型采样格式");
                break;
            case avutil.AV_SAMPLE_FMT_S64P://有符号short 64bit平面型
                p("未处理S64P类型采样格式");
                break;
            default:
                    p("不支持的音频格式");
                 break;
        }
    }
    private void p(String s){
        JOptionPane.showMessageDialog(null, s,s, JOptionPane.ERROR_MESSAGE);
    }
}
