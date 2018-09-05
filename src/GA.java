import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static sun.security.krb5.Confounder.intValue;

class GA {

    // Population solution size
    private final int populationSize = 200;
    // Number of genes in a chromosome
    private final int numberOfAttributes = 7;
    // Size of tournament, used in chromosome selection
    private static final int tournamentPopSize = 50;

    // crossover threshold favouring optimal parent
    private static final double crossoverThreshold = 0.5;
    // Gene mutation rate, small mutation rate
    private static final double mutationMagnitude = 0.01;

    // Store current generation
    private int generation = 0;
    // Max generations before stopping
    private static final int maxGeneration = 5000;

    // Store data for training, testing and validation sets
    // [row number] [# of attributes]
    private int[][] trainingData = null,
                testData = null,
                validationData = null;

    // Container for holding actual salaries, row number corresponding to [x][] of the relevant data set
    private double[] trainingActualSalary = null,
            testActualSalary = null;

    // Container for holding solutions, [solution index][data item index]
    private double[][] calcTrainSalary = null,
            calcTestSalary = null,
            calcValidationSalary = null;
    /**
     * Model Index:
     * 0. Simple Linear
     * 1. Linear with constant
     * 2. Exponential with constant
     */
    private int modelIndex = 0;

    // Container to store training and testing SSE
    private double[] trainingSSE = new double[populationSize],
            testSSE = new double[populationSize];

    // Solutions generated which are evolved. [number of solutions][c values for each solution]
    private double[][] population = null;



    GA(){
    }

    void run(){
        // Generate initial population
        generatePopulation();

        // Determine training and testing MSE
        // Store determined salaries for each
        System.out.print("E #" + generation + " : ");
        evaluatePopulation(population, trainingData, trainingActualSalary, calcTrainSalary, trainingSSE);
        evaluatePopulation(population, testData, testActualSalary, calcTestSalary, testSSE);

        // while terminating condition not reached
        while (!terminate()) {
            //Increment generation
            generation++;
            System.out.print("E #" + generation + " : ");

            // Generate new population using tournament selection and use elitism and mutate based on performance
            double[][] newPop = createOffspring(population);
            System.arraycopy(newPop, 0, population, 0, population.length);

            // Evolve new generation and store solutions
            evaluatePopulation(population, trainingData, trainingActualSalary, calcTrainSalary, trainingSSE);
            evaluatePopulation(population, testData, testActualSalary, calcTestSalary, testSSE);
        }
    }

    private boolean terminate() {
        if (generation >= maxGeneration) {
            System.out.println("NOTE: Generation timeout reached (" + generation + " iterations)");
            return true;
        }

        // Get fittest in population, and the number of matches the solution has
        int fittestValueOfSolution = 0;

        // Fittest index based on solutions index
        int fittestIndex = getFittest(testSSE);
        int fitnessRating = getFitness(calcTestSalary[fittestIndex], testActualSalary);

        writeToFile(generation + ";" + testSSE[fittestIndex] + "\n");

        if (fitnessRating == testActualSalary.length) {
            System.out.println("NOTE: Solution found at generation " + generation + "with solution: " + Arrays.stream(population[fittestIndex]).mapToObj(Double::toString).reduce((s, s2) -> s + "," + s2).orElse("Unknown Solution"));
            return true;
        } else {
            int fitness = getFitness(calcTestSalary[fittestIndex], testActualSalary);

            if (fittestValueOfSolution < fitness)
                fittestValueOfSolution = fitness;
        }

        System.out.print("\t\tFittest solution= " + fittestValueOfSolution + " \t\t\tTestSSE= " + testSSE[fittestIndex] + "\n");

        return false;
    }

    /**
     * Calculate the fitness of a specific solution given the determined salaries and actual salaries
     * @param calculatedSalaries calculated salaries
     * @param actualSalaries actual salaries from data
     * @return fitness value
     */
    private int getFitness(double[] calculatedSalaries, double[] actualSalaries) {
        int currentFittest = 0;
        for (int j = 0; j < calculatedSalaries.length; j++) {
            if (calculatedSalaries[j] == actualSalaries[j]){
                currentFittest++;
            }
        }
        return currentFittest;
    }

    private void writeToFile(String s) {
        try {
            FileWriter writer = new FileWriter("output_model" + (modelIndex + 1)+ ".csv", true);
            PrintWriter printWriter = new PrintWriter(writer, true);
            printWriter.write(s);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeOffspring() {
        try {
            FileWriter writer = new FileWriter("offspring.csv", true);
            PrintWriter printWriter = new PrintWriter(writer, true);

            printWriter.write("#" + generation );
            Arrays.stream(population).forEach(doubles -> {
                printWriter.write(";");
                Arrays.stream(doubles).forEach(value -> printWriter.write(value + ";"));
            });
            printWriter.write("\n");

            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Determines the salary of each data item for all solutions in the current population
     *
     *  Also, the MSE is calculates for each salary
     */
    private void evaluatePopulation(double[][] population, int[][] dataItems, double[] actualSalaryStore, double[][] calcSalaryStore, double[] sseStore) {

        writeOffspring();
        // for each solution in the population, determine the salary using model # and store this.
        for (int i = 0; i < population.length; i++) {

            // current population solution
            double[] solution = population[i];

            Double localSSE = 0.0;

            // Traverse of training data, determining the salary from the current solution
            for (int i1 = 0; i1 < dataItems.length; i1++) {
                // training data item used to generate salary from
                double salary = determineSalary(dataItems[i1], solution);

                // Add determined salary to the list of solutions
                Double sal = salary;
                calcSalaryStore[i][i1] = sal;

                // Add { (Tp - Op) ^ 2 } to MSE set for each iteration. At the end,  MSE[i] / populationSize to get the actual MSE for a solution
                double v = actualSalaryStore[i1] - salary;
                double pow = Math.pow(v, 2);
                localSSE += pow;
            }

            double i1 = localSSE / dataItems.length;
            sseStore[i] = i1;
        }

    }

    private double determineSalary(int[] dataItem, double[] solution) {
        double salary = 0.0;

        // determine salary of a specific data item
        for (int j = 0; j < numberOfAttributes; j++) {
            switch (modelIndex) {
                case 0:
                case 1:
                    salary += dataItem[j] * solution[j];
                    break;
                case 2:
                default:
                    salary += Math.pow(dataItem[j] * solution[j], solution[j + numberOfAttributes + 1]);
                    break;
            }

            // Add 'C8' parameter
            if (modelIndex > 0) {
                salary += solution[(numberOfAttributes / 2) + 1];
            }
        }

        return salary;
    }

    private void generatePopulation() {
        // Initialize first generation solutions
        population = new double[populationSize][numberOfAttributes];
        for (int i = 0; i < populationSize; i++) {
            for (int i1 = 0; i1 < population[i].length; i1++) {
                population[i][i1] = nextRandom();
            }

            trainingSSE[i] = Integer.MAX_VALUE;
            testSSE[i] = Integer.MAX_VALUE;
        }

        // Read Training and Testing data
        readTrainAndValidationData();

        // read Validation Data
//        readEvaluationData();

        calcTrainSalary = new double[population.length][trainingData.length];
        calcTestSalary = new double[population.length][testData.length];
//        calcValidationSalary = new double[population.length][validationData.length];
    }

    private double nextRandom() {
        Random random = new Random();
        double v = random.nextInt(5000);
        while (v < 0){
            v = random.nextInt(5000);
        }
        return v;
    }

    private void readTrainAndValidationData() {
        List<int[]> ints = readCharactersFromFile("/SalData.csv");

        int ninetyPercentStart = ints.size() - (ints.size() / 10);

        // Read 0% - 89% of SalData
        trainingData = new int[ninetyPercentStart - 1][numberOfAttributes];
        trainingActualSalary = new double[trainingData.length];
        for (int i = 0; i < ninetyPercentStart - 1; i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 1, trainingData[i], 0, ints1.length - 1);
            trainingActualSalary[i] = ints1[0];
        }

        // Read 90% - 100% of SalData
        testData = new int[ints.size() - ninetyPercentStart][numberOfAttributes];
        testActualSalary = new double[testData.length];
        for (int i = 0; i < (ints.size() - ninetyPercentStart); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 1, testData[i], 0, ints1.length - 1);
            testActualSalary[i] = ints1[0];
        }
    }

    //TODO seperate actual salary from data
    private void readEvaluationData() {
        List<int[]> ints = readCharactersFromFile("/Evaluation.csv");
        validationData = new int[ints.size()][numberOfAttributes];

        for (int i = 0; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, validationData[i], 0, ints1.length);

        }
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

    private double[][] createOffspring(double[][] population) {

        Random r = new Random();
        int offspringCount = -1;
        double[][] offSpring = new double[population.length][numberOfAttributes];

        // use elitism
        offSpring[++offspringCount] = population[getFittest(trainingSSE)];

        while (offspringCount < (population.length-1)) {
            int p1Index = tournamentSelect(population);
            int p2Index = tournamentSelect(population);

//            System.out.println("Selecting parents: " + p1Index + " & " + p2Index);

            // Favour most fit parent
            if (trainingSSE[p1Index] > trainingSSE[p2Index]){
                int temp = p1Index;
                p2Index = p1Index;
                p1Index = temp;
            }

            double[] crossover = crossover(population[p1Index], population[p2Index]);

            crossover[r.nextInt(crossover.length - 1)] *= (r.nextInt(10) > 4) ? mutationMagnitude : (1 - mutationMagnitude);

            offSpring[++offspringCount] = crossover;
        }
        return offSpring;
    }


    /**
     * Gets the index of the fittest solution in a given population
     * @param population a population
     * @return fittest index
     */
    private int getFittest(double[] population) {
        int fittest = 0;
        for (int i = 1; i < population.length; i++) {
            if (population[fittest] > population[i])
                fittest = i;
        }
        return fittest;
    }

    private double[] crossover(double[] p1, double[] p2) {
        double[] child = new double[p1.length];
        for (int i = 0; i < p1.length; i++) {
            child[i] = (nextRandom() > crossoverThreshold) ? p1[i] : p2[i];
        }
        return child;
    }

    private int tournamentSelect(double[][] population) {
        Random r = new Random();
        int[] selected = new int[tournamentPopSize];

        for (int i = 0; i < tournamentPopSize; i++) {
            selected[i] = r.nextInt(population.length);
        }

        int fittestIndexInSelected = 0;
        for (int i = 1; i < selected.length; i++) {
            int i1 = selected[i];
            double i3 = trainingSSE[i1];

            int i2 = selected[fittestIndexInSelected];
            double i4 = trainingSSE[i2];

            if (i3 < i4)
                fittestIndexInSelected = i;
        }

        int i = selected[fittestIndexInSelected];
        return i;
    }

}
