package com.hamal.egg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class MJPEGGenerator {
    int width = 0;
    int height = 0;
    int frameCount = 0;
    double framerate = 0;
    File aviFile = null;
    FileOutputStream aviOutput = null;
    FileChannel aviChannel = null;
    final int dummy_int = 'Z' + ('Z'<<8) + ('Z'<<16) + ('Z'<<24);
    long aviMovieOffset = 0;

    AVIIndexList indexlist = null;
    public MJPEGGenerator(File aviFile, int width, int height, double framerate) throws Exception {
        this.aviFile = aviFile;
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        aviOutput = new FileOutputStream(aviFile);
        aviChannel = aviOutput.getChannel();

        RIFFHeader(aviOutput);
        AVIMainHeader(aviOutput);
        AVIStreamList(aviOutput);
        AVIStreamHeader(aviOutput);
        AVIStreamFormat(aviOutput);
        AVIJunk(aviOutput);
        aviMovieOffset = aviChannel.position();
        AVIMovieList(aviOutput);
        indexlist = new AVIIndexList();
    }

    public void addImage(byte[] imageData) throws Exception {
        byte[] fcc = {'0', '0', 'd', 'b'};
        int useLength = imageData.length;
        long position = aviChannel.position();
        int extra = (useLength + (int) position) % 4;
        if (extra > 0)
            useLength = useLength + extra;

        indexlist.addAVIIndex((int) position, useLength);

        aviOutput.write(fcc);
        aviOutput.write(intBytes(swapInt(useLength)));
        aviOutput.write(imageData);
        if (extra > 0) {
            for (int i = 0; i < extra; i++)
                aviOutput.write(0);
        }
        frameCount++;
    }

    public void finishAVI() throws Exception {
        byte[] indexlistBytes = indexlist.toBytes();
        aviOutput.write(indexlistBytes);
        aviOutput.close();
        long size = aviFile.length();
        RandomAccessFile raf = new RandomAccessFile(aviFile, "rw");
        // todo add frame count
        raf.seek(4);
        raf.write(intBytes(swapInt((int) size - 8)));
        raf.seek(aviMovieOffset + 4);
        raf.write(intBytes(swapInt((int) (size - 8 - aviMovieOffset - indexlistBytes.length))));
        raf.seek(48);
        raf.write(intBytes(swapInt(frameCount)));
        raf.seek(140);
        raf.write(intBytes(swapInt(frameCount)));
        raf.close();
    }

    public static int swapInt(int v) {
        return (v >>> 24) | (v << 24) | ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
    }

    public static short swapShort(short v) {
        return (short) ((v >>> 8) | (v << 8));
    }

    public static byte[] intBytes(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i >>> 24);
        b[1] = (byte) ((i >>> 16) & 0x000000FF);
        b[2] = (byte) ((i >>> 8) & 0x000000FF);
        b[3] = (byte) (i & 0x000000FF);
        return b;
    }

    public static byte[] shortBytes(short i) {
        byte[] b = new byte[2];
        b[0] = (byte) (i >>> 8);
        b[1] =(byte) (i & 0x000000FF);
        return b;
    }

    void RIFFHeader(FileOutputStream fos) throws IOException {
        byte[] fcc1 = {'R', 'I', 'F', 'F'};
        int fileSize = 0;
        byte[] fcc2 = {'A', 'V', 'I', ' '};
        byte[] fcc3 = {'L', 'I', 'S', 'T'};
        int listSize = 200;
        byte[] fcc4 = {'h', 'd', 'r', 'l'};

        fos.write(fcc1);
        fos.write(intBytes(swapInt(fileSize)));
        fos.write(fcc2);
        fos.write(fcc3);
        fos.write(intBytes(swapInt(listSize)));
        fos.write(fcc4);
    }

    void AVIMainHeader(FileOutputStream fos) throws IOException {
        byte[] fcc = {'a', 'v', 'i', 'h'};
        int cb = 56;
        int dwMicroSecPerFrame = (int) ((1.0 / framerate) * 1000000.0);
        int dwMaxBytesPerSec = 10000000;
        int dwPaddingGranularity = 0;
        int dwFlags = 65552;
        int dwInitialFrames = 0;
        int dwStreams = 1;
        int dwSuggestedBufferSize = 0;
        int dwReserved = 0;

        fos.write(fcc);
        fos.write(intBytes(swapInt(cb)));
        fos.write(intBytes(swapInt(dwMicroSecPerFrame)));
        fos.write(intBytes(swapInt(dwMaxBytesPerSec)));
        fos.write(intBytes(swapInt(dwPaddingGranularity)));
        fos.write(intBytes(swapInt(dwFlags)));
        fos.write(intBytes(swapInt(dummy_int)));
        fos.write(intBytes(swapInt(dwInitialFrames)));
        fos.write(intBytes(swapInt(dwStreams)));
        fos.write(intBytes(swapInt(dwSuggestedBufferSize)));
        fos.write(intBytes(swapInt(width)));
        fos.write(intBytes(swapInt(height)));
        fos.write(intBytes(swapInt(dwReserved)));
        fos.write(intBytes(swapInt(dwReserved)));
        fos.write(intBytes(swapInt(dwReserved)));
        fos.write(intBytes(swapInt(dwReserved)));
    }

    void AVIStreamList(FileOutputStream fos) throws IOException {
        byte[] fcc = {'L', 'I', 'S', 'T'};
        int size = 124;
        byte[] fcc2 = {'s', 't', 'r', 'l'};

        fos.write(fcc);
        fos.write(intBytes(swapInt(size)));
        fos.write(fcc2);
    }

    void AVIStreamHeader(FileOutputStream fos) throws IOException {

        byte[] fcc = {'s', 't', 'r', 'h'};
        int cb = 64;
        byte[] fccType = {'v', 'i', 'd', 's'};
        byte[] fccHandler = {'M', 'J', 'P', 'G'};
        int dwFlags = 0;
        short wPriority = 0;
        short wLanguage = 0;
        int dwInitialFrames = 0;
        int dwScale = (int) ((1.0 / framerate) * 1000000.0); // microseconds per frame
        int dwRate = 1000000; // dwRate / dwScale = frame rate
        int dwStart = 0;
        int dwSuggestedBufferSize = 0;
        int dwQuality = -1;
        int dwSampleSize = 0;
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;

        fos.write(fcc);
        fos.write(intBytes(swapInt(cb)));
        fos.write(fccType);
        fos.write(fccHandler);
        fos.write(intBytes(swapInt(dwFlags)));
        fos.write(shortBytes(swapShort(wPriority)));
        fos.write(shortBytes(swapShort(wLanguage)));
        fos.write(intBytes(swapInt(dwInitialFrames)));
        fos.write(intBytes(swapInt(dwScale)));
        fos.write(intBytes(swapInt(dwRate)));
        fos.write(intBytes(swapInt(dwStart)));
        fos.write(intBytes(swapInt(dummy_int)));
        fos.write(intBytes(swapInt(dwSuggestedBufferSize)));
        fos.write(intBytes(swapInt(dwQuality)));
        fos.write(intBytes(swapInt(dwSampleSize)));
        fos.write(intBytes(swapInt(left)));
        fos.write(intBytes(swapInt(top)));
        fos.write(intBytes(swapInt(right)));
        fos.write(intBytes(swapInt(bottom)));
    }

    void AVIStreamFormat(FileOutputStream fos) throws IOException {
        byte[] fcc = {'s', 't', 'r', 'f'};
        int cb = 40;
        int biSize = 40; // same as cb
        int biWidth = width;
        int biHeight = height;
        short biPlanes = 1;
        short biBitCount = 24;
        byte[] biCompression = {'M', 'J', 'P', 'G'};
        int biSizeImage = width * height;
        int biXPelsPerMeter = 0;
        int biYPelsPerMeter = 0;
        int biClrUsed = 0;
        int biClrImportant = 0;

        fos.write(fcc);
        fos.write(intBytes(swapInt(cb)));
        fos.write(intBytes(swapInt(biSize)));
        fos.write(intBytes(swapInt(biWidth)));
        fos.write(intBytes(swapInt(biHeight)));
        fos.write(shortBytes(swapShort(biPlanes)));
        fos.write(shortBytes(swapShort(biBitCount)));
        fos.write(biCompression);
        fos.write(intBytes(swapInt(biSizeImage)));
        fos.write(intBytes(swapInt(biXPelsPerMeter)));
        fos.write(intBytes(swapInt(biYPelsPerMeter)));
        fos.write(intBytes(swapInt(biClrUsed)));
        fos.write(intBytes(swapInt(biClrImportant)));
    }

    void AVIMovieList(FileOutputStream fos) throws IOException {
        byte[] fcc = {'L', 'I', 'S', 'T'};
        int listSize = 0;
        byte[] fcc2 = {'m', 'o', 'v', 'i'};
        // 00db size jpg image data ...

        fos.write(fcc);
        fos.write(intBytes(swapInt(listSize)));
        fos.write(fcc2);
    }

    private class AVIIndexList {
        public byte[] fcc = {'i', 'd', 'x', '1'};
        public int cb = 0;
        public ArrayList ind = new ArrayList();

        public AVIIndexList() {

        }

        public void addAVIIndex(int dwOffset, int dwSize) {
            ind.add(new AVIIndex(dwOffset, dwSize));
        }

        public byte[] toBytes() throws Exception {
            cb = 16 * ind.size();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(fcc);
            baos.write(intBytes(swapInt(cb)));
            for (int i = 0; i < ind.size(); i++) {
                AVIIndex in = (AVIIndex) ind.get(i);
                baos.write(in.toBytes());
            }

            baos.close();

            return baos.toByteArray();
        }
    }

    private class AVIIndex {
        public byte[] fcc = {'0', '0', 'd', 'b'};
        public int dwFlags = 16;
        public int dwOffset = 0;
        public int dwSize = 0;

        public AVIIndex(int dwOffset, int dwSize) {
            this.dwOffset = dwOffset;
            this.dwSize = dwSize;
        }

        public byte[] toBytes() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(fcc);
            baos.write(intBytes(swapInt(dwFlags)));
            baos.write(intBytes(swapInt(dwOffset)));
            baos.write(intBytes(swapInt(dwSize)));
            baos.close();
            return baos.toByteArray();
        }
    }

    void AVIJunk(FileOutputStream fos) throws IOException{
        byte[] fcc = {'J', 'U', 'N', 'K'};
        int size = 1808;
        byte[] data = new byte[size];
        Arrays.fill(data, (byte) 0);

        fos.write(fcc);
        fos.write(intBytes(swapInt(size)));
        fos.write(data);
    }
}



