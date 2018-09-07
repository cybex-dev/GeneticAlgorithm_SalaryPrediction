// http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    List<int[]> trainData, validationData;

    private Main() {


        trainData = readCharactersFromFile("/SalData.csv");
        validationData = readCharactersFromFile("/Evaluation.csv");

//        test();
        stats();
    }


    private List<int[]> readCharactersFromFile(String resource) {
        //  Read iterator from characters text file
        InputStream resourceAsStream = getClass().getResourceAsStream(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        List<int[]> data = new ArrayList<>();

        try {
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                data.add(Arrays.stream(split).mapToInt(Integer::parseInt).toArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return characters
        return data;
    }

    private void test() {
        new GA()
                .setTrainTestData(trainData)
                .setValidationData(validationData)
                .evolve()
                .earlyStop(false)
                .predict();
    }

    private void stats() {
        for (int model: new int[]{0, 1, 2}){
            for (int popSize: new int[]{50, 100, 500, 1000}) {
                System.out.println("Population: " + popSize);
                for (double xOver: new double[]{0.5, 0.6, 0.8}) {
                    System.out.println("- Crossover Rate: " + xOver);
                    for (double prob: new double[]{0.33, 0.2, 0.5}){
                        System.out.println("-- Mutation Probability: " + prob);
                        for (double rate: new double[]{0.5, 1, 5, 10}){
                            System.out.println("--- Mutation Magnitude: " + rate);
                            GA ga = new GA();
                            ga.setTrainTestData(trainData)
                                    .setValidationData(validationData)
                                    .evolve(model, popSize, xOver, prob, rate)
                                    .earlyStop(false)
                                    .predict();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new File("output").mkdirs();

        new Main();
    }
}
