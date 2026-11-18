package cn.edu.healthtrack;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Store extends SQLiteOpenHelper {
  public Store(Context c) {
    super(c, "health.db", null, 1);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,salt"
            + " TEXT NOT NULL,hash TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE records(id INTEGER PRIMARY KEY AUTOINCREMENT,user_id INTEGER NOT NULL"
            + " REFERENCES users(id),day TEXT NOT NULL,steps INTEGER NOT NULL CHECK(steps BETWEEN 0"
            + " AND 100000),minutes INTEGER NOT NULL CHECK(minutes BETWEEN 0 AND 1440),type TEXT"
            + " NOT NULL,note TEXT NOT NULL DEFAULT '',UNIQUE(user_id,day))");
    db.execSQL("CREATE INDEX idx_user_day ON records(user_id,day)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("需要数据库迁移，禁止删除用户数据");
  }

  static String hash(String password, String salt) {
    try {
      PBEKeySpec spec =
          new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 120000, 256);
      try {
        return Base64.getEncoder()
            .encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded());
      } finally {
        spec.clearPassword();
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public long register(String name, String password) {
    Rules.account(name, password);
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    String s = Base64.getEncoder().encodeToString(salt);
    ContentValues v = new ContentValues();
    v.put("name", name);
    v.put("salt", s);
    v.put("hash", hash(password, s));
    try {
      return getWritableDatabase().insertOrThrow("users", null, v);
    } catch (SQLiteConstraintException e) {
      throw new IllegalArgumentException("用户名已存在");
    }
  }

  public long login(String name, String password) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT id,salt,hash FROM users WHERE name=?", new String[] {name})) {
      if (c.moveToFirst()
          && MessageDigest.isEqual(
              hash(password, c.getString(1)).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              c.getString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        return c.getLong(0);
    }
    return -1;
  }

  public static class Record {
    public long id;
    public String day, type, note;
    public int steps, minutes;
  }

  public List<Record> list(long user, String start, String end, String type) {
    String sql =
        "SELECT id,day,steps,minutes,type,note FROM records WHERE user_id=? AND day>=? AND day<=?";
    List<String> args = new ArrayList<>(Arrays.asList("" + user, start, end));
    if (!type.equals("全部")) {
      sql += " AND type=?";
      args.add(type);
    }
    sql += " ORDER BY day DESC,id DESC";
    List<Record> result = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(sql, args.toArray(new String[0]))) {
      while (c.moveToNext()) {
        Record r = new Record();
        r.id = c.getLong(0);
        r.day = c.getString(1);
        r.steps = c.getInt(2);
        r.minutes = c.getInt(3);
        r.type = c.getString(4);
        r.note = c.getString(5);
        result.add(r);
      }
    }
    return result;
  }

  public void save(
      long user, long id, String day, int steps, int minutes, String type, String note) {
    Rules.record(day, steps, minutes, type, note);
    ContentValues v = new ContentValues();
    v.put("user_id", user);
    v.put("day", day);
    v.put("steps", steps);
    v.put("minutes", minutes);
    v.put("type", type);
    v.put("note", note);
    try {
      if (id == 0) getWritableDatabase().insertOrThrow("records", null, v);
      else if (getWritableDatabase()
              .update("records", v, "id=? AND user_id=?", new String[] {"" + id, "" + user})
          != 1) throw new IllegalArgumentException("记录不存在");
    } catch (SQLiteConstraintException e) {
      throw new IllegalArgumentException("该日期已有记录，请在记录列表中修改");
    }
  }

  public void delete(long user, long id) {
    getWritableDatabase()
        .delete("records", "id=? AND user_id=?", new String[] {"" + id, "" + user});
  }

  public long[] totals(long user, String start, String end) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT COALESCE(SUM(steps),0),COALESCE(SUM(minutes),0),COUNT(*) FROM records WHERE"
                    + " user_id=? AND day>=? AND day<=?",
                new String[] {"" + user, start, end})) {
      c.moveToFirst();
      return new long[] {c.getLong(0), c.getLong(1), c.getLong(2)};
    }
  }
}
