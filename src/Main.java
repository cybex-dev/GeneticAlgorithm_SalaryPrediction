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
    public int[][] tdData,
            vdData,
            edData;
    // Chromosome Weights
    public double[][] population;

    // Population solution size
    final int populationSize = 80;
    // Number of genes in a chromosome
    final int numberOfGenes = 8;
    // Size of tournament, used in chromosome selection
    final int tournamentPopSize = 10;

    // crossover threshold favouring optimal parent
    final double crossoverThreshold = 0.6;
    // Gene mutation rate, small mutation rate
    final double mutationRate = 0.01;

    // Store current generation
    private int generation = 0;
    // Max generations before stopping
    private int maxGeneration = 1000;

    /**
     * Model Index:
     * 0. Simple Linear
     * 1. Linear with constant
     * 2. Exponential with constant
     */
    private int modelIndex = 0;

    // Container to store training and testing SSE
    int[] trainingMSE = new int[populationSize],
            testMSE = new int[populationSize];


    private Main() {
        // Evolution Algorithm
        generatePopulation();

        // Determines training SSE
        evaluateFitness();

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
                offspring[i][i1] *= (r.nextGaussian() > 0.5) ? mutationRate : 1 - mutationRate;
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

        if ((1 - error) >= requiredAccuracy) {
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
    private double[] crossover(double[] p1, double[] p2) {
        Random r = new Random();
        double[] child = new double[p1.length];
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
    private int tournamentSelect(int[] population) {
        Random r = new Random(1);
        int[] selected = new int[tournamentPopSize];

        for (int i = 0; i < tournamentPopSize; i++) {
            Double pos = r.nextGaussian() * population.length;
            selected[i] = pos.intValue();
        }
        return getFittest(selected);
    }

    private int getFittest(int[] selected) {
        int best = 0;

        for (int i = 1; i < selected.length; i++) {
            int index = selected[i];
            if (trainingMSE[index] < best) {
                best = index;
            }
        }

        return best;
    }


    /**
     * Calculates training and testing MSE
     *
     * @return
     */
    private void evaluateFitness() {

        // For all solutions in population
        for (int popIndex = 0; popIndex < populationSize; popIndex++) {
            Double salaryTrain = Double.MAX_VALUE,
                    salaryTest = Double.MAX_VALUE;

            // For each model, evaluate the following
                switch (modelIndex) {
                    case 0:
                    case 1:
                        salaryTrain += population[popIndex][modelIndex] * tdData[popIndex][modelIndex];
                        salaryTest += population[popIndex][modelIndex] * vdData[popIndex][modelIndex];
                        break;
                    case 2:
                    default:
                        salaryTrain += Math.pow(population[popIndex][modelIndex] * tdData[popIndex][modelIndex], tdData[popIndex][modelIndex + population.length + 1]);
                        salaryTest += Math.pow(population[popIndex][modelIndex] * vdData[popIndex][modelIndex], vdData[popIndex][modelIndex + population.length + 1]);
                        break;
                }

                if (modelIndex == 1) {
                    salaryTrain += tdData[popIndex][tdData.length - 1];
                    salaryTest += vdData[popIndex][vdData.length - 1];
                } else if (modelIndex == 2) {
                    salaryTrain += tdData[popIndex][(populationSize / 2) + 1];
                    salaryTest += vdData[popIndex][(populationSize / 2) + 1];
                }
                // TODO fix this

            // Calculate MSE for each model of the current training solution and add to MSE arrays
            double localerror = 0.0;
            for (int i = 0; i < tdData.length; i++) {
                localerror += Math.pow(tdData[popIndex][0] - salaryTrain, 2);
            }
            trainingMSE[popIndex] = new Double(localerror / tdData.length).intValue();

            // Calculate MSE for each model of the current testing solution and add to MSE arrays
            localerror = 0.0;
            for (int i = 0; i < vdData.length; i++) {
                localerror += Math.pow(vdData[popIndex][0] - salaryTest, 2);
            }
            testMSE[popIndex] = new Double(localerror / vdData.length).intValue();
        }
    }

    private void generatePopulation() {
        // Initialize first generation solutions
        population = new double[populationSize][numberOfGenes];
        Random random = new Random(1);
        for (int i = 0; i < populationSize; i++) {
            for (int i1 = 0; i1 < population[i].length; i1++) {
                population[i][i1] = random.nextGaussian();
            }

            trainingMSE[i] = Integer.MAX_VALUE;
            testMSE[i] = Integer.MAX_VALUE;
        }

        // Read Training and Testing data
        readTrainAndValidationData();

        // read Validation Data
        readEvaluationData();
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
