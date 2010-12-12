package com.notatrick.csm.applalgos.finalproj;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import Jama.Matrix;

public class AdditiveUpdate {

    private static double MIN_ALLOWABLE_DIFF = .02;
    private static double MIN_CHANGE_CUTOFF = .01;
    private static double EPSILON = .00000000001;
    private static double LEARNING_RATE = .0001;

    public static double lastDiff = 0;

    /**
     * @param v
     *            The data matrix of size
     * @param numFeatures
     *            The number of features to find
     * @return The weights and feature matrices
     */
    public List<Matrix> buildMatrix(List<Triplet> data, int numFeatures, int maxNumIter) {

        int largestRow = 0;
        int largestCol = 0;

        for (Triplet item : data) {
            if (item.row > largestRow) {
                largestRow = item.row;
            }
            if (item.col > largestCol) {
                largestCol = item.col;
            }
        }

        // Start with random values
        Matrix w = Matrix.random(largestRow + 1, numFeatures);
        Matrix h = Matrix.random(numFeatures, largestCol + 1);

        lastDiff = Double.MAX_VALUE;

        for (int f = 0; f < numFeatures; f++) {

            for (int i = 0; i < maxNumIter; i++) {

                double diff = 0;

                for (Triplet item : data) {
                    double err = LEARNING_RATE * (item.score - predictRating(w, h, item.row, item.col, numFeatures));
                    double wval = w.get(item.row, f);
                    w.set(item.row, f, w.get(item.row, f) + err * h.get(f, item.col) < 0 ? 0 : w.get(item.row, f) + err * h.get(f, item.col));
                    h.set(f, item.col, h.get(f, item.col) + err * wval < 0 ? 0 : h.get(f, item.col) + err * wval);
                    diff += Math.pow(err, 2);
                }

                // System.out.println((f + 1) + "/" + numFeatures + " - " + i + " : " + diff);

                if (diff < MIN_ALLOWABLE_DIFF || i >= maxNumIter || Math.abs(diff - lastDiff) < MIN_CHANGE_CUTOFF) {
                    lastDiff = diff;
                    break;
                }

            }

        }
        return Arrays.asList(new Matrix[] { w, h });

    }

    private double predictRating(Matrix w, Matrix h, int row, int col, int numFeatures) {
        double prediction = 0;
        for (int i = 0; i < numFeatures; i++) {
            prediction += w.get(row, i) * h.get(i, col);
        }
        return prediction;
    }

    private double calcMSE(Matrix a, Matrix b) {

        if (a.getColumnDimension() != b.getColumnDimension() || a.getRowDimension() != b.getRowDimension()) {
            throw new Error("Matrices not the same size");
        }

        double diff = 0;
        int count = 0;
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                if (a.get(i, j) != 0) {
                    count++;
                    diff += Math.pow(a.get(i, j) - b.get(i, j), 2);
                }
            }
        }

        return diff / count;

    }

    private List<Triplet> loadFromNetflixFile(String fileName) throws FileNotFoundException {

        List<Triplet> ret = new ArrayList<Triplet>();

        Scanner in = new Scanner(new File(fileName));

        in.nextLine(); // Skip a line
        in.nextInt(); // Skip first word

        while (in.hasNext()) {
            in.nextInt();
            int userNum = in.nextInt();
            String nextItem = in.next();
            do {
                ret.add(new Triplet(userNum, Integer.parseInt(nextItem.split(":")[0]), Integer.parseInt(nextItem.split(":")[1])));
                if (in.hasNext()) {
                    nextItem = in.next();
                } else {
                    break;
                }
            } while (nextItem.contains(":"));

        }

        return ret;

    }

    private List<Triplet> loadFromDocAnalysisFile(String fileName) throws FileNotFoundException {

        List<Triplet> ret = new ArrayList<Triplet>();

        Scanner in = new Scanner(new File(fileName));

        int docNum = in.nextInt();
        int wordNum = in.nextInt();

        in.nextInt(); // Number of word instances

        while (in.hasNext()) {
            ret.add(new Triplet(in.nextInt() - 1, in.nextInt() - 1, in.nextInt()));
        }

        return ret;

    }

    private List<Triplet> loadFromMusicCountsFile(String fileName) throws FileNotFoundException {

        List<Triplet> ret = new ArrayList<Triplet>();

        Scanner in = new Scanner(new File(fileName));
        in.useDelimiter("\t");

        HashMap<String, Integer> userMap = new HashMap<String, Integer>();
        HashMap<String, Integer> artistMap = new HashMap<String, Integer>();

        int i = 0;
        int j = 0;

        while (in.hasNext()) {

            String[] linePieces = in.nextLine().split("\t");

            if (linePieces.length != 3) {
                continue;
            }

            String user = linePieces[0];
            String artist = linePieces[1];
            int playCount = Integer.parseInt(linePieces[2].trim());

            if (user.length() == 0 || artist.length() == 0) {
                continue;
            }

            if (!userMap.containsKey(user)) {
                userMap.put(user, i++);
            }
            if (!artistMap.containsKey(artist)) {
                artistMap.put(artist, j++);
            }

            int userNum = userMap.get(user);
            int artistNum = artistMap.get(artist);

            ret.add(new Triplet(userNum, artistNum, playCount));

        }

        in.close();

        return ret;

    }

    private List<Triplet> createRandomDistr(int x, int y) {

        List<Triplet> ret = new ArrayList<Triplet>();

        Random r = new Random();

        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                ret.add(new Triplet(i, j, (int) (Math.abs(r.nextGaussian()) * (i + j) * 5. / (x + y))));
            }
        }

        return ret;

    }

    public static void runMe(int sizeX, int sizeY, int numF) throws FileNotFoundException {

        AdditiveUpdate mu = new AdditiveUpdate();

        // Load the data into mem
        List<Triplet> data = mu.loadFromNetflixFile(System.getProperty("user.dir") + "\\data\\netflix_100k_21m.txt");
        // List<Triplet> data = mu.loadFromDocAnalysisFile(System.getProperty("user.dir") + "\\data\\docword.kos.txt");
        // List<Triplet> data = mu.createRandomDistr(sizeX, sizeY);

        // Run the algo
        Long startTime = System.currentTimeMillis();
        List<Matrix> model = mu.buildMatrix(data, numF, 100);
        Long endTime = System.currentTimeMillis();

        int largestRow = 0;
        int largestCol = 0;

        for (Triplet item : data) {
            if (item.row > largestRow) {
                largestRow = item.row;
            }
            if (item.col > largestCol) {
                largestCol = item.col;
            }
        }

        // Matrix v = new Matrix(largestRow + 1, largestCol + 1);
        // for (Triplet item : data) {
        // v.set(item.row, item.col, item.score);
        // }

        System.out.println(sizeX + ", " + sizeY + ", " + numF + ", sdg, " + lastDiff + ", " + (endTime - startTime) / 1000.);

    }

}
