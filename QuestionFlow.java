import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

/**
 * 質問の流れと画面表示を担当するクラス
 */
class QuestionFlow {

    private final Scanner scanner;

    QuestionFlow(Scanner scanner) {
        this.scanner = scanner;
    }

    // ===== 質問の流れ =====

    DecisionRequest ask() {
        printBanner();

        DecisionRequest req = new DecisionRequest();

        req.cautiousLine = askCautiousLine();

        // STEP 1: 価格情報
        printStep(1, "価格情報");
        req.actualPrice = askInt("実売価格（円）");
        req.marketPrice = askInt("相場の価格（円）");

        int saleChoice = askMenu("今セール中ですか？", "はい", "いいえ");
        req.isOnSale = (saleChoice == 1);

        if (req.isOnSale) {
            int discountChoice = askMenu("割引率を選んでください",
                                         "10%引き", "20%引き", "30%引き", "50%引き", "70%以上");
            if      (discountChoice == 1) req.saleDiscount = "TEN";
            else if (discountChoice == 2) req.saleDiscount = "TWENTY";
            else if (discountChoice == 3) req.saleDiscount = "THIRTY";
            else if (discountChoice == 4) req.saleDiscount = "FIFTY";
            else                          req.saleDiscount = "OVER_SEVENTY";
        }

        // STEP 2: ジャンル
        printStep(2, "商品ジャンル");
        int genreChoice = askMenu("何を買おうとしていますか？",
                                   "食品", "美容品", "ガジェット", "ゲーム", "娯楽品", "プレゼント");
        if      (genreChoice == 1) req.genre = "FOOD";
        else if (genreChoice == 2) req.genre = "BEAUTY";
        else if (genreChoice == 3) req.genre = "GADGET";
        else if (genreChoice == 4) req.genre = "GAME";
        else if (genreChoice == 5) req.genre = "LEISURE";
        else                       req.genre = "GIFT";

        // STEP 3: 購入動機
        printStep(3, "購入動機");
        int motivationChoice = askMenu("この買い物の一番の理由は何ですか？",
                                        "ないと困る", "投資目的", "楽しみたい（娯楽）", "試してみたい（経験）");
        if      (motivationChoice == 1) req.motivation = "NECESSITY";
        else if (motivationChoice == 2) req.motivation = "INVESTMENT";
        else if (motivationChoice == 3) req.motivation = "ENTERTAINMENT";
        else                            req.motivation = "EXPERIENCE";

        // STEP 4: 詳細条件
        printStep(4, "詳細条件");

        // ガジェット・ゲーム・美容品のみ永続性を聞く
        if (req.genre.equals("GADGET") || req.genre.equals("GAME") || req.genre.equals("BEAUTY")) {
            int longevityChoice = askMenu("どのくらい続きそうですか？（永続性）",
                                           "1ヶ月未満", "半年くらい", "1〜2年", "3〜5年", "5年以上");
            if      (longevityChoice == 1) req.longevity = "MONTH";
            else if (longevityChoice == 2) req.longevity = "HALF_YEAR";
            else if (longevityChoice == 3) req.longevity = "ONE_TWO_YEARS";
            else if (longevityChoice == 4) req.longevity = "THREE_FIVE_YEARS";
            else                           req.longevity = "OVER_FIVE_YEARS";
        }

        // 食品以外は使用頻度を聞く
        if (!req.genre.equals("FOOD")) {
            int freqChoice = askMenu("使用頻度を教えてください", "毎日", "週数回", "たまに", "今回のみ");
            if      (freqChoice == 1) req.usageFrequency = "DAILY";
            else if (freqChoice == 2) req.usageFrequency = "FEW_PER_WEEK";
            else if (freqChoice == 3) req.usageFrequency = "OCCASIONALLY";
            else                      req.usageFrequency = "ONE_TIME";
        }

        int effectChoice = askMenu("期待効果を教えてください", "劇的な増加", "微増", "現状維持");
        if      (effectChoice == 1) req.expectedEffect = "DRAMATIC";
        else if (effectChoice == 2) req.expectedEffect = "SLIGHT";
        else                        req.expectedEffect = "MAINTAIN";

        int subChoice = askMenu("代替不可度を教えてください",
                                 "これしかない", "他に選択肢あり", "家にあるもので代用可");
        if      (subChoice == 1) req.substitutability = "UNIQUE";
        else if (subChoice == 2) req.substitutability = "HAS_ALTERNATIVES";
        else                     req.substitutability = "HOME_SUBSTITUTE";

        int riskChoice = askMenu("失敗の可能性を教えてください",
                                  "経験あり（知っている）", "似たものはある", "全くの未知");
        if      (riskChoice == 1) req.failureRisk = "EXPERIENCED";
        else if (riskChoice == 2) req.failureRisk = "SIMILAR";
        else                      req.failureRisk = "UNKNOWN";

        int maintenanceChoice = askMenu("面倒くささ（管理コスト）を教えてください",
                                         "手間なし", "多少の手間", "専用の場所・手入れが必要");
        if      (maintenanceChoice == 1) req.maintenanceCost = "NONE";
        else if (maintenanceChoice == 2) req.maintenanceCost = "SOME";
        else                             req.maintenanceCost = "HIGH";

        return req;
    }

    // ===== 結果表示 =====

    void printResult(DecisionResult res) {
        System.out.println();
        System.out.println("==========================================");

        if (res.isGo) {
            System.out.println("  >>> GO  ---  買い <<<");
        } else {
            System.out.println("  >>> STOP  ---  見送り <<<");
        }

        System.out.printf("  スコア : %.1f pt%n", res.score);
        System.out.println("------------------------------------------");
        System.out.printf("  基本スコア    : %+.1f%n", res.baseScore);
        System.out.printf("  リターン合計  : %+.1f%n", res.returnScore);
        System.out.printf("  リスク合計    : %+.1f%n", -res.riskScore);
        System.out.println("------------------------------------------");
        System.out.println("  " + res.message);
        System.out.println("==========================================");
        System.out.println();
    }

    //表示

    private void printBanner() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("   HAKARI  -  買い物判定アプリ");
        System.out.println("==========================================");
        System.out.println();
    }

    private void printStep(int num, String title) {
        System.out.println();
        System.out.println("--- STEP " + num + " / 4 :  " + title + " ---");
    }


    // 慎重ラインの保存と読み込み

    private static final String CONFIG_FILE = "config.txt";

    // 慎重ラインを聞く。前回の値が保存されていればそれを提示する
    private int askCautiousLine() {
        int saved = loadCautiousLine();

        if (saved > 0) {
            System.out.println("\n前回の慎重ライン : " + saved + " 円");
            int choice = askMenu("どうしますか？", "そのまま使う", "変更する");
            if (choice == 1) {
                return saved;
            }
        }

        int line = askInt("慎重ライン（例: 10000）");
        saveCautiousLine(line);
        return line;
    }

    // config.txt から慎重ラインを読み込む。ファイルがなければ -1 を返す
    private int loadCautiousLine() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return -1;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            reader.close();
            return Integer.parseInt(line.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // 慎重ラインを config.txt に保存する
    private void saveCautiousLine(int value) {
        try {
            FileWriter writer = new FileWriter(CONFIG_FILE);
            writer.write(String.valueOf(value));
            writer.close();
        } catch (Exception e) {
            System.out.println("  ※ 設定の保存に失敗しました");
        }
    }

    // 数値を入力させる。1未満や文字列は弾いて再入力
    private int askInt(String question) {
        while (true) {
            System.out.print("\n" + question + ": ");
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val > 0) {
                    return val;
                }
            } catch (NumberFormatException e) {
                // 数字以外が入力された場合は何もせず、下のメッセージを表示する
            }
            System.out.println("  ※ 1以上の整数を入力してください");
        }
    }

    // 選択肢を番号で選ばせる。範囲外は弾いて再選択
    private int askMenu(String question, String... options) {
        while (true) {
            System.out.println("\n" + question);
            for (int i = 0; i < options.length; i++) {
                System.out.println("  " + (i + 1) + ". " + options[i]);
            }
            System.out.print("選択 (1-" + options.length + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= options.length) {
                    return choice;
                }
            } catch (NumberFormatException e) {
                // 数字以外が入力された場合は何もせず、下のメッセージを表示する
            }
            System.out.println("  ※ 1〜" + options.length + " の数字を入力してください");
        }
    }
}
