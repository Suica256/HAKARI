import java.util.Scanner;

public class HakariConsole {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        QuestionFlow questions = new QuestionFlow(scanner);
        DecisionRequest req = questions.ask();

        DecisionLogic logic = new DecisionLogic();
        DecisionResult result = logic.calculate(req);

        questions.printResult(result);
    }
}
