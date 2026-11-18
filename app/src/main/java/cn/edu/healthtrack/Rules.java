package cn.edu.healthtrack;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;

public final class Rules {
  public static final String[] TYPES = {"步行", "跑步", "骑行", "游泳", "力量训练", "其他"};

  public static LocalDate date(String value) {
    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
      throw new IllegalArgumentException("日期格式为 YYYY-MM-DD");
    try {
      LocalDate date = LocalDate.parse(value);
      if (date.getYear() < 1) throw new IllegalArgumentException();
      return date;
    } catch (Exception e) {
      throw new IllegalArgumentException("日期不存在");
    }
  }

  public static void dateRange(String start, String end) {
    if (!start.isEmpty()) date(start);
    if (!end.isEmpty()) date(end);
    if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0)
      throw new IllegalArgumentException("开始日期不能晚于结束日期");
  }

  public static LocalDate weekStart(LocalDate today) {
    return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  public static int number(String value, int max, String name) {
    try {
      int n = Integer.parseInt(value);
      if (n < 0 || n > max) throw new Exception();
      return n;
    } catch (Exception e) {
      throw new IllegalArgumentException(name + "必须是 0 到 " + max + " 的整数");
    }
  }

  public static void record(String day, int steps, int minutes, String type, String note) {
    if (date(day).isAfter(LocalDate.now())) throw new IllegalArgumentException("不能录入未来日期");
    if (steps < 0 || steps > 100000 || minutes < 0 || minutes > 1440)
      throw new IllegalArgumentException("步数或时长超出范围");
    if (!Arrays.asList(TYPES).contains(type)) throw new IllegalArgumentException("请选择运动类型");
    if (steps == 0 && minutes == 0) throw new IllegalArgumentException("步数与时长不能同时为零");
    if (note.length() > 200) throw new IllegalArgumentException("备注最多 200 字");
  }

  public static void account(String name, String password) {
    if (!name.matches("[a-zA-Z0-9_]{3,20}"))
      throw new IllegalArgumentException("用户名须为 3–20 位字母、数字或下划线");
    if (password.length() < 6 || password.length() > 64)
      throw new IllegalArgumentException("密码须为 6–64 位");
  }
}
