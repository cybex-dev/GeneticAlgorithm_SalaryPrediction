import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class GA {

    // Max generations before stopping
    private static final int maxGeneration = 1000;
    private final String carat = ",";
    // Number of genes in a chromosome
    private final int numberOfAttributes = 7;
    // Population solution size
    private int populationSize = 1000;
    // Size of tournament, used in chromosome selection
    private int tournamentPopSize = 10;
    // crossover threshold favouring optimal parent
    private double crossoverThreshold = 0.5;
    // Probabilty of mutation
    private double mutationProb = 0.5;
    // Gene mutation rate, small mutation rate
    private double mutationMagnitude = 10;
    // Store current generation
    private int generation = 0;
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
            calcTestSalary = null;

    // Previous fittest test SSE
    private double previousTestSSE = Double.MAX_VALUE;
    private double[] previousTestSolution = new double[numberOfAttributes];

    private File popSolutionsFile;

    private String[] results = null;
    int resultIndex = -1;

    /**
     * Model Index:
     * 0. Simple Linear             - c1*v1 + c2*v2 + ... + cN * vN
     * 1. Linear with constant      - c1*v1 + c2*v2 + ... + cN * vN + cN+1
     * 2. Exponential with constant - (c1*v1)^c9 + (c2*v2)^c10 + ... + (cN * vN)^c2N+1
     */
    private int modelIndex = 2;

    // Container to store training and testing SSE
    private double[] trainingSSE = null,
            testSSE = null;

    // Solutions generated which are evolved. [number of solutions][c values for each solution]
    private double[][] population = null;

    // Stops processing after fittest solution is found
    private boolean stopOnFittestFound;

    GA() {}

    public GA evolve() {
        tournamentPopSize = populationSize / 10;

        System.out.println("Starting: \tPopSize=" + populationSize + " \tTour=" + tournamentPopSize + " \tX=" + crossoverThreshold + " \tMProb=" + mutationProb + " \tMMag=" + mutationMagnitude);

        popSolutionsFile = new File("output/pop_solutions_model" + (modelIndex + 1) + "_PopSize" + populationSize + "_Tour" + tournamentPopSize + "_X" + crossoverThreshold + "_MProb" + mutationProb + "_MMag" + mutationMagnitude + ".csv");
        try {
            if (popSolutionsFile.exists())
                popSolutionsFile.delete();
            popSolutionsFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        run();
        return this;
    }

    public GA evolve(int modelIndex, int populationSize, double crossoverThreshold, double mutationProb, double mutationMagnitude) {
        this.modelIndex = modelIndex;
        this.populationSize = populationSize;
        this.crossoverThreshold = crossoverThreshold;
        this.mutationProb = mutationProb;
        this.mutationMagnitude = mutationMagnitude;
        return evolve();
    }

    private void run() {
        // Generate initial population
        generatePopulation();

        // Determine training and testing MSE
        // Store determined salaries for each
        System.out.print("Progress: " + generation + "...");
        evaluatePopulation(trainingData, trainingActualSalary, calcTrainSalary, trainingSSE);
        evaluatePopulation(testData, testActualSalary, calcTestSalary, testSSE);

        // while terminating condition not reached
        while (!terminate()) {
            //Increment generation
            generation++;
            if (generation % 100 == 0) {
                System.out.print(generation + "...");
            }

            // Generate new population using tournament selection and use elitism and mutate based on performance
            double[][] newPop = createOffspring(population);
            System.arraycopy(newPop, 0, population, 0, population.length);

            // Evolve new generation and store solutions
            evaluatePopulation(trainingData, trainingActualSalary, calcTrainSalary, trainingSSE);
            evaluatePopulation(testData, testActualSalary, calcTestSalary, testSSE);
        }

        writeResults(popSolutionsFile, results);

        String[] solution = new String[population[0].length];
        System.arraycopy(Arrays.stream(previousTestSolution).mapToObj(v -> String.valueOf(v) + carat).toArray(), 0, solution, 0, solution.length);
        writeResults(popSolutionsFile, solution);

        System.out.println("Done\n\tSSE:\t\t" + previousTestSSE);
        System.out.println("Solution: \t" + Arrays.stream(solution).reduce((s, s2) -> s + "\t" + s2).orElse("Unknown Solution"));
    }

    private boolean terminate() {
//        saveOffspring(population);


        if (generation >= maxGeneration) {
            //$system.out.println("NOTE: Generation timeout reached (" + generation + " iterations)");
            return true;
        }

        // Fittest index based on solutions index
        int fittestIndex = getFittest(testSSE);

        saveResult(testSSE[fittestIndex] + "\n");

        // Check if lowest SSE is reached
        if (testSSE[fittestIndex] > previousTestSSE) {
            //$system.out.println("NOTE: Lowest Test SSE reached [" + fittestIndex + "]: " + Arrays.stream(population[fittestIndex]).mapToObj(Double::toString).reduce((s, s2) -> s + "\t\t" + s2).orElse("Unknown Solution"));
            if (stopOnFittestFound)
                return true;
        } else {
            // Save current fittest solution
            previousTestSSE = testSSE[fittestIndex];
            previousTestSolution = population[fittestIndex];
        }

        //$system.out.print("\t\t#" + generation + " Matches= " + numberOfMatches + " \t\t\tTestSSE= " + testSSE[fittestIndex] + "\n");

        return false;
    }

    private void saveResult(String s) {
        resultIndex++;
        results[resultIndex] = s;
    }

//    private void saveOffspring(double[] results){
//        resultsOffspring[++resultOffspringIndex] = results;
//    }

    /**
     * Calculate the fitness of a specific solution given the determined salaries and actual salaries
     *
     * @param calculatedSalaries calculated salaries
     * @param actualSalaries     actual salaries from data
     * @return fitness value
     */
    private int salaryMatches(double[] calculatedSalaries, double[] actualSalaries) {
        int currentFittest = 0;
        for (int j = 0; j < calculatedSalaries.length; j++) {
            if (calculatedSalaries[j] == actualSalaries[j]) {
                currentFittest++;
            }
        }
        return currentFittest;
    }

    private void writeResults(File file, String[] arr) {
        try {
            FileWriter writer = new FileWriter(file, true);
            PrintWriter printWriter = new PrintWriter(writer, true);
            for (int i = 0; i < arr.length; i++) {
                printWriter.write(arr[i]);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines the salary of each data item for all solutions in the current population
     * <p>
     * Also, the MSE is calculates for each salary
     */
    private void evaluatePopulation(int[][] dataItems, double[] actualSalaryStore, double[][] calcSalaryStore, double[] sseStore) {

        // for each solution in the population, determine the salary using model # and store this.
        for (int i = 0; i < population.length; i++) {

            // current population solution
            double[] solution = population[i];

            double localSSE = 0.0;

            // Traverse of training data, determining the salary from the current solution
            for (int i1 = 0; i1 < dataItems.length; i1++) {
                // training data item used to generate salary from
                double salary = determineSalary(dataItems[i1], solution);

                // Add determined salary to the list of solutions
                calcSalaryStore[i][i1] = salary;

                // Add { (Tp - Op) ^ 2 } to MSE set for each iteration. At the end,  MSE[i] / populationSize to get the actual MSE for a solution
                double v = actualSalaryStore[i1] - salary;
                localSSE += v * v;
            }

            sseStore[i] = localSSE;
        }

    }

    private double determineSalary(int[] dataItem, double[] solution) {
        double salary = 0.0;

        // determine salary of a specific data item
        for (int j = 0; j < numberOfAttributes; j++) {
            if (modelIndex == 0) {
                salary += dataItem[j] * solution[j];
            } else if (modelIndex == 1) {
                salary += dataItem[j] * solution[j];
            } else {
                double s = dataItem[j] * solution[j];
                s *= s;
                salary += s;
            }

            // Add 'C8' parameter
            if (modelIndex > 0) {
                salary += solution[(numberOfAttributes / 2) + 1];
            }
        }

        return salary;
    }

    private void generatePopulation() {


        int numWeights = 0;

        switch (modelIndex) {
            case 2:
                numWeights = numberOfAttributes;
            case 1:
                numWeights += 1;
            case 0:
                numWeights += numberOfAttributes;
                break;
        }

        trainingSSE = new double[populationSize];
        testSSE = new double[populationSize];

        results = new String[maxGeneration];

        // Initialize first generation solutions
        population = new double[populationSize][numWeights];
        for (int i = 0; i < populationSize; i++) {
            for (int i1 = 0; i1 < population[i].length; i1++) {
                population[i][i1] = nextRandom();
            }

            trainingSSE[i] = Integer.MAX_VALUE;
            testSSE[i] = Integer.MAX_VALUE;
        }

        // Read Training and Testing data
        //readTrainAndValidationData();

        // read Validation Data
        //readEvaluationData();

        calcTrainSalary = new double[population.length][trainingData.length];
        calcTestSalary = new double[population.length][testData.length];
    }

    private double nextRandom() {
        return new Random().nextDouble();
    }

    private double nextGaussian() {
        return new Random().nextGaussian();
    }

    public GA setTrainTestData(List<int[]> ints){
        return readTrainAndValidationData(ints);
    }

    private GA readTrainAndValidationData(List<int[]> ints) {

        int ninetyPercentStart = ints.size() - (ints.size() / 5);

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

        return this;
    }

    public GA setValidationData(List<int[]> ints){
        return readEvaluationData(ints);
    }

    //TODO seperate actual salary from data
    private GA readEvaluationData(List<int[]> ints) {

        validationData = new int[ints.size()][numberOfAttributes];

        for (int i = 0; i < ints.size(); i++) {
            int[] ints1 = ints.get(i);
            System.arraycopy(ints1, 0, validationData[i], 0, ints1.length - 1);
        }

        return this;
    }

    private double[][] createOffspring(double[][] population) {

        int offspringCount = -1;
        double[][] offSpring = new double[population.length][numberOfAttributes];

        // use elitism
        int fittestIndex = getFittest(trainingSSE);

        ////$system.out.println("Fittest Training SSE Index [" + fittestIndex + "] : " + Arrays.stream(population[fittestIndex]).mapToObj(Double::toString).reduce((s, s2) -> s + "\t\t" + s2).orElse("Unknown Solution"));

        offSpring[++offspringCount] = population[fittestIndex];

        int worstPerformers = population.length / 10;

        // Crossovers with mutation
        while (offspringCount < ((population.length - 1) - worstPerformers)) {
            int p1Index = tournamentSelect(population);
            int p2Index = tournamentSelect(population);

//            //$system.out.println("Selecting parents: " + p1Index + " & " + p2Index);

            // Favour most fit parent
            if (trainingSSE[p1Index] > trainingSSE[p2Index]) {
                int temp = p1Index;
                p2Index = p1Index;
                p1Index = temp;
            }

            double[] child = mutate(crossover(population[p1Index], population[p2Index]));

            offSpring[++offspringCount] = child;
        }

        // Add worst performers
        for (int i = 0; i < worstPerformers; i++) {
            offSpring[++offspringCount] = population[nextRandomIndex(population.length)];
        }

        return offSpring;
    }

    private double[] mutate(double[] crossover) {
        // Check if should mutate
        if (nextRandom() <= mutationProb) {
            // Mutate child with new random values
            double mutationValue = nextGaussian() * mutationMagnitude;

            for (int i = 0; i < crossover.length; i++) {
                if (nextRandom() <= mutationProb) {
                    crossover[i] += mutationValue;
                }
            }
        }

        return crossover;
    }


    /**
     * Gets the index of the fittest solution in a given population
     *
     * @param population a population
     * @return fittest index
     */
    private int getFittest(double[] population) {
        int fittest = 0;
        for (int i = 0; i < population.length; i++) {
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

    /**
     * Select ${tournamentPopSize} number of random individuals from current population
     *
     * @param population
     * @return
     */
    private int tournamentSelect(double[][] population) {
        int[] selected = new int[tournamentPopSize];

        for (int i = 0; i < tournamentPopSize; i++) {
            selected[i] = nextRandomIndex(population.length);
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

    private int nextRandomIndex(int tournamentPopSize) {
        return new Random().nextInt(tournamentPopSize);
    }

    public void predict() {
        double[] predictions = new double[validationData.length];
        //$system.out.println("Using solution: " + Arrays.stream(previousTestSolution).mapToObj(Double::toString).reduce((s, s2) -> s + "," + s2).orElse("Unknown Solution"));
        for (int i = 0; i < validationData.length; i++) {
            predictions[i] = determineSalary(validationData[i], previousTestSolution);
            //$system.out.println("Using DataItem: " + Arrays.stream(validationData[i]).mapToObj(Double::toString).reduce((s, s2) -> s + "," + s2).orElse("Unknown Solution"));
            //$system.out.println("#" + i + " Salary = " + salary);
        }
        String[] results = new String[predictions.length];
        System.arraycopy(Arrays.stream(predictions).mapToObj(v -> String.valueOf(v) + carat).toArray(),0, results, 0, predictions.length);
        writeResults(popSolutionsFile, results);
    }

    public GA earlyStop(boolean b) {
        stopOnFittestFound = b;
        return this;
    }
}
