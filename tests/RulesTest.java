import cn.edu.healthtrack.Rules;
import java.time.LocalDate;

public class RulesTest {
  static int count = 0;

  static void check(boolean b) {
    count++;
    if (!b) throw new AssertionError("case " + count);
  }

  static void rejects(Runnable r) {
    count++;
    try {
      r.run();
    } catch (IllegalArgumentException e) {
      return;
    }
    throw new AssertionError("case " + count);
  }

  public static void main(String[] args) {
    check(Rules.weekStart(LocalDate.of(2026, 11, 5)).equals(LocalDate.of(2026, 11, 2)));
    check(Rules.weekStart(LocalDate.of(2026, 11, 1)).equals(LocalDate.of(2026, 10, 26)));
    check(Rules.weekStart(LocalDate.of(2027, 1, 1)).equals(LocalDate.of(2026, 12, 28)));
    check(Rules.weekStart(LocalDate.of(2026, 11, 2)).equals(LocalDate.of(2026, 11, 2)));
    check(Rules.date("2024-02-29").getDayOfMonth() == 29);
    rejects(() -> Rules.date("2025-02-29"));
    rejects(() -> Rules.date("2026-2-01"));
    rejects(() -> Rules.date("2026-13-01"));
    rejects(() -> Rules.date("0000-01-01"));
    Rules.dateRange("", "");
    check(true);
    Rules.dateRange("2026-01-01", "");
    check(true);
    Rules.dateRange("", "2026-01-01");
    check(true);
    Rules.dateRange("2026-01-01", "2026-01-01");
    check(true);
    rejects(() -> Rules.dateRange("2026-02-01", "2026-01-01"));
    rejects(() -> Rules.dateRange("2026-02-30", ""));
    check(Rules.number("0", 100000, "步数") == 0);
    check(Rules.number("100000", 100000, "步数") == 100000);
    rejects(() -> Rules.number("100001", 100000, "步数"));
    rejects(() -> Rules.number("-1", 100000, "步数"));
    rejects(() -> Rules.number("1.5", 1440, "时长"));
    rejects(() -> Rules.number("", 1440, "时长"));
    rejects(() -> Rules.number("999999999999", 100000, "步数"));
    Rules.record("2026-01-01", 0, 30, "游泳", "");
    check(true);
    rejects(() -> Rules.record("2026-01-01", 0, 0, "步行", ""));
    rejects(() -> Rules.record(LocalDate.now().plusDays(1).toString(), 1, 1, "步行", ""));
    rejects(() -> Rules.record("2026-01-01", 1, 1, "不存在", ""));
    rejects(() -> Rules.record("2026-01-01", 1, 1441, "步行", ""));
    rejects(() -> Rules.record("2026-01-01", 100001, 1, "步行", ""));
    String note = new String(new char[200]).replace('\0', '字');
    Rules.record("2026-01-01", 100000, 1440, "其他", note);
    check(true);
    rejects(() -> Rules.record("2026-01-01", 1, 1, "步行", note + "字"));
    rejects(() -> Rules.account("ab", "123456"));
    rejects(() -> Rules.account("demo", "12345"));
    rejects(() -> Rules.account("demo'--", "123456"));
    Rules.account("demo_01", "Abc12345");
    check(true);
    System.out.println("PASS " + count + " assertions");
  }
}
