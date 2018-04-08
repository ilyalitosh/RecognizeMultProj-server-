import iztypes.IZBinarizationModes;
import iztypes.IZFilterMaskSize;
import iztypes.IZFilterModes;
import iztypes.IZMorphologyModes;
import javafx.application.Application;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ilya litosh on 01.11.2017.
 */
public class StartServer {

    //private static Socket socket;
    private static ServerSocket serverSocket;
    //private static InputStream in;
    //private static OutputStream out;
    //private static DataInputStream dataIn;
    //private static DataOutputStream dataOut;
    private static String data;
    private static int counter = 0;
    private static int counterImages = 0;
    //private static byte[] byteData = new byte[270000];
    private static String ip;
    private static int port;
    private static NeuralNetwork nn = new NeuralNetwork();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(ImageZ.LIBRARY_NAME);
        Scanner scanInput = new Scanner(System.in);
        System.out.println("Введите ip-адрес");
        ip = scanInput.nextLine();
        if(ip.equals("/restart")){
            restartApplication();
        }
        System.out.println("Введите port");
        port = scanInput.nextInt();
        System.out.println("[Server]: Запуск...");
        System.out.println("[Server]: Загружаю и инициализирую память нейросети...");
        nn.initOutputNeuralsAndWeights();

        try {
            serverSocket = new ServerSocket(port, 0, InetAddress.getByName(ip));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[Server]: Сервер запущен, ip: " + serverSocket.getInetAddress().getHostAddress());
        //System.out.println("[Server]: Клиент подключился");
        while(true){
            try {
                Socket socket = serverSocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream in = socket.getInputStream();
                            OutputStream out = socket.getOutputStream();
                            DataInputStream dataIn = new DataInputStream(in);
                            DataOutputStream dataOut = new DataOutputStream(out);
                            byte[] byteData = new byte[270000];
                            //CamPreview cp = new CamPreview();
                            short countOfObject = 0;
                            long start = 0, end = 0;
                            while(socket.isConnected()){
                                try {
                                    dataIn.readFully(byteData);
                                    start = System.currentTimeMillis();
                                }catch (Exception e){
                                    socket.close();
                                    break;
                                }
                                System.out.println("[Server]: Кадр пришел, анализирую...");

                                IZMatrix comingImg = new IZMatrix(new IZSize(900, 300));
                                decodingFrameToIZMatrix(comingImg, byteData);

                                IZMatrix binImg = new IZMatrix(new IZSize(comingImg.getWidth(), comingImg.getHeight()));
                                IZProc.binarize(comingImg, binImg, IZBinarizationModes.IZ_BRADLEY_ROT, 10, 0.05, 0, 0);

                                IZMatrix filteredImg = new IZMatrix(new IZSize(binImg.getWidth(), binImg.getHeight()));
                                IZProc.filter(binImg, filteredImg, IZFilterModes.MEDIAN_FILTER, IZFilterMaskSize.MASK_7x7);

                                IZMatrix dilatedImg = new IZMatrix(new IZSize(filteredImg.getWidth(), filteredImg.getHeight()));
                                IZProc.morphology(filteredImg, dilatedImg, IZMorphologyModes.DILATION, new IZSize(7, 27));

                                /*BufferedImage saveed = IZConverter.convertIZMatrixToBImage(dilatedImg);
                                ImageIO.write(saveed, "PNG", new FileOutputStream(new File("src/frames/result/resultt.png")));*/

                                System.out.println("тут может стопиться сервер");
                                IZMatrix clippedImg = new IZMatrix(new IZSize(dilatedImg.getWidth(), dilatedImg.getHeight()));
                                IZProc.adaptiveClip(dilatedImg, clippedImg, 70);

                                IZImageObjects objects = IZProc.findObjects(clippedImg);
                                BufferedImage imgToRegognition;
                                IZImageObjects filteredImages = filterObjects(objects);
                                StringBuilder response = new StringBuilder();
                                for(int i = 0; i < filteredImages.size(); i++){
                                    if((filteredImages.get(i).getWidth()*filteredImages.get(i).getHeight() + .0)/filteredImages.get(i).getArea() < 1.2){
                                        if((filteredImages.get(i).getWidth() + .0)/filteredImages.get(i).getHeight() > 1.6){
                                            response.append("-");
                                        }else {
                                            response.append(".");
                                        }
                                    }else{
                                        imgToRegognition = IZConverter.convertIZMatrixToBImage(
                                                new IZMatrix(filteredImages.get(i).getObject(),
                                                new IZSize(filteredImages.get(i).getWidth(),
                                                filteredImages.get(i).getHeight())));
                                        //ImageIO.write(imgToRegognition, "PNG", new FileOutputStream(new File("src/frames/result/result" + i + ".png")));
                                        nn.enterInputData(imgToRegognition);
                                        nn.computeOutputDataNeuralNetwork();
                                        response.append(nn.neuralNetworkResponse());
                                    }
                                }
                                System.out.println("[Server]: Вижу " + response);
                                end = System.currentTimeMillis();
                                System.out.println("[Server]: Время анализа: " + ((end-start+0.0)/1000) + " сек. ");
                                dataOut.writeUTF(response.toString());
                            }
                            System.out.println("[Server]: Клиент отключился");
                            //cp.dispose();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[Server]: Клиент подключился");
        }
    }

    private static void restartApplication(){
       StartServer.main(null);
    }

    private static IZImageObjects filterObjects(IZImageObjects objects){
        IZImageObjects filteredImages = new IZImageObjects();
        for(int i = 0; i < objects.size(); i++){
            if(objects.get(i).getWidth() < 275 && objects.get(i).getArea() > 1100 /*&& ((objects.get(i).getHeight() + .0)/objects.get(i).getWidth()) < 5*/){
                if(objects.get(i).getArea() > 15000){
                    filteredImages.add(objects.get(i));
                    continue;
                }
                if((objects.get(i).getWidth()*objects.get(i).getHeight() + .0)/objects.get(i).getArea() < 1.2){
                    filteredImages.add(objects.get(i));
                }
            }
        }
        filteredImages.sortAsc();
        return filteredImages;
    }

    /*private static void decodingFrame(BufferedImage bImage){
        int iteratorX = 0, iteratorY = 0;
        Color pixelColor;
        Graphics g = bImage.getGraphics();
        //Graphics g1 = bImageSrc.getGraphics();
        for(int i = 0; i < byteData.length; i += 3){
            pixelColor = new Color((byteData[i] + 128), (byteData[i + 1] + 128), (byteData[i + 2] + 128));
            g.setColor(pixelColor);
            g.drawLine(iteratorX, iteratorY, iteratorX, iteratorY);
            //g1.setColor(pixelColor);
            //g1.drawLine(iteratorX, iteratorY, iteratorX, iteratorY);
            iteratorX++;
            if(iteratorX == 900){
                iteratorY++;
                iteratorX = 0;
            }
        }
    }*/

    /*private static void decodingFrameToMat(Mat src){
        int iteratorX = 0, iteratorY = 0;
        for(int i = 0; i < byteData.length; i++){
            src.put(iteratorY, iteratorX, byteData[i] + 128);
            iteratorX++;
            if(iteratorX == 900){
                iteratorY++;
                iteratorX = 0;
            }
        }
    }*/

    private static void decodingFrameToIZMatrix(IZMatrix src, byte[] byteData){
        int iteratorX = 0, iteratorY = 0;
        int srcWidth = src.getWidth();
        for(int i = 0; i < byteData.length; i++){
            src.setValue( byteData[i] + 128, iteratorX, iteratorY);
            iteratorX++;
            if(iteratorX == srcWidth){
                iteratorY++;
                iteratorX = 0;
            }
        }
    }

    private static BufferedImage clipFrame(BufferedImage bImage, short maxBorderClip){
        short maxWidthNumberImage = (short)(bImage.getWidth() / 4);
        short clipUp = 0, clipDown = 0;
        short pixelCounter = 0;
        Color colorPixel;
        /* определяем кол-во обрезаемых строк пикселей сверху */
        for(int i = 0; i < maxBorderClip; i++){
            pixelCounter = 0;
            for(int j = 0; j < bImage.getWidth(); j++){
                colorPixel = new Color(bImage.getRGB(j, i));
                if(colorPixel.getRed() == 255){
                    pixelCounter++;
                    if(pixelCounter == maxWidthNumberImage){
                        clipUp++;
                        break;
                    }
                }else{
                    pixelCounter = 0;
                }
            }
        }
        /* определяем кол-во обрезаемых строк пикселей снизу */
        for(int i = bImage.getHeight() - 1; i > bImage.getHeight() - maxBorderClip; i--){
            pixelCounter = 0;
            for(int j = 0; j < bImage.getWidth(); j++){
                colorPixel = new Color(bImage.getRGB(j, i));
                if(colorPixel.getRed() == 255){
                    pixelCounter++;
                    if(pixelCounter == maxWidthNumberImage){
                        clipDown++;
                        break;
                    }
                }else{
                    pixelCounter = 0;
                }
            }
        }
        BufferedImage bImagePreClipped = bImage.getSubimage(0, clipUp + 10, bImage.getWidth(), bImage.getHeight() - (clipUp + clipDown + 20));
        BufferedImage bImageClipped = new BufferedImage(bImagePreClipped.getWidth(), bImagePreClipped.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = bImageClipped.getGraphics();
        g.drawImage(bImagePreClipped, 0, 0, null);

        return bImageClipped;
    }

    private static boolean isNeededImage(/*BufferedImage bImage,*/ Rect rect, List<MatOfPoint> counters){
        /*int blackCount = 0, whiteCount = 0;
        Color colorCurrentPixel;
        for(int i = 0; i < bImage.getHeight(); i++){
            for(int j = 0; j < bImage.getWidth(); j++){
                colorCurrentPixel = new Color(bImage.getRGB(j, i));
                if(colorCurrentPixel.getRed() == 0){
                    blackCount++;
                }else{
                    whiteCount++;
                }
            }
        }
        //System.out.println(whiteCount + " --- " + blackCount + " --- " + ((blackCount + 0.0)/whiteCount));
        if(((blackCount + 0.0)/whiteCount) < 1.4){
            return true;
        }else{
            return false;
        }*/
        Rect currentRect;
        for(int i = 0; i < counters.size(); i++){
            currentRect = Imgproc.boundingRect(counters.get(i));
            if((rect.x > currentRect.x) && (rect.x + rect.width < currentRect.x + currentRect.width)){
                return false;
            }
        }

        return true;
    }

}
