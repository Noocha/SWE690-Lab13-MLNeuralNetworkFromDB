public class MLNeuralNetwork {

    public static void main(String[] args) {
        String templateFile = "src/bookTemplate.arff";
        NNGetDB knnModel = new NNGetDB(templateFile);
//        knnModel.getDataFromDB();
        knnModel.train();
    }

}
