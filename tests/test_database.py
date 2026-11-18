"""Validate the actual CREATE TABLE statements against desktop SQLite.
This is a schema/SQL test, not an Android instrumentation test.
"""
import re,sqlite3,pathlib,unittest
src=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/java/cn/edu/healthtrack/Store.java').read_text(encoding='utf8')
class DatabaseTest(unittest.TestCase):
 def setUp(self):
  self.db=sqlite3.connect(':memory:');self.db.execute('PRAGMA foreign_keys=ON')
  statements=re.findall(r'db[.]execSQL[(](.*?)[)];',src,re.S)
  self.assertEqual(len(statements),3)
  for statement in statements:
   sql=''.join(re.findall(r'"([^"]*)"',statement));self.db.execute(sql)
  self.db.executemany('INSERT INTO users(name,salt,hash) VALUES(?,?,?)',[('alice','salt1','hash1'),('bob','salt2','hash2')])
 def add(self,u=1,day='2026-11-02',steps=3000,minutes=30):self.db.execute('INSERT INTO records(user_id,day,steps,minutes,type,note) VALUES(?,?,?,?,?,?)',(u,day,steps,minutes,'步行',''))
 def test_unique_day_per_user(self):
  self.add();self.add(2)
  with self.assertRaises(sqlite3.IntegrityError):self.add()
 def test_foreign_key(self):
  with self.assertRaises(sqlite3.IntegrityError):self.add(999)
 def test_range_constraint(self):
  with self.assertRaises(sqlite3.IntegrityError):self.add(steps=-1)
 def test_empty_totals(self):
  self.assertEqual(self.db.execute('SELECT COALESCE(SUM(steps),0),COALESCE(SUM(minutes),0),COUNT(*) FROM records').fetchone(),(0,0,0))
 def test_user_isolation_and_week(self):
  self.add(day='2026-11-01');self.add(day='2026-11-02',steps=5000,minutes=45);self.add(2,steps=9999)
  sql='SELECT SUM(steps),SUM(minutes),COUNT(*) FROM records WHERE user_id=? AND day>=? AND day<=?'
  self.assertEqual(self.db.execute(sql,(1,'2026-11-02','2026-11-08')).fetchone(),(5000,45,1))
  self.assertEqual(self.db.execute(sql,(1,'2026-11-01','2026-11-30')).fetchone(),(8000,75,2))
 def test_ownership_update_delete(self):
  self.add();self.assertEqual(self.db.execute('UPDATE records SET steps=1 WHERE id=1 AND user_id=2').rowcount,0)
  self.assertEqual(self.db.execute('DELETE FROM records WHERE id=1 AND user_id=2').rowcount,0)
  self.assertEqual(self.db.execute('UPDATE records SET steps=6000 WHERE id=1 AND user_id=1').rowcount,1)
  self.assertEqual(self.db.execute('SELECT SUM(steps) FROM records').fetchone()[0],6000)
  self.assertEqual(self.db.execute('DELETE FROM records WHERE id=1 AND user_id=1').rowcount,1)
if __name__=='__main__':unittest.main(verbosity=2)
