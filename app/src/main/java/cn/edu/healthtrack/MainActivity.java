package cn.edu.healthtrack;

import android.app.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private Store db;
  private long user = -1;
  private String username = "";
  private LinearLayout page;
  private String currentPage = "login";
  private String filterFrom = "", filterTo = "", filterType = "全部";
  private Store.Record editing;
  private int nextViewId;
  private Button loginButton, registerButton;
  private ProgressBar progress;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private boolean busy = false;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    db = new Store(this);
    if (Build.VERSION.SDK_INT >= 33) {
      getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
          android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
    }
    if (state == null || state.getLong("user", -1) < 0) {
      loginPage();
      return;
    }
    user = state.getLong("user");
    username = state.getString("username", "");
    filterFrom = state.getString("from", "");
    filterTo = state.getString("to", "");
    filterType = state.getString("type", "全部");
    String savedPage = state.getString("page", "home");
    if (savedPage.equals("records")) records(filterFrom, filterTo, filterType);
    else if (savedPage.equals("edit")) {
      Store.Record r = null;
      if (state.getLong("recordId", 0) != 0) {
        r = new Store.Record();
        r.id = state.getLong("recordId");
        r.day = state.getString("day");
        r.steps = state.getInt("steps");
        r.minutes = state.getInt("minutes");
        r.type = state.getString("recordType");
        r.note = state.getString("note");
      }
      edit(r);
    } else home();
  }

  private int dp(int v) {
    return (int) (getResources().getDisplayMetrics().density * v);
  }

  private void screen(String title, String sub) {
    screen(title, sub, false);
  }

  private void screen(String title, String sub, boolean listHeader) {
    // Reuse the same field IDs so Android can restore each page's form after rotation.
    nextViewId = 100;
    page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(16), dp(12), dp(16), dp(16));
    if (!listHeader) {
      ScrollView scroll = new ScrollView(this);
      scroll.setFillViewport(true);
      scroll.addView(page);
      scroll.setFitsSystemWindows(true);
      setContentView(scroll);
    }
    text(title, 22, true);
    if (!sub.isEmpty()) text(sub, 14, false);
  }

  private TextView text(String s, int size, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(Color.DKGRAY);
    t.setTextSize(size);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    t.setPadding(0, dp(6), 0, dp(6));
    page.addView(t);
    return t;
  }

  private EditText input(String label, String value, int type) {
    TextView caption = text(label, 14, false);
    EditText e = new EditText(this);
    e.setId(nextViewId++);
    caption.setLabelFor(e.getId());
    e.setSingleLine(true);
    e.setTextSize(16);
    e.setInputType(type);
    e.setText(value);
    e.setMinHeight(dp(48));
    page.addView(e, new LinearLayout.LayoutParams(-1, -2));
    return e;
  }

  private Button button(String s, Runnable action) {
    Button b = new Button(this);
    b.setText(s);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(4);
    page.addView(b, lp);
    b.setOnClickListener(
        v -> {
          perform(action);
        });
    return b;
  }

  private Spinner types(boolean all, String selected) {
    Spinner s = new Spinner(this);
    s.setId(nextViewId++);
    List<String> a = new ArrayList<>();
    if (all) a.add("全部");
    a.addAll(Arrays.asList(Rules.TYPES));
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, a);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    s.setSelection(Math.max(0, a.indexOf(selected)));
    page.addView(s, new LinearLayout.LayoutParams(-1, dp(48)));
    return s;
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private void perform(Runnable action) {
    try {
      action.run();
    } catch (IllegalArgumentException e) {
      toast(e.getMessage());
    } catch (Exception e) {
      toast("操作失败，请重试");
    }
  }

  private EditText dateInput(String label, String value) {
    EditText field = input(label, value, InputType.TYPE_CLASS_TEXT);
    field.setHint("选择日期");
    field.setFocusable(false);
    field.setOnClickListener(v -> {
      LocalDate date = value(field).isEmpty() ? LocalDate.now() : Rules.date(value(field));
      DatePickerDialog picker = new DatePickerDialog(this,
          (view, y, m, d) -> field.setText(LocalDate.of(y, m + 1, d).toString()),
          date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
      picker.getDatePicker().setMaxDate(System.currentTimeMillis());
      picker.show();
    });
    return field;
  }

  private String value(EditText e) {
    return e.getText().toString().trim();
  }

  private void loginPage() {
    currentPage = "login";
    screen("健康运动记录", "");
    EditText name = input("用户名", "", InputType.TYPE_CLASS_TEXT);
    name.setHint("3–20 位字母、数字或下划线");
    EditText password =
        input("密码", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    password.setHint("6–64 位");
    password.setSaveEnabled(false);
    loginButton = button("登录", () -> authenticate(name, password, false));
    registerButton = button("注册", () -> authenticate(name, password, true));
    progress = new ProgressBar(this);
    progress.setVisibility(View.GONE);
    page.addView(progress, new LinearLayout.LayoutParams(-1, dp(32)));
    text("首次使用请填写账号后注册。", 14, false);
  }

  private void setBusy(boolean value) {
    busy = value;
    loginButton.setEnabled(!value);
    registerButton.setEnabled(!value);
    progress.setVisibility(value ? View.VISIBLE : View.GONE);
  }

  private void authenticate(EditText name, EditText password, boolean register) {
    if (busy) return;
    String n = value(name), p = password.getText().toString();
    Rules.account(n, p);
    setBusy(true);
    worker.execute(
        () -> {
          try {
            long id = register ? db.register(n, p) : db.login(n, p);
            runOnUiThread(
                () -> {
                  if (isFinishing() || isDestroyed()) return;
                  setBusy(false);
                  if (id < 0) {
                    toast("用户名或密码错误");
                    return;
                  }
                  user = id;
                  username = n;
                  home();
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (isFinishing() || isDestroyed()) return;
                  setBusy(false);
                  toast(e instanceof IllegalArgumentException ? e.getMessage() : "账号操作失败");
                });
          }
        });
  }

  private void home() {
    if (user < 0) {
      loginPage();
      return;
    }
    currentPage = "home";
    screen("运动统计", "账号：" + username + "    " + LocalDate.now());
    LocalDate now = LocalDate.now();
    summary("今日运动", now, now);
    summary("本周（周一起）", Rules.weekStart(now), now);
    summary("本月累计", now.withDayOfMonth(1), now);
    button("新增记录", () -> edit(null));
    button("查看记录", () -> records("", "", "全部"));
    button(
        "退出登录",
        () -> {
          user = -1;
          username = "";
          loginPage();
        });
  }

  private void summary(String title, LocalDate start, LocalDate end) {
    long[] n = db.totals(user, start.toString(), end.toString());
    text(title, 18, true);
    text(n[0] + " 步 / " + n[1] + " 分钟 / " + n[2] + " 天", 16, false);
  }

  private void records(String from, String to, String type) {
    currentPage = "records";
    filterFrom = from;
    filterTo = to;
    filterType = type;
    screen("运动记录", "点击记录可修改或删除", true);
    EditText a = dateInput("开始日期（可空）", from);
    EditText b = dateInput("结束日期（可空）", to);
    text("运动类型", 14, false);
    Spinner s = types(true, type);
    button(
        "查询",
        () -> {
          String x = value(a), y = value(b);
          Rules.dateRange(x, y);
          records(x, y, s.getSelectedItem().toString());
        });
    button("清空筛选", () -> records("", "", "全部"));
    button("返回", () -> home());
    List<Store.Record> rows =
        db.list(user, from.isEmpty() ? "0000-01-01" : from, to.isEmpty() ? "9999-12-31" : to, type);
    text("共 " + rows.size() + " 条记录", 18, true);
    if (rows.isEmpty()) text("暂无记录", 16, false);
    ListView list = new ListView(this);
    list.setId(1);
    list.setFitsSystemWindows(true);
    list.addHeaderView(page, null, false);
    list.setAdapter(new ArrayAdapter<Store.Record>(this, android.R.layout.simple_list_item_2, rows) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView == null
            ? getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
            : convertView;
        Store.Record r = getItem(position);
        ((TextView) row.findViewById(android.R.id.text1)).setText(r.day + "  " + r.type);
        ((TextView) row.findViewById(android.R.id.text2)).setText(
            r.steps + " 步 / " + r.minutes + " 分钟" + (r.note.isEmpty() ? "" : "\n" + r.note));
        return row;
      }
    });
    list.setOnItemClickListener((parent, view, position, id) -> {
      Store.Record r = (Store.Record) parent.getItemAtPosition(position);
      if (r == null) return;
      new AlertDialog.Builder(this).setTitle(r.day)
          .setItems(new String[] {"修改", "删除"}, (dialog, which) -> {
            if (which == 0) edit(r);
            else new AlertDialog.Builder(this).setTitle("删除记录")
                .setMessage("确定删除 " + r.day + " 的记录？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> perform(() -> {
                  db.delete(user, r.id);
                  toast("已删除");
                  records(from, to, type);
                })).show();
          }).show();
    });
    setContentView(list);
  }

  private void edit(Store.Record r) {
    currentPage = "edit";
    editing = r;
    screen(r == null ? "新增记录" : "修改记录", "每天一条，填写全天步数、时长和主要运动类型");
    EditText date = dateInput("日期", r == null ? LocalDate.now().toString() : r.day);
    EditText steps =
        input(
            "每日步数（0–100000）",
            r == null ? "" : String.valueOf(r.steps),
            InputType.TYPE_CLASS_NUMBER);
    EditText minutes =
        input(
            "运动时长（分钟，0–1440）",
            r == null ? "" : String.valueOf(r.minutes),
            InputType.TYPE_CLASS_NUMBER);
    text("主要运动类型", 14, true);
    Spinner type = types(false, r == null ? "步行" : r.type);
    EditText note = input("备注（可空，最多 200 字）", r == null ? "" : r.note,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    note.setSingleLine(false);
    note.setMinLines(2);
    note.setMaxLines(4);
    button(
        "保存",
        () -> {
          db.save(
              user,
              r == null ? 0 : r.id,
              value(date),
              Rules.number(value(steps), 100000, "步数"),
              Rules.number(value(minutes), 1440, "时长"),
              type.getSelectedItem().toString(),
              value(note));
          toast("保存成功");
          if (r == null) home();
          else records(filterFrom, filterTo, filterType);
        });
    button("取消", this::goBack);
  }

  @Override
  public void onBackPressed() {
    goBack();
  }

  private void goBack() {
    if (currentPage.equals("edit") && editing != null) records(filterFrom, filterTo, filterType);
    else if (currentPage.equals("edit") || currentPage.equals("records")) home();
    else finish();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putLong("user", user);
    state.putString("username", username);
    state.putString("page", currentPage);
    state.putString("from", filterFrom);
    state.putString("to", filterTo);
    state.putString("type", filterType);
    if (currentPage.equals("edit") && editing != null) {
      state.putLong("recordId", editing.id);
      state.putString("day", editing.day);
      state.putInt("steps", editing.steps);
      state.putInt("minutes", editing.minutes);
      state.putString("recordType", editing.type);
      state.putString("note", editing.note);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    // Wait for any account operation before closing its database connection.
    worker.execute(() -> db.close());
    worker.shutdown();
    super.onDestroy();
  }
}
