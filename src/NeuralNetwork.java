import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.*;
import java.util.ArrayList;

public class NeuralNetwork {

    private static ArrayList<Double> weights;                                               //веса нейронов
    private static double[] inputNeurals = new double[160000];                              //массив входных нейронов
    private static ArrayList<Double> outputNeurals = new ArrayList<>();                     //массив выходных нейронов
    private static ArrayList<String> recognizableResult = new ArrayList<>();

    double[] getInputNeurals(){

        return inputNeurals;
    }

    ArrayList<Double> getWeights() {

        return weights;
    }

    ArrayList<Double> getOutputNeurals(){

        return outputNeurals;
    }

    ArrayList<String> getRecognizableSymbols(){

        return recognizableResult;
    }

    void enterInputData(BufferedImage inputBImage){
        BufferedImage bImage = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D gBImage = (Graphics2D) bImage.getGraphics();
        gBImage.setColor(new Color(255, 255, 255));
        gBImage.fillRect(0, 0, 400, 400);
        gBImage.scale(400.0/inputBImage.getWidth(), 400.0/inputBImage.getHeight());
        gBImage.drawImage(inputBImage, 0, 0, null);

        int numberOfNeural = 0;
        for(int i = 0; i < 400; i++){
            for(int j = 0; j < 400; j++){
                Color color = new Color(bImage.getRGB(j, i));
                if((color.getRed() == 0) && (color.getGreen() == 0) && (color.getBlue() == 0)){
                    inputNeurals[numberOfNeural] = 1;
                }else{
                    inputNeurals[numberOfNeural] = 0;
                }
                numberOfNeural++;
            }
        }
    }

    /** инициализация весов и выходного слоя */
    void initOutputNeuralsAndWeights(){
        loadObjectData();
        for(int i = 0; i < recognizableResult.size(); i++){
            outputNeurals.add(.0);
        }
        weights = new ArrayList<>(inputNeurals.length * outputNeurals.size());
        loadMemory();
    }

    /** выходные данные выходных нейронов */
    void computeOutputDataNeuralNetwork(){
        for(int i = 0; i < outputNeurals.size(); i++){
            outputNeurals.set(i, .0);
        }
        for(int i = 0; i < outputNeurals.size(); i++){
            for(int j = 0; j < inputNeurals.length; j++){
                outputNeurals.set(i, outputNeurals.get(i) + (inputNeurals[j] * weights.get((weights.size()/outputNeurals.size()) * i + j)));
            }
        }
    }

    String neuralNetworkResponse(){
        double maxValue = getOutputNeurals().get(0);
        int indexOfMax = 0;
        for(int i = 0; i < getOutputNeurals().size(); i++){
            if(maxValue < getOutputNeurals().get(i)){
                maxValue = getOutputNeurals().get(i);
                indexOfMax = i;
            }
        }
        return getRecognizableSymbols().get(indexOfMax);
    }

    private void loadMemory(){
        String readedMemory = "";
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("src/memory/memory_multimeter.crn")));
            String c;
            while((c = in.readLine())!= null){
                readedMemory += c;
            }
            in.close();

            readedMemory = readedMemory.replace("\uFEFF","");           //
            readedMemory = readedMemory.replace("[","");                // убираем мусор
            readedMemory = readedMemory.replace("]","");                //
            readedMemory = readedMemory.replace(",","");                //

            String[] bufferString = readedMemory.split(" ");
            for(int i = 0; i < bufferString.length; i++){
                weights.add(Double.parseDouble(bufferString[i]));
            }

        }catch (IOException e){}
    }

    private void loadObjectData(){
        String readedData = "";
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("src/memory/learnedObjects.crn"),"windows-1251"));
            String c;
            while((c = in.readLine()) != null){
                readedData += c;
            }
            in.close();
            String s = new String(readedData.getBytes("windows-1251"),"UTF-8");

            s = s.replace("\uFEFF","");           //
            s = s.replace(",","");                // убираем мусор
            s = s.replace("[","");                //
            s = s.replace("]","");                //

            String[] bufferString = s.split(" ");

            for(int i = 0; i < bufferString.length; i++){
                recognizableResult.add(bufferString[i]);
            }

        }catch (IOException e){
            System.out.println("Error10! Ошибка при считывании learnedObjects.crn");
        }
    }

}
