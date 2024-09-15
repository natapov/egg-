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

    long aviMovieOffset = 0;

    AVIIndexList indexlist = null;
    public MJPEGGenerator(File aviFile, int width, int height, double framerate) throws Exception {
        this.aviFile = aviFile;
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        aviOutput = new FileOutputStream(aviFile);
        aviChannel = aviOutput.getChannel();

        aviOutput.write(RIFFHeader());
        aviOutput.write(AVIMainHeader());
        aviOutput.write(AVIStreamList());
        aviOutput.write(AVIStreamHeader());
        aviOutput.write(AVIStreamFormat());
        aviOutput.write(AVIJunk());
        aviMovieOffset = aviChannel.position();
        aviOutput.write(AVIMovieList());
        indexlist = new AVIIndexList();
    }

    public void addImage(byte[] imageData) throws Exception {
        byte[] fcc = new byte[]{'0', '0', 'd', 'b'};
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
        raf.close();
    }

    public static int swapInt(int v) {
        return (v >>> 24) | (v << 24) |
                ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
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
        b[1] = (byte) (i & 0x000000FF);
        return b;
    }

    private byte[] RIFFHeader() throws IOException {
        byte[] fcc = new byte[]{'R', 'I', 'F', 'F'};
        int fileSize = 0;
        byte[] fcc2 = new byte[]{'A', 'V', 'I', ' '};
        byte[] fcc3 = new byte[]{'L', 'I', 'S', 'T'};
        int listSize = 200;
        byte[] fcc4 = new byte[]{'h', 'd', 'r', 'l'};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(fileSize)));
        baos.write(fcc2);
        baos.write(fcc3);
        baos.write(intBytes(swapInt(listSize)));
        baos.write(fcc4);
        baos.close();

        return baos.toByteArray();
    }

    private byte [] AVIMainHeader() throws IOException {
        byte[] fcc = new byte[]{'a', 'v', 'i', 'h'};
        int cb = 56;
        int dwMicroSecPerFrame = 0; //  (1 / frames per sec) * 1,000,000
        int dwMaxBytesPerSec = 10000000;
        int dwPaddingGranularity = 0;
        int dwFlags = 65552;
        int dwInitialFrames = 0;
        int dwStreams = 1;
        int dwSuggestedBufferSize = 0;
        int dwReserved = 0;
        dwMicroSecPerFrame = (int) ((1.0 / framerate) * 1000000.0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(cb)));
        baos.write(intBytes(swapInt(dwMicroSecPerFrame)));
        baos.write(intBytes(swapInt(dwMaxBytesPerSec)));
        baos.write(intBytes(swapInt(dwPaddingGranularity)));
        baos.write(intBytes(swapInt(dwFlags)));
        baos.write(intBytes(swapInt(-1)));
        baos.write(intBytes(swapInt(dwInitialFrames)));
        baos.write(intBytes(swapInt(dwStreams)));
        baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
        baos.write(intBytes(swapInt(width)));
        baos.write(intBytes(swapInt(height)));
        baos.write(intBytes(swapInt(dwReserved)));
        baos.write(intBytes(swapInt(dwReserved)));
        baos.write(intBytes(swapInt(dwReserved)));
        baos.write(intBytes(swapInt(dwReserved)));
        baos.close();
        return baos.toByteArray();
    }

    private byte [] AVIStreamList() throws IOException {
        byte[] fcc = new byte[]{'L', 'I', 'S', 'T'};
        int size = 124;
        byte[] fcc2 = new byte[]{'s', 't', 'r', 'l'};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(size)));
        baos.write(fcc2);
        baos.close();
        return baos.toByteArray();
    }

    private byte[] AVIStreamHeader() throws IOException {

        byte[] fcc = new byte[]{'s', 't', 'r', 'h'};
        int cb = 64;
        byte[] fccType = new byte[]{'v', 'i', 'd', 's'};
        byte[] fccHandler = new byte[]{'M', 'J', 'P', 'G'};
        int dwFlags = 0;
        short wPriority = 0;
        short wLanguage = 0;
        int dwInitialFrames = 0;
        int dwScale = 0; // microseconds per frame
        int dwRate = 1000000; // dwRate / dwScale = frame rate
        int dwStart = 0;
        int dwSuggestedBufferSize = 0;
        int dwQuality = -1;
        int dwSampleSize = 0;
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;

        dwScale = (int) ((1.0 / framerate) * 1000000.0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(cb)));
        baos.write(fccType);
        baos.write(fccHandler);
        baos.write(intBytes(swapInt(dwFlags)));
        baos.write(shortBytes(swapShort(wPriority)));
        baos.write(shortBytes(swapShort(wLanguage)));
        baos.write(intBytes(swapInt(dwInitialFrames)));
        baos.write(intBytes(swapInt(dwScale)));
        baos.write(intBytes(swapInt(dwRate)));
        baos.write(intBytes(swapInt(dwStart)));
        baos.write(intBytes(swapInt(-1)));
        baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
        baos.write(intBytes(swapInt(dwQuality)));
        baos.write(intBytes(swapInt(dwSampleSize)));
        baos.write(intBytes(swapInt(left)));
        baos.write(intBytes(swapInt(top)));
        baos.write(intBytes(swapInt(right)));
        baos.write(intBytes(swapInt(bottom)));
        baos.close();
        return baos.toByteArray();
    }

    private byte[] AVIStreamFormat() throws IOException {
        byte[] fcc = new byte[]{'s', 't', 'r', 'f'};
        int cb = 40;
        int biSize = 40; // same as cb
        int biWidth = 0;
        int biHeight = 0;
        short biPlanes = 1;
        short biBitCount = 24;
        byte[] biCompression = new byte[]{'M', 'J', 'P', 'G'};
        int biSizeImage = 0; // width x height in pixels
        int biXPelsPerMeter = 0;
        int biYPelsPerMeter = 0;
        int biClrUsed = 0;
        int biClrImportant = 0;

        biWidth = width;
        biHeight = height;
        biSizeImage = width * height;


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(cb)));
        baos.write(intBytes(swapInt(biSize)));
        baos.write(intBytes(swapInt(biWidth)));
        baos.write(intBytes(swapInt(biHeight)));
        baos.write(shortBytes(swapShort(biPlanes)));
        baos.write(shortBytes(swapShort(biBitCount)));
        baos.write(biCompression);
        baos.write(intBytes(swapInt(biSizeImage)));
        baos.write(intBytes(swapInt(biXPelsPerMeter)));
        baos.write(intBytes(swapInt(biYPelsPerMeter)));
        baos.write(intBytes(swapInt(biClrUsed)));
        baos.write(intBytes(swapInt(biClrImportant)));
        baos.close();

        return baos.toByteArray();
    }

    private byte[] AVIMovieList() throws IOException {
        byte[] fcc = new byte[]{'L', 'I', 'S', 'T'};
        int listSize = 0;
        byte[] fcc2 = new byte[]{'m', 'o', 'v', 'i'};
        // 00db size jpg image data ...

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(listSize)));
        baos.write(fcc2);
        baos.close();

        return baos.toByteArray();
    }

    private class AVIIndexList {
        public byte[] fcc = new byte[]{'i', 'd', 'x', '1'};
        public int cb = 0;
        public ArrayList ind = new ArrayList();

        public AVIIndexList() {

        }
        public void addAVIIndex(AVIIndex ai) {
            ind.add(ai);
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
        public byte[] fcc = new byte[]{'0', '0', 'd', 'b'};
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

    byte[] AVIJunk() throws IOException{
        byte[] fcc = new byte[]{'J', 'U', 'N', 'K'};
        int size = 1808;
        byte[] data = new byte[size];
        Arrays.fill(data, (byte) 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fcc);
        baos.write(intBytes(swapInt(size)));
        baos.write(data);
        baos.close();
        return baos.toByteArray();
    }
}



