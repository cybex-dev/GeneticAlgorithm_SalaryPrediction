// http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3

import java.io.File;

public class Main {

    private Main() {

        test();
//        stats();
    }

    private void test() {
        new GA()
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
                            new GA()
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
