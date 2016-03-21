/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.radixdattool;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author gbl
 */
public class RadixDatTool {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String inputFileName="/home/gbl/Oldgames/RADIX.DAT";
        RandomAccessFile inputFile=new RandomAccessFile(inputFileName, "r");
        inputFile.seek(0x11);
        int fileCount=endianSwap(inputFile.readInt());
        int indexPos=endianSwap(inputFile.readInt());
        System.out.println(""+fileCount+" file names at: "+Long.toHexString(indexPos));
        
        byte[] fileEntryBytes=new byte[46];
        for (int n=0;;n++) {
            try {
                inputFile.seek(indexPos+46*n);
                inputFile.readFully(fileEntryBytes);
            } catch (EOFException e) {
                break;
            }
            String fileName=getStringAt(fileEntryBytes, 0, 32);
            int startPos=getDwordAt(fileEntryBytes, 32);
            int fileSize=getDwordAt(fileEntryBytes, 36);
            System.out.println(String.format("%3d: %-35s: %08x-%08x %08x", n, fileName, startPos, startPos+fileSize, fileSize));
            if (fileName.equals("ObjectBitmaps") || fileName.equals("WallBitmaps")) {
                byte[] bitmapsDescriptor=new byte[6];
                byte[] bitmapDescriptor=new byte[40];
                inputFile.seek(startPos);
                inputFile.readFully(bitmapsDescriptor);
                int bitmapsCount=getWordAt(bitmapsDescriptor, 0);
                int descriptorsPos=getDwordAt(bitmapsDescriptor, 2);
                int lastBitmapFilePos=0;
                for (int j=0; j<bitmapsCount; j++) {
                    inputFile.seek(descriptorsPos+j*40);
                    inputFile.readFully(bitmapDescriptor);
                    fileName=getStringAt(bitmapDescriptor, 0, 32);
                    int bitmapFilePos = getDwordAt(bitmapDescriptor, 32);
                    int bitmapP1=getWordAt(bitmapDescriptor, 36);
                    int bitmapP2=getWordAt(bitmapDescriptor, 38);
                    System.out.print(String.format("  %3d: %-33s: %08x %3d %3d", j, fileName, bitmapFilePos, bitmapP1, bitmapP2));
                    if (j>0) {
                        System.out.print(String.format("%8d after previous", bitmapFilePos-lastBitmapFilePos));
                    }
                    lastBitmapFilePos=bitmapFilePos;
                    System.out.println();
                }
            } else {
                // copy("/home/gbl/Oldgames/Radix_expanded/"+fileName, inputFile, startPos, fileSize);
            }
        }
    }
    
    static int endianSwap(int x) {
        return ((x&0xff000000)>>>24) |
               ((x&0x00ff0000)>>>8)  |
               ((x&0x0000ff00)<<8)   |
               ((x&0x000000ff)<<24);
    }
    
    private static int getByteAt(byte[] data, int pos) {
        return (data[pos]&0xff);
    }

    private static int getWordAt(byte[] data, int pos) {
        return getByteAt(data, pos) | (getByteAt(data, pos+1)<<8);
    }

    private static int getDwordAt(byte[] data, int pos) {
        return  getByteAt(data, pos) | 
               (getByteAt(data, pos+1)<<8) |
               (getByteAt(data, pos+2)<<16) |
               (getByteAt(data, pos+3)<<24);
    }
    
    private static String getStringAt(byte[] data, int pos, int maxlen) {
        String result="";
        for (int i=0; i<maxlen && data[pos+i]!='\0'; i++)
            result+=(char)data[pos+i];
        return result;
    }
    
    private static void copy(String outputName, RandomAccessFile inputFile, int pos, int size) throws IOException {
        File outputFile=new File(outputName);
        outputFile.delete();
        try (RandomAccessFile writer = new RandomAccessFile(outputFile, "rw")) {
            byte[] buffer=new byte[4096];
            inputFile.seek(pos);
            while (size>0) {
                int n=(size>4096 ? 4096 : size);
                if ((n=inputFile.read(buffer, 0, n))<=0) {
                    throw new IOException("Cannot read input");
                }
                writer.write(buffer, 0, n);
                size-=n;
            }
        }
    }
}
