import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3

public class Main {

    // Chromosome Values
    public static int[][] tdData,
            vdData,
            edData;
    // Chromosome Weights
    public static double[][] population;

    // Number of genes in a chromosome
    final int numberOfGenes = 8;
    // Size of tournament, used in chromosome selection
    final int tournamentPopSize = 50;

    // crossover threshold favouring optimal parent
    final double crossoverThreshold = 0.6;
    // Gene mutation rate, small mutation rate
    final double mutationRate = 0.01;

    // Store current generation
    private int generation = 0;
    // Max generations before stopping
    private int maxGeneration = 1000;

    // Container to store accuracy error (used in determining fitness)
    int trainingMSE = Integer.MAX_VALUE,
            testMSE = Integer.MAX_VALUE;


    private Main() {
        // Evolution Algorithm
        generatePopulation();

        // Determines SSE
        evaluateFitness(population, 1);

        // Training set
        while (liveOn()) {
            generation++;
            System.out.print("E #" + generation + " : ");

            // Create new population from current population using crossover technique
            // Mutate genes of offspring for genetic diversity
            int[][] offspring = mutate(createOffspring(tdData));

            // replace old generation with new generation
            System.arraycopy(offspring, 0, tdData, 0, tdData.length);

            // Predict/Determine salaries
            int[] salaries = evaluateFitness(tdData, tWeight, 1);
        }
    }

    public static void main(String[] args) {
        new Main();
    }

    /**
     * Mutate current generation genes for genetic diversity
     */
    private int[][] mutate(int[][] offspring) {
        Random r = new Random(1);
        for (int i = 0; i < offspring.length; i++) {
            for (int i1 = 0; i1 < offspring[i].length; i1++) {
                offspring[i][i1] *= (r.nextGaussian() > 0.5) ? mutationRate : 1-mutationRate;
            }
        }

        return offspring;
    }

    /**
     * Check terminating conditions
     *
     * @return
     */
    private boolean liveOn() {
        if (generation >= maxGeneration) {
            System.out.println("Generation timeout reached (" + generation + " iterations)");
            return false;
        }

        if ((1-error) >= requiredAccuracy) {
            System.out.println("Solution found at generation " + generation);
            return false;
        }

        return true;
    }

    /**
     * Using tournament selection, fitessed parents are selected out of a group of ${tournamentPopSize} and added to ${selected} array
     */
    private int[][] createOffspring(int[][] population) {

        int[][] offSpring = new int[population.length][numberOfGenes];

        // use elitism
        offSpring[0] = getFittest(population);

        while (offSpring.length < population.length) {
            int p1 = tournamentSelect(population);
            int p2 = tournamentSelect(population);
            crossover(tWeight[p1], tWeight[p2]);
        }
        return offSpring;
    }

    // Create offspring from selected parents. This creates offspring by using crossover genes from each parent.
    private int[] crossover(double[] p1, double[] p2) {
        Random r = new Random();
        int[] child = new int[p1.length];
        for (int i = 0; i < p1.length; i++) {
            child[i] = (r.nextGaussian() > crossoverThreshold) ? p1[i] : p2[i];
        }
        return child;
    }

    /**
     * Gets the X fittest in population
     *
     * @param population
     * @return
     */
    private int tournamentSelect(int[][] population) {
        Random r = new Random(1);
        int[][] selected = new int[tournamentPopSize][numberOfGenes];

        for (int i = 0; i < tournamentPopSize; i++) {
            Double pos = r.nextGaussian() * population.length;
            selected[i] = population[pos.intValue()];
        }
        return getFittest(selected);
    }

    private int[] getFittest(int[][] pop) {
        for (int i = 0; i < pop.length; i++) {
            if (getFitness(parent) > getFitness(population[fittestPos]))
                fittestPos = i;
        }
    }

    private double getFitness(int[] gene, int solution) {
        double error = gene[0] - solution;
        return Math.abs(error / gene[0]);
    }

    private int[] evaluateFitness(int[][] population, int[][] data, final int modelNum) {
        int[] salaries = new int[population.length];

        for (int j = 0; j < population.length; j++) {
            Double salary = 0.0;

            for (int i = 0; i < population[j].length; i++) {
                switch (modelNum) {
                    case 1: salary += population[j][i] * data[j][i]; break;
                    case 2: salary += population[j][i] * data[j][i]; break;
                    case 3:
                    default: salary += Math.pow(population[j][i] * data[j][i], data[j][i + population.length + 1]); break;
                }
            }

            if (modelNum == 2) {
                salary += data[j][data.length - 1];
            } else if (modelNum == 3) {
                salary += data[j][(population.length / 2) + 1];
            }

            salaries[j] = salary.intValue();
        }


        double localerror = 0.0;
        for (int i = 0; i < tdGene.length; i++) {
            localerror += getFitness(data[i], predictedSalaries[i]);
        }

        // determines error in predicting salary
        error = localerror / tdGene.length;



        return salaries;
    }

    private void generatePopulation() {
        // Initialize Weights
        initializeWeights();

        // Read Training and Testing data
        readTrainAndValidationData();

        // read Validation Data
        readEvaluationData();
    }

    private void initializeWeights() {
        tWeight = new double[tdData.length][numberOfGenes];
        fillWithRandomWeights(tWeight);
        vWeight = new double[vdData.length][numberOfGenes];
        fillWithRandomWeights(vWeight);
        eWeight = new double[edData.length][numberOfGenes];
        fillWithRandomWeights(eWeight);
    }

    private void fillWithRandomWeights(double[][] weightsArray) {
        Random random = new Random(1);
        for (int i = 0; i < weightsArray.length; i++) {
            for (int i1 = 0; i1 < weightsArray[i].length; i1++) {
                weightsArray[i][i1] = random.nextGaussian();
            }
        }
    }

    private void readTrainAndValidationData() {
        List<int[]> ints = readCharactersFromFile("/Evalution.csv");

        int ninetyPercentStart = ints.size() - (ints.size() / 10);

        // Read 0% - 89% of SalData
        tdData = new int[ninetyPercentStart - 1][numberOfGenes];
        for (int i = 0; i < ninetyPercentStart - 1; i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, tdData[i], 0, ints1.length);
        }

        // Read 90% - 100% of SalData
        vdData = new int[ints.size() - ninetyPercentStart][numberOfGenes];
        for (int i = ninetyPercentStart; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, vdData[i], 0, ints1.length);
        }
    }

    private void readEvaluationData() {
        List<int[]> ints = readCharactersFromFile("/SalData.csv");
        edData = new int[ints.size()][numberOfGenes];

        for (int i = 0; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, vdData[i], 0, ints1.length);
        }
    }

    private List<int[]> readCharactersFromFile(String resource) {
        //  Read iterator from characters text file
        InputStream resourceAsStream = getClass().getResourceAsStream(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line = "";
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
}
