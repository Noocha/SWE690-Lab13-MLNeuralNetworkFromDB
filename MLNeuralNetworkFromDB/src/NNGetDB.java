import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class NNGetDB {
    String template_training_dataset_filename;

    public NNGetDB(String template_training_dataset_filename) {
        this.template_training_dataset_filename = template_training_dataset_filename;
    }

    public Instances getDataSet(String filename) {
        ArffLoader loader = new ArffLoader();
        try {
            loader.setFile(new File(filename));
            Instances dataSet = loader.getDataSet();
            return dataSet;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Instances getDataFromDB() {
        Instances dataSet = getDataSet(template_training_dataset_filename);
        Instance newInstance = new Instance(5);
        newInstance.setDataset(dataSet);

        System.out.println("Data from Database");
        String connectionURL = "jdbc:db2://10.4.53.14:50000/SAMPLE";
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
            connection = DriverManager.getConnection(connectionURL, "stds61107", "ds690");
            statement = connection.createStatement();
            String query = "select location, age, b.isbn, yearOfPublication, bookRating\n" +
                    "from WARAPORN.users as u, WARAPORN.bookRatings as br, WARAPORN.books as b\n" +
                    "where u.userid = br.userid and br.isbn = b.isbn and age is not null and bookRating > 0 ";

            rs = statement.executeQuery(query);

            String[] address;
            int row = 0;
            while (rs.next()) {
                address = rs.getString(1).split(",");
                if (address.length > 2) {
                    newInstance.setValue(dataSet.attribute("country"), address[2].trim());
                    newInstance.setValue(dataSet.attribute("age"), rs.getInt(2));

                    if (rs.getString(3).endsWith("X")) {
                        String isbn = rs.getString(3).substring(0, rs.getString(3).length() - 1);
                        newInstance.setValue(dataSet.attribute("bookisbn"), Long.parseLong(isbn));
                    } else {
                        newInstance.setValue(dataSet.attribute("bookisbn"), Long.parseLong(rs.getString(3)));
                    }
                    newInstance.setValue(dataSet.attribute("yearpublication"), rs.getInt(4));
                    newInstance.setValue(dataSet.attribute("rating"), rs.getInt(5));

                    dataSet.add(newInstance);
                    row++;
                }
            }

            System.out.println(row);
            System.out.println(dataSet.numInstances());
            System.out.println(dataSet.instance(0).toString());
            System.out.println(dataSet.instance(1).toString());

            dataSet.delete(0);
            System.out.println(dataSet.numInstances());
            System.out.println(dataSet.instance(0).toString());
            System.out.println(dataSet.instance(1).toString());

            return dataSet;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void train() {
        Instances trainingData = getDataFromDB();
        trainingData.setClassIndex(trainingData.numAttributes() - 1);
        MultilayerPerceptron mlp = new MultilayerPerceptron();
        mlp.setLearningRate(0.9);
        mlp.setMomentum(0.2);
        mlp.setTrainingTime(3000);
        mlp.setHiddenLayers("70");
        try {
            mlp.buildClassifier(trainingData);

            Evaluation eval = new Evaluation(trainingData);
            eval.evaluateModel(mlp, trainingData);
            System.out.println(eval.errorRate());
            System.out.println(eval.toSummaryString());

            //K-Nearest Neighbor(KNN)
            Classifier ibk = new IBk(1);
            double percentConparison = 0;
            double currentConparison = 0;
            double bestK = 0;

            for (int i = 3; i < 20; i+=2) {
                ibk = new IBk(i);

                ibk.buildClassifier(trainingData);
                System.out.println(ibk);

                eval = new Evaluation(trainingData);
                eval.evaluateModel(ibk, trainingData);
                System.out.println("KNN k = " + i);
                percentConparison = eval.correct()/eval.incorrect();
                if (percentConparison > currentConparison) {
                    currentConparison = percentConparison;
                    bestK = i;
                }

                System.out.println(eval.toSummaryString());
                System.out.println(eval.toClassDetailsString());
                System.out.println(eval.toMatrixString());
            }

            System.out.println("The best correct percentage is when k = " + bestK);

            ibk = new IBk((int) bestK);
            ibk.buildClassifier(trainingData);
            System.out.println("Prediction");
            Instances predictData = getDataSet("src/predictData.arff");
            predictData.setClassIndex(predictData.numAttributes() - 1);
            Instance predictInstance;
            double answer;
            for (int i = 0; i < predictData.numInstances() ; i++) {
                predictInstance = predictData.instance(i);
                answer = ibk.classifyInstance(predictInstance);
                System.out.println("Rating = " + answer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
