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
    private final static double EXPECTED_VALUE = 1.0;
    private static double error;
    private static double weightsDelta; // корректирующее значение весов
    private final static double LEARNING_RATE = 0.001; // коэффициент скорости обучения
    Raster raster;
    ColorModel model;

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

    /** функция активации(сигмоида) */
    private double activation(double value){

        return (1/(1 + Math.exp(-value)));
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

    /** тренировка сети */
    private void training(String nameOfNeural){
        if(nameOfNeural.equals("")){
            System.out.println("Error11! Не введено название обучаемого объекта");
            return;
        }
        int numberOfNeural = 0;                                         // номер нейрона
        for(int i = 0; i < recognizableResult.size(); i++){             // определение номера нейрона по названию распозноваемого объекта
            if(recognizableResult.get(i).equals(nameOfNeural)){
                numberOfNeural = i;
                break;
            }
            if(i == (recognizableResult.size() - 1)){
                for(int j = 0; j < 160000; j++){
                    weights.add(.0);
                }
                recognizableResult.add(nameOfNeural);
                outputNeurals.add(.0);
            }
        }
        error = EXPECTED_VALUE - activation(outputNeurals.get(numberOfNeural));
        weightsDelta = error * (activation(outputNeurals.get(numberOfNeural))*(1 - activation(outputNeurals.get(numberOfNeural))));
        System.out.println(weightsDelta + "------");
        for(int i = 0; i < recognizableResult.size(); i++){
            if(recognizableResult.get(i).equals(nameOfNeural)){
                for(int j = 0; j < inputNeurals.length; j++){
                    weights.set(j + (i*160000), weights.get(j + (i*160000)) + inputNeurals[j] * weightsDelta * LEARNING_RATE);
                }
                break;
            }
        }
        System.out.println("тренировка завершина");
        updateMemory();
    }

    /** вывод данных после пробега через функцию активации(результат расчета нейросети) */
    private void showOutput(){
        for(int i = 0; i < recognizableResult.size(); i++){
            System.out.println(recognizableResult.get(i) + " : \t" + activation(outputNeurals.get(i)));
        }
    }

    /** сохраняем(обновляем) память нейросети */
    private void updateMemory(){
        try{
            FileWriter writeInFile = new FileWriter(new File("src/memory/memory.crn"));
            writeInFile.write(weights.toString());
            writeInFile.flush();
            writeInFile.close();
        }catch (IOException e){}
        try{
            FileWriter writeInFile = new FileWriter(new File("src/memory/learnedObjects.crn"));
            writeInFile.write(recognizableResult.toString());
            writeInFile.flush();
            writeInFile.close();
        }catch (IOException e){}
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
