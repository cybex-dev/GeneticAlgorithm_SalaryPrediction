// http://www.theprojectspot.com/tutorial-post/creating-a-genetic-algorithm-for-beginners/3

public class Main {

    private Main() {

        new GA()
                .evolve(100, 10, 0.5, 0.33, 0.5)
                .earlyStop(false)
                .predict();
    }

    public static void main(String[] args) {
        new Main();
    }
}
