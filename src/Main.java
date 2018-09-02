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

    // Number of genes in a chromosome
    final int numberOfGenes = 8;

    // Size of tournament, used in chromosome selection
    final int tournamentPopSize = 50;

    // crossover threshold favouring optimal parent
    final double crossoverThreshold = 0.6;

    // Gene mutation rate, small mutation rate
    final double mutationRate = 0.01;

    // persist best solution for each generation
    final boolean elitism = true;

    // Store current generation
    private int generation = 0;

    // Chromosome Values
    public static int[][] tdGene, vdGene, edGene;

    // Chromosome Weights
    public static double[][] twGene, vwGene, ewGene;

    public static void main(String[] args) {
        new Main();
    }

    private Main(){
        // Evolution Algorithm
        generatePopulation();

        evaluateFitness();

        // Training set
        while (liveOn()) {
            // Selects new parents from current population using tournament selection.
            selectParents(tdGene);

            // Create offspring from selected parents. This creates offspring by using crossover genes from each parent.
            createOffspring();

            // Mutate genes of offspring for genetic diversity
            mutateOffspringGenes();

            evaluateFitness();
        }
    }

    /**
     * Mutate current generation genes for genetic diversity
     */
    private void mutateOffspringGenes() {

    }

    /**
     * Check terminating conditions
     * @return
     */
    private boolean liveOn() {
        return false;
    }

    // Creates offspring using parent gene crossover
    private void createOffspring() {

    }

    /**
     * Using tournament selection, fitessed parents are selected out of a group of ${tournamentPopSize} and added to ${selected} array
     */
    private void selectParents(int[][] population) {
        int[][] selected = new int[population.length][numberOfGenes];

        while (selected.length < population.length) {
            int[] p1 = selectFitessed(population, 1);
            int[] p2 = selectFitessed(population, 2);
        }
    }

    /**
     * Gets the X fittest in population
     * @param population
     * @param pos
     * @return
     */
    private int[] selectFitessed(int[][] population, int pos) {
        return new int[0];
    }


    private void evaluateFitness() {


    }

    private void evolve() {
        generation++;
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
        twGene = new double[tdGene.length][numberOfGenes];
        fillWithRandomWeights(twGene);
        vwGene = new double[vdGene.length][numberOfGenes];
        fillWithRandomWeights(vwGene);
        ewGene = new double[edGene.length][numberOfGenes];
        fillWithRandomWeights(ewGene);
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
        tdGene = new int[ninetyPercentStart-1][numberOfGenes];
        for (int i = 0; i < ninetyPercentStart - 1; i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, tdGene[i], 0, ints1.length);
        }

        // Read 90% - 100% of SalData
        vdGene = new int[ints.size()-ninetyPercentStart][numberOfGenes];
        for (int i = ninetyPercentStart; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, vdGene[i], 0, ints1.length);
        }
    }

    private void readEvaluationData() {
        List<int[]> ints = readCharactersFromFile("/SalData.csv");
        edGene = new int[ints.size()][numberOfGenes];

        for (int i = 0; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, vdGene[i], 0, ints1.length);
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

    private void run(){

    }


}
