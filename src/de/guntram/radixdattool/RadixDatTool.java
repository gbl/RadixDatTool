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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author gbl
 */
public class RadixDatTool {

    final static String dataDir="/home/gbl/Oldgames";
    final static String expandSubdir="Radix_expanded";
    

    public static void main(String[] args) throws IOException {
        
        String whatToDo;
        String file;
        
        // whatToDo="extract";
        // file=dataDir+File.separatorChar+"RADIX.DAT";
        
        whatToDo="obitmap";
        file=dataDir+File.separatorChar+expandSubdir+File.separatorChar+
                "ObjectBitmaps"+File.separatorChar+"Nuke1-32-85";

        if (args.length==2) {
            whatToDo=args[0];
            file=args[1];
        }
        
        if (whatToDo.equals("extract")) {
            extract(file);
        } else if (whatToDo.equals("obitmap")) {
            obitmap(file);
        }
    }
    
    
    public static void extract(String inputFileName) throws IOException {
        RandomAccessFile inputFile=new RandomAccessFile(inputFileName, "r");
        inputFile.seek(0x11);
        int fileCount=endianSwap(inputFile.readInt());
        int indexPos=endianSwap(inputFile.readInt());
        System.out.println(""+fileCount+" file names at: "+Long.toHexString(indexPos));
        
        new File(dataDir+File.separatorChar+expandSubdir).mkdirs();
        new File(dataDir+File.separatorChar+expandSubdir+File.separatorChar+"ObjectBitmaps").mkdirs();
        new File(dataDir+File.separatorChar+expandSubdir+File.separatorChar+"WallBitmaps").mkdirs();
        
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
                    String fileName2=getStringAt(bitmapDescriptor, 0, 32);
                    int bitmapFilePos = getDwordAt(bitmapDescriptor, 32);
                    int width=getWordAt(bitmapDescriptor, 36);
                    int height=getWordAt(bitmapDescriptor, 38);
                    int nBytes;
                    System.out.println(String.format("  %3d: %-33s: %08x %3d %3d", j, fileName2, bitmapFilePos, width, height));
                    if (fileName.equals("WallBitmaps")) {
                        nBytes=width*height;
                    } else {
                        nBytes=width*height;
                        fileName2+="-"+width+"-"+height;
                    }
                    String outputSubDir=dataDir+File.separatorChar+expandSubdir+File.separatorChar+fileName;
                    if (fileName2.indexOf('/')>0) {
                        String outputSubSubDir=outputSubDir+File.separatorChar+fileName2.substring(0, fileName2.lastIndexOf('/'));
                        System.out.println("making dir "+outputSubSubDir);
                        new File(outputSubSubDir).mkdirs();
                    }
                    copy(outputSubDir+File.separatorChar+fileName2,
                            inputFile, bitmapFilePos, nBytes);
                }
            } else {
                copy(dataDir+File.separatorChar+expandSubdir+File.separatorChar+fileName, inputFile, startPos, fileSize);
            }
        }
    }
    
    static int endianSwap(int x) {
        return ((x&0xff000000)>>>24) |
               ((x&0x00ff0000)>>>8)  |
               ((x&0x0000ff00)<<8)   |
               ((x&0x000000ff)<<24);
    }
    
    public static void obitmap(String inputFileName) throws IOException {
        int width, height;
        char[] byteMap=new char[256];
        char nextMap='!';
        Matcher m=(Pattern.compile(".*-(\\d+)-(\\d+)$").matcher(inputFileName));
        if (m.find()) {
            System.out.println(m.group(1));
            width=Integer.parseInt(m.group(1));
            System.out.println(m.group(2));
            height=Integer.parseInt(m.group(2));
        } else {
            throw new IOException("Filename does not contain width/height");
        }
        RandomAccessFile inputFile=new RandomAccessFile(inputFileName, "r");
        int bytes=(int) inputFile.length();
        byte[] content=new byte[bytes];
        inputFile.readFully(content);
        for (int i=0; i<height; i++) {
            int pos=getWordAt(content, i*4);
            int first=getByteAt(content, i*4+2);
            int count=getByteAt(content, i*4+3);
            for (int j=0; j<first; j++) {
                System.out.print(' ');
            }
            for (int j=0; j<count; j++) {
                int color=getByteAt(content, pos++);
                if (byteMap[color]==0)
                    byteMap[color]=nextMap++;
                System.out.print(byteMap[color]);
            }
            System.out.println();
        }
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
