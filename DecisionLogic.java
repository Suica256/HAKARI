/*
  判定ロジックを担当するクラス
  データクラスも合わせて定義
 */
class DecisionLogic {

    // 価格スコアの計算に使う定数
    private static final double PRICE_SCALE        = 0.01;  // ¥1 あたりのスコア換算率
    private static final double PRICE_PENALTY_RATE = 0.005; // 慎重ライン超過時のペナルティ率

    DecisionResult calculate(DecisionRequest req) {

        // ① 基本スコア：相場より安いほどプラス
        double baseScore = (req.marketPrice - req.actualPrice) * PRICE_SCALE;
        double saleBonus = getSaleBonus(req.isOnSale, req.saleDiscount);

        // ② 動機係数（動機によってリターンとリスクの重みを変える）
        double returnWeight = getReturnWeight(req.motivation);
        double riskWeight   = getRiskWeight(req.motivation);

        // ③ リターン合計
        double frequencyPts    = getFrequencyPoints(req.usageFrequency);
        double effectPts       = getEffectPoints(req.expectedEffect);
        double substitutPts    = getSubstitutabilityPoints(req.substitutability);
        double longevityBonus  = getLongevityBonus(req.longevity, req.usageFrequency, req.actualPrice);
        double returnScore     = (frequencyPts + effectPts + substitutPts + longevityBonus) * returnWeight;

        // ④ リスク合計
        double pricePenalty = 0.0;
        if (req.actualPrice > req.cautiousLine) {
            pricePenalty = (req.actualPrice - req.cautiousLine) * PRICE_PENALTY_RATE;
        }
        double failurePts  = getFailureRiskPoints(req.failureRisk);
        double maintPts    = getMaintenancePoints(req.maintenanceCost);
        double riskScore   = (failurePts + maintPts + pricePenalty) * riskWeight;

        // ⑤ 最終スコアと判定
        double finalScore = baseScore + saleBonus + returnScore - riskScore;
        boolean isGo = (finalScore >= 0);

        String message;
        if (isGo) {
            message = getGoMessage();
        } else {
            message = getStopMessage();
        }

        return new DecisionResult(finalScore, isGo, message, baseScore + saleBonus, returnScore, riskScore);
    }

    //  以下、各選択肢の点数を返すメソッド
    //  点数を変えたいときはここの数字を書き換える

    // セール割引率に応じたボーナス点
    private double getSaleBonus(boolean isOnSale, String saleDiscount) {
        if (!isOnSale || saleDiscount == null) {
            return 0.0;
        }
        if (saleDiscount.equals("TEN"))          return 10.0;
        if (saleDiscount.equals("TWENTY"))       return 20.0;
        if (saleDiscount.equals("THIRTY"))       return 30.0;
        if (saleDiscount.equals("FIFTY"))        return 45.0;
        if (saleDiscount.equals("OVER_SEVENTY")) return 60.0;
        return 0.0;
    }

    // 購入動機：リターンへの係数
    private double getReturnWeight(String motivation) {
        if (motivation.equals("NECESSITY"))     return 1.0; // 補充 → 普通
        if (motivation.equals("INVESTMENT"))    return 1.2; // 投資 → リターンを重視
        if (motivation.equals("ENTERTAINMENT")) return 0.8; // 娯楽 → リターンを少し低く見る
        if (motivation.equals("EXPERIENCE"))    return 1.0; // 経験 → 普通
        return 1.0;
    }

    // 購入動機：リスクへの係数
    private double getRiskWeight(String motivation) {
        if (motivation.equals("NECESSITY"))     return 0.8; // 補充 → リスクを少し甘く見る
        if (motivation.equals("INVESTMENT"))    return 1.0; // 投資 → 普通
        if (motivation.equals("ENTERTAINMENT")) return 1.2; // 娯楽 → リスクを厳しく見る
        if (motivation.equals("EXPERIENCE"))    return 1.1; // 経験 → リスクをやや厳しく
        return 1.0;
    }

    // 使用頻度の点数（よく使うほど高得点）
    // ※ 食品ジャンルでは null が来ることがある → 0点として扱う
    private double getFrequencyPoints(String frequency) {
        if (frequency == null)                    return 0.0;
        if (frequency.equals("DAILY"))            return 30.0;
        if (frequency.equals("FEW_PER_WEEK"))     return 20.0;
        if (frequency.equals("OCCASIONALLY"))     return 10.0;
        if (frequency.equals("ONE_TIME"))         return  0.0;
        return 0.0;
    }

    // 期待効果の点数
    private double getEffectPoints(String effect) {
        if (effect.equals("DRAMATIC")) return 30.0;
        if (effect.equals("SLIGHT"))   return 15.0;
        if (effect.equals("MAINTAIN")) return  5.0;
        return 0.0;
    }

    // 代替不可度の点数（他で代用できるほど低得点）
    private double getSubstitutabilityPoints(String substitutability) {
        if (substitutability.equals("UNIQUE"))           return  25.0;
        if (substitutability.equals("HAS_ALTERNATIVES")) return   5.0;
        if (substitutability.equals("HOME_SUBSTITUTE"))  return -10.0; // 家にあるもので代用可なのでマイナス
        return 0.0;
    }

    // 失敗リスクの点数（未知なほど高リスク）
    private double getFailureRiskPoints(String failureRisk) {
        if (failureRisk.equals("EXPERIENCED")) return  0.0;
        if (failureRisk.equals("SIMILAR"))     return 15.0;
        if (failureRisk.equals("UNKNOWN"))     return 30.0;
        return 0.0;
    }

    // 管理コストの点数（手間がかかるほど高リスク）
    private double getMaintenancePoints(String maintenanceCost) {
        if (maintenanceCost.equals("NONE")) return  0.0;
        if (maintenanceCost.equals("SOME")) return 15.0;
        if (maintenanceCost.equals("HIGH")) return 30.0;
        return 0.0;
    }

    // 永続性ボーナス：1回あたりのコストが安いほど高得点
    // ※ ガジェット・ゲーム・美容品のみ送られてくる。それ以外は null → 0点
    private double getLongevityBonus(String longevity, String usageFrequency, int actualPrice) {
        if (longevity == null) {
            return 0.0;
        }

        // 使用期間を日数に換算
        int longevityDays = 0;
        if      (longevity.equals("MONTH"))            longevityDays =   30;
        else if (longevity.equals("HALF_YEAR"))        longevityDays =  180;
        else if (longevity.equals("ONE_TWO_YEARS"))    longevityDays =  548; // 1.5年の平均
        else if (longevity.equals("THREE_FIVE_YEARS")) longevityDays = 1460; // 4年の平均
        else if (longevity.equals("OVER_FIVE_YEARS"))  longevityDays = 2190; // 6年

        if (longevityDays == 0) {
            return 0.0;
        }

        // 年間使用回数（使用頻度から換算）
        String freq;
        if (usageFrequency != null) {
            freq = usageFrequency;
        } else {
            freq = "OCCASIONALLY";
        }
        double usagePerYear = 26.0; // デフォルト：たまに（2週に1回）
        if      (freq.equals("DAILY"))        usagePerYear = 365.0;
        else if (freq.equals("FEW_PER_WEEK")) usagePerYear = 156.0; // 週3回
        else if (freq.equals("OCCASIONALLY")) usagePerYear =  26.0;
        else if (freq.equals("ONE_TIME"))     usagePerYear =   1.0;

        // 総使用回数 = 使用期間(年) × 年間使用回数
        double totalUsages = (longevityDays / 365.0) * usagePerYear;
        if (totalUsages <= 0) {
            return 0.0;
        }

        // 1回あたりのコスト（円）
        double costPerUse = actualPrice / totalUsages;

        // 1回あたりのコストが低いほど高いボーナス点を付与
        if (costPerUse <   5) return 60.0;
        if (costPerUse <  20) return 45.0;
        if (costPerUse <  50) return 30.0;
        if (costPerUse < 100) return 15.0;
        if (costPerUse < 200) return  5.0;
        return 0.0; // ¥200以上/回 → ボーナスなし
    }

    // GOメッセージをランダムに1つ返す
    private String getGoMessage() {
        String[] messages = {
            "リターンがリスクを上回っています。この買い物はプラスになる可能性が高いです。",
            "コスト対効果が見合っています。前向きに購入を検討してください。",
            "感情を除いた判断でも、この選択は合理的と言えます。"
        };
        int index = (int) (Math.random() * messages.length);
        return messages[index];
    }

    // STOPメッセージをランダムに1つ返す
    private String getStopMessage() {
        String[] messages = {
            "リスクまたはコストが期待リターンを上回っています。今は見送りましょう。",
            "感情を排除した判定では、まだ購入時期ではないかもしれません。",
            "本当に今必要か、もう一度考えてみましょう。"
        };
        int index = (int) (Math.random() * messages.length);
        return messages[index];
    }
}

// ================================================================
//  入力データの入れ物
// ================================================================
class DecisionRequest {
    public int     cautiousLine;
    public int     actualPrice;
    public int     marketPrice;
    public boolean isOnSale;
    public String  saleDiscount;     // TEN / TWENTY / THIRTY / FIFTY / OVER_SEVENTY
    public String  genre;            // FOOD / BEAUTY / GADGET / GAME / LEISURE / GIFT
    public String  motivation;       // NECESSITY / INVESTMENT / ENTERTAINMENT / EXPERIENCE
    public String  longevity;        // GADGET/GAME/BEAUTYのみ: MONTH / HALF_YEAR / ONE_TWO_YEARS / THREE_FIVE_YEARS / OVER_FIVE_YEARS
    public String  usageFrequency;   // DAILY / FEW_PER_WEEK / OCCASIONALLY / ONE_TIME
    public String  expectedEffect;   // DRAMATIC / SLIGHT / MAINTAIN
    public String  substitutability; // UNIQUE / HAS_ALTERNATIVES / HOME_SUBSTITUTE
    public String  failureRisk;      // EXPERIENCED / SIMILAR / UNKNOWN
    public String  maintenanceCost;  // NONE / SOME / HIGH
}

// ================================================================
//  出力データの入れ物
// ================================================================
class DecisionResult {
    public double  score;
    public boolean isGo;
    public String  message;
    public double  baseScore;
    public double  returnScore;
    public double  riskScore;

    public DecisionResult(double score, boolean isGo, String message,
                          double baseScore, double returnScore, double riskScore) {
        this.score       = score;
        this.isGo        = isGo;
        this.message     = message;
        this.baseScore   = baseScore;
        this.returnScore = returnScore;
        this.riskScore   = riskScore;
    }
}
